package io.unlogged.atomic;

public class StoredCandidateMetadata {
    private String recordedBy;
    private String hostMachineName;
    private long timestamp;
    private CandidateStatus candidateStatus = CandidateStatus.NA;
    public StoredCandidateMetadata(String recordedBy, String hostMachineName, long timestamp, CandidateStatus candidateStatus) {
        this.recordedBy = recordedBy;
        this.hostMachineName = hostMachineName;
        this.timestamp = timestamp;
        this.candidateStatus = candidateStatus;
    }
    public StoredCandidateMetadata(String recordedBy, String hostMachineName, long timestamp) {
        this.recordedBy = recordedBy;
        this.hostMachineName = hostMachineName;
        this.timestamp = timestamp;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(String recordedBy) {
        this.recordedBy = recordedBy;
    }

    public String getHostMachineName() {
        return hostMachineName;
    }

    public void setHostMachineName(String hostMachineName) {
        this.hostMachineName = hostMachineName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public StoredCandidateMetadata() {
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public CandidateStatus getCandidateStatus() {
        return candidateStatus;
    }

    public void setCandidateStatus(CandidateStatus candidateStatus) {
        this.candidateStatus = candidateStatus;
    }

    @Override
    public String toString() {
        return "StoredCandidateMetadata{" +
                "recordedBy='" + recordedBy + '\'' +
                ", hostMachineName='" + hostMachineName + '\'' +
                ", timestamp=" + timestamp +
                ", candidateStatus=" + candidateStatus +
                '}';
    }

    public enum CandidateStatus {NA, PASSING, FAILING}
}
