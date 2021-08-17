package me.jomi.orchardvision;

import android.location.Location;
import com.google.android.gms.common.internal.Preconditions;
import com.google.android.gms.maps.model.LatLng;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public abstract class Func {
    public static LatLng toLatLng(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }
    public static int Int(String intToParse) {
        try {
            return Integer.parseInt(intToParse);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static RuntimeException throwEx(Throwable t) {
        if (t == null)
            throw new NullPointerException();
        throw _throwEx(t);
    }
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T _throwEx(Throwable t) throws T {
        throw (T) t;
    }


    public static byte[] toByteArray(InputStream stream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Preconditions.checkNotNull(stream);
        Preconditions.checkNotNull(byteArrayOutputStream);
        byte[] buff = new byte[4096];

        int read;
        while((read = stream.read(buff)) != -1) {
            byteArrayOutputStream.write(buff, 0, read);
        }

        return byteArrayOutputStream.toByteArray();
    }

    public static InputStream sendRequest(String url) throws IOException {
        HttpURLConnection client = (HttpURLConnection) new URL(url).openConnection();
        client.setDoInput(true);

        client.connect();

        return client.getInputStream();
    }

    public static String readData(InputStream inputStream) {
        StringBuilder strB = new StringBuilder();
        try (Scanner in = new Scanner(inputStream)) {
            while (in.hasNextLine())
                strB.append(in.nextLine());
        }
        return strB.toString();
    }
    public static JSONObject sendRequestForJson(String url) throws IOException {
        JSONObject json;

        try {
            String data = readData(Func.sendRequest(url));
            json = new JSONObject(data);
        } catch (JSONException e) {
            throw Func.throwEx(e);
        }

        return json;
    }
}
