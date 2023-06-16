/*
 * These are stub versions of various bits of javac-internal API (for various different versions of javac). Lombok is compiled against these.
 */
package com.sun.tools.javac.file;

import javax.tools.JavaFileObject;
import java.nio.file.Path;

public abstract class PathFileObject implements JavaFileObject {
	protected PathFileObject(BaseFileManager fileManager, Path path) {}
}
