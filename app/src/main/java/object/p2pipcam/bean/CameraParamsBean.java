package object.p2pipcam.bean;

import android.graphics.Bitmap;
import java.io.Serializable;

/* loaded from: classes.dex */
public class CameraParamsBean implements Serializable {
    private static final long serialVersionUID = -1893938966606638092L;
    private int alarm;
    private boolean authority;
    private Bitmap bmp;
    private String did;
    private int mode;
    private int motion_sensitivity;
    private String name;
    private String pwd;
    private boolean selected;
    private int status;
    private int sum;
    private int sum_pic;
    private String user;

    public int getSum_pic() {
        return this.sum_pic;
    }

    public void setSum_pic(int sum_pic) {
        this.sum_pic = sum_pic;
    }

    public boolean isSelected() {
        return this.selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getMode() {
        return this.mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getSensitivity() {
        return this.motion_sensitivity;
    }

    public void setSensitivity(int s) {
        this.motion_sensitivity = s;
    }

    public int getAlarm() {
        return this.alarm;
    }

    public void setAlarm(int alarm) {
        this.alarm = alarm;
    }

    public boolean isAuthority() {
        return this.authority;
    }

    public void setAuthority(boolean authority) {
        this.authority = authority;
    }

    public Bitmap getBmp() {
        return this.bmp;
    }

    public void setBmp(Bitmap bmp) {
        this.bmp = bmp;
    }

    public int getSum() {
        return this.sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getDid() {
        return this.did;
    }

    public void setDid(String did) {
        this.did = did;
    }

    public String getUser() {
        return this.user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPwd() {
        return this.pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        return "CameraParamsBean [did=" + this.did + ", user=" + this.user + ", pwd=" + this.pwd + ", name=" + this.name + "]";
    }
}
