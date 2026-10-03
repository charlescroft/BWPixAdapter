package com.easyview.common;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import java.util.List;

/* loaded from: classes.dex */
public class WifiUtils {
    private WifiConfiguration _config;
    private Context _context;
    private List<WifiConfiguration> _wifiConfigurations;
    private WifiInfo _wifiInfo;
    private List<ScanResult> _wifiList;
    private WifiManager.WifiLock _wifiLock;
    private WifiManager _wifiManager;

    public WifiUtils(Context context) {
        this._wifiManager = (WifiManager) context.getSystemService("wifi");
        this._wifiInfo = this._wifiManager.getConnectionInfo();
        this._context = context;
    }

    public int getRssi() {
        int rssi = ((WifiManager) this._context.getSystemService("wifi")).getConnectionInfo().getRssi();
        return rssi;
    }

    public boolean WifiEnabled() {
        if (this._wifiManager.isWifiEnabled()) {
            return true;
        }
        this._wifiManager.setWifiEnabled(true);
        return false;
    }

    public List<ScanResult> Scan() {
        if (!this._wifiManager.isWifiEnabled()) {
            this._wifiManager.setWifiEnabled(true);
        }
        this._wifiManager.startScan();
        this._wifiList = this._wifiManager.getScanResults();
        this._wifiConfigurations = this._wifiManager.getConfiguredNetworks();
        if (this._wifiConfigurations != null) {
            for (WifiConfiguration config : this._wifiConfigurations) {
                if (config.SSID.equals(this._wifiInfo.getSSID())) {
                    this._config = config;
                }
            }
        }
        return this._wifiList;
    }

    public void Connect(int index) {
        if (index <= this._wifiConfigurations.size()) {
            this._wifiManager.enableNetwork(this._wifiConfigurations.get(index).networkId, true);
        }
    }

    private void connect(String ssid) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + ssid + "\"";
        config.allowedKeyManagement.set(0);
        this._wifiManager.enableNetwork(this._wifiManager.addNetwork(config), true);
    }

    private String trimSSID(String ssid) {
        if (!TextUtils.isEmpty(ssid) && ssid.startsWith("\"") && ssid.endsWith("\"")) {
            return ssid.substring(1, ssid.length() - 1);
        }
        return ssid;
    }

    public boolean Connect(String SSID) {
        if (!this._wifiManager.isWifiEnabled()) {
            this._wifiManager.setWifiEnabled(true);
        }
        String ssid = trimSSID(this._wifiManager.getConnectionInfo().getSSID());
        if (TextUtils.isEmpty(ssid) || !ssid.equals(SSID)) {
            connect(SSID);
        }
        int count = 0;
        do {
            if (TextUtils.isEmpty(ssid) || !ssid.equals(SSID)) {
                ssid = trimSSID(this._wifiManager.getConnectionInfo().getSSID());
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                count++;
            } else {
                try {
                    Thread.sleep(1000L);
                    return true;
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                    return true;
                }
            }
        } while (count <= 20);
        return false;
    }

    public boolean Connect() {
        if (!this._wifiManager.isWifiEnabled()) {
            this._wifiManager.setWifiEnabled(true);
        }
        String ssid = this._wifiManager.getConnectionInfo().getSSID();
        String SSID = trimSSID(this._wifiInfo.getSSID());
        String ssid2 = trimSSID(ssid);
        if (TextUtils.isEmpty(ssid2) || !ssid2.equals(SSID)) {
            this._wifiManager.enableNetwork(this._wifiInfo.getNetworkId(), true);
        }
        int count = 0;
        do {
            if (TextUtils.isEmpty(ssid2) || !ssid2.equals(SSID)) {
                String ssid3 = this._wifiManager.getConnectionInfo().getSSID();
                ssid2 = trimSSID(ssid3);
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                count++;
            } else {
                if (count <= 0) {
                    return true;
                }
                try {
                    Thread.sleep(1000L);
                    return true;
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                    return true;
                }
            }
        } while (count <= 20);
        return false;
    }

    public int getKeyType() {
        if (this._config == null || this._config.allowedKeyManagement.get(1)) {
            return 3;
        }
        if (this._config.allowedKeyManagement.get(2)) {
            return 2;
        }
        return this._config.allowedKeyManagement.get(0) ? 1 : 3;
    }

    public String getSSID() {
        return trimSSID(this._wifiInfo.getSSID());
    }

    public String getCurrentSSID() {
        return trimSSID(this._wifiManager.getConnectionInfo().getSSID());
    }

    public static boolean checkNetworkConnection(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService("connectivity");
        NetworkInfo wifi = connMgr.getNetworkInfo(1);
        NetworkInfo mobile = connMgr.getNetworkInfo(0);
        return wifi.isAvailable() || mobile.isAvailable();
    }

    public static boolean checkWifiConnection(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService("connectivity");
        NetworkInfo wifi = connMgr.getNetworkInfo(1);
        return wifi.isAvailable();
    }
}
