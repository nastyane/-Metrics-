package com.vk.itmo;

import org.objectweb.asm.*;


public class CustomClassVisitor extends ClassVisitor {

    public CustomClassVisitor() {
        super(Opcodes.ASM9);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        System.out.println("Visit started" + " " + name);
    }

    @Override
    public void visitOuterClass(String owner, String name, String descriptor) {
        System.out.println("Visit outer class finished" + " " + name);
    }

    @Override
    public void visitAttribute(Attribute attribute) {
        System.out.println("Visit attribute finished" + " " + attribute);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        System.out.println("Visit field started" + " " + name);
        FieldVisitor fieldVisitor = super.visitField(access, name, descriptor, signature, value);
        System.out.println("Visit field finished" + " " + name);
        return fieldVisitor;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        System.out.println("Visit method started" + " " + name);
        MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
        System.out.println("Visit method finished" + " " + name);
        return methodVisitor;
    }

    @Override
    public void visitEnd() {
        System.out.println("Visit end started");
        super.visitEnd();
        System.out.println("Visit end finished");
    }
}

