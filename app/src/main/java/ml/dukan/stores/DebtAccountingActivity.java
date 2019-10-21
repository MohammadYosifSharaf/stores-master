package ml.dukan.stores;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import ml.dukan.stores.HelperClasses.Util;
import ml.dukan.stores.Models.DebtSummary;
import ml.dukan.stores.Models.User;

/**
 * Created by Khaled on 17/08/17.
 */

public class DebtAccountingActivity extends NetworkCheckingActivity {

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    DatabaseReference base_ref = FirebaseDatabase.getInstance().getReference();
    DatabaseReference debt_accounts_ref;
    String uid;
    Drawable greed_edge, red_edge;
    String acc_enable, acc_disable;
    ArrayList<DebtSummary> data = new ArrayList<>();
    DebtAccountAdapter DebtAdapter;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycler_view);
        ButterKnife.bind(this);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        DebtAdapter = new DebtAccountAdapter(data);
        recyclerView.setAdapter(DebtAdapter);

        debt_accounts_ref = base_ref.child("debt_summary/markets").child(uid);
        debt_accounts_ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                addDebt(dataSnapshot);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                addDebt(dataSnapshot);
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
        debt_accounts_ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                showNoDebt(dataSnapshot.getChildrenCount() == 0);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        greed_edge = ResourcesCompat.getDrawable(getResources(), R.drawable.button_green_edge, null);
        red_edge = ResourcesCompat.getDrawable(getResources(), R.drawable.button_red_edge, null);

        acc_enable = getResources().getString(R.string.enable_debt_acc);
        acc_disable = getResources().getString(R.string.disable_debt_acc);
    }

    private void addDebt (DataSnapshot dataSnapshot){
        final String customer_uid = dataSnapshot.getKey();
        final DebtSummary debtSummary = dataSnapshot.getValue(DebtSummary.class);
        boolean join_profile = true;
        for (int i = 0; i < data.size(); ++i){
            if (data.get(i).profile.uid.equals(customer_uid)){
                join_profile = false;
                data.get(i).copy(debtSummary);
                DebtAdapter.notifyDataSetChanged();
                break;
            }
        }
        if (join_profile)
            base_ref.child("users").child(customer_uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    User user = dataSnapshot.getValue(User.class);
                    user.uid = customer_uid;
                    debtSummary.profile = user;
                    data.add(debtSummary);
                    DebtAdapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
    }

    class DebtAccountAdapter extends RecyclerView.Adapter<DebtAccountAdapter.ViewHolder>{
        ArrayList<DebtSummary> list;
        public DebtAccountAdapter (ArrayList<DebtSummary> list){
            this.list = list;
        }
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_debt_account, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            DebtSummary model = list.get(position);
            holder.debt_price.setText(getString(R.string.price_msg, (model.total_price-model.partial_payment)));
            holder.name.setText(model.profile.name);
            holder.number.setText(model.profile.number);
            String address = getString(R.string.address_msg, model.profile.address);
            holder.address.setText(Util.toHTML(address));
            holder.show_invoices.setTag(position);
            holder.show_invoices.setOnClickListener(show_invoice_listener);
            if (model.enabled){
            }else{
            }

        }

        View.OnClickListener show_invoice_listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = (int) view.getTag();
                DebtSummary summary = list.get(position);
                Intent intent = new Intent(DebtAccountingActivity.this, DebtInvoicesActivity.class);
                intent.putExtra("shopper_uid", summary.profile.uid);
                startActivity(intent);
            }
        };



        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            public TextView name, number, address, debt_price, show_invoices;
            public ViewHolder(View itemView) {
                super(itemView);
                name = (TextView) itemView.findViewById(R.id.name);
                number = (TextView) itemView.findViewById(R.id.number);
                address = (TextView) itemView.findViewById(R.id.address);
                debt_price = (TextView) itemView.findViewById(R.id.debt_price);
                show_invoices = (TextView) itemView.findViewById(R.id.show_invoices);
            }
        }

    }

    private void showNoDebt (boolean val){
        if (val){
            findViewById(R.id.empty_message).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.message)).setText(getString(R.string.empty_debt));
        }else{
            findViewById(R.id.empty_message).setVisibility(View.GONE);
        }
    }


}