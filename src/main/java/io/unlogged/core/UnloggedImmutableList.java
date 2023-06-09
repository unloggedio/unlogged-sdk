/*
 * Copyright (C) 2013 The Project Lombok Authors.
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

import java.util.*;

public final class UnloggedImmutableList<T> implements Iterable<T> {
	private Object[] content;
	private static final UnloggedImmutableList<?> EMPTY = new UnloggedImmutableList<Object>(new Object[0]);
	
	@SuppressWarnings("unchecked")
	public static <T> UnloggedImmutableList<T> of() {
		return (UnloggedImmutableList<T>) EMPTY;
	}
	
	public static <T> UnloggedImmutableList<T> of(T a) {
		return new UnloggedImmutableList<T>(new Object[] {a});
	}
	
	public static <T> UnloggedImmutableList<T> of(T a, T b) {
		return new UnloggedImmutableList<T>(new Object[] {a, b});
	}
	
	public static <T> UnloggedImmutableList<T> of(T a, T b, T c) {
		return new UnloggedImmutableList<T>(new Object[] {a, b, c});
	}
	
	public static <T> UnloggedImmutableList<T> of(T a, T b, T c, T d) {
		return new UnloggedImmutableList<T>(new Object[] {a, b, c, d});
	}
	
	public static <T> UnloggedImmutableList<T> of(T a, T b, T c, T d, T e) {
		return new UnloggedImmutableList<T>(new Object[] {a, b, c, d, e});
	}
	
	@SuppressWarnings({"all", "unchecked"})
	public static <T> UnloggedImmutableList<T> of(T a, T b, T c, T d, T e, T f, T... g) {
		Object[] rest = g == null ? new Object[] {null} : g;
		Object[] val = new Object[rest.length + 6];
		System.arraycopy(rest, 0, val, 6, rest.length);
		val[0] = a;
		val[1] = b;
		val[2] = c;
		val[3] = d;
		val[4] = e;
		val[5] = f;
		return new UnloggedImmutableList<T>(val);
	}
	
	public static <T> UnloggedImmutableList<T> copyOf(Collection<? extends T> list) {
		return new UnloggedImmutableList<T>(list.toArray());
	}
	
	public static <T> UnloggedImmutableList<T> copyOf(Iterable<? extends T> iterable) {
		List<T> list = new ArrayList<T>();
		for (T o : iterable) list.add(o);
		return copyOf(list);
	}
	
	public static <T> UnloggedImmutableList<T> copyOf(T[] array) {
		Object[] content = new Object[array.length];
		System.arraycopy(array, 0, content, 0, array.length);
		return new UnloggedImmutableList<T>(content);
	}
	
	private UnloggedImmutableList(Object[] content) {
		this.content = content;
	}
	
	public UnloggedImmutableList<T> replaceElementAt(int idx, T newValue) {
		Object[] newContent = content.clone();
		newContent[idx] = newValue;
		return new UnloggedImmutableList<T>(newContent);
	}
	
	public UnloggedImmutableList<T> append(T newValue) {
		int len = content.length;
		Object[] newContent = new Object[len + 1];
		System.arraycopy(content, 0, newContent, 0, len);
		newContent[len] = newValue;
		return new UnloggedImmutableList<T>(newContent);
	}
	
	public UnloggedImmutableList<T> prepend(T newValue) {
		int len = content.length;
		Object[] newContent = new Object[len + 1];
		System.arraycopy(content, 0, newContent, 1, len);
		newContent[0] = newValue;
		return new UnloggedImmutableList<T>(newContent);
	}
	
	public int indexOf(T val) {
		int len = content.length;
		if (val == null) {
			for (int i = 0; i < len; i++) if (content[i] == null) return i;
			return -1;
		}
		
		for (int i = 0; i < len; i++) if (val.equals(content[i])) return i;
		return -1;
	}
	
	public UnloggedImmutableList<T> removeElement(T val) {
		int idx = indexOf(val);
		return idx == -1 ? this : removeElementAt(idx);
	}
	
	public UnloggedImmutableList<T> removeElementAt(int idx) {
		int len = content.length;
		Object[] newContent = new Object[len - 1];
		if (idx > 0) System.arraycopy(content, 0, newContent, 0, idx);
		if (idx < len - 1) System.arraycopy(content, idx + 1, newContent, idx, len - idx - 1);
		return new UnloggedImmutableList<T>(newContent);
	}
	
	public boolean isEmpty() {
		return content.length == 0;
	}
	
	public int size() {
		return content.length;
	}
	
	@SuppressWarnings("unchecked")
	public T get(int idx) {
		return (T) content[idx];
	}
	
	public boolean contains(T in) {
		if (in == null) {
			for (Object e : content) if (e == null) return true;
			return false;
		}
		
		for (Object e : content) if (in.equals(e)) return true;
		return false;
	}
	
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			private int idx = 0;
			@Override public boolean hasNext() {
				return idx < content.length;
			}
			
			@SuppressWarnings("unchecked")
			@Override public T next() {
				if (idx < content.length) return (T) content[idx++];
				throw new NoSuchElementException();
			}
			
			@Override public void remove() {
				throw new UnsupportedOperationException("List is immutable");
			}
		};
	}
	
	@Override public String toString() {
		return Arrays.toString(content);
	}
	
	@Override public boolean equals(Object obj) {
		if (!(obj instanceof UnloggedImmutableList)) return false;
		if (obj == this) return true;
		return Arrays.equals(content, ((UnloggedImmutableList<?>) obj).content);
	}
	
	@Override public int hashCode() {
		return Arrays.hashCode(content);
	}
}
