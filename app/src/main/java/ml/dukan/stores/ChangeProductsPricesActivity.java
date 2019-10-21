package ml.dukan.stores;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import ml.dukan.stores.Adapters.ProductsAdapter;
import ml.dukan.stores.CustomViews.SpacesItemDecoration;
import ml.dukan.stores.Models.Product;
import ml.dukan.stores.listeners.ProductClickListener;

/**
 * Created by Khaled on 16/02/18.
 */

public class ChangeProductsPricesActivity extends SearchableActivity {

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    ChangePricesAdapter adapter;
    ArrayList<Product> searchResult = new ArrayList<>();
    ProductsAdapter searchProductsAdapter;
    boolean usingSearchAdapter = false;
    DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
    String uid;

    OnSearchListener listener = new OnSearchListener() {
        @Override
        public void onSearchAction(String query) {
            searchResult.clear();
            if (!usingSearchAdapter){
                recyclerView.setLayoutManager(new GridLayoutManager(ChangeProductsPricesActivity.this, 3));
                recyclerView.setAdapter(searchProductsAdapter);
            }
        }

        @Override
        public void onResult(ArrayList<Product> result) {
            for (final Product p : result){
                ref.child("products")
                        .child(uid)
                        .child(p.barcode)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()){
                                    Product product = dataSnapshot.getValue(Product.class);
                                    searchResult.add(product);
                                }else{
                                    p.unavailable = true;
                                    searchResult.add(p);
                                }
                                searchProductsAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                p.unavailable = true;
                                searchResult.add(p);
                                searchProductsAdapter.notifyDataSetChanged();
                            }
                        });
            }
            if (!usingSearchAdapter){
                recyclerView.setLayoutManager(new GridLayoutManager(ChangeProductsPricesActivity.this, 3));
                recyclerView.setAdapter(searchProductsAdapter);
                usingSearchAdapter = true;
            }
        }

        @Override
        public void onError(Exception e) {
            searchResult.clear();
            if (usingSearchAdapter){
                searchProductsAdapter.notifyDataSetChanged();
                recyclerView.setLayoutManager(new GridLayoutManager(ChangeProductsPricesActivity.this, 2));
                recyclerView.setAdapter(adapter);
                usingSearchAdapter = false;
            }
        }

        @Override
        public void onClear() {
            searchResult.clear();
            if (usingSearchAdapter){
                searchProductsAdapter.notifyDataSetChanged();
                recyclerView.setLayoutManager(new GridLayoutManager(ChangeProductsPricesActivity.this, 2));
                recyclerView.setAdapter(adapter);
                usingSearchAdapter = false;
            }
        }
    };
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setSearchListener(listener);
        setContentView(R.layout.recycler_view_with_search);
        ButterKnife.bind(this);
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();


        super.init();
        searchView.setSearchHint(getString(R.string.search_hint));



        String[] arr = getResources().getStringArray(R.array.categories);
        List<Pair<String, Integer>> categories = new ArrayList<>(8);
        for (String s : arr){
            categories.add(new Pair<>(s, R.drawable.ic_launcher));
        }
        adapter = new ChangePricesAdapter(this, categories);
        searchProductsAdapter = new ProductsAdapter(this, recyclerView, searchResult);
        recyclerView.addItemDecoration(new SpacesItemDecoration(1));
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);


        searchProductsAdapter.productClickListener = new ProductClickListener() {
            @Override
            public void OnClick(Product product) {
                ChangePriceDialog dialog = new ChangePriceDialog(ChangeProductsPricesActivity.this, product, adapter);
                dialog.show();
            }
        };
    }

    class ChangePricesAdapter extends RecyclerView.Adapter<ChangePricesAdapter.ViewHolder> {

        List<Pair<String, Integer>> list;
        Context context;
        public ChangePricesAdapter (Context context, List<Pair<String, Integer>> list){
            this.list = list;
            this.context = context;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.change_product_grid_item, parent, false);
            view.setOnClickListener(onClickListener);
            return new ViewHolder(view);
        }

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = recyclerView.getChildLayoutPosition(view);
                Intent intent = new Intent(context, ChangeProductsPricesBrowser.class);
                intent.putExtra("category", String.valueOf((position)));
                startActivity(intent);
            }
        };

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Pair<String, Integer> obj = list.get(position);
            holder.title.setText(obj.first);
            // holder.image.setImageResource(obj.second);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder{
            ImageView image;
            TextView title;
            public ViewHolder(View itemView) {
                super(itemView);
                image = (ImageView) itemView.findViewById(R.id.image);
                title = (TextView) itemView.findViewById(R.id.name);
            }
        }

    }
}
