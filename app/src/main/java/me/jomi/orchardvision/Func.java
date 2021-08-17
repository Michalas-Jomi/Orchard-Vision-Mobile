package me.jomi.orchardvision;

import android.location.Location;
import android.util.Pair;
import com.google.android.gms.common.internal.Preconditions;
import com.google.android.gms.maps.model.LatLng;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

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

    public static String[] stringArray(Collection<String> collection) {
        List<String> list = new ArrayList<>(collection);
        Collections.sort(list, (s1, s2) -> s1.toLowerCase().compareTo(s2.toLowerCase()));

        String[] array = new String[list.size()];

        for (int i=0; i < list.size(); i++)
            array[i] = list.get(i);

        return array;
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

    public static byte[] getPostBytes(Pair<String, String>... data) throws UnsupportedEncodingException {
        StringBuilder strB = new StringBuilder();

        for (Pair<String, String> pair : data) {
            if (strB.length() > 0)
                strB.append('&');
            strB.append(URLEncoder.encode(pair.first, "utf-8"))
                    .append('=')
                    .append(URLEncoder.encode(pair.second, "utf-8"));
        }

        return strB.toString().getBytes("utf-8");
    }
    public static String sendPostRequest(String url, Pair<String, String>... data) throws IOException {
        byte[] dataBytes = Func.getPostBytes(data);

        HttpURLConnection client = (HttpURLConnection) new URL(url).openConnection();

        client.setDoOutput(true);
        client.setUseCaches(false);
        client.setInstanceFollowRedirects(false);

        client.setRequestMethod("POST");
        client.setRequestProperty("charset", "utf-8");
        client.setRequestProperty("Content-Length", Integer.toString(dataBytes.length));

        try(DataOutputStream wr = new DataOutputStream(client.getOutputStream())) {
            wr.write(dataBytes);
            wr.flush();
        }

        try {
            return Func.readData(client.getInputStream());
        } catch (FileNotFoundException e) {
            return "";
        }
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
