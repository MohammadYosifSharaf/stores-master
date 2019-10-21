package ml.dukan.stores;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import ml.dukan.stores.Adapters.OrderBrowserAdapter;
import ml.dukan.stores.Adapters.ProductsAdapter;
import ml.dukan.stores.Models.Product;

/**
 * Created by Khaled on 16/02/18.
 */

public class ChangePriceDialog extends Dialog {


    public interface ChangeCallback {
        void onChange (boolean unavailable, float price, boolean priceChanged);
    }

    DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
    Product product;
    Context context;
    RecyclerView.Adapter adapter;
    ChangeCallback callback;
    public ChangePriceDialog(@NonNull Context context, Product product, RecyclerView.Adapter adapter) {
        super(context);
        this.context = context;
        this.product = product;
        this.adapter = adapter;
    }


    public void setOnChangeCallback (ChangeCallback callback){
        this.callback = callback;
    }




    ImageView imageView;
    TextView nameView;
    EditText priceView;
    Button confirm, cancel;
    CheckBox notExist;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_price_dialog);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;


        imageView = (ImageView) findViewById(R.id.image);
        nameView = (TextView) findViewById(R.id.name);
        priceView = (EditText) findViewById(R.id.price);
        confirm = (Button) findViewById(R.id.confirm_button);
        cancel = (Button) findViewById(R.id.cancel_button);
        notExist = (CheckBox) findViewById(R.id.notExistCB);


        nameView.setText(product.name);
        priceView.setText(String.valueOf(product.price));
        notExist.setChecked(product.unavailable);

        StorageReference p_img = FirebaseStorage.getInstance().getReference().child(product.getImagePath());
        GlideApp.with(context)
                .load(p_img)
                .placeholder(R.drawable.product_place_holder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView);


        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                final float price = Float.parseFloat(priceView.getText().toString());
                if (callback != null)
                    callback.onChange(notExist.isChecked(), price, price!=product.price);

                Map<String, Object> updates = new HashMap<>();
                updates.put("price", price);
                updates.put("unavailable", notExist.isChecked());
                updates.put("seen", true);
                ref.child("products").child(uid).child(product.barcode)
                        .updateChildren(updates).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            adapter.notifyDataSetChanged();
                            dismiss();
                        }
                    }
                });
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

    }
}
