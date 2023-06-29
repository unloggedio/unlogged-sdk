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

import io.unlogged.core.DiagnosticsReceiver;
import io.unlogged.core.PostCompilerTransformation;
import io.unlogged.weaver.DataInfoProvider;
import io.unlogged.weaver.TypeHierarchy;

import java.io.IOException;
import java.io.OutputStream;


public class ProbeInstrumenter implements PostCompilerTransformation {

    private final Weaver weaver;
    private DataInfoProvider dataInfoProvider;

    public ProbeInstrumenter(TypeHierarchy typeHierarchy) throws IOException {
        weaver = new Weaver(new WeaveConfig(new RuntimeWeaverParameters("")), typeHierarchy);
    }


    @Override
    public byte[] applyTransformations(byte[] original, String fileName,
                                       DiagnosticsReceiver diagnostics,
                                       OutputStream classWeaveOutputStream,
                                       DataInfoProvider dataInfoProvider) throws IOException {

        ClassFileMetaData classFileMetadata = new ClassFileMetaData(original);
        InstrumentedClass instrumentedClassBytes;
        try {

            weaver.setDataInfoProvider(dataInfoProvider);
            instrumentedClassBytes = weaver.weave(fileName, classFileMetadata.getClassName(), original);
            dataInfoProvider.flushIdInformation();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] classWeaveInfo = instrumentedClassBytes.getClassWeaveInfo();
        classWeaveOutputStream.write(classWeaveInfo);
        classWeaveOutputStream.flush();


//        if (instrumentedClassBytes.classWeaveInfo.length == 0) {
        return instrumentedClassBytes.getBytes();
//        }

//        ClassReader reader = new ClassReader(instrumentedClassBytes.getBytes());
//        ClassWriter writer = new ClassWriter(reader, 0);
//
//        final AtomicBoolean changesMade = new AtomicBoolean();
//        if (fileName.contains("$")) {
//            classWeaveBytesToBeWritten.write(classWeaveInfo);
//            return instrumentedClassBytes.getBytes();
//        }
//
//
//        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
//            private boolean foundClassInit = false;
//
//            @Override
//            public void visitEnd() {
////                changesMade.set(true);
////                FieldVisitor fieldVisitor =
////                        super.visitField(
////                                Opcodes.ACC_FINAL + Opcodes.ACC_STATIC, "classWeaveBytes", "[B", null,
////                                classWeaveInfo);
////                if (fieldVisitor != null) {
////                    fieldVisitor.visitEnd();
////                }
//            }
//
//            @Override
//            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
//                MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
//                if (name.equals("<clinit>")) {
//                    foundClassInit = true;
//                    MethodVisitor staticFieldInitializerAdapter = new MethodVisitor(Opcodes.ASM6, methodVisitor) {
//                        @Override
//                        public void visitCode() {
//                            super.visitCode();
//
////                            super.visitMethodInsn(Opcodes.INVOKESTATIC, "io/unlogged/logging/Runtime", "registerClass",
////                                    "([B)V", false);
//                        }
//                    };
//                }
//                return methodVisitor;
//            }
//        }, 0);
//        return changesMade.get() ? writer.toByteArray() : instrumentedClassBytes.getBytes();
////        return original;
    }
}
