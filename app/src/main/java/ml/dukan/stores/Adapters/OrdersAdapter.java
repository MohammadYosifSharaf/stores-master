package ml.dukan.stores.Adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spanned;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Date;

import ml.dukan.stores.GlideApp;
import ml.dukan.stores.HelperClasses.DatabaseAdapter;
import ml.dukan.stores.HelperClasses.Util;
import ml.dukan.stores.MainActivity;
import ml.dukan.stores.Models.Order;
import ml.dukan.stores.Models.Product;
import ml.dukan.stores.OrderBrowser;
import ml.dukan.stores.R;

/**
 * Created by khaled on 12/07/17.
 */

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.ViewHolder> {


    Context context;
    SharedPreferences preferences;
    ArrayList<Order> list;
    StorageReference storageReference;
    RecyclerView recyclerView;
    Location storeLocation;
    MainActivity mainActivity;
    String uid;
    int PRODUCTS_IMAGES_SIZE = 0;
    int PRODUCTS_IMAGES_MARGIN = 0;

    DatabaseAdapter helper;
    public OrdersAdapter(Context context, RecyclerView recyclerView, ArrayList<Order> list){
        this.context = context;
        helper = new DatabaseAdapter(context.getApplicationContext());
        this.recyclerView = recyclerView;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        double lat = Double.parseDouble(preferences.getString(Util.STORE_LAT, "0"));
        double lng = Double.parseDouble(preferences.getString(Util.STORE_LNG, "0"));
        storeLocation = new Location("store");
        storeLocation.setLatitude(lat);
        storeLocation.setLongitude(lng);
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        storageReference = FirebaseStorage.getInstance().getReference();
        this.list = list;
        mainActivity = (MainActivity) context;


        calculateProductImageSize();
    }
    private void calculateProductImageSize(){
        Display display = mainActivity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        PRODUCTS_IMAGES_MARGIN = (int) (5 * context.getResources().getDisplayMetrics().density);
        PRODUCTS_IMAGES_SIZE = (int) Math.ceil(size.x/8)  ;
    }

    @Override
    public OrdersAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fresh_order, parent, false);
        return new ViewHolder(itemView);

    }


    class ViewHolder extends RecyclerView.ViewHolder {
        TextView customer_name, date_view, payment_type, totalPrice, customer_address;
        GridLayout products_grid;
        Button showItems;
        public ViewHolder(View view) {
            super(view);
            customer_name = (TextView) view.findViewById(R.id.order_fresh_customer_name);
            customer_address = (TextView) view.findViewById(R.id.item_fresh_customer_address);
            date_view = (TextView) view.findViewById(R.id.order_fresh_order_date);
            showItems = (Button) view.findViewById(R.id.item_fresh_show_items);
            totalPrice = (TextView) view.findViewById(R.id.item_fresh_order_totalPrice);
            payment_type = (TextView) view.findViewById(R.id.item_fresh_order_payment_type);
            products_grid = (GridLayout) view.findViewById(R.id.item_fresh_products_grid);
            showItems.setOnClickListener(showItemsClickListener);
        }
    }

    @Override
    public void onBindViewHolder(OrdersAdapter.ViewHolder holder, int position) {
        Order o = list.get(position);
        holder.customer_name.setText(o.personal_information.name);

        Spanned address = Util.toHTML(context.getResources().getString(R.string.address_msg, o.personal_information.address));
        holder.customer_address.setText(address);
        holder.date_view.setText(Util.formatDate(context, new Date(o.date)));

        holder.showItems.setTag(position);
        holder.totalPrice.setText(Util.toHTML(context.getResources().getString(R.string.colored_total, o.totalPrice)));


        String debt_type = context.getString(R.string.debt_type);
        String cash_type = context.getString(R.string.cash_type);
        String type = o.payment_method.equals("DEBT_METHOD") ? debt_type : cash_type;
        holder.payment_type.setText(Util.toHTML(context.getString(R.string.payment_type_msg, type)));


        int product_size = o.products.size();
        // Remove any views cached on this GridLayout (Recycling Issue)
        holder.products_grid.removeAllViews();
        /**
         * Loop through products, display at most 5 product
         */
        for (int j = 0; j < product_size; ++j) {
            Product p = o.products.get(j);
            // Create new ImageView
            ImageView imageView = new ImageView(context);
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

            // Shift to next column
            params.columnSpec = GridLayout.spec(j);
            // Apply 5dp right margin
            params.rightMargin = PRODUCTS_IMAGES_MARGIN;
            // Finally add view to GridLayout
            imageView.setLayoutParams(params);
            holder.products_grid.addView(imageView);
            // Display Images or Plus icon
            if (j == 5) {
                imageView.setImageResource(R.drawable.plus_circle);

            }else {
                GlideApp.with(context)
                        .load(storageReference.child(p.getImagePath()))
                        .into(imageView);
            }
            // If we reached our max 6 columns break
            if (j == 5) break;
        }

    }

    @Override
    public int getItemCount() {
        return list.size();
    }



    View.OnClickListener showItemsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int position = (int) view.getTag();
            Order o = list.get(position);
            Intent intent = new Intent(context, OrderBrowser.class);
            intent.putExtra("order_key", o.firebase_key);
            mainActivity.startActivityForResult(intent, 2020);
        }
    };


}
