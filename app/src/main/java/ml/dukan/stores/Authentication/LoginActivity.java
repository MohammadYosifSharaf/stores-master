package ml.dukan.stores.Authentication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import ml.dukan.stores.ChangeProductsPricesBrowser;
import ml.dukan.stores.HelperClasses.Util;
import ml.dukan.stores.MainActivity;
import ml.dukan.stores.Models.Market;
import ml.dukan.stores.NetworkCheckingActivity;
import ml.dukan.stores.R;
import ml.dukan.stores.SplashActivity;
import ml.dukan.stores.SweetAlert.SweetAlertDialog;

/**
 * Created by khaled on 29/01/17.
 */

public class LoginActivity extends NetworkCheckingActivity {
    private static final String TAG = "LoginActivity";

    DatabaseReference mRef = FirebaseDatabase.getInstance().getReference();
    ProgressDialog progressDialog;

    @BindView(R.id.sign_up_with_phone)
    TextView signUpPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_login);
        ButterKnife.bind(this);

        signUpPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signUpPhone();
            }
        });
    }

    private static final int PhoneSignInCode = 5002;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e(TAG, "onActivityResult");
        if (requestCode == PhoneSignInCode){
            IdpResponse response = IdpResponse.fromResultIntent(data);

            // Successfully signed in
            if (resultCode == RESULT_OK) {
                progressDialog = ProgressDialog.show(LoginActivity.this, null, "Loading", true);
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                checkMarketExist(uid);
            } else {
                // Sign in failed
                if (response == null) {
                    // User pressed back button
                    return;
                }

                if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                    return;
                }

                if (response.getError().getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                    return;
                }
            }
        }
    }

    private void signUpPhone (){
        AuthUI.IdpConfig phoneConfigWithDefaultNumber = new AuthUI.IdpConfig.PhoneBuilder()
                .build();
        startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setIsSmartLockEnabled(true)
                            .setAvailableProviders(Arrays.asList(phoneConfigWithDefaultNumber)).build(),
                    PhoneSignInCode);
    }

    private void deleteAccount(){
        Log.e(TAG, "Deleting user...");
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {finish(); return;}
        String uid = user.getUid();
        Query query = mRef.child("users").child(uid);
        query.keepSynced(true);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (progressDialog!=null) progressDialog.dismiss();
                if (dataSnapshot.exists()){
                    Util.signOut(getApplicationContext());
                    alertAccountNotExist ();
                }else{
                    user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                Log.e(TAG, "User deleted successful");
                                alertAccountNotExist ();
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    };
    private void alertAccountNotExist(){
        new SweetAlertDialog(LoginActivity.this, SweetAlertDialog.ERROR_TYPE)
                .setTitleText(getString(R.string.market_not_exit_title))
                .setContentText(getString(R.string.market_not_exit_content))
                .setConfirmText(getString(R.string.yes))
                .setCancelText(getString(R.string.no))
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
                        sweetAlertDialog.dismiss();
                    }
                })
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.dismiss();
                    }
                }).show();
    }

    private void checkMarketExist (final String uid){
        Log.e(TAG, "Checking user with id: "+uid);
        Query query = mRef.child("markets").child(uid);
        query.keepSynced(true);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e(TAG, dataSnapshot.toString());
                if (dataSnapshot.exists() == false){
                    Log.e(TAG, "User does not exist");
                    deleteAccount();
                }else{
                    Market market = dataSnapshot.getValue(Market.class);
                    if (market == null) {finish(); return;}
                    market.uid = uid;
                    Log.e(TAG, "User exist!");
                    Log.e(TAG, "Getting user info");
                    enter(market);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void enter (final Market market){
        String token = FirebaseInstanceId.getInstance().getToken();
        if (token != null)
            mRef.child("markets").child(market.uid).child("tokens").child(token).setValue(true);
        saveInformationLocally(market);
        if (progressDialog!=null) progressDialog.dismiss();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void saveInformationLocally(Market market){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor prefEdit = preferences.edit();
        prefEdit.putString(Util.STORE_NAME, market.name);
        prefEdit.putString(Util.STORE_OWNER_NAME, market.owner_name);
        prefEdit.putString(Util.STORE_PHONE_NUMBER, market.phone_number);
        prefEdit.putString(Util.STORE_DELIVERING_RADIUS, String.valueOf(market.range));
        prefEdit.putString(Util.STORE_ADDRESS, market.address);
        prefEdit.putString(Util.STORE_LAT, String.valueOf(market.lat));
        prefEdit.putString(Util.STORE_LNG, String.valueOf(market.lng));
        Log.e(TAG, market.name+", done");
        prefEdit.apply();
    }
    @Override
    public void onStart() {
        super.onStart();
    }


    public void register (View v){

        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }
}
