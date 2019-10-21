package ml.dukan.stores;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by Khaled on 27/01/18.
 */

public class StatisticsActivity extends AppCompatActivity {


    TextView info;
    DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.footer_statistics);
      //  getSupportActionBar().setTitle(getString(R.string.statistics_title));

        info = (TextView) findViewById(R.id.stat_info);
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        ref.child("statistics").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long delivered_orders = dataSnapshot.child("delivered_orders").getValue(Long.class);
                long canceled_orders = dataSnapshot.child("canceled_orders").getValue(Long.class);
                float total_income = dataSnapshot.child("total_income").getValue(Float.class);
                long total = delivered_orders+canceled_orders;
                String str = getString(R.string.statistics, delivered_orders, canceled_orders, total, total_income);
                info.setText(str);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
