package org.fitchfamily.android.gsmlocation;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.SystemService;
import org.fitchfamily.android.gsmlocation.ui.MainActivity;
import org.fitchfamily.android.gsmlocation.ui.MainActivity_;
import org.microg.nlp.api.LocationBackendService;

import java.util.List;

import static org.fitchfamily.android.gsmlocation.LogUtils.makeLogTag;

@EService
public class GsmService extends LocationBackendService {
    private static final String TAG = makeLogTag("service");
    private static final boolean DEBUG = Config.DEBUG;

    private TelephonyManager tm;
    private TelephonyHelper th;

    protected Thread worker = null;
    private Thread thread;

    private Context ctx = null;

    private static final int NOTIFICATION = 42;
    private boolean permissionNotificationShown = false;

    @SystemService
    protected NotificationManager notificationManager;

    public synchronized void start() {

        if (DEBUG)
            Log.i(TAG, "Starting location backend");

        ctx = getApplicationContext();

        if (hasLocationAccess()) {
            setShowPermissionNotification(false);
            setServiceRunning(true);
        } else {
            setShowPermissionNotification(true);
            setServiceRunning(false);
        }
    }

     @Override
     protected synchronized Location update() {
         scanGsm("update");
         return null;
     }

    @Override
    protected synchronized void onOpen() {
        if (DEBUG) Log.i(TAG, "Binder OPEN called");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Context ctx = getApplicationContext();

            if (ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(ctx, ReqLocationPermActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, intent, 0);

                Notification notification = new Notification.Builder(ctx)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.notification_location_permission))
                        .setSmallIcon(R.drawable.icon)
                        .setContentIntent(pendingIntent)
                        .build();

                ((NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE))
                        .notify(ReqLocationPermActivity.NOTIFICATION_ID, notification);
                return;
            }
        }

        start();
    }

    protected synchronized void onClose() {
        if (DEBUG) Log.i(TAG, "Binder CLOSE called");
        super.onClose();
        setServiceRunning(false);
    }

    private boolean hasLocationAccess() {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void setServiceRunning(boolean st) {
        final boolean cur_st = (worker != null);

        if (cur_st  == st)
            return;

        if (st) {
            tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            th = new TelephonyHelper(ctx);

            try {
                if (worker != null && worker.isAlive()) worker.interrupt();

                worker = new Thread() {

                    public void run() {
                        if (DEBUG) Log.i(TAG, "Starting reporter thread");
                        Looper.prepare();

                        final PhoneStateListener listener = new PhoneStateListener() {
                            public void onServiceStateChanged(ServiceState serviceState) {
                                scanGsm("onServiceStateChanged: ");
                            }

                            public void onCellLocationChanged(CellLocation location) {
                                scanGsm("onCellLocationChanged: ");
                            }

                            public void onCellInfoChanged(List<android.telephony.CellInfo> cellInfo) {
                                scanGsm("onCellInfoChanged: ");
                            }
                        };
                        tm.listen(
                                listener,
                                PhoneStateListener.LISTEN_CELL_INFO |
                                        PhoneStateListener.LISTEN_CELL_LOCATION |
                                        PhoneStateListener.LISTEN_SERVICE_STATE
                        );
                        Looper.loop();
                    }
                };
                worker.start();
            } catch (Exception e) {
                if (DEBUG) Log.e(TAG, "Start failed: " + e.getMessage());
                e.printStackTrace();
                worker = null;
            }
        } else {
            try {
                if (worker != null && worker.isAlive())
                    worker.interrupt();

                if (worker != null)
                    worker = null;

            } finally {
                worker = null;
            }
        }
    }

    public synchronized void scanGsm(final String calledFrom) {
        if (thread != null) {
            if (DEBUG)
                Log.i(TAG, "scanGsm() : Thread busy?!");
            return;
        }

        if (th != null) {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Location rslt = th.getLocationEstimate();
                    String logString;

                    if (rslt != null) {
                        rslt.setTime(System.currentTimeMillis());
                        logString = "scanGsm(" + calledFrom + ") " + rslt.toString();
                    } else
                        logString = "scanGsm(" + calledFrom + ")  null position";

                    if (DEBUG)
                        Log.i(TAG, logString);

                    report(rslt);

                    thread = null;
                }
            });
            thread.start();
        } else if (DEBUG)
            Log.i(TAG, "Telephony helper is null?!?");
    }

    private void setShowPermissionNotification(boolean visible) {
        if(visible != permissionNotificationShown) {
            if(visible) {
                if(DEBUG) {
                    Log.i(TAG, "setShowPermissionNotification(true)");
                }

                notificationManager.notify(
                        NOTIFICATION,
                        new NotificationCompat.Builder(this)
                                .setWhen(0)
                                .setShowWhen(false)
                                .setAutoCancel(false)
                                .setOngoing(true)
                                .setContentIntent(
                                        PendingIntent.getActivity(
                                                this,
                                                0,
                                                MainActivity_.intent(this).action(MainActivity.Action.request_permission).get(),
                                                PendingIntent.FLAG_UPDATE_CURRENT
                                        )
                                )
                                .setContentTitle(getString(R.string.app_name))
                                .setContentText(getString(R.string.preference_grant_permission))
                                .setSmallIcon(R.drawable.ic_stat_no_location)
                                .build()
                );

            } else {
                if(DEBUG) {
                    Log.i(TAG, "setShowPermissionNotification(false)");
                }

                notificationManager.cancel(NOTIFICATION);
            }

            permissionNotificationShown = visible;
        }
    }

}
