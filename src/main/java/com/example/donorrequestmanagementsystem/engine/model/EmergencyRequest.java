package com.example.donorrequestmanagementsystem.engine.model;

import java.util.Objects;

public class EmergencyRequest {

    public enum UrgencyLevel {
        LOW(1), MEDIUM(2), CRITICAL(3);
        private final int weight;
        UrgencyLevel(int weight) { this.weight = weight; }
        public int getWeight() { return weight; }
    }

    private String requestId;
    private String hospitalName;
    private Donor.BloodType requiredBloodType;
    private String hospitalNodeId;
    private UrgencyLevel urgency;
    private final long timestamp;

    public EmergencyRequest(String requestId, String hospitalName, Donor.BloodType requiredBloodType,
                            String hospitalNodeId, UrgencyLevel urgency) {
        this.requestId = Objects.requireNonNull(requestId, "Request ID cannot be null.").trim();
        this.hospitalName = Objects.requireNonNull(hospitalName, "Hospital name cannot be null.").trim();
        this.requiredBloodType = Objects.requireNonNull(requiredBloodType, "Required blood type cannot be null.");
        this.hospitalNodeId = Objects.requireNonNull(hospitalNodeId, "Hospital Node ID cannot be null.").trim();
        this.urgency = Objects.requireNonNull(urgency, "Urgency level cannot be null.");

        if (this.requestId.isEmpty()) throw new IllegalArgumentException("Request ID cannot be empty.");
        if (this.hospitalName.isEmpty()) throw new IllegalArgumentException("Hospital name cannot be empty.");
        if (this.hospitalNodeId.isEmpty()) throw new IllegalArgumentException("Hospital Node ID cannot be empty.");

        this.timestamp = System.currentTimeMillis();
    }

    public String getRequestId() { return requestId; }
    public String getHospitalName() { return hospitalName; }
    public Donor.BloodType getRequiredBloodType() { return requiredBloodType; }
    public String getHospitalNodeId() { return hospitalNodeId; }
    public UrgencyLevel getUrgency() { return urgency; }
    public long getTimestamp() { return timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmergencyRequest that = (EmergencyRequest) o;
        return requestId.equals(that.requestId);
    }

    @Override
    public int hashCode() { return Objects.hash(requestId); }

    @Override
    public String toString() {
        return String.format("EmergencyRequest[ID='%s', Hospital='%s', Blood='%s', Node='%s', Urgency=%s, Time=%d]",
                requestId, hospitalName, requiredBloodType.getLabel(), hospitalNodeId, urgency.name(), timestamp);
    }
}