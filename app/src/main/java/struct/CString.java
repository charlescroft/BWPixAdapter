package struct;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;

@StructClass
/* loaded from: classes.dex */
public class CString implements Serializable {
    private static final long serialVersionUID = -3393948411351663341L;

    @StructField(order = 0)
    private byte[] buffer;

    public CString(int len) {
        this.buffer = null;
        this.buffer = new byte[len];
    }

    public CString(String str, int len) {
        this.buffer = null;
        this.buffer = new byte[len];
        copyData(str.getBytes(), len);
    }

    public CString(byte[] data, int len) {
        this.buffer = null;
        this.buffer = new byte[len];
        copyData(data, len);
    }

    public CString(String str, char fillChar, int len) {
        this.buffer = null;
        str = str == null ? "" : str;
        this.buffer = new byte[len];
        for (int i = 0; i < this.buffer.length; i++) {
            this.buffer[i] = (byte) fillChar;
        }
        copyData(str.getBytes(), len);
    }

    public CString(byte[] data, char fillChar, int len) {
        this.buffer = null;
        this.buffer = new byte[len];
        for (int i = 0; i < this.buffer.length; i++) {
            this.buffer[i] = (byte) fillChar;
        }
        copyData(data, len);
    }

    private void copyData(byte[] data, int len) {
        if (data.length < len) {
            System.arraycopy(data, 0, this.buffer, 0, data.length);
        } else {
            System.arraycopy(data, 0, this.buffer, 0, len);
        }
    }

    public boolean equals(Object obj) {
        CString str = (CString) obj;
        return str.toString().equals(toString());
    }

    public void setString(String str) {
        System.arraycopy(str.getBytes(), 0, this.buffer, 0, str.getBytes().length);
        this.buffer[str.getBytes().length] = 0;
    }

    public void setString(String str, String charsetName) {
        try {
            byte[] data = str.getBytes(charsetName);
            System.arraycopy(data, 0, this.buffer, 0, data.length);
            this.buffer[data.length] = 0;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public String toString() {
        return new String(this.buffer).trim();
    }

    public String asCString() {
        int i = 0;
        while (i < this.buffer.length && this.buffer[i] != 0) {
            i++;
        }
        String str = new String(this.buffer, 0, i);
        return str;
    }

    public String asCString(String charsetName) {
        int i = 0;
        while (i < this.buffer.length && this.buffer[i] != 0) {
            i++;
        }
        String str = new String(this.buffer, 0, i);
        try {
            String str2 = new String(this.buffer, 0, i, charsetName);
            return str2;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return str;
        }
    }

    public byte[] getBuffer() {
        return this.buffer;
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }
}
