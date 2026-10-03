package com.easyview.common;

/* loaded from: classes.dex */
public class EVTimeZone {
    public int hour;
    public int minute;
    public String name;

    public EVTimeZone(String s, int h, int m) {
        this.name = s;
        this.hour = h;
        this.minute = m;
    }

    public int getGMTDiff() {
        return this.hour > 0 ? (this.hour * 60) + this.minute : (this.hour * 60) - this.minute;
    }
}
