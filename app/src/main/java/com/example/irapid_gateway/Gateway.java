package com.example.irapid_gateway;

public class Gateway {
    private String deviceID;

    public Gateway(String deviceID) {
        this.deviceID = deviceID;
    }

    public String getDeviceId() { return deviceID; }

    public void setDeviceId(String deviceID) {
        this.deviceID = deviceID;
    }
}