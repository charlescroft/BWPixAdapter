package com.easyview.basecamera;

import com.easyview.bean.AlermBean;
import com.easyview.bean.DateBean;
import com.easyview.bean.MailBean;
import com.easyview.bean.SdcardBean;
import com.easyview.bean.WifiBean;
import com.easyview.bean.WifiScanBean;

/* loaded from: classes.dex */
public interface ICamera {

    public interface IAlarmListener {
        void OnAlarm(ICamera iCamera, int i);
    }

    public interface IAlarmParamListener {
        void OnAlarmParam(ICamera iCamera, AlermBean alermBean);
    }

    public interface IDataListener {
        void OnData(ICamera iCamera, byte[] bArr, int i);
    }

    public interface IDownloadListener {
        void OnProgress(ICamera iCamera, int i, int i2);
    }

    public interface IRespondListener {
        void OnRespondResult(ICamera iCamera, int i, int i2);
    }

    public interface IWifiParamListener {
        void OnWifiParam(ICamera iCamera, WifiBean wifiBean);
    }

    public interface IWifiScanListener {
        void OnScanResult(ICamera iCamera, WifiScanBean wifiScanBean);
    }

    public interface IYUVDataListener {
        void OnData(ICamera iCamera, byte[] bArr, int i, int i2, int i3);
    }

    void RequestParams(IRespondListener iRespondListener);

    void Start();

    void StartAudio();

    void StartTalk();

    void StartVideo(IDataListener iDataListener);

    void StartVideoYUV(IYUVDataListener iYUVDataListener);

    void Stop();

    void StopAudio();

    void StopTalk();

    void StopVideo();

    void TalkAudioData(byte[] bArr, int i);

    void clearListener();

    void delEvents(int[] iArr, IRespondListener iRespondListener);

    void downRecord(int i, int i2, IDownloadListener iDownloadListener);

    void enablePairing(IRespondListener iRespondListener);

    void formatTF(IRespondListener iRespondListener);

    void getAlarmParam(IAlarmParamListener iAlarmParamListener);

    int getBrightness();

    void getCaps(IRespondListener iRespondListener);

    int getContrast();

    String getDefaultWiFiName();

    void getExtThres(IRespondListener iRespondListener);

    String getID();

    void getMailParam(MailBean mailBean, IRespondListener iRespondListener);

    int getRotate();

    int getShowOSD();

    void getStoreParam(SdcardBean sdcardBean, IRespondListener iRespondListener);

    void getTimeParam(DateBean dateBean, IRespondListener iRespondListener);

    String getWiFiName();

    void getWifiParam(WifiBean wifiBean, IRespondListener iRespondListener);

    boolean isOnline();

    void playMusic(int i, IRespondListener iRespondListener);

    void ptzControl(int i);

    void queryDeviceInfo(IRespondListener iRespondListener);

    void queryEvent(int i, String str, IRespondListener iRespondListener);

    void queryEventInfo(IRespondListener iRespondListener);

    void queryEventList(int i, int i2, IRespondListener iRespondListener);

    void queryLightValue(IRespondListener iRespondListener);

    void queryWifiResult(IRespondListener iRespondListener);

    void querystorageState(IRespondListener iRespondListener);

    void recordPlay(String str);

    void recordStop();

    void searchEvents(int i, int i2, IRespondListener iRespondListener);

    void searchRecordList(long j, long j2, IRespondListener iRespondListener);

    void searchRecords(int i, int i2, int i3, IRespondListener iRespondListener);

    void setAlarmParam(AlermBean alermBean, IRespondListener iRespondListener);

    void setBrightness(int i, IRespondListener iRespondListener);

    void setContrast(int i, IRespondListener iRespondListener);

    void setCustomListener(IRespondListener iRespondListener);

    void setExtThres(IRespondListener iRespondListener);

    void setLanguage(int i, IRespondListener iRespondListener);

    void setLightValue(int i, int i2, int i3, IRespondListener iRespondListener);

    void setMailParam(MailBean mailBean, IRespondListener iRespondListener);

    void setPassword(String str, String str2, IRespondListener iRespondListener);

    void setResolution(int i);

    void setRotate(int i, IRespondListener iRespondListener);

    void setRssi(int i);

    void setSensor(int i, IRespondListener iRespondListener);

    void setShowOSD(int i, IRespondListener iRespondListener);

    void setStoreParam(SdcardBean sdcardBean, IRespondListener iRespondListener);

    void setTimeParam(DateBean dateBean, IRespondListener iRespondListener);

    void setVideoQuality(int i);

    void setWiFiName(String str, IRespondListener iRespondListener);

    void setWifiParam(WifiBean wifiBean, IRespondListener iRespondListener);

    void snapShot(IRespondListener iRespondListener);

    void startRecord(IRespondListener iRespondListener);

    void stopDownRecord(IDownloadListener iDownloadListener);

    void stopMusic(int i, IRespondListener iRespondListener);

    void stopRecord(IRespondListener iRespondListener);

    void timing();

    void upgradeCheck(IRespondListener iRespondListener);

    void upgradeDevice(IRespondListener iRespondListener);

    void wifiScan(IWifiScanListener iWifiScanListener);
}
