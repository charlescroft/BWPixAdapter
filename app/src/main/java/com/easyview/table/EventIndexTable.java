package com.easyview.table;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;
import java.util.Date;
import object.p2pipcam.utils.DataBaseHelper;

/* loaded from: classes.dex */
public class EventIndexTable {
    private static final String DATABASE_TABLE = "event_index";
    public static final String EVENT_INDEX = "event_index";
    public static final String KEY_DID = "did";
    public static final String KEY_ID = "id";
    private SQLiteDatabase _db;
    public static final String FILE_ID = "file_id";
    public static final String UPDATE_TIME = "update_time";
    private static final String CREATE_ALARM_TABLE = String.format("create table %s (%s integer primary key autoincrement, %s text not null,%s int,%s int,%s int);", "event_index", "id", "did", FILE_ID, "event_index", UPDATE_TIME);

    public static EventIndexTable getInstance(SQLiteDatabase db) {
        return new EventIndexTable(db);
    }

    public static EventIndexTable getInstance(Context context) {
        return DataBaseHelper.getInstance(context).getEventIndexTable();
    }

    public EventIndexTable(SQLiteDatabase db) {
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

    public void UpgradeToVersion4() {
        CreateTable();
    }

    public Pair<Integer, Integer> getLastIndex(String did) {
        int fileID = 0;
        int index = 0;
        String sql = String.format("select %s from %s where %s = '%s' order by %s desc limit 1", FILE_ID, "event_index", "did", did, UPDATE_TIME);
        Cursor cursor = this._db.rawQuery(sql, null);
        if (cursor != null) {
            if (cursor.moveToNext()) {
                fileID = cursor.getInt(0);
            }
            cursor.close();
        }
        String sql2 = String.format("select %s from %s where %s = %d order by %s desc limit 1", "event_index", "event_index", FILE_ID, Integer.valueOf(fileID), "event_index");
        Cursor cursor2 = this._db.rawQuery(sql2, null);
        if (cursor2 != null) {
            if (cursor2.moveToNext()) {
                index = cursor2.getInt(0);
            }
            cursor2.close();
        }
        Pair<Integer, Integer> result = new Pair<>(Integer.valueOf(index), Integer.valueOf(fileID));
        if (fileID == 0 && index == 0) {
            return EventTable.getInstance(this._db).getLastIndexAndFID(did);
        }
        return result;
    }

    public int getLastIndex(String did, int fileID) {
        int result = 0;
        String sql = String.format("select %s from %s where %s = %d order by %s desc limit 1", "event_index", "event_index", FILE_ID, Integer.valueOf(fileID), "event_index");
        Cursor cursor = this._db.rawQuery(sql, null);
        if (cursor != null) {
            if (cursor.moveToNext()) {
                result = cursor.getInt(0);
            }
            cursor.close();
        }
        if (result == 0) {
            return EventTable.getInstance(this._db).getLastIndex(did, fileID);
        }
        return result;
    }

    public void updateIndex(String did, int fileID, int index) {
        Date now = new Date();
        int time = (int) (now.getTime() / 1000);
        Log.d("Event", String.format("update event index: %s fileID:%d index:%d", did, Integer.valueOf(fileID), Integer.valueOf(index)));
        if (isExist(did, fileID)) {
            ContentValues cv = new ContentValues();
            cv.put("event_index", Integer.valueOf(index));
            cv.put(UPDATE_TIME, Integer.valueOf(time));
            String Where = String.format("%s = ? and %s = ?", FILE_ID, "did");
            String[] args = {String.valueOf(fileID), did};
            this._db.update("event_index", cv, Where, args);
            return;
        }
        ContentValues cv2 = new ContentValues();
        cv2.put("did", did);
        cv2.put(FILE_ID, Integer.valueOf(fileID));
        cv2.put("event_index", Integer.valueOf(index));
        cv2.put(UPDATE_TIME, Integer.valueOf(time));
        this._db.insert("event_index", null, cv2);
    }

    public boolean isExist(String did, int fileID) {
        String sql = String.format("select * from %s where %s = '%s' and %s = %d", "event_index", "did", did, FILE_ID, Integer.valueOf(fileID));
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
}
