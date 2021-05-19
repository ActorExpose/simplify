//package org.cf.smalivm;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//
//import org.cf.smalivm.context.ExecutionGraph;
//import org.cf.smalivm.context.HeapItem;
//import org.cf.smalivm.context.MethodState;
//import org.cf.smalivm.exception.VirtualMachineException;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//public class TestMethodInheritance {
//
//    public static class ChildClass {
//
//        private static final String CLASS_NAME = "Lchild_class;";
//        private static final String EXPECTED_VALUE = "abstract?";
//        private VirtualMachine vm;
//
//        @BeforeEach
//        public void setupVM() {
//            vm = VMTester.spawnVM(true);
//        }
//
//        @Test
//        public void canInvokeAbstractMethodThroughParentReference() throws VirtualMachineException {
//            String methodName = "callsAbstractMethod()Ljava/lang/String;";
//            ExecutionGraph graph = vm.execute(CLASS_NAME + "->" + methodName);
//            HeapItem item = graph.getTerminatingRegisterConsensus(MethodState.ReturnRegister);
//
//            assertEquals(EXPECTED_VALUE, item.getValue());
//        }
//
//        @Test
//        public void canInvokeImplemenetedAbstractMethod() throws VirtualMachineException {
//            String methodName = "abstractMethod()Ljava/lang/String;";
//            ExecutionGraph graph = vm.execute(CLASS_NAME + "->" + methodName);
//            HeapItem item = graph.getTerminatingRegisterConsensus(MethodState.ReturnRegister);
//            String value = (String) item.getValue();
//
//            assertEquals(EXPECTED_VALUE, value);
//        }
//
//        @Test
//        public void canInvokeParentMethodOfChild() throws VirtualMachineException {
//            String methodName = "callsParentMethod()Ljava/lang/String;";
//            ExecutionGraph graph = vm.execute(CLASS_NAME + "->" + methodName);
//            HeapItem item = graph.getTerminatingRegisterConsensus(MethodState.ReturnRegister);
//
//            assertEquals("parentMethod", item.getValue());
//        }
//    }
//
//    public static class ParentClass {
//
//        private static final String CLASS_NAME = "Lparent_class;";
//
//        private VirtualMachine vm;
//
//        @BeforeEach
//        public void setupVM() {
//            vm = VMTester.spawnVM();
//        }
//
//        @Test
//        public void executingAbstractMethodReturnsNull() {
//            String methodName = "abstractMethod()Ljava/lang/String;";
//            assertThrows(IllegalArgumentException.class, () -> vm.execute(CLASS_NAME + "->" + methodName));
//        }
//    }
//
//}
