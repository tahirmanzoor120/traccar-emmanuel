package org.traccar.model;

import org.traccar.storage.StorageName;

import java.util.Date;

@StorageName("tc_inmates")
public class Inmate extends ExtendedModel {
    private String firstName;
    private String lastName;
    private String dniIdentification;
    private Date dateOfBirth;
    private Date dateOfAdmission;
    private String reasonForAdmission;
    private String caseNumber;
    private String sentenceDuration;
    private String pavilion;
    private String cell;
    private boolean highRisk;
    private boolean requiresMedicalAttention;
    private boolean isolation;
    private String observations;

    public Inmate() {}

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDniIdentification() {
        return dniIdentification;
    }

    public void setDniIdentification(String dniIdentification) {
        this.dniIdentification = dniIdentification;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Date getDateOfAdmission() {
        return dateOfAdmission;
    }

    public void setDateOfAdmission(Date dateOfAdmission) {
        this.dateOfAdmission = dateOfAdmission;
    }

    public String getReasonForAdmission() {
        return reasonForAdmission;
    }

    public void setReasonForAdmission(String reasonForAdmission) {
        this.reasonForAdmission = reasonForAdmission;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public String getSentenceDuration() {
        return sentenceDuration;
    }

    public void setSentenceDuration(String sentenceDuration) {
        this.sentenceDuration = sentenceDuration;
    }

    public String getPavilion() {
        return pavilion;
    }

    public void setPavilion(String pavilion) {
        this.pavilion = pavilion;
    }

    public String getCell() {
        return cell;
    }

    public void setCell(String cell) {
        this.cell = cell;
    }

    public boolean getHighRisk() {
        return highRisk;
    }

    public void setHighRisk(boolean highRisk) {
        this.highRisk = highRisk;
    }

    public boolean getRequiresMedicalAttention() {
        return requiresMedicalAttention;
    }

    public void setRequiresMedicalAttention(boolean requiresMedicalAttention) {
        this.requiresMedicalAttention = requiresMedicalAttention;
    }

    public boolean getIsolation() {
        return isolation;
    }

    public void setIsolation(boolean isolation) {
        this.isolation = isolation;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }
}
