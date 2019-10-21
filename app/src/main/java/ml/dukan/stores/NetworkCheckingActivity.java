package ml.dukan.stores;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import ml.dukan.stores.HelperClasses.Util;
import ml.dukan.stores.SweetAlert.SweetAlertDialog;

/**
 * Created by Khaled on 07/09/17.
 */

public class NetworkCheckingActivity extends AppCompatActivity {
    Snackbar network_snackbar;
    private void networkSnackbar (boolean online){
        if (!online) {
            network_snackbar = Snackbar.make(getWindow().getDecorView().getRootView(), getString(R.string.no_connection), Snackbar.LENGTH_INDEFINITE);
            network_snackbar.getView().setBackgroundColor(Color.parseColor("#F27474"));
            network_snackbar.show();
        }else if (network_snackbar!=null){
            network_snackbar.dismiss();
        }
    }
    BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean online = intent.getBooleanExtra("is_online", false);
            networkSnackbar(online);
        }
    };
    FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
    private void setRemoteConfig(){
        remoteConfig.setConfigSettings(new FirebaseRemoteConfigSettings.Builder()/*.setDeveloperModeEnabled(true)*/.build());
        HashMap<String, Object> defaults = new HashMap<>();
        defaults.put("stores_urgent_update", BuildConfig.VERSION_NAME);
        remoteConfig.setDefaults(defaults);
        Task<Void> fetch = remoteConfig.fetch(TimeUnit.HOURS.toSeconds(5));
        fetch.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                remoteConfig.activateFetched();
                checkUpdateUrgency(NetworkCheckingActivity.this);
            }
        });
    }
    public static void checkUpdateUrgency(final Context context){
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        String version = remoteConfig.getString("stores_urgent_update");
        if (!TextUtils.equals(version, BuildConfig.VERSION_NAME)){
            new SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE)
                    .isCancelable(false)
                    .setTitleText("تحديث هام")
                    .setContentText("هذه النسخة من التطبيق لم تعد صالحة، نرجوا التحديث")
                    .setConfirmText("تحديث")
                    .setCancelText("اغلاق")
                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse("market://details?id=ml.dukan.stores"));
                            context.startActivity(intent);
                            ((Activity) context).finish();
                        }
                    })
                    .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                            ((Activity) context).finish();
                        }
                    }).show();
        }
    }






    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRemoteConfig();
    }


    @Override
    protected void onStart() {
        super.onStart();
        networkSnackbar(Util.isOnline(getApplicationContext()));
        registerReceiver(networkReceiver, new IntentFilter(Util.NETWORK_RECEIVER_FILTER));

    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(networkReceiver);
    }
}
