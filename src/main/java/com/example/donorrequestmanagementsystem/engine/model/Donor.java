package com.example.donorrequestmanagementsystem.engine.model;

import java.util.Objects;
import java.util.regex.Pattern;

public class Donor {

    // Enforcing strict types for blood groups to prevent string mismatch bugs
    public enum BloodType {
        O_PLUS("O+"), O_MINUS("O-"),
        A_PLUS("A+"), A_MINUS("A-"),
        B_PLUS("B+"), B_MINUS("B-"),
        AB_PLUS("AB+"), AB_MINUS("AB-");

        private final String label;

        BloodType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    // RegEx pattern to validate standard 11-digit local mobile numbers (e.g., 03001234567)
    private static final Pattern PHONE_PATTERN = Pattern.compile("^03\\d{9}$");

    private final String id;
    private final String name;
    private final BloodType bloodType;
    private final String phoneNumber;
    private final String locationNodeId; // Reference to the unique ID of a Node in our Graph
    private boolean isAvailable;

    /**
     * Constructs a validated, secure Donor instance.
     * Throws IllegalArgumentException immediately if any invalid data tries to compromise the engine.
     */
    public Donor(String id, String name, BloodType bloodType, String phoneNumber, String locationNodeId) {
        this.id = Objects.requireNonNull(id, "Donor ID cannot be null.").trim();
        this.name = Objects.requireNonNull(name, "Donor name cannot be null.").trim();
        this.bloodType = Objects.requireNonNull(bloodType, "Blood type cannot be null.");
        this.locationNodeId = Objects.requireNonNull(locationNodeId, "Location Node ID cannot be null.").trim();

        // Fail-fast validation on core string inputs
        if (this.id.isEmpty()) throw new IllegalArgumentException("Donor ID cannot be empty.");
        if (this.name.isEmpty()) throw new IllegalArgumentException("Donor name cannot be empty.");
        if (this.locationNodeId.isEmpty()) throw new IllegalArgumentException("Location Node ID cannot be empty.");

        // Validate phone number format
        Objects.requireNonNull(phoneNumber, "Phone number cannot be null.");
        String cleanPhone = phoneNumber.trim();
        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            throw new IllegalArgumentException("Invalid phone number format. Must be an 11-digit number starting with 03.");
        }
        this.phoneNumber = cleanPhone;

        this.isAvailable = true; // Default state on registration
    }

    // --- Getters & Setters ---

    public String getId() { return id; }
    public String getName() { return name; }
    public BloodType getBloodType() { return bloodType; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getLocationNodeId() { return locationNodeId; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { this.isAvailable = available; }

    // --- Identity Methods ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Donor donor = (Donor) o;
        return id.equals(donor.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Donor[ID='%s', Name='%s', Blood='%s', Phone='%s', Node='%s', Available=%b]",
                id, name, bloodType.getLabel(), phoneNumber, locationNodeId, isAvailable);
    }
}