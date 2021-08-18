package me.jomi.orchardvision;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

@SuppressLint("MissingPermission")
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, GoogleMap.InfoWindowAdapter {

    public static GoogleMap mMap;
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
            HttpUtils.init(json -> Data.init(json));
            HttpUtils.downloadMarkerIcon(Data::initMarkerIcon);
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
            if (curLatLng == null) {
                Toast.makeText(MapsActivity.this, "Brak danych GPS", Toast.LENGTH_SHORT).show();
                return;
            }

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
            tree.sendToServer();
            Toast.makeText(this, "Dodano nowe drzewo", Toast.LENGTH_LONG).show();
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


        for (Data.Tree tree : Data.trees)
            tree.makeMaker();


        mMap.setInfoWindowAdapter(this);
        mMap.setOnInfoWindowLongClickListener(marker -> {
            DialogInterface.OnClickListener yesListener = (dialog, which) -> {
                if (which != DialogInterface.BUTTON_POSITIVE)
                    return;

                Data.Tree tree = Data.Tree.fromMarker.get(marker);
                if (tree == null) return;

                if (tree.json == null) {
                    Toast.makeText(MapsActivity.this, "Trwa wczytywanie drzewa, spróbuj ponownie", Toast.LENGTH_LONG).show();
                    return;
                }

                Bundle data = new Bundle();
                data.putInt("id", tree.id);
                data.putString("type", tree.getType());
                data.putString("variant", tree.getVariant());
                data.putString("planting_date", tree.jsonPlantingDate());
                data.putString("note", tree.jsonNote());
                ActivityCompat.startActivityForResult(this, new Intent(this, EditTreeActivity.class).putExtras(data), 0, data);

            };
            if (Data.Tree.fromMarker.containsKey(marker))
                new AlertDialog.Builder(MapsActivity.this)
                        .setMessage("Chcesz Edytować to drzewo?")
                        .setPositiveButton("Tak", yesListener)
                        .setNegativeButton("Nie", null)
                        .show();
        });

        // Add a marker in Sydney and move the camera
        /*
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        */
    }



    // GPS

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

        // Info

        TextView type    = root.findViewById(R.id.tree_infoWindow_type_Text);
        TextView variant = root.findViewById(R.id.tree_infoWindow_variant_Text);
        TextView planted = root.findViewById(R.id.tree_infoWindow_planted_Text);
        TextView note    = root.findViewById(R.id.tree_infoWindow_note_Text);
        if (tree.json == null) {
            type.setText(tree.getType());
            variant.setText(tree.getVariant());
            planted.setText(getText(R.string.loading));
            note.setText(getText(R.string.loading));

            if (tree.needDownload) {
                tree.needDownload = false;
                HttpUtils.downloadTree(tree.id);
            }
        }
        if (tree.json != null) {
            try {
                type   .setText(tree.getType());
                variant.setText(tree.getVariant());
                planted.setText(tree.jsonPlantingDate());
                note   .setText(tree.jsonNote());
            } catch (Throwable e) {
                e.printStackTrace();
                tree.json = null;
            }
        }

        // Color

        setBlackColor(root);

        return root;
    }
    private static void setBlackColor(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i=0; i < viewGroup.getChildCount(); i++) {
                setBlackColor(viewGroup.getChildAt(i));
            }
        } else {
            if (view instanceof TextView)
                ((TextView) view).setTextColor(0xFF000000);
        }
    }
}
