package com.easyview.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/* loaded from: classes.dex */
public class CgiUtils {
    private EV_NetInfo _info;

    public void SetInfo(EV_NetInfo info) {
        this._info = info;
    }

    public String GetSnapFileName(String ip) {
        String fileName = null;
        String url = String.format("http://%s/cgi-bin/hi3510/param.cgi?cmd=snap&", ip);
        try {
            URL u = new URL(url);
            InputStream is = u.openStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String result = br.readLine();
            fileName = parsePathName(result);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        if (fileName != null && fileName.length() > 10) {
            return fileName;
        }
        return null;
    }

    public boolean ConfigWifi(String ip, String ssid, String key, int keytype) {
        String url = String.format("http://%s/cgi-bin/setwifiattr.cgi?-cmd=setwifiattr&-wktype=%d&-wepid=0&-enable=1&-ssid=%s&-key=%s&-checkname=admin&-checkpasswd=YWRtaW4%%3D&", ip, Integer.valueOf(keytype), ssid, key);
        try {
            URL u = new URL(url);
            u.openStream();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        return true;
    }

    private String parsePathName(String result) {
        int k = result.indexOf("\"");
        if (k != -1) {
            String str = result.substring(k + 1);
            int k2 = str.indexOf("\"");
            return k2 == -1 ? str : str.substring(0, k2);
        }
        return result;
    }
}
