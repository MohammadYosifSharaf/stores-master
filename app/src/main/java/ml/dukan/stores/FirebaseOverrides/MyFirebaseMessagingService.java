package ml.dukan.stores.FirebaseOverrides;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.TaskStackBuilder;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import ml.dukan.stores.MainActivity;
import ml.dukan.stores.R;

/**
 * Created by khaled on 12/06/17.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private final static String TAG = "FirebaseNotification";
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);


        String type = remoteMessage.getData().get("type");

        String title = "NaN";
        String body = "NaN";
        String customer_name = remoteMessage.getData().get("customer");

        switch (type){
            case "issue":
                String issue = remoteMessage.getData().get("issue");
                switch (issue) {
                    case "ping":
                        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        v.vibrate(new long[]{0, 500, 150, 500, 150, 1000}, -1);
                        title = getString(R.string.issue_ping_title);
                        body = getString(R.string.issue_ping_body, customer_name);
                        break;
                    case "not_delivered":
                        title = getString(R.string.issue_no_delivery_title);
                        body = getString(R.string.issue_no_delivery_body, customer_name);
                        break;
                    case "order":
                        Vibrator v1 = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        v1.vibrate(new long[]{0, 500, 150, 500, 150, 1000}, -1);


                            title = getString(R.string.new_order_notification_title);
                        body = getString(R.string.new_order_notification_body, customer_name);
                        break;
                }
                break;
            case "new_order":
                title = getString(R.string.new_order_notification_title);
                body = getString(R.string.new_order_notification_body, customer_name);
                break;
            case "debt":
                String status = remoteMessage.getData().get("status");
                switch (status){
                    case "DEBT_ACC_ISSUED":
                        title = getString(R.string.app_name);
                        body = getString(R.string.request_debt_account_title);
                        break;
                }
                break;

        }



        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null){
            boolean isScreenOn = pm.isInteractive();
            if(!isScreenOn){
                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |PowerManager.ACQUIRE_CAUSES_WAKEUP |PowerManager.ON_AFTER_RELEASE,"MyLock");
                wl.acquire(10000);
                PowerManager.WakeLock wl_cpu = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MyCpuLock");
                wl_cpu.acquire(10000);
            }
        }



        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        //Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Uri soundUri = Uri.parse("android.resource://"+getPackageName()+"/raw/notification_sound");
        Notification.Builder nb = new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.logo_small_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSound(soundUri)
                .setLargeIcon(largeIcon);

        showNotification(nb);
    }


    private void showNotification (Notification.Builder nb){
        Intent resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        nb.setContentIntent(resultPendingIntent);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(0, nb.build());
    }
}
