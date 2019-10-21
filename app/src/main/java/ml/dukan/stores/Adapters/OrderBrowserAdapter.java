package ml.dukan.stores.Adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ml.dukan.stores.AlternativeActivity;
import ml.dukan.stores.ChangePriceDialog;
import ml.dukan.stores.GlideApp;
import ml.dukan.stores.HelperClasses.Util;
import ml.dukan.stores.Models.Product;
import ml.dukan.stores.OrderBrowser;
import ml.dukan.stores.QuantityPicker;
import ml.dukan.stores.R;
import ml.dukan.stores.listeners.OrderChangeListener;
import ml.dukan.stores.listeners.QuantityChangeListener;

/**
 * Created by Khaled on 15/08/17.
 */

public class OrderBrowserAdapter extends RecyclerView.Adapter<OrderBrowserAdapter.ViewHolder> {
    private static final int ALTERNATIVE_CODE = 5001;


    ArrayList<Product> list;
    StorageReference storageReference;
    SparseBooleanArray deselectedItems;
    public Map<String, Product> alternatives = new HashMap<>();
    public SparseIntArray adjustedQuantity;
    Context context;

    public OrderChangeListener orderChangeListener;

    int[] adjusted_items_count = new int[4];


    FirebaseAnalytics firebaseAnalytics;

    public OrderBrowserAdapter(Context context, ArrayList<Product> list) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        this.context = context;
        this.list = list;
        deselectedItems = new SparseBooleanArray();
        adjustedQuantity = new SparseIntArray();
        storageReference = FirebaseStorage.getInstance().getReference();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_browser_product, parent, false);
        return new ViewHolder(itemView);
    }

    public void addAlternative (String changed_barcode, Product alternative){
        if (alternatives.containsKey(changed_barcode)){
            alternatives.remove(changed_barcode);
        }
        int quantity = getProductQuantity(changed_barcode);
        alternative.quantity = quantity;
        alternatives.put(changed_barcode, alternative);
        if (orderChangeListener!=null){
            orderChangeListener.OnChange();
        }
        notifyDataSetChanged();
    }

    private int getProductQuantity (String barcode){
        int size =  list.size();
        for (int i = 0; i < size; ++i){
            Product p = list.get(i);
            if (p.barcode.equals(barcode)){
                return p.quantity;
            }
        }
        return 1;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        Product p = list.get(position);;
        if (alternatives.containsKey(p.barcode)){
            p = alternatives.get(p.barcode);
        }

        holder.name.setText(p.name);
        holder.price.setText(context.getResources().getString(R.string.price_msg, p.price));
        holder.totalPrice.setText(context.getResources().getString(R.string.price_msg, p.getTotalPrice()));

        int newQ = adjustedQuantity.get(position, -1);
        if (newQ != -1) {
            holder.quantity.setText(String.valueOf(newQ));
        } else {
            holder.quantity.setText(String.valueOf(p.quantity));
        }
        boolean deselected = deselectedItems.get(position);

        StorageReference image2_ref = storageReference.child(p.getImagePath());
        GlideApp.with(context)
                .load(image2_ref)
                .into(holder.imageView);

        if (deselected) {
            if (holder.itemView!=null)
                holder.itemView.setBackgroundColor(Color.parseColor("#30ff0000"));
            holder.totalPrice.setTextColor(Color.parseColor("#8f8f8f"));
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            holder.totalPrice.setTextColor(Color.parseColor("#f51a3f"));
        }
        holder.itemView.setTag(position);
        holder.quantity.setTag(position);
        holder.alternativeIV.setTag(position);
        holder.changePrice.setTag(position);
    }


    View.OnClickListener itemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int position = (int) view.getTag();
            boolean isDeselected = deselectedItems.get(position, false);
            if (isDeselected)
                deselectedItems.delete(position);
            else
                deselectedItems.put(position, true);

            if (orderChangeListener!=null){
                orderChangeListener.OnChange();
            }
            notifyDataSetChanged();
        }
    };

    QuantityChangeListener quantityChangeListener = new QuantityChangeListener() {
        @Override
        public void onChange(int position, int quantity) {
            Product p = list.get(position);
            adjustedQuantity.delete(position);
            if (p.quantity != quantity)
                adjustedQuantity.put(position, quantity);
            if (orderChangeListener!=null){
                orderChangeListener.OnChange();
            }
            notifyDataSetChanged();
        }
    };

    View.OnClickListener quantityClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            firebaseAnalytics.logEvent("attempt_quantity_change", null);
            int position = (int) view.getTag();
            Product p = list.get(position);
            int newQ = adjustedQuantity.get(position, -1);
            new QuantityPicker(context, position, newQ, p.quantity)
                    .setQuantityChangeListener(quantityChangeListener)
                    .show();
        }
    };


    public boolean isHealthy() {
        return (deselectedItems.size() == 0
                && adjustedQuantity.size() == 0
                && alternatives.size() == 0);
    }


    View.OnClickListener alternativeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            firebaseAnalytics.logEvent("attempt_alternative", null);
            int position = (int) view.getTag();
            Product p = list.get(position);
            Intent intent = new Intent (context, AlternativeActivity.class);
            intent.putExtra("barcode", p.barcode);
            ((Activity)context).startActivityForResult(intent, ALTERNATIVE_CODE);
        }
    };

    View.OnClickListener changePriceClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            firebaseAnalytics.logEvent("attempt_changePrice", null);
            final int position = (int) view.getTag();
            final Product p = list.get(position);
            ChangePriceDialog dialog = new ChangePriceDialog(context, p, OrderBrowserAdapter.this);
            dialog.setOnChangeCallback(new ChangePriceDialog.ChangeCallback() {
                @Override
                public void onChange(boolean unavailable, float price, boolean priceChanged) {
                    if (unavailable){
                        deselectedItems.put(position, true);
                    }else {
                        deselectedItems.delete(position);
                    }

                    if (priceChanged){
                        p.price = price;
                    }
                    notifyDataSetChanged();
                    if (orderChangeListener!=null){
                        orderChangeListener.OnChange();
                    }

                }
            });
            dialog.show();
        }
    };

    public SparseBooleanArray getDeselectedItems() {
        return deselectedItems;
    }

    public SparseIntArray getAdjustedQuantity() {
        return adjustedQuantity;
    }

    public ArrayList<Product> getFinalProducts() {
        ArrayList<Product> adj = new ArrayList<>();
        for (int i = 0; i < list.size(); ++i) {
            Product product = list.get(i);
            boolean deselected = deselectedItems.get(i);
            if (deselected){
                product.adjustment = Util.DESELECTED;
                countAdjustedItems(Util.DESELECTED);
                adj.add(product);
                continue;
            }
            boolean hasAlternative = alternatives.containsKey(product.barcode);
            if (hasAlternative){
                String subs_barcode = product.barcode;
                String subs_name = product.name;
                product = alternatives.get(product.barcode);
                product.substituted_barcode = subs_barcode;
                product.substituted_name = subs_name;
            }

            int new_quantity = adjustedQuantity.get(i, -1);
            boolean changedQuantity = new_quantity != -1;
            if (changedQuantity) {
                product.quantity = new_quantity;
            }

            product.adjustment = (hasAlternative && changedQuantity) ? Util.QUANTITY_ALTERNATIVE_CHANGE
                                    : (hasAlternative ? Util.ALTERNATIVE_CHANGE :
                    (changedQuantity ? Util.QUANTITY_CHANGE : 0) );

            countAdjustedItems (product.adjustment);
            adj.add(product);
        }
        return adj;
    }

    private void countAdjustedItems(int type){
        switch (type){
            case Util.DESELECTED:
                adjusted_items_count[0]++;
                break;
            case Util.QUANTITY_CHANGE:
                adjusted_items_count[1]++;
                break;
            case Util.ALTERNATIVE_CHANGE:
                adjusted_items_count[2]++;
                break;
            case Util.QUANTITY_ALTERNATIVE_CHANGE:
                adjusted_items_count[3]++;
                break;
        }
    }

    public int[] getAdjustedItemsCount (){
        return adjusted_items_count;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, price, totalPrice;
        ImageView imageView, alternativeIV, changePrice;
        TextView quantity;
        View itemView;

        public ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            imageView = (ImageView) itemView.findViewById(R.id.browser_product_item_imageView);
            name = (TextView) itemView.findViewById(R.id.browser_product_item_name);
            price = (TextView) itemView.findViewById(R.id.browser_product_item_price);
            totalPrice = (TextView) itemView.findViewById(R.id.browser_product_item_total_price);
            alternativeIV= (ImageView) itemView.findViewById(R.id.product_item_alternative);
            changePrice = (ImageView) itemView.findViewById(R.id.product_item_price);
            quantity = (TextView) itemView.findViewById(R.id.quantity);
            itemView.setOnClickListener(itemClickListener);
            quantity.setOnClickListener(quantityClickListener);
            alternativeIV.setOnClickListener(alternativeClickListener);
            changePrice.setOnClickListener(changePriceClickListener);
        }
    }
}