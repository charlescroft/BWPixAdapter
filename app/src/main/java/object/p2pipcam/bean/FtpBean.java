package object.p2pipcam.bean;

/* loaded from: classes.dex */
public class FtpBean {
    private String dir;
    private int mode;
    private int port;
    private String pwd;
    private String svr_ftp;
    private int upload_interval;
    private String user;

    public String getDir() {
        return this.dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getSvr_ftp() {
        return this.svr_ftp;
    }

    public void setSvr_ftp(String svr_ftp) {
        this.svr_ftp = svr_ftp;
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

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMode() {
        return this.mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getUpload_interval() {
        return this.upload_interval;
    }

    public void setUpload_interval(int upload_interval) {
        this.upload_interval = upload_interval;
    }

    public String toString() {
        return "FtpBean [svr_ftp=" + this.svr_ftp + ", user=" + this.user + ", pwd=" + this.pwd + ", port=" + this.port + ", mode=" + this.mode + ", upload_interval=" + this.upload_interval + ", dir=" + this.dir + "]";
    }
}
