package me.jomi.orchardvision;

import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.widget.EditText;
import androidx.annotation.Nullable;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class NewTreeActivity extends DetailTreeActivity {
    public static class Tree {
        public final String type;
        public final String variant;
        public final int age;

        public final LatLng loc;

        private Marker marker;
        private Data.Tree dataTree;
        private GoogleMap map;

        public Tree(Bundle data) {
            loc = new LatLng(data.getDouble("latitude"), data.getDouble("longitude"));

            type = data.getString("type");
            variant = data.getString("variant");
            age = data.getInt("age");
        }
        public Tree(LatLng loc, String type, String variant, int age) {
            this.loc = loc;

            this.type = type;
            this.variant = variant;
            this.age = age;
        }


        public void serialize(Bundle data) {
            data.putDouble("latitude",  loc.latitude);
            data.putDouble("longitude", loc.longitude);

            data.putString("type", type);
            data.putString("variant", variant);
            data.putInt("age", age);
        }
        public Bundle buildBundle() {
            Bundle bundle = new Bundle();
            serialize(bundle);
            return bundle;
        }


        public void addMaker(GoogleMap map) {
            if (dataTree != null && this.map == null) {
                dataTree.makeMaker();
            } else if (dataTree != null)
                return;

            this.map = map;
            this.marker = map.addMarker(new MarkerOptions().position(loc).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            if (Data.markerIcon != null) {
                this.marker.setIcon(Data.markerIcon);
            }
        }

        public void sendToServer() {
            HttpUtils.sendTree(id -> {
                if (id == -1)
                    return;

                Data.addTypeVariant(type, variant);

                dataTree = new Data.Tree(id, type, variant, loc.latitude, loc.longitude);
                if (map != null) {
                    if (marker != null)
                        marker.remove();
                    dataTree.makeMaker();
                }
            },
                    new Pair("type", type),
                    new Pair("variant", variant),
                    new Pair("age", String.valueOf(age)),
                    new Pair("latitude", String.valueOf(loc.latitude)),
                    new Pair("longitude", String.valueOf(loc.longitude))
            );
        }
    }

    private double latitude;
    private double longitude;

    private EditText mAge;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_new_tree);


        // Data
        latitude  = getIntent().getExtras().getDouble("latitude");
        longitude = getIntent().getExtras().getDouble("longitude");

        // Views
        mType    = findViewById(R.id.NewTree_Type_Text);
        mVariant = findViewById(R.id.NewTree_Variant_Text);
        mAge     = findViewById(R.id.NewTree_Age_Number);
        mConfirm = findViewById(R.id.NewTree_Confirm_Button);

        // Register
        setupTypeVariant();

        mConfirm.setOnClickListener(v -> {
            Tree tree = new Tree(
                    new LatLng(latitude, longitude),
                    mType.getText().toString(),
                    mVariant.getText().toString(),
                    Func.Int(mAge.getText().toString())
            );

            NewTreeActivity.this.setResult(0, new Intent()
                    .putExtras(tree.buildBundle())
                    .putExtra("activity", NewTreeActivity.class.getName())
            );
            NewTreeActivity.this.finish();
        });
    }
}
