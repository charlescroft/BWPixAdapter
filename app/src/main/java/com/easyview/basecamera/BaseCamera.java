package com.easyview.basecamera;

import android.graphics.Bitmap;
import com.easyview.basecamera.ICamera;
import com.easyview.bean.StorageStateBean;
import com.easyview.struct.EVCommandDefs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;

/* loaded from: classes.dex */
public abstract class BaseCamera implements ICamera {
    public static final int CAMERA_STATE_CONNECTED = 2;
    public static final int CAMERA_STATE_CONNECTING = 1;
    public static final int CAMERA_STATE_CONNECT_FAILED = 8;
    public static final int CAMERA_STATE_DISCONNECTED = 3;
    public static final int CAMERA_STATE_NONE = 0;
    public static final int CAMERA_STATE_TIMEOUT = 6;
    public static final int CAMERA_STATE_UNKNOWN_DEVICE = 4;
    public static final int CAMERA_STATE_UNSUPPORTED = 7;
    public static final int CAMERA_STATE_WRONG_PASSWORD = 5;
    public static final int COMMAND_RESPOND = 0;
    public static final int COMMAND_RESPOND_OK = 1;
    public static final int DATA_RESPOND = 1;
    public static final int DOORBELL_ALARM = 34;
    public static final int EVENT_BEGIN_RECORD = 513;
    public static final int EVENT_CALL = 48;
    public static final int EVENT_DEVICE_DESTROY = 40;
    public static final int EVENT_END_RECORD = 514;
    public static final int EVENT_PLAY_MUSIC_FINISH = 516;
    public static final int EV_CUSTOM_EVENT = 4;
    public static final int EV_CUSTOM_EVENTS = 5;
    public static final int EV_CUSTOM_EXTINFO = 16;
    public static final int EV_CUSTOM_PICTURE = 1;
    public static final int EV_CUSTOM_RECORD_DOWN = 2;
    public static final int EV_CUSTOM_RECORD_INDEX = 3;
    public static final int EV_EVENT_BEGIN_MESSAGE = 258;
    public static final int EV_EVENT_BODYTEMPALARM = 39;
    public static final int EV_EVENT_HOVERALARM = 7;
    public static final int EV_EVENT_HUMALARM = 32;
    public static final int EV_EVENT_ILLEGAL_UNLOCK = 259;
    public static final int EV_EVENT_LOWVOL = 42;
    public static final int EV_EVENT_OPEN_DOOR = 257;
    public static final int EV_EVENT_POWERALARM = 19;
    public static final int EV_EVENT_TEMPALARM = 33;
    public static final int EV_EVENT_VOICEALARM = 18;
    public static final int EXT_INFO_SN_433 = 1102;
    public static final int EXT_INFO_SN_BODYTEMP = 1003;
    public static final int EXT_INFO_SN_CALLBUTTON = 1101;
    public static final int EXT_INFO_SN_HOVER = 1011;
    public static final int EXT_INFO_SN_HUMIDITY = 1001;
    public static final int EXT_INFO_SN_TEMPERATURE = 1002;
    public static final int EXT_INFO_SN_THERVOL = 1004;
    public static final int GPIO_ALARM = 2;
    public static final int MOTION_ALARM = 1;
    private String _id;
    private String _name;
    private String _pwd;
    private String _user;
    private List<ICameraDataListener> _DataListeners = Collections.synchronizedList(new Vector());
    private List<ICameraParamListener> _ParamListeners = Collections.synchronizedList(new Vector());
    private ICamera.IAlarmListener _alarmListener = null;
    private ICamera.IRespondListener _customListener = null;
    private Bitmap _bitmap = null;
    private int _reconnect = 0;
    protected StorageStateBean _storageStateBean = null;
    protected EVCommandDefs.StorageStateStruct _storageStateStruct = new EVCommandDefs.StorageStateStruct();
    protected int _quality = 10;
    protected boolean _isOnline = false;
    protected boolean _isRecording = false;
    protected int _startRecordTime = 0;
    protected int _record_duration = 0;
    private CameraCaps _caps = new CameraCaps();
    protected EVCommandDefs.ExtInfos _extInfo = new EVCommandDefs.ExtInfos();
    protected EVCommandDefs.ExtThresholds _extThres = new EVCommandDefs.ExtThresholds();
    protected EVCommandDefs.EventInfo _eventInfo = new EVCommandDefs.EventInfo();
    protected List<EVCommandDefs.Event> _events = new ArrayList();
    protected int _eventFileID = 0;
    protected int _sensorres = 0;
    protected long _activeTime = 0;
    public EVCommandDefs.Event event = null;
    public String picturePathName = null;

    public BaseCamera(String name, String id, String user, String pwd) {
        this._name = name;
        this._id = id;
        this._user = user;
        this._pwd = pwd;
        for (int i = 0; i < 10; i++) {
            this._extInfo.values[i] = new EVCommandDefs.ExtInfo();
        }
        for (int i2 = 0; i2 < 10; i2++) {
            this._extThres.values[i2] = new EVCommandDefs.ExtThreshold();
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public String getID() {
        return this._id;
    }

    public String getName() {
        return this._name;
    }

    public String getUser() {
        return this._user;
    }

    public String getPwd() {
        return this._pwd;
    }

    public Bitmap getBitmap() {
        return this._bitmap;
    }

    @Override // com.easyview.basecamera.ICamera
    public boolean isOnline() {
        return this._isOnline;
    }

    public boolean isRecording() {
        return this._isRecording;
    }

    public String getRecordText() {
        Date date = new Date();
        long ms = date.getTime();
        long sec = (ms / 1000) - this._startRecordTime;
        if (sec < 0 || sec > 3600) {
            this._startRecordTime = (int) (ms / 1000);
        }
        String result = String.format("  %02d:%02d  ", Integer.valueOf(this._record_duration / 60), Integer.valueOf(this._record_duration % 60));
        this._record_duration++;
        return result;
    }

    public EVCommandDefs.StorageStateStruct getStorageState() {
        return this._storageStateStruct;
    }

    public void setPwd(String pwd) {
        this._pwd = pwd;
    }

    public void setBitmap(Bitmap bmp) {
        this._bitmap = bmp;
    }

    public void AddDataListener(ICameraDataListener listener) {
        if (!this._DataListeners.contains(listener)) {
            this._DataListeners.add(listener);
        }
    }

    public void RemoveDataListener(ICameraDataListener listener) {
        if (this._DataListeners.contains(listener)) {
            this._DataListeners.remove(listener);
        }
    }

    public void AddParamListener(ICameraParamListener listener) {
        if (!this._ParamListeners.contains(listener)) {
            this._ParamListeners.add(listener);
        }
    }

    public void RemoveParamListener(ICameraParamListener listener) {
        if (this._ParamListeners.contains(listener)) {
            this._ParamListeners.remove(listener);
        }
    }

    public void SetAlarmListener(ICamera.IAlarmListener listener) {
        this._alarmListener = listener;
    }

    @Override // com.easyview.basecamera.ICamera
    public void setCustomListener(ICamera.IRespondListener listener) {
        this._customListener = listener;
    }

    protected void OnAlarm(int alarmType) {
        if (this._alarmListener != null) {
            this._alarmListener.OnAlarm(this, alarmType);
        }
    }

    protected void OnCustom(int type, int result) {
        if (this._customListener != null) {
            this._customListener.OnRespondResult(this, type, result);
        }
    }

    public void OnParam(int type) {
        for (int i = 0; i < this._ParamListeners.size(); i++) {
            ICameraParamListener listener = this._ParamListeners.get(i);
            listener.OnParam(this, type);
        }
    }

    public void OnH264Data(byte[] data, int length, int type) {
        for (int i = 0; i < this._DataListeners.size(); i++) {
            ICameraDataListener listener = this._DataListeners.get(i);
            listener.OnH264Data(this, data, length, type);
        }
    }

    public CameraCaps get_caps() {
        return this._caps;
    }

    public EVCommandDefs.ExtInfos getExtInfo() {
        return this._extInfo;
    }

    public EVCommandDefs.ExtThresholds getExtThres() {
        return this._extThres;
    }

    public int getQuality() {
        return this._quality;
    }

    public boolean Reconnect() {
        if (this._reconnect >= 3) {
            return false;
        }
        this._reconnect++;
        return true;
    }

    public void ClearEvents() {
        this._events.clear();
    }

    public List<EVCommandDefs.Event> GetEvents() {
        return this._events;
    }

    public int GetEventFileID() {
        return this._eventFileID;
    }

    public int getSensorRes() {
        return this._sensorres;
    }

    public int getActivePeriod(int minMS) {
        long now = System.currentTimeMillis();
        int period = (int) (now - this._activeTime);
        if (period > minMS) {
            this._activeTime = now;
        }
        return period;
    }
}
