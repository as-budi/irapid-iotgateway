package com.example.irapid_gateway;

import com.kontakt.sdk.android.common.Proximity;

import java.util.UUID;

public class Beacon {
    private long timestamp;
    private String bleAddress;
    private double distance;
    private Proximity proximity;
//    private UUID proximityUUID;
    private int rssi;
    private int txPower;
    private int isRead;

    public Beacon (long timestamp, String bleAddress, double distance, Proximity proximity, int rssi, int txPower, int isRead) {
        this.timestamp = timestamp;
        this.bleAddress = bleAddress;
        this.distance = distance;
        this.proximity = proximity;
//        this.proximityUUID = proximityUUID;
        this.rssi = rssi;
        this.txPower = txPower;
        this.isRead = isRead;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getBleAddress() {
        return bleAddress;
    }

    public double getDistance() {
        return distance;
    }

    public Proximity getProximity() {
        return proximity;
    }

//    public UUID getProximityUUID() {
//        return proximityUUID;
//    }

    public int getRssi() {
        return rssi;
    }

    public int getTxPower() {
        return txPower;
    }

    public int getIsRead() { return isRead; }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setBleAddress(String bleAddress) {
        this.bleAddress = bleAddress;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public void setProximity(Proximity proximity) {
        this.proximity = proximity;
    }

//    public void setProximityUUID(UUID proximityUUID) {
//        this.proximityUUID = proximityUUID;
//    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public void setTxPower(int txPower) {
        this.txPower = txPower;
    }

    public void setIsRead(int isRead) {
        this.isRead = isRead;
    }
}
