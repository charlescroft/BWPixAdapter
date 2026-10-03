package com.easyview.basecamera;

/* loaded from: classes.dex */
public interface ICameraDataListener {
    void OnH264Data(ICamera iCamera, byte[] bArr, int i, int i2);

    void OnVideoData(ICamera iCamera, byte[] bArr, int i, int i2, int i3);
}
