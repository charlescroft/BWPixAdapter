package object.p2pipcam.bean;

import java.io.Serializable;

/* loaded from: classes.dex */
public class LocalVideoPictureBean implements Serializable {
    private static final long serialVersionUID = 1;
    private String createTime;
    private String did;
    private String filePath;
    private String type;

    public String getFilePath() {
        return this.filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDid() {
        return this.did;
    }

    public void setDid(String did) {
        this.did = did;
    }

    public String toString() {
        return "LocalVideoPictureBean [filePath=" + this.filePath + ", createTime=" + this.createTime + ", type=" + this.type + ", did=" + this.did + "]";
    }
}
