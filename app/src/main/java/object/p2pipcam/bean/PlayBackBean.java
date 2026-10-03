package object.p2pipcam.bean;

import java.io.Serializable;

/* loaded from: classes.dex */
public class PlayBackBean implements Serializable {
    private static final long serialVersionUID = 1;
    private String did;
    private String path;

    public String getDid() {
        return this.did;
    }

    public void setDid(String did) {
        this.did = did;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
