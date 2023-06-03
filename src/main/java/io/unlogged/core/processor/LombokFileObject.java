package io.unlogged.core.processor;


import java.nio.charset.CharsetDecoder;

import javax.tools.JavaFileObject;

public interface LombokFileObject extends JavaFileObject {
	CharsetDecoder getDecoder(boolean ignoreEncodingErrors);
}