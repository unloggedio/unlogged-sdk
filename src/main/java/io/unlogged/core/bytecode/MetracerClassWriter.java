/*
 * Copyright 2015-2016 Michael Kocherov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * This code is imported to SELogger to avoid invalid bytecode
 */

package io.unlogged.core.bytecode;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;

public class MetracerClassWriter extends ClassWriter {
	private ClassLoader classLoader;

	public MetracerClassWriter(ClassReader theReader, ClassLoader theLoader) {
		super(theReader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classLoader = theLoader;
	}

	// Need to override in order to overcome class loaders isolation of modern app servers
	// Code is based on a test from an ASM framework and based on behaviour Javassist
	protected String getCommonSuperClass(String theType1, String theType2) {
		try {
			System.err.println("Get commonSuperClass for " + theType1 + " and " + theType2);
			ClassReader info1 = typeInfo(theType1);
			ClassReader info2 = typeInfo(theType2);

			if ((info1.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
				if (typeImplements(theType2, info2, theType1)) {
					return theType1;
				}
				if ((info2.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
					if (typeImplements(theType1, info1, theType2)) {
						return theType2;
					}
				}
				return "java/lang/Object";
			}

			if ((info2.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
				if (typeImplements(theType1, info1, theType2)) {
					return theType2;
				} else {
					return "java/lang/Object";
				}
			}

			StringBuilder b1 = typeAncestors(theType1, info1);
			StringBuilder b2 = typeAncestors(theType2, info2);
			String result = "java/lang/Object";
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

	private ClassReader typeInfo(final String theType) throws IOException {
		StringBuilder visitedLoaders = new StringBuilder();
		ClassLoader loader = classLoader;

		while(loader != null) {
			if(visitedLoaders.length() > 0)
				visitedLoaders.append(", ");

			visitedLoaders.append(loader.toString());
			InputStream is = loader.getResourceAsStream(theType + ".class");

			if(is != null) {
				try {
					return new ClassReader(is);
				} finally {
					is.close();
				}
			}

			loader = loader.getParent();
		}

		if(visitedLoaders.length() == 0)
			visitedLoaders.append("<empty>");

		throw new IOException(String.format("Failed to open %1$s in all known class loaders: %2$s", theType, visitedLoaders.toString()));
	}

	private boolean typeImplements(String theType, ClassReader theReader, String theInterface) throws IOException {
		while (!"java/lang/Object".equals(theType)) {
			String[] itfs = theReader.getInterfaces();

			for (int i = 0; i < itfs.length; ++i) {
				if (itfs[i].equals(theInterface)) {
					return true;
				}
			}

			for (int i = 0; i < itfs.length; ++i) {
				if (typeImplements(itfs[i], typeInfo(itfs[i]), theInterface)) {
					return true;
				}
			}

			theType = theReader.getSuperName();
			theReader = typeInfo(theType);
		}

		return false;
	}

	private StringBuilder typeAncestors(String theType, ClassReader theReader) throws IOException {
		StringBuilder b = new StringBuilder();

		while (!"java/lang/Object".equals(theType)) {
			b.append(';').append(theType);
			theType = theReader.getSuperName();
			theReader = typeInfo(theType);
		}

		return b;
	}
}
