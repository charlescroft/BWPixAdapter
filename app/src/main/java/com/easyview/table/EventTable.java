package com.easyview.table;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;
import com.easyview.struct.EVCommandDefs;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import object.p2pipcam.bean.EventDetailBean;
import object.p2pipcam.utils.DataBaseHelper;

/* loaded from: classes.dex */
public class EventTable {
    public static final String FILE_ID = "fileID";
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
    public static final String RECORD_INDEX = "recordIndex";
    private SQLiteDatabase _db;
    private static final String DATABASE_EVENT_TABLE = "event";
    public static final String EVENT_INDEX = "eventIndex";
    public static final String EVENT_TYPE = "eventType";
    public static final String EVENT_TIME = "eventTime";
    public static final String CREATE_TIME = "createTime";
    public static final String EVENT_VALUE = "eventValue";
    public static final String HAVE_PICTURE = "havePicture";
    private static final String CREATE_ALARM_TABLE = String.format("create table %s (%s integer primary key autoincrement, %s text not null,%s int,%s int,%s int,%s int,%s int,%s int,%s int,%s int,%s text);", DATABASE_EVENT_TABLE, "id", "did", "fileID", EVENT_INDEX, EVENT_TYPE, EVENT_TIME, CREATE_TIME, EVENT_VALUE, "recordIndex", HAVE_PICTURE, "picturePath");

    public static EventTable getInstance(SQLiteDatabase db) {
        return new EventTable(db);
    }

    public static EventTable getInstance(Context context) {
        return DataBaseHelper.getInstance(context).getEventTable();
    }

    public EventTable(SQLiteDatabase db) {
        this._db = null;
        this._db = db;
    }

    public void setDatabase(SQLiteDatabase db) {
        this._db = db;
    }

    public void CreateTable() {
        if (this._db != null) {
            this._db.execSQL(CREATE_ALARM_TABLE);
        }
    }

    public void UpgradeToVersion2() {
        this._db.execSQL("alter table event rename to temp_A");
        CreateTable();
        Date now = new Date();
        int time = (int) (now.getTime() / 1000);
        String sql = String.format("INSERT INTO %s SELECT %s,%s,%s,%s,%s,%d as %s,endTime as %s,%s,%s,%s FROM temp_A;", DATABASE_EVENT_TABLE, "id", "did", EVENT_INDEX, EVENT_TYPE, EVENT_TIME, Integer.valueOf(time), CREATE_TIME, EVENT_VALUE, "recordIndex", HAVE_PICTURE, "picturePath");
        this._db.execSQL(sql);
        this._db.execSQL("DROP TABLE temp_A;");
    }

    public void UpgradeToVersion3() {
        this._db.execSQL("alter table event rename to temp_A");
        CreateTable();
        String sql = String.format("INSERT INTO %s SELECT %s,%s,0,%s,%s,%s,%s,%s,%s,%s,%s FROM temp_A;", DATABASE_EVENT_TABLE, "id", "did", EVENT_INDEX, EVENT_TYPE, EVENT_TIME, CREATE_TIME, EVENT_VALUE, "recordIndex", HAVE_PICTURE, "picturePath");
        this._db.execSQL(sql);
        this._db.execSQL("DROP TABLE temp_A;");
    }

    public long Save(String did, int fileID, int eventIndex, int eventType, int eventTime, int recordIndex) {
        String path;
        if (isExist(did, eventType, eventTime)) {
            return 0L;
        }
        if (fileID == 0) {
            Pair<Integer, Integer> pair = getLastIndexAndFID(did);
            fileID = ((Integer) pair.second).intValue();
        }
        DataBaseHelper.incAlarmCount(this._db, did, eventType);
        ContentValues initialValues = new ContentValues();
        initialValues.put("did", did);
        initialValues.put("fileID", Integer.valueOf(fileID));
        initialValues.put(EVENT_INDEX, Integer.valueOf(eventIndex));
        initialValues.put(EVENT_TYPE, Integer.valueOf(eventType));
        initialValues.put(EVENT_TIME, Integer.valueOf(eventTime));
        Date now = new Date();
        int time = (int) (now.getTime() / 1000);
        initialValues.put(CREATE_TIME, Integer.valueOf(time));
        initialValues.put(EVENT_VALUE, (Integer) 0);
        initialValues.put("recordIndex", Integer.valueOf(recordIndex));
        initialValues.put(HAVE_PICTURE, (Integer) 0);
        if (eventType == 514 && (path = RecordTable.getInstance(this._db).getPicturePath(did, fileID, eventTime)) != null) {
            initialValues.put(HAVE_PICTURE, (Integer) 1);
            initialValues.put("recordIndex", (Integer) 1);
            initialValues.put("picturePath", path);
        }
        long result = this._db.insert(DATABASE_EVENT_TABLE, null, initialValues);
        Log.d("EventTable", String.format("insert event result: %d %s fID:%d index:%d %d %d", Long.valueOf(result), did, Integer.valueOf(fileID), Integer.valueOf(eventIndex), Integer.valueOf(eventType), Integer.valueOf(eventTime)));
        return result;
    }

    public long Save(String did, int fileID, EVCommandDefs.Event event, String picPath) {
        if (isExist(fileID, did, event) || isExist(did, event.event_type, event.event_time)) {
            Update(did, fileID, event.event_type, event.event_time, picPath);
            return 0L;
        }
        DataBaseHelper.incAlarmCount(this._db, did, event.event_type);
        ContentValues initialValues = new ContentValues();
        initialValues.put("did", did);
        initialValues.put("fileID", Integer.valueOf(fileID));
        initialValues.put(EVENT_INDEX, Integer.valueOf(event.index));
        initialValues.put(EVENT_TYPE, Short.valueOf(event.event_type));
        initialValues.put(EVENT_TIME, Integer.valueOf(event.event_time));
        Date now = new Date();
        int time = (int) (now.getTime() / 1000);
        initialValues.put(CREATE_TIME, Integer.valueOf(time));
        initialValues.put(EVENT_VALUE, Integer.valueOf(event.value));
        initialValues.put("recordIndex", Integer.valueOf(event.record_index));
        if (picPath != null) {
            initialValues.put(HAVE_PICTURE, (Integer) 1);
            initialValues.put("picturePath", picPath);
        } else {
            initialValues.put(HAVE_PICTURE, (Integer) 0);
        }
        long result = this._db.insert(DATABASE_EVENT_TABLE, null, initialValues);
        Log.d("Event", String.format("insert event: result:%d index:%d fileID:%d %s %d %d %s", Long.valueOf(result), Integer.valueOf(event.index), Integer.valueOf(fileID), did, Short.valueOf(event.event_type), Integer.valueOf(event.event_time), picPath));
        return result;
    }

    public boolean isExist(int fileID, String did, EVCommandDefs.Event event) {
        String sql = String.format("select * from %s where %s = %d AND %s = '%s' and %s = %d and %s = %d", DATABASE_EVENT_TABLE, "fileID", Integer.valueOf(fileID), "did", did, EVENT_TYPE, Short.valueOf(event.event_type), EVENT_TIME, Integer.valueOf(event.event_time));
        Cursor cursor = this._db.rawQuery(sql, null);
        boolean result = false;
        if (cursor != null) {
            if (cursor.moveToNext()) {
                result = true;
            }
            cursor.close();
        }
        return result;
    }

    public boolean isExist(String did, int eventType, int eventTime) {
        String sql = String.format("select * from %s where %s = '%s' and %s = %d and %s = %d", DATABASE_EVENT_TABLE, "did", did, EVENT_TYPE, Integer.valueOf(eventType), EVENT_TIME, Integer.valueOf(eventTime));
        Cursor cursor = this._db.rawQuery(sql, null);
        boolean result = false;
        if (cursor != null) {
            if (cursor.moveToNext()) {
                result = true;
            }
            cursor.close();
        }
        return result;
    }

    public boolean Update(int fileID, String did, EVCommandDefs.Event event, String picPath) {
        if (this._db == null) {
            return false;
        }
        ContentValues cv = new ContentValues();
        cv.put(EVENT_INDEX, Integer.valueOf(event.index));
        cv.put(EVENT_TYPE, Short.valueOf(event.event_type));
        cv.put(EVENT_TIME, Integer.valueOf(event.event_time));
        if (picPath != null) {
            cv.put(HAVE_PICTURE, (Integer) 1);
            cv.put("picturePath", picPath);
        }
        String Where = String.format("%s = ? and %s = ? and %s = ? and %s = ?", "fileID", "did", EVENT_TYPE, EVENT_TIME);
        String[] args = {String.valueOf(fileID), did, String.valueOf((int) event.event_type), String.valueOf(event.event_time)};
        long result = this._db.update(DATABASE_EVENT_TABLE, cv, Where, args);
        Log.d("EventTable", String.format("Update event:%d %s %d %d %s", Long.valueOf(result), did, Short.valueOf(event.event_type), Integer.valueOf(event.event_time), picPath));
        return result > 0;
    }

    public boolean Update(String did, int fileID, int eventType, int eventTime, String picPath) {
        if (this._db == null) {
            return false;
        }
        ContentValues cv = new ContentValues();
        cv.put(HAVE_PICTURE, (Integer) 1);
        cv.put("picturePath", picPath);
        String Where = String.format("%s = ? AND %s = ? and %s = ? and %s = ?", "did", "fileID", EVENT_TYPE, EVENT_TIME);
        String[] args = {did, String.valueOf(fileID), String.valueOf(eventType), String.valueOf(eventTime)};
        long result = this._db.update(DATABASE_EVENT_TABLE, cv, Where, args);
        if (result <= 0) {
            String fallbackWhere = String.format("%s = ? and %s = ?", "did", EVENT_TIME);
            String[] fallbackArgs = {did, String.valueOf(eventTime)};
            result = this._db.update(DATABASE_EVENT_TABLE, cv, fallbackWhere, fallbackArgs);
        }
        Log.d("EventTable", String.format("Update result:%d %s %d %d", Long.valueOf(result), did, Integer.valueOf(eventType), Integer.valueOf(eventTime)));
        return result > 0;
    }

    public boolean Update(String did, int fileID, int eventType, int beginTime, int endTime) {
        if (this._db == null) {
            return false;
        }
        ContentValues cv = new ContentValues();
        cv.put(EVENT_VALUE, Integer.valueOf(endTime));
        String Where = String.format("%s = ? and %s = ? and %s = ? and %s = ?", "did", "fileID", EVENT_TYPE, EVENT_TIME);
        String[] args = {did, String.valueOf(fileID), String.valueOf(eventType), String.valueOf(beginTime)};
        long result = this._db.update(DATABASE_EVENT_TABLE, cv, Where, args);
        Log.d("EventTable", String.format("Update result:%d %s %d %d", Long.valueOf(result), did, Integer.valueOf(eventType), Integer.valueOf(beginTime)));
        return result > 0;
    }

    public boolean UpdateRecordIndex(String did, int fileID, int beginTime, int endTime, int recordIndex) {
        if (this._db == null) {
            return false;
        }
        ContentValues cv = new ContentValues();
        cv.put("recordIndex", Integer.valueOf(recordIndex));
        String Where = String.format("%s = ? AND %s = ? and (%s BETWEEN ? and ?)", "did", "fileID", EVENT_TIME);
        String[] args = {did, String.valueOf(fileID), String.valueOf(beginTime - 15), String.valueOf(endTime)};
        long result = this._db.update(DATABASE_EVENT_TABLE, cv, Where, args);
        Log.d("EventTable", String.format("Update event record index,result:%d %s %d %d record_index:%d", Long.valueOf(result), did, Integer.valueOf(beginTime), Integer.valueOf(endTime), Integer.valueOf(recordIndex)));
        return true;
    }

    public Pair<Integer, Integer> getMaxIndex(String did) {
        Pair<Integer, Integer> result = new Pair<>(-1, -1);
        String sql = String.format("select %s,%s from %s where %s = '%s' order by %s desc limit 1", EVENT_INDEX, EVENT_TIME, DATABASE_EVENT_TABLE, "did", did, EVENT_INDEX);
        Cursor cursor = this._db.rawQuery(sql, null);
        if (cursor != null) {
            if (cursor.moveToNext()) {
                result = new Pair<>(Integer.valueOf(cursor.getInt(0)), Integer.valueOf(cursor.getInt(1)));
            }
            cursor.close();
        }
        return result;
    }

    public Pair<Integer, Integer> getLastIndexAndFID(String did) {
        int fileID = 0;
        int index = 0;
        String sql = String.format("select %s from %s where %s = '%s' order by %s desc limit 1", "fileID", DATABASE_EVENT_TABLE, "did", did, CREATE_TIME);
        Cursor cursor = this._db.rawQuery(sql, null);
        if (cursor != null) {
            if (cursor.moveToNext()) {
                fileID = cursor.getInt(0);
            }
            cursor.close();
        }
        String sql2 = String.format("select %s from %s where %s = %d order by %s desc limit 1", EVENT_INDEX, DATABASE_EVENT_TABLE, "fileID", Integer.valueOf(fileID), EVENT_INDEX);
        Cursor cursor2 = this._db.rawQuery(sql2, null);
        if (cursor2 != null) {
            if (cursor2.moveToNext()) {
                index = cursor2.getInt(0);
            }
            cursor2.close();
        }
        Pair<Integer, Integer> result = new Pair<>(Integer.valueOf(index), Integer.valueOf(fileID));
        return result;
    }

    public int getLastIndex(String did, int fileID) {
        int result = 0;
        String sql = String.format("select %s from %s where %s = %d order by %s desc limit 1", EVENT_INDEX, DATABASE_EVENT_TABLE, "fileID", Integer.valueOf(fileID), EVENT_INDEX);
        Cursor cursor = this._db.rawQuery(sql, null);
        if (cursor != null) {
            if (cursor.moveToNext()) {
                result = cursor.getInt(0);
            }
            cursor.close();
        }
        return result;
    }

    public Cursor queryAllEvent(String did) {
        String sql = String.format("select * from %s where %s = '%s' order by %s desc", DATABASE_EVENT_TABLE, "did", did, EVENT_TIME);
        return this._db.rawQuery(sql, null);
    }

    public List<Integer> lastNoPictures(String did, int fileID, int count) {
        List<Integer> list = new ArrayList<>();
        String sql = String.format("select %s,%s from %s where %s = '%s' and %s = %d order by %s desc limit %d", EVENT_INDEX, HAVE_PICTURE, DATABASE_EVENT_TABLE, "did", did, "fileID", Integer.valueOf(fileID), EVENT_INDEX, Integer.valueOf(count));
        Log.i("Event", "SQL:" + sql);
        Cursor cursor = this._db.rawQuery(sql, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int index = cursor.getInt(0);
                int have = cursor.getInt(1);
                if (have == 0) {
                    Log.i("Event", "no picture:" + index);
                    list.add(Integer.valueOf(index));
                }
            }
            cursor.close();
        }
        Log.i("Event", "result count:" + list.size());
        return list;
    }

    public List<EventDetailBean> allEvent(String did, int fileID) {
        List<EventDetailBean> list = new ArrayList<>();
        Cursor cursor = queryAllEvent(did);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int eventIndex = cursor.getInt(cursor.getColumnIndex(EVENT_INDEX));
                int eventType = cursor.getInt(cursor.getColumnIndex(EVENT_TYPE));
                int eventTime = cursor.getInt(cursor.getColumnIndex(EVENT_TIME));
                int endTime = cursor.getInt(cursor.getColumnIndex(EVENT_VALUE));
                int recordIndex = cursor.getInt(cursor.getColumnIndex("recordIndex"));
                int havePicture = cursor.getInt(cursor.getColumnIndex(HAVE_PICTURE));
                String picturePath = havePicture == 1 ? cursor.getString(cursor.getColumnIndex("picturePath")) : null;
                if (eventType != 513 && eventType != 514) {
                    EventDetailBean bean = new EventDetailBean(did, fileID);
                    bean.setDid(did);
                    bean.setEventIndex(eventIndex);
                    bean.setEventType(eventType);
                    bean.setEventTime(eventTime);
                    bean.setEndTime(endTime);
                    bean.setRecordIndex(recordIndex);
                    bean.setHavePicture(havePicture);
                    bean.setPicturePath(picturePath);
                    list.add(bean);
                }
            }
            cursor.close();
        }
        return list;
    }

    public List<EventDetailBean> allEvent(String did, int fileID, int time) {
        List<EventDetailBean> list = new ArrayList<>();
        String sql = String.format("select * from %s where %s = '%s' and %s < %d ", DATABASE_EVENT_TABLE, "did", did, CREATE_TIME, Integer.valueOf(time));
        Cursor cursor = this._db.rawQuery(sql, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int eventType = cursor.getInt(cursor.getColumnIndex(EVENT_TYPE));
                int eventTime = cursor.getInt(cursor.getColumnIndex(EVENT_TIME));
                int endTime = cursor.getInt(cursor.getColumnIndex(EVENT_VALUE));
                int recordIndex = cursor.getInt(cursor.getColumnIndex("recordIndex"));
                int havePicture = cursor.getInt(cursor.getColumnIndex(HAVE_PICTURE));
                String picturePath = havePicture == 1 ? cursor.getString(cursor.getColumnIndex("picturePath")) : null;
                if (eventType == 513) {
                    int duration = RecordTable.getInstance(this._db).getDuration(did, fileID, recordIndex);
                    if (duration != 0) {
                    }
                }
                EventDetailBean bean = new EventDetailBean(did, fileID);
                bean.setDid(did);
                bean.setEventType(eventType);
                bean.setEventTime(eventTime);
                bean.setEndTime(endTime);
                bean.setRecordIndex(recordIndex);
                bean.setHavePicture(havePicture);
                bean.setPicturePath(picturePath);
                list.add(bean);
            }
            cursor.close();
        }
        return list;
    }

    public String getPicturePath(int fileID, String did, int index) {
        String picturePath = null;
        String sql = String.format("SELECT %s,%s FROM %s WHERE %s = %d AND %s = '%s' AND %s = %d ", HAVE_PICTURE, "picturePath", DATABASE_EVENT_TABLE, "fileID", Integer.valueOf(fileID), "did", did, EVENT_INDEX, Integer.valueOf(index));
        Cursor cursor = this._db.rawQuery(sql, null);
        if (cursor != null) {
            if (cursor.moveToNext()) {
                int havePicture = cursor.getInt(cursor.getColumnIndex(HAVE_PICTURE));
                if (havePicture == 1) {
                    picturePath = cursor.getString(cursor.getColumnIndex("picturePath"));
                }
            }
            cursor.close();
        }
        return picturePath;
    }

    public boolean delEventById(int id) {
        if (this._db == null) return false;
        return this._db.delete(DATABASE_EVENT_TABLE, "id=?", new String[]{String.valueOf(id)}) > 0;
    }

    public int delEventsByIds(List<Integer> ids) {
        if (this._db == null || ids == null || ids.isEmpty()) return 0;
        int count = 0;
        for (int id : ids) {
            if (delEventById(id)) count++;
        }
        return count;
    }

    public boolean delEvent(String did, int eventType, int eventTime) {
        String clause = String.format("%s=? and %s=? and %s=?", "did", EVENT_TYPE, EVENT_TIME);
        return this._db.delete(DATABASE_EVENT_TABLE, clause, new String[]{did, String.format("%d", Integer.valueOf(eventType)), String.format("%d", Integer.valueOf(eventTime))}) > 0;
    }

    public boolean delEvent(String did) {
        return this._db.delete(DATABASE_EVENT_TABLE, "did=? ", new String[]{did}) > 0;
    }

    public static int getEventCount(String did, Context context) {
        SQLiteDatabase db = DataBaseHelper.getReadableDB(context);
        String sql = String.format("select count(*) from %s where %s = '%s'", DATABASE_EVENT_TABLE, "did", did);
        int result = 0;
        Cursor cursor = db.rawQuery(sql, null);
        if (cursor != null) {
            if (cursor.moveToNext()) {
                result = cursor.getInt(0);
            }
            cursor.close();
        }
        return result;
    }

    public int deleteOldEvents(String did, int cutoffTimestamp) {
        if (this._db == null) return 0;
        try {
            // 查询并删除旧图片文件
            Cursor cursor = this._db.query(DATABASE_EVENT_TABLE, new String[]{"picturePath"},
                    "did=? and eventTime < ? and havePicture = 1",
                    new String[]{did, String.valueOf(cutoffTimestamp)}, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String path = cursor.getString(0);
                    if (path != null) {
                        try {
                            new File(path).delete();
                        } catch (Exception ignored) {}
                    }
                }
                cursor.close();
            }
            int deleted = this._db.delete(DATABASE_EVENT_TABLE, "did=? and eventTime < ?",
                    new String[]{did, String.valueOf(cutoffTimestamp)});
            Log.i("EventTable", "Cleaned up old events: " + deleted + " before " + cutoffTimestamp);
            return deleted;
        } catch (Exception e) {
            Log.e("EventTable", "deleteOldEvents error: " + e.getMessage(), e);
            return 0;
        }
    }

    public static List<EventDetailBean> getEventsPaged(Context context, String did, int limit, int offset) {
        SQLiteDatabase db = DataBaseHelper.getReadableDB(context);
        String[] columns = {"id", "fileID", EVENT_INDEX, EVENT_TYPE, EVENT_TIME, EVENT_VALUE, "recordIndex", HAVE_PICTURE, "picturePath"};
        List<EventDetailBean> list = new ArrayList<>();
        String limitStr = offset >= 0 && limit > 0 ? (offset + "," + limit) : null;
        Cursor cursor = db.query(DATABASE_EVENT_TABLE, columns, "did=?", new String[]{did}, null, null, "eventTime desc, id desc", limitStr);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int dbId = cursor.getInt(cursor.getColumnIndex("id"));
                int fileID = cursor.getInt(cursor.getColumnIndex("fileID"));
                int eventIndex = cursor.getInt(cursor.getColumnIndex(EVENT_INDEX));
                int eventType = cursor.getInt(cursor.getColumnIndex(EVENT_TYPE));
                int eventTime = cursor.getInt(cursor.getColumnIndex(EVENT_TIME));
                int endTime = cursor.getInt(cursor.getColumnIndex(EVENT_VALUE));
                int recordIndex = cursor.getInt(cursor.getColumnIndex("recordIndex"));
                int havePicture = cursor.getInt(cursor.getColumnIndex(HAVE_PICTURE));
                String picturePath = havePicture == 1 ? cursor.getString(cursor.getColumnIndex("picturePath")) : null;
                if (eventType != 513 && eventType != 514) {
                    EventDetailBean bean = new EventDetailBean(did, fileID);
                    bean.setId(dbId);
                    bean.setDid(did);
                    bean.setEventIndex(eventIndex);
                    bean.setEventType(eventType);
                    bean.setEventTime(eventTime);
                    bean.setEndTime(endTime);
                    bean.setRecordIndex(recordIndex);
                    bean.setHavePicture(havePicture);
                    bean.setPicturePath(picturePath);
                    list.add(bean);
                }
            }
            cursor.close();
        }
        return list;
    }

    public static List<EventDetailBean> getAllEvent(Context context, String did) {
        return getEventsPaged(context, did, 300, 0);
    }
}
