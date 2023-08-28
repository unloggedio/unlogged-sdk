package io.unlogged;

import fi.iki.elonen.NanoHTTPD;
import io.unlogged.command.AgentCommandServer;
import io.unlogged.command.ServerMetadata;
import io.unlogged.logging.IErrorLogger;
import io.unlogged.logging.IEventLogger;
import io.unlogged.logging.Logging;
import io.unlogged.logging.SimpleFileLogger;
import io.unlogged.logging.impl.DetailedEventStreamAggregatedLogger;
import io.unlogged.logging.perthread.PerThreadBinaryFileAggregatedLogger;
import io.unlogged.logging.perthread.RawFileCollector;
import io.unlogged.logging.util.FileNameGenerator;
import io.unlogged.logging.util.NetworkClient;
import io.unlogged.util.StreamUtil;
import io.unlogged.weaver.WeaveConfig;
import io.unlogged.weaver.WeaveParameters;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class is the main program of SELogger as a javaagent.
 */
public class Runtime {

    public static final int AGENT_SERVER_PORT = 12100;
    private static Runtime instance;
    private static List<Pair<String, List<Integer>>> pendingClassRegistrations = new ArrayList<>();
    private final ScheduledExecutorService probeReaderExecutor = Executors.newSingleThreadScheduledExecutor();
    private AgentCommandServer httpServer;
    private IErrorLogger errorLogger;
    /**
     * The logger receives method calls from injected instructions via selogger.logging.Logging class.
     */
    private IEventLogger logger = Logging.initialiseDiscardLogger();
    private long lastProbesLoadTime;

    /**
     * Process command line arguments and prepare an output directory
     *
     * @param args string arguments for weaver
     */
    private Runtime(String args) {

        try {
            WeaveParameters weaveParameters = new WeaveParameters(args);


            ServerMetadata serverMetadata =
                    new ServerMetadata(weaveParameters.getIncludedNames().toString(), Constants.AGENT_VERSION);

            httpServer = new AgentCommandServer(AGENT_SERVER_PORT, serverMetadata);


            File outputDir = new File(weaveParameters.getOutputDirname());
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            if (!outputDir.isDirectory() || !outputDir.canWrite()) {
                System.err.println("[unlogged] ERROR: " + outputDir.getAbsolutePath() + " is not writable.");
                return;
            }

            WeaveConfig config = new WeaveConfig(weaveParameters);

            if (!config.isValid()) {
                System.out.println("[unlogged] no weaving option is specified.");
                return;
            }

            errorLogger = new SimpleFileLogger(outputDir);

            errorLogger.log("Java version: " + System.getProperty("java.version"));
            errorLogger.log("Agent version: " + Constants.AGENT_VERSION);
            errorLogger.log("Params: " + args);

            System.out.println("[unlogged]" +
                    " session Id: [" + config.getSessionId() + "]" +
                    " on hostname [" + NetworkClient.getHostname() + "]");

            URL probesToRecordUrl = this.getClass().getClassLoader().getResource("probes.dat");

            List<Integer> probesToRecord = new ArrayList<>();
            try {

                File file = new File(probesToRecordUrl.toURI());
                lastProbesLoadTime = file.lastModified();
                probesToRecord = probeFileToIdList(file);
            } catch (Exception e) {
                // uri is not hierarchical when running a jar
                probesToRecordUrl = null;
                probesToRecord = probeFileStreamToIdList(
                        this.getClass().getClassLoader().getResourceAsStream("probes.dat"));
            }

            switch (weaveParameters.getMode()) {


                case Discard:
                    logger = Logging.initialiseDiscardLogger();

                case PerThread:

                    NetworkClient networkClient = new NetworkClient(weaveParameters.getServerAddress(),
                            config.getSessionId(), weaveParameters.getAuthToken(), errorLogger);

                    FileNameGenerator fileNameGenerator1 = new FileNameGenerator(outputDir, "index-", ".zip");
                    RawFileCollector fileCollector =
                            new RawFileCollector(weaveParameters.getFilesPerIndex(), fileNameGenerator1,
                                    networkClient, errorLogger, outputDir);

                    FileNameGenerator fileNameGenerator = new FileNameGenerator(outputDir, "log-", ".selog");
                    PerThreadBinaryFileAggregatedLogger perThreadBinaryFileAggregatedLogger
                            = new PerThreadBinaryFileAggregatedLogger(fileNameGenerator, errorLogger, fileCollector);

                    logger = Logging.initialiseAggregatedLogger(perThreadBinaryFileAggregatedLogger, outputDir);
                    break;

                case Testing:

                    NetworkClient networkClient1 =
                            new NetworkClient(weaveParameters.getServerAddress(),
                                    config.getSessionId(), weaveParameters.getAuthToken(), errorLogger);

                    FileNameGenerator archiveFileNameGenerator =
                            new FileNameGenerator(outputDir, "index-", ".zip");

                    RawFileCollector fileCollector1 =
                            new RawFileCollector(weaveParameters.getFilesPerIndex(), archiveFileNameGenerator,
                                    networkClient1, errorLogger, outputDir);

                    FileNameGenerator logFileNameGenerator =
                            new FileNameGenerator(outputDir, "log-", ".selog");

                    PerThreadBinaryFileAggregatedLogger perThreadBinaryFileAggregatedLogger1
                            = new PerThreadBinaryFileAggregatedLogger(logFileNameGenerator, errorLogger,
                            fileCollector1);

                    logger = Logging.initialiseDetailedAggregatedLogger(perThreadBinaryFileAggregatedLogger1,
                            outputDir, probesToRecord);

                    DetailedEventStreamAggregatedLogger detailedLogger = (DetailedEventStreamAggregatedLogger) logger;
                    if (probesToRecordUrl != null) {
                        URL finalProbesToRecordUrl = probesToRecordUrl;
                        probeReaderExecutor.scheduleWithFixedDelay(
                                () -> {
                                    try {
                                        File probesFile = new File(finalProbesToRecordUrl.toURI());
                                        if (!probesFile.exists()) {
                                            return;
                                        }
                                        long newProbesFileModifiedTime = probesFile.lastModified();
                                        if (newProbesFileModifiedTime > lastProbesLoadTime) {
                                            lastProbesLoadTime = newProbesFileModifiedTime;
                                            List<Integer> newProbeIdList = probeFileToIdList(probesFile);
                                            detailedLogger.setProbesToRecord(newProbeIdList);
                                        }
                                    } catch (URISyntaxException | IOException e) {
                                        // should never happen
                                    }
                                }, 1000, 300, TimeUnit.MILLISECONDS
                        );
                    }
                    break;

            }


            httpServer.setAgentCommandExecutor(new AgentCommandExecutorImpl(logger.getObjectMapper(), logger));
            httpServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);

            java.lang.Runtime.getRuntime()
                    .addShutdownHook(new Thread(() -> {
                        close();
                    }));


        } catch (Throwable thx) {
            logger = Logging.initialiseDiscardLogger();
            thx.printStackTrace();
            System.err.println(
                    "[unlogged] agent init failed, this session will not be recorded => " + thx.getMessage());
        }
    }

    public static Runtime getInstance(String args) {
        if (instance != null) {
            return instance;
        }
        synchronized (Runtime.class) {
            if (instance != null) {
                return instance;
            }

            try {
                StackTraceElement callerClassAndMethodStack = new Exception().getStackTrace()[1];
                Class<?> callerClass = Class.forName(callerClassAndMethodStack.getClassName());
                for (Method method : callerClass.getMethods()) {
                    if (method.getAnnotation(Unlogged.class) != null) {
                        // caller method
                        Unlogged annotationData = method.getAnnotation(Unlogged.class);
                        if (!annotationData.enable()) {
                            return null;
                        }
                        break;
                    }
                }

            } catch (ClassNotFoundException e) {
                // should never happen
                // disable if happened
                return null;
            }
            instance = new Runtime(args);
//            for (Pair<String, List<Integer>> pendingClassRegistration : pendingClassRegistrations) {
//                registerClass(pendingClassRegistration.getFirst(), pendingClassRegistration.getSecond());
//            }

        }
        return instance;
    }

    @NotNull
    private List<Integer> probeFileToIdList(File file) throws IOException {
        InputStream probesFile = this.getClass().getClassLoader().getResourceAsStream(file.getName());
        return probeFileStreamToIdList(probesFile);
    }

    @NotNull
    private List<Integer> probeFileStreamToIdList(InputStream probesFile) throws IOException {
        List<Integer> probesToRecord = new ArrayList<>();
        if (probesFile == null) {
            return probesToRecord;
        }
        byte[] probeToRecordBytes = StreamUtil.streamToBytes(probesFile);
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(probeToRecordBytes));
        try {
            while (true) {
                int probeId = dis.readInt();
                probesToRecord.add(probeId);
            }
        } catch (EOFException e) {

        }
        return probesToRecord;
    }

//    // this method is called by all classes which were probed during compilation time
//    public static boolean registerClass(String classInfoBytes, List<Integer> probeIdsToRecord) {
//        if (instance != null) {
//            byte[] decodedBytes = Base64.getDecoder().decode(classInfoBytes);
//            ClassInfo classInfo = new ClassInfo();
//
//            try {
//                ByteArrayInputStream in = new ByteArrayInputStream(decodedBytes);
//                classInfo.readFromDataStream(in);
//            } catch (IOException e) {
////            throw new RuntimeException(e);
//                return false;
//            }
//
//            instance.logger.recordWeaveInfo(decodedBytes, classInfo, probeIdsToRecord);
//
//        } else {
//
//            pendingClassRegistrations.add(new Pair<>(classInfoBytes, probeIdsToRecord));
//        }
//        return true;
//    }

    /**
     * Close data streams if necessary
     */
    public void close() {
        if (logger != null) {
            logger.close();
        }
        if (httpServer != null) {
            httpServer.stop();

        }
        if (errorLogger != null) {
            errorLogger.close();
        }
        System.out.println("[unlogged] shutdown complete");

    }

    public enum Mode {Stream, Frequency, FixedSize, Discard, Network, PerThread, Testing}

}
