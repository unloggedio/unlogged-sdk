package io.unlogged;

import fi.iki.elonen.NanoHTTPD;
import io.unlogged.command.AgentCommandServer;
import io.unlogged.command.ServerMetadata;
import io.unlogged.logging.IErrorLogger;
import io.unlogged.logging.IEventLogger;
import io.unlogged.logging.Logging;
import io.unlogged.logging.perthread.PerThreadBinaryFileAggregatedLogger;
import io.unlogged.logging.perthread.RawFileCollector;
import io.unlogged.logging.util.FileNameGenerator;
import io.unlogged.logging.util.NetworkClient;
import io.unlogged.weaver.WeaveConfig;
import io.unlogged.weaver.WeaveParameters;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is the main program of SELogger as a javaagent.
 */
public class Runtime {

    public static final int AGENT_SERVER_PORT = 12100;
    private AgentCommandServer httpServer;
    private IErrorLogger errorLogger;
    /**
     * The logger receives method calls from injected instructions via selogger.logging.Logging class.
     */
    private IEventLogger logger;


    /**
     * Process command line arguments and prepare an output directory
     *
     * @param args string arguments for weaver
     */
    public Runtime(String args) {

        try {
            WeaveParameters weaveParameters = new WeaveParameters(args);

            File outputDir = new File(weaveParameters.getOutputDirname());
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            ServerMetadata serverMetadata =
                    new ServerMetadata(weaveParameters.getIncludedNames().toString(), Constants.AGENT_VERSION);


            if (!outputDir.isDirectory() || !outputDir.canWrite()) {
                System.err.println("[unlogged] ERROR: " + outputDir.getAbsolutePath() + " is not writable.");
                return;
            }

            WeaveConfig config = new WeaveConfig(weaveParameters);

            if (!config.isValid()) {
                System.out.println("[unlogged] no weaving option is specified.");
                return;
            }

            errorLogger = new IErrorLogger() {
                @Override
                public void log(Throwable throwable) {
                    throwable.printStackTrace();
                    System.out.println(throwable.getMessage());
                }

                @Override
                public void log(String message) {
                    System.out.println(message);

                }

                @Override
                public void close() {

                }
            };


            System.out.println("[unlogged]" +
                    " session Id: [" + config.getSessionId() + "]" +
                    " on hostname [" + NetworkClient.getHostname() + "]");

            switch (weaveParameters.getMode()) {

                case PerThread:

                    NetworkClient networkClient = new NetworkClient(weaveParameters.getServerAddress(),
                            config.getSessionId(), weaveParameters.getAuthToken(), errorLogger);

                    FileNameGenerator fileNameGenerator1 = new FileNameGenerator(outputDir, "index-", ".zip");
                    RawFileCollector fileCollector =
                            new RawFileCollector(weaveParameters.getFilesPerIndex(), fileNameGenerator1,
                                    networkClient,
                                    errorLogger, outputDir);

                    FileNameGenerator fileNameGenerator = new FileNameGenerator(outputDir, "log-", ".selog");
                    PerThreadBinaryFileAggregatedLogger perThreadBinaryFileAggregatedLogger
                            = new PerThreadBinaryFileAggregatedLogger(fileNameGenerator, errorLogger,
                            fileCollector);

                    logger = Logging.initialiseAggregatedLogger(errorLogger,
                            perThreadBinaryFileAggregatedLogger,
                            outputDir);
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

                    logger = Logging.initialiseDetailedAggregatedLogger(
                            weaveParameters.getIncludedNames().get(0), perThreadBinaryFileAggregatedLogger1,
                            outputDir);
                    break;

            }

            httpServer = new AgentCommandServer(AGENT_SERVER_PORT, serverMetadata);
            httpServer.setAgentCommandExecutor(new AgentCommandExecutorImpl(logger.getObjectMapper(), logger));
            httpServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);

            java.lang.Runtime.getRuntime()
                    .addShutdownHook(new Thread(() -> {
                        close();
                    }));


        } catch (Throwable thx) {
            thx.printStackTrace();
            System.err.println(
                    "[unlogged] agent init failed, this session will not be recorded => " + thx.getMessage());
        }
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

    public enum Mode {Stream, Frequency, FixedSize, Discard, Network, PerThread, Testing}

}
