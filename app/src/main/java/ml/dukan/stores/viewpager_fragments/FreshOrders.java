package ml.dukan.stores.viewpager_fragments;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spanned;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Date;
import java.util.Locale;

import ml.dukan.stores.GlideApp;
import ml.dukan.stores.HelperClasses.Util;
import ml.dukan.stores.MainActivity;
import ml.dukan.stores.Models.Order;
import ml.dukan.stores.Models.Product;
import ml.dukan.stores.OrderBrowser;
import ml.dukan.stores.R;
import ml.dukan.stores.ViewHolder.FreshOrderViewHolder;

/**
 * Created by khaled on 02/07/17.
 */


public class FreshOrders extends Fragment {

    RecyclerView recyclerView;
    MainActivity mainActivity;

    FirebaseRecyclerAdapter adapter;
    int PRODUCTS_IMAGES_SIZE = 0;
    int PRODUCTS_IMAGES_MARGIN = 0;
    String debt_type, cash_type;
    StorageReference storageReference = FirebaseStorage.getInstance().getReference();

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null){
            adapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null){
            adapter.stopListening();
        }
    }

    @Nullable
    View root;
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        root = inflater.inflate(R.layout.fragment_fresh_orders, container, false);
        recyclerView = (RecyclerView) root.findViewById(R.id.fresh_orders_lv);
        mainActivity = ((MainActivity) getActivity());

        calculateProductImageSize();

        String UID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference orders_ref = FirebaseDatabase.getInstance().getReference()
                .child("orders").child("markets").child(UID);
        Query orders_query =
                orders_ref.orderByChild("status")
                        .equalTo(Integer.parseInt(Util.ORDER_PLACED));
        debt_type = getActivity().getString(R.string.debt_type);
        cash_type = getActivity().getString(R.string.cash_type);


        FirebaseRecyclerOptions<Order> options =
                new FirebaseRecyclerOptions.Builder<Order>()
                        .setQuery(orders_query, Order.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<Order, FreshOrderViewHolder>(options) {

            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (isAdded()){
                    int item_count = getItemCount();
                    mainActivity.updateBadge(1, item_count);
                    if (item_count == 0){
                        root.findViewById(R.id.empty_message).setVisibility(View.VISIBLE);
                        ((TextView) root.findViewById(R.id.message)).setText(getString(R.string.empty_fresh_orders));
                    }else {
                        root.findViewById(R.id.empty_message).setVisibility(View.GONE);
                    }
                }
            }

            @NonNull
            @Override
            public FreshOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fresh_order, parent, false);
                return new FreshOrderViewHolder(v);
            }

            @Override
            protected void onBindViewHolder(@NonNull FreshOrderViewHolder holder, int position, @NonNull Order o) {
                o.firebase_key = getRef(position).getKey();

                if (o.personal_information.subscription_number!=null) {
                    holder.customer_name.setText(String.format(Locale.getDefault(), "%s, %s", o.personal_information.name, o.personal_information.subscription_number));
                }else {
                    holder.customer_name.setText(o.personal_information.name);
                }
                Spanned address = Util.toHTML(getActivity().getResources().getString(R.string.address_msg, o.personal_information.address));
                holder.customer_address.setText(address);
                holder.date_view.setText(Util.formatDate(getActivity().getApplicationContext(), new Date(o.date)));



                holder.totalPrice.setVisibility(View.VISIBLE);

                String type = o.payment_method.equals("DEBT_METHOD") ? debt_type : cash_type;
                if (o.discount != 0){
                    if (o.discount == o.totalPrice){
                        holder.payment_type.setText(getString(R.string.paid_title));
                        holder.totalPrice.setVisibility(View.GONE);
                    } else {
                        String discount_msg = getString(R.string.discount, o.discount);
                        Spanned payment_type_msg = Util.toHTML( discount_msg +
                                "\n" +
                                getActivity().getString(R.string.payment_type_msg, type) );
                        holder.payment_type.setText(payment_type_msg);
                        holder.totalPrice.setText(Util.toHTML(getActivity().getResources().getString(R.string.colored_total, o.totalPrice-o.discount)));
                    }
                }else {
                    Spanned payment_type_msg = Util.toHTML(getActivity().getString(R.string.payment_type_msg, type) );
                    holder.payment_type.setText(payment_type_msg);
                    holder.totalPrice.setText(Util.toHTML(getActivity().getResources().getString(R.string.colored_total, o.totalPrice)));
                }


                int product_size = o.products.size();
                // Remove any views cached on this GridLayout (Recycling Issue)
                holder.products_grid.removeAllViews();
                /**
                 * Loop through products, display at most 5 product
                 */
                for (int j = 0; j < product_size; ++j) {
                    Product p = o.products.get(j);
                    // Create new ImageView
                    ImageView imageView = new ImageView(getActivity());
                    // Set an appropriate LayoutParams
                    GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                    /**
                     * if we're in the 6th product
                     * we shall display a PLUS icon in %75 of the regular size.
                     */
                    if (j == 5) {
                        params.width = (int) (PRODUCTS_IMAGES_SIZE*0.75f);
                        params.height = (int) (PRODUCTS_IMAGES_SIZE*0.75f);
                    }
                    /**
                     * Otherwise apply the regular size.
                     */
                    else{
                        params.width = PRODUCTS_IMAGES_SIZE;
                        params.height = PRODUCTS_IMAGES_SIZE;
                    }

                    // Shift to next column.
                    params.columnSpec = GridLayout.spec(j);
                    // Apply 5dp right margin.
                    params.rightMargin = PRODUCTS_IMAGES_MARGIN;
                    // Finally add view to GridLayout.
                    imageView.setLayoutParams(params);
                    holder.products_grid.addView(imageView);
                    // Display Images or Plus icon.
                    if (j == 5) {
                        imageView.setImageResource(R.drawable.plus_circle);

                    }else {
                        GlideApp.with(getActivity().getApplicationContext())
                                .load(storageReference.child(p.getImagePath()))
                                .into(imageView);
                    }
                    // If we reached our max 6 columns break.
                    if (j == 5) break;
                }


                holder.showItems.setTag(position);
                holder.showItems.setOnClickListener(clickListener);
            }

            private View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = (int) view.getTag();
                    Order o = getItem(position);
                    Intent intent = new Intent(getActivity(), OrderBrowser.class);
                    intent.putExtra("order_key", o.firebase_key);
                    mainActivity.startActivityForResult(intent, 2020);
                }
            };

        };


        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);
        return root;
    }


    private void calculateProductImageSize(){
        Display display = mainActivity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        PRODUCTS_IMAGES_MARGIN = (int) (5 * getResources().getDisplayMetrics().density);
        PRODUCTS_IMAGES_SIZE = (int) Math.ceil(size.x/8)  ;
    }

}
