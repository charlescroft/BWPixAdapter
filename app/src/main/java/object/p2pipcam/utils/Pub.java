package object.p2pipcam.utils;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.media.MediaExtractor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.view.Display;
import android.widget.RemoteViews;
import com.easyview.basecamera.BaseCamera;
import com.easyview.common.WifiUtils;
import com.easyview.ndt.LogUtil;
import com.easyview.struct.EVCommandDefs;
import com.tencent.android.tpush.common.Constants;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import object.easyview.bwpix.R;
import object.p2pipcam.content.ContentCommon;
import object.p2pwificam.clientActivity.CallVideoActivity;
import object.p2pwificam.clientActivity.EventDetailActivity;
import object.p2pwificam.clientActivity.StartActivity;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

/* loaded from: classes.dex */
public class Pub {
    private static Context _context;
    private static Map<Integer, String> _mapEventType = new HashMap();
    private static Map<Integer, Integer> _mapEventRes = new HashMap();
    private static int _appType = 0;
    private static boolean _save_log_file = false;
    private static int _auto_run = 0;
    private static int _set_wifi = 0;
    private static String _set_wifi_id = "";
    private static boolean _isReverseLandscape = false;
    private static boolean isDebug = false;

    public static void load(Context c) {
        _context = c;
        String appType = c.getString(R.string.app_type);
        _appType = Integer.parseInt(appType);
        initEventTypeText(c);
        initEventTypeRes(c);
        setAutoRun(c, 1);
    }

    public static Context getContext() {
        return _context;
    }

    public static boolean isAppOnForeground(Context context) {
        String packageName = context.getPackageName();
        ActivityManager manager = (ActivityManager) context.getSystemService(Constants.FLAG_ACTIVITY_NAME);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = manager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(packageName) && appProcess.importance == 100) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBell(Context c) {
        String appType = c.getString(R.string.app_type);
        _appType = Integer.parseInt(appType);
        return _appType == 1;
    }

    public static boolean isBell() {
        return _appType == 1;
    }

    public static boolean isDrone(Context c) {
        String appType = c.getString(R.string.app_type);
        _appType = Integer.parseInt(appType);
        return _appType == 2;
    }

    public static boolean isDrone() {
        return _appType == 2;
    }

    public static boolean isSupportExt() {
        return _appType == 3;
    }

    public static boolean isSupportBiVoice() {
        return false;
    }

    public static boolean isSupportApm() {
        return false;
    }

    public static String getCameraUser(Context c) {
        SharedPreferences preference = c.getSharedPreferences("camera_info", 0);
        return preference.getString(ContentCommon.STR_CAMERA_USER, "admin");
    }

    public static int get_ao_start_thred(Context c) {
        SharedPreferences preference = c.getSharedPreferences("system", 0);
        int val = preference.getInt("input_volume_limit", 6000);
        if (val > 50000 || val < 100) {
            return 6000;
        }
        return val;
    }

    public static void set_ao_start_thred(Context c, int val) {
        SharedPreferences preference = c.getSharedPreferences("system", 0);
        SharedPreferences.Editor editor = preference.edit();
        editor.putInt("input_volume_limit", val);
        editor.commit();
    }

    public static int get_ao_continue_thred(Context c) {
        SharedPreferences preference = c.getSharedPreferences("system", 0);
        int val = preference.getInt("output_volume_limit", 2000);
        if (val > 50000 || val < 100) {
            return 2000;
        }
        return val;
    }

    public static void set_ao_continue_thred(Context c, int val) {
        SharedPreferences preference = c.getSharedPreferences("system", 0);
        SharedPreferences.Editor editor = preference.edit();
        editor.putInt("output_volume_limit", val);
        editor.commit();
    }

    public static int get_event_reserve_days(Context c) {
        SharedPreferences preference = c.getSharedPreferences("system", 0);
        int val = preference.getInt("event_reserve_days", 30);
        return val;
    }

    public static void set_event_reserve_days(Context c, int val) {
        SharedPreferences preference = c.getSharedPreferences("system", 0);
        SharedPreferences.Editor editor = preference.edit();
        editor.putInt("event_reserve_days", val);
        editor.commit();
    }

    public static int get_event_max_count(Context c) {
        SharedPreferences preference = c.getSharedPreferences("system", 0);
        int val = preference.getInt("event_max_count", 200);
        return val;
    }

    public static void set_event_max_count(Context c, int val) {
        SharedPreferences preference = c.getSharedPreferences("system", 0);
        SharedPreferences.Editor editor = preference.edit();
        editor.putInt("event_max_count", val);
        editor.commit();
    }

    public static boolean get_event_notification(Context c) {
        SharedPreferences preference = c.getSharedPreferences("system", 0);
        boolean val = preference.getBoolean("event_notification", true);
        return val;
    }

    public static void set_event_event_notification(Context c, boolean val) {
        SharedPreferences preference = c.getSharedPreferences("system", 0);
        SharedPreferences.Editor editor = preference.edit();
        editor.putBoolean("event_notification", val);
        editor.commit();
    }

    public static boolean getSaveLogFile() {
        SharedPreferences preference = _context.getSharedPreferences("system", 0);
        boolean val = preference.getBoolean("save_log", false);
        return val;
    }

    public static boolean getSaveLogFile(Context context) {
        SharedPreferences preference = context.getSharedPreferences("system", 0);
        boolean val = preference.getBoolean("save_log", false);
        return val;
    }

    public static void setSaveLogFile(boolean val) {
        SharedPreferences preference = _context.getSharedPreferences("system", 0);
        SharedPreferences.Editor editor = preference.edit();
        editor.putBoolean("save_log", val);
        editor.commit();
    }

    public static int getAutoRun(Context c) {
        SharedPreferences preference = c.getSharedPreferences("system", 0);
        int val = preference.getInt("auto_run", 1);
        return val;
    }

    public static void setAcceptLicense(Context c, int val) {
        SharedPreferences preference = c.getSharedPreferences("system", 0);
        SharedPreferences.Editor editor = preference.edit();
        editor.putInt("accept_license", val);
        editor.commit();
    }

    public static int getAcceptLicense(Context c) {
        SharedPreferences preference = c.getSharedPreferences("system", 0);
        int val = preference.getInt("accept_license", 0);
        return val;
    }

    public static void setCallTime(Context c, String did, int time) {
        SharedPreferences preference = c.getSharedPreferences("system", 0);
        SharedPreferences.Editor editor = preference.edit();
        String key = String.format("CallTime_%s", did);
        editor.putInt(key, time);
        editor.commit();
    }

    public static int getCallTime(Context c, String did) {
        SharedPreferences preference = c.getSharedPreferences("system", 0);
        String key = String.format("CallTime_%s", did);
        int val = preference.getInt(key, 0);
        return val;
    }

    public static void setDeviceState(Context c, String did, int state) {
        SharedPreferences preference = c.getSharedPreferences("device", 0);
        SharedPreferences.Editor editor = preference.edit();
        editor.putInt(did, state);
        editor.commit();
    }

    public static int getDeviceState(Context c, String did) {
        SharedPreferences preference = c.getSharedPreferences("device", 0);
        int val = preference.getInt(did, 0);
        return val;
    }

    public static void setXGToken(Context c, String token) {
        SharedPreferences preference = c.getSharedPreferences("XG_PUSH", 0);
        SharedPreferences.Editor editor = preference.edit();
        editor.putString("TOKEN", token);
        editor.commit();
    }

    public static String getXGToken(Context c) {
        SharedPreferences preference = c.getSharedPreferences("XG_PUSH", 0);
        return preference.getString("TOKEN", "");
    }

    public static void setAutoRun(Context c, int val) {
        SharedPreferences preference = c.getSharedPreferences("system", 0);
        SharedPreferences.Editor editor = preference.edit();
        editor.putInt("auto_run", val);
        editor.commit();
    }

    public static String DisturbUID(String uid) {
        int len = uid.length();
        return String.valueOf(uid.substring(0, len - 5)) + "XXXXX";
    }

    public static int byte2int(byte[] b, int start) {
        return ((b[start + 3] & 255) << 24) + ((b[start + 2] & 255) << 16) + ((b[start + 1] & 255) << 8) + (b[start + 0] & 255);
    }

    public static short byte2short(byte[] b, int start) {
        int val = ((b[start + 1] & 255) << 8) + (b[start + 0] & 255);
        return (short) val;
    }

    public static void short2byte(short val, byte[] b, int start) {
        b[start + 0] = (byte) (val & 255);
        b[start + 1] = (byte) ((val >> 8) & 255);
    }

    public static int initEventTypeText(Context c) {
        _mapEventType.put(1, c.getString(R.string.light_alerm_motion_alarm));
        _mapEventType.put(2, c.getString(R.string.alerm_gpio_alarm));
        _mapEventType.put(34, c.getString(R.string.event_bell_call));
        _mapEventType.put(40, c.getString(R.string.event_remove));
        _mapEventType.put(48, c.getString(R.string.event_urgent_call));
        _mapEventType.put(257, c.getString(R.string.event_open_door));
        _mapEventType.put(Integer.valueOf(BaseCamera.EVENT_BEGIN_RECORD), c.getString(R.string.event_record));
        _mapEventType.put(32, c.getString(R.string.event_humidity));
        _mapEventType.put(33, c.getString(R.string.event_temperature));
        _mapEventType.put(39, c.getString(R.string.event_bodytemp));
        _mapEventType.put(258, c.getString(R.string.event_message));
        _mapEventType.put(259, c.getString(R.string.event_illegal_unlock));
        _mapEventType.put(7, c.getString(R.string.event_hover));
        _mapEventType.put(19, c.getString(R.string.event_lowvol));
        _mapEventType.put(18, c.getString(R.string.light_event_voice));
        return _mapEventType.size();
    }

    public static Map<Integer, String> getEventText() {
        return _mapEventType;
    }

    public static String getEventTypeText(int eventType) {
        return _mapEventType.containsKey(Integer.valueOf(eventType)) ? _mapEventType.get(Integer.valueOf(eventType)) : _mapEventType.get(1);
    }

    public static String getEventText(int eventType, int val) {
        String result = _mapEventType.get(1);
        if (_mapEventType.containsKey(Integer.valueOf(eventType))) {
            String result2 = _mapEventType.get(Integer.valueOf(eventType));
            result = result2;
        }
        if (val < 1000) {
            switch (eventType) {
                case 19:
                    break;
                case 32:
                    break;
                case 33:
                case 39:
                    break;
            }
            return result;
        }
        return result;
    }

    public static int initEventTypeRes(Context c) {
        _mapEventRes.put(1, Integer.valueOf(R.drawable.event_md));
        _mapEventRes.put(2, Integer.valueOf(R.drawable.event_io));
        _mapEventRes.put(34, Integer.valueOf(R.drawable.event_bell));
        _mapEventRes.put(40, Integer.valueOf(R.drawable.event_remove));
        _mapEventRes.put(48, Integer.valueOf(R.drawable.event_call));
        _mapEventRes.put(Integer.valueOf(BaseCamera.EVENT_END_RECORD), Integer.valueOf(R.drawable.event_rec));
        _mapEventRes.put(258, Integer.valueOf(R.drawable.event_message));
        _mapEventRes.put(7, Integer.valueOf(R.drawable.event_hover));
        return _mapEventRes.size();
    }

    public static int getEventRes(int eventType) {
        return _mapEventRes.containsKey(Integer.valueOf(eventType)) ? _mapEventRes.get(Integer.valueOf(eventType)).intValue() : _mapEventRes.get(2).intValue();
    }

    public static void set_auto_run() {
        _auto_run = 1;
    }

    public static int get_auto_run() {
        return _auto_run;
    }

    public static void setWifi(String id) {
        _set_wifi_id = id;
        _set_wifi = 1;
    }

    public static int getWifi(String id) {
        if (!id.equals(_set_wifi_id)) {
            return 0;
        }
        int result = _set_wifi;
        _set_wifi = 0;
        return result;
    }

    public static String getString(int rid) {
        return _context == null ? "" : _context.getString(rid);
    }

    public static int get_p2p_sn(BaseCamera camera) {
        String p2p = camera.getID();
        String[] s = p2p.split("-");
        if (s.length == 3) {
            return Integer.parseInt(s[1]);
        }
        return 1;
    }

    public static String getDIDNum(String did) {
        String[] s = did.split("-");
        if (s.length == 3) {
            return s[1];
        }
        return did;
    }

    @SuppressLint({"NewApi"})
    public static int showNotification(Context context, BaseCamera camera, int event_type, int event_value, int event_time) {
        Date date = new Date();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String strDate = f.format(date);
        String title = camera.getName();
        String content = getEventText(event_type, event_value);
        int id = get_p2p_sn(camera) + 1;
        boolean haveSound = true;
        int icon = R.drawable.app;
        Intent intent = new Intent(context, (Class<?>) StartActivity.class);
        if (event_type == 34 || event_type == 40) {
            intent.setClass(context, CallVideoActivity.class);
            id = event_time;
            icon = R.drawable.call_start;
            content = String.valueOf(content) + "...";
            haveSound = false;
        }
        int start_mode = 10;
        if (event_type > 32768) {
            intent.setClass(context, StartActivity.class);
            id = get_p2p_sn(camera);
            icon = R.drawable.missed_call;
            content = String.valueOf(context.getString(R.string.missed)) + getEventText(event_type - 32768, event_value);
            haveSound = false;
            start_mode = 0;
        }
        intent.putExtra("mode", start_mode);
        intent.putExtra("did", camera.getID());
        intent.setFlags(335544320);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 134217728);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.notification_layout);
        views.setTextViewText(R.id.no_title, title);
        views.setTextViewText(R.id.no_time, strDate);
        views.setImageViewResource(R.id.no_img, icon);
        views.setTextViewText(R.id.no_content, content);
        NotificationManager nm = (NotificationManager) context.getSystemService("notification");
        Notification.Builder builder = new Notification.Builder(context).setSmallIcon(icon).setContentIntent(pendingIntent).setContent(views);
        Notification noti = builder.build();
        if (haveSound || event_type > 32768) {
            noti.flags |= 16;
        }
        if (haveSound) {
            noti.sound = Uri.parse("android.resource://" + _context.getPackageName() + "/" + R.raw.bwalarm);
        }
        Log.i("Pub", "notify:" + id + " type:" + event_type);
        nm.notify(id, noti);
        return id;
    }

    public static void onEvent(Context context, String did, int event_index, int event_type, int event_record, int event_time, int file_id) {
        BaseCamera camera = DataBaseHelper.getCamera(context, did);
        if (camera != null) {
            if (event_index >= 0 && DataBaseHelper.SaveEvent(context, did, file_id, event_index, event_type, event_time, event_record) == 0) {
                LogUtil.i("Pub", "onEvent,have event");
                return;
            }
            load(context);
            if (event_type == 34 || event_type == 40) {
                if (getCallTime(context, did) != event_time) {
                    LogUtil.i("Pub", "OpenLockScreen:" + did + " index:" + event_index + " type:" + event_type);
                    setCallTime(context, did, event_time);
                    OpenLockScreen(context, did, 0, event_type);
                    return;
                }
                return;
            }
            showNotification(context, camera, event_type, 0, event_time);
            return;
        }
        LogUtil.i("Pub", "onEvent,not find camera:" + did);
    }

    @SuppressLint({"NewApi"})
    public static void showNotification1(BaseCamera camera, EVCommandDefs.Event event) {
        Date date = new Date();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String strDate = f.format(date);
        String title = camera.getName();
        String content = getEventText(event.event_type, event.value);
        Intent intent = new Intent(_context, (Class<?>) EventDetailActivity.class);
        intent.putExtra(ContentCommon.STR_CAMERA_ID, camera.getID());
        intent.setFlags(335544320);
        PendingIntent pendingIntent = PendingIntent.getActivity(_context, 0, intent, 134217728);
        RemoteViews views = new RemoteViews(_context.getPackageName(), R.layout.notification_layout);
        views.setTextViewText(R.id.no_title, title);
        views.setTextViewText(R.id.no_time, strDate);
        views.setImageViewResource(R.id.no_img, R.drawable.app);
        views.setTextViewText(R.id.no_content, content);
        NotificationManager nm = (NotificationManager) _context.getSystemService("notification");
        Notification noti = new Notification.Builder(_context).setSmallIcon(R.drawable.app).setContentIntent(pendingIntent).setContent(views).setDefaults(-1).build();
        noti.sound = Uri.parse("android.resource://" + _context.getPackageName() + "/" + R.raw.bwalarm);
        noti.flags |= 16;
        nm.notify(1, noti);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public static int videoFileLength(String str) {
        int i;
        int i2 = 0;
        i2 = 0;
        File videoSavePathFile = videoSavePathFile();
        if (!videoSavePathFile.exists()) {
            return 0;
        }
        try {
            File file = new File(videoSavePathFile, str);
            if (!file.exists()) {
                Log.i("DownRecord", "file not exists:" + file.getAbsolutePath());
                i = 0;
            } else {
                long length = file.length();
                i = (int) length;
                i2 = (int) length;
            }
            return i;
        } catch (Exception e) {
            return i2;
        }
    }

    public static String videoSavePath() {
        String path = String.valueOf(_context.getString(R.string.app_name)) + "/Videos";
        return path;
    }

    public static File videoSavePathFile() {
        String path = videoSavePath();
        File div = new File(Environment.getExternalStorageDirectory(), path);
        if (!div.exists()) {
            div.mkdirs();
        }
        return div;
    }

    public static String photoSavePath() {
        String path = String.valueOf(_context.getString(R.string.app_name)) + "/Photos";
        return path;
    }

    public static File photoSavePathFile() {
        File dir = null;
        if (_context != null) {
            dir = _context.getExternalFilesDir("Photos");
            if (dir == null) {
                dir = new File(_context.getFilesDir(), "Photos");
            }
        }
        if (dir == null) {
            String path = photoSavePath();
            dir = new File(Environment.getExternalStorageDirectory(), path);
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            File noMedia = new File(dir, ".nomedia");
            if (!noMedia.exists()) {
                noMedia.createNewFile();
            }
        } catch (Exception ignored) {}
        return dir;
    }

    public static String getSavePath(Context context) {
        if (context != null) {
            File base = context.getExternalFilesDir(null);
            if (base != null) {
                return base.getAbsolutePath();
            }
            return context.getFilesDir().getAbsolutePath();
        }
        return Environment.getExternalStorageDirectory() + "/" + (context != null ? context.getString(R.string.app_name) : "BWPixAdapter");
    }

    public static File snapPathFile(Context context) {
        File div = new File(getSavePath(context), "Snaps");
        if (!div.exists()) {
            div.mkdirs();
        }
        try {
            File noMedia = new File(div, ".nomedia");
            if (!noMedia.exists()) {
                noMedia.createNewFile();
            }
        } catch (Exception ignored) {}
        return div;
    }

    /** 通过 MediaStore 将 AVI 视频保存到公共 Movies/BWPixAdapter 目录（兼容 Android 10+ 分区存储） */
    public static boolean saveVideoToMovies(Context context, File srcFile) {
        if (context == null || srcFile == null || !srcFile.exists()) return false;
        try {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, srcFile.getName());
            values.put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/x-msvideo");
            values.put(android.provider.MediaStore.Video.Media.TITLE, srcFile.getName());
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                values.put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/BWPixAdapter");
                values.put(android.provider.MediaStore.Video.Media.IS_PENDING, 1);
            }
            Uri uri = context.getContentResolver().insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                java.io.InputStream in = new java.io.FileInputStream(srcFile);
                java.io.OutputStream out = context.getContentResolver().openOutputStream(uri);
                byte[] buffer = new byte[65536];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                in.close();
                out.close();
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    values.clear();
                    values.put(android.provider.MediaStore.Video.Media.IS_PENDING, 0);
                    context.getContentResolver().update(uri, values, null, null);
                } else {
                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                }
                return true;
            }
        } catch (Exception e) {
            Log.e("Pub", "saveVideoToMovies error: " + e.getMessage(), e);
        }
        return false;
    }

    public static boolean saveImageToGallery(Context context, File srcFile, String title) {
        if (context == null || srcFile == null || !srcFile.exists()) return false;
        try {
            // 通道 1：直接复制到公共相册目录 (Pictures/BWPixAdapter) 并通知媒体扫描器
            File picturesDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "BWPixAdapter");
            if (!picturesDir.exists()) {
                picturesDir.mkdirs();
            }
            File dstFile = new File(picturesDir, srcFile.getName());
            boolean copied = false;
            try {
                java.io.InputStream in = new java.io.FileInputStream(srcFile);
                java.io.OutputStream out = new java.io.FileOutputStream(dstFile);
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                in.close();
                out.close();
                copied = true;
                android.media.MediaScannerConnection.scanFile(context, new String[]{dstFile.getAbsolutePath()}, new String[]{"image/jpeg"}, null);
            } catch (Exception e) {
                Log.w("Pub", "Direct file copy failed, trying MediaStore: " + e.getMessage());
            }

            if (copied && dstFile.exists() && dstFile.length() > 0) {
                return true;
            }

            // 通道 2：兼容高版本 Android MediaStore API 插入
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, srcFile.getName());
            values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(android.provider.MediaStore.Images.Media.TITLE, title);
            Uri uri = context.getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                java.io.InputStream in = new java.io.FileInputStream(srcFile);
                java.io.OutputStream out = context.getContentResolver().openOutputStream(uri);
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                in.close();
                out.close();
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                return true;
            }
        } catch (Exception e) {
            Log.e("Pub", "saveImageToGallery error: " + e.getMessage(), e);
        }
        return false;
    }

    public static File recordPathFile(Context context) {
        File div = new File(getSavePath(context), "Records");
        if (!div.exists()) {
            div.mkdirs();
        }
        return div;
    }

    public static String recordPathName(Context context, String name) {
        File div = new File(getSavePath(context), "Records");
        if (!div.exists()) {
            div.mkdirs();
        }
        return String.valueOf(div.getAbsolutePath()) + "/" + name + ".avi";
    }

    public static boolean isReverseLandscape() {
        return _isReverseLandscape;
    }

    public static void setReverseLandscape(boolean val) {
        _isReverseLandscape = val;
    }

    public static Intent getPackageIntent(String apkName) {
        PackageManager packageManager = _context.getPackageManager();
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        for (int i = 0; i < pinfo.size(); i++) {
            String name = pinfo.get(i).packageName;
            Log.i("pub", "package:" + name);
            if (name.equalsIgnoreCase(apkName)) {
                return packageManager.getLaunchIntentForPackage(apkName);
            }
        }
        return null;
    }

    @SuppressLint({"NewApi"})
    public static int getAviTrackCount(String name) {
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(name);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return extractor.getTrackCount();
    }

    public static boolean isSupportMediaCodecHardDecoder() {
        boolean isHardcode = false;
        new Build();
        String model = Build.MODEL;
        Log.i("pub", "model:" + model);
        if (!model.equals("SM-G5309W") && !model.equals("Coolpad 8297W")) {
            isHardcode = false;
            File file = new File("/system/etc/media_codecs.xml");
            InputStream inFile = null;
            try {
                InputStream inFile2 = new FileInputStream(file);
                inFile = inFile2;
            } catch (Exception e) {
            }
            if (inFile != null) {
                try {
                    XmlPullParserFactory pullFactory = XmlPullParserFactory.newInstance();
                    XmlPullParser xmlPullParser = pullFactory.newPullParser();
                    xmlPullParser.setInput(inFile, "UTF-8");
                    for (int eventType = xmlPullParser.getEventType(); eventType != 1; eventType = xmlPullParser.next()) {
                        String tagName = xmlPullParser.getName();
                        switch (eventType) {
                            case 2:
                                if ("MediaCodec".equals(tagName)) {
                                    String componentName = xmlPullParser.getAttributeValue(0);
                                    if (componentName.startsWith("OMX.") && !componentName.startsWith("OMX.google.")) {
                                        isHardcode = true;
                                        break;
                                    }
                                } else {
                                    break;
                                }
                                break;
                        }
                    }
                } catch (Exception e2) {
                }
            }
        }
        return isHardcode;
    }

    public static boolean isDeviceWifiAp() {
        WifiUtils wifiUtils = new WifiUtils(_context);
        String ssid = wifiUtils.getCurrentSSID();
        Resources res = _context.getResources();
        for (String prefix : res.getStringArray(R.array.WIFI_AP_PREFIX)) {
            if (ssid.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static boolean OpenLockScreen(Context context, String did, int alarmNum, int mode) {
        SharedPreferences preference = context.getSharedPreferences("isFirst", 0);
        Boolean activity = Boolean.valueOf(preference.getBoolean(Constants.FLAG_ACTIVITY_NAME, false));
        Log.i("BridgeService", "callBack_AlarmNotify did:==========11111111111111111111111111111111111111");
        Log.i("BridgeService", "activity  ===" + activity);
        Boolean activity2 = false;
        if (activity2.booleanValue()) {
            return false;
        }
        Intent in = new Intent();
        if (mode == 34) {
            in.setClass(context, CallVideoActivity.class);
        } else if (mode == 40) {
            in.setClass(context, CallVideoActivity.class);
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
        in.addFlags(268697600);
        context.startActivity(in);
        return true;
    }

    @SuppressLint({"InlinedApi", "NewApi"})
    private static boolean isScreenOn() {
        if (Build.VERSION.SDK_INT >= 20) {
            DisplayManager dm = (DisplayManager) _context.getSystemService("display");
            for (Display display : dm.getDisplays()) {
                if (display.getState() == 2 || display.getState() == 0) {
                    return true;
                }
            }
            return false;
        }
        PowerManager powerManager = (PowerManager) _context.getSystemService("power");
        return powerManager.isScreenOn();
    }

    public static boolean isNeedLowFlow() {
        if (!isDebug && WifiUtils.checkWifiConnection(_context)) {
            return false;
        }
        if (isAppOnForeground(_context) && isScreenOn()) {
            return false;
        }
        new Build();
        String str = Build.MODEL;
        return true;
    }

    public static boolean isNeedAppBackgroud() {
        return false;
    }

    public static void setDebug(boolean val) {
        isDebug = val;
    }

    public static boolean getDebug() {
        return isDebug;
    }
}
