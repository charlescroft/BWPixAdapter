package com.easyview.bean;

import java.io.Serializable;

/* loaded from: classes.dex */
public class WifiBean implements Serializable {
    private static final long serialVersionUID = 1;
    private int authtype;
    private int channel;
    private int dbm0;
    private int defkey;
    private int enable;
    private int encryp;
    private int key1_bits;
    private int key2_bits;
    private int key3_bits;
    private int key4_bits;
    private int keyformat;
    private int mode;
    private String did = "";
    private String ssid = "";
    private String key1 = "";
    private String key2 = "";
    private String key3 = "";
    private String key4 = "";
    private String wpa_psk = "";

    public int getDbm0() {
        return this.dbm0;
    }

    public void setDbm0(int dbm0) {
        this.dbm0 = dbm0;
    }

    public String getDid() {
        return this.did;
    }

    public void setDid(String did) {
        this.did = did;
    }

    public int getEnable() {
        return this.enable;
    }

    public void setEnable(int enable) {
        this.enable = enable;
    }

    public String getSsid() {
        return this.ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public int getChannel() {
        return this.channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getMode() {
        return this.mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getAuthtype() {
        return this.authtype;
    }

    public void setAuthtype(int authtype) {
        this.authtype = authtype;
    }

    public int getEncryp() {
        return this.encryp;
    }

    public void setEncryp(int encryp) {
        this.encryp = encryp;
    }

    public int getKeyformat() {
        return this.keyformat;
    }

    public void setKeyformat(int keyformat) {
        this.keyformat = keyformat;
    }

    public int getDefkey() {
        return this.defkey;
    }

    public void setDefkey(int defkey) {
        this.defkey = defkey;
    }

    public String getKey1() {
        return this.key1;
    }

    public void setKey1(String key1) {
        this.key1 = key1;
    }

    public String getKey2() {
        return this.key2;
    }

    public void setKey2(String key2) {
        this.key2 = key2;
    }

    public String getKey3() {
        return this.key3;
    }

    public void setKey3(String key3) {
        this.key3 = key3;
    }

    public String getKey4() {
        return this.key4;
    }

    public void setKey4(String key4) {
        this.key4 = key4;
    }

    public int getKey1_bits() {
        return this.key1_bits;
    }

    public void setKey1_bits(int key1_bits) {
        this.key1_bits = key1_bits;
    }

    public int getKey2_bits() {
        return this.key2_bits;
    }

    public void setKey2_bits(int key2_bits) {
        this.key2_bits = key2_bits;
    }

    public int getKey3_bits() {
        return this.key3_bits;
    }

    public void setKey3_bits(int key3_bits) {
        this.key3_bits = key3_bits;
    }

    public int getKey4_bits() {
        return this.key4_bits;
    }

    public void setKey4_bits(int key4_bits) {
        this.key4_bits = key4_bits;
    }

    public String getWpa_psk() {
        return this.wpa_psk;
    }

    public void setWpa_psk(String wpa_psk) {
        this.wpa_psk = wpa_psk;
    }

    public String toString() {
        return "WifiBean [did=" + this.did + ", enable=" + this.enable + ", ssid=" + this.ssid + ", channel=" + this.channel + ", mode=" + this.mode + ", authtype=" + this.authtype + ", encryp=" + this.encryp + ", keyformat=" + this.keyformat + ", defkey=" + this.defkey + ", key1=" + this.key1 + ", key2=" + this.key2 + ", key3=" + this.key3 + ", key4=" + this.key4 + ", key1_bits=" + this.key1_bits + ", key2_bits=" + this.key2_bits + ", key3_bits=" + this.key3_bits + ", key4_bits=" + this.key4_bits + ", wpa_psk=" + this.wpa_psk + "]";
    }
}
