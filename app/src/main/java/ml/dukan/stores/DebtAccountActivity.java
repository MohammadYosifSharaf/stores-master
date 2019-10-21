package ml.dukan.stores;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
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
import ml.dukan.stores.Models.DebtSummary;
import ml.dukan.stores.Models.User;

public class DebtAccountActivity extends AppCompatActivity {

    DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
    ArrayList<Pair<User, String>> list = new ArrayList<>();
    DebtAccountsAdapter adapter;

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    String uid;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycler_view);
        ButterKnife.bind(this);
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        adapter = new DebtAccountsAdapter(list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        ref.child("debt_accounts/markets").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot s :  dataSnapshot.getChildren()){
                    final String uid = s.getKey();
                    final String status = s.child("status").getValue(String.class);
                    ref.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()){
                                User user = dataSnapshot.getValue(User.class);
                                user.uid = uid;
                                list.add(new Pair<User, String>(user, status));
                                adapter.notifyDataSetChanged();
                            }

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }


    class DebtAccountsAdapter extends RecyclerView.Adapter<DebtAccountsAdapter.ViewHolder> {

        ArrayList<Pair<User, String>> list;
        String acc_enable, acc_disable;
        Drawable green_edge, red_edge;
        public DebtAccountsAdapter (ArrayList<Pair<User, String>> list){
            this.list = list;
            acc_enable = getResources().getString(R.string.enable_debt_acc);
            acc_disable = getResources().getString(R.string.disable_debt_acc);
            green_edge = ResourcesCompat.getDrawable(getResources(), R.drawable.button_green_edge, null);
            red_edge = ResourcesCompat.getDrawable(getResources(), R.drawable.button_red_edge, null);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_debt_accounting, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Pair<User, String> item = list.get(position);
            holder.name.setText(item.first.name);
            holder.phone_number.setText(item.first.number);
            holder.enable_disable_button.setTag(position);
            holder.enable_disable_button.setOnClickListener(enable_disable_acc);

            switch (item.second){
                case "DEBT_ACC_ISSUED":
                case "DEBT_ACC_REJECTED":
                    holder.enable_disable_button.setText(acc_enable);
                    holder.enable_disable_button.setBackground(green_edge);
                    break;
                case "DEBT_ACC_ACCEPTED":
                    holder.enable_disable_button.setBackground(red_edge);
                    holder.enable_disable_button.setText(acc_disable);
                    break;

            }
        }

        View.OnClickListener enable_disable_acc = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = (int) view.getTag();
                Map<String, Object> map = new HashMap<>();
                User user = list.get(position).first;
                String customer_uid = user.uid;
                String new_status = "";
                boolean val;
                if (list.get(position).second.equals("DEBT_ACC_ACCEPTED")){
                    new_status = "DEBT_ACC_REJECTED";
                    val = false;
                }else{
                    new_status = "DEBT_ACC_ACCEPTED";
                    val = true;
                }
                map.put(String.format("debt_accounts/customers/%s/%s/status", customer_uid, uid), new_status);
                map.put(String.format("debt_accounts/markets/%s/%s/status", uid, customer_uid), new_status);
                map.put(String.format("debt_summary/customers/%s/%s/enabled", customer_uid, uid), val);
                map.put(String.format("debt_summary/markets/%s/%s/enabled", uid, customer_uid), val);
                ref.updateChildren(map);
                list.get(position).second = new_status;
                notifyDataSetChanged();
            }
        };


        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, status, phone_number, enable_disable_button;
            public ViewHolder(View itemView) {
                super(itemView);
                name = (TextView) itemView.findViewById(R.id.name);
                phone_number = (TextView) itemView.findViewById(R.id.number);
                status = (TextView) itemView.findViewById(R.id.status);
                enable_disable_button = (TextView) itemView.findViewById(R.id.enable_disable);
            }
        }
    }

    class Pair <S, V> {
        S first;
        V second;
        public Pair (S s, V v){
            this.first = s;
            this.second = v;
        }
    }
}
