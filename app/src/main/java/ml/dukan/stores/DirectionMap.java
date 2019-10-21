package ml.dukan.stores;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.maps.android.ui.IconGenerator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ml.dukan.stores.HelperClasses.Util;

/**
 * Created by khaled on 25/07/17.
 */

public class DirectionMap extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    float c_lat, c_lng;
    float s_lat, s_lng;
    String customerName = "خالد";
    String storeName;

    TextView durationTV, distanceTV;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.direction_map);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        durationTV = (TextView) findViewById(R.id.direction_map_duration);
        distanceTV= (TextView) findViewById(R.id.direction_map_distance);
        storeName = preferences.getString(Util.STORE_NAME, getString(R.string.app_name));

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.direction_map_fragment);
        mapFragment.getMapAsync(this);

        Bundle extras = getIntent().getExtras();
        customerName = extras.getString("name");
        c_lat = extras.getFloat("c_lat");
        c_lng = extras.getFloat("c_lng");
        s_lat= extras.getFloat("s_lat");
        s_lng = extras.getFloat("s_lng");

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        drawLine();

        IconGenerator icon = new IconGenerator(this);

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(new LatLng(c_lat, c_lng));
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon.makeIcon(customerName)));
        MarkerOptions markerOptions2 = new MarkerOptions();
        markerOptions2.position(new LatLng(s_lat, s_lng));
        markerOptions2.icon(BitmapDescriptorFactory.fromBitmap(icon.makeIcon(storeName)));
        mMap.addMarker(markerOptions);
        mMap.addMarker(markerOptions2);


        float avgLat = (s_lat+c_lat)/2;
        float avgLng = (s_lng+c_lng)/2;
        LatLng averageLocation = new LatLng(avgLat, avgLng);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(averageLocation, 17f));
    }

    private void drawLine (){
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://maps.googleapis.com/maps/api/directions/" +
                "json?origin="+s_lat+","+s_lng+"&" +
                "language=ar"+
                "&destination="+c_lat+","+c_lng+"&key=AIzaSyDAAfa6doNw5cr_AMbULpRidPd1RN3ZLj0";
        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject json = new JSONObject(response);
                    JSONArray routeArray = json.getJSONArray("routes");
                    JSONObject routes = routeArray.getJSONObject(0);
                    JSONArray legsArray = routes.getJSONArray("legs");
                    JSONObject duration = legsArray.getJSONObject(0).getJSONObject("duration");
                    //long seconds_duration = duration.getLong("value");
                    String duration_text = duration.getString("text");
                    JSONObject distance = legsArray.getJSONObject(0).getJSONObject("distance");
                    long distance_n = distance.getLong("value");
/*
                    String time = String.format("%02d:%02d:%02d",
                            seconds_duration/60/60, seconds_duration/60%60, seconds_dura*%60);*/
                    durationTV.setText(getString(R.string.duration_title, duration_text));
                    distanceTV.setText(getString(R.string.distance_title, (distance_n/1000.0f)));

                    JSONObject overviewPolylines = routes
                            .getJSONObject("overview_polyline");
                    String encodedString = overviewPolylines.getString("points");
                    List<LatLng> list = decodePoly(encodedString);

                    PolylineOptions options = new PolylineOptions()
                            .startCap(new RoundCap()).endCap(new RoundCap()).width(20).color(Color.parseColor("#4E8CF4")).geodesic(false);
                    for (int z = 0; z < list.size(); z++) {
                        LatLng point = list.get(z);
                        options.add(point);
                    }

                    Log.e("Points", "Done");
                    mMap.addPolyline(options);
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        queue.add(request);
    }


    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

}
