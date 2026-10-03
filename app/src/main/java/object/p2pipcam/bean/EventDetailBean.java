package object.p2pipcam.bean;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.easyview.table.EventTable;
import com.easyview.table.RecordTable;
import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import object.p2pipcam.utils.Pub;

/* loaded from: classes.dex */
public class EventDetailBean implements Serializable {
    private static final long serialVersionUID = 1;
    private int id = 0;
    private String camName;
    private String content;
    private String createtime;
    private String did;
    private int endTime;
    private int eventIndex;
    private int eventTime;
    private int eventType;
    private int fileID;
    private int havePicture;
    private String picturePath;
    private int recordIndex;
    private int haveLocalVideo = -1;
    private boolean is_downing = false;
    private boolean is_can_sel = false;
    private boolean is_selected = false;
    private String eventTimeStr = null;
    private String eventTypeStr = null;
    private String videoPath = null;
    private String periodStr = null;
    public String devID = null;

    public EventDetailBean(String did, int fileID) {
        this.did = did;
        this.fileID = fileID;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getFileID() {
        return this.fileID;
    }

    public void setFileID(int fileID) {
        this.fileID = fileID;
    }

    public int getRecordIndex() {
        return this.recordIndex;
    }

    public void setRecordIndex(int recordIndex) {
        this.recordIndex = recordIndex;
    }

    public int getEventIndex() {
        return this.eventIndex;
    }

    public void setEventIndex(int index) {
        this.eventIndex = index;
    }

    public int getHavePicture() {
        return this.havePicture;
    }

    public void setHavePicture(int havePicture) {
        this.havePicture = havePicture;
    }

    public String getPicturePath() {
        return this.picturePath;
    }

    public String getPicturePath(boolean update) {
        this.picturePath = EventTable.getInstance(Pub.getContext()).getPicturePath(this.fileID, this.did, this.eventIndex);
        if (this.picturePath != null) {
            this.havePicture = 1;
        }
        return this.picturePath;
    }

    public void setPicturePath(String picturePath) {
        this.picturePath = picturePath;
    }

    public int getEventType() {
        return this.eventType;
    }

    public void setEventType(int eventType) {
        this.eventType = eventType;
    }

    public int getEventTime() {
        return this.eventTime;
    }

    public void setEventTime(int eventTime) {
        this.eventTime = eventTime;
    }

    public int getEndTime() {
        return this.endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    public String getCamName() {
        return this.camName;
    }

    public void setCamName(String camName) {
        this.camName = camName;
    }

    public String getDid() {
        return this.did;
    }

    public void setDid(String did) {
        this.did = did;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatetime() {
        return this.createtime;
    }

    public void setCreatetime(String createtime) {
        this.createtime = createtime;
    }

    public String getTimeText() {
        if (this.eventTimeStr == null) {
            Date date = new Date();
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            long ms = this.eventTime;
            date.setTime(ms * 1000);
            this.eventTimeStr = f.format(date);
        }
        return this.eventTimeStr;
    }

    public String getPeriodText() {
        if (this.periodStr == null) {
            String strKey = this.did;
            if (this.devID != null) {
                strKey = this.devID;
            }
            int duration = RecordTable.getInstance(Pub.getContext()).getDuration(strKey, this.fileID, this.recordIndex, this.eventTime);
            if (duration == 0 && (duration = this.endTime - this.eventTime) < 0) {
                duration = 0;
            }
            this.periodStr = String.format("%02d:%02d", Integer.valueOf(duration / 60), Integer.valueOf(duration % 60));
        }
        return this.periodStr;
    }

    public String getTimeFileText() {
        return String.format("TIME:%d", Integer.valueOf(this.eventTime));
    }

    public String getIndexFileText() {
        return String.format("INDEX:%d", Integer.valueOf(this.recordIndex));
    }

    public String getEventText() {
        if (this.eventTypeStr == null) {
            this.eventTypeStr = Pub.getEventText(this.eventType, this.endTime);
        }
        return this.eventTypeStr;
    }

    public int getEventRes() {
        return Pub.getEventRes(this.eventType);
    }

    public Bitmap getBitmap(int inSampleSize) {
        if (this.havePicture != 1) {
            return null;
        }
        Bitmap bitmap = null;
        try {
            File file = new File(this.picturePath);
            if (file.exists()) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = inSampleSize;
                opts.inInputShareable = true;
                opts.inPurgeable = true;
                bitmap = BitmapFactory.decodeFile(this.picturePath, opts);
            }
        } catch (Exception e) {
        }
        if (bitmap == null) {
            return null;
        }
        return bitmap;
    }

    public void recycleBitmap() {
    }

    public void remove() {
        EventTable.getInstance(Pub.getContext()).delEvent(this.did, this.eventType, this.eventTime);
        if (this.eventType == 513) {
            String strKey = this.did;
            if (this.devID != null) {
                strKey = this.devID;
            }
            this.haveLocalVideo = RecordTable.getInstance(Pub.getContext()).getHaveVideo(strKey, this.fileID, this.recordIndex, this.eventTime);
            if (this.haveLocalVideo == 1) {
                this.videoPath = RecordTable.getInstance(Pub.getContext()).getVideoPath(strKey, this.fileID, this.recordIndex, this.eventTime);
                File f = new File(this.videoPath);
                if (f.exists()) {
                    Log.i("EventBean", String.format("del video:%s", this.videoPath));
                    f.delete();
                }
            }
            RecordTable.getInstance(Pub.getContext()).delete(strKey, this.fileID, this.recordIndex);
        }
        removePicture();
    }

    public void removePicture() {
        File file;
        if (this.havePicture == 1 && (file = new File(this.picturePath)) != null && file.exists()) {
            Log.i("EventBean", String.format("del picture:%s", this.picturePath));
            file.delete();
        }
    }

    public int getHaveLocalVideo() {
        String strKey = this.did;
        if (this.devID != null) {
            strKey = this.devID;
        }
        this.haveLocalVideo = RecordTable.getInstance(Pub.getContext()).getHaveVideo(strKey, this.fileID, this.recordIndex, this.eventTime);
        if (this.haveLocalVideo == 1) {
            this.videoPath = RecordTable.getInstance(Pub.getContext()).getVideoPath(strKey, this.fileID, this.recordIndex, this.eventTime);
            File f = new File(this.videoPath);
            if (!f.exists()) {
                Log.i("EventBean", String.format("video file not exits:%s", this.videoPath));
                RecordTable.getInstance(Pub.getContext()).setHaveVideo(strKey, this.fileID, this.recordIndex, this.eventTime);
                this.haveLocalVideo = 0;
            }
        }
        return this.haveLocalVideo;
    }

    public void setHaveLocalVideo(int val) {
        this.haveLocalVideo = val;
    }

    public String getVideoPath() {
        if (this.videoPath == null) {
            String strKey = this.did;
            if (this.devID != null) {
                strKey = this.devID;
            }
            this.videoPath = RecordTable.getInstance(Pub.getContext()).getVideoPath(strKey, this.fileID, this.recordIndex, this.eventTime);
        }
        return this.videoPath;
    }

    public boolean isDowning() {
        return this.is_downing;
    }

    public void setIsDowning(boolean val) {
        this.is_downing = val;
    }

    public boolean isCanSel() {
        return this.is_can_sel;
    }

    public void setCanSel(boolean val) {
        this.is_can_sel = val;
        this.is_selected = false;
    }

    public boolean isSelected() {
        return this.is_selected;
    }

    public void setSelected(boolean val) {
        this.is_selected = val;
    }

    public String toString() {
        return "AlarmLogBean [camName=" + this.camName + ", did=" + this.did + ", content=" + this.content + ", createtime=" + this.createtime + "]";
    }
}
