package com.easyview.camera;

import android.content.Context;
import android.util.Log;
import com.easyview.basecamera.BaseCamera;
import com.easyview.basecamera.ICamera;
import com.easyview.bean.AlermBean;
import com.easyview.struct.EVCommandDefs;
import com.easyview.table.EventTable;
import com.easyview.table.RecordTable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import object.p2pipcam.bean.EventDetailBean;
import object.p2pipcam.utils.Pub;
import struct.StructException;
import struct.StructUnpacker;

/* loaded from: classes.dex */
public abstract class EVBaseCamera extends BaseCamera {
    private static final int LOW_POWER_FLAG = 2;
    private static final int RECORD_STATE_FLAG = 1;
    protected EVCommandDefs.DeviceInfoParam DevInfoParam;
    protected EVCommandDefs.DeviceParams DeviceParams;
    protected AlermBean _alermBean;
    protected ICamera.IDataListener _audioDataListener;
    protected ICamera.IRespondListener _commandListener;
    protected ICamera.IRespondListener _deviceStateListener;
    protected int _eventIndex;
    protected int _eventTime;
    protected ICamera.IRespondListener _extSetThresListener;
    protected ICamera.IRespondListener _extThresListener;
    protected ICamera.IRespondListener _getEventInfoListener;
    protected boolean _isStartVideo;
    private List<EventDetailBean> _listEventDetail;
    protected ICamera.IRespondListener _playAudioListener;
    protected int _quality;
    protected ICamera.IRespondListener _queryEventInfoListener;
    protected ICamera.IRespondListener _queryEventListener;
    protected ICamera.IRespondListener _queryEventsListener;
    protected ICamera.IRespondListener _requestParamsListener;
    protected ICamera.IRespondListener _searchEventsListener;
    protected ICamera.IRespondListener _statusListener;
    protected ICamera.IRespondListener _storageStateListener;
    protected int _totalEvents;
    protected ICamera.IDataListener _videoDataListener;
    protected ICamera.IYUVDataListener _yuvDataListener;
    protected ICamera.IDownloadListener downRecordListener;

    public EVBaseCamera(String name, String id, String user, String pwd) {
        super(name, id, user, pwd);
        this._listEventDetail = new ArrayList();
        this._quality = 0;
        this._totalEvents = 0;
        this._eventIndex = 0;
        this._isStartVideo = false;
        this._eventTime = 0;
        this.downRecordListener = null;
        this._queryEventInfoListener = null;
        this._extThresListener = null;
        this._extSetThresListener = null;
        this._searchEventsListener = null;
        this._getEventInfoListener = null;
        this._commandListener = null;
        this._playAudioListener = null;
        this._storageStateListener = null;
        this._videoDataListener = null;
        this._audioDataListener = null;
        this._yuvDataListener = null;
        this._queryEventsListener = null;
        this._queryEventListener = null;
        this._deviceStateListener = null;
        this._requestParamsListener = null;
        this._statusListener = null;
        this.DevInfoParam = new EVCommandDefs.DeviceInfoParam();
        this.DeviceParams = new EVCommandDefs.DeviceParams();
        this._alermBean = null;
        this._storageStateStruct.power_val = 1;
        this._alermBean = new AlermBean();
    }

    public String getPicturePath(EVCommandDefs.Event event) {
        File div = Pub.photoSavePathFile();
        Date date = new Date();
        long ms = event.event_time;
        date.setTime(ms * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String fileName = String.format("%04d_%s.jpg", Short.valueOf(event.event_type), sdf.format(date));
        File file = new File(div, fileName);
        return file.getAbsolutePath();
    }

    public void setStorageMemory(int total, int free) {
        this._storageStateStruct.total_bytes = total;
        this._storageStateStruct.remain_bytes = free;
    }

    public boolean haveStorage() {
        return this._storageStateStruct.total_bytes > 0;
    }

    public boolean isLowMemory(int bytes) {
        return this._storageStateStruct.total_bytes != 0 && this._storageStateStruct.remain_bytes <= bytes;
    }

    public boolean isLowPower() {
        return this._storageStateStruct.power_val == 0;
    }

    public boolean isStartVideo() {
        return this._isStartVideo;
    }

    public int getEventTime() {
        return this._eventTime;
    }

    public void setEventTime(int time) {
        this._eventTime = time;
    }

    public int getEventIndex() {
        return this._eventIndex;
    }

    public List<EventDetailBean> readEventDetailList(Context context) {
        this._listEventDetail = EventTable.getAllEvent(context, getID());
        return this._listEventDetail;
    }

    public List<EventDetailBean> getEventDetailList() {
        return this._listEventDetail;
    }

    public int getEventCount(Context context) {
        return EventTable.getEventCount(getID(), context);
    }

    public void setDeviceStateListener(ICamera.IRespondListener listener) {
        this._deviceStateListener = listener;
    }

    @Override // com.easyview.basecamera.BaseCamera
    public int getQuality() {
        return this._quality;
    }

    public EVCommandDefs.DeviceInfoParam getDeviceInfo() {
        return this.DevInfoParam;
    }

    public AlermBean getAlarm() {
        return this._alermBean;
    }

    public void onDeviceState(int state, int quality) {
        this._quality = quality;
        if ((state & 1) != 0) {
            this._isRecording = true;
        } else {
            this._isRecording = false;
        }
        if ((state & 2) != 0) {
            this._storageStateStruct.power_val = 0;
        } else {
            this._storageStateStruct.power_val = 1;
        }
        if (this._deviceStateListener != null) {
            this._deviceStateListener.OnRespondResult(this, 1, state);
        }
    }

    public void OnVideoData(byte[] data, int length) {
        if (this._videoDataListener != null) {
            this._videoDataListener.OnData(this, data, length);
        }
    }

    public void OnYUVData(byte[] data, int len, int width, int height) {
        if (this._yuvDataListener != null) {
            this._yuvDataListener.OnData(this, data, len, width, height);
        }
    }

    public void OnCustomData(int type, byte[] data, int len) {
        Log.i("EVNet", String.format("OnCustomData:0x%X len:%d", Integer.valueOf(type), Integer.valueOf(len)));
        switch (type) {
            case 4:
                try {
                    dealEvent(data, len);
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            case 5:
                try {
                    dealEvents(data, len);
                    break;
                } catch (IOException e2) {
                    e2.printStackTrace();
                    return;
                } catch (StructException e3) {
                    e3.printStackTrace();
                    return;
                }
            case 258:
                try {
                    onEvents(data, len);
                    break;
                } catch (IOException e4) {
                    e4.printStackTrace();
                    return;
                } catch (StructException e5) {
                    e5.printStackTrace();
                    return;
                }
            case 266:
                try {
                    dealEvent(data, len);
                    break;
                } catch (IOException e6) {
                    e6.printStackTrace();
                    return;
                }
        }
    }

    public void OnEventNotify(int index, int type, int beginTime, int endTime, int value) {
        EventTable.getInstance(Pub.getContext()).Save(getID(), this._eventFileID, index, type, beginTime, endTime);
    }

    public void OnStatusNotify(int type) {
        Log.i("EVBaseCamera", "OnStatusNotify:" + type + " l:" + this._statusListener);
        if (this._statusListener != null) {
            this._statusListener.OnRespondResult(this, 0, type);
        }
    }

    public void setStatusListener(ICamera.IRespondListener listener) {
        this._statusListener = listener;
    }

    private void dealEvents(byte[] data, int len) throws IOException, StructException {
        StructUnpacker up = new StructUnpacker(data, ByteOrder.LITTLE_ENDIAN);
        EVCommandDefs.EVEventListResp resp = new EVCommandDefs.EVEventListResp();
        up.readObject(resp);
        Log.i("Event", String.format("event list,total: %d index:%d count:%d flag:%d", Integer.valueOf(resp.total), Byte.valueOf(resp.index), Short.valueOf(resp.count), Byte.valueOf(resp.endflag)));
        for (int i = 0; i < resp.count; i++) {
            EVCommandDefs.Event event = new EVCommandDefs.Event();
            up.readObject(event);
            this._events.add(event);
        }
        if (resp.endflag == 1) {
            Log.i("Event", String.format("event count %d", Integer.valueOf(this._events.size())));
            if (this._queryEventsListener != null) {
                this._queryEventsListener.OnRespondResult(this, 0, this._events.size());
            }
        }
    }

    private void onEvents(byte[] data, int len) throws IOException, StructException {
        if (len == 0) {
            Log.i("Event", String.format("event count %d", Integer.valueOf(this._events.size())));
            if (this._queryEventsListener != null) {
                this._queryEventsListener.OnRespondResult(this, 0, this._events.size());
                return;
            }
            return;
        }
        StructUnpacker up = new StructUnpacker(data, ByteOrder.LITTLE_ENDIAN);
        int count = len / 20;
        Log.i("Event", String.format("onevents count:%d", Integer.valueOf(count)));
        for (int i = 0; i < count; i++) {
            EVCommandDefs.Event event = new EVCommandDefs.Event();
            up.readObject(event);
            this._events.add(event);
        }
    }

    private void dealEvent(byte[] data, int len) throws IOException {
        if (len != 0) {
            File div = Pub.photoSavePathFile();
            if (!div.exists()) {
                div.mkdirs();
            }
            try {
                StructUnpacker up = new StructUnpacker(data, ByteOrder.LITTLE_ENDIAN);
                this.event = new EVCommandDefs.Event();
                up.readObject(this.event);
                Date date = new Date();
                long ms = this.event.event_time;
                date.setTime(ms * 1000);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                String fileName = String.format("%04d_%s.jpg", Short.valueOf(this.event.event_type), sdf.format(date));
                File file = new File(div, fileName);
                FileOutputStream fos = new FileOutputStream(file);
                try {
                    fos.write(data, 20, len - 20);
                    fos.close();
                    Log.i("Event", String.format("%d Save Picture:%s index:%d time:%d", Short.valueOf(this.event.event_type), fileName, Integer.valueOf(this.event.index), Integer.valueOf(this.event.event_time)));
                    EventTable.getInstance(Pub.getContext()).Save(getID(), this._eventFileID, this.event, file.getAbsolutePath());
                    if (this._queryEventListener != null) {
                        this._queryEventListener.OnRespondResult(this, 1, this.event.index);
                    }
                    if (this.event.event_type == 514) {
                        RecordTable.getInstance(Pub.getContext()).Save(getID(), this._eventFileID, this.event, file.getAbsolutePath());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    @Override // com.easyview.basecamera.ICamera
    public void clearListener() {
        this.downRecordListener = null;
        this._queryEventInfoListener = null;
        this._extThresListener = null;
        this._extSetThresListener = null;
        this._searchEventsListener = null;
        this._getEventInfoListener = null;
        this._commandListener = null;
        this._playAudioListener = null;
        this._storageStateListener = null;
        this._queryEventsListener = null;
        this._queryEventListener = null;
    }
}
