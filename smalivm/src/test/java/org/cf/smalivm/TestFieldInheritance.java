//package org.cf.smalivm;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//import org.cf.smalivm.context.ExecutionGraph;
//import org.cf.smalivm.context.HeapItem;
//import org.cf.smalivm.exception.VirtualMachineException;
//import org.cf.smalivm.type.ClassManager;
//import org.cf.smalivm.type.VirtualClass;
//import org.cf.smalivm.type.VirtualField;
//import org.cf.smalivm.type.VirtualMethod;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//public class TestFieldInheritance {
//
//    private VirtualMachine vm;
//    private VirtualClass childClass;
//    private VirtualClass parentClass;
//    private VirtualClass grandparentClass;
//
//    @BeforeEach
//    public void setupVM() {
//        /*
//         * On TravisCI, these tests fail with StackOverflowError and missing classes unless classes are reloaded. Local test classes are actually
//         * completely missing for no obvious reason.
//         */
//        vm = VMTester.spawnVM(true);
//        ClassManager classManager = vm.getClassManager();
//        childClass = classManager.getVirtualClass("Lchild_class;");
//        parentClass = classManager.getVirtualClass("Lparent_class;");
//        grandparentClass = classManager.getVirtualClass("Lgrandparent_class;");
//    }
//
//    @Test
//    public void parentInitializedAndAccessible() throws VirtualMachineException {
//        VirtualMethod method = childClass.getMethod("stubMethod()V");
//        ExecutionGraph graph = vm.execute(method);
//
//        String fieldName = "parentField";
//        int expectedValue = 0x3;
//        HeapItem fieldItem;
//        VirtualField field;
//
//        field = childClass.getField(fieldName);
//        fieldItem = graph.getTerminatingFieldConsensus(field);
//        assertEquals(expectedValue, fieldItem.getValue());
//
//        field = parentClass.getField(fieldName);
//        fieldItem = graph.getTerminatingFieldConsensus(field);
//        assertEquals(expectedValue, fieldItem.getValue());
//    }
//
//    @Test
//    public void grandparentInitializedAndAccessible() throws VirtualMachineException {
//        VirtualMethod method = childClass.getMethod("stubMethod()V");
//        ExecutionGraph graph = vm.execute(method);
//
//        String fieldName = "grandparentField";
//        int expectedValue = 0x4;
//        HeapItem fieldItem;
//        VirtualField field;
//
//        field = childClass.getField(fieldName);
//        fieldItem = graph.getTerminatingFieldConsensus(field);
//        assertEquals(expectedValue, fieldItem.getValue());
//
//        field = parentClass.getField(fieldName);
//        fieldItem = graph.getTerminatingFieldConsensus(field);
//        assertEquals(expectedValue, fieldItem.getValue());
//
//        field = grandparentClass.getField(fieldName);
//        fieldItem = graph.getTerminatingFieldConsensus(field);
//        assertEquals(expectedValue, fieldItem.getValue());
//    }
//
//    @Test
//    public void grandparentInitializedIncludingLiteralFieldsAndAccessible() throws VirtualMachineException {
//        VirtualMethod method = childClass.getMethod("stubMethod()V");
//        ExecutionGraph graph = vm.execute(method);
//
//        String fieldName = "intLiteral";
//        int expectedValue = 0x5;
//        HeapItem fieldItem;
//        VirtualField field;
//
//        field = childClass.getField(fieldName);
//        fieldItem = graph.getTerminatingFieldConsensus(field);
//        assertEquals(expectedValue, fieldItem.getValue());
//
//        field = parentClass.getField(fieldName);
//        fieldItem = graph.getTerminatingFieldConsensus(field);
//        assertEquals(expectedValue, fieldItem.getValue());
//
//        field = grandparentClass.getField(fieldName);
//        fieldItem = graph.getTerminatingFieldConsensus(field);
//        assertEquals(expectedValue, fieldItem.getValue());
//    }
//}
