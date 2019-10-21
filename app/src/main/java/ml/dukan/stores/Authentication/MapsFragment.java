package ml.dukan.stores.Authentication;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import ml.dukan.stores.R;
import ml.dukan.stores.SweetAlert.SweetAlertDialog;


public class MapsFragment extends Fragment implements OnMapReadyCallback, LocationListener{

    Location location;
    Marker mMarker;
    boolean manuallyDragged = false;

    @BindView(R.id.maps_address_tv) TextView address_tv;
    @BindView(R.id.maps_confirm) TextView confirm_btn;
    @BindView(R.id.maps_add_hint) TextView add_hint_btn;
    @BindView(R.id.maps_progress_bar) ProgressBar progressBar;
    @BindView(R.id.mapView) MapView mapView;

    LocationRequest mLocationRequest;
    private GoogleMap mMap;

    SharedPreferences preferences;

    RegisterActivity.OnStoreChosenListener OnStoreChosenListener;
    public void setStoreChosenListener (RegisterActivity.OnStoreChosenListener listener){
        OnStoreChosenListener = listener;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_maps, container, false);
        ButterKnife.bind(this, view);
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());



        confirm_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirm();
            }
        });

        view.findViewById(R.id.maps_add_hint).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showHintDialog();
            }
        });

        mapView.getMapAsync(this);

        mapView.onCreate(getArguments());

        if (getActivity() instanceof RegisterActivity){
            ((RegisterActivity) getActivity()).addGoogleApiEventListener(googleApiEventListener);
        }

        return view;
    }


    private void showHintDialog () {
        new SweetAlertDialog(getActivity(), SweetAlertDialog.CUSTOM_IMAGE_TYPE)
                .setCustomImage(R.drawable.map_icon)
                .setTitleText(getString(R.string.add_hint))
                .showEditText(true, getString(R.string.add_hint))
                .setEditTextContent(address != null ? address.trim() : "")
                .setConfirmText(getString(R.string.add))
                .setCancelText(getString(R.string.cancel_title))
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        String text =sweetAlertDialog.getText();
                        manuallyDragged = true;
                        if (text.trim().length()!=0){
                            address = text;
                            address_tv.setText(address);
                        }
                        sweetAlertDialog.dismissWithAnimation();
                    }
                })
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        sweetAlertDialog.dismissWithAnimation();
                    }
                })
                .show();
    }


    RegisterActivity.GoogleApiEventListener googleApiEventListener = new RegisterActivity.GoogleApiEventListener() {
        @Override
        public void onConnected(GoogleApiClient client) {
            Location last_loco = LocationServices.FusedLocationApi.getLastLocation(client);
            if (last_loco != null) {
                location = last_loco;
                LatLng last_latlng = new LatLng(last_loco.getLatitude(), last_loco.getLongitude());
                addMarker(last_latlng, true);
            }
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(5000);
         //   mLocationRequest.setInterval(60 * 1000);\
            mLocationRequest.setFastestInterval(1000);
           // mLocationRequest.setFastestInterval(3500);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                LocationServices.FusedLocationApi.requestLocationUpdates(client, mLocationRequest, MapsFragment.this);
            }
            LocationSettingsRequest.Builder locationSettingsRequestBuilder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
            PendingResult<LocationSettingsResult> result =
                    LocationServices.SettingsApi.checkLocationSettings(client, locationSettingsRequestBuilder.build());
            result.setResultCallback(mResultCallbackFromSettings);
        }

        @Override
        public void OnImagePicked(Bitmap bitmap) {
            // nothing to do here
        }

        @Override
        public void onSignInEvent(Intent data, String address, LatLng location) {
            // nothing to do here
        }

        @Override
        public void onPhoneSignIn(int resultCode, Intent data, String address, LatLng location) {
            // nothing to do here
        }
    };

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setMapToolbarEnabled(false);


        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                manuallyDragged = true;
                location = new Location("provider");
                location.setLatitude(latLng.latitude);
                location.setLongitude(latLng.longitude);
                addMarker(latLng, false);
            }
        });


    }


    String address;
    private void getMyAddress(LatLng latLng){
        confirm_btn.setEnabled(false);
        add_hint_btn.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        address_tv.setVisibility(View.GONE);
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        String url =String.format(Locale.US, "https://maps.googleapis.com/maps/api/geocode/json?language=ar&latlng=%f,%f&key=%s",
                latLng.latitude, latLng.longitude, "AIzaSyD6eJbngaXU6uoSoGxAwT9a3wjIvlNTJFw");

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            System.out.println(jsonObject.toString());
                            StringBuilder s = new StringBuilder(jsonObject.getJSONArray("results").getJSONObject(0).getString("formatted_address"));
                            for (int i = 0; i < s.length();){
                                if (Character.isDigit(s.charAt(i))){
                                    s.deleteCharAt(i);
                                }else{
                                    ++i;
                                }
                            }
                            address = s.toString();
                            address = address.replace("، السعودية", "");
                            confirm_btn.setEnabled(true);
                            add_hint_btn.setEnabled(true);
                            progressBar.setVisibility(View.GONE);
                            address_tv.setVisibility(View.VISIBLE);
                            address_tv.setText(address == null ? "غير متاح" : address);
                        } catch (JSONException e) {
                            confirm_btn.setEnabled(true);
                            add_hint_btn.setEnabled(true);
                            progressBar.setVisibility(View.GONE);
                            address_tv.setVisibility(View.VISIBLE);
                            address_tv.setText(address == null ? "غير متاح" : address);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                confirm_btn.setEnabled(true);
                add_hint_btn.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                address_tv.setVisibility(View.VISIBLE);
                address_tv.setText(address == null ? "غير متاح" : address);
            }
        });

        queue.add(stringRequest);
    }

    public void confirm (){
        if (getActivity() == null) return;
        LatLng user_location = new LatLng(location.getLatitude(), location.getLongitude());
        OnStoreChosenListener.storePicked(address, user_location);
    }
    @Override
    public void onLocationChanged(Location location) {
        if (!manuallyDragged) {
            this.location = location;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            addMarker(latLng, true);
        }
    }


    private ResultCallback<LocationSettingsResult> mResultCallbackFromSettings = new ResultCallback<LocationSettingsResult>() {
        @Override
        public void onResult(LocationSettingsResult result) {
            final Status status = result.getStatus();
            switch (status.getStatusCode()) {
                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    try {
                        status.startResolutionForResult(
                                getActivity(),
                                552);
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }
                    break;
                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                    // Todo: Show alert
                    //Toast.makeText(getApplicationContext(), "Location not available, please try again", Toast.LENGTH_SHORT).show();
                    break;

            }
        }
    };

    private void addMarker(LatLng latLng, boolean zoom){
        boolean compute = true;
        if (mMarker != null) {
            LatLng prev = mMarker.getPosition();
            compute = prev.longitude != latLng.longitude;
        }
        if (compute) {
            if (mMarker != null) {
                mMarker.remove();
            }

            getMyAddress(latLng);
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(359.0f));
            if(mMap==null)
                return;
            mMarker = mMap.addMarker(markerOptions);
            if (zoom)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
        }
    }


    // for testing
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

            switch (item.getItemId()){
            case KeyEvent.KEYCODE_BACK:
            {
                Intent intent = new Intent(getActivity(),RegisterActivity.class);
                startActivity(intent);
                //finish();
            }
               // finish();
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null)
            mapView.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mapView != null)
            mapView.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mapView != null)
            mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mapView != null)
            mapView.onStop();

    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null)
            mapView.onSaveInstanceState(outState);
    }


}
