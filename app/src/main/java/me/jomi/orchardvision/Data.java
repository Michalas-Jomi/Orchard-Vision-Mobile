package me.jomi.orchardvision;

import android.graphics.Bitmap;
import androidx.annotation.Nullable;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.*;
import me.jomi.orchardvision.interfaces.Consumer;
import me.jomi.orchardvision.json.Json;
import me.jomi.orchardvision.json.JsonArray;

import java.util.*;

public class Data {
    public static class Tree {
        public static final Map<Marker, Tree> fromMarker = new HashMap<>();
        public static final Map<Integer, Tree> fromId = new HashMap<>();

        public final int id;
        private String type;
        private String variant;
        private double latitude;
        private double longitude;

        public Marker marker;

        public Json json;
        public boolean needDownload = true;

        public Tree(int id, String type, String variant, double latitude, double longitude) {
            this.id = id;
            this.type = type;
            this.variant = variant;
            this.latitude = latitude;
            this.longitude = longitude;

            fromId.put(id, this);
        }
        public Tree(Json json) {
            this(
                    json.getInt("id"),
                    json.getJson("variant").getJson("type").getString("name"),
                    json.getJson("variant").getString("name"),
                    json.getDouble("latitude"),
                    json.getDouble("longitude")
            );

            update(json);
        }


        public void destroy() {
            if (marker != null) {
                marker.remove();
                fromMarker.remove(marker);
            }
            fromId.remove(id);
        }


        public void makeMaker() {
            if (marker != null) {
                fromMarker.remove(marker);
                marker.remove();
            }
            marker = MapsActivity.mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(latitude, longitude))
                    .alpha(.8f)
                    .icon(markerIcon)
                    .title(variant)
            );
            fromMarker.put(marker, this);
        }


        public void update(Json json) {
            this.json = json;
            type    = json.getJson("variant").getJson("type").getString("name");
            variant = json.getJson("variant").getString("name");

            latitude  = json.getDouble("latitude");
            longitude = json.getDouble("longitude");

            update();
        }
        public void update() {
            boolean show = marker != null && marker.isInfoWindowShown();
            makeMaker();
            if (show)
                marker.showInfoWindow();
        }

        public String getType() {
            return type;
        }
        public String getVariant() {
            return variant;
        }
        public String jsonPlantingDate() {
            return json.getString("planting_date");
        }
        public String jsonNote() {
            return json.getString("note");
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Tree))
                return false;

            Consumer<Boolean> check = predicate -> {
                if (predicate)
                    throw new AssertionError();
            };

            Tree tree = (Tree) obj;
            try {
                check.accept(this.id == tree.id);
                check.accept(this.type.equals(tree.type));
                check.accept(this.latitude == tree.latitude);
                check.accept(this.longitude == tree.longitude);
                check.accept(this.variant.equals(tree.variant));
            } catch (AssertionError e) {
                return false;
            }
            return true;
        }
    }

    private static boolean needDownload = true;

    // type: [variants]
    private static final Map<String, Set<String>> types = new HashMap<>();
    public static final List<Tree> trees = new ArrayList<>();

    protected static BitmapDescriptor markerIcon;



    public static String[] getTypes() {
        return Func.stringArray(types.keySet());
    }
    public static String[] getVariants(@Nullable  String type) {
        if (types.containsKey(type)) {
            return Func.stringArray(types.get(type));
        } else {
            Set<String> set = new HashSet<>();
            for (Set<String> variants : types.values())
                set.addAll(variants);
            return Func.stringArray(set);
        }
    }

    public static boolean isNeedDownload() {
        return needDownload;
    }

    public static void init(Json json) {
        // Init Info
        types.clear();
        trees.clear();

        Json jsonTypes = json.getJson("types");
        for (String type : jsonTypes.keys()) {
            Set<String> variants = new HashSet<>();
            JsonArray array = jsonTypes.getArray(type);
            for (int i=0; i < array.length(); i++)
                variants.add(array.getString(i));
            types.put(type, variants);
        }

        JsonArray jsonTrees = json.getArray("trees");
        for (int i=0; i < jsonTrees.length(); i++) {
            Json tree = jsonTrees.getJson(i);
            trees.add(new Tree(
                    tree.getInt("id"),
                    tree.getString("type"),
                    tree.getString("variant"),
                    tree.getDouble("latitude"),
                    tree.getDouble("longitude")
            ));
        }


        // Post

        needDownload = false;

        if (MapsActivity.mMap != null)
            for (Data.Tree tree : Data.trees)
                tree.makeMaker();
    }
    public static void initMarkerIcon(Bitmap icon) {
        markerIcon = BitmapDescriptorFactory.fromBitmap(icon);
        for (Marker marker : Tree.fromMarker.keySet())
            marker.setIcon(markerIcon);
    }


    public static void addTypeVariant(String type, String variant) {
        if (!types.containsKey(type))
            types.put(type, new HashSet<>());
        types.get(type).add(variant);
    }

}
