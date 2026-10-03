package object.p2pipcam.bean;

import android.graphics.Bitmap;

/* loaded from: classes.dex */
public class VideoRecordBean {
    private Bitmap bitmap;
    private int height;
    private byte[] length;
    private byte[] picture;
    private byte[] time;
    private int tspan;
    private int type;
    private int width;

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Bitmap getBitmap() {
        return this.bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public int getTspan() {
        return this.tspan;
    }

    public void setTspan(int tspan) {
        this.tspan = tspan;
    }

    public byte[] getLength() {
        return this.length;
    }

    public void setLength(byte[] length) {
        this.length = length;
    }

    public byte[] getTime() {
        return this.time;
    }

    public void setTime(byte[] time) {
        this.time = time;
    }

    public byte[] getPicture() {
        return this.picture;
    }

    public void setPicture(byte[] picture) {
        this.picture = picture;
    }
}
