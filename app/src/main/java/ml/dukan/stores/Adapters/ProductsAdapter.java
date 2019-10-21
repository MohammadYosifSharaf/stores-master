package ml.dukan.stores.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import ml.dukan.stores.GlideApp;
import ml.dukan.stores.Models.Product;
import ml.dukan.stores.R;
import ml.dukan.stores.listeners.ProductClickListener;

/**
 * Created by Khaled on 13/08/17.
 */

public class ProductsAdapter extends RecyclerView.Adapter <RecyclerView.ViewHolder> {
    Context context;
    List<Product> list = new ArrayList<>();
    RecyclerView recyclerView;
    ColorMatrixColorFilter greyFilter;
    public ProductClickListener productClickListener;

    static StorageReference storageReference = FirebaseStorage.getInstance().getReference();;
    public ProductsAdapter(Context context, RecyclerView recyclerView, List<Product> data){
        this.context = context;
        this.recyclerView = recyclerView;
        this.list = data;
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        greyFilter = new ColorMatrixColorFilter(colorMatrix);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.product_cell, parent, false);
        itemView.setOnClickListener(productOnClickList);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder aholder, int position) {
        ViewHolder holder = (ViewHolder) aholder;
        Product product = list.get(position);
        holder.name.setText(product.name);
        holder.price.setText(context.getResources().getString(R.string.price_msg, product.price));
        StorageReference image_ref = storageReference.child(product.getImagePath());
        GlideApp.with(context)
                .load(image_ref)
                .placeholder(R.drawable.product_place_holder)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .into(holder.image);
        if (product.unavailable) {
            holder.image.setColorFilter(greyFilter);
            holder.price.setTextColor(Color.BLACK);
        }else{
            holder.image.clearColorFilter();
            holder.price.setTextColor(Color.parseColor("#f51a3f"));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        public TextView name, price;
        public ImageView image;
        public ViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.product_cell_name);
            price = (TextView) itemView.findViewById(R.id.product_cell_price);
            image = (ImageView) itemView.findViewById(R.id.product_cell_image);
        }
    }


    public void remove (Product t){

        for (int i = 0; i < list.size(); ++i){
            Product p = list.get(i);
            if (p.barcode.equals(t.barcode)){
                list.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }

    }

    public View.OnClickListener productOnClickList = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int position = recyclerView.getChildLayoutPosition(view);
            Product product = list.get(position);
            if (productClickListener!=null) {
                productClickListener.OnClick(product);
            }
        }
    };

}
