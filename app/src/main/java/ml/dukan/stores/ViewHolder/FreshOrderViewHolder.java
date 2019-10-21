package ml.dukan.stores.ViewHolder;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import ml.dukan.stores.Models.Order;
import ml.dukan.stores.OrderBrowser;
import ml.dukan.stores.R;

/**
 * Created by Khaled on 18/09/17.
 */
public class FreshOrderViewHolder extends RecyclerView.ViewHolder{
    public TextView customer_name, date_view, payment_type, totalPrice, customer_address;
    public GridLayout products_grid;
    public Button showItems;
    public FreshOrderViewHolder(View view) {
        super(view);
        customer_name = (TextView) view.findViewById(R.id.order_fresh_customer_name);
        customer_address = (TextView) view.findViewById(R.id.item_fresh_customer_address);
        date_view = (TextView) view.findViewById(R.id.order_fresh_order_date);
        showItems = (Button) view.findViewById(R.id.item_fresh_show_items);
        totalPrice = (TextView) view.findViewById(R.id.item_fresh_order_totalPrice);
        payment_type = (TextView) view.findViewById(R.id.item_fresh_order_payment_type);
        products_grid = (GridLayout) view.findViewById(R.id.item_fresh_products_grid);

    }
}
