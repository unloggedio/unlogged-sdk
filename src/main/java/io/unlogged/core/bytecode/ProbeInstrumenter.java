/*
 * Copyright (C) 2010-2021 The Project Lombok Authors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.unlogged.core.bytecode;

import com.insidious.common.weaver.ClassInfo;
import io.unlogged.Runtime;
import io.unlogged.core.DiagnosticsReceiver;
import io.unlogged.core.PostCompilerTransformation;
import io.unlogged.util.ByteTools;
import io.unlogged.weaver.DataInfoProvider;
import io.unlogged.weaver.TypeHierarchy;
import org.objectweb.asm.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class ProbeInstrumenter implements PostCompilerTransformation {

    private final Weaver weaver;
    private final TypeHierarchy typeHierarchy;
    private DataInfoProvider dataInfoProvider;

    public ProbeInstrumenter(TypeHierarchy typeHierarchy) throws IOException {
        this.typeHierarchy = typeHierarchy;
        weaver = new Weaver(new WeaveConfig(new RuntimeWeaverParameters("")), typeHierarchy);
    }

    private static List<String> splitString(String text, int maxLength) {
        List<String> results = new ArrayList<>();
        int length = text.length();

        for (int i = 0; i < length; i += maxLength) {
            results.add(text.substring(i, Math.min(length, i + maxLength)));
        }

        return results;
    }

    @Override
    public byte[] applyTransformations(byte[] original, String fileName,
                                       DiagnosticsReceiver diagnostics,
                                       OutputStream classWeaveOutputStream,
                                       DataInfoProvider dataInfoProvider) throws IOException {

        ClassFileMetaData classFileMetadata = new ClassFileMetaData(original);
        InstrumentedClass instrumentedClassBytes;
        final String className = classFileMetadata.getClassName();

        ByteArrayOutputStream probesToRecordOutputStream = new ByteArrayOutputStream();
        try {

            dataInfoProvider.setProbeOutputStream(probesToRecordOutputStream);
            weaver.setDataInfoProvider(dataInfoProvider);
            instrumentedClassBytes = weaver.weave(fileName, className, original);
            dataInfoProvider.flushIdInformation();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] probesToRecordBytes = probesToRecordOutputStream.toByteArray();
        byte[] classWeaveInfo = instrumentedClassBytes.getClassWeaveInfo();

        ClassInfo classInfo = new ClassInfo();
        classInfo.readFromDataStream(new ByteArrayInputStream(classWeaveInfo));

        final String probeDataCompressedBase64 = ByteTools.compressBase64String(probesToRecordBytes);
        final String compressedClassWeaveInfo = ByteTools.compressBase64String(classWeaveInfo);

//        List<Integer> probeIdsAgain = Runtime.bytesToIntList(
//                ByteTools.decompressBase64String(probeDataCompressedBase64));
//        System.out.println("Probes to record: " + probeDataCompressedBase64 + " === " + compressedClassWeaveInfo);


//        if (instrumentedClassBytes.classWeaveInfo.length == 0) {
//        return instrumentedClassBytes.getBytes();
//        }


//        final AtomicBoolean changesMade = new AtomicBoolean();
//        if (fileName.contains("$")) {
//            classWeaveBytesToBeWritten.write(classWeaveInfo);
//            return instrumentedClassBytes.getBytes();
//        }

        // Create a ClassReader to read the original class bytes
        ClassReader reader = new ClassReader(instrumentedClassBytes.getBytes());

        // Create a ClassWriter to write the modified class bytes
        ClassWriter writer = new FixedClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES,
                typeHierarchy);

        // Create a ClassVisitor to visit and modify the class
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, writer) {
            private boolean hasStaticInitializer = false;

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                // Check if this is the static initializer (clinit)
                if (name.equals("<clinit>")) {
                    hasStaticInitializer = true;
//                    System.out.println("Modify existing static method in [" + className + "]");
                    // Create a method visitor to add code to the static initializer
                    MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);

                    // Create a MethodVisitor to visit and modify the method
                    return new MethodVisitor(Opcodes.ASM9, methodVisitor) {
                        @Override
                        public void visitCode() {
                            addClassWeaveInfo(mv, compressedClassWeaveInfo, probeDataCompressedBase64);
                            mv.visitMaxs(3, 0);
                            super.visitCode();
                        }

                    };

                }

                return super.visitMethod(access, name, desc, signature, exceptions);
            }

            @Override
            public void visitEnd() {
                // If the class doesn't have a static initializer, create one
                if (!hasStaticInitializer) {
//                    System.out.println("Adding new static method in [" + className + "]");

                    MethodVisitor methodVisitor = writer.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                    methodVisitor.visitCode();
                    addClassWeaveInfo(methodVisitor, compressedClassWeaveInfo, probeDataCompressedBase64);
                    methodVisitor.visitMaxs(3, 0);
                    methodVisitor.visitInsn(Opcodes.RETURN);
                    methodVisitor.visitEnd();

                }

                super.visitEnd();
            }
        };

        // Start the class modification process
        reader.accept(cv, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
////        return original;
    }

    private void addClassWeaveInfo(MethodVisitor mv, String base64Bytes, String probesToRecordBase64) {

        // new string on the stack
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);

        List<String> stringParts = splitString(base64Bytes, 40000);
        for (String stringPart : stringParts) {
            mv.visitLdcInsn(stringPart);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

        }

        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);

        // new string on the stack
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);

        List<String> probesToRecordParts = splitString(probesToRecordBase64, 40000);
        for (String stringPart : probesToRecordParts) {
            mv.visitLdcInsn(stringPart);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

        }

        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);


        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "io/unlogged/Runtime", "registerClass",
                "(Ljava/lang/String;Ljava/lang/String;)V", false);
    }
}
