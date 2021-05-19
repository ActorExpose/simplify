package org.cf.smalivm.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.cf.smalivm.Tester;
import org.cf.smalivm.dex.CommonTypes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class VirtualArrayTest {


    private static ClassManager classManager;
    private VirtualArray virtualArray;

    @BeforeAll
    public static void getClassManager() throws IOException {
        classManager = new ClassManagerFactory().build(Tester.TEST_CLASS_PATH);
    }

    @Test
    public void twoDimensionalStringArrayHasExpectedImmediateAncestors() {
        String arrayType = "[[" + CommonTypes.STRING;
        virtualArray = (VirtualArray) classManager.getVirtualType(arrayType);

        String[] expectedImmediateAncestorNames =
                new String[]{ "[[Ljava/lang/Object;", "[[Ljava/lang/CharSequence;", "[[Ljava/lang/Comparable;", "[[Ljava/io/Serializable;" };
        Set<VirtualType> expectedImmediateAncestors =
                Arrays.stream(expectedImmediateAncestorNames).map(a -> classManager.getVirtualType(a)).collect(Collectors.toSet());
        Set<? extends VirtualType> ancestors = virtualArray.getImmediateAncestors();

        assertEquals(expectedImmediateAncestors, ancestors);
    }

    @Test
    public void oneDimensionalStringArrayHasExpectedImmediateAncestors() {
        String arrayType = "[" + CommonTypes.STRING;
        virtualArray = (VirtualArray) classManager.getVirtualType(arrayType);

        String[] expectedImmediateAncestorNames =
                new String[]{ "[Ljava/lang/Object;", "[Ljava/lang/CharSequence;", "[Ljava/lang/Comparable;", "[Ljava/io/Serializable;" };
        Set<VirtualType> expectedImmediateAncestors =
                Arrays.stream(expectedImmediateAncestorNames).map(a -> classManager.getVirtualType(a)).collect(Collectors.toSet());
        Set<? extends VirtualType> ancestors = virtualArray.getImmediateAncestors();

        assertEquals(expectedImmediateAncestors, ancestors);
    }

    @Test
    public void threeDimensionalStringArrayHasExpectedAncestors() {
        String arrayType = "[[[" + CommonTypes.STRING;
        virtualArray = (VirtualArray) classManager.getVirtualType(arrayType);

        String[] expectedAncestorNames =
                new String[]{ "[[[Ljava/lang/Object;", "[[[Ljava/lang/CharSequence;", "[[[Ljava/lang/Comparable;", "[[[Ljava/io/Serializable;",
                              "[[Ljava/lang/Object;", "[Ljava/lang/Object;", "Ljava/lang/Object;" };
        Set<VirtualType> expectedAncestors =
                Arrays.stream(expectedAncestorNames).map(a -> classManager.getVirtualType(a)).collect(Collectors.toSet());
        Set<? extends VirtualType> ancestors = virtualArray.getAncestors();

        assertEquals(expectedAncestors, ancestors);
    }

    @Test
    public void threeDimensionalIntArrayHasExpectedAncestors() {
        String arrayType = "[[[" + CommonTypes.INTEGER;
        virtualArray = (VirtualArray) classManager.getVirtualType(arrayType);

        String[] expectedAncestorNames = new String[]{ "[[Ljava/lang/Object;", "[Ljava/lang/Object;", "Ljava/lang/Object;" };
        Set<VirtualType> expectedAncestors =
                Arrays.stream(expectedAncestorNames).map(a -> classManager.getVirtualType(a)).collect(Collectors.toSet());
        Set<? extends VirtualType> ancestors = virtualArray.getAncestors();

        assertEquals(expectedAncestors, ancestors);
    }
}
