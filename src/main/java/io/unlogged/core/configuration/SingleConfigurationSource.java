/*
 * Copyright (C) 2014-2020 The Project Lombok Authors.
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
package io.unlogged.core.configuration;


import java.util.*;
import java.util.Map.Entry;

import static io.unlogged.core.configuration.ConfigurationParser.*;

public final class SingleConfigurationSource implements ConfigurationSource {
	private final Map<ConfigurationKey<?>, Result> values;
	private final List<ConfigurationFile> imports;
	
	public static ConfigurationSource parse(ConfigurationFile context, ConfigurationParser parser) {
		final Map<ConfigurationKey<?>, Result> values = new HashMap<ConfigurationKey<?>, Result>();
		final List<ConfigurationFile> imports = new ArrayList<ConfigurationFile>();
		Collector collector = new Collector() {
			@Override public void addImport(ConfigurationFile importFile, ConfigurationFile context, int lineNumber) {
				imports.add(importFile);
			}
			
			@Override public void clear(ConfigurationKey<?> key, ConfigurationFile context, int lineNumber) {
				values.put(key, new Result(null, true));
			}
			
			@Override public void set(ConfigurationKey<?> key, Object value, ConfigurationFile context, int lineNumber) {
				values.put(key, new Result(value, true));
			}
			
			@Override public void add(ConfigurationKey<?> key, Object value, ConfigurationFile context, int lineNumber) {
				modifyList(key, value, true);
			}
			
			@Override public void remove(ConfigurationKey<?> key, Object value, ConfigurationFile context, int lineNumber) {
				modifyList(key, value, false);
			}
			
			@SuppressWarnings("unchecked")
			private void modifyList(ConfigurationKey<?> key, Object value, boolean add) {
				Result result = values.get(key);
				List<ListModification> list;
				if (result == null || result.getValue() == null) {
					list = new ArrayList<ListModification>();
					values.put(key, new Result(list, result != null));
				} else {
					list = (List<ListModification>) result.getValue();
				}
				list.add(new ListModification(value, add));
			}
		};
		parser.parse(context, collector);
		return new SingleConfigurationSource(values, imports);
	}
	
	private SingleConfigurationSource(Map<ConfigurationKey<?>, Result> values, List<ConfigurationFile> imports) {
		this.values = new HashMap<ConfigurationKey<?>, Result>();
		for (Entry<ConfigurationKey<?>, Result> entry : values.entrySet()) {
			Result result = entry.getValue();
			if (result.getValue() instanceof List<?>) {
				this.values.put(entry.getKey(), new Result(Collections.unmodifiableList((List<?>) result.getValue()), result.isAuthoritative()));
			} else {
				this.values.put(entry.getKey(), result);
			}
		}
		this.imports = Collections.unmodifiableList(imports);
	}
	
	@Override 
	public Result resolve(ConfigurationKey<?> key) {
		return values.get(key);
	}
	
	@Override
	public List<ConfigurationFile> imports() {
		return imports;
	}
}
