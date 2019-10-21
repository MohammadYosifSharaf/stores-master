package ml.dukan.stores;

import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import ml.dukan.stores.HelperClasses.Util;
import ml.dukan.stores.Models.Order;
import ml.dukan.stores.Models.Product;

/**
 * Created by Khaled on 20/08/17.
 */

public class DebtLineItemsActivity extends NetworkCheckingActivity{

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    String uid;
    String shopper_uid;
    String invoice_ref;

    DatabaseReference base_ref = FirebaseDatabase.getInstance().getReference();
    DatabaseReference invoice_lineItems_ref;


    ArrayList<Order> list = new ArrayList<>();
    DebtLineItemsAdapter adapter;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycler_view);
        ButterKnife.bind(this);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Bundle extras = getIntent().getExtras();
        invoice_ref = extras.getString("invoice_ref");
        shopper_uid = extras.getString("shopper_uid");


        invoice_lineItems_ref = base_ref.child("debt_lineitems").child(shopper_uid).child(uid).child(invoice_ref);

        adapter = new DebtLineItemsAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        Query q = invoice_lineItems_ref;

        q.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Order order = dataSnapshot.getValue(Order.class);
                list.add(order);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

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
        q.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }





    class DebtLineItemsAdapter extends RecyclerView.Adapter<DebtLineItemsAdapter.ViewHolder>{

        StorageReference storageReference;
        int PRODUCTS_IMAGES_SIZE = 0;
        int PRODUCTS_IMAGES_MARGIN = 0;


        public DebtLineItemsAdapter (){
            storageReference = FirebaseStorage.getInstance().getReference();
            calculateProductImageSize();
        }
        private void calculateProductImageSize(){
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            PRODUCTS_IMAGES_MARGIN = (int) (5 * getResources().getDisplayMetrics().density);
            PRODUCTS_IMAGES_SIZE = (int) Math.ceil(size.x/7.5)  ;
        }




        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_status_item2, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            final Order order = list.get(position);

            /**
             *  Set meta-data
             */
            Date date = new Date(order.date);
            holder.date.setText(Util.formatDate(getApplicationContext(), date));
            float total_price = order.totalPrice;
            String total_msg = getString(R.string.colored_total, total_price);
            holder.totalPrice.setText(Util.toHTML(total_msg));
            if (order.note!=null){
                holder.note.setVisibility(View.VISIBLE);
                String note = getString(R.string.note_title, order.note);
                holder.note.setText(Util.toHTML(note));
            }else {
                holder.note.setVisibility(View.GONE);
            }


            int product_size = order.products.size();
            // Remove any views cached on this GridLayout (Recycling Issue)
            holder.products_grid.removeAllViews();
            /**
             * Loop through products, display at most 5 product
             */
            int row = 0;
            int col = 0;
            for (int i = 0; i < product_size; ++i) {
                Product p = order.products.get(i);
                // Create new ImageView
                ImageView imageView = new ImageView(getApplicationContext());
                // Set an appropriate LayoutParams
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = PRODUCTS_IMAGES_SIZE;
                params.height = PRODUCTS_IMAGES_SIZE;

                // Shift to next column & row
                params.columnSpec = GridLayout.spec(col);
                params.rowSpec = GridLayout.spec(row);
                // Apply 5dp right margin
                params.rightMargin = PRODUCTS_IMAGES_MARGIN;
                if (row!=0) params.topMargin = PRODUCTS_IMAGES_MARGIN;
                // Finally add view to GridLayout
                imageView.setLayoutParams(params);
                holder.products_grid.addView(imageView);
                GlideApp.with(getApplicationContext())
                        .load(storageReference.child(p.getImagePath()))
                        .diskCacheStrategy(DiskCacheStrategy.DATA)
                        .into(imageView);
                col++;
                if (i == 0) continue;
                if (i % 5 == 0) {row++; col = 0;};
            }
        }


        class ViewHolder extends RecyclerView.ViewHolder {
            TextView date, totalPrice, note;
            GridLayout products_grid;
            public ViewHolder(View itemView) {
                super(itemView);
                date = (TextView) itemView.findViewById(R.id.order_status_date);
                totalPrice = (TextView) itemView.findViewById(R.id.order_status_totalPrice);
                note = (TextView) itemView.findViewById(R.id.note);
                products_grid = (GridLayout) itemView.findViewById(R.id.order_status_products_grid);
            }
        }


        @Override
        public int getItemCount() {
            return list.size();
        }

    }


}
