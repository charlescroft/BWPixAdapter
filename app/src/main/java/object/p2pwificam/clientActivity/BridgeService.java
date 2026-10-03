package object.p2pwificam.clientActivity;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;
import com.easyview.bean.AlermBean;
import com.easyview.ndt.LogUtil;
import com.easyview.ppcs.PPCSCamera;
import com.tencent.android.tpush.common.Constants;
import java.text.SimpleDateFormat;
import java.util.Date;
import object.easyview.bwpix.R;
import object.p2pipcam.content.ContentCommon;
import object.p2pipcam.nativecaller.NativeCaller;
import object.p2pipcam.system.SystemValue;
import object.p2pipcam.utils.DataBaseHelper;
import object.p2pipcam.utils.Pub;

/* loaded from: classes.dex */
public class BridgeService extends Service {
    private static AddCameraInterface addCameraInterface;
    private static AlarmInterface alarmInterface;
    private static DateTimeInterface dateTimeInterface;
    private static EVCommandInterface evCommandInterface;
    private static FtpInterface ftpInterface;
    private static IpcamClientInterface ipcamClientInterface;
    private static final java.util.concurrent.CopyOnWriteArrayList<IpcamClientInterface> ipcamClientListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static MailInterface mailInterface;
    private static OpenLockInterface openLockInterface;
    private static PictureInterface pictureInterface;
    private static PlayBackInterface playBackInterface;
    private static PlayBackTFInterface playBackTFInterface;
    private static PlayInterface playInterface;
    private static SDCardInterface sCardInterface;

    public interface AlarmNotifyListener {
        void onAlarmNotify(String did, int alarmType, int beginTime, int endTime, int index);
    }

    private static final java.util.concurrent.CopyOnWriteArrayList<AlarmNotifyListener> alarmNotifyListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void addAlarmNotifyListener(AlarmNotifyListener l) {
        if (l != null && !alarmNotifyListeners.contains(l)) {
            alarmNotifyListeners.add(l);
        }
    }

    public static void removeAlarmNotifyListener(AlarmNotifyListener l) {
        if (l != null) {
            alarmNotifyListeners.remove(l);
        }
    }
    private static UserInterface userInterface;
    private static VideoInterface videoInterface;
    private static WifiInterface wifiInterface;
    private NotificationManager mCustomMgr;
    private Notification mNotify2;
    private NotificationManager ntfManager;
    private SharedPreferences preference;
    public static KeyguardManager.KeyguardLock mKeyguardLock = null;
    public static Boolean lockFlag = false;
    private String TAG = BridgeService.class.getSimpleName();
    private DataBaseHelper helper = null;
    private KeyguardManager mKeyguardManager = null;

    public interface AddCameraInterface {
        void callBackSearchResultData(int i, String str, String str2, String str3, String str4, int i2);
    }

    public interface AlarmInterface {
        void callBackAlarmParams(String str, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11, int i12, int i13, int i14, int i15, int i16, int i17, int i18, int i19, int i20, int i21, int i22, int i23, int i24, int i25, int i26, int i27, int i28, int i29, int i30, int i31, int i32, int i33);

        void callBackSetSystemParamsResult(String str, int i, int i2);
    }

    public interface DateTimeInterface {
        void callBackDatetimeParams(String str, int i, int i2, int i3, String str2);

        void callBackSetSystemParamsResult(String str, int i, int i2);
    }

    public interface EVCommandInterface {
        void OnEVCommand(String str, byte[] bArr, int i);
    }

    public interface FtpInterface {
        void callBackFtpParams(String str, String str2, String str3, String str4, String str5, int i, int i2, int i3);

        void callBackSetSystemParamsResult(String str, int i, int i2);
    }

    public interface IpcamClientInterface {
        void BSMsgNotifyData(String str, int i, int i2);

        void BSSnapshotNotify(String str, byte[] bArr, int i);

        void callBackUserParams(String str, String str2, String str3, String str4, String str5, String str6, String str7);
    }

    public interface MailInterface {
        void callBackMailParams(String str, String str2, int i, String str3, String str4, int i2, String str5, int i3, String str6, String str7, String str8, String str9);

        void callBackSetSystemParamsResult(String str, int i, int i2);
    }

    public interface OpenLockInterface {
        void callBackpenLockParams(String str, byte[] bArr, int i);
    }

    public interface PictureInterface {
        void BSMsgNotifyData(String str, int i, int i2);
    }

    public interface PlayBackInterface {
        void callBackAudioData(byte[] bArr, int i);

        void callBackMessageNotify(String str, int i, int i2);

        void callBackPlaybackVideoData(byte[] bArr, int i, int i2, int i3, int i4, int i5);
    }

    public interface PlayBackTFInterface {
        void callBackRecordFileSearchResult(String str, String str2, int i, int i2);
    }

    public interface PlayInterface {
        void callBaceVideoData(byte[] bArr, int i, int i2, int i3, int i4);

        void callBackAudioData(byte[] bArr, int i);

        void callBackCameraParamNotify(String str, int i, int i2, int i3, int i4, int i5, int i6);

        void callBackH264Data(byte[] bArr, int i, int i2);

        void callBackMessageNotify(String str, int i, int i2);
    }

    public interface SDCardInterface {
        void callBackRecordSchParams(String str, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11, int i12, int i13, int i14, int i15, int i16, int i17, int i18, int i19, int i20, int i21, int i22, int i23, int i24, int i25, int i26, int i27, int i28, int i29);

        void callBackSetSystemParamsResult(String str, int i, int i2);
    }

    public interface UserInterface {
        void callBackPPPPMsgNotifyData(String str, int i, int i2);

        void callBackSetSystemParamsResult(String str, int i, int i2);

        void callBackUserParams(String str, String str2, String str3, String str4, String str5, String str6, String str7);
    }

    public interface VideoInterface {
        void BSMsgNotifyData(String str, int i, int i2);
    }

    public interface WifiInterface {
        void callBackPPPPMsgNotifyData(String str, int i, int i2);

        void callBackSetSystemParamsResult(String str, int i, int i2);

        void callBackWifiParams(String str, int i, String str2, int i2, int i3, int i4, int i5, int i6, int i7, String str3, String str4, String str5, String str6, int i8, int i9, int i10, int i11, String str7);

        void callBackWifiScanResult(String str, String str2, String str3, int i, int i2, int i3, int i4, int i5, int i6);
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        Log.d("tag", "BridgeService onBind()");
        return new ControllerBinder();
    }

    class ControllerBinder extends Binder {
        ControllerBinder() {
        }

        public BridgeService getBridgeService() {
            return BridgeService.this;
        }
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        SystemValue.ISRUN = true;
        Log.d("tagx", "BridgeService onCreate()" + SystemValue.ISRUN);
        this.mCustomMgr = (NotificationManager) getSystemService("notification");
        this.helper = DataBaseHelper.getInstance(this);
        NativeCaller.PPPPSetCallbackContext(this);
        Log.d("tagx", "NativeCaller.PPPPSetCallbackContext");
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int flags, int startId) {
        return 2;
    }

    @Override // android.app.Service
    public void onDestroy() {
        SystemValue.ISRUN = false;
        super.onDestroy();
        this.mCustomMgr.cancel(R.drawable.app);
        LogUtil.d("tagx", "BridgeService onDestroy()==" + SystemValue.ISRUN);
        if (this.helper != null) {
            this.helper = null;
        }
    }

    private void CallBack_VideoData(String did, byte[] videobuf, int h264Data, int len, int width, int height) {
        Log.i("VideoData", "CallBack_VideoData did=" + did + " len=" + len + " w=" + width + " h=" + height);
        if (playInterface != null) {
            playInterface.callBaceVideoData(videobuf, h264Data, len, width, height);
        }
        com.easyview.camera.CameraApplication app = (com.easyview.camera.CameraApplication) getApplication();
        PPCSCamera camera = (PPCSCamera) app.getCameraList().getCamera(did);
        if (camera != null) {
            camera.OnYUVData(videobuf, len, width, height);
        }
    }

    private void CallBack_MessageNotify(String did, int msgType, int param) {
        if (playInterface != null) {
            playInterface.callBackMessageNotify(did, msgType, param);
        }
        if (playBackInterface != null) {
            playBackInterface.callBackMessageNotify(did, msgType, param);
        }
    }

    private void CallBack_AudioData(String did, byte[] pcm, int len) {
        if (playInterface != null) {
            playInterface.callBackAudioData(pcm, len);
        }
    }

    private void CallBack_EVCommand(String did, byte[] data, int len) {
        com.easyview.camera.CameraApplication app = (com.easyview.camera.CameraApplication) getApplication();
        PPCSCamera camera = (PPCSCamera) app.getCameraList().getCamera(did);
        if (camera != null) {
            camera.OnEVCommand(data, len);
        }
    }

    private void CallBack_DoorBellCmd(String did, byte[] data, int len) {
        if (openLockInterface != null) {
            openLockInterface.callBackpenLockParams(did, data, len);
        }
    }

    private void CallBack_Command(String did, byte[] data, int len, int type) {
        com.easyview.camera.CameraApplication app = (com.easyview.camera.CameraApplication) getApplication();
        PPCSCamera camera = (PPCSCamera) app.getCameraList().getCamera(did);
        if (camera != null) {
            camera.OnCommand(type, data, len);
        }
    }

    private void CallBack_CustomData(String did, byte[] data, int len, int type) {
        com.easyview.camera.CameraApplication app = (com.easyview.camera.CameraApplication) getApplication();
        PPCSCamera camera = (PPCSCamera) app.getCameraList().getCamera(did);
        if (camera != null) {
            camera.OnCustomData(type, data, len);
        }
    }

    private void CallBack_PPPPMsgNotify(String did, int type, int param) {
        LogUtil.d("P2P", "PPPPMsgNotify  did:" + did + " type:" + type + " param:" + param);
        com.easyview.camera.CameraApplication app = (com.easyview.camera.CameraApplication) getApplication();
        PPCSCamera camera = (PPCSCamera) app.getCameraList().getCamera(did);
        if (camera != null) {
            camera.OnNotify(type, param);
        }
        if (ipcamClientInterface != null) {
            ipcamClientInterface.BSMsgNotifyData(did, type, param);
        }
        for (IpcamClientInterface listener : ipcamClientListeners) {
            try {
                listener.BSMsgNotifyData(did, type, param);
            } catch (Exception ignored) {}
        }
        if (wifiInterface != null) {
            wifiInterface.callBackPPPPMsgNotifyData(did, type, param);
        }
        if (userInterface != null) {
            userInterface.callBackPPPPMsgNotifyData(did, type, param);
        }
    }

    public void CallBack_SearchResult(int cameraType, String strMac, String strName, String strDeviceID, String strIpAddr, int port) {
        Log.d(this.TAG, "SearchResult: " + strIpAddr + " " + port);
        if (strDeviceID.length() != 0 && addCameraInterface != null) {
            addCameraInterface.callBackSearchResultData(cameraType, strMac, strName, strDeviceID, strIpAddr, port);
        }
    }

    public void CallBack_SetSystemParamsResult(String did, int paramType, int result) {
        switch (paramType) {
            case 10:
                if (userInterface != null) {
                    userInterface.callBackSetSystemParamsResult(did, paramType, result);
                    break;
                }
                break;
            case 11:
                if (wifiInterface != null) {
                    wifiInterface.callBackSetSystemParamsResult(did, paramType, result);
                    break;
                }
                break;
            case 12:
                if (dateTimeInterface != null) {
                    Log.d(this.TAG, "user result:" + result + " paramType:" + paramType);
                    dateTimeInterface.callBackSetSystemParamsResult(did, paramType, result);
                    break;
                }
                break;
            case 16:
                if (mailInterface != null) {
                    mailInterface.callBackSetSystemParamsResult(did, paramType, result);
                    break;
                }
                break;
            case 17:
                if (ftpInterface != null) {
                    ftpInterface.callBackSetSystemParamsResult(did, paramType, result);
                    break;
                }
                break;
            case 18:
                if (alarmInterface != null) {
                    alarmInterface.callBackSetSystemParamsResult(did, paramType, result);
                    break;
                }
                break;
            case 28:
                if (sCardInterface != null) {
                    sCardInterface.callBackSetSystemParamsResult(did, paramType, result);
                    break;
                }
                break;
        }
    }

    public void CallBack_CameraParams(String did, int resolution, int brightness, int contrast, int hue, int saturation, int flip, int frame, int mode) {
        Log.d("ddd", "CallBack_CameraParams");
        com.easyview.camera.CameraApplication app = (com.easyview.camera.CameraApplication) getApplication();
        PPCSCamera camera = (PPCSCamera) app.getCameraList().getCamera(did);
        if (camera != null) {
            camera.onCameraParams(resolution, brightness, contrast, hue, saturation, flip, frame, mode);
        }
        if (playInterface != null) {
            playInterface.callBackCameraParamNotify(did, resolution, brightness, contrast, hue, saturation, flip);
        }
    }

    public void CallBack_WifiParams(String did, int enable, String ssid, int channel, int mode, int authtype, int encryp, int keyformat, int defkey, String key1, String key2, String key3, String key4, int key1_bits, int key2_bits, int key3_bits, int key4_bits, String wpa_psk) {
        Log.d("ddd", "CallBack_WifiParams");
        if (wifiInterface != null) {
            wifiInterface.callBackWifiParams(did, enable, ssid, channel, mode, authtype, encryp, keyformat, defkey, key1, key2, key3, key4, key1_bits, key2_bits, key3_bits, key4_bits, wpa_psk);
        }
    }

    public void CallBack_UserParams(String did, String user1, String pwd1, String user2, String pwd2, String user3, String pwd3) {
        if (userInterface != null) {
            userInterface.callBackUserParams(did, user1, pwd1, user2, pwd2, user3, pwd3);
        }
        if (ipcamClientInterface != null) {
            ipcamClientInterface.callBackUserParams(did, user1, pwd1, user2, pwd2, user3, pwd3);
        }
    }

    public void CallBack_FtpParams(String did, String svr_ftp, String user, String pwd, String dir, int port, int mode, int upload_interval) {
        if (ftpInterface != null) {
            ftpInterface.callBackFtpParams(did, svr_ftp, user, pwd, dir, port, mode, upload_interval);
        }
    }

    public void CallBack_DDNSParams(String did, int service, String user, String pwd, String host, String proxy_svr, int ddns_mode, int proxy_port) {
        Log.d("ddd", "CallBack_DDNSParams");
    }

    public void CallBack_MailParams(String did, String svr, int port, String user, String pwd, int ssl, String sender, int auth, String receiver1, String receiver2, String receiver3, String receiver4) {
        if (mailInterface != null) {
            mailInterface.callBackMailParams(did, svr, port, user, pwd, ssl, sender, auth, receiver1, receiver2, receiver3, receiver4);
        }
    }

    public void CallBack_DatetimeParams(String did, int now, int tz, int ntp_enable, String ntp_svr) {
        if (dateTimeInterface != null) {
            dateTimeInterface.callBackDatetimeParams(did, now, tz, ntp_enable, ntp_svr);
        }
    }

    private void PPPPSnapshotNotify(String did, byte[] bImage, int len) {
        Log.d(this.TAG, "did:" + did + " len:" + len);
        if (ipcamClientInterface != null) {
            ipcamClientInterface.BSSnapshotNotify(did, bImage, len);
        }
    }

    public void CallBack_Snapshot(String did, byte[] data, int len) {
        Log.d("ddd", "CallBack_Snapshot");
        if (ipcamClientInterface != null) {
            ipcamClientInterface.BSSnapshotNotify(did, data, len);
        }
    }

    public void CallBack_NetworkParams(String did, String ipaddr, String netmask, String gateway, String dns1, String dns2, int dhcp, int port, int rtsport) {
        Log.d("ddd", "CallBack_NetworkParams");
    }

    public void CallBack_CameraStatusParams(String did, String sysver, String devname, String devid, int alarmstatus, int sdcardstatus, int sdcardtotalsize, int sdcardremainsize) {
        Log.d("ddd", String.format("CallBack_CameraStatusParams sdcardtotalsize:%d sdcardremainsize:%d", Integer.valueOf(sdcardtotalsize), Integer.valueOf(sdcardremainsize)));
    }

    public void CallBack_PTZParams(String did, int led_mod, int ptz_center_onstart, int ptz_run_times, int ptz_patrol_rate, int ptz_patrul_up_rate, int ptz_patrol_down_rate, int ptz_patrol_left_rate, int ptz_patrol_right_rate, int disable_preset) {
        Log.d("ddd", "CallBack_PTZParams");
    }

    public void CallBack_WifiScanResult(String did, String ssid, String mac, int security, int dbm0, int dbm1, int mode, int channel, int bEnd) {
        Log.d("tag", "CallBack_WifiScanResult");
        if (wifiInterface != null) {
            wifiInterface.callBackWifiScanResult(did, ssid, mac, security, dbm0, dbm1, mode, channel, bEnd);
        }
    }

    public void CallBack_AlarmParams(String did, int motion_armed, int motion_sensitivity, int input_armed, int ioin_level, int iolinkage, int ioout_level, int alarmpresetsit, int mail, int snapshot, int record, int upload_interval, int schedule_enable, int schedule_sun_0, int schedule_sun_1, int schedule_sun_2, int schedule_mon_0, int schedule_mon_1, int schedule_mon_2, int schedule_tue_0, int schedule_tue_1, int schedule_tue_2, int schedule_wed_0, int schedule_wed_1, int schedule_wed_2, int schedule_thu_0, int schedule_thu_1, int schedule_thu_2, int schedule_fri_0, int schedule_fri_1, int schedule_fri_2, int schedule_sat_0, int schedule_sat_1, int schedule_sat_2) {
        com.easyview.camera.CameraApplication app = (com.easyview.camera.CameraApplication) getApplication();
        PPCSCamera camera = (PPCSCamera) app.getCameraList().getCamera(did);
        if (camera != null) {
            AlermBean alermBean = camera.getAlarm();
            alermBean.setDid(did);
            alermBean.setMotion_armed(motion_armed);
            alermBean.setMotion_sensitivity(motion_sensitivity);
            alermBean.setInput_armed(input_armed);
            alermBean.setIoin_level(ioin_level);
            alermBean.setIolinkage(iolinkage);
            alermBean.setIoout_level(ioout_level);
            alermBean.setAlermpresetsit(alarmpresetsit);
            alermBean.setMail(mail);
            alermBean.setSnapshot(snapshot);
            alermBean.setRecord(record);
            alermBean.setUpload_interval(upload_interval);
            alermBean.setSchedule_enable(1);
            alermBean.setSchedule_sun_0(-1);
            alermBean.setSchedule_sun_1(-1);
            alermBean.setSchedule_sun_2(-1);
            alermBean.setSchedule_mon_0(-1);
            alermBean.setSchedule_mon_1(-1);
            alermBean.setSchedule_mon_2(-1);
            alermBean.setSchedule_thu_0(-1);
            alermBean.setSchedule_thu_1(-1);
            alermBean.setSchedule_thu_2(-1);
            alermBean.setSchedule_wed_0(-1);
            alermBean.setSchedule_wed_1(-1);
            alermBean.setSchedule_wed_2(-1);
            alermBean.setSchedule_tue_0(-1);
            alermBean.setSchedule_tue_1(-1);
            alermBean.setSchedule_tue_2(-1);
            alermBean.setSchedule_fri_0(-1);
            alermBean.setSchedule_fri_1(-1);
            alermBean.setSchedule_fri_2(-1);
            alermBean.setSchedule_sat_0(-1);
            alermBean.setSchedule_sat_1(-1);
            alermBean.setSchedule_sat_2(-1);
        }
        if (alarmInterface != null) {
            alarmInterface.callBackAlarmParams(did, motion_armed, motion_sensitivity, input_armed, ioin_level, iolinkage, ioout_level, alarmpresetsit, mail, snapshot, record, upload_interval, schedule_enable, schedule_sun_0, schedule_sun_1, schedule_sun_2, schedule_mon_0, schedule_mon_1, schedule_mon_2, schedule_tue_0, schedule_tue_1, schedule_tue_2, schedule_wed_0, schedule_wed_1, schedule_wed_2, schedule_thu_0, schedule_thu_1, schedule_thu_2, schedule_fri_0, schedule_fri_1, schedule_fri_2, schedule_sat_0, schedule_sat_1, schedule_sat_2);
        }
    }

    public void SaveAlarm(String did, int position) {
        Boolean flag = false;
        String alarm = null;
        Cursor cursor = this.helper.fetchAllAlarmCameras();
        if (cursor != null) {
            while (cursor.moveToNext() && !flag.booleanValue()) {
                String id = cursor.getString(1);
                Log.v("BridgeService", "id  ===" + id);
                alarm = cursor.getString(position);
                if (did.equals(id)) {
                    flag = true;
                }
            }
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.v("BridgeService", "motionAlarm  ===" + alarm);
        if (alarm == null) {
            alarm = "0";
        }
        int num = Integer.parseInt(alarm);
        String alarm2 = String.valueOf(num + 1);
        Log.v("BridgeService", "motionAlarm  ===" + alarm2);
        this.helper.updateCameraAlarm(did, alarm2, position);
    }

    private static boolean isRunningForeground(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Constants.FLAG_ACTIVITY_NAME);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        String currentPackageName = cn.getPackageName();
        Log.v("cameraserver", "currentPackageName  =====" + currentPackageName);
        return !TextUtils.isEmpty(currentPackageName) && currentPackageName.equals("object.easyview.ebell");
    }

    private boolean OpenLockScreen(String did, int alarmNum, int mode) {
        this.preference = getSharedPreferences("isFirst", 0);
        Boolean activity = Boolean.valueOf(this.preference.getBoolean(Constants.FLAG_ACTIVITY_NAME, false));
        Log.d("BridgeService", "callBack_AlarmNotify did:==========11111111111111111111111111111111111111");
        Log.v("BridgeService", "activity  ===" + activity);
        SaveAlarm(did, alarmNum);
        Log.d("BridgeService", "isRunningForeground(getApplicationContext());==" + isRunningForeground(getApplicationContext()));
        if (!isRunningForeground(getApplicationContext()) && activity.booleanValue()) {
            activity = false;
        }
        if (activity.booleanValue()) {
            return false;
        }
        this.preference = getSharedPreferences("isFirst", 0);
        Intent in = new Intent();
        if (mode == 34) {
            in.setClass(this, CallVideoActivity.class);
        } else if (mode == 40) {
            in.setClass(this, CallVideoActivity.class);
        }
        in.putExtra(ContentCommon.STR_CAMERA_TYPE, 1);
        in.putExtra(ContentCommon.STR_STREAM_TYPE, 3);
        in.putExtra(ContentCommon.STR_CAMERA_NAME, "IPCAM");
        in.putExtra(ContentCommon.STR_CAMERA_ID, did);
        in.putExtra(ContentCommon.STR_CAMERA_USER, "admin");
        in.putExtra(ContentCommon.STR_CAMERA_PWD, "admin");
        in.putExtra(ContentCommon.STR_CAMERA_TYPE, 1);
        in.putExtra("play_type", 1);
        in.putExtra(ContentCommon.STR_PLAY_MODE, mode);
        in.addFlags(268435456);
        startActivity(in);
        return true;
    }

    public void CallBack_AlarmNotify(String did, int alarmType, int beginTime, int endTime, int value, int index, int fileID) {
        LogUtil.i("P2P", "AlarmNotify did:" + did + " alarmtype:" + alarmType);
        com.easyview.camera.CameraApplication app = (com.easyview.camera.CameraApplication) getApplication();
        PPCSCamera camera = (PPCSCamera) app.getCameraList().getCamera(did);
        if (alarmType != 513 && alarmType != 514) {
            if (fileID == 0) {
                fileID = camera.GetEventFileID();
            }
            LogUtil.i("P2P", String.format("AlarmNotify %s index:%d type:%d time:%d file_id:%d", did, Integer.valueOf(index), Integer.valueOf(alarmType), Integer.valueOf(beginTime), Integer.valueOf(fileID)));
            Pub.onEvent(this, did, index, alarmType, value, beginTime, fileID);
        }
        for (AlarmNotifyListener l : alarmNotifyListeners) {
            try {
                l.onAlarmNotify(did, alarmType, beginTime, endTime, index);
            } catch (Exception ignored) {}
        }
    }

    private void CallBack_RecordFileSearchResult(String did, String filename, int nFileSize, int nRecordCount, int nPageCount, int nPageIndex, int nPageSize, int bEnd) {
        Log.d(this.TAG, "CallBack_RecordFileSearchResult did: " + did + " filename: " + filename + " size: " + nFileSize);
        if (playBackTFInterface != null) {
            playBackTFInterface.callBackRecordFileSearchResult(did, filename, nFileSize, bEnd);
        }
    }

    private void CallBack_PlaybackVideoData(String did, byte[] videobuf, int h264Data, int len, int width, int height, int timestamp) {
        Log.d(this.TAG, "CallBack_PlaybackVideoData  len:" + len + " width:" + width + " height:" + height);
        if (playBackInterface != null) {
            playBackInterface.callBackPlaybackVideoData(videobuf, h264Data, len, width, height, timestamp);
        }
    }

    private void CallBack_H264Data(String did, byte[] h264, int type, int size) {
        if (size > 0 && h264 != null) {
            String headHex = "";
            for (int k = 0; k < Math.min(16, h264.length); k++) {
                headHex += String.format("%02x ", h264[k]);
            }
            Log.i("VideoH264", "CallBack_H264Data did=" + did + " size=" + size + " type=" + type + " head=" + headHex);
        }
        com.easyview.camera.CameraApplication app = (com.easyview.camera.CameraApplication) getApplication();
        PPCSCamera camera = (PPCSCamera) app.getCameraList().getCamera(did);
        if (camera != null) {
            camera.OnVideoData(h264, size);
        }
        if (playInterface != null) {
            playInterface.callBackH264Data(h264, type, size);
        }
    }

    public void CallBack_RecordSchParams(String did, int record_cover_enable, int record_timer, int record_size, int record_time_enable, int record_schedule_sun_0, int record_schedule_sun_1, int record_schedule_sun_2, int record_schedule_mon_0, int record_schedule_mon_1, int record_schedule_mon_2, int record_schedule_tue_0, int record_schedule_tue_1, int record_schedule_tue_2, int record_schedule_wed_0, int record_schedule_wed_1, int record_schedule_wed_2, int record_schedule_thu_0, int record_schedule_thu_1, int record_schedule_thu_2, int record_schedule_fri_0, int record_schedule_fri_1, int record_schedule_fri_2, int record_schedule_sat_0, int record_schedule_sat_1, int record_schedule_sat_2, int record_sd_status, int sdtotal, int sdfree, int record_mode) {
        if (sCardInterface != null) {
            sCardInterface.callBackRecordSchParams(did, record_cover_enable, record_timer, record_size, record_time_enable, record_schedule_sun_0, record_schedule_sun_1, record_schedule_sun_2, record_schedule_mon_0, record_schedule_mon_1, record_schedule_mon_2, record_schedule_tue_0, record_schedule_tue_1, record_schedule_tue_2, record_schedule_wed_0, record_schedule_wed_1, record_schedule_wed_2, record_schedule_thu_0, record_schedule_thu_1, record_schedule_thu_2, record_schedule_fri_0, record_schedule_fri_1, record_schedule_fri_2, record_schedule_sat_0, record_schedule_sat_1, record_schedule_sat_2, record_sd_status, sdtotal, sdfree, record_mode);
        }
    }

    private Notification getNotification(String content, String did, boolean isAlarm) {
        String titlePrompt;
        String title;
        Intent intent;
        Date date = new Date();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String strDate = f.format(date);
        if (isAlarm) {
            boolean flag = false;
            String CamName = "";
            Cursor cursor = this.helper.fetchAllCameras();
            if (cursor != null) {
                while (cursor.moveToNext() && !flag) {
                    String name = cursor.getString(1);
                    String id = cursor.getString(2);
                    cursor.getString(3);
                    cursor.getString(4);
                    Log.d("tag", "notification  name:" + name + " id:" + id);
                    if (did.equals(id)) {
                        CamName = name;
                        flag = true;
                    }
                }
                if (cursor != null) {
                    cursor.close();
                }
            }
            if (!flag) {
                return null;
            }
            Log.d("tag", "alarm name:" + CamName);
            this.helper.insertAlarmLogToDB(did, content, strDate);
            titlePrompt = String.valueOf(CamName) + " " + content;
            intent = new Intent(this, (Class<?>) EventDetailActivity.class);
            intent.putExtra(ContentCommon.STR_CAMERA_ID, did);
            intent.putExtra(ContentCommon.STR_CAMERA_NAME, CamName);
            intent.putExtra("log_content", content);
            title = CamName;
        } else {
            titlePrompt = String.valueOf(getResources().getString(R.string.app_name)) + " " + content;
            title = getResources().getString(R.string.app_name);
            intent = new Intent("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.LAUNCHER");
            intent.setClass(this, StartActivity.class);
        }
        this.mNotify2 = new Notification(R.drawable.app, titlePrompt, System.currentTimeMillis());
        this.mNotify2.flags |= 16;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, R.drawable.app, intent, 134217728);
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification_layout);
        this.mNotify2.contentIntent = pendingIntent;
        this.mNotify2.contentView = views;
        this.mNotify2.contentView.setTextViewText(R.id.no_title, title);
        this.mNotify2.contentView.setTextViewText(R.id.no_content, content);
        if (isAlarm) {
            this.mNotify2.contentView.setTextViewText(R.id.no_time, strDate);
        }
        this.mNotify2.contentView.setImageViewResource(R.id.no_img, R.drawable.app);
        if (isAlarm) {
            this.mNotify2.defaults = -1;
            this.mCustomMgr.notify(0, this.mNotify2);
        }
        return this.mNotify2;
    }

    public static void addIpcamClientInterface(IpcamClientInterface listener) {
        if (listener != null && !ipcamClientListeners.contains(listener)) {
            ipcamClientListeners.add(listener);
        }
    }

    public static void removeIpcamClientInterface(IpcamClientInterface listener) {
        if (listener != null) {
            ipcamClientListeners.remove(listener);
        }
    }

    public static void setIpcamClientInterface(IpcamClientInterface ipcInterface) {
        ipcamClientInterface = ipcInterface;
    }

    public static void setPictureInterface(PictureInterface pi) {
        pictureInterface = pi;
    }

    public static void setVideoInterface(VideoInterface vi) {
        videoInterface = vi;
    }

    public static void setWifiInterface(WifiInterface wi) {
        wifiInterface = wi;
    }

    public static void setUserInterface(UserInterface ui) {
        userInterface = ui;
    }

    public static void setAlarmInterface(AlarmInterface ai) {
        alarmInterface = ai;
    }

    public static void setOpenLockInterface(OpenLockInterface ai) {
        openLockInterface = ai;
    }

    public static void setDateTimeInterface(DateTimeInterface di) {
        dateTimeInterface = di;
    }

    public static void setMailInterface(MailInterface mi) {
        mailInterface = mi;
    }

    public static void setFtpInterface(FtpInterface fi) {
        ftpInterface = fi;
    }

    public static void setSDCardInterface(SDCardInterface si) {
        sCardInterface = si;
    }

    public static void setPlayInterface(PlayInterface pi) {
        playInterface = pi;
    }

    public static void setPlayBackTFInterface(PlayBackTFInterface pbtfi) {
        playBackTFInterface = pbtfi;
    }

    public static void setPlayBackInterface(PlayBackInterface pbi) {
        playBackInterface = pbi;
    }

    public static void setAddCameraInterface(AddCameraInterface aci) {
        addCameraInterface = aci;
    }

    public static void setEVCommandInterface(EVCommandInterface pbi) {
        evCommandInterface = pbi;
    }
}
