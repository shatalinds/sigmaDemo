package ru.sigmacall.sigma.gcm;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import ru.sigmacall.sigma.AppConf;
import ru.sigmacall.sigma.MainActivity;
import ru.sigmacall.sigma.R;
import ru.sigmacall.sigma.SigmaApp;
import ru.sigmacall.sigma.http.HttpTask;
import ru.sigmacall.sigma.http.SigmaRespond;
import ru.sigmacall.sigma.tools.RefRequest;
import ru.sigmacall.sigma.tools.ServerCallback;

public class GcmHelper {
    public static final String TAG = "GcmHelper: ";
    private final String SERVER_API_KEY = "AIzYC3hnl5wIFM";
    private final String SENDER_ID = "699861";

    private static final String PROPERTY_APP_VERSION = "sigma-app-version";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    String gcmRegId;
    GoogleCloudMessaging gcm;

    AtomicInteger msgId = new AtomicInteger();
    Context context;
    Activity activity;
    AppConf conf;

    public GcmHelper(Activity activity) {
        this.context = SigmaApp.getContext();
        this.activity = activity;
        this.conf = new AppConf(context);

        gcm = GoogleCloudMessaging.getInstance(SigmaApp.getContext());
        gcmRegId = conf.getGcmRegId();
        if (gcmRegId.isEmpty()) {
            registerGcmInBackground();
        } else {
            SigmaApp.log("Already registered: " + gcmRegId, TAG);
            addDevicePushId(gcmRegId);
        }
    }

    private void addDevicePushId(final String pushId) {
        RefRequest ref = new RefRequest(activity);
        final String login = conf.getSipUser();
        final String sessId = conf.getSessId();


        final ProgressDialog pd = MainActivity.createAndShowProgressDialog(activity);
        ref.addDevicePushId(login, sessId, pushId, new ServerCallback() {
            @Override
            public void onSuccess(String[] result) {
                if (result != null) {
                    if (result.length > 0) {
                        if (result[0] != null && result[1] != null) {
                            if (Boolean.valueOf(result[1])) {
                                //Success
                            }
                        }
                    }
                }
                if (activity != null) {
                    try {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pd.cancel();
                            }
                        });
                    } catch (NumberFormatException ex) {
                        ex.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(final String message) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.cancel();
                        if (message != null) {
                            AppConf.alertDialog(activity, activity.getString(R.string.ui_warning),
                                    message, R.drawable.ic_info, null, 0, activity.getString(R.string.ui_ok),
                                    null, null, null, null, null);
                        }
                    }
                });
            }
        });
    }


    private void registerGcmInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg;
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(SigmaApp.getContext());
                    }
                    gcmRegId = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + gcmRegId;

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addDevicePushId(gcmRegId);
                        }
                    });

                    conf.setGcmRegId(gcmRegId);
                } catch (IOException ex) {
                    msg = "Error registerGcm:" + ex.getMessage();
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                SigmaApp.log(msg, TAG);
            }
        }.execute(null, null, null);
    }
}
