package struct;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteOrder;

/* loaded from: classes.dex */
public class StructPacker extends StructOutput {
    private ByteArrayOutputStream bos;
    protected DataOutput dataOutput;

    protected void init(OutputStream outStream, ByteOrder order) {
        if (order == ByteOrder.LITTLE_ENDIAN) {
            this.dataOutput = new LEDataOutputStream(outStream);
        } else {
            this.dataOutput = new DataOutputStream(outStream);
        }
    }

    public StructPacker() {
        this(new ByteArrayOutputStream(), ByteOrder.BIG_ENDIAN);
    }

    public StructPacker(ByteOrder order) {
        this(new ByteArrayOutputStream(), order);
    }

    public StructPacker(OutputStream os, ByteOrder order) {
        init(os, order);
        this.bos = (ByteArrayOutputStream) os;
    }

    public byte[] pack(Object objectToPack) throws StructException {
        writeObject(objectToPack);
        return this.bos.toByteArray();
    }

    public byte[] toArray() {
        return this.bos.toByteArray();
    }

    @Override // struct.StructOutput
    public void writeBoolean(boolean value) throws IOException {
        this.dataOutput.writeBoolean(value);
    }

    @Override // struct.StructOutput
    public void writeByte(byte value) throws IOException {
        this.dataOutput.writeByte(value);
    }

    @Override // struct.StructOutput
    public void writeShort(short value) throws IOException {
        this.dataOutput.writeShort(value);
    }

    @Override // struct.StructOutput
    public void writeInt(int value) throws IOException {
        this.dataOutput.writeInt(value);
    }

    @Override // struct.StructOutput
    public void writeLong(long value) throws IOException {
        this.dataOutput.writeLong(value);
    }

    @Override // struct.StructOutput
    public void writeChar(char value) throws IOException {
        this.dataOutput.writeChar(value);
    }

    @Override // struct.StructOutput
    public void writeFloat(float value) throws IOException {
        this.dataOutput.writeFloat(value);
    }

    @Override // struct.StructOutput
    public void writeDouble(double value) throws IOException {
        this.dataOutput.writeDouble(value);
    }

    @Override // struct.StructOutput
    public void writeBooleanArray(boolean[] buffer, int len) throws IOException {
        if (len == -1 || len > buffer.length) {
            len = buffer.length;
        }
        for (int i = 0; i < len; i++) {
            this.dataOutput.writeBoolean(buffer[i]);
        }
    }

    @Override // struct.StructOutput
    public void writeByteArray(byte[] buffer, int len) throws IOException {
        if (len != 0) {
            if (len == -1 || len > buffer.length) {
                len = buffer.length;
            }
            this.dataOutput.write(buffer, 0, len);
        }
    }

    @Override // struct.StructOutput
    public void writeCharArray(char[] buffer, int len) throws IOException {
        if (len == -1 || len > buffer.length) {
            len = buffer.length;
        }
        for (int i = 0; i < len; i++) {
            this.dataOutput.writeChar(buffer[i]);
        }
    }

    @Override // struct.StructOutput
    public void writeShortArray(short[] buffer, int len) throws IOException {
        if (len == -1 || len > buffer.length) {
            len = buffer.length;
        }
        for (int i = 0; i < len; i++) {
            this.dataOutput.writeShort(buffer[i]);
        }
    }

    @Override // struct.StructOutput
    public void writeIntArray(int[] buffer, int len) throws IOException {
        if (len == -1 || len > buffer.length) {
            len = buffer.length;
        }
        for (int i = 0; i < len; i++) {
            this.dataOutput.writeInt(buffer[i]);
        }
    }

    @Override // struct.StructOutput
    public void writeLongArray(long[] buffer, int len) throws IOException {
        if (len == -1 || len > buffer.length) {
            len = buffer.length;
        }
        for (int i = 0; i < len; i++) {
            this.dataOutput.writeLong(buffer[i]);
        }
    }

    @Override // struct.StructOutput
    public void writeFloatArray(float[] buffer, int len) throws IOException {
        if (len == -1 || len > buffer.length) {
            len = buffer.length;
        }
        for (int i = 0; i < len; i++) {
            this.dataOutput.writeFloat(buffer[i]);
        }
    }

    @Override // struct.StructOutput
    public void writeDoubleArray(double[] buffer, int len) throws IOException {
        if (len == -1 || len > buffer.length) {
            len = buffer.length;
        }
        for (int i = 0; i < len; i++) {
            this.dataOutput.writeDouble(buffer[i]);
        }
    }

    @Override // struct.StructOutput
    public void writeObjectArray(Object[] buffer, int len) throws IOException, IllegalAccessException, InvocationTargetException, StructException {
        if (buffer != null && len != 0) {
            if (len == -1 || len > buffer.length) {
                len = buffer.length;
            }
            for (int i = 0; i < len; i++) {
                writeObject(buffer[i]);
            }
        }
    }
}
