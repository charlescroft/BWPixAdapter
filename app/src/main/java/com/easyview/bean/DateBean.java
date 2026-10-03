package com.easyview.bean;

/* loaded from: classes.dex */
public class DateBean {
    private int now;
    private int ntp_enable;
    private String ntp_ser;
    private int tz;

    public int getNow() {
        return this.now;
    }

    public void setNow(int now) {
        this.now = now;
    }

    public int getTz() {
        return this.tz;
    }

    public void setTz(int tz) {
        this.tz = tz;
    }

    public int getNtp_enable() {
        return this.ntp_enable;
    }

    public void setNtp_enable(int ntp_enable) {
        this.ntp_enable = ntp_enable;
    }

    public String getNtp_ser() {
        return this.ntp_ser;
    }

    public void setNtp_ser(String ntp_ser) {
        this.ntp_ser = ntp_ser;
    }

    public String toString() {
        return "DateBean [now=" + this.now + ", tz=" + this.tz + ", ntp_enable=" + this.ntp_enable + ", ntp_ser=" + this.ntp_ser + "]";
    }
}
