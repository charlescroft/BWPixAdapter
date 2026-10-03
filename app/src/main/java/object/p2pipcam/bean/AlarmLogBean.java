package object.p2pipcam.bean;

import java.io.Serializable;

/* loaded from: classes.dex */
public class AlarmLogBean implements Serializable {
    private static final long serialVersionUID = 1;
    private String camName;
    private String content;
    private String createtime;
    private String did;

    public String getCamName() {
        return this.camName;
    }

    public void setCamName(String camName) {
        this.camName = camName;
    }

    public String getDid() {
        return this.did;
    }

    public void setDid(String did) {
        this.did = did;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatetime() {
        return this.createtime;
    }

    public void setCreatetime(String createtime) {
        this.createtime = createtime;
    }

    public String toString() {
        return "AlarmLogBean [camName=" + this.camName + ", did=" + this.did + ", content=" + this.content + ", createtime=" + this.createtime + "]";
    }
}
