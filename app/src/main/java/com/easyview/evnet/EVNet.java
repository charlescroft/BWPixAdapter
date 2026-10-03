package com.easyview.evnet;
import com.easyview.camera.EVBaseCamera;
public class EVNet {
    public interface IProgressListener { void OnProgress(Object obj, int cur, int total); }
    public static void SetCamera(DCamera c) {}
    public static void SetDownProgressListener(EVBaseCamera camera, IProgressListener l) {}
    public static void Command(int cmd, byte[] data, int len) {}
    public static int RequestCustomData(int type, byte[] data, int len) { return 0; }
    public static int DownRecord(String u, String p, String ip, int port, String path, int idx, int off) { return -1; }
    public static void Initial() {}
    public static void Deinitial() {}
    public static void StartVideo() {}
    public static void StopVideo() {}
    public static void SetRssi(int level) {}
}
