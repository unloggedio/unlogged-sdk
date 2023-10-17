package io.unlogged.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.unlogged.atomic.MethodUnderTest;
import io.unlogged.atomic.StoredCandidate;
import io.unlogged.atomic.StoredCandidateMetadata;
import io.unlogged.mocking.DeclaredMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static java.io.File.separator;

public class AtomicRecordService {
    public static final String TEST_CONTENT_PATH = "src" + separator + "test" + separator;
    public static final String TEST_RESOURCES_PATH = TEST_CONTENT_PATH + "resources" + separator;
    private static final Logger logger = LoggerFactory.getLogger(AtomicRecordService.class);
    private final String UNLOGGED_RESOURCE_FOLDER_NAME = "unlogged";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, AtomicRecord> classAtomicRecordMap = null;

    public String getTestResourcesPath() {
        return TEST_RESOURCES_PATH + separator + UNLOGGED_RESOURCE_FOLDER_NAME;
    }


    public Map<String, List<StoredCandidate>> filterCandidates(Map<String, List<StoredCandidate>> candidates) {
        if (candidates == null || candidates.size() == 0) {
            return null;
        }

        for (String methodHashKey : candidates.keySet()) {
            Map<Integer, StoredCandidate> storedCandidateMap = new TreeMap<>();
            List<StoredCandidate> storedCandidateList = candidates.get(methodHashKey);

            for (StoredCandidate candidate : storedCandidateList) {
                if (candidate.getName() != null) {
                    int hash = candidate.getMethodArguments().hashCode() + (candidate.getMethod().getName().hashCode());
                    if (storedCandidateMap.containsKey(hash)) {
                        if (storedCandidateMap.get(hash).getMetadata().getTimestamp() <
                                candidate.getMetadata().getTimestamp()) {
                            storedCandidateMap.put(hash, candidate);
                        }
                    } else {
                        storedCandidateMap.put(hash, candidate);
                    }
                }
            }
            candidates.put(methodHashKey, new ArrayList<>(storedCandidateMap.values()));
        }
        return candidates;
    }


    public String getMessageForOperationType(FileUpdateType type, String path, boolean positive) {
        switch (type) {
            case ADD_CANDIDATE:
                if (positive) {
                    return "Added test candidate" + (path == null ? "" : " at: " + path);
                } else {
                    return "Failed to add test candidate" +
                            "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.";
                }
            case UPDATE_CANDIDATE:
                if (positive) {
                    return "Updated test candidate" + (path == null ? "" : " at: " + path);
                } else {
                    return "Failed to update test candidate" +
                            "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.";
                }
            case DELETE_CANDIDATE:
                if (positive) {
                    return "Deleted test candidate" + (path == null ? "" : " at: " + path);
                } else {
                    return "Failed to delete test candidate" +
                            "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.";
                }
            case ADD_MOCK:
                if (positive) {
                    return "Saved mock" + (path == null ? "" : " at: " + path);
                } else {
                    return "Failed to add test candidate" +
                            "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.";
                }
            case UPDATE_MOCK:
                if (positive) {
                    return "Updated mock" + (path == null ? "" : " at: " + path);
                } else {
                    return "Failed to update test candidate" +
                            "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.";
                }
            case DELETE_MOCK:
                if (positive) {
                    return "Deleted mock" + (path == null ? "" : " at: " + path);
                } else {
                    return "Failed to delete test candidate" +
                            "\n Need help ? \n<a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.";
                }
            default:
                throw new RuntimeException("Unknown operation: " + type);
        }
    }

    private List<File> getFilesInUnloggedFolder() {
        ArrayList<File> returnFileList = new ArrayList<>();

        File rootDir = new File(TEST_RESOURCES_PATH + UNLOGGED_RESOURCE_FOLDER_NAME);
        File[] files = rootDir.listFiles();
        if (files != null) {
            Collections.addAll(returnFileList, files);
        }

        return returnFileList;
    }


    //called once in start, when map is null, updated after that.
    public Map<String, AtomicRecord> updateMap() {
        Map<String, AtomicRecord> recordsMap = new TreeMap<>();
        List<File> files = getFilesInUnloggedFolder();
//        if (files.size() == 0) {
//            return new TreeMap<>();
//        }
        for (File file : files) {
            System.out.println("Load resource file: " + file.getPath());
            AtomicRecord record = getAtomicRecordFromFile(file);
            if (record != null) {
                String classname = record.getClassname();
                recordsMap.put(classname, record);
            }
        }
        return recordsMap;
    }

    public Boolean hasStoredCandidateForMethod(MethodUnderTest methodUnderTest) {
        try {
            AtomicRecord record = classAtomicRecordMap.get(methodUnderTest.getClassName());
            if (record == null) {
                return false;
            }
            String methodKey = methodUnderTest.getMethodHashKey();
            Map<String, List<StoredCandidate>> storedCandidateMap = record.getStoredCandidateMap();
            return storedCandidateMap.containsKey(methodKey) &&
                    storedCandidateMap.get(methodKey).size() > 0;
        } catch (Exception e) {
            logger.warn("Exception checking if method has stored candidates", e);
            return false;
        }
    }

    private AtomicRecord getAtomicRecordFromFile(File file) {
        try {
            if (file == null || !file.exists()) {
                return null;
            }
            InputStream inputStream = new FileInputStream(file);
            return objectMapper.readValue(inputStream, AtomicRecord.class);
        } catch (IOException e) {
            logger.error("Exception getting atomic record from file", e);
            return null;
        }
    }


    public List<StoredCandidate> getStoredCandidatesForMethod(MethodUnderTest methodUnderTest) {
        if (methodUnderTest.getClassName() == null) {
            return new ArrayList<>();
        }
        AtomicRecord record = classAtomicRecordMap.get(methodUnderTest.getClassName());
        if (record == null) {
            return new ArrayList<>();
        }
        return record.getStoredCandidateMap().getOrDefault(methodUnderTest.getMethodHashKey(), new ArrayList<>());
    }


    public void setCandidateStateForCandidate(String candidateID, String classname,
                                              String methodKey, StoredCandidateMetadata.CandidateStatus state) {
        AtomicRecord record = classAtomicRecordMap.get(classname);
        if (record == null
                || !record.getStoredCandidateMap().containsKey(methodKey)
                || record.getStoredCandidateMap().get(methodKey).size() == 0) {
            return;
        }
        List<StoredCandidate> list = record.getStoredCandidateMap().get(methodKey);
        for (StoredCandidate candidate : list) {
            if (candidateID.equals(candidate.getCandidateId())) {
                candidate.getMetadata().setCandidateStatus(state);
            }
        }
    }


    /**
     * Returns mocks of the class
     *
     * @param methodUnderTest target method
     * @return mocks which can be used in the class
     */
    public List<DeclaredMock> getDeclaredMocksOf(MethodUnderTest methodUnderTest) {

        if (!classAtomicRecordMap.containsKey(methodUnderTest.getClassName())) {
            return new ArrayList<>();
        }
        Map<String, List<DeclaredMock>> declaredMockMap = classAtomicRecordMap
                .get(methodUnderTest.getClassName())
                .getDeclaredMockMap();
        String methodHashKey = methodUnderTest.getMethodHashKey();
        if (!declaredMockMap.containsKey(methodHashKey)) {
            return new ArrayList<>();
        }
        return declaredMockMap.get(methodHashKey);


    }

    /**
     * returns mocks which can be used in the class
     *
     * @param methodUnderTest source method
     * @return mocks which can be used in the class
     */
    public List<DeclaredMock> getDeclaredMocksFor(MethodUnderTest methodUnderTest) {
        return classAtomicRecordMap.values()
                .stream().map(e -> e.getDeclaredMockMap().values())
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
                .filter(e -> e.getSourceClassName() != null && e.getSourceClassName()
                        .equals(methodUnderTest.getClassName()))
                .collect(Collectors.toList());

    }

    public List<DeclaredMock> getAllDeclaredMocks() {
        return classAtomicRecordMap.values()
                .stream().map(e -> e.getDeclaredMockMap().values())
                .flatMap(Collection::stream)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

    }

    public Map<String, List<StoredCandidate>> getCandidatesByClass(String fullyClassifiedClassName) {
        if (!classAtomicRecordMap.containsKey(fullyClassifiedClassName)) {
            return null;
        }
        return classAtomicRecordMap.get(fullyClassifiedClassName).getStoredCandidateMap();
    }

    public enum FileUpdateType {
        ADD_MOCK,
        UPDATE_MOCK,
        DELETE_MOCK,
        ADD_CANDIDATE,
        UPDATE_CANDIDATE,
        DELETE_CANDIDATE
    }
}
