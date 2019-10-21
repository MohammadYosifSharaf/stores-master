package ml.dukan.stores.ViewHolder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import ml.dukan.stores.R;

/**
 * Created by Khaled on 18/09/17.
 */

public class ProcessingOrderViewHolder extends RecyclerView.ViewHolder{
    public TextView customer_name, date, customer_address, total_price;
    public Button delivered_btn;
    public ProcessingOrderViewHolder(View itemView) {
        super(itemView);
        customer_name = (TextView) itemView.findViewById(R.id.item_processing_customer_name);
        delivered_btn = (Button) itemView.findViewById(R.id.item_processing_delivered);
        customer_address = (TextView) itemView.findViewById(R.id.item_processing_customer_address);
        date = (TextView) itemView.findViewById(R.id.item_processing_date);
        total_price = (TextView) itemView.findViewById(R.id.item_processing_totalPrice);
    }
}
