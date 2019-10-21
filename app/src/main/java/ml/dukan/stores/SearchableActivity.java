package ml.dukan.stores;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ProgressBar;

import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.Client;
import com.algolia.search.saas.CompletionHandler;
import com.algolia.search.saas.Index;
import com.algolia.search.saas.Query;
import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.BindView;
import ml.dukan.stores.Models.Product;

/**
 * Created by Khaled on 12/05/18.
 */

public class SearchableActivity extends NetworkCheckingActivity {


    public interface OnSearchListener {
        void onSearchAction(String query);
        void onResult (ArrayList<Product> result);
        void onError (Exception e);
        void onClear();
    }
    boolean searchIsActive = false;
    @Override
    public void onBackPressed() {
        if (searchIsActive){
            searchIsActive = false;
            if (listener != null)
                listener.onClear();
            searchView.clearQuery();
        }else{
            super.onBackPressed();
        }
    }

    private OnSearchListener listener;
    @BindView(R.id.searchView)
    protected FloatingSearchView searchView;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    private Client algoliaClient;
    private Index productsIndex;
    private String category;

    public void init(){
        searchView.requestFocus();
        searchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {

            }

            @Override
            public void onSearchAction(String currentQuery) {
                if (listener != null)
                    listener.onSearchAction(currentQuery);
                progressBar.setVisibility(View.VISIBLE);
                algoliaSearchFor(currentQuery);
            }
        });

        algoliaClient = new Client("DZQOUVTI88", "01df22cc30ee9fad4389e3da7c71d062");
        productsIndex = algoliaClient.getIndex("products");
    }

    private void algoliaSearchFor (String query_str){
        searchIsActive = true;
        Query query = new Query(query_str);
        if (category != null)
            query.setFilters("category="+category);
        productsIndex.searchAsync(query, new CompletionHandler() {
            @Override
            public void requestCompleted(JSONObject jsonObject, AlgoliaException e) {
                try {
                    JSONArray array = jsonObject.getJSONArray("hits");
                    int length = array.length();
                    ArrayList<Product> list = new ArrayList<Product>();
                    for (int i = 0; i < length; ++i){
                        JSONObject obj = array.getJSONObject(i);
                        String barcode = obj.getString("barcode");
                        String name = obj.getString("name");
                        double price = obj.getDouble("price");
                        Product product = new Product(name, barcode, (float)price, 1);
                        list.add(product);
                    }
                    if (listener != null)
                        listener.onResult(list);

                } catch (JSONException | NullPointerException e1) {
                    if (listener != null)
                        listener.onError(e1);
                    e1.printStackTrace();
                }finally {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    protected void setSearchCategory (String category){
        this.category = category;
    }
    protected void setSearchCategory (int category){
        this.category = String.valueOf(category);
    }

    public void setSearchListener (OnSearchListener listener){
        this.listener = listener;
    }
}
