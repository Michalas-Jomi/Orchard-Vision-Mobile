package me.jomi.orchardvision;

import android.location.Location;
import com.google.android.gms.maps.model.LatLng;

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
}
