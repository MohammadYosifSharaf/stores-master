package ml.dukan.stores.HelperClasses;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import ml.dukan.stores.Models.Product;
import ml.dukan.stores.R;
import ml.dukan.stores.SweetAlert.SweetAlertDialog;

/**
 * Created by khaled on 18/06/17.
 */

public class Util {
    public static long MAX_AD = 100000000000000000L, MAX_BROWSE = 100000L;
    public final static String STORE_NAME = "name", STORE_PHONE_NUMBER = "phone_number",
                                STORE_TELEPHONE_NUMBER = "telephone_number",
                                STORE_TRADE_NUMBER = "trade_number",
                                STORE_HAS_VEG = "store_has_veg",
                                STORE_TAKES_TAX = "store_takes_tax",
                                STORE_OWNER_NAME = "owner_name",  STORE_DRIVER_NUMBER = "driver_number",
                                STORE_DELIVERING_RADIUS = "delivering_radius",
                                STORE_ADDRESS = "address", STORE_LAT = "store_lat", STORE_LNG = "lng",
                                STORE_PHOTO = "store_photo",
            ORDER_CANCEL = "0",
            ORDER_PLACED = "1",
            ORDER_UNDER_PROCESSING = "2",
            ORDER_DELIVERED = "3",
            ORDER_ADJUSTED = "4",
            ORDER_CANCELED = "5",
            ORDER_DECLINED = "6";
    public final static String NETWORK_RECEIVER_FILTER = "NRF";
    public static void showFeedbackAlert (final Context context){
        new SweetAlertDialog(context, SweetAlertDialog.CUSTOM_IMAGE_TYPE)
                .setCustomImage(R.drawable.feedback)
                .setTitleText(context.getString(R.string.feed_back_title))
                .showEditText(true, "")
                .hasMinLines(4)
                .setConfirmText(context.getString(R.string.send))
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        String text = sweetAlertDialog.getText();
                        if (!TextUtils.isEmpty(text)){
                            sendFeedback(text);
                        }else{
                            sweetAlertDialog.dismissWithAnimation();
                        }
                        sweetAlertDialog.setTitleText(context.getString(R.string.feed_back_thank_title));
                        //sweetAlertDialog.setContentText(context.getString(R.string.feed_back_thank_content));
                        sweetAlertDialog.setConfirmText(context.getString(R.string.close_title));
                        sweetAlertDialog.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                sweetAlertDialog.dismissWithAnimation();
                            }
                        });
                        sweetAlertDialog.changeAlertType(SweetAlertDialog.SUCCESS_TYPE);
                    }
                })
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.dismissWithAnimation();
                    }
                })
                .show();
    }
    private static void sendFeedback (String text){
        String uid = getUID();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        ref.child("feedback").child(uid==null? "unknown" : uid).push().setValue(text);
    }

    private static String getUID (){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user!=null?user.getUid():null;
    }

    public static int getOverallQuantity(ArrayList<Product> list){
        int n = 0;
        for (Product p : list){
            n += p.quantity;
        }
        return n;
    }


    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static String formatDate(Context context, Date messageDate) {
        String time = "";
        DateFormat numbericFormat = new SimpleDateFormat("yyMMdd");
        DateFormat yearFormat = new SimpleDateFormat("yy");
        Date now = new Date(new Date().getTime());
        int deltaDays = Integer.parseInt(numbericFormat.format(now)) - Integer.parseInt(numbericFormat.format(messageDate));
        int deltaYear = Integer.parseInt(yearFormat.format(now)) - Integer.parseInt(yearFormat.format(messageDate));
        if (deltaDays == 0) {
            DateFormat less_than_weekFormat = new SimpleDateFormat(" KK:mm a", new Locale("ar"));
            time = context.getResources().getString(R.string.today) + less_than_weekFormat.format(messageDate);
        } else if (deltaDays == 1) {
            DateFormat less_than_weekFormat = new SimpleDateFormat(" KK:mm a", new Locale("ar"));
            time = context.getResources().getString(R.string.yesterday) + less_than_weekFormat.format(messageDate);
        } else if (deltaDays < 7) {
            DateFormat less_than_weekFormat = new SimpleDateFormat("E KK:mm a", new Locale("ar"));
            time = less_than_weekFormat.format(messageDate);
        } else if (deltaYear > 0) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd    KK:mm a", new Locale("ar"));
            time = dateFormat.format(messageDate);
        } else {
            DateFormat more_than_weekFormat = new SimpleDateFormat("E،  d MMM  KK:mm a", new Locale("ar"));
            time = more_than_weekFormat.format(messageDate);
        }
        return time;
    }
    public static Spanned toHTML(String html) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT);
        } else {
            return Html.fromHtml(html);
        }
    }


    public static float calculateAverage (float cumulative, int num){
        num = num == 0 ? 1 : num;
        return cumulative / num;
    }

    public static final int QUANTITY_CHANGE = 1, ALTERNATIVE_CHANGE = 2, QUANTITY_ALTERNATIVE_CHANGE = 3, DESELECTED = 4;

    public static final long
            TEN_MINUTES = 10 * 60 * 1000,
            FIFTEEN_MINUTES = 15 * 60 * 1000,
            THIRTY_MINUTES = 30 * 60 * 1000,
            HOUR = 60 * 60 * 1000;

    public static long getDuration (int selection){
        long now = System.currentTimeMillis();
        switch (selection){
            case 0: return now+TEN_MINUTES;
            case 1: return now+FIFTEEN_MINUTES;
            case 2: return now+THIRTY_MINUTES;
            case 3: return now+HOUR;
            default: return now+TEN_MINUTES;
        }
    }

    public static void signOut (Context context){
        FirebaseAuth.getInstance().signOut();
        new DatabaseAdapter(context).drop();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(Util.STORE_NAME);
        editor.remove(Util.STORE_OWNER_NAME);
        editor.remove(Util.STORE_PHONE_NUMBER);
        editor.remove(Util.STORE_DELIVERING_RADIUS);
        editor.remove(Util.STORE_ADDRESS);
        editor.remove(Util.STORE_LAT);
        editor.remove(Util.STORE_LNG);
        editor.apply();
    }

    public static float totalOrderPrice(ArrayList<Product> list, SparseBooleanArray selectedList, Map<String, Product> alternatives, SparseIntArray adjustedQuantity) {
        float total = 0;
        int counter = 0;
        for (Product p : list) {
            if (alternatives.containsKey(p.barcode))
                p = alternatives.get(p.barcode);
            boolean deselected = selectedList.get(counter, false);
            int q = adjustedQuantity.get(counter, -1);
            counter++;
            if (deselected) continue;
            total += q == -1 ? p.getTotalPrice() : p.getTotalPrice(q);
        }
        return total;
    }

    public static float totalOrderPrice(ArrayList<Product> list) {
        float total = 0;
        for (Product p : list) {
            total += p.price;
        }
        return total;
    }



    public static double distanceBetween (Location store, double cLat, double cLng){
        Location client = new Location("client");
        client.setLatitude(cLat);
        client.setLongitude(cLng);
        return store.distanceTo(client);
    }

}
