package com.easyview.basecamera;

import java.util.LinkedList;

/* loaded from: classes.dex */
public class VideoFrameQueue {
    private volatile LinkedList<VideoFrame> listData = new LinkedList<>();
    private volatile int mSize = 0;

    public synchronized int getCount() {
        return this.mSize;
    }

    public synchronized void addLast(VideoFrame node) {
        if (this.mSize > 150) {
            this.listData.clear();
        }
        this.listData.addLast(node);
        this.mSize++;
    }

    public synchronized VideoFrame removeHead() {
        VideoFrame removeFirst;
        if (this.mSize == 0) {
            removeFirst = null;
        } else {
            removeFirst = this.listData.removeFirst();
            this.mSize--;
        }
        return removeFirst;
    }

    public synchronized void removeAll() {
        if (!this.listData.isEmpty()) {
            this.listData.clear();
        }
        this.mSize = 0;
    }
}
