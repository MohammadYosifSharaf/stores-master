package ml.dukan.stores;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import ml.dukan.stores.HelperClasses.Util;
import ml.dukan.stores.Models.DebtInvoice;
import ml.dukan.stores.SweetAlert.SweetAlertDialog;

/**
 * Created by Khaled on 19/08/17.
 */

public class DebtInvoicesActivity extends NetworkCheckingActivity {

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    String uid;
    String shopper_uid;

    DatabaseReference base_ref = FirebaseDatabase.getInstance().getReference();
    ArrayList<DebtInvoice> invoices = new ArrayList<>();
    InvoiceAdapter adapter;

    FirebaseAnalytics firebaseAnalytics;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycler_view);
        ButterKnife.bind(this);

        firebaseAnalytics = FirebaseAnalytics.getInstance(getApplicationContext());


        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        shopper_uid = getIntent().getStringExtra("shopper_uid");

        adapter = new InvoiceAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        Query invoice_query = base_ref.child("debt_invoices").child(shopper_uid)
                .child(uid).limitToLast(15);
        invoice_query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Log.e("invoice", "onChildAdded()");
                DebtInvoice invoice = dataSnapshot.getValue(DebtInvoice.class);
                invoice.ref = dataSnapshot.getRef();
                addInvoice(invoice);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Log.e("invoice", "onChildChanged()");
                DebtInvoice invoice = dataSnapshot.getValue(DebtInvoice.class);
                invoice.ref = dataSnapshot.getRef();
                addInvoice(invoice);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        invoice_query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Collections.sort(invoices, new Comparator<DebtInvoice>() {
                    @Override
                    public int compare(DebtInvoice a, DebtInvoice b) {
                        long t1 = a.date;
                        long t2 = b.date;
                        if(t2 > t1)
                            return 1;
                        else if(t1 > t2)
                            return -1;
                        else
                            return 0;
                    }
                });
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void addInvoice (DebtInvoice invoice){
        for (int i = 0; i < invoices.size(); ++i){
            DebtInvoice e = invoices.get(i);
            if (e.date == invoice.date){
                e.copy(invoice);
                return;
            }
        }
        invoices.add(invoice);
    }



    class InvoiceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        class ViewHolder extends RecyclerView.ViewHolder{
            public TextView date, price_details;
            Button partial_payment, total_payment;
            public ViewHolder(View itemView) {
                super(itemView);
                date = (TextView) itemView.findViewById(R.id.date);
                price_details = (TextView) itemView.findViewById(R.id.price_details);
                partial_payment = (Button) itemView.findViewById(R.id.partial_payment);
                total_payment = (Button) itemView.findViewById(R.id.total_payment);
            }
        }

        class PaidViewHolder extends RecyclerView.ViewHolder{
            public TextView date, price_details;
            public PaidViewHolder(View itemView) {
                super(itemView);
                date = (TextView) itemView.findViewById(R.id.date);
                price_details = (TextView) itemView.findViewById(R.id.price_details);
            }
        }


        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView;
            if (viewType == 0){
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_debt_invoice_paid, parent, false);
                itemView.setOnClickListener(show_products_listener);
                return new PaidViewHolder(itemView);

            }else{
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_debt_invoice, parent, false);
                itemView.setOnClickListener(show_products_listener);
                return new ViewHolder(itemView);
            }
        }

        View.OnClickListener show_products_listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = recyclerView.getChildLayoutPosition(view);
                DebtInvoice invoice = invoices.get(position);
                Intent intent = new Intent(DebtInvoicesActivity.this, DebtLineItemsActivity.class);
                intent.putExtra("shopper_uid", shopper_uid);
                intent.putExtra("invoice_ref", invoice.ref.getKey());
                startActivity(intent);
            }
        };

        @Override
        public int getItemViewType(int position) {
            return invoices.get(position).paid ? 0 : 1;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder h, int position) {
            DebtInvoice model = invoices.get(position);

            int viewType = getItemViewType(position);

            if (viewType == 0){
                PaidViewHolder holder = (PaidViewHolder) h;
                holder.date.setText(Util.formatDate(getApplicationContext(), new Date(model.date)));
                String details = getString(R.string.invoice_total, model.total_price);
                holder.price_details.setText(Util.toHTML(details));
            }else{
                ViewHolder holder = (ViewHolder) h;
                holder.date.setText(Util.formatDate(getApplicationContext(), new Date(model.date)));
                float x = model.total_price-model.partial_payment;
                String details = getString(R.string.invoice_details, model.total_price, model.partial_payment, x);
                holder.price_details.setText(Util.toHTML(details));
                holder.partial_payment.setTag(position);
                holder.partial_payment.setOnClickListener(partial_payment_listener);

                holder.total_payment.setTag(position);
                holder.total_payment.setOnClickListener(total_payment_listener);
            }
        }

        View.OnClickListener total_payment_listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = (int) view.getTag();
                final DebtInvoice invoice = invoices.get(position);
                new SweetAlertDialog(DebtInvoicesActivity.this, SweetAlertDialog.NORMAL_TYPE)
                        .setTitleText(getString(R.string.pay_total))
                        .setConfirmText(getString(R.string.pay_title))
                        .setContentText(getString(R.string.pay_total_content))
                        .setCancelText(getString(R.string.cancel_title))
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                sweetAlertDialog.dismissWithAnimation();
                                payTotal(invoice);
                            }
                        })
                        .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                sweetAlertDialog.dismissWithAnimation();
                            }
                        }).show();
            }
        };

        View.OnClickListener partial_payment_listener= new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                int position = (int) view.getTag();
                final DebtInvoice invoice = invoices.get(position);
                new SweetAlertDialog(DebtInvoicesActivity.this, SweetAlertDialog.NORMAL_TYPE)
                        .setTitleText(getString(R.string.pay_partial))
                        .showEditText(true, "المبلغ")
                        .setInputType(InputType.TYPE_CLASS_NUMBER)
                        .setConfirmText(getString(R.string.pay_title))
                        .setCancelText(getString(R.string.cancel_title))
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                String text = sweetAlertDialog.getText();
                                if (text.length()==0){
                                    sweetAlertDialog.dismissWithAnimation();
                                    return;
                                }
                                float price = Float.parseFloat(text);
                                price += invoice.partial_payment;
                                if (price >= invoice.total_price){
                                    total_payment_listener.onClick(view);
                                }else{
                                    payPartial(price, invoice);
                                }
                                sweetAlertDialog.dismiss();

                            }
                        })
                        .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sweetAlertDialog) {
                                sweetAlertDialog.dismissWithAnimation();
                            }
                        }).show();

            }
        };

        void payPartial (float price, DebtInvoice invoice){
            String shopper_summary = String.format(Locale.US, "debt_summary/customers/%s/%s/partial_payment", shopper_uid, uid);
            String market_summary = String.format(Locale.US, "debt_summary/markets/%s/%s/partial_payment", uid, shopper_uid);
            String invoice_ref = String.format(Locale.US, "debt_invoices/%s/%s/%s/partial_payment", shopper_uid, uid, invoice.ref.getKey());
            Map<String, Object> updates = new HashMap<>();
            updates.put(shopper_summary, price);
            updates.put(market_summary, price);
            updates.put(invoice_ref, price);

            Bundle bundle = new Bundle();
            bundle.putLong("date", System.currentTimeMillis());
            bundle.putFloat("amount", price);
            bundle.putFloat("total_price", invoice.total_price);
            firebaseAnalytics.logEvent("debt_partial_payment", bundle);

            base_ref.updateChildren(updates);
        }

        void payTotal (final DebtInvoice invoice){
            String shopper_summary = String.format(Locale.US, "debt_summary/customers/%s/%s", shopper_uid, uid);
            String market_summary = String.format(Locale.US, "debt_summary/markets/%s/%s", uid, shopper_uid);
            String invoice_ref = String.format(Locale.US, "debt_invoices/%s/%s/%s", shopper_uid, uid, invoice.ref.getKey());
            Map<String, Object> updates = new HashMap<>();
            updates.put(shopper_summary+"/total_price", 0);
            updates.put(market_summary+"/total_price", 0);
            updates.put(shopper_summary+"/partial_payment", 0);
            updates.put(market_summary+"/partial_payment", 0);

            updates.put(shopper_summary+"/current_invoice", -1);
            updates.put(market_summary+"/current_invoice", -1);

            updates.put(invoice_ref+"/paid", true);
            updates.put(invoice_ref+"/paid_date", System.currentTimeMillis());

            Bundle bundle = new Bundle();
            bundle.putLong("date", System.currentTimeMillis());
            bundle.putFloat("total_price", invoice.total_price);
            firebaseAnalytics.logEvent("debt_total_payment", bundle);


            base_ref.updateChildren(updates);
        }

        @Override
        public int getItemCount() {
            return invoices.size();
        }
    }
}
