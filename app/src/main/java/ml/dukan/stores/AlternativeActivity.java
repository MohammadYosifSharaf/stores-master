package ml.dukan.stores;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.Client;
import com.algolia.search.saas.CompletionHandler;
import com.algolia.search.saas.Index;
import com.algolia.search.saas.Query;
import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.ButterKnife;
import ml.dukan.stores.Adapters.ProductsAdapter;
import ml.dukan.stores.CustomViews.SpacesItemDecoration;
import ml.dukan.stores.Models.Product;
import ml.dukan.stores.SweetAlert.SweetAlertDialog;
import ml.dukan.stores.listeners.ProductClickListener;

/**
 * Created by Khaled on 15/08/17.
 */

public class AlternativeActivity extends SearchableActivity {

    RecyclerView recyclerView;
    ProductsAdapter adapter;
    ArrayList <Product> result  = new ArrayList<>();
    String change_barcode;
    OnSearchListener listener = new OnSearchListener() {
        @Override
        public void onSearchAction(String query) {
            result.clear();
            adapter.notifyDataSetChanged();
        }

        @Override
        public void onResult(ArrayList<Product> list) {
            result.addAll(list);
            adapter.notifyDataSetChanged();
        }

        @Override
        public void onError(Exception e) {
            result.clear();
            adapter.notifyDataSetChanged();
        }

        @Override
        public void onClear() {
            result.clear();
            adapter.notifyDataSetChanged();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setSearchListener(listener);
        setContentView(R.layout.recycler_view_with_search);
        ButterKnife.bind(this);
        super.init();

        searchView.setSearchHint(getString(R.string.alternative_search_hint));

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        Bundle extras = getIntent().getExtras();
        change_barcode = extras.getString("barcode");
        adapter = new ProductsAdapter(this, recyclerView, result);
        adapter.productClickListener = productClickListener;
        recyclerView.addItemDecoration(new SpacesItemDecoration(1));
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(adapter);

    }

    StorageReference storageReference = FirebaseStorage.getInstance().getReference();
    ProductClickListener productClickListener = new ProductClickListener() {
        @Override
        public void OnClick(final Product product) {
            SweetAlertDialog dialog = new SweetAlertDialog(AlternativeActivity.this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText(getString(R.string.alternative_dialog_title))
                    .setContentText(getString(R.string.alternative_dialog_content))
                    .setConfirmText(getString(R.string.yes))
                    .setCancelText(getString(R.string.no))
                    .showAlternativeWindow(true)
                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                            sweetAlertDialog.dismiss();
                            Intent intent = new Intent();
                            intent.putExtra("name", product.name);
                            intent.putExtra("price", product.price);
                            intent.putExtra("barcode", product.barcode);
                            intent.putExtra("change_barcode", change_barcode);
                            setResult(Activity.RESULT_OK, intent);
                            finish();
                        }
                    })
                    .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                            sweetAlertDialog.dismissWithAnimation();
                        }
                    });
            dialog.show();
            Product product1 = new Product(product.name, product.barcode, product.price, 1);
            Product product2 = new Product("", change_barcode, 0, 1);
            ImageView p1 = dialog.getProduct1();
            ImageView p2 = dialog.getProduct2();

            StorageReference p1_img = storageReference.child(product1.getImagePath());
            GlideApp.with(AlternativeActivity.this)
                    .load(p1_img)
                    .placeholder(R.drawable.product_place_holder)
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .into(p1);
            StorageReference p2_img = storageReference.child(product2.getImagePath());
            GlideApp.with(AlternativeActivity.this)
                    .load(p2_img)
                    .placeholder(R.drawable.product_place_holder)
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .into(p2);
        }
    };
}
