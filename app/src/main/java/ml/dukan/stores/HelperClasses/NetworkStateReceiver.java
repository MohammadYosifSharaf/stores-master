package ml.dukan.stores.HelperClasses;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static ml.dukan.stores.HelperClasses.Util.NETWORK_RECEIVER_FILTER;

/**
 * Created by Khaled on 06/09/17.
 */

public class NetworkStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        notifyActivities(context, Util.isOnline(context));
    }

    private void notifyActivities (Context context, boolean online){
        Intent intent = new Intent();
        intent.setAction(NETWORK_RECEIVER_FILTER);
        intent.putExtra("is_online", online);
        context.sendBroadcast(intent);
    }

}
