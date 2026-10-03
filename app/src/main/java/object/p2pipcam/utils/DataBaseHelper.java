package object.p2pipcam.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.easyview.basecamera.BaseCamera;
import com.easyview.camera.CameraApplication;
import com.easyview.ppcs.PPCSCamera;
import com.easyview.table.EventIndexTable;
import com.easyview.table.EventTable;
import com.easyview.table.RecordTable;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class DataBaseHelper extends SQLiteOpenHelper {
    public static final String ALARM_BOOT = "bootAlarm";
    public static final String ALARM_MESSAGE = "messageAlarm";
    public static final String ALARM_MOTION = "motionAlarm";
    public static final String ALARM_TIME = "alarmTime";
    public static final String ALARM_VISITOR = "visitorAlarm";
    private static final String CREATE_ALARMLOG_TABLE = "create table alarmlog(id integer primary key autoincrement, did text not null, content text not null, createtime text not null);";
    private static final String CREATE_ALARMTYPENUM_TABLE = "create table alarmtypenum(id integer primary key autoincrement, did text not null, motionAlarm text not null, visitorAlarm text not null, messageAlarm text not null, bootAlarm text not null);";
    private static final String CREATE_FIRSTPIC_TABLE = "create table firstpic(id integer primary key autoincrement,did text not null, filepath text not null)";
    private static final String CREATE_STUDENT_TABLE = "create table cameralist (id integer primary key autoincrement, name text not null, did text not null, user text not null,pwd text);";
    private static final String CREATE_VIDEO_PICTURE_TABLE = "create table cameravidpic(id integer primary key autoincrement, did text not null, filepath text not null, createtime text not null, type text not null);";
    private static final String CREATE_WIFI_TABLE = "create table wifilist (id integer primary key autoincrement,, ssid VARCHAR, password VARCHAR);";
    private static final String DATABASE_ALARMLOG_TABLE = "alarmlog";
    private static final String DATABASE_ALARMTYPENUM_TABLE = "alarmtypenum";
    private static final String DATABASE_FIRSTPIC_TABLE = "firstpic";
    private static final String DATABASE_NAME = "p2p_camera_database";
    private static final String DATABASE_TABLE = "cameralist";
    private static final int DATABASE_VERSION = 4;
    private static final String DATABASE_WIFI_TABLE = "wifilist";
    private static final String DATABASW_VIDEOPICTURE_TABLE = "cameravidpic";
    public static final String HAVE_PICTURE = "haveRecord";
    public static final String HAVE_RECORD = "haveRecord";
    public static final String KEY_ALARMLOG_CONTENT = "content";
    public static final String KEY_CREATETIME = "createtime";
    public static final String KEY_DID = "did";
    public static final String KEY_FILEPATH = "filepath";
    public static final String KEY_ID = "id";
    public static final String KEY_NAME = "name";
    public static final String KEY_PWD = "pwd";
    public static final String KEY_TYPE = "type";
    public static final String KEY_USER = "user";
    public static final String PICTURE_PATH = "picturePath";
    public static final String TYPE_PICTURE = "picture";
    public static final String TYPE_VIDEO = "video";
    private String TAG;
    private Context context;
    private SQLiteDatabase db;
    private static DataBaseHelper _helper = null;
    private static final String DATABASE_ALARMCOUNT_TABLE = "alarmcount";
    public static final String ALARM_TYPE = "alarmType";
    public static final String ALARM_COUNT = "alarmCount";
    private static final String CREATE_ALARMCOUNT_TABLE = String.format("create table %s (%s text not null,%s int,%s int,constraint pk_t2 primary key (%s,%s));", DATABASE_ALARMCOUNT_TABLE, "did", ALARM_TYPE, ALARM_COUNT, "did", ALARM_TYPE);

    public static DataBaseHelper getInstance(Context ctx) {
        if (_helper == null) {
            _helper = new DataBaseHelper(ctx);
            _helper.db = _helper.getWritableDatabase();
        }
        return _helper;
    }

    public static DataBaseHelper getInstance() {
        return _helper;
    }

    public static SQLiteDatabase getReadableDB(Context context) {
        DataBaseHelper helper = new DataBaseHelper(context);
        return helper.getReadableDatabase();
    }

    private DataBaseHelper(Context context) {
        super(context, DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, 4);
        this.TAG = "DataBaseHelper";
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase db) {
        Log.i(this.TAG, "Creating student_table: create table cameralist (id integer primary key autoincrement, name text not null, did text not null, user text not null,pwd text);");
        Log.i(this.TAG, "Creating Video_Picture_Table: create table cameravidpic(id integer primary key autoincrement, did text not null, filepath text not null, createtime text not null, type text not null);");
        Log.i(this.TAG, "Creating alarmlog_table: create table alarmlog(id integer primary key autoincrement, did text not null, content text not null, createtime text not null);");
        Log.i(this.TAG, "Creating Firstpic_tablecreate table firstpic(id integer primary key autoincrement,did text not null, filepath text not null)");
        Log.i(this.TAG, "CREATE_ALARMTYPENUM_TABLE == create table alarmtypenum(id integer primary key autoincrement, did text not null, motionAlarm text not null, visitorAlarm text not null, messageAlarm text not null, bootAlarm text not null);");
        db.execSQL(CREATE_STUDENT_TABLE);
        db.execSQL(CREATE_VIDEO_PICTURE_TABLE);
        db.execSQL(CREATE_ALARMLOG_TABLE);
        db.execSQL(CREATE_FIRSTPIC_TABLE);
        db.execSQL(CREATE_ALARMTYPENUM_TABLE);
        db.execSQL(CREATE_ALARMCOUNT_TABLE);
        EventTable.getInstance(db).CreateTable();
        RecordTable.getInstance(db).CreateTable();
        EventIndexTable.getInstance(db).CreateTable();
    }

    public SQLiteDatabase getDatabase() {
        return this.db;
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.beginTransaction();
        while (oldVersion != newVersion) {
            if (oldVersion == 1) {
                try {
                    EventTable.getInstance(db).UpgradeToVersion2();
                    oldVersion++;
                } catch (Throwable ex) {
                    Log.e(this.TAG, ex.getMessage(), ex);
                    return;
                } finally {
                    db.endTransaction();
                }
            }
            if (oldVersion == 2) {
                EventTable.getInstance(db).UpgradeToVersion3();
                RecordTable.getInstance(db).UpgradeToVersion3();
                oldVersion++;
            }
            if (oldVersion == 3) {
                EventIndexTable.getInstance(db).UpgradeToVersion4();
                oldVersion++;
            }
        }
        db.setTransactionSuccessful();
    }

    public EventIndexTable getEventIndexTable() {
        return EventIndexTable.getInstance(this.db);
    }

    public EventTable getEventTable() {
        return EventTable.getInstance(this.db);
    }

    public RecordTable getRecordTable() {
        return RecordTable.getInstance(this.db);
    }

    @Override // android.database.sqlite.SQLiteOpenHelper, java.lang.AutoCloseable
    public void close() {
        _helper = null;
        this.db.close();
    }

    public long createCamera(String name, String did, String user, String pwd) {
        ContentValues initialValues = new ContentValues();
        initialValues.put("name", name);
        initialValues.put("did", did);
        initialValues.put("user", user);
        initialValues.put("pwd", pwd);
        return this.db.insert(DATABASE_TABLE, null, initialValues);
    }

    public boolean deleteCamera(long rowId) {
        return this.db.delete(DATABASE_TABLE, new StringBuilder("id=").append(rowId).toString(), null) > 0;
    }

    public boolean deleteCamera(String did) {
        this.db.delete(DATABASE_FIRSTPIC_TABLE, "did='" + did + "'", null);
        this.db.delete(DATABASW_VIDEOPICTURE_TABLE, "did='" + did + "'", null);
        this.db.delete(DATABASE_ALARMLOG_TABLE, "did='" + did + "'", null);
        this.db.delete(DATABASE_ALARMTYPENUM_TABLE, "did='" + did + "'", null);
        return this.db.delete(DATABASE_TABLE, new StringBuilder("did='").append(did).append("'").toString(), null) > 0;
    }

    public Cursor fetchAllCameras() {
        return this.db.query(DATABASE_TABLE, new String[]{"id", "name", "did", "user", "pwd"}, null, null, null, null, null);
    }

    public Cursor fetchCamera(String did) {
        String[] args = {did};
        return this.db.query(DATABASE_TABLE, new String[]{"id", "name", "did", "user", "pwd"}, "did=?", args, null, null, null);
    }

    public static BaseCamera getCamera(Context context, String did) {
        DataBaseHelper helper = new DataBaseHelper(context);
        SQLiteDatabase db = helper.getReadableDatabase();
        String[] args = {did};
        Cursor cursor = db.query(DATABASE_TABLE, new String[]{"id", "name", "did", "user", "pwd"}, "did=?", args, null, null, null);
        CameraApplication app = (CameraApplication) context.getApplicationContext();
        if (cursor.moveToNext()) {
            String name = cursor.getString(1);
            String user = cursor.getString(3);
            String pwd = cursor.getString(4);
            if (Pub.isBell(context)) {
                user = Pub.getCameraUser(context);
            }
            app.getCameraList().Add(name, did, user, pwd);
        }
        db.close();
        return app.getCameraList().getCamera(did);
    }

    public static List<PPCSCamera> getCameras(Context context) {
        List<PPCSCamera> cameras = new ArrayList<>();
        DataBaseHelper helper = new DataBaseHelper(context);
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(DATABASE_TABLE, new String[]{"id", "name", "did", "user", "pwd"}, null, null, null, null, null);
        while (cursor.moveToNext()) {
            String name = cursor.getString(1);
            String did = cursor.getString(2);
            String user = cursor.getString(3);
            String pwd = cursor.getString(4);
            if (Pub.isBell(context)) {
                user = Pub.getCameraUser(context);
            }
            PPCSCamera camera = new PPCSCamera(name, did, user, pwd);
            cameras.add(camera);
        }
        db.close();
        return cameras;
    }

    public static long SaveEvent(Context context, String did, int fileID, int eventIndex, int eventType, int eventTime, int recordIndex) {
        DataBaseHelper helper = new DataBaseHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        EventTable table = EventTable.getInstance(db);
        long result = table.Save(did, fileID, eventIndex, eventType, eventTime, recordIndex);
        db.close();
        return result;
    }

    public Cursor fetchAllAlarmCameras() {
        return this.db.query(DATABASE_ALARMTYPENUM_TABLE, new String[]{"id", "did", ALARM_MOTION, ALARM_VISITOR, ALARM_MESSAGE, ALARM_BOOT}, null, null, null, null, null);
    }

    public long createCameraAlarm(String did, String motionalarm, String visitoralarm, String messagealarm, String bootalarm) {
        ContentValues initialValues = new ContentValues();
        initialValues.put("did", did);
        initialValues.put(ALARM_MOTION, motionalarm);
        initialValues.put(ALARM_VISITOR, visitoralarm);
        initialValues.put(ALARM_MESSAGE, messagealarm);
        initialValues.put(ALARM_BOOT, bootalarm);
        return this.db.insert(DATABASE_ALARMTYPENUM_TABLE, null, initialValues);
    }

    public boolean updateCameraAlarm(String did, String alarm, int num) {
        ContentValues alarmvalues = new ContentValues();
        String[] arlarmName = {ALARM_MOTION, ALARM_VISITOR, ALARM_MESSAGE, ALARM_BOOT};
        alarmvalues.put(arlarmName[num - 2], alarm);
        return this.db.update(DATABASE_ALARMTYPENUM_TABLE, alarmvalues, new StringBuilder("did='").append(did).append("'").toString(), null) > 0;
    }

    public Cursor fetchCamera(long id) throws SQLException {
        Cursor mCursor = this.db.query(true, DATABASE_TABLE, new String[]{"id", "name", "did", "user", "pwd"}, "id=" + id, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public boolean updateCamera(String oldDID, String name, String did, String user, String pwd) {
        ContentValues args = new ContentValues();
        args.put("name", name);
        args.put("did", did);
        args.put("user", user);
        args.put("pwd", pwd);
        return this.db.update(DATABASE_TABLE, args, new StringBuilder("did='").append(oldDID).append("'").toString(), null) > 0;
    }

    public boolean updateCameraUser(String did, String username, String pwd) {
        ContentValues values = new ContentValues();
        values.put("user", username);
        values.put("pwd", pwd);
        return this.db.update(DATABASE_TABLE, values, new StringBuilder("did='").append(did).append("'").toString(), null) > 0;
    }

    public long createVideoOrPic(String did, String filepath, String type, String createtime) {
        ContentValues initialValues = new ContentValues();
        initialValues.put("did", did);
        initialValues.put("filepath", filepath);
        initialValues.put("type", type);
        initialValues.put("createtime", createtime);
        return this.db.insert(DATABASW_VIDEOPICTURE_TABLE, null, initialValues);
    }

    public Cursor queryAllVideo(String did) {
        String sql = "select * from cameravidpic where  type='video' and did='" + did + "' order by filepath desc";
        return this.db.rawQuery(sql, null);
    }

    public Cursor queryAllPicture(String did) {
        String sql = "select * from cameravidpic where  type='picture' and did='" + did + "'";
        return this.db.rawQuery(sql, null);
    }

    public Cursor queryVideoOrPictureByDate(String did, String date, String type) {
        String sql = "select * from cameravidpic where  type='picture' and did='" + did + "' and createtime='" + date + "'";
        return this.db.rawQuery(sql, null);
    }

    public boolean deleteVideoOrPicture(String did, String filePath, String type) {
        return this.db.delete(DATABASW_VIDEOPICTURE_TABLE, "did=? and filepath=? and type=?", new String[]{did, filePath, type}) > 0;
    }

    public boolean deleteAllVideoOrPicture(String did, String type) {
        return this.db.delete(DATABASW_VIDEOPICTURE_TABLE, "did=? and type=?", new String[]{did, type}) > 0;
    }

    public boolean deldteAllVideoPicture(String did) {
        return this.db.delete(DATABASW_VIDEOPICTURE_TABLE, "did=?", new String[]{did}) > 0;
    }

    public long insertAlarmLogToDB(String did, String content, String createTime) {
        ContentValues initialValues = new ContentValues();
        initialValues.put("did", did);
        initialValues.put("content", content);
        initialValues.put("createtime", createTime);
        return this.db.insertOrThrow(DATABASE_ALARMLOG_TABLE, null, initialValues);
    }

    public Cursor queryAllAlarmLog(String did) {
        String sql = "select * from alarmlog where did='" + did + "' order by createtime desc";
        return this.db.rawQuery(sql, null);
    }

    public boolean delAlarmLog(String did, String createtime) {
        return this.db.delete(DATABASE_ALARMLOG_TABLE, "did=? and createtime=?", new String[]{did, createtime}) > 0;
    }

    public boolean delAlarmLog(String did) {
        return this.db.delete(DATABASE_ALARMLOG_TABLE, "did=? ", new String[]{did}) > 0;
    }

    public int queryAlarmCount(String did) {
        String sql = String.format("select SUM(%s) from %s where %s = '%s'", ALARM_COUNT, DATABASE_ALARMCOUNT_TABLE, "did", did, ALARM_TYPE);
        Cursor cursor = this.db.rawQuery(sql, null);
        int count = -1;
        if (cursor != null) {
            if (cursor.moveToNext()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }

    public static int queryAlarmCount(SQLiteDatabase db, String did, int alarmType) {
        String sql = String.format("select %s from %s where %s = '%s' and %s = %d", ALARM_COUNT, DATABASE_ALARMCOUNT_TABLE, "did", did, ALARM_TYPE, Integer.valueOf(alarmType));
        Cursor cursor = db.rawQuery(sql, null);
        int count = -1;
        if (cursor != null) {
            if (cursor.moveToNext()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }

    public boolean clearAlarmCount(String did) {
        return this.db.delete(DATABASE_ALARMCOUNT_TABLE, "did=? ", new String[]{did}) > 0;
    }

    public static void incAlarmCount(SQLiteDatabase db, String did, int alarmType) {
        int count = queryAlarmCount(db, did, alarmType);
        int alarm_count = count + 1;
        if (alarm_count == 0) {
            alarm_count = 1;
        }
        ContentValues values = new ContentValues();
        values.put("did", did);
        values.put(ALARM_TYPE, Integer.valueOf(alarmType));
        values.put(ALARM_COUNT, Integer.valueOf(alarm_count));
        if (count == -1) {
            db.insert(DATABASE_ALARMCOUNT_TABLE, null, values);
        } else {
            String whereClause = String.format("%s = '%s' and %s = %d", "did", did, ALARM_TYPE, Integer.valueOf(alarmType));
            db.update(DATABASE_ALARMCOUNT_TABLE, values, whereClause, null);
        }
    }

    public boolean addFirstpic(String did, String filepath) {
        ContentValues initialValues = new ContentValues();
        initialValues.put("did", did);
        initialValues.put("filepath", filepath);
        return this.db.insert(DATABASE_FIRSTPIC_TABLE, null, initialValues) > 0;
    }

    public Cursor queryFirstpic(String did) {
        String sql = "select *  from firstpic where did='" + did + "'";
        return this.db.rawQuery(sql, null);
    }
}
