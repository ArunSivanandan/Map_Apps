package com.maptrack;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;

/**
 * Created by Jeffrey on 6/21/2017.
 */

public class Constants {
    /**
     * Used to set an expiration time for a geofence. After this amount of time Location Services
     * stops tracking the geofence.
     */
    public static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;

    /**
     * For this sample, geofences expire after twelve hours.
     */
    public static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;
    public static final float GEOFENCE_RADIUS_IN_METERS = 1; // 1 mile, 1.6 km

    private Constants() {
    }

    public static final HashMap<String, LatLng> BAY_AREA_LANDMARKS = new HashMap<>();

    static {

    }
}
