package object.p2pipcam.nativecaller;

import android.content.Context;

/* loaded from: classes.dex */
public class NativeCaller {
    private static final String LOG_TAG = "NativeCaller";

    public static native int CloseAvi();

    public static native int DecodeH264Frame(byte[] bArr, int i, byte[] bArr2, int i2, int[] iArr);

    public static native int DoorBellCmd(String str, byte[] bArr, int i);

    public static native int EVCommand(String str, byte[] bArr, int i);

    public static native void Free();

    public static native void Init();

    public static native int OpenAvi(String str, String str2, int i, int i2, int i3);

    public static native int PPPPAlarmSetting(String str, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11, int i12, int i13, int i14, int i15, int i16, int i17, int i18, int i19, int i20, int i21, int i22, int i23, int i24, int i25, int i26, int i27, int i28, int i29, int i30, int i31, int i32, int i33);

    public static native int PPPPCameraControl(String str, int i, int i2);

    public static native int PPPPDDNSSetting(String str, int i, String str2, String str3, String str4, String str5, int i2, int i3);

    public static native int PPPPDatetimeSetting(String str, int i, int i2, int i3, String str2);

    public static native int PPPPFormatSD(String str);

    public static native int PPPPFtpSetting(String str, String str2, String str3, String str4, String str5, int i, int i2, int i3);

    public static native int PPPPGetCGI(String str, int i);

    public static native int PPPPGetSDCardRecordFileList(String str, int i, int i2);

    public static native int PPPPGetSystemParams(String str, int i);

    public static native void PPPPInitial(String str);

    public static native int PPPPMailSetting(String str, String str2, int i, String str3, String str4, int i2, String str5, int i3, String str6, String str7, String str8, String str9);

    public static native int PPPPNetworkDetect();

    public static native int PPPPNetworkSetting(String str, String str2, String str3, String str4, String str5, String str6, int i, int i2, int i3);

    public static native int PPPPPTZControl(String str, int i);

    public static native int PPPPPTZSetting(String str, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9);

    public static native int PPPPRebootDevice(String str);

    public static native int PPPPRestorFactory(String str);

    public static native int PPPPSDRecordSetting(String str, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11, int i12, int i13, int i14, int i15, int i16, int i17, int i18, int i19, int i20, int i21, int i22, int i23, int i24, int i25, int i26);

    public static native int PPPPSetCallbackContext(Context context);

    public static native int PPPPStartAudio(String str);

    public static native int PPPPStartTalk(String str);

    public static native int PPPPStopAudio(String str);

    public static native int PPPPStopTalk(String str);

    public static native int PPPPTalkAudioData(String str, byte[] bArr, int i);

    public static native int PPPPUserSetting(String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8);

    public static native int PPPPWifiSetting(String str, int i, String str2, int i2, int i3, int i4, int i5, int i6, int i7, String str3, String str4, String str5, String str6, int i8, int i9, int i10, int i11, String str7);

    public static native int SendCommonCGI(String str, String str2);

    public static native int StartPPPP(String str, String str2, String str3, int i);

    public static native int StartPPPPLivestream(String str, int i);

    public static native int StartPlayBack(String str, String str2, int i);

    public static native void StartSearch(int i, String str);

    public static native int StopPPPP(String str);

    public static native int StopPPPPLivestream(String str);

    public static native int StopPlayBack(String str);

    public static native void StopSearch();

    public static native int WriteData(byte[] bArr, int i, int i2);

    public static native int YUV4202RGB565(byte[] bArr, byte[] bArr2, int i, int i2);

    public static native int closeAvi(int i);

    public static native int openAvi(String str, byte[] bArr, int i);

    public static native int reWriteAvi(String str);

    public static native int readFrame(int i, byte[] bArr, int i2, byte[] bArr2, int i3);

    public static native int seekNextKeyFrame(int i, int i2);

    public static native int seekPrevKeyFrame(int i, int i2);

    static {
        System.loadLibrary("ffmpeg");
        System.loadLibrary("PPPP_API");
        System.loadLibrary("object_jni");
        System.loadLibrary("avi_utils");
    }
}
