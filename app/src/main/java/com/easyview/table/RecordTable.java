package com.easyview.table;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.easyview.struct.EVCommandDefs;
import object.p2pipcam.utils.DataBaseHelper;

/* loaded from: classes.dex */
public class RecordTable {
    public static final String FILE_ID = "fileID";
    public static final String KEY_DID = "did";
    public static final String KEY_ID = "id";
    private static final String LOG_TAG = "RecordTable";
    public static final String PICTURE_PATH = "picturePath";
    public static final String RECORD_INDEX = "recordIndex";
    private SQLiteDatabase _db;
    private static final String DATABASE_RECORD_TABLE = "record";
    public static final String BEGIN_TIME = "beginTime";
    public static final String END_TIME = "endTime";
    public static final String HAVE_LOCAL = "haveLocal";
    public static final String PATH_NAME = "pathName";
    private static final String CREATE_RECORD_TABLE = String.format("create table %s (%s integer primary key autoincrement, %s text not null,%s int,%s int,%s int,%s int,%s int,%s text,%s text);", DATABASE_RECORD_TABLE, "id", "did", "fileID", "recordIndex", BEGIN_TIME, END_TIME, HAVE_LOCAL, "picturePath", PATH_NAME);

    public static RecordTable getInstance(SQLiteDatabase db) {
        return new RecordTable(db);
    }

    public static RecordTable getInstance(Context context) {
        return DataBaseHelper.getInstance(context).getRecordTable();
    }

    public RecordTable(SQLiteDatabase db) {
        this._db = null;
        this._db = db;
    }

    public void setDatabase(SQLiteDatabase db) {
        this._db = db;
    }

    public void CreateTable() {
        if (this._db != null) {
            this._db.execSQL(CREATE_RECORD_TABLE);
        }
    }

    public void UpgradeToVersion3() {
        this._db.execSQL("alter table record rename to temp_B");
        CreateTable();
        String sql = String.format("INSERT INTO %s SELECT %s,%s,0,%s,%s,%s,%s,%s,%s FROM temp_B;", DATABASE_RECORD_TABLE, "id", "did", "recordIndex", BEGIN_TIME, END_TIME, HAVE_LOCAL, "picturePath", PATH_NAME);
        this._db.execSQL(sql);
        this._db.execSQL("DROP TABLE temp_B;");
    }

    public long Save(String did, int fileID, EVCommandDefs.Event event, String picPath) {
        if (isExist(did, fileID, event)) {
            return 0L;
        }
        ContentValues initialValues = new ContentValues();
        initialValues.put("did", did);
        initialValues.put("fileID", Integer.valueOf(fileID));
        initialValues.put("recordIndex", Integer.valueOf(event.record_index));
        initialValues.put(BEGIN_TIME, Integer.valueOf(event.event_time));
        initialValues.put(END_TIME, Integer.valueOf(event.event_time));
        initialValues.put(HAVE_LOCAL, (Integer) 0);
        initialValues.put("picturePath", picPath);
        long result = this._db.insert(DATABASE_RECORD_TABLE, null, initialValues);
        Log.d(LOG_TAG, String.format("insert result: %d %s %d %d", Long.valueOf(result), did, Integer.valueOf(event.event_time), Integer.valueOf(event.record_index)));
        return result;
    }

    public long Save(String did, int fileID, int beginTime, int endTime, String picPath) {
        ContentValues initialValues = new ContentValues();
        initialValues.put("did", did);
        initialValues.put("fileID", Integer.valueOf(fileID));
        initialValues.put(BEGIN_TIME, Integer.valueOf(beginTime));
        initialValues.put(END_TIME, Integer.valueOf(endTime));
        initialValues.put(HAVE_LOCAL, (Integer) 0);
        initialValues.put("picturePath", picPath);
        long result = this._db.insert(DATABASE_RECORD_TABLE, null, initialValues);
        Log.d(LOG_TAG, String.format("insert result: %d %s %d %d", Long.valueOf(result), did, Integer.valueOf(beginTime), Integer.valueOf(endTime)));
        return result;
    }

    public boolean Update(String did, int fileID, int recordIndex, int endTime) {
        if (this._db == null) {
            return false;
        }
        int beginTime = getBeginTime(did, fileID, recordIndex);
        if (endTime - beginTime > 3600) {
            return false;
        }
        ContentValues cv = new ContentValues();
        cv.put(END_TIME, Integer.valueOf(endTime));
        String Where = String.format("%s = ? and %s = ? and %s = ?", "did", "recordIndex", BEGIN_TIME);
        String[] args = {did, String.valueOf(recordIndex), String.valueOf(beginTime)};
        long result = this._db.update(DATABASE_RECORD_TABLE, cv, Where, args);
        Log.d(LOG_TAG, String.format("Update result:%d %s %d %d", Long.valueOf(result), did, Integer.valueOf(recordIndex), Integer.valueOf(endTime)));
        return result > 0;
    }

    public int getBeginTime(String did, int fileID, int recordIndex) {
        String sql = String.format("select %s from %s where %s = '%s' AND %s = %d AND %s = %d order by %s desc", BEGIN_TIME, DATABASE_RECORD_TABLE, "did", did, "fileID", Integer.valueOf(fileID), "recordIndex", Integer.valueOf(recordIndex), BEGIN_TIME);
        Cursor cursor = this._db.rawQuery(sql, null);
        int result = 0;
        if (cursor != null) {
            if (cursor.moveToNext()) {
                result = cursor.getInt(cursor.getColumnIndex(BEGIN_TIME));
            }
            cursor.close();
        }
        return result;
    }

    public boolean Update(String did, int fileID, int beginTime, String pathName) {
        if (this._db == null) {
            return false;
        }
        String sql = String.format("UPDATE %s SET %s = 1,%s = '%s' WHERE %s = '%s' AND %s = %d AND %s = %d ", DATABASE_RECORD_TABLE, HAVE_LOCAL, PATH_NAME, pathName, "did", did, "fileID", Integer.valueOf(fileID), BEGIN_TIME, Integer.valueOf(beginTime));
        Log.d(LOG_TAG, String.format("SQL:%s", sql));
        this._db.execSQL(sql);
        return true;
    }

    public String getPicturePath(String did, int fileID, int beginTime) {
        String sql = String.format("select %s from %s where %s = '%s' AND %s = %d and %s = %d", "picturePath", DATABASE_RECORD_TABLE, "did", did, "fileID", Integer.valueOf(fileID), BEGIN_TIME, Integer.valueOf(beginTime));
        Cursor cursor = this._db.rawQuery(sql, null);
        String path = null;
        if (cursor != null) {
            if (cursor.moveToNext()) {
                path = cursor.getString(cursor.getColumnIndex("picturePath"));
            }
            cursor.close();
        }
        return path;
    }

    public String getVideoPath(String did, int fileID, int recordIndex, int eventTime) {
        String sql = String.format("select %s from %s where %s = '%s' AND %s = %d and %s = %d  and %s <= %d and %s >= %d", PATH_NAME, DATABASE_RECORD_TABLE, "did", did, "fileID", Integer.valueOf(fileID), "recordIndex", Integer.valueOf(recordIndex), BEGIN_TIME, Integer.valueOf(eventTime + 15), END_TIME, Integer.valueOf(eventTime - 2));
        Cursor cursor = this._db.rawQuery(sql, null);
        String path = null;
        if (cursor != null) {
            if (cursor.moveToNext()) {
                path = cursor.getString(cursor.getColumnIndex(PATH_NAME));
            }
            cursor.close();
        }
        return path;
    }

    public int getHaveVideo(String did, int fileID, int recordIndex, int eventTime) {
        String sql = String.format("select %s from %s where %s = '%s' AND %s = %d and %s = %d and %s <= %d and %s >= %d", HAVE_LOCAL, DATABASE_RECORD_TABLE, "did", did, "fileID", Integer.valueOf(fileID), "recordIndex", Integer.valueOf(recordIndex), BEGIN_TIME, Integer.valueOf(eventTime + 15), END_TIME, Integer.valueOf(eventTime - 2));
        Cursor cursor = this._db.rawQuery(sql, null);
        int haveVideo = 0;
        if (cursor != null) {
            if (cursor.moveToNext()) {
                haveVideo = cursor.getInt(cursor.getColumnIndex(HAVE_LOCAL));
            }
            cursor.close();
        }
        return haveVideo;
    }

    public boolean setHaveVideo(String did, int fileID, int recordIndex, int eventTime) {
        if (this._db == null) {
            return false;
        }
        ContentValues cv = new ContentValues();
        cv.put(HAVE_LOCAL, (Integer) 0);
        String Where = String.format("%s = ? and %s = ? AND %s = ? and %s = ?", "did", "fileID", BEGIN_TIME, "recordIndex");
        String[] args = {did, String.valueOf(fileID), String.valueOf(eventTime), String.valueOf(recordIndex)};
        long result = this._db.update(DATABASE_RECORD_TABLE, cv, Where, args);
        return result > 0;
    }

    public int getDuration(String did, int fileID, int recordIndex) {
        String sql = String.format("select %s - %s from %s where %s = '%s' AND %s = %d and %s = %d ", END_TIME, BEGIN_TIME, DATABASE_RECORD_TABLE, "did", did, "fileID", Integer.valueOf(fileID), "recordIndex", Integer.valueOf(recordIndex));
        Cursor cursor = this._db.rawQuery(sql, null);
        int haveVideo = 0;
        if (cursor != null) {
            if (cursor.moveToNext()) {
                haveVideo = cursor.getInt(0);
            }
            cursor.close();
        }
        return haveVideo;
    }

    public int getDuration(String did, int fileID, int recordIndex, int eventTime) {
        String sql = String.format("select %s - %s from %s where %s = '%s' AND %s = %d and %s = %d and %s <= %d and %s >= %d", END_TIME, BEGIN_TIME, DATABASE_RECORD_TABLE, "did", did, "fileID", Integer.valueOf(fileID), "recordIndex", Integer.valueOf(recordIndex), BEGIN_TIME, Integer.valueOf(eventTime + 15), END_TIME, Integer.valueOf(eventTime - 2));
        Cursor cursor = this._db.rawQuery(sql, null);
        int haveVideo = 0;
        if (cursor != null) {
            if (cursor.moveToNext()) {
                haveVideo = cursor.getInt(0);
            }
            cursor.close();
        }
        return haveVideo;
    }

    public boolean isExist(String did, int fileID, EVCommandDefs.Event event) {
        String sql = String.format("select * from %s where %s = '%s' AND %s = %d and %s = %d and %s = %d", DATABASE_RECORD_TABLE, "did", did, "fileID", Integer.valueOf(fileID), "recordIndex", Integer.valueOf(event.record_index), BEGIN_TIME, Integer.valueOf(event.event_time));
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

    public boolean delete(String did) {
        return this._db.delete(DATABASE_RECORD_TABLE, "did=? ", new String[]{did}) > 0;
    }

    public boolean delete(String did, int fileID, int recordIndex) {
        String clause = String.format("%s=? AND %s=? and %s=?", "did", "fileID", "recordIndex");
        return this._db.delete(DATABASE_RECORD_TABLE, clause, new String[]{did, String.valueOf(fileID), String.format("%d", Integer.valueOf(recordIndex))}) > 0;
    }
}
