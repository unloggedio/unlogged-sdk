package io.unlogged.core.processor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.tools.*;
import javax.tools.JavaFileObject.Kind;

import io.unlogged.core.DiagnosticsReceiver;

final class InterceptingJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
	private final DiagnosticsReceiver diagnostics;
	private final UnloggedFileObjects.Compiler compiler;
	private final FileObject classWeaveDat;
	private final OutputStream classWeaveOutputStream;
	private final FileObject probesToCaptureDat;
	private final FileOutputStream probesToCaptureOutputStream;

	InterceptingJavaFileManager(JavaFileManager original, DiagnosticsReceiver diagnostics) {
		super(original);
		this.compiler = UnloggedFileObjects.getCompiler(original);
		this.diagnostics = diagnostics;
		try {
			// we need to make sure that we get a direct fileoutputstream created here for our class weave dat file
			// and not a buffered or proxy via BAOS since it will lead to OOM on big code bases
			// we can get a BAOS when lombok is also sitting and we try to do a getJavaFileForOutput and the
			// compilation is invoked by intellij
			classWeaveDat = fileManager.getFileForOutput(StandardLocation.CLASS_OUTPUT, "",
					"class.weave.dat", null);
			String classWeaveOutputPath = classWeaveDat.toUri().getPath();
			File weaveOutputFile = new File(classWeaveOutputPath);
			classWeaveOutputStream = new FileOutputStream(weaveOutputFile);

			probesToCaptureDat = fileManager.getFileForOutput(StandardLocation.CLASS_OUTPUT, "",
					"probes.dat", null);
			String probeToCapturePath = probesToCaptureDat.toUri().getPath();
			File probesToCaptureOutputFile = new File(probeToCapturePath);
			probesToCaptureOutputStream = new FileOutputStream(probesToCaptureOutputFile);


		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override public JavaFileObject getJavaFileForOutput(Location location, String className, final Kind kind, FileObject sibling) throws IOException {
		JavaFileObject fileObject = fileManager.getJavaFileForOutput(location, className, kind, sibling);
		if (kind != Kind.CLASS) return fileObject;
		
		return UnloggedFileObjects.createIntercepting(compiler,
				fileObject, className, diagnostics, classWeaveOutputStream, probesToCaptureOutputStream);
	}

	@Override
	public void close() throws IOException {
		classWeaveOutputStream.flush();
		classWeaveOutputStream.close();
		probesToCaptureOutputStream.flush();
		probesToCaptureOutputStream.close();
		super.close();
	}
}