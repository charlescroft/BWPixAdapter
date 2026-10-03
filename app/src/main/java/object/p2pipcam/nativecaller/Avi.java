package object.p2pipcam.nativecaller;

import object.p2pipcam.utils.Pub;

/* loaded from: classes.dex */
public class Avi {
    public static final int AUDIO_FRAME = 1;
    private static final int MAX_BUF_SIZE = 6220800;
    public static final int VIDEO_FRAME = 2;
    private int _handle = 0;
    private byte[] _buffer = new byte[MAX_BUF_SIZE];
    private byte[] _fileInfo = new byte[32];
    private byte[] _frameInfo = new byte[16];
    private int _frame_size = 0;
    private int _duration = 0;
    private int _height = 0;
    private int _width = 0;
    private int _bitrate = 0;
    private int _framerate = 0;

    public int Open(String fileName) {
        this._handle = NativeCaller.openAvi(fileName, this._fileInfo, 32);
        this._frame_size = TotalDuration();
        this._height = Pub.byte2int(this._fileInfo, 4);
        this._width = Pub.byte2int(this._fileInfo, 8);
        this._bitrate = Pub.byte2int(this._fileInfo, 12);
        this._framerate = Pub.byte2int(this._fileInfo, 16);
        this._duration = 0;
        return this._handle;
    }

    public void Close() {
        NativeCaller.closeAvi(this._handle);
    }

    public byte[] Frame() {
        this._frame_size = NativeCaller.readFrame(this._handle, this._buffer, MAX_BUF_SIZE, this._frameInfo, 16);
        if (GetFrameType() == 2) {
            int duration = Pub.byte2int(this._frameInfo, 4);
            this._duration = duration;
        }
        return this._buffer;
    }

    public void SeekNext(int pts) {
        NativeCaller.seekNextKeyFrame(this._handle, pts);
    }

    public int TotalDuration() {
        return Pub.byte2int(this._fileInfo, 0);
    }

    public int getHeight() {
        return this._height;
    }

    public int getWidth() {
        return this._width;
    }

    public int getBitRate() {
        return this._bitrate;
    }

    public int getFrameRate() {
        return this._framerate;
    }

    public int GetFrameType() {
        return Pub.byte2int(this._frameInfo, 0);
    }

    public int Duration() {
        return this._duration;
    }

    public boolean isAudioFrame() {
        return GetFrameType() == 1;
    }

    public boolean isVideoFrame() {
        return GetFrameType() == 2;
    }

    public int frameSize() {
        return this._frame_size;
    }

    public boolean EOF() {
        return this._frame_size == 0;
    }
}
