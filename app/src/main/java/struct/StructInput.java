package struct;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import struct.Constants;

/* loaded from: classes.dex */
public abstract class StructInput extends InputStream {
    private static /* synthetic */ int[] $SWITCH_TABLE$struct$Constants$Primitive;

    protected abstract boolean readBoolean() throws IOException;

    protected abstract void readBooleanArray(boolean[] zArr) throws IOException;

    protected abstract byte readByte() throws IOException;

    protected abstract void readByteArray(byte[] bArr) throws IOException;

    protected abstract char readChar() throws IOException;

    protected abstract void readCharArray(char[] cArr) throws IOException;

    protected abstract double readDouble() throws IOException;

    protected abstract void readDoubleArray(double[] dArr) throws IOException;

    protected abstract float readFloat() throws IOException;

    protected abstract void readFloatArray(float[] fArr) throws IOException;

    protected abstract int readInt() throws IOException;

    protected abstract void readIntArray(int[] iArr) throws IOException;

    protected abstract long readLong() throws IOException;

    protected abstract void readLongArray(long[] jArr) throws IOException;

    protected abstract void readObjectArray(Object[] objArr) throws IOException, StructException;

    protected abstract short readShort() throws IOException;

    protected abstract void readShortArray(short[] sArr) throws IOException;

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

    public void readField(StructFieldData fieldData, Method getter, Method setter, Object obj) throws IOException, InvocationTargetException, InstantiationException, IllegalAccessException, StructException {
        Field field = fieldData.getField();
        if (!field.getType().isArray()) {
            switch ($SWITCH_TABLE$struct$Constants$Primitive()[fieldData.getType().ordinal()]) {
                case 1:
                    if (setter == null) {
                        field.setBoolean(obj, readBoolean());
                        return;
                    } else {
                        setter.invoke(obj, Boolean.valueOf(readBoolean()));
                        return;
                    }
                case 2:
                    if (setter == null) {
                        field.setByte(obj, readByte());
                        return;
                    } else {
                        setter.invoke(obj, Byte.valueOf(readByte()));
                        return;
                    }
                case 3:
                    if (setter == null) {
                        field.setChar(obj, readChar());
                        return;
                    } else {
                        setter.invoke(obj, Character.valueOf(readChar()));
                        return;
                    }
                case 4:
                    if (setter == null) {
                        field.setShort(obj, readShort());
                        return;
                    } else {
                        setter.invoke(obj, Short.valueOf(readShort()));
                        return;
                    }
                case 5:
                    if (setter == null) {
                        field.setInt(obj, readInt());
                        return;
                    } else {
                        setter.invoke(obj, Integer.valueOf(readInt()));
                        return;
                    }
                case 6:
                    if (setter == null) {
                        field.setLong(obj, readLong());
                        return;
                    } else {
                        setter.invoke(obj, Long.valueOf(readLong()));
                        return;
                    }
                case 7:
                    if (setter == null) {
                        field.setFloat(obj, readFloat());
                        return;
                    } else {
                        setter.invoke(obj, Float.valueOf(readFloat()));
                        return;
                    }
                case 8:
                    if (setter == null) {
                        field.setDouble(obj, readDouble());
                        return;
                    } else {
                        setter.invoke(obj, Double.valueOf(readDouble()));
                        return;
                    }
                default:
                    if (setter != null) {
                        Object object2 = getter.invoke(obj, null);
                        if (object2 == null) {
                            if (field.getName().endsWith("CString")) {
                                throw new StructException("CString objects should be initialized :" + field.getName());
                            }
                            object2 = field.getType().newInstance();
                        }
                        readObject(object2);
                        setter.invoke(obj, object2);
                        return;
                    }
                    handleObject(field, obj);
                    return;
            }
        }
        if (getter != null && getter.invoke(obj, null) == null) {
            throw new StructException("Arrays can not be null : " + field.getName());
        }
        switch ($SWITCH_TABLE$struct$Constants$Primitive()[fieldData.getType().ordinal()]) {
            case 1:
                if (getter == null) {
                    readBooleanArray((boolean[]) field.get(obj));
                    return;
                } else {
                    readBooleanArray((boolean[]) getter.invoke(obj, null));
                    return;
                }
            case 2:
                if (getter == null) {
                    readByteArray((byte[]) field.get(obj));
                    return;
                } else {
                    readByteArray((byte[]) getter.invoke(obj, null));
                    return;
                }
            case 3:
                if (getter == null) {
                    readCharArray((char[]) field.get(obj));
                    return;
                } else {
                    readCharArray((char[]) getter.invoke(obj, null));
                    return;
                }
            case 4:
                if (getter == null) {
                    readShortArray((short[]) field.get(obj));
                    return;
                } else {
                    readShortArray((short[]) getter.invoke(obj, null));
                    return;
                }
            case 5:
                if (getter == null) {
                    readIntArray((int[]) field.get(obj));
                    return;
                } else {
                    readIntArray((int[]) getter.invoke(obj, null));
                    return;
                }
            case 6:
                if (getter == null) {
                    readLongArray((long[]) field.get(obj));
                    return;
                } else {
                    readLongArray((long[]) getter.invoke(obj, null));
                    return;
                }
            case 7:
                if (getter == null) {
                    readFloatArray((float[]) field.get(obj));
                    return;
                } else {
                    readFloatArray((float[]) getter.invoke(obj, null));
                    return;
                }
            case 8:
                if (getter == null) {
                    readDoubleArray((double[]) field.get(obj));
                    return;
                } else {
                    readDoubleArray((double[]) getter.invoke(obj, null));
                    return;
                }
            default:
                if (getter == null) {
                    readObjectArray((Object[]) field.get(obj));
                    return;
                } else {
                    readObjectArray((Object[]) getter.invoke(obj, null));
                    return;
                }
        }
    }

    public void handleObject(Field field, Object obj) throws IllegalArgumentException, StructException, IOException, InstantiationException, IllegalAccessException {
        if (field.get(obj) == null) {
            if (field.getType().getName().endsWith("CString")) {
                throw new StructException("CString objects should be initialized before unpacking :" + field.getName());
            }
            field.set(obj, field.getType().newInstance());
        }
        readObject(field.get(obj));
    }

    @Override // java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
    public void close() throws IOException {
    }

    @Override // java.io.InputStream
    public int read() throws IOException {
        return -1;
    }
}
