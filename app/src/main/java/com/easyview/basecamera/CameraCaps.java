package com.easyview.basecamera;

import com.easyview.struct.EV_CAPS_STRUCT;
import struct.StructException;
import struct.StructUnpacker;

/* loaded from: classes.dex */
public class CameraCaps {
    private static final byte EV_CAP_BABYCAM_INFO = 2;
    private static final byte EV_CAP_BIVOICE = 4;
    private static final byte EV_CAP_PLAY_AUDIO = 1;
    private static final byte EV_CAP_UNLOCK = 8;
    private EV_CAPS_STRUCT _caps = new EV_CAPS_STRUCT();

    public void read(StructUnpacker up) {
        try {
            up.readObject(this._caps);
        } catch (StructException e) {
            e.printStackTrace();
        }
    }

    private boolean isSupport(byte bit_val) {
        int val = this._caps.caps[0] & bit_val;
        return val == bit_val;
    }

    public boolean isSupportPlayAudio() {
        int val = this._caps.caps[0] & 1;
        return val == 1;
    }

    public boolean isSupportBabyCamInfo() {
        int val = this._caps.caps[0] & 2;
        return val == 2;
    }

    public boolean isSupportBiVoice() {
        return isSupport((byte) 4);
    }

    public boolean isSupportUnlock() {
        return isSupport((byte) 8);
    }
}
