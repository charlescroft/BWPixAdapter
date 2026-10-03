package struct;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteOrder;

/* loaded from: classes.dex */
public class StructUnpacker extends StructInput {
    DataInput dataInput;

    protected void init(InputStream inStream, ByteOrder order) {
        if (order == ByteOrder.LITTLE_ENDIAN) {
            this.dataInput = new LEDataInputStream(inStream);
        } else {
            this.dataInput = new DataInputStream(inStream);
        }
    }

    public StructUnpacker(byte[] bufferToUnpack) {
        this(new ByteArrayInputStream(bufferToUnpack), ByteOrder.BIG_ENDIAN);
    }

    public StructUnpacker(byte[] bufferToUnpack, ByteOrder order) {
        this(new ByteArrayInputStream(bufferToUnpack), order);
    }

    public StructUnpacker(InputStream is, ByteOrder order) {
        init(is, order);
    }

    public void unpack(Object objectToUnpack) throws StructException {
        readObject(objectToUnpack);
    }

    @Override // struct.StructInput
    public void readObject(Object obj) throws StructException {
        if (obj == null) {
            throw new StructException("Struct objects cannot be null.");
        }
        StructData info = StructUtils.getStructInfo(obj);
        Field[] fields = info.getFields();
        for (Field currentField : fields) {
            StructFieldData fieldData = info.getFieldData(currentField.getName());
            if (fieldData == null) {
                throw new StructException("Field Data not found for field: " + currentField.getName());
            }
            int arrayLength = -1;
            boolean lengthedArray = false;
            try {
                if (info.isLenghtedArray(currentField)) {
                    Field f = info.getLenghtedArray(currentField.getName());
                    StructFieldData lengthMarker = info.getFieldData(f.getName());
                    if (lengthMarker.requiresGetterSetter()) {
                        arrayLength = ((Number) lengthMarker.getGetter().invoke(obj, null)).intValue();
                    } else {
                        arrayLength = ((Number) lengthMarker.getField().get(obj)).intValue();
                    }
                    lengthedArray = true;
                }
                if (fieldData.requiresGetterSetter()) {
                    Method getter = fieldData.getGetter();
                    Method setter = fieldData.getSetter();
                    if (getter == null || setter == null) {
                        throw new StructException(" getter/setter required for : " + currentField.getName());
                    }
                    if (lengthedArray && arrayLength >= 0) {
                        Object ret = Array.newInstance(currentField.getType().getComponentType(), arrayLength);
                        setter.invoke(obj, ret);
                        if (!currentField.getType().getComponentType().isPrimitive()) {
                            Object[] array = (Object[]) ret;
                            for (int j = 0; j < arrayLength; j++) {
                                array[j] = currentField.getType().getComponentType().newInstance();
                            }
                        }
                    }
                    if (!lengthedArray && currentField.getType().isArray() && getter.invoke(obj, null) == null) {
                        throw new StructException("Arrays can not be null :" + currentField.getName());
                    }
                    readField(fieldData, getter, setter, obj);
                } else {
                    if (lengthedArray && arrayLength >= 0) {
                        Object ret2 = Array.newInstance(currentField.getType().getComponentType(), arrayLength);
                        currentField.set(obj, ret2);
                        if (!currentField.getType().getComponentType().isPrimitive()) {
                            Object[] array2 = (Object[]) ret2;
                            for (int j2 = 0; j2 < arrayLength; j2++) {
                                array2[j2] = currentField.getType().getComponentType().newInstance();
                            }
                        }
                    }
                    if (!lengthedArray && currentField.getType().isArray() && currentField.get(obj) == null) {
                        throw new StructException("Arrays can not be null. : " + currentField.getName());
                    }
                    if (!lengthedArray || (lengthedArray && arrayLength >= 0)) {
                        readField(fieldData, null, null, obj);
                    }
                }
            } catch (Exception e) {
                throw new StructException(e);
            }
        }
    }

    @Override // struct.StructInput
    public boolean readBoolean() throws IOException {
        return this.dataInput.readBoolean();
    }

    @Override // struct.StructInput
    public byte readByte() throws IOException {
        return this.dataInput.readByte();
    }

    @Override // struct.StructInput
    public short readShort() throws IOException {
        return this.dataInput.readShort();
    }

    @Override // struct.StructInput
    public int readInt() throws IOException {
        return this.dataInput.readInt();
    }

    @Override // struct.StructInput
    public long readLong() throws IOException {
        return this.dataInput.readLong();
    }

    @Override // struct.StructInput
    protected char readChar() throws IOException {
        return this.dataInput.readChar();
    }

    @Override // struct.StructInput
    protected float readFloat() throws IOException {
        return this.dataInput.readFloat();
    }

    @Override // struct.StructInput
    protected double readDouble() throws IOException {
        return this.dataInput.readDouble();
    }

    @Override // struct.StructInput
    protected void readBooleanArray(boolean[] buffer) throws IOException {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = readBoolean();
        }
    }

    @Override // struct.StructInput
    protected void readByteArray(byte[] buffer) throws IOException {
        this.dataInput.readFully(buffer);
    }

    @Override // struct.StructInput
    protected void readCharArray(char[] buffer) throws IOException {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = readChar();
        }
    }

    @Override // struct.StructInput
    protected void readShortArray(short[] buffer) throws IOException {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = readShort();
        }
    }

    @Override // struct.StructInput
    protected void readIntArray(int[] buffer) throws IOException {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = readInt();
        }
    }

    @Override // struct.StructInput
    protected void readLongArray(long[] buffer) throws IOException {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = readLong();
        }
    }

    @Override // struct.StructInput
    protected void readFloatArray(float[] buffer) throws IOException {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = readFloat();
        }
    }

    @Override // struct.StructInput
    protected void readDoubleArray(double[] buffer) throws IOException {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = readDouble();
        }
    }

    @Override // struct.StructInput
    protected void readObjectArray(Object[] objects) throws IOException, StructException {
        for (Object obj : objects) {
            readObject(obj);
        }
    }
}
