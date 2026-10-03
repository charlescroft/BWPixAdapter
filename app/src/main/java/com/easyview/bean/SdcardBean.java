package com.easyview.bean;

import java.io.Serializable;

/* loaded from: classes.dex */
public class SdcardBean implements Serializable {
    private static final long serialVersionUID = 1;
    private String did;
    private int record_conver_enable;
    private int record_mode;
    private int record_sd_status;
    private int record_size;
    private int record_time_enable;
    private int record_timer;
    private int sdfree;
    private int sdtotal;

    public String getDid() {
        return this.did;
    }

    public void setDid(String did) {
        this.did = did;
    }

    public int getRecord_conver_enable() {
        return this.record_conver_enable;
    }

    public void setRecord_conver_enable(int record_conver_enable) {
        this.record_conver_enable = record_conver_enable;
    }

    public int getRecord_timer() {
        return this.record_timer;
    }

    public void setRecord_timer(int record_timer) {
        this.record_timer = record_timer;
    }

    public int getRecord_size() {
        return this.record_size;
    }

    public void setRecord_size(int record_size) {
        this.record_size = record_size;
    }

    public int getRecord_time_enable() {
        return this.record_time_enable;
    }

    public void setRecord_time_enable(int record_time_enable) {
        this.record_time_enable = record_time_enable;
    }

    public int getRecord_sd_status() {
        return this.record_sd_status;
    }

    public void setRecord_sd_status(int record_sd_status) {
        this.record_sd_status = record_sd_status;
    }

    public int getSdtotal() {
        return this.sdtotal;
    }

    public void setSdtotal(int sdtotal) {
        this.sdtotal = sdtotal;
    }

    public int getSdfree() {
        return this.sdfree;
    }

    public void setSdfree(int sdfree) {
        this.sdfree = sdfree;
    }

    public int getRecordMode() {
        return this.record_mode;
    }

    public void setRecordMode(int mode) {
        this.record_mode = mode;
    }
}
