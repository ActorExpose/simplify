package org.cf.smalivm.type;

import org.cf.util.ClassNameUtils;
import org.jf.dexlib2.util.ReferenceUtil;
import org.jf.dexlib2.writer.builder.BuilderClassDef;
import org.jf.dexlib2.writer.builder.BuilderField;
import org.jf.dexlib2.writer.builder.BuilderMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class VirtualClass extends VirtualGeneric {

    private static final Logger log = LoggerFactory.getLogger(VirtualClass.class.getSimpleName());

    private final BuilderClassDef classDef;
    private Set<VirtualClass> ancestors;
    private Map<String, VirtualMethod> methodDescriptorToMethod;
    private Map<String, VirtualField> fieldNameToField;

    VirtualClass(BuilderClassDef classDef) {
        super(classDef, classDef.getType(), ClassNameUtils.internalToBinary(classDef.getType()),
                ClassNameUtils.internalToSource(classDef.getType()));
        this.classDef = classDef;
        methodDescriptorToMethod = null;
        fieldNameToField = null;
    }

    private static void getAncestors0(VirtualClass virtualClass, Set<VirtualClass> ancestors) {
        if (ancestors.contains(virtualClass)) {
            return;
        }
        ancestors.add(virtualClass);

        for (VirtualClass ancestor : getImmediateAncestors(virtualClass)) {
            getAncestors0(ancestor, ancestors);
        }
    }

    public boolean isAncestorOf(VirtualClass other) {
        return other.getAncestors().contains(this);
    }

    public boolean isChildOf(VirtualClass other) {
        return getAncestors().contains(other);
    }

    private static Set<VirtualClass> getImmediateAncestors(VirtualClass virtualClass) {
        List<String> parentNames = new LinkedList<>();
        BuilderClassDef classDef = virtualClass.getClassDef();
        parentNames.addAll(classDef.getInterfaces());
        if (classDef.getSuperclass() != null) {
            parentNames.add(classDef.getSuperclass());
        }

        ClassManager classManager = getClassManager();
        return parentNames.stream().map(classManager::getVirtualClass).collect(Collectors.toSet());
    }

    @Override
    public Set<VirtualClass> getAncestors() {
        if (ancestors != null) {
            return ancestors;
        }

        ancestors = new LinkedHashSet<>(3);
        getAncestors0(this, ancestors);
        ancestors.remove(this);

        return ancestors;
    }

    /**
     * If a static field is declared in an ancestor, that field may be referenced through a
     * child class, e.g. Lchild_class;->parentField:I
     * Or it may be referenced directly, e.g. Lparent_class;->parentField:I
     * Additionally, identically named static fields may be in child, parent, or grandparent.
     * If referencing child but it's actually declared in parent and grandparent, the value
     * from the parent is used. If not in the parent and only in the grandparent, the value
     * from the grandparent is used.
     */
    @Override
    public final
    @Nullable
    VirtualField getField(String fieldName) {
        VirtualField field = getField0(fieldName);
        if (field != null) {
            return field;
        }
        for (VirtualClass ancestor : getAncestors()) {
            field = ancestor.getField0(fieldName);
            if (field != null) {
                return field;
            }
        }

        return null;
    }

    @Override
    public Collection<VirtualField> getFields() {
        if (fieldNameToField == null) {
            fieldNameToField = buildFieldsMap();
        }
        List<VirtualField> fields = new LinkedList<>();
        fields.addAll(fieldNameToField.values());

        return fields;
    }

    @Override
    public
    @Nullable
    VirtualMethod getMethod(String methodDescriptor) {
        VirtualMethod method = getMethod0(methodDescriptor);
        if (method != null) {
            return method;
        }
        for (VirtualClass ancestor : getAncestors()) {
            method = ancestor.getMethod0(methodDescriptor);
            if (method != null) {
                return method;
            }
        }

        return null;
    }

    @Override
    public Collection<VirtualMethod> getMethods() {
        if (methodDescriptorToMethod == null) {
            methodDescriptorToMethod = buildMethodsMap();
        }
        List<VirtualMethod> methods = new LinkedList<>();
        methods.addAll(methodDescriptorToMethod.values());

        return methods;
    }

    @Override
    public boolean instanceOf(VirtualGeneric targetType) {
        if (targetType instanceof VirtualArray || targetType instanceof VirtualPrimitive) {
            return false;
        }

        if (equals(targetType)) {
            return true;
        }
        for (VirtualGeneric ancestor : getAncestors()) {
            if (ancestor.equals(targetType)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public BuilderClassDef getClassDef() {
        return classDef;
    }

    @Override
    public boolean isInnerClassOf(VirtualGeneric parentClass) {
        // TODO: easy - add tests
        return getBinaryName().startsWith(parentClass.getBinaryName() + "$");
    }

    @Override
    public String getPackage() {
        String binaryName = getBinaryName();
        int lastDot = binaryName.lastIndexOf('.');
        if (lastDot < 0) {
            return "";
        }

        return binaryName.substring(0, lastDot);
    }

    @Override
    public boolean isSamePackageOf(VirtualGeneric otherClass) {
        return getPackage().equals(otherClass.getPackage());
    }

    private Map<String, VirtualField> buildFieldsMap() {
        Map<String, VirtualField> fields = new HashMap<>();
        for (BuilderField builderField : getClassDef().getFields()) {
            String name = builderField.getName();
            if (builderField.getName().startsWith("shadow$_")) {
                // These fields are added by debugger.
                continue;
            }
            VirtualField field = new VirtualField(builderField, this);
            fields.put(name, field);
        }

        return fields;
    }

    private Map<String, VirtualMethod> buildMethodsMap() {
        Map<String, VirtualMethod> methods = new HashMap<>();
        for (BuilderMethod method : getClassDef().getMethods()) {
            String descriptor = ReferenceUtil.getMethodDescriptor(method).split("->")[1];
            VirtualMethod virtualMethod = new VirtualRealMethod(method, this);
            methods.put(descriptor, virtualMethod);
        }

        return Collections.unmodifiableMap(methods);
    }

    private VirtualField getField0(String fieldName) {
        if (fieldNameToField == null) {
            fieldNameToField = buildFieldsMap();
        }

        return fieldNameToField.get(fieldName);
    }

    private VirtualMethod getMethod0(String methodDescriptor) {
        if (methodDescriptorToMethod == null) {
            methodDescriptorToMethod = buildMethodsMap();
        }

        return methodDescriptorToMethod.get(methodDescriptor);
    }

}
