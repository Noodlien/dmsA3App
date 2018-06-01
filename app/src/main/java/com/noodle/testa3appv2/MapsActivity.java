package com.noodle.testa3appv2;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Activity to display a Map with marker of the position from the location message pressed in
 * ConversationActivity
 * Craig Fraser 15889604
 * Connor Hewett 15903849
 */
public class MapsActivity extends FragmentActivity implements  OnMapReadyCallback {

    private GoogleMap mMap;
    double latitude;
    double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Get the latitude and longitude in the intent passed to this Activity
        latitude = this.getIntent().getDoubleExtra("latitude", latitude);
        longitude = this.getIntent().getDoubleExtra("longitude", longitude);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker on the location and move the camera
        LatLng location = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(location).title("Marker on location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(location));
    }
}
