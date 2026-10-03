package object.p2pipcam.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/* loaded from: classes.dex */
public class HttpUtil {
    private static final String STR_TAG = "HttpUtil";
    private HttpResult httpRt;
    private HttpURLConnection url_con;

    public interface HttpResult {
        void httpResult(String str, int i);
    }

    HttpUtil(HttpResult httpResult) {
        this.httpRt = null;
        this.httpRt = httpResult;
    }

    public void send_get_request(String urlStr, int operation, boolean result) {
        try {
            try {
                String temp = "";
                URL url = new URL(urlStr);
                this.url_con = (HttpURLConnection) url.openConnection();
                InputStream in = this.url_con.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(in));
                while (true) {
                    String inputLine = rd.readLine();
                    if (inputLine == null) {
                        break;
                    } else {
                        temp = String.valueOf(temp) + inputLine;
                    }
                }
                System.out.println(temp);
                if (result && this.httpRt != null) {
                    this.httpRt.httpResult(temp, operation);
                }
                in.close();
                if (this.url_con != null) {
                    this.url_con.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (this.url_con != null) {
                    this.url_con.disconnect();
                }
            }
        } catch (Throwable th) {
            if (this.url_con != null) {
                this.url_con.disconnect();
            }
            throw th;
        }
    }
}
