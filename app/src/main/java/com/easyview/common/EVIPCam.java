package com.easyview.common;

import android.util.Log;
import com.tutk.IOTC.AVAPIs;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class EVIPCam {
    private static ArrayList<EV_NetInfo> infos = null;

    public static native void LanSearch(int i);

    public static native String Modify(String str, String str2, String str3, String str4);

    static {
        try {
            System.loadLibrary("evipc");
        } catch (UnsatisfiedLinkError ule) {
            System.out.println("loadLibrary evipc lib," + ule.getMessage());
        }
    }

    public static void Add(String devID, String IP, String Port, String P2P) {
        Log.i("EVIPCam", String.format("add:%s %s %s %s", devID, IP, Port, P2P));
        EV_NetInfo i = new EV_NetInfo();
        i.DevID = devID;
        i.P2P = P2P;
        i.IP = IP;
        i.Port = Port;
        infos.add(i);
    }

    public static ArrayList<EV_NetInfo> Search(int waitms) {
        infos = new ArrayList<>();
        LanSearch(AVAPIs.TIME_SPAN_LOSED);
        return infos;
    }
}
