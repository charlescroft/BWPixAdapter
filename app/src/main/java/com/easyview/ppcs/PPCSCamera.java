package com.easyview.ppcs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.easyview.basecamera.BaseCamera;
import com.easyview.basecamera.CameraSearchInfo;
import com.easyview.basecamera.ICamera;
import com.easyview.basecamera.ICameraSearchListener;
import com.easyview.bean.AlermBean;
import com.easyview.bean.DateBean;
import com.easyview.bean.MailBean;
import com.easyview.bean.SdcardBean;
import com.easyview.bean.WifiBean;
import com.easyview.camera.CameraApplication;
import com.easyview.camera.EVBaseCamera;
import com.easyview.evnet.DCamera;
import com.easyview.evnet.EVNet;
import com.easyview.ndt.LogUtil;
import com.easyview.struct.EVCommandDefs;
import com.easyview.table.EventIndexTable;
import com.easyview.table.EventTable;
import com.easyview.table.RecordTable;
import com.easyview.tutk.QVConfigStruct;
import com.tutk.IOTC.AVAPIs;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import object.p2pipcam.nativecaller.NativeCaller;
import object.p2pipcam.utils.Pub;
import object.p2pwificam.clientActivity.BridgeService;
import struct.CString;
import struct.StructClass;
import struct.StructException;
import struct.StructField;
import struct.StructPacker;
import struct.StructUnpacker;

/* loaded from: classes.dex */
public class PPCSCamera extends EVBaseCamera {
    private static final String LOG_TAG = "PPCS";
    private final int BRIGHT;
    private final int CONTRAST;
    public String DevID;
    private int _alarmTime;
    private int _alarmType;
    private int _bellCallTime;
    private int _bellDamageTime;
    private int _brightness;
    private int _contrast;
    private boolean _event_modify;
    private boolean _ready_get_events;
    private int _recordIndex;
    private int _rotate;
    private ICamera.IRespondListener _wifiResultListener;
    private ICamera.IDownloadListener downListener;
    private File down_file;
    private FileOutputStream down_fos;
    private int down_record_index;
    public EVCommandDefs.Event event;
    public String eventFilePath;
    private int event_count;
    private boolean getFirstIndex;
    private boolean haveFirstIndex;
    public EVCommandDefs.IOSwitchParam[] ioSwitchParams;
    private ICamera.IRespondListener ioSwitchParamsListener;
    private boolean is_down_record;
    public EVCommandDefs.LCDCtrlParam lcdCtrlParam;
    private ICamera.IRespondListener lcdParamsListener;
    public PlaneParams planeParams;
    public boolean planeParams_OK;
    private ICamera.IRespondListener queryDeviceInfoListener;
    private ICamera.IRespondListener queryLightValueListener;
    public int reconnect;
    private int record_down_size;
    private int record_file_size;
    private ICamera.IRespondListener upgradeCheckListener;
    private ICamera.IRespondListener upgradeDeviceListener;
    public String videoPath;
    private int write_file_size;
    public static String CameraUser = "admin";
    private static boolean _init = false;
    private static Context _context = null;

    @StructClass
    public static class PlaneParams {

        @StructField(order = 2)
        public int showDate;

        @StructField(order = 0)
        public CString WiFiName = new CString(64);

        @StructField(order = 1)
        public CString DefaultName = new CString(64);
    }

    public static void Init(Context context) {
        _context = context;
        if (!_init) {
            Log.i(LOG_TAG, "Init");
            NativeCaller.PPPPInitial("EFGBFFBDKPILGKJDFJHPFAELGDNAHGMAHMFBBIDHAOJBLKKODOACDEPFGLKFIKLBADNJKKDJOLNLBNCKJMMI");
            NativeCaller.Init();
            _init = true;
            Intent intent = new Intent();
            intent.setClass(context, BridgeService.class);
            context.startService(intent);
        }
    }

    public static void Free(Context context) {
        if (_init) {
            Log.i(LOG_TAG, "Free...");
            CameraApplication app = (CameraApplication) context.getApplicationContext();
            app.getCameraList().Stop();
            app.getCameraList().SetAlarmListener(null);
            Intent intent = new Intent();
            intent.setClass(context, BridgeService.class);
            context.stopService(intent);
            _context = null;
            _init = false;
            NativeCaller.Free();
            Log.i(LOG_TAG, "Free");
        }
    }

    public static boolean isInit() {
        return _init;
    }

    public static void StartSearch(final ICameraSearchListener listener, int port) {
        BridgeService.AddCameraInterface addCamera = new BridgeService.AddCameraInterface() { // from class: com.easyview.ppcs.PPCSCamera.1
            @Override // object.p2pwificam.clientActivity.BridgeService.AddCameraInterface
            public void callBackSearchResultData(int cameraType, String strMac, String strName, String strDeviceID, String strIpAddr, int port2) {
                CameraSearchInfo info = new CameraSearchInfo();
                info.CameraType = 2;
                info.ID = strDeviceID;
                info.IP = strIpAddr;
                info.port = port2;
                info.deviceName = strName;
                if (listener != null) {
                    listener.OnSearch(info);
                }
            }
        };
        BridgeService.setAddCameraInterface(addCamera);
        NativeCaller.StartSearch(port, CameraUser);
    }

    public static void StopSearch() {
        BridgeService.setAddCameraInterface(null);
        NativeCaller.StopSearch();
    }

    public static boolean IsValidID(String id) {
        return id.startsWith(LOG_TAG) || id.startsWith("PPXW") || id.startsWith("JASCH") || id.startsWith("BWLED");
    }

    public static boolean IsPPXW(String id) {
        return id.startsWith("PPXW");
    }

    public void setBellCallTime(int time) {
        this._bellCallTime = time;
    }

    public int getBellCallTime() {
        return this._bellCallTime;
    }

    public void setBellDamageTime(int time) {
        this._bellDamageTime = time;
    }

    public int getBellDamageTime() {
        return this._bellDamageTime;
    }

    public String getKeyText() {
        return this.DevID != null ? this.DevID : getID();
    }

    public PPCSCamera(String name, String id, String user, String pwd) {
        super(name, id, user, pwd);
        this.BRIGHT = 1;
        this.CONTRAST = 2;
        this._alarmType = 0;
        this._alarmTime = 0;
        this._bellCallTime = 0;
        this._bellDamageTime = 0;
        this._recordIndex = -1;
        this._event_modify = true;
        this._ready_get_events = false;
        this.event_count = 0;
        this._brightness = 0;
        this._contrast = 0;
        this._rotate = 0;
        this.event = null;
        this.eventFilePath = null;
        this.videoPath = null;
        this.planeParams = new PlaneParams();
        this.planeParams_OK = false;
        this.ioSwitchParams = new EVCommandDefs.IOSwitchParam[8];
        this.lcdCtrlParam = new EVCommandDefs.LCDCtrlParam();
        this.ioSwitchParamsListener = null;
        this.lcdParamsListener = null;
        this.queryDeviceInfoListener = null;
        this.upgradeDeviceListener = null;
        this.upgradeCheckListener = null;
        this.queryLightValueListener = null;
        this.DevID = null;
        this.reconnect = 6;
        this.is_down_record = false;
        this.down_record_index = 0;
        this.record_file_size = 0;
        this.record_down_size = 0;
        this.down_file = null;
        this.write_file_size = 0;
        this.down_fos = null;
        this.downListener = null;
        this._wifiResultListener = null;
        this.getFirstIndex = false;
        this.haveFirstIndex = false;
        for (int i = 0; i < 8; i++) {
            this.ioSwitchParams[i] = new EVCommandDefs.IOSwitchParam();
        }
    }

    public void onCameraParams(int resolution, int brightness, int contrast, int hue, int saturation, int flip, int frame, int mode) {
        Log.i(LOG_TAG, "onCameraParams");
        this._brightness = brightness;
        this._contrast = contrast;
        this._rotate = 0;
        if ((flip & 1) == 1) {
            this._rotate = 1;
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void Start() {
        Log.i(LOG_TAG, "Start");
        if (!_init && _context != null) {
            Init(_context);
        }
        if (!this._isOnline) {
            NativeCaller.StartPPPP(getID(), getUser(), getPwd(), this.reconnect);
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void Stop() {
        NativeCaller.StopPPPP(getID());
        this._isOnline = false;
        this._isStartVideo = false;
    }

    @Override // com.easyview.basecamera.ICamera
    public void StartVideo(ICamera.IDataListener listener) {
        this._videoDataListener = listener;
        if (isOnline()) {
            int ret = NativeCaller.StartPPPPLivestream(getID(), 0);
            Log.i(LOG_TAG, "StartVideo ret:" + ret);
            this._isStartVideo = true;
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void StartVideoYUV(ICamera.IYUVDataListener listener) {
        this._yuvDataListener = listener;
        if (isOnline()) {
            int ret = NativeCaller.StartPPPPLivestream(getID(), 0);
            Log.i(LOG_TAG, "StartVideo ret:" + ret);
            this._isStartVideo = true;
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void StopVideo() {
        this._videoDataListener = null;
        NativeCaller.StopPPPPLivestream(getID());
        this._isStartVideo = false;
    }

    @Override // com.easyview.basecamera.ICamera
    public void StartAudio() {
        LogUtil.i(LOG_TAG, "StartAudio");
        NativeCaller.PPPPStartAudio(getID());
    }

    @Override // com.easyview.basecamera.ICamera
    public void StopAudio() {
        LogUtil.i(LOG_TAG, "StopAudio");
        NativeCaller.PPPPStopAudio(getID());
    }

    @Override // com.easyview.basecamera.ICamera
    public void StartTalk() {
        LogUtil.i(LOG_TAG, "StartTalk");
        NativeCaller.PPPPStartTalk(getID());
    }

    @Override // com.easyview.basecamera.ICamera
    public void TalkAudioData(byte[] data, int len) {
        NativeCaller.PPPPTalkAudioData(getID(), data, len);
    }

    @Override // com.easyview.basecamera.ICamera
    public void StopTalk() {
        LogUtil.i(LOG_TAG, "StopTalk");
        NativeCaller.PPPPStopTalk(getID());
    }

    @Override // com.easyview.basecamera.ICamera
    public void getWifiParam(WifiBean bean, ICamera.IRespondListener listener) {
        NativeCaller.PPPPGetSystemParams(getID(), 4);
    }

    @Override // com.easyview.basecamera.ICamera
    public void wifiScan(ICamera.IWifiScanListener listener) {
        NativeCaller.PPPPGetSystemParams(getID(), 20);
    }

    @Override // com.easyview.basecamera.ICamera
    public void setWifiParam(WifiBean bean, ICamera.IRespondListener listener) {
        NativeCaller.PPPPWifiSetting(getID(), bean.getEnable(), bean.getSsid(), bean.getChannel(), bean.getMode(), bean.getAuthtype() + 1, bean.getEncryp(), bean.getKeyformat(), bean.getDefkey(), bean.getKey1(), bean.getKey2(), bean.getKey3(), bean.getKey4(), bean.getKey1_bits(), bean.getKey2_bits(), bean.getKey3_bits(), bean.getKey4_bits(), bean.getWpa_psk());
    }

    @Override // com.easyview.basecamera.ICamera
    public void setPassword(String newPwd, String oldPwd, ICamera.IRespondListener listener) {
    }

    @Override // com.easyview.basecamera.ICamera
    public void getAlarmParam(ICamera.IAlarmParamListener listener) {
    }

    @Override // com.easyview.basecamera.ICamera
    public void setAlarmParam(AlermBean alermBean, ICamera.IRespondListener listener) {
        NativeCaller.PPPPAlarmSetting(getID(), alermBean.getMotion_armed(), alermBean.getMotion_sensitivity(), alermBean.getInput_armed(), alermBean.getIoin_level(), alermBean.getIolinkage(), alermBean.getIoout_level(), alermBean.getAlermpresetsit(), alermBean.getMail(), alermBean.getSnapshot(), alermBean.getRecord(), alermBean.getUpload_interval(), alermBean.getSchedule_enable(), -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1);
    }

    @Override // com.easyview.basecamera.ICamera
    public void getTimeParam(DateBean bean, ICamera.IRespondListener listener) {
    }

    @Override // com.easyview.basecamera.ICamera
    public void setTimeParam(DateBean bean, ICamera.IRespondListener listener) {
        Log.i(LOG_TAG, String.format("time:%d", Integer.valueOf(bean.getNow())));
        NativeCaller.PPPPDatetimeSetting(getID(), bean.getNow(), bean.getTz(), bean.getNtp_enable(), bean.getNtp_ser());
    }

    @Override // com.easyview.basecamera.ICamera
    public void getMailParam(MailBean bean, ICamera.IRespondListener listener) {
    }

    @Override // com.easyview.basecamera.ICamera
    public void setMailParam(MailBean mailBean, ICamera.IRespondListener listener) {
        NativeCaller.PPPPMailSetting(getID(), mailBean.getSvr(), mailBean.getPort(), mailBean.getUser(), mailBean.getPwd(), mailBean.getSsl(), mailBean.getSender(), mailBean.getAuth(), mailBean.getReceiver1(), mailBean.getReceiver2(), mailBean.getReceiver3(), mailBean.getReceiver4());
    }

    @Override // com.easyview.basecamera.ICamera
    public void getStoreParam(SdcardBean bean, ICamera.IRespondListener listener) {
        NativeCaller.PPPPGetSystemParams(getID(), 22);
    }

    @Override // com.easyview.basecamera.ICamera
    public void setStoreParam(SdcardBean sdcardBean, ICamera.IRespondListener listener) {
        if (sdcardBean.getRecord_time_enable() == 1) {
            NativeCaller.PPPPSDRecordSetting(getID(), sdcardBean.getRecord_conver_enable(), sdcardBean.getRecord_timer(), sdcardBean.getRecord_size(), sdcardBean.getRecord_time_enable(), -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, sdcardBean.getRecordMode());
        } else {
            NativeCaller.PPPPSDRecordSetting(getID(), sdcardBean.getRecord_conver_enable(), sdcardBean.getRecord_timer(), sdcardBean.getRecord_size(), sdcardBean.getRecord_time_enable(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, sdcardBean.getRecordMode());
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void querystorageState(ICamera.IRespondListener listener) {
        this._storageStateListener = listener;
        NativeCaller.SendCommonCGI(getID(), "get_sd_state.cgi?");
    }

    @Override // com.easyview.basecamera.ICamera
    public void formatTF(ICamera.IRespondListener listener) {
        NativeCaller.PPPPFormatSD(getID());
    }

    @Override // com.easyview.basecamera.ICamera
    public void setResolution(int resolution) {
        this._quality = resolution;
        NativeCaller.PPPPCameraControl(getID(), 0, resolution);
        if (IsPPXW(getID())) {
            switch (resolution) {
                case 0:
                case 1:
                    NativeCaller.StartPPPPLivestream(getID(), 3);
                    break;
                case 2:
                    NativeCaller.StartPPPPLivestream(getID(), 0);
                    break;
                case 3:
                case 4:
                    NativeCaller.StartPPPPLivestream(getID(), 1);
                    break;
            }
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void setVideoQuality(int quality) {
        this._quality = quality;
        int resolution = 4 - (quality / 20);
        NativeCaller.PPPPCameraControl(getID(), 0, resolution);
        if (IsPPXW(getID())) {
            switch (resolution) {
                case 0:
                case 1:
                    NativeCaller.StartPPPPLivestream(getID(), 3);
                    break;
                case 2:
                    NativeCaller.StartPPPPLivestream(getID(), 0);
                    break;
                case 3:
                case 4:
                    NativeCaller.StartPPPPLivestream(getID(), 1);
                    break;
            }
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void ptzControl(int command) {
        NativeCaller.PPPPPTZControl(getID(), command);
    }

    @Override // com.easyview.basecamera.ICamera
    public void searchRecordList(long beginTime, long endTime, ICamera.IRespondListener listener) {
        NativeCaller.PPPPGetSDCardRecordFileList(getID(), (int) beginTime, (int) endTime);
    }

    @Override // com.easyview.basecamera.ICamera
    public void recordPlay(String fileName) {
        NativeCaller.StartPlayBack(getID(), fileName, 0);
    }

    @Override // com.easyview.basecamera.ICamera
    public void recordStop() {
        NativeCaller.StopPlayBack(getID());
    }

    @Override // com.easyview.basecamera.ICamera
    public void getCaps(ICamera.IRespondListener listener) {
        StructPacker packer = new StructPacker(ByteOrder.LITTLE_ENDIAN);
        EVCommandDefs.EVCommandStruct s = new EVCommandDefs.EVCommandStruct();
        s.symbol = EVCommandDefs.EVCommandStruct.SYMBOL;
        s.size = (byte) 8;
        s.command = EVCommandDefs.GET_CAPS;
        s.length = 0;
        try {
            packer.writeObject(s);
            byte[] data = packer.toArray();
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    public void OnEVCommand(byte[] data, int len) {
        StructUnpacker up = new StructUnpacker(data, ByteOrder.LITTLE_ENDIAN);
        EVCommandDefs.EVCommandStruct s = new EVCommandDefs.EVCommandStruct();
        int len2 = len - 8;
        try {
            up.readObject(s);
            Log.i("Event", String.format("OnEVCommand command:0x%X len:%d", Short.valueOf(s.command), Integer.valueOf(len2)));
            switch (s.command) {
                case 257:
                    get_caps().read(up);
                    return;
                case 258:
                    Log.i("Event", String.format("GET_EVENTS len:%d", Integer.valueOf(len2)));
                    if (len2 == 4) {
                        EVCommandDefs.EVSimpleResp resp = new EVCommandDefs.EVSimpleResp();
                        up.readObject(resp);
                        if (this._searchEventsListener != null) {
                            this._searchEventsListener.OnRespondResult(this, s.command, resp.result);
                        }
                        this._totalEvents = resp.result;
                        this.event_count = 0;
                        if (resp.result == 0 && this._ready_get_events) {
                            Log.i("Event", String.format("searchEvents", new Object[0]));
                            this._ready_get_events = false;
                            searchEvents(-10001, 0, null);
                            return;
                        }
                        return;
                    }
                    if (len2 == 32) {
                        EVCommandDefs.EVCommonResp resp2 = new EVCommandDefs.EVCommonResp();
                        up.readObject(resp2);
                        if (resp2.command == -10002) {
                            if (this._totalEvents == resp2.data0 && this._eventFileID == resp2.data1 && this._eventIndex == resp2.data2) {
                                this._event_modify = false;
                            } else {
                                this._event_modify = true;
                                this._totalEvents = resp2.data0;
                                this._eventFileID = resp2.data1;
                                this._eventIndex = resp2.data2;
                            }
                            Log.i("Event", String.format("total:%d fid:%d index:%d", Integer.valueOf(this._totalEvents), Integer.valueOf(this._eventFileID), Integer.valueOf(resp2.data2)));
                            if (this._queryEventInfoListener != null) {
                                this._queryEventInfoListener.OnRespondResult(this, resp2.command, resp2.data0);
                                return;
                            }
                            return;
                        }
                        return;
                    }
                    break;
                case DCamera.EV_COMMAND_DOWNLOAD_RECORD /* 261 */:
                    EVCommandDefs.EVDownRecordResp resp3 = new EVCommandDefs.EVDownRecordResp();
                    up.readObject(resp3);
                    if (resp3.file_size == 0) {
                        if (this.downListener != null) {
                            this.downListener.OnProgress(this, 0, 0);
                            return;
                        }
                        return;
                    }
                    this.record_file_size = resp3.file_size;
                    return;
                case QVConfigStruct.QV_CMD_SET_TIME /* 264 */:
                    break;
                case 265:
                    int count = up.readInt();
                    Log.i("Event", String.format("event list:%d", Integer.valueOf(count)));
                    this.event = new EVCommandDefs.Event();
                    this.event.index = 0;
                    for (int i = 0; i < count; i++) {
                        up.readObject(this.event);
                        EventIndexTable.getInstance(Pub.getContext()).updateIndex(getID(), this._eventFileID, this.event.index);
                        Log.i("Event", String.format("index:%d type:%d time:%d record:%d", Integer.valueOf(this.event.index), Short.valueOf(this.event.event_type), Integer.valueOf(this.event.event_time), Integer.valueOf(this.event.record_index)));
                        if (this.event.event_type == 513) {
                            RecordTable.getInstance(Pub.getContext()).Save(getKeyText(), this._eventFileID, this.event, null);
                        } else if (this.event.event_type == 514) {
                            int beginTime = RecordTable.getInstance(Pub.getContext()).getBeginTime(getKeyText(), this._eventFileID, this.event.record_index);
                            if (beginTime > 0) {
                                RecordTable.getInstance(Pub.getContext()).Update(getKeyText(), this._eventFileID, this.event.record_index, this.event.event_time);
                                EventTable.getInstance(Pub.getContext()).UpdateRecordIndex(getID(), this._eventFileID, beginTime, this.event.event_time, this.event.record_index);
                            }
                        } else {
                            EventTable.getInstance(Pub.getContext()).Save(getID(), this._eventFileID, this.event, null);
                        }
                    }
                    if (this._queryEventsListener != null) {
                        this._queryEventsListener.OnRespondResult(this, this.event.index, this._eventIndex);
                        return;
                    }
                    return;
                case 266:
                    EVCommandDefs.EVSimpleResp resp4 = new EVCommandDefs.EVSimpleResp();
                    up.readObject(resp4);
                    if (this._queryEventListener != null && resp4.result == 0) {
                        ICamera.IRespondListener l = this._queryEventListener;
                        this._queryEventListener = null;
                        Log.i("Event", "Get_EVENT");
                        l.OnRespondResult(this, s.command, resp4.result);
                        return;
                    }
                    return;
                case 279:
                    up.readObject(this._extThres);
                    if (this._extThresListener != null) {
                        this._extThresListener.OnRespondResult(this, 279, 0);
                        return;
                    }
                    return;
                case 280:
                    EVCommandDefs.EVSimpleResp resp5 = new EVCommandDefs.EVSimpleResp();
                    up.readObject(resp5);
                    if (this._extSetThresListener != null) {
                        this._extSetThresListener.OnRespondResult(this, 280, resp5.result);
                        return;
                    }
                    return;
                case 304:
                    for (int i2 = 0; i2 < 8; i2++) {
                        up.readObject(this.ioSwitchParams[i2]);
                    }
                    if (this.ioSwitchParamsListener != null) {
                        this.ioSwitchParamsListener.OnRespondResult(this, 304, 0);
                        this.ioSwitchParamsListener = null;
                        return;
                    }
                    return;
                case 305:
                    if (this.ioSwitchParamsListener != null) {
                        this.ioSwitchParamsListener.OnRespondResult(this, 305, 0);
                        this.ioSwitchParamsListener = null;
                        return;
                    }
                    return;
                case QVConfigStruct.QV_CMD_SET_DATETIME /* 307 */:
                    up.readObject(this.lcdCtrlParam);
                    if (this.lcdParamsListener != null) {
                        ICamera.IRespondListener listener = this.lcdParamsListener;
                        this.lcdParamsListener = null;
                        listener.OnRespondResult(this, 0, this.lcdCtrlParam.cmd);
                        return;
                    }
                    return;
                case 320:
                    EVCommandDefs.EVCommonResp resp6 = new EVCommandDefs.EVCommonResp();
                    up.readObject(resp6);
                    if (this.queryLightValueListener != null) {
                        ICamera.IRespondListener listener2 = this.queryLightValueListener;
                        this.queryLightValueListener = null;
                        listener2.OnRespondResult(this, resp6.data0, resp6.data1);
                        return;
                    }
                    return;
                case BaseCamera.EVENT_BEGIN_RECORD /* 513 */:
                    EVCommandDefs.EVSimpleResp resp7 = new EVCommandDefs.EVSimpleResp();
                    up.readObject(resp7);
                    if (this._wifiResultListener != null) {
                        this._wifiResultListener.OnRespondResult(this, BaseCamera.EVENT_BEGIN_RECORD, resp7.result);
                        return;
                    }
                    return;
                case 515:
                    up.readObject(this.DevInfoParam);
                    if (this.queryDeviceInfoListener != null) {
                        this.queryDeviceInfoListener.OnRespondResult(this, 515, 0);
                    }
                    this.queryDeviceInfoListener = null;
                    return;
                case BaseCamera.EVENT_PLAY_MUSIC_FINISH /* 516 */:
                    if (this.upgradeDeviceListener != null) {
                        this.upgradeDeviceListener.OnRespondResult(this, BaseCamera.EVENT_PLAY_MUSIC_FINISH, 0);
                    }
                    this.upgradeDeviceListener = null;
                    return;
                case 517:
                    if (this.upgradeCheckListener != null) {
                        this.upgradeCheckListener.OnRespondResult(this, 517, 0);
                    }
                    this.upgradeCheckListener = null;
                    return;
                default:
                    return;
            }
            if (len2 >= 12) {
                up.readObject(this._eventInfo);
                if (this._eventFileID == 0) {
                    Pair<Integer, Integer> result = EventIndexTable.getInstance(Pub.getContext()).getLastIndex(getID());
                    this._eventIndex = ((Integer) result.first).intValue() + 1;
                    this._eventFileID = ((Integer) result.second).intValue();
                    Log.i("Event", String.format("read table  fid:%d index:%d", Integer.valueOf(this._eventFileID), Integer.valueOf(this._eventIndex)));
                }
                if (this._eventFileID != this._eventInfo.file_id) {
                    this._eventFileID = this._eventInfo.file_id;
                    this._eventIndex = EventIndexTable.getInstance(Pub.getContext()).getLastIndex(getID(), this._eventFileID);
                }
                if (this._eventFileID == this._eventInfo.file_id && this._eventIndex == this._eventInfo.index) {
                    this._event_modify = false;
                } else {
                    this._event_modify = true;
                    this._totalEvents = this._eventInfo.total;
                    this._eventFileID = this._eventInfo.file_id;
                    this._eventIndex = this._eventInfo.index;
                }
                Log.i("Event", String.format("total:%d id:%d index:%d", Integer.valueOf(this._totalEvents), Integer.valueOf(this._eventFileID), Integer.valueOf(this._eventIndex)));
            }
            if (this._queryEventInfoListener != null) {
                this._queryEventInfoListener.OnRespondResult(this, s.command, this._eventIndex);
            }
            this._queryEventInfoListener = null;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (StructException e2) {
            e2.printStackTrace();
        }
    }

    public void OnCommand(int type, byte[] data, int len) {
        StructUnpacker up = new StructUnpacker(data, ByteOrder.LITTLE_ENDIAN);
        switch (type) {
            case 20481:
                try {
                    up.readObject(this.planeParams);
                    String wifiName = this.planeParams.WiFiName.toString();
                    Log.i(LOG_TAG, "OnCommand wifiName:" + wifiName);
                    if (TextUtils.isEmpty(wifiName)) {
                        this.planeParams_OK = false;
                    } else {
                        this.planeParams_OK = true;
                    }
                    if (this._requestParamsListener != null) {
                        this._requestParamsListener.OnRespondResult(this, 1, 0);
                        break;
                    }
                } catch (StructException e) {
                    e.printStackTrace();
                    return;
                }
                break;
            case 20482:
                try {
                    up.readObject(this._storageStateStruct);
                    if (this._storageStateStruct.record_state > 0) {
                        if (!this._isRecording) {
                            Log.i("Event", "state start record!");
                            this._isRecording = true;
                        }
                        Date date = new Date();
                        long ms = date.getTime();
                        if (this._storageStateStruct.record_duration < 0) {
                            this._storageStateStruct.record_duration = 0;
                        }
                        if (this._storageStateStruct.record_duration > 3600) {
                            this._storageStateStruct.record_duration = 0;
                        }
                        this._record_duration = this._storageStateStruct.record_duration;
                        this._startRecordTime = ((int) (ms / 1000)) - this._storageStateStruct.record_duration;
                        break;
                    } else if (this._isRecording) {
                        Log.i("Event", "state stop record!");
                        this._isRecording = false;
                        break;
                    }
                } catch (StructException e2) {
                    e2.printStackTrace();
                    return;
                }
                break;
            case 20483:
                try {
                    this._sensorres = up.readInt();
                    Log.i(LOG_TAG, "SensorRes:" + this._sensorres);
                    break;
                } catch (IOException e3) {
                    e3.printStackTrace();
                    return;
                }
        }
    }

    @Override // com.easyview.camera.EVBaseCamera
    public void OnCustomData(int type, byte[] data, int len) {
        int eventType = 0;
        int eventTime = 0;
        String fileName = null;
        File file = null;
        FileOutputStream fos = null;
        Log.i("Event", String.format("OnCustomData len:%d", Integer.valueOf(len)));
        switch (type) {
            case 1:
                File div = Pub.photoSavePathFile();
                if (div != null && !div.exists()) {
                    div.mkdirs();
                }
                try {
                    eventType = Pub.byte2int(data, 0);
                    eventTime = Pub.byte2int(data, 4);
                    fileName = String.format("%s_%d.jpg", Pub.getDIDNum(getID()), Integer.valueOf(eventTime));
                    file = new File(div, fileName);
                    fos = new FileOutputStream(file);
                } catch (Exception e) {
                    Log.e("EventTable", "Save Picture failed to open file: " + e.getMessage(), e);
                    return;
                }
                try {
                    fos.write(data, 8, len - 8);
                    fos.close();
                    String path = file.getAbsolutePath();
                    Log.i("EventTable", String.format("Save Picture success:%s %d path:%s", fileName, Integer.valueOf(len - 8), path));
                    if (eventType == 513) {
                        if (!Pub.isDrone()) {
                            RecordTable.getInstance(Pub.getContext()).Save(getID(), this._eventFileID, eventTime, eventTime, path);
                        }
                    } else {
                        boolean updated = EventTable.getInstance(Pub.getContext()).Update(getID(), this._eventFileID, eventType, eventTime, path);
                        Log.i("EventTable", "EventTable.Update returned: " + updated);
                    }
                    break;
                } catch (Exception e2) {
                    Log.e("EventTable", "Save Picture write error: " + e2.getMessage(), e2);
                    return;
                }
            case 2:
                try {
                    dealDownRecord(data, len);
                    break;
                } catch (IOException e3) {
                    e3.printStackTrace();
                    return;
                } catch (StructException e4) {
                    e4.printStackTrace();
                    return;
                }
            case 4:
                try {
                    dealEvent(data, len);
                    break;
                } catch (IOException e5) {
                    e5.printStackTrace();
                    return;
                }
            case 5:
                try {
                    dealEvents(data, len);
                    break;
                } catch (IOException e6) {
                    e6.printStackTrace();
                    return;
                } catch (StructException e7) {
                    e7.printStackTrace();
                    return;
                }
            case 16:
                StructUnpacker up = new StructUnpacker(data, ByteOrder.LITTLE_ENDIAN);
                try {
                    up.readObject(this._extInfo);
                    OnCustom(type, 0);
                    break;
                } catch (StructException e8) {
                    e8.printStackTrace();
                    return;
                }
        }
    }

    public void OnNotify(int type, int param) {
        Log.i(LOG_TAG, "OnNotify:" + type + " param:" + param);
        if (type == 0) {
            OnStatusNotify(param);
            if (param == 2) {
                this._isOnline = true;
                NativeCaller.PPPPNetworkDetect();
            } else if (param == 3 || param == 6 || param == 4) {
                this._isOnline = false;
                this._isStartVideo = false;
            }
            if (param == 3) {
                _init = false;
            }
        }
    }

    private void dealDownRecord(byte[] data, int len) throws IOException, StructException {
        int dup_size;
        int write_size;
        if (this.is_down_record) {
            StructUnpacker up = new StructUnpacker(data, ByteOrder.LITTLE_ENDIAN);
            EVCommandDefs.EVRecordDataHeader s = new EVCommandDefs.EVRecordDataHeader();
            up.readObject(s);
            if (s.curr_size > this.write_file_size) {
                Log.i("DownRecord", String.format("data loss %d -> %d", Integer.valueOf(this.write_file_size), Integer.valueOf(s.curr_size)));
                try {
                    byte[] send_data = EVCommandDefs.MakeDownRecordReqPacket(this.down_record_index, this.write_file_size);
                    NativeCaller.EVCommand(getID(), send_data, send_data.length);
                    return;
                } catch (StructException e) {
                    e.printStackTrace();
                    return;
                }
            }
            int data_size = len - 20;
            this.record_down_size = s.curr_size + data_size;
            if (this.record_down_size < this.write_file_size) {
                Log.i("DownRecord", String.format("data dup %d -> %d", Integer.valueOf(this.write_file_size), Integer.valueOf(s.curr_size)));
                return;
            }
            if (this.down_fos == null) {
                File div = Pub.videoSavePathFile();
                Date date = new Date();
                long ms = s.begin_time;
                date.setTime(ms * 1000);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                String fileName = String.format("%s_%s.avi", Pub.getDIDNum(getID()), sdf.format(date));
                Log.i("DownRecord", String.format("time:%d file:%s", Integer.valueOf(s.begin_time), fileName));
                this.down_file = new File(div, fileName);
                this.down_fos = new FileOutputStream(this.down_file, true);
            }
            Log.i("DownRecord", String.format("%d + %d = %d -> %d", Integer.valueOf(s.curr_size), Integer.valueOf(data_size), Integer.valueOf(this.record_down_size), Integer.valueOf(this.record_file_size)));
            if (this.record_down_size > this.write_file_size && (write_size = data_size - (dup_size = data_size - (this.record_down_size - this.write_file_size))) > 0) {
                this.down_fos.write(data, dup_size + 20, write_size);
                this.write_file_size += write_size;
            }
            if (this.downListener != null) {
                this.downListener.OnProgress(this, s.curr_size + data_size, s.total_size);
            }
            if (this.record_down_size >= s.total_size) {
                String path = this.down_file.getAbsolutePath();
                RecordTable.getInstance(Pub.getContext()).Update(getKeyText(), this._eventFileID, s.begin_time, path);
                this.down_fos.flush();
                this.down_fos.close();
                this.down_fos = null;
                this.write_file_size = 0;
                Pub.getContext().sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE", Uri.parse("file://" + path)));
            }
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void downRecord(int recordIndex, int offset, ICamera.IDownloadListener listener) {
        this.downListener = listener;
        EVNet.SetDownProgressListener(this, new EVNet.IProgressListener() { // from class: com.easyview.ppcs.PPCSCamera.2
            int count = 0;

            @Override // com.easyview.evnet.EVNet.IProgressListener
            public void OnProgress(Object obj, int cur, int total) {
                PPCSCamera camera = (PPCSCamera) obj;
                if (camera.downListener != null) {
                    int i = this.count;
                    this.count = i + 1;
                    if (i > 10) {
                        Log.i("DownRecord", String.format(" %d -> %d", Integer.valueOf(cur), Integer.valueOf(total)));
                        this.count = 0;
                    }
                    camera.downListener.OnProgress((ICamera) obj, cur, total);
                }
            }
        });
        this.down_record_index = recordIndex;
        if (offset == -1) {
            int beginTime = RecordTable.getInstance(Pub.getContext()).getBeginTime(getKeyText(), this._eventFileID, recordIndex);
            Date date = new Date();
            long ms = beginTime;
            date.setTime(ms * 1000);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String fileName = String.format("%s_%s.avi", Pub.getDIDNum(getID()), sdf.format(date));
            offset = Pub.videoFileLength(fileName);
            Log.i("DownRecord", String.format("continue down offset:%d index:%d t:%d %s", Integer.valueOf(offset), Integer.valueOf(recordIndex), Integer.valueOf(beginTime), fileName));
        }
        this.record_down_size = offset;
        this.write_file_size = offset;
        this.is_down_record = true;
        Log.i("DownRecord", String.format("start down %d", Integer.valueOf(offset)));
        if (0 == 0) {
            if (offset > 100) {
                offset -= 100;
            }
            try {
                byte[] data = EVCommandDefs.MakeDownRecordReqPacket(recordIndex, offset);
                NativeCaller.EVCommand(getID(), data, data.length);
            } catch (StructException e) {
                e.printStackTrace();
            }
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void stopDownRecord(ICamera.IDownloadListener listener) {
        Log.i("DownRecord", String.format("stop down %d", Integer.valueOf(this.write_file_size)));
        if (this.down_fos != null) {
            try {
                this.down_fos.flush();
                this.down_fos.close();
                this.down_fos = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.is_down_record = false;
        try {
            byte[] data = EVCommandDefs.MakeSimpleReqPacket(EVCommandDefs.STOP_DOWN_RECORD, new int[0]);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e2) {
            e2.printStackTrace();
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void searchRecords(int beginTime, int endTime, int with_snap, ICamera.IRespondListener listener) {
        try {
            byte[] data = EVCommandDefs.MakeSearchRecordReqPacket(beginTime, endTime, with_snap);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void searchEvents(int beginIndex, int endIndex, ICamera.IRespondListener listener) {
        this._searchEventsListener = listener;
        if (beginIndex == -1) {
            Pair<Integer, Integer> result = EventTable.getInstance(Pub.getContext()).getMaxIndex(getID());
            beginIndex = ((Integer) result.first).intValue();
            ((Integer) result.second).intValue();
        }
        if (beginIndex == -10001) {
            ClearEvents();
        }
        try {
            byte[] data = EVCommandDefs.MakeSearchEventReqPacket(beginIndex, endIndex, 0, 1);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void queryEventInfo(ICamera.IRespondListener listener) {
        this._queryEventInfoListener = listener;
        try {
            byte[] data = EVCommandDefs.MakeSimpleReqPacket(EVCommandDefs.GET_EVENT_INFO, 0);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void delEvents(int[] array, ICamera.IRespondListener listener) {
        this._eventIndex = -1;
        try {
            byte[] data = EVCommandDefs.MakeDelEventsReqPacket(array);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (StructException e2) {
            e2.printStackTrace();
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:22:0x017d  */
    /* JADX WARN: Removed duplicated region for block: B:25:0x0188  */
    /* JADX WARN: Removed duplicated region for block: B:35:? A[RETURN, SYNTHETIC] */
    private void dealEvent(byte[] data, int len) throws IOException {
        if (len < 20 || data == null) {
            return;
        }
        FileOutputStream fos = null;
        File div = Pub.photoSavePathFile();
        if (div != null && !div.exists()) {
            div.mkdirs();
        }
        try {
            StructUnpacker up = new StructUnpacker(data, ByteOrder.LITTLE_ENDIAN);
            this.event = new EVCommandDefs.Event();
            up.readObject(this.event);
            Date date = new Date();
            long ms = this.event.event_time * 1000L;
            date.setTime(ms);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault());
            String fileName = String.format("%s_%s.jpg", Pub.getDIDNum(getID()), sdf.format(date));
            File file = new File(div, fileName);
            fos = new FileOutputStream(file);
            fos.write(data, 20, len - 20);
            fos.close();
            Log.i("Event", String.format("%d Save Picture:%s index:%d time:%d", (int) this.event.event_type, fileName, this.event.index, this.event.event_time));
            String path = file.getAbsolutePath();
            this.eventFilePath = path;
            if (this.event.event_type == 513) {
                RecordTable.getInstance(Pub.getContext()).Save(getKeyText(), this._eventFileID, this.event, path);
            }
            if (this.event.event_type == 514) {
                this._recordIndex = this.event.record_index;
                int beginTime = RecordTable.getInstance(Pub.getContext()).getBeginTime(getKeyText(), this._eventFileID, this.event.record_index);
                if (Pub.isDrone()) {
                    if (beginTime != this.event.event_time) {
                        RecordTable.getInstance(Pub.getContext()).delete(getKeyText(), this._eventFileID, this.event.record_index);
                    }
                    RecordTable.getInstance(Pub.getContext()).Save(getKeyText(), this._eventFileID, this.event, path);
                    EventTable.getInstance(Pub.getContext()).Save(getID(), this._eventFileID, this.event, path);
                } else {
                    RecordTable.getInstance(Pub.getContext()).Save(getKeyText(), this._eventFileID, this.event, path);
                    EventTable.getInstance(Pub.getContext()).Save(getID(), this._eventFileID, this.event, path);
                }
            } else {
                EventTable.getInstance(Pub.getContext()).Save(getID(), this._eventFileID, this.event, path);
            }
        } catch (Exception e) {
            Log.e("Event", "dealEvent write picture error: " + e.getMessage(), e);
        }
        this.event_count++;
        if (this.event.index == 0) {
            this.haveFirstIndex = true;
        }
        if (this._queryEventListener != null) {
            int index = this.event.index;
            if (this.getFirstIndex && this.haveFirstIndex && index == 1) {
                index = 0;
            }
            ICamera.IRespondListener listener = this._queryEventListener;
            this._queryEventListener = null;
            listener.OnRespondResult(this, 1, index);
        }
    }

    private void dealEvents(byte[] data, int len) throws IOException, StructException {
        StructUnpacker up = new StructUnpacker(data, ByteOrder.LITTLE_ENDIAN);
        EVCommandDefs.EVEventListResp resp = new EVCommandDefs.EVEventListResp();
        up.readObject(resp);
        Log.i("Event", String.format("event list,total: %d index:%d count:%d flag:%X", Integer.valueOf(resp.total), Byte.valueOf(resp.index), Short.valueOf(resp.count), Byte.valueOf(resp.endflag)));
        for (int i = 0; i < resp.count; i++) {
            EVCommandDefs.Event event = new EVCommandDefs.Event();
            up.readObject(event);
            this._events.add(event);
        }
        if (resp.endflag == 1 || resp.endflag == -69) {
            Log.i("Event", String.format("event count %d", Integer.valueOf(this._events.size())));
            if (this._queryEventsListener != null) {
                this._queryEventsListener.OnRespondResult(this, -10001, resp.total);
            }
        }
    }

    public void SaveAlarm(int alarmType, int alarmTime, int endTime, int value) {
        if (alarmType == 516) {
            if (this._playAudioListener != null) {
                this._playAudioListener.OnRespondResult(this, BaseCamera.EVENT_PLAY_MUSIC_FINISH, 10);
                return;
            }
            return;
        }
        if (alarmType == 514) {
            EventTable.getInstance(Pub.getContext()).Update(getID(), this._eventFileID, alarmType, alarmTime, endTime);
            if (this._recordIndex >= 0) {
                EventTable.getInstance(Pub.getContext()).UpdateRecordIndex(getID(), this._eventFileID, alarmTime, endTime, this._recordIndex);
                this._recordIndex = -1;
            }
            if (this._isRecording) {
                Log.i("Event", "event stop record!");
                this._isRecording = false;
                return;
            }
            return;
        }
        if (alarmType == 513) {
            Date date = new Date();
            long ms = date.getTime();
            this._startRecordTime = (int) (ms / 1000);
            if (!this._isRecording) {
                Log.i("Event", "event start record!");
                this._isRecording = true;
                this._record_duration = 0;
            }
        }
        if (alarmType != 513) {
            this._alarmType = alarmType;
            this._alarmTime = alarmTime;
        }
    }

    private void SimpleEVCommand(short command) {
        try {
            byte[] data = EVCommandDefs.MakeSimpleReqPacket(command, new int[0]);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void queryWifiResult(ICamera.IRespondListener listener) {
        this._wifiResultListener = listener;
        try {
            byte[] data = EVCommandDefs.MakeSimpleReqPacket(EVCommandDefs.QUERY_WIFI_RESULT, new int[0]);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void snapShot(ICamera.IRespondListener listener) {
        NativeCaller.PPPPGetSystemParams(getID(), 5);
    }

    @Override // com.easyview.basecamera.ICamera
    public void startRecord(ICamera.IRespondListener listener) {
        this._startRecordTime = 0;
        this._record_duration = 0;
        NativeCaller.PPPPGetSystemParams(getID(), 54);
    }

    @Override // com.easyview.basecamera.ICamera
    public void stopRecord(ICamera.IRespondListener listener) {
        NativeCaller.PPPPGetSystemParams(getID(), 55);
    }

    @Override // com.easyview.basecamera.ICamera
    public void setLanguage(int lang, ICamera.IRespondListener listener) {
        try {
            byte[] data = EVCommandDefs.MakeSimpleReqPacket(EVCommandDefs.SET_LANGUAGE, lang);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void getExtThres(ICamera.IRespondListener listener) {
        this._extThresListener = listener;
        SimpleEVCommand(EVCommandDefs.GET_EXT_THRES);
    }

    @Override // com.easyview.basecamera.ICamera
    public void setExtThres(ICamera.IRespondListener listener) {
        this._extSetThresListener = listener;
        try {
            byte[] data = EVCommandDefs.MakeCommonReqPacket(EVCommandDefs.SET_EXT_THRES, this._extThres, EVCommandDefs.ExtThresholds.struct_size);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void playMusic(int index, ICamera.IRespondListener listener) {
        this._playAudioListener = listener;
        try {
            byte[] data = EVCommandDefs.MakeSimpleReqPacket(EVCommandDefs.PLAY_MUSIC, 1);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void stopMusic(int index, ICamera.IRespondListener listener) {
        try {
            byte[] data = EVCommandDefs.MakeSimpleReqPacket(EVCommandDefs.PLAY_MUSIC, 0);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void enablePairing(ICamera.IRespondListener listener) {
        try {
            byte[] data = EVCommandDefs.MakeSimpleReqPacket(EVCommandDefs.ENABLE_PAIRING, 1);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void setBrightness(int value, ICamera.IRespondListener listener) {
        this._brightness = value;
        NativeCaller.PPPPCameraControl(getID(), 1, value);
    }

    @Override // com.easyview.basecamera.ICamera
    public void setContrast(int value, ICamera.IRespondListener listener) {
        this._contrast = value;
        NativeCaller.PPPPCameraControl(getID(), 2, value);
    }

    @Override // com.easyview.basecamera.ICamera
    public void setShowOSD(int value, ICamera.IRespondListener listener) {
        this.planeParams.showDate = value;
        NativeCaller.PPPPCameraControl(getID(), 605, value);
    }

    @Override // com.easyview.basecamera.ICamera
    public void setRotate(int value, ICamera.IRespondListener listener) {
        this._rotate = value;
        NativeCaller.PPPPCameraControl(getID(), 5, value);
    }

    @Override // com.easyview.basecamera.ICamera
    public void RequestParams(ICamera.IRespondListener listener) {
        this._requestParamsListener = listener;
        setVideoQuality(30);
        NativeCaller.PPPPGetSystemParams(getID(), 2);
    }

    @Override // com.easyview.basecamera.ICamera
    public void setWiFiName(String value, ICamera.IRespondListener listener) {
        this.planeParams.WiFiName.setString(value);
        String cgi = String.format("set_wifi_ap.cgi?wifiname=%s&", value);
        NativeCaller.SendCommonCGI(getID(), cgi);
    }

    @Override // com.easyview.basecamera.ICamera
    public int getBrightness() {
        return this._brightness;
    }

    @Override // com.easyview.basecamera.ICamera
    public int getContrast() {
        return this._contrast;
    }

    @Override // com.easyview.basecamera.ICamera
    public int getShowOSD() {
        return this.planeParams.showDate;
    }

    @Override // com.easyview.basecamera.ICamera
    public int getRotate() {
        return this._rotate;
    }

    @Override // com.easyview.basecamera.ICamera
    public String getWiFiName() {
        String wifiName = this.planeParams.WiFiName.toString();
        Log.i("ppcs", "wifiName" + wifiName);
        return wifiName;
    }

    @Override // com.easyview.basecamera.ICamera
    public String getDefaultWiFiName() {
        String wifiName = this.planeParams.DefaultName.toString();
        return wifiName;
    }

    @Override // com.easyview.basecamera.ICamera
    public void queryEventList(int beginIndex, int endIndex, ICamera.IRespondListener listener) {
        this._queryEventsListener = listener;
        try {
            byte[] data = EVCommandDefs.MakeSimpleReqPacket(EVCommandDefs.GET_EVENT_LIST, beginIndex, endIndex);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void queryEvent(int index, String pathName, ICamera.IRespondListener listener) {
        this._queryEventListener = listener;
        Log.i("Event", "Query Event :" + index);
        try {
            byte[] data = EVCommandDefs.MakeSimpleReqPacket(EVCommandDefs.GET_EVENT, index);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void timing() {
        TimeZone timeZone = TimeZone.getDefault();
        int tz = timeZone.getRawOffset() / AVAPIs.TIME_SPAN_LOSED;
        Calendar calendar = Calendar.getInstance();
        int now = (int) (calendar.getTimeInMillis() / 1000);
        NativeCaller.PPPPDatetimeSetting(getID(), now, tz, 0, "");
    }

    @Override // com.easyview.basecamera.ICamera
    public void setRssi(int rssi) {
    }

    @Override // com.easyview.basecamera.ICamera
    public void setSensor(int value, ICamera.IRespondListener listener) {
        this._sensorres = value;
        if (value != 0) {
            String cgi = String.format("set_sensor_res.cgi?sensorres=%d&", Integer.valueOf(value));
            NativeCaller.SendCommonCGI(getID(), cgi);
        }
    }

    public void enableIOSwitch(ICamera.IRespondListener listener, int index) {
        try {
            byte[] data = EVCommandDefs.MakeSimpleReqPacket(EVCommandDefs.EV_COMMAND_OPEN_IOSWITCH, index);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    public void getIOSwitch(ICamera.IRespondListener listener) {
        this.ioSwitchParamsListener = listener;
        try {
            byte[] data = EVCommandDefs.MakeSimpleReqPacket(EVCommandDefs.EV_COMMAND_GET_IOSWITCH, new int[0]);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    public void setIOSwitch(ICamera.IRespondListener listener) {
        this.ioSwitchParamsListener = listener;
        Log.i(LOG_TAG, "setIOSwitch");
        try {
            byte[] data = EVCommandDefs.MakeIOSwitchParamsReqPacket(this.ioSwitchParams);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e2) {
            e2.printStackTrace();
        } catch (InvocationTargetException e3) {
            e3.printStackTrace();
        } catch (StructException e4) {
            e4.printStackTrace();
        }
    }

    public void queryLCDText(ICamera.IRespondListener listener) {
        this.lcdParamsListener = listener;
        try {
            byte[] data = EVCommandDefs.MakeSimpleReqPacket(EVCommandDefs.EV_COMMAND_LCD_CONTROL, 0);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    public void setLCDText(ICamera.IRespondListener listener) {
        this.lcdParamsListener = listener;
        try {
            this.lcdCtrlParam.cmd = 1;
            byte[] data = EVCommandDefs.MakeObjectReqPacket(EVCommandDefs.EV_COMMAND_LCD_CONTROL, this.lcdCtrlParam, 128);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void queryDeviceInfo(ICamera.IRespondListener listener) {
        this.queryDeviceInfoListener = listener;
        try {
            byte[] data = EVCommandDefs.MakeSimpleReqPacket(EVCommandDefs.QUERY_DEVINFO, new int[0]);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void upgradeDevice(ICamera.IRespondListener listener) {
        this.upgradeDeviceListener = listener;
        try {
            byte[] data = EVCommandDefs.MakeSimpleReqPacket(EVCommandDefs.UPGRADE_DEVICE, new int[0]);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void upgradeCheck(ICamera.IRespondListener listener) {
        this.upgradeCheckListener = listener;
        try {
            byte[] data = EVCommandDefs.MakeSimpleReqPacket(EVCommandDefs.UPGRADE_CHECK, new int[0]);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void setLightValue(int white, int yellow, int type, ICamera.IRespondListener listener) {
        try {
            byte[] data = EVCommandDefs.MakeSimpleReqPacket(EVCommandDefs.EV_COMMAND_LIGHT_CONTROL, 1, white, yellow, type);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void queryLightValue(ICamera.IRespondListener listener) {
        this.queryLightValueListener = listener;
        try {
            byte[] data = EVCommandDefs.MakeSimpleReqPacket(EVCommandDefs.EV_COMMAND_LIGHT_CONTROL, 0);
            NativeCaller.EVCommand(getID(), data, data.length);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }
}
