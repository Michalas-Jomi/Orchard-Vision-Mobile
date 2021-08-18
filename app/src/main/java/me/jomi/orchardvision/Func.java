package me.jomi.orchardvision;

import android.location.Location;
import android.util.Pair;
import com.google.android.gms.common.internal.Preconditions;
import com.google.android.gms.maps.model.LatLng;
import me.jomi.orchardvision.interfaces.Consumer;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
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


    public static <T> void doForNonNull(T obj, Consumer<T> cons) {
        if (obj != null)
            cons.accept(obj);
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
    public static String readData(InputStream inputStream) {
        StringBuilder strB = new StringBuilder();
        try (Scanner in = new Scanner(inputStream)) {
            while (in.hasNextLine())
                strB.append(in.nextLine());
        }
        return strB.toString();
    }
}
