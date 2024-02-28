package io.unlogged.core.processor;

import io.unlogged.core.DiagnosticsReceiver;
import io.unlogged.weaver.DataInfoProvider;

import javax.tools.*;
import javax.tools.JavaFileObject.Kind;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

final class InterceptingJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
    private final DiagnosticsReceiver diagnostics;
    private final UnloggedFileObjects.Compiler compiler;
    //    private final FileObject classWeaveDat;
//    private final OutputStream classWeaveOutputStream;
    //    private final FileObject probesToCaptureDat;
//    private final FileOutputStream probesToCaptureOutputStream;
    private final File idsInfoOutputFile;
    private final DataInfoProvider dataInfoProvider;

    InterceptingJavaFileManager(JavaFileManager original, DiagnosticsReceiver diagnostics) {
        super(original);
        this.compiler = UnloggedFileObjects.getCompiler(original);
        this.diagnostics = diagnostics;
        try {
            // we need to make sure that we get a direct fileoutputstream created here for our class weave dat file
            // and not a buffered or proxy via BAOS since it will lead to OOM on big code bases
            // we can get a BAOS when lombok is also sitting and we try to do a getJavaFileForOutput and the
            // compilation is invoked by intellij

            FileObject notARealFile = fileManager.getFileForOutput(StandardLocation.CLASS_OUTPUT, "",
                    "notAFile", null);
            String classesPath = "";
            String actualClassesPath = "";
            String notARealFileOutputUri = notARealFile.toUri().getPath();
            if (notARealFileOutputUri.contains("/classes/")) {
                classesPath = notARealFileOutputUri.substring(0,
                        notARealFileOutputUri.indexOf("/classes/")) + "/classes/";
                actualClassesPath = classesPath;
            } else if (notARealFileOutputUri.contains("/test-classes/")) {
                classesPath = notARealFileOutputUri.substring(0,
                        notARealFileOutputUri.indexOf("/test-classes/")) + "/test-classes/";
                actualClassesPath = notARealFileOutputUri.substring(0,
                        notARealFileOutputUri.indexOf("/test-classes/")) + "/classes/";
            }

//            classWeaveDat = fileManager.getFileForOutput(StandardLocation.CLASS_OUTPUT, "",
//                    "class.weave.dat", null);

            if (classesPath.contains("/build/")) {
                // suspect gradle build
                String subBuildPath = classesPath.substring(0, classesPath.indexOf("/build/"));
                String gradleFilePath = classesPath.substring(0, classesPath.indexOf("/build/")) + "/build.gradle";
                if (new File(gradleFilePath).exists()) {
                    // definitely gradle build
                    // so we need to put resources in build/resources/main/
                    classesPath = subBuildPath + "/build/resources/main/";
                    File resourcesFolder = new File(classesPath);
                    if (!resourcesFolder.exists()) {
                        resourcesFolder.mkdirs();
                    }
                }
            }

//            File weaveOutputFile = new File(classesPath + "class.weave.dat");
//            classWeaveOutputStream = new FileOutputStream(weaveOutputFile, true);


            File probesToCaptureOutputFile = new File(classesPath + "probes.dat");
//            probesToCaptureOutputStream = new FileOutputStream(probesToCaptureOutputFile, true);


            int classId = 0;
            int methodId = 0;
            int probeId = 0;

            Path pathname = FileSystems.getDefault().getPath(System.getProperty("user.home"), ".unlogged", "unlogged" +
                    ".ids.dat");
            idsInfoOutputFile = pathname.toFile();
            if (!idsInfoOutputFile.getParentFile().exists()) {
                idsInfoOutputFile.getParentFile().mkdirs();
            }
            if (idsInfoOutputFile.exists()) {
                try {
                    DataInputStream idsInfoReader = new DataInputStream(new FileInputStream(idsInfoOutputFile));
                    classId = idsInfoReader.readInt();
                    methodId = idsInfoReader.readInt();
                    probeId = idsInfoReader.readInt();
                } catch (IOException e) {
                    // nothing to do, out ids info file is bad
                }
            }

            dataInfoProvider = new DataInfoProvider(classId, methodId, probeId);
            dataInfoProvider.setIdsInfoFile(idsInfoOutputFile);
//            dataInfoProvider.setProbeOutputStream(probesToCaptureOutputStream);

//            probesToCaptureOutputStream = new FileOutputStream(probesToCaptureOutputFile, true);


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, final Kind kind, FileObject sibling) throws IOException {
        JavaFileObject fileObject = fileManager.getJavaFileForOutput(location, className, kind, sibling);
        if (kind != Kind.CLASS) return fileObject;

        return UnloggedFileObjects.createIntercepting(
                compiler, fileObject, className, diagnostics, dataInfoProvider);
    }

    @Override
    public void close() throws IOException {
//        probesToCaptureOutputStream.flush();
//        probesToCaptureOutputStream.close();
        super.close();
    }
}