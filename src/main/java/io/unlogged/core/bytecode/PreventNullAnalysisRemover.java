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
import io.unlogged.weaver.TypeHierarchy;
import org.objectweb.asm.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class PreventNullAnalysisRemover implements PostCompilerTransformation {

    private final Weaver weaver;


    private final File classWeaveDatFile = new File("target/classes/class.weave.dat");
    private final FileOutputStream weaveWriter;
//    private final ZipOutputStream zippedClassWeaveDat;

    public PreventNullAnalysisRemover(TypeHierarchy typeHierarchy) throws IOException {
//        File outputDir = new File("weaver-output");
//        outputDir.mkdir();
//        weaver = new Weaver(outputDir, new WeaveConfig(new RuntimeWeaverParameters("")), typeHierarchy);
        weaver = new Weaver(new WeaveConfig(new RuntimeWeaverParameters("")), typeHierarchy);
//        if (classWeaveDatFile.exists()) {
//            classWeaveDatFile.delete();
//        }
//        classWeaveDatFile.createNewFile();
        weaveWriter = new FileOutputStream(classWeaveDatFile);
//        zippedClassWeaveDat = new ZipOutputStream(weaveWriter);
//        zippedClassWeaveDat.putNextEntry(new ZipEntry("class.weave.dat"));

    }

    @Override
    public byte[] applyTransformations(byte[] original, String fileName, DiagnosticsReceiver diagnostics) throws IOException {

        ClassFileMetaData classFileMetadata = new ClassFileMetaData(original);
        InstrumentedClass instrumentedClassBytes = new InstrumentedClass(original, new byte[0]);
        try {
            instrumentedClassBytes = weaver.weave(fileName, classFileMetadata.getClassName(), original);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] classWeaveInfo = instrumentedClassBytes.getClassWeaveInfo();
        weaveWriter.write(classWeaveInfo);
        weaveWriter.flush();


//        if (instrumentedClassBytes.classWeaveInfo.length == 0) {
            return instrumentedClassBytes.getBytes();
//        }

//        ClassReader reader = new ClassReader(instrumentedClassBytes.getBytes());
//        ClassWriter writer = new ClassWriter(reader, 0);
//
//        final AtomicBoolean changesMade = new AtomicBoolean();
//
//
//        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
//            @Override
//            public void visitEnd() {
//                super.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
//                        "unloggedClassWeaveBytes", Type.VOID, null, )
//                super.visitEnd();
//            }
//        }, 0);
//        return changesMade.get() ? writer.toByteArray() : null;
//        return original;
    }
}
