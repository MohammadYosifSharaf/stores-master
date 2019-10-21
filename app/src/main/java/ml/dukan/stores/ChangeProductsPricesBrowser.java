package ml.dukan.stores;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Document;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import ml.dukan.stores.Adapters.ProductsAdapter;
import ml.dukan.stores.CustomViews.SpacesItemDecoration;
import ml.dukan.stores.HelperClasses.Util;
import ml.dukan.stores.Models.Product;
import ml.dukan.stores.listeners.ProductClickListener;

import static ml.dukan.stores.HelperClasses.Util.MAX_BROWSE;

/**
 * Created by Khaled on 16/02/18.
 */

public class ChangeProductsPricesBrowser extends SearchableActivity {



    DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    ArrayList<Product> list = new ArrayList<>();
    ProductsAdapter adapter;
    boolean isLoading = false, onScrollAssigned = false;
    String uid;
    int category;

    RecyclerView.OnScrollListener onScrollListener;
    OnSearchListener listener = new OnSearchListener() {
        @Override
        public void onSearchAction(String query) {
            list.clear();
            adapter.notifyDataSetChanged();
            recyclerView.removeOnScrollListener(onScrollListener);
            onScrollAssigned = false;
        }

        @Override
        public void onResult(ArrayList<Product> result) {
            list.clear();
            for (final Product p : result){
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
            LAST_SEEN = category * MAX_BROWSE;
            adapter.notifyDataSetChanged();
            load();
        }

        @Override
        public void onClear() {
            list.clear();
            LAST_SEEN = category * MAX_BROWSE;
            adapter.notifyDataSetChanged();
            load();
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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null){
            finish();
            return;
        }

        adapter = new ProductsAdapter(this, recyclerView, list);
        recyclerView.addItemDecoration(new SpacesItemDecoration(1));
        final GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        uid = user.getUid();
        category = Integer.parseInt(getIntent().getStringExtra("category"))+1;
        super.setSearchCategory(category);

        LAST_SEEN = category * MAX_BROWSE;


        onScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int totalItemCount = layoutManager.getItemCount();
                int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                if (!isLoading && totalItemCount <= (lastVisibleItem + 5)) {
                    load();
                    isLoading = true;
                }
            }
        };

        load();

        adapter.productClickListener = new ProductClickListener() {
            @Override
            public void OnClick(Product product) {
                ChangePriceDialog dialog = new ChangePriceDialog(ChangeProductsPricesBrowser.this, product, adapter);
                dialog.show();
            }
        };
    }


    private long LAST_SEEN = 0;
    private void load (){
        progressBar.setVisibility(View.VISIBLE);
        long end = ((category+1)*MAX_BROWSE)-(MAX_BROWSE/10);
        Query q = ref.child("products")
                .child(uid)
                .orderByChild("browse")
                .startAt(LAST_SEEN)
                .endAt(end)
                .limitToFirst(50);
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot s : dataSnapshot.getChildren()){
                    Product p = s.getValue(Product.class);
                    list.add(p);
                    LAST_SEEN =  Math.max(LAST_SEEN, p.browse);
                }
                isLoading = dataSnapshot.getChildrenCount() < 25;
                adapter.notifyDataSetChanged();

               if (!onScrollAssigned && !isLoading){
                    onScrollAssigned = true;
                    recyclerView.addOnScrollListener(onScrollListener);
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }
}
