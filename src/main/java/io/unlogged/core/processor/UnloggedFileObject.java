package io.unlogged.core.processor;


import java.nio.charset.CharsetDecoder;

import javax.tools.JavaFileObject;

public interface UnloggedFileObject extends JavaFileObject {
	CharsetDecoder getDecoder(boolean ignoreEncodingErrors);
}