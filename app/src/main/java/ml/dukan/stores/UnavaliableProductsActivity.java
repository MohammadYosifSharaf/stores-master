package ml.dukan.stores;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import com.algolia.search.saas.Client;
import com.algolia.search.saas.Index;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ml.dukan.stores.Adapters.ProductsAdapter;
import ml.dukan.stores.CustomViews.SpacesItemDecoration;
import ml.dukan.stores.Models.Product;
import ml.dukan.stores.listeners.ProductClickListener;

/**
 * Created by Khaled on 23/02/18.
 */

public class UnavaliableProductsActivity extends SearchableActivity {


    ArrayList<Product> list = new ArrayList<>();
    ProductsAdapter adapter;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    String uid;
    DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
    OnSearchListener listener = new OnSearchListener() {
        @Override
        public void onSearchAction(String query) {
            list.clear();
            adapter.notifyDataSetChanged();
        }

        @Override
        public void onResult(ArrayList<Product> result) {
            for (final Product p : list){
                ref.child("products")
                        .child(uid)
                        .child(p.barcode)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()){
                                    Product product = dataSnapshot.getValue(Product.class);
                                    list.add(product);
                                }else{
                                    p.unavailable = true;
                                    list.add(p);
                                }
                                adapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                p.unavailable = true;
                                list.add(p);
                                adapter.notifyDataSetChanged();
                            }
                        });
            }
        }

        @Override
        public void onError(Exception e) {
            list.clear();
            adapter.notifyDataSetChanged();
        }

        @Override
        public void onClear() {
            list.clear();
            adapter.notifyDataSetChanged();
            loadUnavailableProducts();
        }
    };
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setSearchListener(listener);
        setContentView(R.layout.recycler_view_with_search);
        ButterKnife.bind(this);
        super.init();
        searchView.setSearchHint(getString(R.string.search_hint));

        adapter = new ProductsAdapter(getApplicationContext(), recyclerView, list);
        recyclerView.addItemDecoration(new SpacesItemDecoration(1));
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(adapter);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadUnavailableProducts();


        adapter.productClickListener = new ProductClickListener() {
            @Override
            public void OnClick(Product product) {
                ChangePriceDialog dialog = new ChangePriceDialog(UnavaliableProductsActivity.this, product, adapter);
                dialog.show();
            }
        };

    }

    private void loadUnavailableProducts (){

        Query q = ref.child("products")
                .child(uid)
                .orderByChild("unavailable")
                .equalTo(true);

        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                list.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    try {
                        Product product = snapshot.getValue(Product.class);
                        list.add(product);
                    } catch (Exception e){

                    }

                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
