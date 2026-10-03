package struct;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import struct.Constants;

/* loaded from: classes.dex */
public abstract class StructOutput extends OutputStream {
    private static /* synthetic */ int[] $SWITCH_TABLE$struct$Constants$Primitive;

    public abstract void writeBoolean(boolean z) throws IOException;

    public abstract void writeBooleanArray(boolean[] zArr, int i) throws IOException;

    public abstract void writeByte(byte b) throws IOException;

    public abstract void writeByteArray(byte[] bArr, int i) throws IOException;

    public abstract void writeChar(char c) throws IOException;

    public abstract void writeCharArray(char[] cArr, int i) throws IOException;

    public abstract void writeDouble(double d) throws IOException;

    public abstract void writeDoubleArray(double[] dArr, int i) throws IOException;

    public abstract void writeFloat(float f) throws IOException;

    public abstract void writeFloatArray(float[] fArr, int i) throws IOException;

    public abstract void writeInt(int i) throws IOException;

    public abstract void writeIntArray(int[] iArr, int i) throws IOException;

    public abstract void writeLong(long j) throws IOException;

    public abstract void writeLongArray(long[] jArr, int i) throws IOException;

    public abstract void writeObjectArray(Object[] objArr, int i) throws IOException, IllegalAccessException, InvocationTargetException, StructException;

    public abstract void writeShort(short s) throws IOException;

    public abstract void writeShortArray(short[] sArr, int i) throws IOException;

    static /* synthetic */ int[] $SWITCH_TABLE$struct$Constants$Primitive() {
        int[] iArr = $SWITCH_TABLE$struct$Constants$Primitive;
        if (iArr == null) {
            iArr = new int[Constants.Primitive.valuesCustom().length];
            try {
                iArr[Constants.Primitive.BOOLEAN.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[Constants.Primitive.BYTE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[Constants.Primitive.CHAR.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[Constants.Primitive.DOUBLE.ordinal()] = 8;
            } catch (NoSuchFieldError e4) {
            }
            try {
                iArr[Constants.Primitive.FLOAT.ordinal()] = 7;
            } catch (NoSuchFieldError e5) {
            }
            try {
                iArr[Constants.Primitive.INT.ordinal()] = 5;
            } catch (NoSuchFieldError e6) {
            }
            try {
                iArr[Constants.Primitive.LONG.ordinal()] = 6;
            } catch (NoSuchFieldError e7) {
            }
            try {
                iArr[Constants.Primitive.OBJECT.ordinal()] = 9;
            } catch (NoSuchFieldError e8) {
            }
            try {
                iArr[Constants.Primitive.SHORT.ordinal()] = 4;
            } catch (NoSuchFieldError e9) {
            }
            $SWITCH_TABLE$struct$Constants$Primitive = iArr;
        }
        return iArr;
    }

    @Override // java.io.OutputStream
    public void write(int arg0) throws IOException {
    }

    public void writeObject(Object obj) throws StructException {
        if (obj == null) {
            throw new StructException("Struct classes cant be null. ");
        }
        StructData info = StructUtils.getStructInfo(obj);
        for (Field currentField : info.getFields()) {
            StructFieldData fieldData = info.getFieldData(currentField.getName());
            if (fieldData == null) {
                throw new StructException("Field Data not found for field: " + currentField.getName());
            }
            boolean lengthedArray = false;
            int arrayLength = 0;
            try {
                if (fieldData.isArrayLengthMarker()) {
                    if (fieldData.requiresGetterSetter()) {
                        arrayLength = ((Number) fieldData.getGetter().invoke(obj, null)).intValue();
                    } else {
                        arrayLength = ((Number) fieldData.getField().get(obj)).intValue();
                    }
                    lengthedArray = true;
                }
                if (fieldData.requiresGetterSetter()) {
                    if (lengthedArray && arrayLength >= 0) {
                        writeField(fieldData, fieldData.getGetter(), obj, arrayLength);
                    } else {
                        writeField(fieldData, fieldData.getGetter(), obj, -1);
                    }
                } else if (lengthedArray && arrayLength >= 0) {
                    writeField(fieldData, null, obj, arrayLength);
                } else {
                    writeField(fieldData, null, obj, -1);
                }
            } catch (Exception e) {
                throw new StructException(e);
            }
        }
    }

    public void writeField(StructFieldData fieldData, Method getter, Object obj, int len) throws IllegalAccessException, IOException, InvocationTargetException, StructException {
        Field field = fieldData.getField();
        if (!field.getType().isArray()) {
            switch ($SWITCH_TABLE$struct$Constants$Primitive()[fieldData.getType().ordinal()]) {
                case 1:
                    if (getter == null) {
                        writeBoolean(field.getBoolean(obj));
                        break;
                    } else {
                        writeBoolean(((Boolean) getter.invoke(obj, null)).booleanValue());
                        break;
                    }
                case 2:
                    if (getter == null) {
                        writeByte(field.getByte(obj));
                        break;
                    } else {
                        writeByte(((Byte) getter.invoke(obj, null)).byteValue());
                        break;
                    }
                case 3:
                    if (getter == null) {
                        writeChar(field.getChar(obj));
                        break;
                    } else {
                        writeChar(((Character) getter.invoke(obj, null)).charValue());
                        break;
                    }
                case 4:
                    if (getter == null) {
                        writeShort(field.getShort(obj));
                        break;
                    } else {
                        writeShort(((Short) getter.invoke(obj, null)).shortValue());
                        break;
                    }
                case 5:
                    if (getter == null) {
                        writeInt(field.getInt(obj));
                        break;
                    } else {
                        writeInt(((Integer) getter.invoke(obj, null)).intValue());
                        break;
                    }
                case 6:
                    long longValue = getter != null ? ((Long) getter.invoke(obj, null)).longValue() : field.getLong(obj);
                    writeLong(longValue);
                    break;
                case 7:
                    if (getter == null) {
                        writeFloat(field.getFloat(obj));
                        break;
                    } else {
                        writeFloat(((Float) getter.invoke(obj, null)).floatValue());
                        break;
                    }
                case 8:
                    if (getter == null) {
                        writeDouble(field.getDouble(obj));
                        break;
                    } else {
                        writeDouble(((Double) getter.invoke(obj, null)).doubleValue());
                        break;
                    }
                default:
                    if (getter == null) {
                        handleObject(field, obj);
                        break;
                    } else {
                        handleObject(field, getter.invoke(obj, null));
                        break;
                    }
            }
            return;
        }
        switch ($SWITCH_TABLE$struct$Constants$Primitive()[fieldData.getType().ordinal()]) {
            case 1:
                if (getter == null) {
                    writeBooleanArray((boolean[]) field.get(obj), len);
                    break;
                } else {
                    writeBooleanArray((boolean[]) getter.invoke(obj, null), len);
                    break;
                }
            case 2:
                if (getter == null) {
                    writeByteArray((byte[]) field.get(obj), len);
                    break;
                } else {
                    writeByteArray((byte[]) getter.invoke(obj, null), len);
                    break;
                }
            case 3:
                if (getter == null) {
                    writeCharArray((char[]) field.get(obj), len);
                    break;
                } else {
                    writeCharArray((char[]) getter.invoke(obj, null), len);
                    break;
                }
            case 4:
                if (getter == null) {
                    writeShortArray((short[]) field.get(obj), len);
                    break;
                } else {
                    writeShortArray((short[]) getter.invoke(obj, null), len);
                    break;
                }
            case 5:
                if (getter == null) {
                    writeIntArray((int[]) field.get(obj), len);
                    break;
                } else {
                    writeIntArray((int[]) getter.invoke(obj, null), len);
                    break;
                }
            case 6:
                if (getter == null) {
                    writeLongArray((long[]) field.get(obj), len);
                    break;
                } else {
                    writeLongArray((long[]) getter.invoke(obj, null), len);
                    break;
                }
            case 7:
                if (getter == null) {
                    writeFloatArray((float[]) field.get(obj), len);
                    break;
                } else {
                    writeFloatArray((float[]) getter.invoke(obj, null), len);
                    break;
                }
            case 8:
                if (getter == null) {
                    writeDoubleArray((double[]) field.get(obj), len);
                    break;
                } else {
                    writeDoubleArray((double[]) getter.invoke(obj, null), len);
                    break;
                }
            default:
                if (getter == null) {
                    writeObjectArray((Object[]) field.get(obj), len);
                    break;
                } else {
                    writeObjectArray((Object[]) getter.invoke(obj, null), len);
                    break;
                }
        }
    }

    public void handleObject(Field field, Object obj) throws IllegalArgumentException, StructException, IllegalAccessException, IOException {
        writeObject(field.get(obj));
    }
}
