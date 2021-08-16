package me.jomi.orchardvision;

import android.os.Bundle;
import android.util.Pair;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class Tree {
    public final String type;
    public final String variant;
    public final int age;

    public final LatLng loc;

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
        map.addMarker(new MarkerOptions().position(loc).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
    }

    public void sendToServer(String serverUrl) {
            new Thread(() -> {
                try {
                    _sendToServer(serverUrl);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
    }
    private byte[] getPostBytes() throws UnsupportedEncodingException {
        StringBuilder strB = new StringBuilder();

        for (Pair<String, String> pair : new Pair[] {new Pair("type", type), new Pair("variant", variant), new Pair("age", String.valueOf(age)),
                                                     new Pair("latitude", String.valueOf(loc.latitude)), new Pair("longitude", String.valueOf(loc.longitude))}) {
            if (strB.length() > 0)
                strB.append('&');
            strB.append(URLEncoder.encode(pair.first, "utf-8"))
                .append('=')
                .append(URLEncoder.encode(pair.second, "utf-8"));
        }
        return strB.toString().getBytes("utf-8");
    }
    private void _sendToServer(String serverUrl) throws IOException {
        String url = serverUrl + "broker/new/tree";

        byte[] dataBytes = getPostBytes();

        HttpURLConnection client = (HttpURLConnection) new URL(url).openConnection();

        client.setDoOutput(true);
        client.setUseCaches(false);
        client.setInstanceFollowRedirects(false);

        client.setRequestMethod("POST");
        client.setRequestProperty("charset", "utf-8");
        client.setRequestProperty("Content-Length", Integer.toString(dataBytes.length));

        try( DataOutputStream wr = new DataOutputStream(client.getOutputStream())) {
            wr.write(dataBytes);

            wr.flush();
        }

        client.getInputStream().close();
    }
}
