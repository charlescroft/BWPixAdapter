package com.easyview.bean;

/* loaded from: classes.dex */
public class MailBean {
    private int auth;
    private boolean isChecked;
    private int port;
    private String pwd;
    private String receiver1;
    private String receiver2;
    private String receiver3;
    private String receiver4;
    private String sender;
    private int ssl;
    private String svr;
    private String user;

    public boolean isChecked() {
        return this.isChecked;
    }

    public void setChecked(boolean isChecked) {
        this.isChecked = isChecked;
    }

    public String getSvr() {
        return this.svr;
    }

    public void setSvr(String svr) {
        this.svr = svr;
    }

    public String getUser() {
        return this.user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPwd() {
        return this.pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public int getSsl() {
        return this.ssl;
    }

    public void setSsl(int ssl) {
        this.ssl = ssl;
    }

    public String getSender() {
        return this.sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver1() {
        return this.receiver1;
    }

    public void setReceiver1(String receiver1) {
        this.receiver1 = receiver1;
    }

    public String getReceiver2() {
        return this.receiver2;
    }

    public void setReceiver2(String receiver2) {
        this.receiver2 = receiver2;
    }

    public String getReceiver3() {
        return this.receiver3;
    }

    public void setReceiver3(String receiver3) {
        this.receiver3 = receiver3;
    }

    public String getReceiver4() {
        return this.receiver4;
    }

    public void setReceiver4(String receiver4) {
        this.receiver4 = receiver4;
    }

    public String toString() {
        return "MailBean [svr=" + this.svr + ", user=" + this.user + ", port=" + this.port + ", pwd=" + this.pwd + ", ssl=" + this.ssl + ", sender=" + this.sender + ", auth = " + this.auth + ", receiver1=" + this.receiver1 + ", receiver2=" + this.receiver2 + ", receiver3=" + this.receiver3 + ", receiver4=" + this.receiver4 + "]";
    }

    public int getAuth() {
        return this.auth;
    }

    public void setAuth(int auth) {
        this.auth = auth;
    }
}
