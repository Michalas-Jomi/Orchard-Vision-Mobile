package me.jomi.orchardvision;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

@SuppressLint("MissingPermission")
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, GoogleMap.InfoWindowAdapter {

    private GoogleMap mMap;
    private Button pickUpTree;

    private LocationManager locationManager;

    protected LatLng curLatLng = null;

    public static final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (Data.isNeedDownload()) {
            Data.download(getString(R.string.serverUrl), () -> mHandler.post(() -> {
                    if (mMap != null)
                        for (Data.Tree tree : Data.trees)
                            tree.makeMaker(mMap);
            }));
        }

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
            NewTreeActivity.Tree tree = new NewTreeActivity.Tree(data.getExtras());

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

        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);


        for (Data.Tree tree : Data.trees) {
            tree.makeMaker(mMap);
        }

        mMap.setInfoWindowAdapter(this);

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



    /// Google maps



    @Nullable
    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        return null;
    }
    @Nullable
    @Override
    public View getInfoContents(@NonNull Marker marker) {
        Data.Tree tree = Data.Tree.fromMarker.get(marker);
        if (tree == null)
            return null;

        View root = LayoutInflater.from(this).inflate(R.layout.tree_info_window, null, false);

        TextView type    = root.findViewById(R.id.tree_infoWindow_type_Text);
        TextView variant = root.findViewById(R.id.tree_infoWindow_variant_Text);
        TextView planted = root.findViewById(R.id.tree_infoWindow_planted_Text);
        TextView note    = root.findViewById(R.id.tree_infoWindow_note_Text);
        if (tree.json == null) {
            type.setText(tree.type);
            variant.setText(tree.variant);
            planted.setText(getText(R.string.loading));
            note.setText(getText(R.string.loading));

            if (!tree.json_isDownloading) {
                tree.json_isDownloading = true;
                new Thread(() -> {
                    try {
                        JSONObject json = Func.sendRequestForJson(getString(R.string.serverUrl) + "broker/info/tree/" + tree.id);
                        Log.d("json", json.toString());
                        tree.json = json;
                        mHandler.post(() -> {
                            if (marker.isInfoWindowShown())
                                marker.showInfoWindow();
                        });
                        mHandler.postDelayed(() -> {
                            tree.json = null;
                        }, 30_000L);
                    } catch (IOException e) {
                        Func.throwEx(e);
                    } finally {
                        tree.json_isDownloading = false;
                    }
                }).start();
            }
        } else {
            try {
                type   .setText(tree.json.getJSONObject("variant").getJSONObject("type").getString("name"));
                variant.setText(tree.json.getJSONObject("variant").getString("name"));
                planted.setText(tree.json.getString("planting_date"));
                note   .setText(tree.json.getString("note"));
            } catch (JSONException e) {
                e.printStackTrace();
                tree.json = null;
            }
        }



        return root;
    }
}
