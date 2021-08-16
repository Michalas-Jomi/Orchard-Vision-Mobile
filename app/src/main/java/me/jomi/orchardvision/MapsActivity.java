package me.jomi.orchardvision;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;

@SuppressLint("MissingPermission")
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private Button pickUpTree;

    private LocationManager locationManager;

    protected LatLng curLatLng = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (Data.isNeedDownload())
            Data.download(getString(R.string.serverUrl));

        // Perms
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PackageManager.PERMISSION_GRANTED);


        // Views
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        pickUpTree = findViewById(R.id.Map_PickupTree_Button);


        // Register Views
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);

        pickUpTree.setOnClickListener(v -> {
            Log.d("picker", "Pickup tree");
            LatLng loc = curLatLng;

            Bundle data = new Bundle();
            data.putDouble("latitude",  loc.latitude);
            data.putDouble("longitude", loc.longitude);
            ActivityCompat.startActivityForResult(this, new Intent(this, NewTreeActivity.class).putExtras(data), 0, data);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null)
            return;


        if (NewTreeActivity.class.getName().equals(data.getExtras().getString("activity"))) {
            Tree tree = new Tree(data.getExtras());

            tree.addMaker(mMap);
            tree.sendToServer(getString(R.string.serverUrl));
            Toast.makeText(this, "Dodano nowe drzewo", Toast.LENGTH_LONG);
        }
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

        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

        // Add a marker in Sydney and move the camera
        /*
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        */
    }

    private Marker curLocMaker;
    @Override
    public void onLocationChanged(Location location) {
        if (location == null) {
            Log.d("LocationManager", "null Location");
            return;
        }
        Log.d("LocationManager", location.getLatitude() + " " + location.getLongitude());

        curLatLng = Func.toLatLng(location);


        if (curLocMaker != null)
            curLocMaker.remove();
        else {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder().target(curLatLng).zoom(mMap.getMaxZoomLevel() - .1f).build()));
        }
        curLocMaker = mMap.addMarker(new MarkerOptions().position(curLatLng).title("Jesteś Tutaj"));
    }


    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude","disable");
    }
    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude","enable");
    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude","status");
    }
}