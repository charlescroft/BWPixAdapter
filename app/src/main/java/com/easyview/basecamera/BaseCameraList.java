package com.easyview.basecamera;

import com.easyview.basecamera.ICamera;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class BaseCameraList {
    protected List<BaseCamera> _list = new ArrayList();
    private ICamera.IAlarmListener _alarm = null;

    public boolean Add(BaseCamera camera) {
        boolean add;
        synchronized (this) {
            if (this._alarm != null) {
                camera.SetAlarmListener(this._alarm);
            }
            add = this._list.add(camera);
        }
        return add;
    }

    public void Del(String did) {
        synchronized (this) {
            BaseCamera camera = getCamera(did);
            if (camera != null) {
                this._list.remove(camera);
            }
        }
    }

    public void Clear() {
        synchronized (this) {
            this._list.clear();
        }
    }

    public void Start() {
        synchronized (this) {
            for (BaseCamera camera : this._list) {
                try {
                    Thread.sleep(100L);
                } catch (Exception e) {
                }
                camera.Start();
            }
        }
    }

    public void Stop() {
        synchronized (this) {
            for (BaseCamera camera : this._list) {
                try {
                    Thread.sleep(100L);
                } catch (Exception e) {
                }
                camera.Stop();
            }
        }
    }

    public void AddParamListener(ICameraParamListener listener) {
        synchronized (this) {
            for (BaseCamera camera : this._list) {
                camera.AddParamListener(listener);
            }
        }
    }

    public void RemoveParamListener(ICameraParamListener listener) {
        synchronized (this) {
            for (BaseCamera camera : this._list) {
                camera.RemoveParamListener(listener);
            }
        }
    }

    public BaseCamera getCamera(String id) {
        for (BaseCamera camera : this._list) {
            String i = camera.getID();
            if (i.compareTo(id) == 0) {
                return camera;
            }
        }
        return null;
    }

    public void SetAlarmListener(ICamera.IAlarmListener listener) {
        this._alarm = listener;
        synchronized (this) {
            for (BaseCamera camera : this._list) {
                camera.SetAlarmListener(listener);
            }
        }
    }
}
