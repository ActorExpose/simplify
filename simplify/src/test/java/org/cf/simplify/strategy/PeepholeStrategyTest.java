package org.cf.simplify.strategy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.cf.simplify.ExecutionGraphManipulator;
import org.cf.simplify.OptimizerTester;
import org.cf.smalivm.TestState;
import org.cf.smalivm.Tester;
import org.cf.smalivm.VirtualMachine;
import org.cf.smalivm.type.UninitializedInstance;
import org.cf.smalivm.type.UnknownValue;
import org.cf.smalivm.type.VirtualType;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c;
import org.jf.dexlib2.iface.instruction.OffsetInstruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction35c;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.util.ReferenceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeepholeStrategyTest {

    private static final String CLASS_NAME = "Lpeephole_strategy_test;";
    private static final String CLASS_NAME_BINARY = "peephole_strategy_test";

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(PeepholeStrategyTest.class.getSimpleName());

    private static ExecutionGraphManipulator getOptimizedGraph(String methodName, Object... args) {
        return getOptimizedGraph(null, methodName, args);
    }

    private static ExecutionGraphManipulator getOptimizedGraph(VirtualMachine vm, String methodName, Object... args) {
        TestState initial = new TestState();
        initial.setRegisters(args);
        ExecutionGraphManipulator manipulator;
        if (vm == null) {
            manipulator = OptimizerTester.getGraphManipulator(CLASS_NAME, methodName, initial);
        } else {
            manipulator = OptimizerTester.getGraphManipulator(vm, CLASS_NAME, methodName, initial);
        }
        PeepholeStrategy strategy = new PeepholeStrategy(manipulator);
        strategy.perform();

        return manipulator;
    }

    public static class ConstantPredicate {

        private static final String METHOD_NAME = "constantPredicate()I";

        @Test
        public void constantPredicateReplacedWithUnconditionalBranch() {
            // I say phrases like "unconditional branch" instead of "goto".
            // I'm also a riot at dinner parties.
            ExecutionGraphManipulator manipulator = getOptimizedGraph(METHOD_NAME);

            BuilderInstruction instruction = manipulator.getInstruction(1);

            assertEquals(Opcode.GOTO_32, instruction.getOpcode());
            assertEquals(4, ((OffsetInstruction) instruction).getCodeOffset());
        }

    }

    public static class ReplaceClassForName {

        private static final int ADDRESS = 0;
        private static final String METHOD_NAME = "classForName()V";

        @Test
        public void invokeClassForNameForKnownClassIsReplaced() {
            testForExpectedInstruction("java.lang.String", "Ljava/lang/String;");
        }

        @Test
        public void invokeClassForNameForLocalClassIsReplaced() {
            testForExpectedInstruction("peephole_strategy_test", "Lpeephole_strategy_test;");
        }

        @Test
        public void invokeClassForNameForUnknownValueIsNotReplaced() {
            ExecutionGraphManipulator manipulator =
                    getOptimizedGraph(METHOD_NAME, 0, new UnknownValue(), "Ljava/lang/String;");
            Instruction35c instruction = (Instruction35c) manipulator.getInstruction(ADDRESS);
            String methodDescriptor = ReferenceUtil.getMethodDescriptor((MethodReference) instruction.getReference());

            assertEquals("Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;", methodDescriptor);
        }

        private void testForExpectedInstruction(String register0, String expectedClassName) {
            ExecutionGraphManipulator manipulator = getOptimizedGraph(METHOD_NAME, 0, register0, "Ljava/lang/String;");

            BuilderInstruction21c instruction = (BuilderInstruction21c) manipulator.getInstruction(ADDRESS);
            assertEquals(Opcode.CONST_CLASS, instruction.getOpcode());
            assertEquals(0, instruction.getRegisterA());

            String actualClassName = ReferenceUtil.getReferenceString(instruction.getReference());
            assertEquals(expectedClassName, actualClassName);
        }
    }

    public static class RemoveUselessCheckCast {

        @Test
        public void uselessCheckCastIsRemoved() {
            String methodName = "uselessCheckCast(I)V";
            ExecutionGraphManipulator manipulator = getOptimizedGraph(methodName, 0, 0, "I");

            assertArrayEquals(new int[] { 0 }, manipulator.getAddresses());
            assertEquals("return-void", manipulator.getOp(0).toString());
        }

        @Test
        public void uselessCheckCastWithMultiplePathsIsRemoved() {
            String methodName = "uselessCheckCastWithMultiplePaths(I)V";
            ExecutionGraphManipulator manipulator = getOptimizedGraph(methodName, 0, new UnknownValue(), "I");

            int[] addresses = manipulator.getAddresses();
            assertArrayEquals(new int[] { 0, 2, 4 }, addresses);
            assertEquals("if-eqz r0, :addr_4", manipulator.getOp(0).toString());
            assertEquals("sget r0, Ljava/lang/Integer;->MAX_VALUE:I", manipulator.getOp(2).toString());
            assertEquals("return-void", manipulator.getOp(4).toString());
        }

        @Test
        public void activeCheckCastIsNotRemoved() {
            String methodName = "activeCheckCast(Ljava/lang/Object;)V";
            ExecutionGraphManipulator manipulator =
                    getOptimizedGraph(methodName, 0, new UnknownValue(), "Ljava/lang/Object;");

            assertArrayEquals(new int[] { 0, 2 }, manipulator.getAddresses());
            assertEquals("check-cast r0, Ljava/lang/Integer;", manipulator.getOp(0).toString());
            assertEquals("return-void", manipulator.getOp(2).toString());
        }

        @Test
        public void activeCheckCastWithMultiplePathsIsNotRemoved() {
            String methodName = "activeCheckCastWithMultiplePaths(Ljava/lang/Object;)V";
            ExecutionGraphManipulator manipulator =
                    getOptimizedGraph(methodName, 0, new UnknownValue(), "Ljava/lang/Object;");

            assertArrayEquals(new int[] { 0, 2, 3, 6, 7, 9 }, manipulator.getAddresses());
            assertEquals("if-eqz r1, :addr_7", manipulator.getOp(0).toString());
            assertEquals("const/4 r0, 0x0", manipulator.getOp(2).toString());
            assertEquals("invoke-static {r0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;",
                    manipulator.getOp(3).toString());
            assertEquals("move-result r1", manipulator.getOp(6).toString());
            assertEquals("check-cast r1, Ljava/lang/Integer;", manipulator.getOp(7).toString());
            assertEquals("return-void", manipulator.getOp(9).toString());
        }
    }

    public static class StringInit {

        private static final int ADDRESS = 0;
        private static final String METHOD_NAME = "stringInit()V";
        private static final String ZENSUNNI_POEM = "Sand keeps the skin clean, and the mind.";

        private VirtualMachine vm;
        private VirtualType thisReference;

        @BeforeEach
        public void setUp() {
            vm = Tester.spawnVM(true);
        }

        @Test
        public void stringInitWithKnownStringIsReplaced() {
            testForExpectedInstruction(ZENSUNNI_POEM.getBytes(), ZENSUNNI_POEM);
        }

        @Test
        public void stringInitWithUnknownValueIsNotReplaced() {
            VirtualType instanceType = vm.getClassManager().getVirtualType("Ljava/lang/String;");
            ExecutionGraphManipulator manipulator = getOptimizedGraph(vm, METHOD_NAME, 0, new
                    UninitializedInstance(instanceType), "Ljava/lang/String;", 1, new UnknownValue(), "[B");
            Instruction35c instruction = (Instruction35c) manipulator.getInstruction(ADDRESS);
            String methodDescriptor = ReferenceUtil.getMethodDescriptor((MethodReference) instruction.getReference());

            assertEquals("Ljava/lang/String;-><init>([B)V", methodDescriptor);
        }

        private void testForExpectedInstruction(Object register1, String expectedConstant) {
            VirtualType instanceType = vm.getClassManager().getVirtualType("Ljava/lang/String;");
            ExecutionGraphManipulator manipulator = getOptimizedGraph(METHOD_NAME, 0,
                    new UninitializedInstance(instanceType), "Ljava/lang/String;", 1, register1, "[B");

            BuilderInstruction21c instruction = (BuilderInstruction21c) manipulator.getInstruction
                    (ADDRESS);
            assertEquals(Opcode.CONST_STRING, instruction.getOpcode());
            assertEquals(0, instruction.getRegisterA());

            String actualConstant = ((StringReference) instruction.getReference()).getString();
            assertEquals(expectedConstant, actualConstant);
        }
    }

    public static class UninitializedInstanceThisReference {

        private static final int ADDRESS = 0;

        private VirtualMachine vm;
        private VirtualType thisReference;

        @BeforeEach
        public void setUp() {
            vm = Tester.spawnVM(true);
            thisReference = vm.getClassManager().getVirtualClass(CLASS_NAME);
        }

        @Test
        public void thisReferencedWithInvokeIsCorrectlyOptimizedAsConstClass() {
            String methodName = "invokeGetClassOnThis()V";
            ExecutionGraphManipulator manipulator = getOptimizedGraph(vm, methodName, 1, new UninitializedInstance(thisReference), CLASS_NAME);

            assertEquals(BuilderInstruction21c.class, manipulator.getInstruction(ADDRESS).getClass());
            BuilderInstruction21c instruction = (BuilderInstruction21c) manipulator.getInstruction(ADDRESS);
            assertEquals(Opcode.CONST_CLASS, instruction.getOpcode());
            assertEquals(0, instruction.getRegisterA());

            String actualClassName = ReferenceUtil.getReferenceString(instruction.getReference());
            assertEquals(CLASS_NAME, actualClassName);
        }

        @Test
        public void thisReferencedWithInvokeRangeIsCorrectlyOptimizedAsConstClass() {
            String methodName = "invokeRangeGetClassOnThis()V";
            ExecutionGraphManipulator manipulator = getOptimizedGraph(vm, methodName, 1, new UninitializedInstance(thisReference), CLASS_NAME);

            assertEquals(BuilderInstruction21c.class, manipulator.getInstruction(ADDRESS).getClass());
            BuilderInstruction21c instruction = (BuilderInstruction21c) manipulator.getInstruction(ADDRESS);
            assertEquals(Opcode.CONST_CLASS, instruction.getOpcode());
            assertEquals(0, instruction.getRegisterA());

            String actualClassName = ReferenceUtil.getReferenceString(instruction.getReference());
            assertEquals(CLASS_NAME, actualClassName);
        }
    }
}
