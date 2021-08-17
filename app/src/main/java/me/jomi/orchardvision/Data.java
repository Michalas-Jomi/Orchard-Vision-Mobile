package me.jomi.orchardvision;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.*;
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

        public JSONObject json;
        public boolean json_isDownloading = false;

        public Tree(int id, String type, String variant, double latitude, double longitude) {
            this.id = id;
            this.type = type;
            this.variant = variant;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public void makeMaker(GoogleMap map) {
            if (marker == null) {
                marker = map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).alpha(.8f));
                fromMarker.put(marker, this);
            }
            marker.setPosition(new LatLng(latitude, longitude));
            marker.setIcon(markerIcon);
            marker.setTitle(variant);
        }
    }

    private static boolean needDownload = true;

    // type: [variants]
    private static Map<String, List<String>> types = new HashMap<>();
    public static List<Tree> trees = new ArrayList<>();

    protected static BitmapDescriptor markerIcon;


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
