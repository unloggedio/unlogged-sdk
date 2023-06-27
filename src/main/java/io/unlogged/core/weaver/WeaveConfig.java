package io.unlogged.core.weaver;

import com.insidious.common.weaver.LogLevel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * This object manages options passed to the weaver.
 * This configuration controls the entire weaving process.
 */
public class WeaveConfig {

    public static final String KEY_RECORD_DEFAULT = "";
    public static final String KEY_RECORD_ALL = "ALL";
    public static final String KEY_RECORD_DEFAULT_PLUS_LOCAL = "EXEC+CALL+FIELD+ARRAY+SYNC+OBJECT+PARAM+LOCAL";
    public static final String KEY_RECORD_REPLAY = "EXEC+CALL+FIELD+ARRAY+SYNC+OBJECT+PARAM+LOCAL";
    public static final String KEY_RECORD_NONE = "NONE";
    public static final String KEY_RECORD_EXEC = "EXEC";
    public static final String KEY_RECORD_CALL = "CALL";
    public static final String KEY_RECORD_FIELD = "FIELD";
    public static final String KEY_RECORD_ARRAY = "ARRAY";
    public static final String KEY_RECORD_SYNC = "SYNC";
    public static final String KEY_RECORD_OBJECT = "OBJECT";
    public static final String KEY_RECORD_LABEL = "LABEL";
    public static final String KEY_RECORD_PARAMETERS = "PARAM";
    public static final String KEY_RECORD_LOCAL = "LOCAL";
    public static final String KEY_RECORD_LINE = "LINE";
    private static final String KEY_RECORD = "Events";
    private static final String KEY_RECORD_SEPARATOR = ",";
    private Integer processId;
    private String sessionId;
    private boolean weaveExec = true;
    private boolean weaveMethodCall = true;
    private boolean weaveFieldAccess = true;
    private boolean weaveArray = true;
    private boolean weaveLabel = true;
    private boolean weaveSynchronization = true;
    private boolean weaveParameters = true;
    private boolean weaveLocalAccess = true;
    private String username;
    private String password;
    private boolean weaveObject = true;
    private boolean weaveLineNumber = true;
    private boolean ignoreArrayInitializer = false;
    private boolean weaveNone = false;
    private String authToken;

//    /**
//     * Construct a configuration from string
//     *
//     * @param options       specify a string including: EXEC, CALL, FIELD, ARRAY, SYNC, OBJECT, LABEL, PARAM, LOCAL, and NONE.
//     * @param username
//     * @param password
//     */
//    public WeaveConfig(String options, String username, String password) {
//        String opt = options.toUpperCase();
////        System.out.printf("[unlogged] Recording option: [%s] Server Address [%s] Username [%s] Password [%s]\n", opt, serverAddress, username, password);
//        if (opt.equals(KEY_RECORD_ALL)) {
//            opt = KEY_RECORD_EXEC + KEY_RECORD_CALL + KEY_RECORD_FIELD + KEY_RECORD_ARRAY + KEY_RECORD_SYNC + KEY_RECORD_OBJECT + KEY_RECORD_PARAMETERS + KEY_RECORD_LABEL + KEY_RECORD_LOCAL + KEY_RECORD_LINE;
//        } else if (opt.equals(KEY_RECORD_DEFAULT)) {
//            opt = KEY_RECORD_EXEC + KEY_RECORD_CALL + KEY_RECORD_FIELD + KEY_RECORD_ARRAY + KEY_RECORD_SYNC + KEY_RECORD_OBJECT + KEY_RECORD_PARAMETERS;
//        } else if (opt.equals(KEY_RECORD_NONE)) {
//            opt = "";
//            weaveNone = true;
//        }
//        weaveExec = opt.contains(KEY_RECORD_EXEC);
//        weaveMethodCall = opt.contains(KEY_RECORD_CALL);
//        weaveFieldAccess = opt.contains(KEY_RECORD_FIELD);
//        weaveArray = opt.contains(KEY_RECORD_ARRAY);
//        weaveSynchronization = opt.contains(KEY_RECORD_SYNC);
//        weaveLabel = opt.contains(KEY_RECORD_LABEL);
//        weaveParameters = opt.contains(KEY_RECORD_PARAMETERS);
//        weaveLocalAccess = opt.contains(KEY_RECORD_LOCAL);
//        weaveObject = opt.contains(KEY_RECORD_OBJECT);
//        weaveLineNumber = opt.contains(KEY_RECORD_LINE);
//        ignoreArrayInitializer = false;
//        this.username = username;
//        this.password = password;
//
//        this.sessionId = UUID.randomUUID().toString();
//        this.processId = getProcessId(new Random().nextInt());
//
//    }


    /**
     * A copy constructor with a constraint.
     *
     * @param parent weave config to copy
     * @param level probe logging level
     */
    public WeaveConfig(WeaveConfig parent, LogLevel level) {
        this.weaveExec = parent.weaveExec;
        this.weaveMethodCall = parent.weaveMethodCall;
        this.weaveFieldAccess = parent.weaveFieldAccess;
        this.weaveArray = parent.weaveArray;
        this.weaveSynchronization = parent.weaveSynchronization;
        this.weaveLabel = parent.weaveLabel;
        this.weaveParameters = parent.weaveParameters;
        this.weaveLocalAccess = parent.weaveLocalAccess;
        this.weaveLineNumber = parent.weaveLineNumber;
        this.ignoreArrayInitializer = parent.ignoreArrayInitializer;
        this.weaveNone = parent.weaveNone;
        if (level == LogLevel.IgnoreArrayInitializer) {
            this.ignoreArrayInitializer = true;
        } else if (level == LogLevel.OnlyEntryExit) {
            this.weaveMethodCall = false;
            this.weaveFieldAccess = false;
            this.weaveArray = false;
            this.weaveSynchronization = false;
            this.weaveLabel = false;
            this.weaveParameters = false;
            this.weaveLocalAccess = false;
            this.weaveObject = false;
            this.weaveLineNumber = false;
        }
    }

    public WeaveConfig(WeaveParameters weaveParameters) {
        String options = weaveParameters.getWeaveOption();
        String opt = options.toUpperCase();
//        System.out.printf("[unlogged] Recording option: [%s] Server Address [%s] Username [%s] Password [%s]\n", opt, params.getServerAddress(), params.getUsername(), params.getPassword());
        if (opt.equals(KEY_RECORD_ALL)) {
            opt = KEY_RECORD_EXEC + KEY_RECORD_CALL + KEY_RECORD_FIELD + KEY_RECORD_ARRAY + KEY_RECORD_SYNC + KEY_RECORD_OBJECT + KEY_RECORD_PARAMETERS + KEY_RECORD_LOCAL;
        } else if (opt.equals(KEY_RECORD_DEFAULT)) {
            opt = KEY_RECORD_EXEC + KEY_RECORD_CALL + KEY_RECORD_FIELD + KEY_RECORD_ARRAY + KEY_RECORD_SYNC + KEY_RECORD_OBJECT + KEY_RECORD_PARAMETERS;
        } else if (opt.equals(KEY_RECORD_NONE)) {
            opt = "";
            weaveNone = true;
        }

        weaveExec = opt.contains(KEY_RECORD_EXEC);
        weaveMethodCall = opt.contains(KEY_RECORD_CALL);
        weaveFieldAccess = opt.contains(KEY_RECORD_FIELD);
        weaveArray = opt.contains(KEY_RECORD_ARRAY);
        weaveSynchronization = opt.contains(KEY_RECORD_SYNC);
        weaveLabel = opt.contains(KEY_RECORD_LABEL);
        this.weaveParameters = opt.contains(KEY_RECORD_PARAMETERS);
        weaveLocalAccess = opt.contains(KEY_RECORD_LOCAL);
        weaveObject = opt.contains(KEY_RECORD_OBJECT);
        weaveLineNumber = opt.contains(KEY_RECORD_LINE);
        ignoreArrayInitializer = false;
        this.username = weaveParameters.getUsername();
        this.password = weaveParameters.getPassword();

        this.sessionId = UUID.randomUUID().toString();
//        this.processId = getProcessId(new Random().nextInt());
    }

    private static Integer getProcessId(final Integer fallback) {
        // Note: may fail in some JVM implementations
        // therefore fallback has to be provided

        // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
        final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        final int index = jvmName.indexOf('@');

        if (index < 1) {
            // part before '@' empty (index = 0) / '@' not found (index = -1)
            return fallback;
        }

        try {
            return Integer.parseInt(jvmName.substring(0, index));
        } catch (NumberFormatException e) {
            // ignore
        }
        return fallback;
    }

    /**
     * @return true if the weaver is configured to record some events or
     * explicitly configured to record no events.
     */
    public boolean isValid() {
        return weaveNone || weaveExec || weaveMethodCall || weaveFieldAccess || weaveArray || weaveSynchronization || weaveParameters || weaveLocalAccess || weaveLabel || weaveLineNumber;
    }

    /**
     * @return true if the weaver should record method execution events
     * such as ENTRY and EXIT observed in the callee side.
     */
    public boolean recordExecution() {
        return weaveExec;
    }

    /**
     * @return true if the weaver should record synchronized block events
     */
    public boolean recordSynchronization() {
        return weaveSynchronization;
    }

    /**
     * @return true if the weaver should record field access events
     */
    public boolean recordFieldAccess() {
        return weaveFieldAccess;
    }

    /**
     * @return true if the weaver should record method execution events
     * such as CALL observed in the caller side.
     */
    public boolean recordMethodCall() {
        return weaveMethodCall;
    }

    /**
     * @return true if the weaver should record array manipulation events.
     */
    public boolean recordArrayInstructions() {
        return weaveArray;
    }

    /**
     * @return true if the weaver should record LABEL (control-flow) events.
     */
    public boolean recordLabel() {
        return weaveLabel;
    }

    /**
     * @return true if the weaver should record method parameters.
     */
    public boolean recordParameters() {
        return weaveParameters;
    }

    /**
     * @return true if the weaver should record local access events.
     */
    public boolean recordLocalAccess() {
        return weaveLocalAccess;
    }

    /**
     * @return true if the weaver should record line number events.
     */
    public boolean recordLineNumber() {
        return weaveLineNumber;
    }

    /**
     * @return true if the weaving should ignore array initializers
     * (due to the size of the target class file).
     */
    public boolean ignoreArrayInitializer() {
        return ignoreArrayInitializer;
    }

    /**
     * @return true if the weaver should record CATCH events.
     */
    public boolean recordCatch() {
        return recordMethodCall() ||
                recordFieldAccess() ||
                recordArrayInstructions() ||
                recordLabel() ||
                recordSynchronization();
    }

    /**
     * @return true if the weaver should record OBJECT events.
     */
    public boolean recordObject() {
        return weaveObject;
    }


    /**
     * Save the weaving configuration to a file.
     * @param propertyFile destination file to save the config
     */
    public void save(File propertyFile) {
        ArrayList<String> events = new ArrayList<String>();
        if (weaveExec) events.add(KEY_RECORD_EXEC);
        if (weaveMethodCall) events.add(KEY_RECORD_CALL);
        if (weaveFieldAccess) events.add(KEY_RECORD_FIELD);
        if (weaveArray) events.add(KEY_RECORD_ARRAY);
        if (weaveSynchronization) events.add(KEY_RECORD_SYNC);
        if (weaveLabel) events.add(KEY_RECORD_LABEL);
        if (weaveParameters) events.add(KEY_RECORD_PARAMETERS);
        if (weaveLocalAccess) events.add(KEY_RECORD_LOCAL);
        if (weaveObject) events.add(KEY_RECORD_OBJECT);
        if (weaveLineNumber) events.add(KEY_RECORD_LINE);
        if (weaveNone) events.add(KEY_RECORD_NONE);
        StringBuilder eventsString = new StringBuilder();
        for (int i = 0; i < events.size(); ++i) {
            if (i > 0) eventsString.append(KEY_RECORD_SEPARATOR);
            eventsString.append(events.get(i));
        }

        Properties prop = new Properties();
        prop.setProperty(KEY_RECORD, eventsString.toString());

        try {
            FileOutputStream out = new FileOutputStream(propertyFile);
            prop.store(out, "Generated: " + new Date());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getSessionId() {
        return sessionId;
    }
}
