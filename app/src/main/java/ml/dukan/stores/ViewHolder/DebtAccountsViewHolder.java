package ml.dukan.stores.ViewHolder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import ml.dukan.stores.R;

/**
 * Created by Khaled on 19/08/17.
 */

public class DebtAccountsViewHolder extends RecyclerView.ViewHolder {
    public TextView name, number, address, debt_price, show_invoices, enable_disable;
    public DebtAccountsViewHolder(View itemView) {
        super(itemView);
        name = (TextView) itemView.findViewById(R.id.name);
        number = (TextView) itemView.findViewById(R.id.number);
        address = (TextView) itemView.findViewById(R.id.address);
        debt_price = (TextView) itemView.findViewById(R.id.debt_price);
        show_invoices = (TextView) itemView.findViewById(R.id.show_invoices);
        enable_disable = (TextView) itemView.findViewById(R.id.enable_disable);
    }
}