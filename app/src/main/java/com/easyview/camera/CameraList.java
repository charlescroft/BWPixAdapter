package com.easyview.camera;

import com.easyview.basecamera.BaseCameraList;
import com.easyview.ppcs.PPCSCamera;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class CameraList extends BaseCameraList {
    private List<PPCSCamera> _listPPCS = new ArrayList();

    public boolean Add(String name, String id, String user, String pwd) {
        if (PPCSCamera.IsValidID(id)) {
            PPCSCamera camera = new PPCSCamera(name, id, user, pwd);
            this._listPPCS.add(camera);
            return super.Add(camera);
        }
        return false;
    }

    public void ReConnect() {
        for (PPCSCamera camera : this._listPPCS) {
            if (!camera.isOnline()) {
                camera.Start();
            }
        }
    }

    public void CloseAll() {
        for (PPCSCamera camera : this._listPPCS) {
            camera.Stop();
        }
    }

    public EVBaseCamera getCamera(int index) {
        if (this._list.size() > index) {
            return (EVBaseCamera) this._list.get(index);
        }
        return null;
    }

    public int count() {
        return this._list.size();
    }
}
