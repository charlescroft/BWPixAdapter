package object.p2pipcam.bean;

import java.io.Serializable;
import java.util.Date;

/* loaded from: classes.dex */
public class MovieInfo implements Serializable {
    private static final long serialVersionUID = 1;
    public String camerName;
    public Date date;
    public String displayName;
    public String path;
    public String size;
    public String videoName;

    public String getVideoName() {
        return this.videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public String getCamerName() {
        return this.camerName;
    }

    public void setCamerName(String camerName) {
        this.camerName = camerName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSize() {
        return this.size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
