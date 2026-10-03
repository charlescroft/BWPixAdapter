package com.easyview.struct;

import android.util.Log;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.TimeZone;
import struct.CString;
import struct.StructClass;
import struct.StructException;
import struct.StructField;
import struct.StructPacker;

/* loaded from: classes.dex */
public class EVCommandDefs {
    public static final short DEL_EVENTS = 263;
    public static final short DOWNLOAD_RECORD = 261;
    public static final short DOWN_RECORD = 259;
    public static final short ENABLE_PAIRING = 282;
    public static final short EV_COMMAND_GET_IOSWITCH = 304;
    public static final short EV_COMMAND_LCD_CONTROL = 307;
    public static final short EV_COMMAND_LIGHT_CONTROL = 320;
    public static final short EV_COMMAND_OPEN_IOSWITCH = 306;
    public static final short EV_COMMAND_SET_IOSWITCH = 305;
    public static final short GET_CAPS = 257;
    public static final short GET_EVENT = 266;
    public static final short GET_EVENTS = 258;
    public static final short GET_EVENT_INFO = 264;
    public static final short GET_EVENT_LIST = 265;
    public static final short GET_EXT_THRES = 279;
    public static final int PARAMS_BRIGHT = 8;
    public static final int PARAMS_CONTRAST = 16;
    public static final int PARAMS_DATECODE = 2;
    public static final int PARAMS_ROTATE = 4;
    public static final int PARAMS_WIFINAME = 1;
    public static final short PLAY_MUSIC = 281;
    public static final short QUERY_DEVINFO = 515;
    public static final short QUERY_WIFI_RESULT = 513;
    public static final short SEARCH_RECORD_LIST = 260;
    public static final short SET_EXT_THRES = 280;
    public static final short SET_LANGUAGE = 514;
    public static final short STOP_DOWN_RECORD = 262;
    public static final short UPGRADE_CHECK = 517;
    public static final short UPGRADE_DEVICE = 516;

    @StructClass
    public static class DeviceInfoParam {
        public static final int struct_size = 36;

        @StructField(order = 1)
        public CString DeviceVersion = new CString(16);

        @StructField(order = 2)
        public CString UpgradeVersion = new CString(16);

        @StructField(order = 0)
        public int have_upgrade;
    }

    @StructClass
    public static class DeviceParams {
        public static final int struct_size = 144;

        @StructField(order = 3)
        public int brightness;

        @StructField(order = 4)
        public int contrast;

        @StructField(order = 1)
        public short date_code;

        @StructField(order = 0)
        public int flag;

        @StructField(order = 2)
        public short rotate;

        @StructField(order = 5)
        public CString WiFiName = new CString(64);

        @StructField(order = 6)
        public CString DefaultName = new CString(64);
    }

    @StructClass
    public static class EVCommandStruct {
        public static final byte SYMBOL = 69;
        public static final int struct_size = 8;

        @StructField(order = 2)
        public short command;

        @StructField(order = 3)
        public int length;

        @StructField(order = 1)
        public byte size;

        @StructField(order = 0)
        public byte symbol;
    }

    @StructClass
    public static class EVCommonResp {
        public static final int struct_size = 32;

        @StructField(order = 0)
        public int command;

        @StructField(order = 1)
        public int data0;

        @StructField(order = 2)
        public int data1;

        @StructField(order = 3)
        public int data2;

        @StructField(order = 4)
        public int data3;

        @StructField(order = 5)
        public int data4;

        @StructField(order = 6)
        public int data5;

        @StructField(order = 7)
        public int data6;
    }

    @StructClass
    public static class EVDownRecordReq {
        public static final int struct_size = 8;

        @StructField(order = 1)
        public int offset;

        @StructField(order = 0)
        public int recordIndex;
    }

    @StructClass
    public static class EVDownRecordResp {
        public static final int struct_size = 4;

        @StructField(order = 0)
        public int file_size;
    }

    @StructClass
    public static class EVEventListResp {
        public static final int struct_size = 8;

        @StructField(order = 3)
        public short count;

        @StructField(order = 2)
        public byte endflag;

        @StructField(order = 1)
        public byte index;

        @StructField(order = 0)
        public int total;
    }

    @StructClass
    public static class EVRecordDataHeader {
        public static final int struct_size = 20;

        @StructField(order = 1)
        public int begin_time;

        @StructField(order = 4)
        public int curr_size;

        @StructField(order = 2)
        public int end_time;

        @StructField(order = 0)
        public int index;

        @StructField(order = 3)
        public int total_size;
    }

    @StructClass
    public static class EVSearchEventReq {
        public static final int struct_size = 16;

        @StructField(order = 0)
        public int begin_index;

        @StructField(order = 1)
        public int end_index;

        @StructField(order = 3)
        public int resv;

        @StructField(order = 2)
        public int with_picture;
    }

    @StructClass
    public static class EVSearchEventResp {
        public static final int struct_size = 4;

        @StructField(order = 0)
        public int count;
    }

    @StructClass
    public static class EVSearchRecordReq {
        public static final int struct_size = 8;

        @StructField(order = 0)
        public int begin_time;

        @StructField(order = 1)
        public int end_time;

        @StructField(order = 2)
        public int with_snap;
    }

    @StructClass
    public static class EVSearchRecordResp {
        public static final int struct_size = 4;

        @StructField(order = 0)
        public int record_count;
    }

    @StructClass
    public static class EVSimpleReq {
        public static final int struct_size = 16;

        @StructField(order = 0)
        public int data0;

        @StructField(order = 1)
        public int data1;

        @StructField(order = 2)
        public int data2;

        @StructField(order = 3)
        public int data3;
    }

    @StructClass
    public static class EVSimpleResp {
        public static final int struct_size = 4;

        @StructField(order = 0)
        public int result;
    }

    @StructClass
    public static class Event {
        public static final int struct_size = 20;

        @StructField(order = 3)
        public int event_time;

        @StructField(order = 2)
        public short event_type;

        @StructField(order = 5)
        public int index;

        @StructField(order = 1)
        public byte is_valid;

        @StructField(order = 6)
        public int record_index;

        @StructField(order = 0)
        public byte symbol;

        @StructField(order = 4)
        public int value;
    }

    @StructClass
    public static class EventInfo {
        public static final int struct_size = 12;

        @StructField(order = 1)
        public int file_id;

        @StructField(order = 2)
        public int index;

        @StructField(order = 0)
        public int total;
    }

    @StructClass
    public static class EventListReq {
        public static final int struct_size = 24;

        @StructField(order = 3)
        public int begin_index;

        @StructField(order = 1)
        public EVTime begin_time;

        @StructField(order = 2)
        public EVTime end_time;

        @StructField(order = 0)
        public int event_type;
    }

    @StructClass
    public static class ExtInfo {
        public static final int struct_size = 8;

        @StructField(order = 0)
        public int item_type;

        @StructField(order = 1)
        public int value;
    }

    @StructClass
    public static class ExtInfos {
        public static final int struct_size = 84;

        @StructField(order = 0)
        public int count;

        @StructField(order = 1)
        public ExtInfo[] values = new ExtInfo[10];
    }

    @StructClass
    public static class ExtThreshold {
        public static final int struct_size = 12;

        @StructField(order = 2)
        public int alarm_lower;

        @StructField(order = 1)
        public int alarm_upper;

        @StructField(order = 0)
        public int item_type;
    }

    @StructClass
    public static class ExtThresholds {
        public static final int struct_size = 124;

        @StructField(order = 0)
        public int count;

        @StructField(order = 1)
        public ExtThreshold[] values = new ExtThreshold[10];
    }

    @StructClass
    public static class IOSwitchParam {
        public static final int struct_size = 20;

        @StructField(order = 1)
        public CString Name = new CString(16);

        @StructField(order = 0)
        public int enable;
    }

    @StructClass
    public static class IOSwitchParams {
        public static final int struct_size = 160;

        @StructField(order = 0)
        public IOSwitchParam[] params = new IOSwitchParam[8];
    }

    @StructClass
    public static class LCDCtrlParam {
        public static final int struct_size = 128;

        @StructField(order = 0)
        public int cmd;

        @StructField(order = 1)
        public CString text = new CString(ExtThresholds.struct_size);
    }

    @StructClass
    public static class RecordState {
        public static final int struct_size = 8;

        @StructField(order = 1)
        public int record_period;

        @StructField(order = 0)
        public int record_state;
    }

    @StructClass
    public static class StorageStateStruct {
        public static final int struct_size = 20;

        @StructField(order = 4)
        public int power_val;

        @StructField(order = 3)
        public int record_duration;

        @StructField(order = 2)
        public int record_state;

        @StructField(order = 1)
        public int remain_bytes;

        @StructField(order = 0)
        public int total_bytes;
    }

    @StructClass
    public static class EVTime {

        @StructField(order = 2)
        public byte day;

        @StructField(order = 4)
        public byte hour;

        @StructField(order = 5)
        public byte minute;

        @StructField(order = 1)
        public byte month;

        @StructField(order = 6)
        public byte second;

        @StructField(order = 3)
        public byte wday;

        @StructField(order = 0)
        public short year;

        public void set(long time) {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("gmt"));
            cal.setTimeInMillis(time);
            this.year = (short) cal.get(1);
            this.month = (byte) (cal.get(2) + 1);
            this.day = (byte) cal.get(5);
            this.wday = (byte) cal.get(7);
            this.hour = (byte) cal.get(11);
            this.minute = (byte) cal.get(12);
            this.second = (byte) cal.get(13);
        }
    }

    public static byte[] MakeEventListReqPacket(long beginTime, long endTime, int begin_index) throws StructException {
        StructPacker packer = new StructPacker(ByteOrder.LITTLE_ENDIAN);
        EVCommandStruct s = new EVCommandStruct();
        s.symbol = EVCommandStruct.SYMBOL;
        s.size = (byte) 8;
        s.command = GET_EVENTS;
        s.length = 24;
        EventListReq req = new EventListReq();
        req.begin_index = begin_index;
        req.begin_time.set(beginTime);
        req.end_time.set(endTime);
        packer.writeObject(s);
        packer.writeObject(req);
        return packer.toArray();
    }

    public static byte[] MakeDownRecordReqPacket(int recordIndex, int offset) throws StructException {
        StructPacker packer = new StructPacker(ByteOrder.LITTLE_ENDIAN);
        EVCommandStruct s = new EVCommandStruct();
        s.symbol = EVCommandStruct.SYMBOL;
        s.size = (byte) 8;
        s.command = DOWNLOAD_RECORD;
        s.length = 8;
        EVDownRecordReq req = new EVDownRecordReq();
        req.recordIndex = recordIndex;
        req.offset = offset;
        packer.writeObject(s);
        packer.writeObject(req);
        return packer.toArray();
    }

    public static byte[] MakeSearchRecordReqPacket(int begin_time, int end_time, int with_snap) throws StructException {
        StructPacker packer = new StructPacker(ByteOrder.LITTLE_ENDIAN);
        EVCommandStruct s = new EVCommandStruct();
        s.symbol = EVCommandStruct.SYMBOL;
        s.size = (byte) 8;
        s.command = SEARCH_RECORD_LIST;
        s.length = 8;
        EVSearchRecordReq req = new EVSearchRecordReq();
        req.begin_time = begin_time;
        req.end_time = end_time;
        req.with_snap = with_snap;
        packer.writeObject(s);
        packer.writeObject(req);
        return packer.toArray();
    }

    public static byte[] MakeSearchEventReqPacket(int begin_index, int begin_time, int end_time, int with_snap) throws StructException {
        StructPacker packer = new StructPacker(ByteOrder.LITTLE_ENDIAN);
        EVCommandStruct s = new EVCommandStruct();
        s.symbol = EVCommandStruct.SYMBOL;
        s.size = (byte) 8;
        s.command = GET_EVENTS;
        s.length = 16;
        EVSearchEventReq req = new EVSearchEventReq();
        req.begin_index = begin_index;
        req.end_index = begin_time;
        req.with_picture = with_snap;
        packer.writeObject(s);
        packer.writeObject(req);
        return packer.toArray();
    }

    public static byte[] MakeSimpleReqPacket(short command, int... params) throws StructException {
        StructPacker packer = new StructPacker(ByteOrder.LITTLE_ENDIAN);
        EVCommandStruct s = new EVCommandStruct();
        s.symbol = EVCommandStruct.SYMBOL;
        s.size = (byte) 8;
        s.command = command;
        s.length = params.length * 4;
        Log.i("PPCS", "MakeSimpleReqPacket:" + ((int) command) + " len:" + s.length);
        packer.writeObject(s);
        for (int param : params) {
            try {
                packer.writeInt(param);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return packer.toArray();
    }

    public static byte[] MakeObjectReqPacket(short command, Object obj, int length) throws StructException {
        StructPacker packer = new StructPacker(ByteOrder.LITTLE_ENDIAN);
        EVCommandStruct s = new EVCommandStruct();
        s.symbol = EVCommandStruct.SYMBOL;
        s.size = (byte) 8;
        s.command = command;
        s.length = length;
        packer.writeObject(s);
        packer.writeObject(obj);
        return packer.toArray();
    }

    public static byte[] MakeCommonReqPacket(short command, Object obj, int size) throws StructException {
        StructPacker packer = new StructPacker(ByteOrder.LITTLE_ENDIAN);
        EVCommandStruct s = new EVCommandStruct();
        s.symbol = EVCommandStruct.SYMBOL;
        s.size = (byte) 8;
        s.command = command;
        s.length = size;
        packer.writeObject(s);
        packer.writeObject(obj);
        return packer.toArray();
    }

    public static byte[] MakeDelEventsReqPacket(int[] data) throws StructException, IOException {
        StructPacker packer = new StructPacker(ByteOrder.LITTLE_ENDIAN);
        EVCommandStruct s = new EVCommandStruct();
        s.symbol = EVCommandStruct.SYMBOL;
        s.size = (byte) 8;
        s.command = DEL_EVENTS;
        s.length = data.length;
        packer.writeObject(s);
        packer.writeIntArray(data, data.length);
        return packer.toArray();
    }

    public static byte[] MakeIOSwitchParamsReqPacket(IOSwitchParam[] params) throws StructException, IOException, IllegalAccessException, InvocationTargetException {
        StructPacker packer = new StructPacker(ByteOrder.LITTLE_ENDIAN);
        EVCommandStruct s = new EVCommandStruct();
        s.symbol = EVCommandStruct.SYMBOL;
        s.size = (byte) 8;
        s.command = EV_COMMAND_SET_IOSWITCH;
        s.length = params.length * 20;
        packer.writeObject(s);
        packer.writeObjectArray(params, params.length);
        return packer.toArray();
    }
}
