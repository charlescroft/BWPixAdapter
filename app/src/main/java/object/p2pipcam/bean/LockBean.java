package object.p2pipcam.bean;

/* loaded from: classes.dex */
public class LockBean {
    private String reserver;
    private int cmd = 0;
    private String openLockPwd = "888888";
    private String oldLockPwd = "888888";
    private String newLockPwd = "888888";
    private int openLockTime = 0;

    public int getCmd() {
        return this.cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public String getOpenLockPwd() {
        return this.openLockPwd;
    }

    public void setOpenLockPwd(String openLockPwd) {
        this.openLockPwd = openLockPwd;
    }

    public String getOldLockPwd() {
        return this.oldLockPwd;
    }

    public void setOldLockPwd(String oldLockPwd) {
        this.oldLockPwd = oldLockPwd;
    }

    public String getNewLockPwd() {
        return this.newLockPwd;
    }

    public void setNewLockPwd(String newLockPwd) {
        this.newLockPwd = newLockPwd;
    }

    public int getOpenLockTime() {
        return this.openLockTime;
    }

    public void setOpenLockTime(int openLockTime) {
        this.openLockTime = openLockTime;
    }

    public String getReserver() {
        return this.reserver;
    }

    public void setReserver(String reserver) {
        this.reserver = reserver;
    }

    public byte[] arrary() {
        byte[] ret = new byte[40];
        for (int i = 0; i < 40; i++) {
            ret[i] = 0;
        }
        byte[] data = intToByteArray_Little(this.cmd);
        System.arraycopy(data, 0, ret, 0, 4);
        if (this.openLockPwd.length() == 6) {
            byte[] data2 = this.openLockPwd.getBytes();
            System.arraycopy(data2, 0, ret, 4, 6);
        }
        if (this.oldLockPwd.length() == 6) {
            byte[] data3 = this.oldLockPwd.getBytes();
            System.arraycopy(data3, 0, ret, 12, 6);
        }
        if (this.newLockPwd.length() == 6) {
            byte[] data4 = this.newLockPwd.getBytes();
            System.arraycopy(data4, 0, ret, 20, 6);
        }
        byte[] data5 = intToByteArray_Little(this.openLockTime);
        System.arraycopy(data5, 0, ret, 28, 4);
        return ret;
    }

    public static final int byteArrayToInt_Little(byte[] byt) {
        if (byt.length == 1) {
            return byt[0] & 255;
        }
        if (byt.length == 2) {
            return (byt[0] & 255) | ((byt[1] & 255) << 8);
        }
        if (byt.length == 4) {
            return (byt[0] & 255) | ((byt[1] & 255) << 8) | ((byt[2] & 255) << 16) | ((byt[3] & 255) << 24);
        }
        return 0;
    }

    public static final byte[] intToByteArray_Little(int value) {
        return new byte[]{(byte) value, (byte) (value >>> 8), (byte) (value >>> 16), (byte) (value >>> 24)};
    }

    public String toString() {
        return "cmd == " + this.cmd + "  openLockPwd == " + this.openLockPwd + "   oldLockPwd  == " + this.oldLockPwd + "  newLockPwd ==  " + this.newLockPwd + "  openLockTime==  " + this.openLockTime;
    }
}
