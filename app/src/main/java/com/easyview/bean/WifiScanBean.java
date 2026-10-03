package com.easyview.bean;

import java.io.Serializable;

/* loaded from: classes.dex */
public class WifiScanBean implements Serializable {
    private static final long serialVersionUID = 1;
    private int channel;
    private int dbm0;
    private int dbm1;
    private String did;
    private String mac;
    private int mode;
    private int security;
    private String ssid;

    public String getDid() {
        return this.did;
    }

    public void setDid(String did) {
        this.did = did;
    }

    public String getSsid() {
        return this.ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getMac() {
        return this.mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public int getSecurity() {
        return this.security;
    }

    public void setSecurity(int security) {
        this.security = security;
    }

    public int getDbm0() {
        return this.dbm0;
    }

    public void setDbm0(int dbm0) {
        this.dbm0 = dbm0;
    }

    public int getDbm1() {
        return this.dbm1;
    }

    public void setDbm1(int dbm1) {
        this.dbm1 = dbm1;
    }

    public int getMode() {
        return this.mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getChannel() {
        return this.channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public String toString() {
        return "WifiScanBean [did=" + this.did + ", ssid=" + this.ssid + ", mac=" + this.mac + ", security=" + this.security + ", dbm0=" + this.dbm0 + ", dbm1=" + this.dbm1 + ", mode=" + this.mode + ", channel=" + this.channel + "]";
    }
}
