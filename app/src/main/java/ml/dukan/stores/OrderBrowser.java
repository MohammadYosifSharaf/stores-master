package ml.dukan.stores;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import ml.dukan.stores.Adapters.OrderBrowserAdapter;
import ml.dukan.stores.HelperClasses.DatabaseAdapter;
import ml.dukan.stores.HelperClasses.Util;
import ml.dukan.stores.Models.Order;
import ml.dukan.stores.Models.Product;
import ml.dukan.stores.Models.User;
import ml.dukan.stores.SweetAlert.SweetAlertDialog;
import ml.dukan.stores.listeners.OrderChangeListener;

/**
 * Created by khaled on 06/07/17.
 */

public class

OrderBrowser extends NetworkCheckingActivity {

    DatabaseAdapter helper;
    OrderBrowserAdapter adapter;
    Order order;

    SharedPreferences preferences;

    String UID;
    DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
    @BindView(R.id.order_browser_recyclerView) RecyclerView recyclerView;
    @BindView(R.id.spinner) Spinner spinner;
    @BindView(R.id.order_browser_name) TextView customer_name;
    @BindView(R.id.order_browser_number) TextView customer_number;
    @BindView(R.id.order_browser_address) TextView customer_address;
    @BindView(R.id.order_browser_confirm) Button confirmTV;
    @BindView(R.id.order_browser_totalPrice) TextView totalPrice;
    @BindView(R.id.noteCheckBox) CheckBox noteCheckBox;
    @BindView(R.id.noteET) EditText noteET;

    FirebaseAnalytics firebaseAnalytics;
    boolean hasNote;
    boolean set_completed = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.order_browser);
        ButterKnife.bind(this);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        firebaseAnalytics = FirebaseAnalytics.getInstance(getApplicationContext());

        helper = new DatabaseAdapter(getApplicationContext());
        Bundle extra = getIntent().getExtras();
        String order_key = extra.getString("order_key");
        UID = FirebaseAuth.getInstance().getCurrentUser().getUid();


        initOrder (order_key);

        noteCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (!b){
                    noteET.setText("");
                }
                noteET.setEnabled(b);
                hasNote = b;
            }
        });
    }



    private void initOrder (final String order_key){
        Query q = ref.child("orders").child("markets")
                .child(UID).child(order_key);
        q.keepSynced(true);
        q.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                order = dataSnapshot.getValue(Order.class);
                order.firebase_key = order_key;
                if (order.status == Integer.parseInt(Util.ORDER_CANCELED)
                        ||
                    order.status == Integer.parseInt(Util.ORDER_CANCEL)){

                    alertOrderCanceled();

                    return;

                }
                if (!set_completed){
                    set_completed = true;
                    setUpMeta();
                    setUpRecyclerView();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void alertOrderCanceled (){
    }

    private void setUpRecyclerView(){
        adapter = new OrderBrowserAdapter(this, order.products);
        adapter.orderChangeListener = orderChangeListener;
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setUpMeta (){
        totalPrice.setText(getString(R.string.price_msg, order.totalPrice - order.discount));
        customer_number.setText(String.valueOf(order.personal_information.number));
        customer_name.setText(order.personal_information.name);
        customer_address.setText(order.personal_information.address);
    }

    public void confirmButton (View v){
        if (adapter != null) {
            if (adapter.isHealthy()) {
                accept();
            } else {
                notifyChanges();
            }
            confirmTV.setEnabled(false);
        }
    }

    OrderChangeListener orderChangeListener = new OrderChangeListener() {
        @Override
        public void OnChange() {
            recalculateTotal();
        }
    };

    private void recalculateTotal (){
        SparseBooleanArray deselectedList = adapter.getDeselectedItems();
        SparseIntArray adjustedQuantity = adapter.getAdjustedQuantity();
        Map<String, Product> alternatives = adapter.alternatives;
        order.totalPrice = Util.totalOrderPrice(order.products, deselectedList, alternatives, adjustedQuantity);
        totalPrice.setText(getString(R.string.price_msg, order.totalPrice));

        if (adapter.isHealthy()) {
            confirmTV.setText(getString(R.string.order_accept));
        } else {
            confirmTV.setText(getString(R.string.order_adjusted));
        }
        confirmTV.setEnabled(deselectedList.size() != order.products.size());
    }

    public void showDirectionMap(View v) {
        Intent intent = new Intent(OrderBrowser.this, DirectionMap.class);

        float s_lat = Float.parseFloat(preferences.getString(Util.STORE_LAT, "0"));
        float s_lng = Float.parseFloat(preferences.getString(Util.STORE_LNG, "0"));

        intent.putExtra("name", order.personal_information.name);
        intent.putExtra("c_lat", (float) order.personal_information.latitude);
        intent.putExtra("c_lng", (float) order.personal_information.longitude);

        intent.putExtra("s_lat", s_lat);
        intent.putExtra("s_lng", s_lng);

        firebaseAnalytics.logEvent("show_direction_map", null);

        startActivity(intent);
    }

    public void shareWhatsapp(View v) {
        String textToShare = String.format("الاسم: %s\n" +
                        "رقم الجوال: %s\n" +
                        "العنوان:\n" +
                        "%s\n" +
                        "\n" +
                        "%s",
                order.personal_information.name,
                order.personal_information.number,
                order.personal_information.address,
                "http://maps.google.com/maps?daddr=" + order.personal_information.latitude
                        + "," + order.personal_information.longitude);
        Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
        whatsappIntent.setType("text/plain");
        whatsappIntent.setPackage("com.whatsapp");
        whatsappIntent.putExtra(Intent.EXTRA_TEXT, textToShare);
        try {
            firebaseAnalytics.logEvent("share_whatsapp", null);
            startActivity(whatsappIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            //ToastHelper.MakeShortText("Whatsapp have not been installed.");
        }
    }

    public void decline(View v) {
        new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText(getString(R.string.alert_reject_title))
                .setContentText(getString(R.string.alert_reject_content))
                .showEditText(true, getString(R.string.alert_reject_title))
                .hasMinLines(2)
                .setConfirmText(getString(R.string.yes))
                .setCancelText(getString(R.string.no))
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.dismissWithAnimation();
                        String reason = sweetAlertDialog.getText();
                        rejectOrder(reason);
                    }
                })
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.dismissWithAnimation();
                    }
                }).show();

    }

    private void rejectOrder (final String reason){
        Map<String, Object> order_updates = setupUpdate(Util.ORDER_DECLINED, reason);

        ref.child("orders").updateChildren(order_updates).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Bundle bundle = new Bundle();
                    bundle.putFloat("total_price", order.totalPrice);
                    if (hasNote) {
                        String note = noteET.getText().toString().trim();
                        bundle.putString("note", note);
                    }
                    if (reason!=null && reason.length()>0) {
                        bundle.putString("rejection_reason", reason);
                    }
                    long duration = (System.currentTimeMillis()-order.date);
                    bundle.putLong("response_time", duration);
                    bundle.putLong(FirebaseAnalytics.Param.VALUE, duration);
                    bundle.putLong("date", System.currentTimeMillis());
                    firebaseAnalytics.logEvent("order_rejected", bundle);

                    Intent intent = new Intent();
                    intent.putExtra("order_key", order.firebase_key);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });
    }

    private void accept() {

        Map<String, Object> order_update = setupUpdate(Util.ORDER_UNDER_PROCESSING, null);

        ref.child("orders").updateChildren(order_update).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Bundle bundle = new Bundle();
                    bundle.putFloat("total_price", order.totalPrice);
                    if (hasNote) {
                        String note = noteET.getText().toString().trim();
                        bundle.putString("note", note);
                    }
                    long duration = Util.getDuration(spinner.getSelectedItemPosition());
                    bundle.putLong("response_time", (System.currentTimeMillis()-order.date));
                    bundle.putLong("delivery_time", duration);
                    bundle.putLong("date", System.currentTimeMillis());
                    bundle.putLong(FirebaseAnalytics.Param.VALUE, duration);
                    firebaseAnalytics.logEvent("order_accepted", bundle);

                    new SweetAlertDialog(OrderBrowser.this, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText(getString(R.string.accept_title))
                            .setContentText(getString(R.string.accept_msg))
                            .showCancelButton(false)
                            .setConfirmText(getString(R.string.close_title))
                            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sweetAlertDialog) {
                                    sweetAlertDialog.dismiss();
                                    Intent intent = new Intent();
                                    intent.putExtra("order_key", order.firebase_key);
                                    setResult(RESULT_OK, intent);
                                    finish();
                                }
                            }).show();
                }
            }
        });
    }

    private Map<String, Object> setupUpdate (String status, String reason){
        Map<String, Object> map = new HashMap<>();
        long duration = Util.getDuration(spinner.getSelectedItemPosition());
        String customer_location = "customers/"+order.uid+'/'+order.firebase_key;
        String market_location = "markets/"+UID+'/'+order.firebase_key;

        map.put(market_location+"/status", Integer.parseInt(status));
        map.put(customer_location+"/status", Integer.parseInt(status));
        map.put(customer_location+"/notify", true);

        if (status.equals(Util.ORDER_UNDER_PROCESSING)){
            map.put(customer_location+"/delivery_time", duration);
            map.put(market_location+"/delivery_time", duration);
        }

        if (status.equals(Util.ORDER_ADJUSTED)) {
            ArrayList<Product> products = adapter.getFinalProducts();
            float totalPrice = Util.totalOrderPrice(products);
            map.put(market_location+"/totalPrice", totalPrice);
            map.put(customer_location+"/totalPrice", totalPrice);
            map.put(market_location+"/products", products);
            map.put(customer_location+"/products", products);
        } else if (status.equals(Util.ORDER_UNDER_PROCESSING)){
            float totalPrice = Util.totalOrderPrice(order.products);
            map.put(market_location+"/totalPrice", totalPrice);
            map.put(customer_location+"/totalPrice", totalPrice);
            map.put(market_location+"/products", order.products);
            map.put(customer_location+"/products", order.products);
        }

        if (hasNote) {
            String note = noteET.getText().toString().trim();
            if (note.length() > 0) {
                map.put(market_location+"/note", note);
                map.put(customer_location+"/note", note);
            }
        }

        if (status.equals(Util.ORDER_DECLINED) && reason!=null && reason.length() > 0){
            map.put(customer_location+"/rejection_reason", reason);
            map.put(market_location+"/rejection_reason", reason);
        }
        return map;
    }

    private void notifyChanges() {
        Map<String, Object> order_updates = setupUpdate(Util.ORDER_ADJUSTED, null);

        ref.child("orders").updateChildren(order_updates).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {

                    Bundle bundle = new Bundle();
                    bundle.putFloat("total_price", order.totalPrice);
                    if (hasNote) {
                        String note = noteET.getText().toString().trim();
                        bundle.putString("note", note);
                    }
                    long duration = (System.currentTimeMillis()-order.date);
                    bundle.putLong("response_time", duration);
                    bundle.putLong(FirebaseAnalytics.Param.VALUE, duration);
                    bundle.putLong("date", System.currentTimeMillis());
                    int[] adjusted_items_count = adapter.getAdjustedItemsCount();
                    bundle.putInt("DESELECTED", adjusted_items_count[0]);
                    bundle.putInt("QUANTITY_CHANGE", adjusted_items_count[1]);
                    bundle.putInt("ALTERNATIVE_CHANGE", adjusted_items_count[2]);
                    bundle.putInt("QUANTITY_ALTERNATIVE_CHANGE", adjusted_items_count[3]);
                    bundle.putString("uid", UID);
                    firebaseAnalytics.logEvent("order_adjusted", bundle);

                    new SweetAlertDialog(OrderBrowser.this, SweetAlertDialog.WARNING_TYPE)
                            .setTitleText(getString(R.string.notify_change_title))
                            .setContentText(getString(R.string.notify_change_msg))
                            .showCancelButton(false)
                            .setConfirmText(getString(R.string.close_title))
                            .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(SweetAlertDialog sweetAlertDialog) {
                                    sweetAlertDialog.dismiss();
                                    Intent intent = new Intent();
                                    intent.putExtra("order_key", order.firebase_key);
                                    setResult(RESULT_OK, intent);
                                    finish();
                                }
                            }).show();
                }
            }
        });
    }

    private static final int ALTERNATIVE_CODE = 5001;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ALTERNATIVE_CODE){
            if (resultCode == RESULT_OK){
                String name = data.getStringExtra("name");
                String barcode = data.getStringExtra("barcode");
                float price = data.getFloatExtra("price", 0f);
                String change_barcode = data.getStringExtra("change_barcode");
                Product product = new Product(name, barcode, price, 1);
                adapter.addAlternative(change_barcode, product);
            }
        }

    }
}