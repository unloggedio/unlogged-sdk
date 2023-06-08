package io.unlogged.core.processor;

import java.io.IOException;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

import io.unlogged.core.DiagnosticsReceiver;

final class InterceptingJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
	private final DiagnosticsReceiver diagnostics;
	private final UnloggedFileObjects.Compiler compiler;
	
	InterceptingJavaFileManager(JavaFileManager original, DiagnosticsReceiver diagnostics) {
		super(original);
		this.compiler = UnloggedFileObjects.getCompiler(original);
		this.diagnostics = diagnostics;
	}
	
	@Override public JavaFileObject getJavaFileForOutput(Location location, String className, final Kind kind, FileObject sibling) throws IOException {
		JavaFileObject fileObject = fileManager.getJavaFileForOutput(location, className, kind, sibling);
		if (kind != Kind.CLASS) return fileObject;
		
		return UnloggedFileObjects.createIntercepting(compiler, fileObject, className, diagnostics);
	}
}