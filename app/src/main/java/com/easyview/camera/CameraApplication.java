package com.easyview.camera;

import android.app.Application;
import android.util.Log;
import com.easyview.ppcs.PPCSCamera;

/* loaded from: classes.dex */
public class CameraApplication extends Application {
    public static final int CALL_START_MODE = 2;
    public static final int NORMAL_START_MODE = 1;
    private static final String TAG = "app";
    private CameraList _cameraList = null;
    private int _startMode = 0;
    private boolean _initP2P = false;

    @Override // android.app.Application
    public void onCreate() {
        super.onCreate();
        Log.i("app", "onCreate");
        this._cameraList = new CameraList();
        object.p2pipcam.utils.Pub.load(this);
        InitP2P();
    }

    @Override // android.app.Application
    public void onTerminate() {
        Log.d("app", "onTerminate");
        super.onTerminate();
    }

    @Override // android.app.Application, android.content.ComponentCallbacks
    public void onLowMemory() {
        Log.d("app", "onLowMemory");
        super.onLowMemory();
    }

    @Override // android.app.Application, android.content.ComponentCallbacks2
    public void onTrimMemory(int level) {
        Log.d("app", "onTrimMemory");
        super.onTrimMemory(level);
    }

    public CameraList getCameraList() {
        return this._cameraList;
    }

    public int getStartMode() {
        return this._startMode;
    }

    public void setStartMode(int mode) {
        this._startMode = mode;
    }

    public boolean InitP2P() {
        if (this._initP2P) {
            return false;
        }
        PPCSCamera.Init(this);
        this._initP2P = true;
        return true;
    }

    public void DeinitP2P() {
        if (this._initP2P) {
            PPCSCamera.Free(this);
            this._initP2P = false;
        }
    }

    public boolean isInitP2P() {
        return this._initP2P;
    }
}
