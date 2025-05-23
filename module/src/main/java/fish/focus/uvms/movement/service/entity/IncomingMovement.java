package fish.focus.uvms.movement.service.entity;

import fish.focus.schema.movement.v1.MovementTypeType;
import fish.focus.uvms.movement.service.entity.alarm.AlarmReport;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incomingmovement")
public class IncomingMovement {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", name = "id")
    private UUID id;

    private String assetHistoryId;
    private String ackResponseMessageId;

    private Instant dateReceived;

    private Instant positionTime;

    private Instant lesReportTime;

    private String status;
    private Double reportedSpeed;
    private Double reportedCourse;
    private String assetName;
    private String flagState;
    private String externalMarking;

    private String movementType = MovementTypeType.POS.value();
    private String movementSourceType;

    private String assetType;
    private String assetID;
    private String assetCFR;
    private String assetIRCS;
    private String assetIMO;
    private String assetMMSI;
    private String assetGuid;

    private Double longitude;
    private Double latitude;
    private Double altitude;

    private String activityMessageType;
    private String activityMessageId;
    private String activityCallback;

    private String comChannelType;

    private String mobileTerminalGuid;
    private String mobileTerminalConnectId;

    private String mobileTerminalSerialNumber;
    private String mobileTerminalLES;
    private String mobileTerminalDNID;
    private String mobileTerminalMemberNumber;
    @Column(name = "active")
    private boolean mobileTerminalActive;
    private Short sourceSatelliteId;

    private String pluginType;
    private boolean duplicate = false;

    @NotNull
    private Instant updated;
    @NotNull
    private String updatedBy;

    @Column(name = "ais_position_accuracy")
    //Value can be 0 (>10m) and 1 (<10m). See https://gpsd.gitlab.io/gpsd/AIVDM.html for more info
    private Short aisPositionAccuracy;

    @JsonbTransient
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    //DB is set to allow null values here since, for some reason, hibernate passes a null that is later changed into the correct value.
    private AlarmReport alarmReport;


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAssetHistoryId() {
        return assetHistoryId;
    }

    public void setAssetHistoryId(String assetHistoryId) {
        this.assetHistoryId = assetHistoryId;
    }

    public String getAckResponseMessageId() {
        return ackResponseMessageId;
    }

    public void setAckResponseMessageId(String ackResponseMessageId) {
        this.ackResponseMessageId = ackResponseMessageId;
    }

    public Instant getDateReceived() {
        return dateReceived;
    }

    public void setDateReceived(Instant dateReceived) {
        this.dateReceived = dateReceived;
    }

    public Instant getPositionTime() {
        return positionTime;
    }

    public void setPositionTime(Instant positionTime) {
        this.positionTime = positionTime;
    }

    public Instant getLesReportTime() {
        return lesReportTime;
    }

    public void setLesReportTime(Instant lesReportTime) {
        this.lesReportTime = lesReportTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getReportedSpeed() {
        return reportedSpeed;
    }

    public void setReportedSpeed(Double reportedSpeed) {
        this.reportedSpeed = reportedSpeed;
    }

    public Double getReportedCourse() {
        return reportedCourse;
    }

    public void setReportedCourse(Double reportedCourse) {
        this.reportedCourse = reportedCourse;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getFlagState() {
        return flagState;
    }

    public void setFlagState(String flagState) {
        this.flagState = flagState;
    }

    public String getExternalMarking() {
        return externalMarking;
    }

    public void setExternalMarking(String externalMarking) {
        this.externalMarking = externalMarking;
    }

    public String getMovementType() {
        return movementType;
    }

    public void setMovementType(String movementType) {
        this.movementType = movementType;
    }

    public String getMovementSourceType() {
        return movementSourceType;
    }

    public void setMovementSourceType(String movementSourceType) {
        this.movementSourceType = movementSourceType;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getAssetID() {
        return assetID;
    }

    public void setAssetID(String assetID) {
        this.assetID = assetID;
    }

    public String getAssetCFR() {
        return assetCFR;
    }

    public void setAssetCFR(String assetCFR) {
        this.assetCFR = assetCFR;
    }

    public String getAssetIRCS() {
        return assetIRCS;
    }

    public void setAssetIRCS(String assetIRCS) {
        this.assetIRCS = assetIRCS;
    }

    public String getAssetIMO() {
        return assetIMO;
    }

    public void setAssetIMO(String assetIMO) {
        this.assetIMO = assetIMO;
    }

    public String getAssetMMSI() {
        return assetMMSI;
    }

    public void setAssetMMSI(String assetMMSI) {
        this.assetMMSI = assetMMSI;
    }

    public String getAssetGuid() {
        return assetGuid;
    }

    public void setAssetGuid(String assetGuid) {
        this.assetGuid = assetGuid;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public String getActivityMessageType() {
        return activityMessageType;
    }

    public void setActivityMessageType(String activityMessageType) {
        this.activityMessageType = activityMessageType;
    }

    public String getActivityMessageId() {
        return activityMessageId;
    }

    public void setActivityMessageId(String activityMessageId) {
        this.activityMessageId = activityMessageId;
    }

    public String getActivityCallback() {
        return activityCallback;
    }

    public void setActivityCallback(String activityCallback) {
        this.activityCallback = activityCallback;
    }

    public String getComChannelType() {
        return comChannelType;
    }

    public void setComChannelType(String comChannelType) {
        this.comChannelType = comChannelType;
    }

    public String getMobileTerminalGuid() {
        return mobileTerminalGuid;
    }

    public void setMobileTerminalGuid(String mobileTerminalGuid) {
        this.mobileTerminalGuid = mobileTerminalGuid;
    }

    public String getMobileTerminalConnectId() {
        return mobileTerminalConnectId;
    }

    public void setMobileTerminalConnectId(String mobileTerminalConnectId) {
        this.mobileTerminalConnectId = mobileTerminalConnectId;
    }

    public String getMobileTerminalSerialNumber() {
        return mobileTerminalSerialNumber;
    }

    public void setMobileTerminalSerialNumber(String mobileTerminalSerialNumber) {
        this.mobileTerminalSerialNumber = mobileTerminalSerialNumber;
    }

    public String getMobileTerminalLES() {
        return mobileTerminalLES;
    }

    public void setMobileTerminalLES(String mobileTerminalLES) {
        this.mobileTerminalLES = mobileTerminalLES;
    }

    public String getMobileTerminalDNID() {
        return mobileTerminalDNID;
    }

    public void setMobileTerminalDNID(String mobileTerminalDNID) {
        this.mobileTerminalDNID = mobileTerminalDNID;
    }

    public String getMobileTerminalMemberNumber() {
        return mobileTerminalMemberNumber;
    }

    public void setMobileTerminalMemberNumber(String mobileTerminalMemberNumber) {
        this.mobileTerminalMemberNumber = mobileTerminalMemberNumber;
    }

    public boolean isMobileTerminalActive() {
        return mobileTerminalActive;
    }

    public void setMobileTerminalActive(boolean mobileTerminalActive) {
        this.mobileTerminalActive = mobileTerminalActive;
    }

    public Short getSourceSatelliteId() {
        return sourceSatelliteId;
    }

    public void setSourceSatelliteId(Short sourceSatelliteId) {
        this.sourceSatelliteId = sourceSatelliteId;
    }

    public String getPluginType() {
        return pluginType;
    }

    public void setPluginType(String pluginType) {
        this.pluginType = pluginType;
    }

    public Instant getUpdated() {
        return updated;
    }

    public void setUpdated(Instant updated) {
        this.updated = updated;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public AlarmReport getAlarmReport() {
        return alarmReport;
    }

    public void setAlarmReport(AlarmReport alarmReport) {
        this.alarmReport = alarmReport;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public Short getAisPositionAccuracy() {
        return aisPositionAccuracy;
    }

    public void setAisPositionAccuracy(Short aisPositionAccuracy) {
        this.aisPositionAccuracy = aisPositionAccuracy;
    }
}
