package struct;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/* loaded from: classes.dex */
public class LEDataOutputStream implements DataOutput {
    protected DataOutputStream d;
    byte[] w = new byte[8];

    public LEDataOutputStream(OutputStream out) {
        this.d = new DataOutputStream(out);
    }

    @Override // java.io.DataOutput
    public final void writeShort(int v) throws IOException {
        this.w[0] = (byte) v;
        this.w[1] = (byte) (v >> 8);
        this.d.write(this.w, 0, 2);
    }

    @Override // java.io.DataOutput
    public final void writeChar(int v) throws IOException {
        this.w[0] = (byte) v;
        this.w[1] = (byte) (v >> 8);
        this.d.write(this.w, 0, 2);
    }

    @Override // java.io.DataOutput
    public final void writeInt(int v) throws IOException {
        this.w[0] = (byte) v;
        this.w[1] = (byte) (v >> 8);
        this.w[2] = (byte) (v >> 16);
        this.w[3] = (byte) (v >> 24);
        this.d.write(this.w, 0, 4);
    }

    @Override // java.io.DataOutput
    public final void writeLong(long v) throws IOException {
        this.w[0] = (byte) v;
        this.w[1] = (byte) (v >> 8);
        this.w[2] = (byte) (v >> 16);
        this.w[3] = (byte) (v >> 24);
        this.w[4] = (byte) (v >> 32);
        this.w[5] = (byte) (v >> 40);
        this.w[6] = (byte) (v >> 48);
        this.w[7] = (byte) (v >> 56);
        this.d.write(this.w, 0, 8);
    }

    @Override // java.io.DataOutput
    public final void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    @Override // java.io.DataOutput
    public final void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    @Override // java.io.DataOutput
    public final void writeChars(String s) throws IOException {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            writeChar(s.charAt(i));
        }
    }

    @Override // java.io.DataOutput
    public final synchronized void write(int b) throws IOException {
        this.d.write(b);
    }

    @Override // java.io.DataOutput
    public final synchronized void write(byte[] b, int off, int len) throws IOException {
        this.d.write(b, off, len);
    }

    public void flush() throws IOException {
        this.d.flush();
    }

    @Override // java.io.DataOutput
    public final void writeBoolean(boolean v) throws IOException {
        this.d.writeBoolean(v);
    }

    @Override // java.io.DataOutput
    public final void writeByte(int v) throws IOException {
        this.d.writeByte(v);
    }

    @Override // java.io.DataOutput
    public final void writeBytes(String s) throws IOException {
        this.d.writeBytes(s);
    }

    @Override // java.io.DataOutput
    public final void writeUTF(String str) throws IOException {
        this.d.writeUTF(str);
    }

    public final int size() {
        return this.d.size();
    }

    @Override // java.io.DataOutput
    public final void write(byte[] b) throws IOException {
        this.d.write(b, 0, b.length);
    }

    public final void close() throws IOException {
        this.d.close();
    }
}
