/*
 * Copyright (C) 2010-2019 The Project Lombok Authors.
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
package io.unlogged.core;

import io.unlogged.core.bytecode.ProbeInstrumenter;
import io.unlogged.core.javac.HandlerLibrary;
import io.unlogged.core.processor.UnloggedProcessorConfig;
import io.unlogged.weaver.DataInfoProvider;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PostCompiler {
    private static List<PostCompilerTransformation> transformations;

    ;

    private PostCompiler() {/* prevent instantiation*/}

    public static byte[] applyTransformations(
            byte[] original, String fileName,
			DiagnosticsReceiver diagnostics,
            OutputStream classWeaveOutputStream,
			DataInfoProvider dataInfoProvider,
			UnloggedProcessorConfig unloggedProcessorConfig) {
        if (System.getProperty("unlogged.disablePostCompiler", null) != null) return original;
        init(diagnostics, unloggedProcessorConfig);
        byte[] previous = original;
        for (PostCompilerTransformation transformation : transformations) {
            try {
                byte[] next = transformation.applyTransformations(
                        previous, fileName, diagnostics, classWeaveOutputStream, dataInfoProvider);
                if (next != null) {
                    previous = next;
                }
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw, true));
                diagnostics.addError(String.format(
                        "Error during the transformation of '%s'; post-compiler '%s' caused an exception: %s", fileName,
                        transformation.getClass().getName(), sw));
            }
        }
        return previous;
    }

    private static synchronized void init(DiagnosticsReceiver diagnostics, UnloggedProcessorConfig unloggedProcessorConfig) {
        if (transformations != null) return;
        try {
//			transformations = SpiLoadUtil.readAllFromIterator(SpiLoadUtil.findServices(PostCompilerTransformation.class, PostCompilerTransformation.class.getClassLoader()));
            transformations = Collections.singletonList(
                    new ProbeInstrumenter(HandlerLibrary.getTypeHierarchy(), unloggedProcessorConfig));
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw, true));
            diagnostics.addError("Could not load post-compile transformers: "
                    + e.getMessage() + "\n" + sw);
        }
    }

    public static OutputStream wrapOutputStream(
            final OutputStream originalStream,
            final String fileName,
            final DiagnosticsReceiver diagnostics,
            OutputStream classWeaveOutputStream, 
			DataInfoProvider dataInfoProvider,
			UnloggedProcessorConfig unloggedProcessorConfig) {
//		return originalStream;
        if (System.getProperty("unlogged.disable", null) != null) return originalStream;

        // close() can be called more than once and should be idempotent, therefore, ensure we never transform more than once.
        final AtomicBoolean closed = new AtomicBoolean();

        return new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                if (closed.getAndSet(true)) {
                    originalStream.close();
                    return;
                }

                // no need to call super
                byte[] original = toByteArray();
                byte[] copy = null;
                if (original.length > 0) {
                    try {
                        copy = applyTransformations(original,
                                fileName, diagnostics, classWeaveOutputStream, dataInfoProvider, unloggedProcessorConfig);
                    } catch (Exception e) {
                        e.printStackTrace();
                        diagnostics.addWarning(String.format(
                                "Error during the transformation of '%s'; no post-compilation has been applied",
                                fileName));
                    }
                }

                if (copy == null) {
                    copy = original;
                }

                // Exceptions below should bubble
                originalStream.write(copy);
                originalStream.close();
            }
        };
    }
}
