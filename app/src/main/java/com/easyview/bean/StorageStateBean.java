package com.easyview.bean;

import java.io.Serializable;

/* loaded from: classes.dex */
public class StorageStateBean implements Serializable {
    private static final long serialVersionUID = 1;
    private int record_duration;
    private int record_state;
    private int remain_bytes;
    private int total_bytes;

    public int getTotalBytes() {
        return this.total_bytes;
    }

    public void setTotalBytes(int total_bytes) {
        this.total_bytes = total_bytes;
    }

    public int getRemainBytes() {
        return this.remain_bytes;
    }

    public void setRemainBytes(int remain_bytes) {
        this.remain_bytes = remain_bytes;
    }

    public int getRecordState() {
        return this.record_state;
    }

    public void setRecordState(int record_state) {
        this.record_state = record_state;
    }

    public int getRecordDuration() {
        return this.record_duration;
    }

    public void setRecordDuration(int record_duration) {
        this.record_duration = record_duration;
    }
}
