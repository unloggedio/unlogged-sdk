/*
 * Copyright (C) 2010-2011 The Project Lombok Authors.
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

package io.unlogged.core.processor;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;
import java.io.*;
import java.net.URI;
import java.nio.charset.CharsetDecoder;

class Javac7BaseFileObjectWrapper extends com.sun.tools.javac.file.BaseFileObject {
	private final UnloggedFileObject delegate;
	
	public Javac7BaseFileObjectWrapper(UnloggedFileObject delegate) {
		super(null);
		this.delegate = delegate;
	}
	
	@Override public boolean isNameCompatible(String simpleName, JavaFileObject.Kind kind) {
		return delegate.isNameCompatible(simpleName, kind);
	}
	
	@Override public URI toUri() {
		return delegate.toUri();
	}
	
	@SuppressWarnings("all")
	@Override public String getName() {
		return delegate.getName();
	}
	
	@Override public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
		return delegate.getCharContent(ignoreEncodingErrors);
	}
	
	@Override public InputStream openInputStream() throws IOException {
		return delegate.openInputStream();
	}
	
	@Override public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
		return delegate.openReader(ignoreEncodingErrors);
	}
	
	@Override public Writer openWriter() throws IOException {
		return delegate.openWriter();
	}
	
	@Override public OutputStream openOutputStream() throws IOException {
		return delegate.openOutputStream();
	}
	
	@Override public long getLastModified() {
		return delegate.getLastModified();
	}
	
	@Override public boolean delete() {
		return delegate.delete();
	}
	
	@Override public JavaFileObject.Kind getKind() {
		return delegate.getKind();
	}
	
	@Override public NestingKind getNestingKind() {
		return delegate.getNestingKind();
	}
	
	@Override public Modifier getAccessLevel() {
		return delegate.getAccessLevel();
	}
	
	protected CharsetDecoder getDecoder(boolean ignoreEncodingErrors) {
		return delegate.getDecoder(ignoreEncodingErrors);
	}
	
	@Override public boolean equals(Object obj) {
		if (!(obj instanceof Javac7BaseFileObjectWrapper)) {
			return false;
		}
		return delegate.equals(((Javac7BaseFileObjectWrapper)obj).delegate);
	}
	
	@Override public int hashCode() {
		return delegate.hashCode();
	}
	
	@Override public String toString() {
		return delegate.toString();
	}
}