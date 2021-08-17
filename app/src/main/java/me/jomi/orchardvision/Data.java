package me.jomi.orchardvision;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.annotation.Nullable;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.*;
import me.jomi.orchardvision.json.Json;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class Data {
    public static class Tree {
        public static final Map<Marker, Tree> fromMarker = new HashMap<>();

        public final int id;
        public final String type;
        public final String variant;
        public final double latitude;
        public final double longitude;

        public Marker marker;

        public Json json;
        public boolean needDownload = true;

        public Tree(int id, String type, String variant, double latitude, double longitude) {
            this.id = id;
            this.type = type;
            this.variant = variant;
            this.latitude = latitude;
            this.longitude = longitude;
        }


        public void makeMaker(GoogleMap map) {
            if (marker != null) {
                fromMarker.remove(marker);
                marker.remove();
            }
            marker = map.addMarker(new MarkerOptions()
                    .position(new LatLng(latitude, longitude))
                    .alpha(.8f)
                    .icon(markerIcon)
                    .title(variant)
            );
            fromMarker.put(marker, this);
        }


        public void update(Json json) {
            this.json = json;
        }

        public String jsonType() {
            return json.getJson("variant").getJson("type").getString("name");
        }
        public String jsonVariant() {
            return json.getJson("variant").getString("name");
        }
        public String jsonPlantingDate() {
            return json.getString("planting_date");
        }
        public String jsonNote() {
            return json.getString("note");
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof Tree))
                return false;

            Tree tree = (Tree) obj;
            try {
                assert this.id == tree.id;
                assert this.type.equals(tree.type);
                assert this.latitude == tree.latitude;
                assert this.longitude == tree.longitude;
                assert this.variant.equals(tree.variant);
            } catch (AssertionError e) {
                return false;
            }
            return true;
        }
    }

    private static boolean needDownload = true;

    // type: [variants]
    private static Map<String, List<String>> types = new HashMap<>();
    public static List<Tree> trees = new ArrayList<>();

    protected static BitmapDescriptor markerIcon;



    public static String[] getTypes() {
        return Func.stringArray(types.keySet());
    }
    public static String[] getVariants(@Nullable  String type) {
        if (types.containsKey(type)) {
            return Func.stringArray(types.get(type));
        } else {
            List<String> list = new ArrayList<>();
            for (List<String> variants : types.values())
                list.addAll(variants);
            return Func.stringArray(list);
        }
    }

    public static boolean isNeedDownload() {
        return needDownload;
    }

    private static Thread downloadingThread;
    public static void download(String serverUrl, Runnable callback) {
        if (downloadingThread != null)
            downloadingThread.interrupt();
        downloadingThread = new Thread(() -> {
                try {
                    Data._download(serverUrl, callback);
                } catch (IOException | JSONException e) {
                    Func.throwEx(e);
                }});
        downloadingThread.start();
    }
    private static void _download(String serverUrl, Runnable callback) throws IOException, JSONException {
        // Init Info
        Map<String, List<String>> types = new HashMap<>();
        List<Tree> trees = new ArrayList<>();


        JSONObject json = Func.sendRequestForJson(serverUrl + "broker/initinfo");

        JSONObject jsonTypes = json.getJSONObject("types");
        for (Iterator<String> it = jsonTypes.keys(); it.hasNext(); ) {
            String type = it.next();
            List<String> variantList = new ArrayList<>();
            JSONArray array = jsonTypes.getJSONArray(type);
            for (int i=0; i < array.length(); i++)
                variantList.add(array.getString(i));
            types.put(type, variantList);
        }

        JSONArray jsonTrees = json.getJSONArray("trees");
        for (int i=0; i < jsonTrees.length(); i++) {
            JSONObject tree = jsonTrees.getJSONObject(i);
            trees.add(new Tree(
                    tree.getInt("id"),
                    tree.getString("type"),
                    tree.getString("variant"),
                    tree.getDouble("latitude"),
                    tree.getDouble("longitude")
            ));
        }


        // Marker icon

        byte[] data;
        try (InputStream inputStream = Func.sendRequest(serverUrl + "static/orchardMap/img/treeMarker.png")) {
            data = Func.toByteArray(inputStream);
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 3, bitmap.getHeight() * 3, false);

        // Post
        markerIcon = BitmapDescriptorFactory.fromBitmap(bitmap);

        Data.types = types;
        Data.trees = trees;

        needDownload = false;
        downloadingThread = null;

        callback.run();
    }

}
