package com.easyview.basecamera;

import android.os.Handler;
import com.easyview.basecamera.ICamera;

/* loaded from: classes.dex */
public class QueryFunction {
    private int _timeout = 3000;
    private Handler _handler = new Handler();
    private IFunction _function = null;
    private ICamera.IRespondListener _respond = null;
    private Runnable _runnable = new Runnable() { // from class: com.easyview.basecamera.QueryFunction.1
        @Override // java.lang.Runnable
        public void run() {
            if (QueryFunction.this._function != null) {
                if (QueryFunction.this._function.isPause()) {
                    QueryFunction.this._handler.postDelayed(QueryFunction.this._runnable, QueryFunction.this._timeout);
                } else if (!QueryFunction.this._function.isFinish()) {
                    QueryFunction.this._function.OnStart(QueryFunction.this.respond);
                }
            }
        }
    };
    ICamera.IRespondListener respond = new ICamera.IRespondListener() { // from class: com.easyview.basecamera.QueryFunction.2
        @Override // com.easyview.basecamera.ICamera.IRespondListener
        public void OnRespondResult(ICamera camera, int cmd, int result) {
            QueryFunction.this._handler.removeCallbacks(QueryFunction.this._runnable);
            if (QueryFunction.this._respond != null) {
                QueryFunction.this._respond.OnRespondResult(camera, cmd, result);
            }
        }
    };

    public interface IFunction {
        void OnStart(ICamera.IRespondListener iRespondListener);

        boolean isFinish();

        boolean isPause();
    }

    public void Start(IFunction func, ICamera.IRespondListener listener) {
        this._function = func;
        this._respond = listener;
        this._handler.postDelayed(this._runnable, this._timeout);
    }

    public void Start(Object owner, String methodName, ICamera.IRespondListener listener, Object[] args) {
    }
}
