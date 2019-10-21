package ml.dukan.stores.Authentication;


import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import ml.dukan.stores.ChangeProductsPricesBrowser;
import ml.dukan.stores.HelperClasses.Util;
import ml.dukan.stores.MainActivity;
import ml.dukan.stores.Models.Market;
import ml.dukan.stores.R;
import ml.dukan.stores.SplashActivity;
import ml.dukan.stores.SweetAlert.SweetAlertDialog;

import static android.app.Activity.RESULT_OK;

/**
 * Created by Khaled on 10/08/17.
 */

public class RegisterFragment extends Fragment{

    private static final int PhoneSignInCode = 5002;

    private DatabaseReference mRef = FirebaseDatabase.getInstance().getReference();
    private String user_address;
    private LatLng user_location;
    private Bitmap market_photo;

    GoogleApiClient mGoogleApiClient;
    ProgressDialog progressDialog;

    @BindView(R.id.sign_up_with_phone) TextView signUpPhone;
    EditText name_et;
    ImageView photo_iv;
    CheckBox hasVeg_cb, takesTax_cb;

    RegisterActivity.GoogleApiEventListener googleApiEventListener = new RegisterActivity.GoogleApiEventListener() {
        @Override
        public void onConnected(GoogleApiClient client) {
            mGoogleApiClient = client;
          //  googleButton.setEnabled(true);
        }

        @Override
        public void OnImagePicked(Bitmap bitmap) {
            market_photo = bitmap;
            photo_iv.setVisibility(View.VISIBLE);
            photo_iv.setImageBitmap(market_photo);
        }

        @Override
        public void onSignInEvent(Intent data, String address, LatLng location) {
            /*
            Log.e(TAG, "onSignInEvent");
            user_address = address;
            user_location  = location;

            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                Log.e(TAG, "GoogleSignInResult is success");
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                // Google Sign In failed, update UI appropriately
                Log.e(TAG, result.getStatus().toString());
                Log.e(TAG, "GoogleSignInResult no no no");

            }
            */
        }

        @Override
        public void onPhoneSignIn(int resultCode, Intent data, String address, LatLng location) {
            Log.e(TAG, "response: onPhoneSignIn");
            user_address = address;
            user_location  = location;
            IdpResponse response = IdpResponse.fromResultIntent(data);

            // Successfully signed in
            if (resultCode == RESULT_OK) {
                progressDialog = ProgressDialog.show(getActivity(), null, "Loading", true);
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
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_register, container, false);
        ButterKnife.bind(this, v);

        name_et = (EditText) v.findViewById(R.id.name);
        hasVeg_cb = (CheckBox) v.findViewById(R.id.hasVeg);
        takesTax_cb = (CheckBox) v.findViewById(R.id.takesTax);
        photo_iv = (ImageView) v.findViewById(R.id.photo_iv);

        signUpPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signUpPhone();
            }
        });

        if (getActivity() instanceof RegisterActivity){
            ((RegisterActivity) getActivity()).addGoogleApiEventListener(googleApiEventListener);
        }
        return v;
    }

    private void authenticationCompleted(String uid){
        Log.e(TAG, "Authentication completed!");
        String name = name_et.getText().toString().trim();
        boolean hasVeg = hasVeg_cb.isChecked();
        boolean takesTax = takesTax_cb.isChecked();
        String phone_number = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
        String token = FirebaseInstanceId.getInstance().getToken();
        final Market market = new Market(name, "owner_name", phone_number, token, 0.666, user_address, user_location.latitude, user_location.longitude, hasVeg, takesTax);
        if (market_photo!=null){
            uploadMarketPhoto (uid);
        }
        // for testing
      //  mRef.child("products").setValue(uid);
        mRef.child("markets").child(uid).setValue(market).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    saveInformationLocally (market);
                    if (progressDialog!=null) progressDialog.dismiss();
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.putExtra("category", "8");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                } else {
                    // todo: Show error
                }
            }
        });
    }

    private void uploadMarketPhoto (String UID){
        Log.e(TAG, "Uploading market photo!");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        market_photo.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();
        StorageReference store_storage_ref = FirebaseStorage.getInstance().getReference().child("markets/" + UID + "/market_photo.jpg");
        store_storage_ref.putBytes(data);
    }

    private void saveInformationLocally(Market market){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        SharedPreferences.Editor prefEdit = preferences.edit();
        prefEdit.putString(Util.STORE_NAME, market.name);
        prefEdit.putString(Util.STORE_OWNER_NAME, market.owner_name);
        prefEdit.putBoolean(Util.STORE_HAS_VEG, market.hasVeg);
        prefEdit.putBoolean(Util.STORE_TAKES_TAX, market.takesTax);
        prefEdit.putString(Util.STORE_PHONE_NUMBER, market.phone_number);
        prefEdit.putString(Util.STORE_DELIVERING_RADIUS, String.valueOf(market.range));
        prefEdit.putString(Util.STORE_ADDRESS, market.address);
        prefEdit.putString(Util.STORE_LAT, String.valueOf(market.lat));
        prefEdit.putString(Util.STORE_LNG, String.valueOf(market.lng));
        Log.e(TAG, market.name+", done");
        prefEdit.apply();
    }

    private boolean checkFields (){
        String name = name_et.getText().toString().trim();

        if (name.equals("")){
            return false;
        }

        return true;
    }

    private void signUpPhone (){
        if (checkFields()){
            AuthUI.IdpConfig phoneConfigWithDefaultNumber = new AuthUI.IdpConfig.PhoneBuilder()
                    .build();
            getActivity().startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setIsSmartLockEnabled(true)
                            .setAvailableProviders(Arrays.asList(phoneConfigWithDefaultNumber)).build(),
                    PhoneSignInCode);
        }

    }

    private void checkMarketExist (final String uid){
        Query query = mRef.child("markets").child(uid);
        query.keepSynced(true);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e(TAG, dataSnapshot.toString());
                if (dataSnapshot.exists()){
                    Util.signOut(getActivity().getApplicationContext());
                    alertUserAlreadyExist();
                }else{
                    checkUIDExistInUsers(uid);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void checkUIDExistInUsers (final String uid){
        Query q = mRef.child("users").child(uid);
        q.keepSynced(true);
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    Util.signOut(getActivity().getApplicationContext());
                    alertUserAlreadyExist();
                }else{
                    authenticationCompleted(uid);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void alertUserAlreadyExist(){
        if (progressDialog!=null) progressDialog.dismiss();
        new SweetAlertDialog(getActivity(), SweetAlertDialog.WARNING_TYPE)
                .setTitleText(getString(R.string.already_exist_title))
                .setConfirmText(getString(R.string.close_title))
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.dismiss();
                    }
                }).show();
    }

    private static final String TAG = "RegisterFragment";
}
