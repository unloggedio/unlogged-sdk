package io.unlogged;

import com.insidious.common.weaver.ClassInfo;
import fi.iki.elonen.NanoHTTPD;
import io.unlogged.command.AgentCommandServer;
import io.unlogged.command.ServerMetadata;
import io.unlogged.logging.*;
import io.unlogged.logging.impl.DetailedEventStreamAggregatedLogger;
import io.unlogged.logging.perthread.PerThreadBinaryFileAggregatedLogger;
import io.unlogged.logging.perthread.RawFileCollector;
import io.unlogged.logging.util.FileNameGenerator;
import io.unlogged.logging.util.NetworkClient;
import io.unlogged.util.ByteTools;
import io.unlogged.util.StreamUtil;
import io.unlogged.weaver.WeaveConfig;
import io.unlogged.weaver.WeaveParameters;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * This class is the main program of SELogger as a javaagent.
 */
public class Runtime {

    private static Runtime instance;
    private static List<Pair<String, String>> pendingClassRegistrations = new ArrayList<>();
    private final ScheduledExecutorService probeReaderExecutor = Executors.newSingleThreadScheduledExecutor();
    private AgentCommandServer httpServer;
    private IErrorLogger errorLogger;
    /**
     * The logger receives method calls from injected instructions via selogger.logging.Logging class.
     */
    private IEventLogger logger = Logging.initialiseDiscardLogger();
    private long lastProbesLoadTime;

    private static HashMap<String, Integer> frequencyMap = new HashMap<>();

    /**
     * Process command line arguments and prepare an output directory
     *
     * @param args string arguments for weaver
     */
    private Runtime(String args) {
//        System.err.println("UnloggedInit1" );

        if ("true".equals(System.getProperty("UNLOGGED_DISABLE", "false"))) {
            logger = Logging.initialiseDiscardLogger();
            return;
        }

        if (System.getProperty("UNLOGGED_ARGS") != null) {
            args = System.getProperty("UNLOGGED_ARGS");
        }


        if (System.getenv("UNLOGGED_ARGS") != null) {
            args = System.getenv("UNLOGGED_ARGS");
        }

        try {
            // weave creation
            WeaveParameters weaveParameters = new WeaveParameters(args);
            String agentServerPort1 = weaveParameters.getAgentServerPort();
            if (agentServerPort1 == null || agentServerPort1.equalsIgnoreCase("null")) {
                agentServerPort1 = "0";
            }
            int agentServerPort = Integer.parseInt(agentServerPort1);

            String portFromEnv = System.getenv().get("UNLOGGED_AGENT_PORT");
            if (portFromEnv != null) {
                try {
                    agentServerPort = Integer.parseInt(portFromEnv);
                } catch (Exception e) {
                    //
                }
            }

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

            String hostname = NetworkClient.getHostname();

            ServerMetadata serverMetadata =
                    new ServerMetadata(weaveParameters.getIncludedNames().toString(), Constants.AGENT_VERSION, hostname,
                            0,
                            (weaveParameters.getServerAddress() == null || weaveParameters.getServerAddress()
                                    .isEmpty() ? "local" : "remote"));

            httpServer = new AgentCommandServer(agentServerPort, serverMetadata);

            StringBuilder firstLogLine = new StringBuilder();

            firstLogLine.append("Java version: " + System.getProperty("java.version") + "\n");
            firstLogLine.append("Agent version: " + Constants.AGENT_VERSION + "\n");
            firstLogLine.append("Params: " + args + "\n");

            httpServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            serverMetadata.setAgentServerUrl("http://localhost:" + httpServer.getListeningPort());
            serverMetadata.setAgentServerPort(httpServer.getListeningPort());

            firstLogLine.append(serverMetadata + "\n");
            errorLogger.log(firstLogLine.toString());

            System.out.println("[unlogged]" + " session Id: [" + config.getSessionId() + "] " + serverMetadata);

            switch (weaveParameters.getMode()) {


                case DISCARD:
                    logger = Logging.initialiseDiscardLogger();
                    break;

                case PER_THREAD:

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

                case TESTING:

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
                            outputDir);

                    DetailedEventStreamAggregatedLogger detailedLogger = (DetailedEventStreamAggregatedLogger) logger;
                    break;

            }


            httpServer.setAgentCommandExecutor(new AgentCommandExecutorImpl(
                    ObjectMapperFactory.createObjectMapperReactive(), logger));

            java.lang.Runtime.getRuntime()
                    .addShutdownHook(new Thread(this::close));


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
            for (Pair<String, String> pendingClassRegistration : pendingClassRegistrations) {
                registerClass(pendingClassRegistration.getFirst(), pendingClassRegistration.getSecond());
            }

        }
        return instance;
    }

    // this method is called by all classes which were probed during compilation time
    public static void registerClass(String classInfoBytes, String probesToRecordBase64) {
//        System.out.println(
//                "New class registration [" + classInfoBytes.getBytes().length + "][" + probesToRecordBase64.getBytes().length + "]");
        StackTraceElement callerClassAndMethodStack = new Exception().getStackTrace()[1];
        try {
            Class<?> callerClass = Class.forName(callerClassAndMethodStack.getClassName());
            String args = "";
            for (Method method : callerClass.getMethods()) {
                if (method.isAnnotationPresent(Unlogged.class)) {
                    Unlogged annotationData = method.getAnnotation(Unlogged.class);
                    if (annotationData.enable()) {
                        String includedPackageName = annotationData.includePackage()[0];
                        if (includedPackageName == null || includedPackageName.isEmpty()) {
                            includedPackageName = callerClassAndMethodStack.getClassName();
                            if (includedPackageName.contains(".")) {
                                includedPackageName = includedPackageName.substring(0,
                                        includedPackageName.lastIndexOf("."));
                            }
                        }
                        args =
                                "i=" + includedPackageName +
                                        (annotationData.serverEndpoint() == null ? "" : ",server=" + annotationData.serverEndpoint()) +
                                        (",agentserverport=" + annotationData.port());
                    } else {
                        args = "format=discard";
                    }
                    break;
                }
            }

            if (args.isEmpty()) {
                args = "i=" + callerClass.getPackage().getName();
            }
            getInstance(args);
        } catch (ClassNotFoundException e) {
//            throw new RuntimeException(e);
        }
        if (instance != null) {

            byte[] decodedClassWeaveInfo = new byte[0];
            List<Integer> probesToRecord = null;
            try {
                decodedClassWeaveInfo = ByteTools.decompressBase64String(classInfoBytes);
                byte[] decodedProbesToRecord = ByteTools.decompressBase64String(probesToRecordBase64);
                probesToRecord = bytesToIntList(decodedProbesToRecord);
            } catch (IOException e) {
                // class registration fails
                // no recoding for this class
                System.out.println("Registration for class failed: " + e.getMessage());
                return;
            }
            ClassInfo classInfo = new ClassInfo();

            try {
                ByteArrayInputStream in = new ByteArrayInputStream(decodedClassWeaveInfo);
                classInfo.readFromDataStream(in);
            } catch (IOException e) {
                return;
            }
//            System.out.println("Register class ["+ classInfo.getClassId() +"][" + classInfo.getClassName() + "] => " + probesToRecord.size() +
//                    " probes to record");
            instance.logger.recordWeaveInfo(decodedClassWeaveInfo, classInfo, probesToRecord);
        } else {
//            System.out.println("Adding class to pending registrations");
            pendingClassRegistrations.add(new Pair<>(classInfoBytes, probesToRecordBase64));
        }
    }


    public static List<Integer> bytesToIntList(byte[] probeToRecordBytes) throws IOException {
        List<Integer> probesToRecord = new ArrayList<>();
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

    private List<Integer> probeFileToIdList(File file) throws IOException {
        InputStream probesFile = this.getClass().getClassLoader().getResourceAsStream(file.getName());
        return probeFileStreamToIdList(probesFile);
    }

    private List<Integer> probeFileStreamToIdList(InputStream probesFile) throws IOException {
        if (probesFile == null) {
            return new ArrayList<>();
        }
        byte[] probeToRecordBytes = StreamUtil.streamToBytes(probesFile);
        return bytesToIntList(probeToRecordBytes);
    }

	private static boolean frequencyLogging (long methodCounter, long divisor) {
		if ((methodCounter-1) % divisor == 0){
			return true;
		}
		else {
			return false;
		}
	}

	public static boolean probeCounter(long methodCounter, long divisor, Object... arguments) {
		// This method is not called ATM. It will be used in arg based selective logging. 
		for (Object localArgument : arguments) {
			System.out.println(localArgument);
		}
		
		return frequencyLogging(methodCounter, divisor);
	}
	

	public static boolean probeCounter(long methodCounter, long divisor) {
		return frequencyLogging(methodCounter, divisor);
	}

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

    public enum Mode {STREAM, FREQUENCY, FIXED_SIZE, DISCARD, NETWORK, PER_THREAD, TESTING}

}
