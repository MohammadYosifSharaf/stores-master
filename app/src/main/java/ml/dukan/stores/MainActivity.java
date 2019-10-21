package ml.dukan.stores;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.zhanghai.android.materialratingbar.MaterialRatingBar;
import ml.dukan.stores.Adapters.MainDrawerAdapter;
import ml.dukan.stores.CustomViews.TextViewSquared;
import ml.dukan.stores.HelperClasses.DatabaseAdapter;
import ml.dukan.stores.HelperClasses.Util;
import ml.dukan.stores.Models.MainDrawerItem;
import ml.dukan.stores.Models.User;
import ml.dukan.stores.SweetAlert.SweetAlertDialog;
import ml.dukan.stores.viewpager_fragments.FreshOrders;
import ml.dukan.stores.viewpager_fragments.ProcessingOrders;

/**
 * Created by khaled on 18/06/17.
 */

public class MainActivity extends NetworkCheckingActivity {
    DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
    DatabaseReference orders_ref, issues_ref;
    DatabaseAdapter helper;
    FirebaseAnalytics firebaseAnalytics;

    MyPagerAdapter pagerAdapter;
    Toolbar toolbar;
    SharedPreferences preferences;
    Drawable gray_badge, white_badge;

    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.listView)
    ListView drawerListView;
     ImageView marketPhoto;
     TextView marketName;
     MaterialRatingBar ratingBar;
     TextView ratingTV;
     TextView marketDeliveryDuration;
    ActionBarDrawerToggle mDrawerToggle;
    @BindView(R.id.tab_layout) TabLayout tabLayout;
    @BindView(R.id.main_pager)  ViewPager pager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);
        ButterKnife.bind(this);


        firebaseAnalytics = FirebaseAnalytics.getInstance(getApplicationContext());

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        orders_ref = ref.child("orders").child("markets").child(uid);
        issues_ref = ref.child("market_issues").child(uid);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);


        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setUpSideMenu();

        String store_name = preferences.getString(Util.STORE_NAME, getString(R.string.app_name));
        getSupportActionBar().setTitle(store_name);

        gray_badge = ResourcesCompat.getDrawable(getResources(), R.drawable.gray_badge, null);
        white_badge = ResourcesCompat.getDrawable(getResources(), R.drawable.rounded_white_layout, null);

    //    tabLayout.addTab(tabLayout.newTab().setCustomView(getTabView(getString(R.string.tab_delivered))), false);
        tabLayout.addTab(tabLayout.newTab().setCustomView(getTabView(getString(R.string.tab_processing))), false);
        tabLayout.addTab(tabLayout.newTab().setCustomView(getTabView(getString(R.string.tab_fresh))), true);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                pager.setCurrentItem(tab.getPosition());
                toggleTabsBoldness(tab.getPosition());

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });



        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                tabLayout.setScrollPosition(position, positionOffset, false);
            }

            @Override
            public void onPageSelected(int position) {
                tabLayout.getTabAt(position).select();
                toggleTabsBoldness(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


        pager.setOffscreenPageLimit(2);

        helper = new DatabaseAdapter(getApplicationContext());

        pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(pagerAdapter);
        pager.setCurrentItem(1);



    }

    private void setUpSideMenu(){
        View header = getLayoutInflater().inflate(R.layout.profile, null, false);
        View footer = getLayoutInflater().inflate(R.layout.footer_statistics, null, false);
        drawerListView.addHeaderView(header);
        drawerListView.addFooterView(footer);
        setUpStatisticFooter(footer);
        setUpProfile (header);
        List<MainDrawerItem> list = new ArrayList<>();
        list.add(new MainDrawerItem(getString(R.string.change_prices_title), R.drawable.change_items));
        list.add(new MainDrawerItem(getString(R.string.uavailable_products), R.drawable.out_of_stock));
        list.add(new MainDrawerItem(getString(R.string.menu_debt_management), R.drawable.menu_records));
        list.add(new MainDrawerItem(getString(R.string.menu_debt_acc_management), R.drawable.accounts));
        list.add(new MainDrawerItem(getString(R.string.feed_back_title), R.drawable.feedback));
        list.add(new MainDrawerItem(getString(R.string.menu_sign_out), R.drawable.menu_sign_out));
        MainDrawerAdapter adapter = new MainDrawerAdapter(this, R.layout.item_main_drawer, list);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
            }
        };
        drawerListView.setAdapter(adapter);
        mDrawerToggle.syncState();
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        drawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i){
                    case 6:
                        signOut(null);
                        break;
                    case 5:
                        Util.showFeedbackAlert(MainActivity.this);

                        break;
                    case 4:
                        startActivity(new Intent(MainActivity.this, DebtAccountActivity.class));
                        break;
                    case 3:
                        startActivity(new Intent(MainActivity.this, DebtAccountingActivity.class));
                        break;
                    case 2:
                        startActivity(new Intent(MainActivity.this, UnavaliableProductsActivity.class));
                        break;
                    case 1:
                        startActivity(new Intent(MainActivity.this, ChangeProductsPricesActivity.class));
                }
                mDrawerLayout.closeDrawer(Gravity.START);
            }
        });
    }
    
    private void setUpProfile (View header){
        String store_name = preferences.getString(Util.STORE_NAME, null);

        marketPhoto = (ImageView) header.findViewById(R.id.imageView);;
        marketName = (TextView) header.findViewById(R.id.name);
        ratingBar = (MaterialRatingBar) header.findViewById(R.id.ratingBar);
        ratingTV = (TextView) header.findViewById(R.id.ratingTV);
        marketDeliveryDuration = (TextView) header.findViewById(R.id.delivery_duration);

        marketName.setText(store_name);
        getRateAndDeliveryTime();
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        GlideApp.with(this)
                .load(storageReference.child("markets/"+uid+"/market_photo.jpg"))
                .error(R.drawable.profile_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .into(marketPhoto);
    }

    private void setUpStatisticFooter (View footer){
        final TextView stat = (TextView) footer.findViewById(R.id.stat_info);
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        ref.child("statistics").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try{
                    long delivered_orders = dataSnapshot.child("delivered_orders").getValue(Long.class);
                    long canceled_orders = dataSnapshot.child("canceled_orders").getValue(Long.class);
                    float total_income = dataSnapshot.child("total_income").getValue(Float.class);
                    long total = delivered_orders+canceled_orders;
                    String str = getString(R.string.statistics, delivered_orders, canceled_orders, total, total_income);
                    stat.setText(str);
                } catch (NullPointerException e){
                    String str = getString(R.string.statistics, 0, 0, 0, 0.0f);
                    stat.setText(str);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void signOut (View v){
        Util.signOut(getApplicationContext());
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void  getRateAndDeliveryTime (){
        Query q = ref.child("markets").child(uid);
        q.keepSynced(true);
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                DataSnapshot cumulativeRatingObj = dataSnapshot.child("cumulativeRating");
                DataSnapshot numRatingsObj = dataSnapshot.child("numRatings");

                DataSnapshot cumulativeDurationObj = dataSnapshot.child("cumulativeDuration");
                DataSnapshot numDeliveryObj = dataSnapshot.child("numDelivery");

                if (!(cumulativeRatingObj.getValue() == null
                        && numRatingsObj.getValue() ==null)){
                    float cumulativeRating = cumulativeRatingObj.getValue(Float.class);
                    int numRatings = numRatingsObj.getValue(Integer.class);
                    float rate = Util.calculateAverage(cumulativeRating, numRatings);
                    ratingBar.setRating(rate);
                    if (rate == 0){
                        ratingTV.setText("0.0");
                        return;
                    }
                    ratingTV.setText(String.format("%.2f",rate));
                }

                if (!(cumulativeDurationObj.getValue() == null
                        && numDeliveryObj.getValue() ==null)){
                    long cumulativeDuration = cumulativeDurationObj.getValue(Long.class);
                    int numDelivery = numDeliveryObj.getValue(Integer.class);
                    long duration =(long)Util.calculateAverage(cumulativeDuration, numDelivery);
                    if (duration != 0) {
                        duration = TimeUnit.MILLISECONDS.toMinutes(duration);
                        marketDeliveryDuration.setText(Util.toHTML(getString(R.string.delivery_duration_title, duration)));
                    }else{
                        marketDeliveryDuration.setText(Util.toHTML(getString(R.string.no_delivery_duration_title)));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void toggleTabsBoldness(int bold_pos){
        for (int i = 0; i < 2; ++i){
            View v = tabLayout.getTabAt(i).getCustomView();
            TextViewSquared badge = (TextViewSquared) v.findViewById(R.id.custom_tab_badge);
            TextView titleTV = (TextView) v.findViewById(R.id.custom_tab_title);
            badge.setBackground(white_badge);
            badge.setAlpha(bold_pos == i ? 1f : 0.6f);
            titleTV.setAlpha(bold_pos == i ? 1f : 0.6f);
            titleTV.setTypeface(null, bold_pos == i ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    public View getTabView(String title) {

        View v = LayoutInflater.from(MainActivity.this).inflate(R.layout.custom_tab, null);
        TextView titleTV = (TextView) v.findViewById(R.id.custom_tab_title);
        titleTV.setText(title);
        return v;
    }

    public void updateBadge(int position, int n){
        View v = tabLayout.getTabAt(position).getCustomView();
        TextViewSquared badge = (TextViewSquared) v.findViewById(R.id.custom_tab_badge);
        if (n > 0){
            badge.setText(String.valueOf(n));


        badge.setVisibility(View.VISIBLE);
        }else{
            badge.setVisibility(View.GONE);
        }
    }


    String uid;
    @Override
    protected void onStart() {
        super.onStart();
        issues_ref.addChildEventListener(issuesEventListener);
        ref.child("debt_accounts/markets").child(uid)
                .orderByChild("status").equalTo("DEBT_ACC_ISSUED").addChildEventListener(debtAccountsChild);
    }

    @Override
    protected void onStop() {
        super.onStop();
       // orders_ref.removeEventListener(valueEventListener);
       // orders_ref.removeEventListener(childEventListener);
        issues_ref.removeEventListener(issuesEventListener);
        ref.child("debt_accounts/markets").child(uid).removeEventListener(debtAccountsChild);
    }

    ChildEventListener debtAccountsChild = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            final String uid = dataSnapshot.getKey();
            // JOING User profile
            ref.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()){
                        User shopper = dataSnapshot.getValue(User.class);
                        shopper.uid = uid;
                        alertIssueDebtAccount (uid, shopper);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    ChildEventListener issuesEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            try {
                String order_key = dataSnapshot.getKey();
                String name = dataSnapshot.child("name").getValue(String.class);
                String issue = dataSnapshot.child("issue").getValue(String.class);
                switch (issue) {
                    case "ping":
                        //// TODO: Show alert 
                        break;
                    case "not_delivered":

                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                dataSnapshot.getRef().removeValue();
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    private void alertIssueDebtAccount (final String shopper_uid, User user){
        new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText(getString(R.string.request_debt_account_title))
                .setContentText(getString(R.string.request_debt_account_content, user.name, user.address, user.number))
                .setConfirmText(getString(R.string.accept_account))
                .setCancelText(getString(R.string.reject_account))
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.dismissWithAnimation();
                        Bundle bundle = new Bundle();
                        bundle.putBoolean("accepted", true);
                        firebaseAnalytics.logEvent("debt_request_response", null);
                        respondDebtAccount(shopper_uid, true);
                    }
                })
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.dismissWithAnimation();
                        Bundle bundle = new Bundle();
                        bundle.putBoolean("accepted", false);
                        firebaseAnalytics.logEvent("debt_request_response", null);
                        respondDebtAccount(shopper_uid, false);
                    }
                }).show();

    }

    private void respondDebtAccount (String shopper_uid, boolean accept){
        Map<String, Object> map = new HashMap<>();
        map.put(String.format("debt_accounts/customers/%s/%s/status", shopper_uid, uid), accept ? "DEBT_ACC_ACCEPTED" : "DEBT_ACC_REJECTED");
        map.put(String.format("debt_accounts/markets/%s/%s/status", uid, shopper_uid), accept ? "DEBT_ACC_ACCEPTED" : "DEBT_ACC_REJECTED");
        if (accept){
            Map<String, Object> summary = new HashMap<>();
            summary.put("current_invoice", -1);
            summary.put("total_price", 0);
            summary.put("enabled", true);
            map.put(String.format("debt_summary/customers/%s/%s", shopper_uid, uid), summary);
            map.put(String.format("debt_summary/markets/%s/%s", uid, shopper_uid), summary);
        }
        ref.updateChildren(map);
    }

    public static class MyPagerAdapter extends FragmentPagerAdapter {
        private static int NUM_ITEMS = 2;
        FreshOrders freshOrders;
        ProcessingOrders processingOrders;
        public MyPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
            freshOrders = new FreshOrders();
            processingOrders = new ProcessingOrders();
        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: // Fragment # 0 - This will show FirstFragment
                    return processingOrders;
                case 1:
                    return freshOrders;
                default:
                    return null;
            }
        }

    }

    @Override
    public void onBackPressed() {
        // If menu is opened, close it
        if (mDrawerLayout.isDrawerOpen(Gravity.START)) {
            mDrawerLayout.closeDrawer(Gravity.START);
        }else{
            // Otherwise destroy activity.
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // On click on hamburger icon, open side menu
                if (mDrawerLayout.isDrawerOpen(Gravity.START)) {
                    mDrawerLayout.closeDrawer(Gravity.START);
                } else {
                    mDrawerLayout.openDrawer(Gravity.START);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
