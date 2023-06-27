/*
 * Copyright (C) 2010-2012 The Project Lombok Authors.
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

import io.unlogged.core.weaver.TypeHierarchy;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;

class FixedClassWriter extends ClassWriter {
    private final TypeHierarchy typeHierarchy;

    FixedClassWriter(ClassReader classReader, int flags, TypeHierarchy typeHierarchy) {
        super(classReader, flags);
        this.typeHierarchy = typeHierarchy;
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        //By default, ASM will attempt to live-load the class types, which will fail if meddling with classes in an
        //environment with custom classloaders, such as Equinox. It's just an optimization; returning Object is always legal.
        type1 = type1.replace('/', '.');
        type2 = type2.replace('/', '.');

        try {
            try {
                String fromOld = getCommonSuperClassWithRealClass(type1, type2);
                fromOld = fromOld.replace('.', '/');
                return fromOld;
            } catch (Exception e) {
                // cant do with real class definitions
            }
            String fromNew = typeHierarchy.findCommonSuper(type1, type2);
            fromNew = fromNew.replace('.', '/');
            return fromNew;
//			return super.getCommonSuperClass(type1, type2);
        } catch (OutOfMemoryError e) {
            throw e;
        } catch (Throwable t) {
            return "java/lang/Object";
        }
    }


    // Need to override in order to overcome class loaders isolation of modern app servers
    // Code is based on a test from an ASM framework and based on behaviour Javassist
    protected String getCommonSuperClassWithRealClass(String theType1, String theType2) throws ClassNotFoundException {
        try {
            Class<?> info11 = Class.forName(theType1);
            Class<?> info12 = Class.forName(theType2);

            if (info11.isInterface()) {
                if (typeImplements(theType2, info12, theType1)) {
                    return theType1;
                }
                if (info12.isInterface()) {
                    if (typeImplements(theType1, info12, theType2)) {
                        return theType2;
                    }
                }
                return "java.lang.Object";
            }

            if (info12.isInterface()) {
                if (typeImplements(theType1, info11, theType2)) {
                    return theType2;
                } else {
                    return "java.lang.Object";
                }
            }

            StringBuilder b1 = typeAncestors(info11);
            StringBuilder b2 = typeAncestors(info12);
            String result = "java.lang.Object";
            int end1 = b1.length();
            int end2 = b2.length();

            while (true) {
                int start1 = b1.lastIndexOf(";", end1 - 1);
                int start2 = b2.lastIndexOf(";", end2 - 1);
                if (start1 != -1 && start2 != -1
                        && end1 - start1 == end2 - start2) {
                    String p1 = b1.substring(start1 + 1, end1);
                    String p2 = b2.substring(start2 + 1, end2);
                    if (p1.equals(p2)) {
                        result = p1;
                        end1 = start1;
                        end2 = start2;
                    } else {
                        return result;
                    }
                } else {
                    return result;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
    }

    private boolean typeImplements(String theType, Class<?> theReader, String theInterface) throws IOException, ClassNotFoundException {
        while (!"java/lang/Object".equals(theType)) {
            Class<?>[] itfs = theReader.getInterfaces();

            for (int i = 0; i < itfs.length; ++i) {
                if (itfs[i].getCanonicalName().equals(theInterface)) {
                    return true;
                }
            }

            for (int i = 0; i < itfs.length; ++i) {
                if (typeImplements(itfs[i].getCanonicalName(), Class.forName(itfs[i].getCanonicalName()),
                        theInterface)) {
                    return true;
                }
            }

            theReader = theReader.getSuperclass();
        }

        return false;
    }

    private StringBuilder typeAncestors(Class<?> theReader) throws IOException {
        StringBuilder b = new StringBuilder();

        while (!"java.lang.Object".equals(theReader.getCanonicalName())) {
            b.append(';').append(theReader.getCanonicalName());
            theReader = theReader.getSuperclass();
        }

        return b;
    }
}