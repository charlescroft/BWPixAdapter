package com.easyview.basecamera;

/* loaded from: classes.dex */
public class VideoFrame {
    public byte[] data;
    public int videoHeight;
    public int videoWidth;

    public VideoFrame(byte[] frameData, int width, int height) {
        this.videoWidth = 0;
        this.videoHeight = 0;
        this.data = null;
        this.data = frameData;
        this.videoWidth = width;
        this.videoHeight = height;
    }
}
