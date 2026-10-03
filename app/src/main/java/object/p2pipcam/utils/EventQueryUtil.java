package object.p2pipcam.utils;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import com.easyview.basecamera.ICamera;
import com.easyview.camera.EVBaseCamera;
import com.easyview.common.WifiUtils;
import com.easyview.table.EventIndexTable;
import com.easyview.table.EventTable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
public class EventQueryUtil {
    private static Map<String, EventQueryUtil> utils = new HashMap();
    private EVBaseCamera _camera;
    private Context _context;
    private boolean _firstRun = false;
    private int _downPictureCount = 0;
    private int _eventListIndex = 0;
    private List<Integer> _missPictureList = null;
    private ICamera.IRespondListener _listener = null;
    private Handler _handler = new Handler();
    private long _lastActive = 0;
    Runnable runnable_queryEventInfo = new Runnable() { // from class: object.p2pipcam.utils.EventQueryUtil.1
        @Override // java.lang.Runnable
        public void run() {
            EventQueryUtil.this.queryEventInfo();
        }
    };
    Runnable runnable_queryEventList = new Runnable() { // from class: object.p2pipcam.utils.EventQueryUtil.2
        @Override // java.lang.Runnable
        public void run() {
            EventQueryUtil.this.queryEventList(EventQueryUtil.this._eventListIndex);
        }
    };
    private ICamera.IRespondListener onEventInfo = new ICamera.IRespondListener() { // from class: object.p2pipcam.utils.EventQueryUtil.3
        @Override // com.easyview.basecamera.ICamera.IRespondListener
        public void OnRespondResult(ICamera camera, int cmd, int result) {
            Log.i("Event", String.format("onEventInfo cmd: %d result:%d ", Integer.valueOf(cmd), Integer.valueOf(result)));
            EventQueryUtil.this._handler.removeCallbacks(EventQueryUtil.this.runnable_queryEventInfo);
            if (EventQueryUtil.this._firstRun) {
                EVBaseCamera evCamera = (EVBaseCamera) camera;
                EventIndexTable.getInstance(Pub.getContext()).updateIndex(camera.getID(), evCamera.GetEventFileID(), evCamera.getEventIndex());
                EventQueryUtil.this.stop();
                Log.i("Event", String.format("first run ,query event finish ", new Object[0]));
                return;
            }
            if (result <= 0) {
                Log.i("Event", String.format("no tf,query event finish", new Object[0]));
                EventQueryUtil.this.stop();
                return;
            }
            int last = EventIndexTable.getInstance(Pub.getContext()).getLastIndex(camera.getID(), ((EVBaseCamera) camera).GetEventFileID());
            EventQueryUtil.this._eventListIndex = last + 1;
            Log.i("Event", String.format("ready queryEventList %d -> %d", Integer.valueOf(EventQueryUtil.this._eventListIndex), Integer.valueOf(result)));
            if (EventQueryUtil.this._eventListIndex < result) {
                if (EventQueryUtil.this._eventListIndex < result - 200) {
                    EventQueryUtil.this._eventListIndex = result - 200;
                }
                EventQueryUtil.this.queryEventList(EventQueryUtil.this._eventListIndex);
                return;
            }
            EventQueryUtil.this.startQueryEvent();
        }
    };
    private ICamera.IRespondListener onEventList = new ICamera.IRespondListener() { // from class: object.p2pipcam.utils.EventQueryUtil.4
        @Override // com.easyview.basecamera.ICamera.IRespondListener
        public void OnRespondResult(ICamera camera, int cmd, int result) {
            Log.i("Event", String.format("onEventList cmd: %d result:%d ", Integer.valueOf(cmd), Integer.valueOf(result)));
            if (cmd + 1 < result) {
                int index = cmd + 1;
                EventQueryUtil.this.queryEventList(index);
            } else {
                EventQueryUtil.this.startQueryEvent();
            }
        }
    };
    private ICamera.IRespondListener onEvent = new ICamera.IRespondListener() { // from class: object.p2pipcam.utils.EventQueryUtil.5
        @Override // com.easyview.basecamera.ICamera.IRespondListener
        public void OnRespondResult(ICamera camera, int cmd, int result) {
            Log.i("Event", String.format("onEvent cmd: %d result:%d ", Integer.valueOf(cmd), Integer.valueOf(result)));
            if (EventQueryUtil.this._listener != null) {
                EventQueryUtil.this._listener.OnRespondResult(EventQueryUtil.this._camera, cmd, result);
            }
            EventQueryUtil.this._downPictureCount++;
            if (EventQueryUtil.this._missPictureList.size() > 0) {
                EventQueryUtil.this._missPictureList.remove(0);
            }
            EventQueryUtil.this.queryEventPicture();
        }
    };

    public static void add(Context context, EVBaseCamera camera, boolean firstRun, ICamera.IRespondListener listener) {
        String did = camera.getID();
        if (utils.containsKey(did)) {
            EventQueryUtil util = utils.get(did);
            util.setListener(listener);
            if (util.getIdleMS() < 5000) {
                Log.i("Event", String.format("is query event! ", new Object[0]));
                return;
            } else {
                Log.i("Event", String.format("query event timeout,restart! ", new Object[0]));
                util.stop();
            }
        }
        EventQueryUtil util2 = new EventQueryUtil(context, camera);
        utils.put(did, util2);
        util2.setListener(listener);
        util2.start(firstRun);
    }

    public static void stopAll() {
        for (EventQueryUtil util : utils.values()) {
            util.stop();
        }
        utils.clear();
    }

    public EventQueryUtil(Context context, EVBaseCamera camera) {
        this._camera = null;
        this._context = null;
        this._context = context;
        this._camera = camera;
    }

    public void start(boolean firstRun) {
        this._firstRun = firstRun;
        this._handler.postDelayed(this.runnable_queryEventInfo, 200L);
    }

    public void setListener(ICamera.IRespondListener listener) {
        Log.i("Event", "setListener:" + listener);
        if (listener != null) {
            this._listener = listener;
        }
    }

    public void stop() {
        this._handler.removeCallbacks(this.runnable_queryEventInfo);
        utils.remove(this._camera.getID());
        this._listener = null;
    }

    public int getIdleMS() {
        long now = System.currentTimeMillis();
        return (int) (now - this._lastActive);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void queryEventInfo() {
        if (this._camera.isStartVideo()) {
            this._handler.postDelayed(this.runnable_queryEventInfo, 2000L);
            return;
        }
        Log.i("Event", String.format("queryEventInfo... ", new Object[0]));
        this._lastActive = System.currentTimeMillis();
        this._camera.queryEventInfo(this.onEventInfo);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void queryEventList(int index) {
        if (this._camera.isOnline()) {
            if (this._camera.isStartVideo()) {
                this._handler.postDelayed(this.runnable_queryEventList, 2000L);
                return;
            }
            Log.i("Event", String.format("queryEventList %d... ", Integer.valueOf(index)));
            this._lastActive = System.currentTimeMillis();
            this._camera.queryEventList(index, index + 30, this.onEventList);
        }
    }

    public void startQueryEvent() {
        int count;
        if (!WifiUtils.checkWifiConnection(this._context)) {
            count = 5;
        } else {
            count = 50;
        }
        this._missPictureList = EventTable.getInstance(Pub.getContext()).lastNoPictures(this._camera.getID(), this._camera.GetEventFileID(), count);
        this._downPictureCount = 0;
        queryEvent();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void queryEvent() {
        if (this._camera.isOnline()) {
            if (this._camera.isStartVideo()) {
                this._handler.postDelayed(new Runnable() { // from class: object.p2pipcam.utils.EventQueryUtil.6
                    @Override // java.lang.Runnable
                    public void run() {
                        EventQueryUtil.this.queryEvent();
                    }
                }, 2000L);
            } else {
                queryEventPicture();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void queryEventPicture() {
        if (!this._camera.isOnline()) {
            Log.i("Event", String.format("device in offline,query event finish ", new Object[0]));
            stop();
            return;
        }
        if (this._missPictureList.size() == 0) {
            Log.i("Event", String.format("query event finish ", new Object[0]));
            stop();
            return;
        }
        if (!WifiUtils.checkWifiConnection(this._context) && this._downPictureCount > 5) {
            Log.i("Event", String.format("no wifi,query event finish %d", Integer.valueOf(this._downPictureCount)));
            stop();
        } else {
            if (this._camera.isStartVideo()) {
                this._handler.postDelayed(new Runnable() { // from class: object.p2pipcam.utils.EventQueryUtil.7
                    @Override // java.lang.Runnable
                    public void run() {
                        EventQueryUtil.this.queryEvent();
                    }
                }, 2000L);
                return;
            }
            int index = this._missPictureList.get(0).intValue();
            Log.i("Event", String.format("queryEvent %d... ", Integer.valueOf(index)));
            this._lastActive = System.currentTimeMillis();
            this._camera.queryEvent(index, null, this.onEvent);
        }
    }
}
