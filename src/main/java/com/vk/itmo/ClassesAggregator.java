package com.vk.itmo;

import org.objectweb.asm.*;

import java.util.*;

public class ClassesAggregator extends ClassVisitor {
    private final Map<String, String> classToParent = new HashMap<>();
    private final Map<String, Set<MethodSignature>> classToMethods = new HashMap<>();
    private final Map<String, Integer> classToFields = new HashMap<>();
    private int countAssignments = 0;
    private int countBranches = 0;
    private int countConditions = 0;

    private String currentClass;

    public ClassesAggregator() {
        super(Opcodes.ASM9);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        currentClass = null;
        if (isOpcode(Opcodes.ACC_ENUM, access) ||
                isOpcode(Opcodes.ACC_RECORD, access) ||
                isOpcode(Opcodes.ACC_INTERFACE, access) ||
                isOpcode(Opcodes.ACC_MODULE, access)) {
            return;
        }

        assert name != null;
        assert superName != null;

        currentClass = name;
        if (!"java/lang/Object".equals(superName)) {
            classToParent.put(name, superName);
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (currentClass != null && access != Opcodes.ACC_PRIVATE) {
            MethodSignature methodSignature = new MethodSignature(name, descriptor);
            classToMethods.computeIfAbsent(currentClass, k -> new HashSet<>()).add(methodSignature);
        }
        return new MethodVisitor(api) {
            @Override
            public void visitVarInsn(int opcode, int var) {
                if (Opcodes.ISTORE <= opcode && opcode <= Opcodes.ASTORE) {
                    countAssignments++;
                }
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                if (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC) {
                    countAssignments++;
                }
            }

            @Override
            public void visitJumpInsn(int opcode, Label label) {

                countBranches++;

                if (Opcodes.IFEQ <= opcode && opcode <= Opcodes.IF_ACMPNE) {
                    countConditions++;
                }
            }

            int countLabels = 0;

            @Override
            public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
                countBranches += labels.length + 1;
                countLabels++;
            }

            @Override
            public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
                countBranches += labels.length + 1;
                countLabels++;
            }
        };
    }


    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        if (currentClass != null) {
            classToFields.merge(currentClass, 1, Integer::sum);
        }
        return null;
    }


    public Metrics calculateMetrics() {
        Map<String, Integer> countInheritance = new HashMap<>();
        for (String key : classToParent.keySet()) {
            recursiveSearch(key, countInheritance);
        }
        int sum = 0;
        int max = 0;
        for (String clazz : countInheritance.keySet()) {
            int inheritance = countInheritance.get(clazz);
            max = Math.max(max, inheritance);
            sum += inheritance;
        }
        double average = 0.0;
        if (!countInheritance.isEmpty()) {
            average = (double) sum / countInheritance.size();
        }

        int countRedefinition = 0;
        for (String className : classToMethods.keySet()) {
            countRedefinition += countOverrideMethods(className);
        }


        double averageCountOverride = 0.;
        if (!classToMethods.isEmpty()) {
            averageCountOverride = (double) countRedefinition / classToMethods.size();
        }

        int countField = 0;
        for (String className : classToFields.keySet()) {
            countField += classToFields.get(className);
        }

        double averageCountField = 0.;
        if (!classToFields.isEmpty()) {
            averageCountField = (double) countField / classToFields.size();
        }
        double metricABC = Math.sqrt(countAssignments * countAssignments + countBranches * countBranches + countConditions * countConditions);
        return new Metrics(average, max, averageCountOverride, averageCountField, metricABC);
    }


    private int countOverrideMethods(String className) {
        Map<MethodSignature, Integer> methodToCount = new HashMap<>();

        String currentClass = className;
        while (currentClass != null) {
            Set<MethodSignature> methods = classToMethods.get(currentClass);
            if (methods == null) {
                // супер класс не из jar файла
                break;
            }

            for (MethodSignature method : methods) {
                methodToCount.merge(method, 1, Integer::sum);
            }
            currentClass = classToParent.get(currentClass);
        }

        int res = 0;
        for (int count : methodToCount.values()) {
            if (count > 1) {
                res++;
            }
        }

        return res;
    }

    private void recursiveSearch(String clazz, Map<String, Integer> countInheritance) {
        String parent = classToParent.get(clazz);
        if (parent == null) {
            countInheritance.put(clazz, 0);
            return;
        }
        if (!countInheritance.containsKey(parent)) {
            recursiveSearch(parent, countInheritance);
        }
        countInheritance.put(clazz, countInheritance.get(parent) + 1);
    }

    private static boolean isOpcode(int opcode, int access) {
        return (opcode & access) != 0;
    }
}
