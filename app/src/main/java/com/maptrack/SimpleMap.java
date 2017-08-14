package com.maptrack;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class SimpleMap extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMapLoadedCallback,
        View.OnClickListener, StreetViewPanorama.OnStreetViewPanoramaChangeListener, GoogleMap.OnMarkerDragListener,
            ResultCallback<Status> {

    private GoogleMap mMap;
    ArrayList<LatLng> points = null;

    int count = 0;

    private Geocoder geocoder;

    private List<Address> addresses;

    private UiSettings mUiSettings;

    private GoogleApiClient mGoogleApiClient;

    private Location mLastLocation;

    private Button bt_draw, bt_street, bt_show_street;

    private View centerLine;

    private Marker mMarker;

    LatLng currentLatLng = null;

    private StreetViewPanorama mStreetViewPanorama;

    final String TAG = getClass().getSimpleName();
    private static final long GEO_DURATION = 60 * 60 * 1000;
    private static final String GEOFENCE_REQ_ID = "My Geofence";
    private static final float GEOFENCE_RADIUS = 500.0f; // in meters
    private PendingIntent geoFencePendingIntent;
    private Circle geoFenceLimits;
    private final int GEOFENCE_REQ_CODE = 0;
    protected ArrayList<Geofence> mGeofenceList;
    private boolean flag = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_map);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        SupportStreetViewPanoramaFragment streetViewPanoramaFragment = (SupportStreetViewPanoramaFragment) getSupportFragmentManager().findFragmentById(R.id.streetviewpanorama);
        streetViewPanoramaFragment.getStreetViewPanoramaAsync(new OnStreetViewPanoramaReadyCallback() {
            @Override
            public void onStreetViewPanoramaReady(StreetViewPanorama panorama) {
                mStreetViewPanorama = panorama;
                mStreetViewPanorama.setPosition(new LatLng(-33.87365, 151.20689));
                mStreetViewPanorama.setOnStreetViewPanoramaChangeListener(SimpleMap.this);
                // Only need to set the position once as the streetview fragment will maintain
                // its state.
                        /*if (savedInstanceState == null) {
                            mStreetViewPanorama.setPosition(SYDNEY);
                        }*/
            }
        });

        mGeofenceList = new ArrayList<Geofence>();

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        bt_draw = (Button) findViewById(R.id.bt_draw);
        bt_street = (Button) findViewById(R.id.bt_street);
        bt_show_street = (Button) findViewById(R.id.bt_show_street);
        centerLine = (View) findViewById(R.id.center_line);
        bt_draw.setOnClickListener(this);
        bt_street.setOnClickListener(this);
        bt_show_street.setOnClickListener(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mUiSettings = mMap.getUiSettings();

        // mapTimer();


        // Keep the UI Settings state in sync with the checkboxes.
        mUiSettings.setZoomControlsEnabled(false);
        mUiSettings.setCompassEnabled(false);
        mUiSettings.setMyLocationButtonEnabled(false);
        mMap.setMyLocationEnabled(false);
        mMap.setTrafficEnabled(true);
        mUiSettings.setScrollGesturesEnabled(true);
        mUiSettings.setZoomGesturesEnabled(true);
        mUiSettings.setTiltGesturesEnabled(true);
        mUiSettings.setRotateGesturesEnabled(true);

        /*if(currentLatLng !=null ) {
        }*/

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            if (addresses != null && addresses.size() > 0) {
                addresses.clear();
            }
            geocoder = new Geocoder(this, Locale.getDefault());
            try {
                addresses = geocoder.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            currentLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
            CameraUpdate zoom = CameraUpdateFactory.zoomTo(11);
            mMap.moveCamera(center);
            mMap.animateCamera(zoom);
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                    .title("Current Location"));
            mMarker = null;
            mMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                    .title("Dragable")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.pegman))
                    .draggable(true));
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(12.9760, 80.2212))
                    .title("Nungampakam"));




            mMap.setOnMarkerDragListener(this);

            ///  mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
            //  mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
        }
    }

    // Create a Geofence
    private Geofence createGeofence(LatLng latLng, float radius) {
        Log.d(TAG, "createGeofence");
        return new Geofence.Builder()
                .setRequestId(GEOFENCE_REQ_ID)
                .setCircularRegion(latLng.latitude, latLng.longitude, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setNotificationResponsiveness(1000)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }

    // Create a Geofence Request
    private GeofencingRequest createGeofenceRequest(Geofence geofence) {
        Log.d(TAG, "createGeofenceRequest");
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();
    }


    private PendingIntent createGeofencePendingIntent() {
        Log.d(TAG, "createGeofencePendingIntent");
        if (geoFencePendingIntent != null)
            return geoFencePendingIntent;

        Intent intent = new Intent(this, GeofenceTransitionIntentService.class);
        return PendingIntent.getService(
                this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    // Add the created GeofenceRequest to the device's monitoring list
    private void addGeofence(GeofencingRequest request) {
        Log.d(TAG, "addGeofence");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.GeofencingApi.addGeofences(
                mGoogleApiClient,
                request,
                createGeofencePendingIntent()
        ).setResultCallback((ResultCallback<? super Status>) this);

    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapLoaded() {

    }


    private String  getMapsApiDirectionsUrl(LatLng origin,LatLng dest) {
        // Origin of route
        String str_origin = "origin="+origin.latitude+","+origin.longitude;

        // Destination of route
        String str_dest = "destination="+dest.latitude+","+dest.longitude;


        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;


        return url;

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bt_draw:
                String url = getMapsApiDirectionsUrl(currentLatLng, new LatLng(12.9760, 80.2212));
                ReadTask downloadTask = new ReadTask();
                // Start downloading json data from Google Directions API
                downloadTask.execute(url);
                break;
            case R.id.bt_street:
                /*mStreetViewPanorama.setPosition(mMarker.getPosition(), 150);*/
                mapTimer();
                break;
            case R.id.bt_show_street:
                if (flag) {
                    flag = false;
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) centerLine.getLayoutParams();
                    layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
                    centerLine.setLayoutParams(layoutParams);
                } else {
                    flag = true;
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) centerLine.getLayoutParams();
                    layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, -1);
                    centerLine.setLayoutParams(layoutParams);
                }
                break;
        }
    }

    @Override
    public void onStreetViewPanoramaChange(StreetViewPanoramaLocation streetViewPanoramaLocation) {
      //  Log.e("streetViewPanoramaLocation", String.valueOf(streetViewPanoramaLocation.position));
        if (streetViewPanoramaLocation != null) {
            mStreetViewPanorama.setPosition(streetViewPanoramaLocation.position);
        }
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {
        //Log.e("position", String.valueOf(marker.getPosition()));

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        Log.e("posi", String.valueOf(marker.getPosition()));
        mStreetViewPanorama.setPosition(marker.getPosition(), 150);
    }

    @Override
    public void onResult(@NonNull Status status) {
        Log.i(TAG, "onResult: " + status);
        if ( status.isSuccess() ) {
            drawGeofence();
        } else {
            // inform about fail
        }
    }

    private void drawGeofence() {

        if ( geoFenceLimits != null )
            geoFenceLimits.remove();

        //Instantiates a new CircleOptions object +  center/radius
        CircleOptions circleOptions = new CircleOptions()
                .center(new LatLng(12.9760, 80.2212))
                .radius(GEOFENCE_RADIUS)
                .fillColor(Color.argb(255, 45, 45, 215))
                .strokeColor(Color.TRANSPARENT)
                .strokeWidth(2);

        // Get back the mutable Circle
        geoFenceLimits = mMap.addCircle(circleOptions);
    }

    // Start Geofence creation process
    /*private void startGeofence() {
        Log.i(TAG, "startGeofence()");
        if( geoFenceMarker != null ) {
            Geofence geofence = createGeofence( geoFenceMarker.getPosition(), GEOFENCE_RADIUS );
            GeofencingRequest geofenceRequest = createGeofenceRequest( geofence );
            addGeofence( geofenceRequest );
        } else {
            Log.e(TAG, "Geofence marker is null");
        }
    }*/

    private class ReadTask extends AsyncTask<String, Void , String> {

        @Override
        protected String doInBackground(String... url) {
            // TODO Auto-generated method stub
            String data = "";
            try {
                MapHttpConnection http = new MapHttpConnection();
                data = http.readUr(url[0]);


            } catch (Exception e) {
                // TODO: handle exception
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            new ParserTask().execute(result);
        }

    }

    public class MapHttpConnection {
        public String readUr(String mapsApiDirectionsUrl) throws IOException {
            String data = "";
            InputStream istream = null;
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(mapsApiDirectionsUrl);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();
                istream = urlConnection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(istream));
                StringBuffer sb = new StringBuffer();
                String line ="";
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                data = sb.toString();
                br.close();


            }
            catch (Exception e) {
                Log.d("reading url", e.toString());
            } finally {
                istream.close();
                urlConnection.disconnect();
            }
            return data;

        }
    }

    public class PathJSONParser {

        public List<List<HashMap<String, String>>> parse(JSONObject jObject) {
            List<List<HashMap<String, String>>> routes = new ArrayList<List<HashMap<String,String>>>();
            JSONArray jRoutes = null;
            JSONArray jLegs = null;
            JSONArray jSteps = null;
            try {
                jRoutes = jObject.getJSONArray("routes");
                for (int i=0 ; i < jRoutes.length() ; i ++) {
                    jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
                    List<HashMap<String, String>> path = new ArrayList<HashMap<String,String>>();
                    for(int j = 0 ; j < jLegs.length() ; j++) {
                        jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");
                        for(int k = 0 ; k < jSteps.length() ; k ++) {
                            String polyline = "";
                            polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                            List<LatLng> list = decodePoly(polyline);
                            for(int l = 0 ; l < list.size() ; l ++){
                                HashMap<String, String> hm = new HashMap<String, String>();
                                hm.put("lat",
                                        Double.toString(((LatLng) list.get(l)).latitude));
                                hm.put("lng",
                                        Double.toString(((LatLng) list.get(l)).longitude));
                                path.add(hm);
                            }
                        }
                        routes.add(path);
                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;

        }

        private List<LatLng> decodePoly(String encoded) {
            List<LatLng> poly = new ArrayList<LatLng>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                LatLng p = new LatLng((((double) lat / 1E5)),
                        (((double) lng / 1E5)));
                poly.add(p);
            }
            return poly;
        }}


    private class ParserTask extends AsyncTask<String,Integer, List<List<HashMap<String , String >>>> {
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(
                String... jsonData) {
            // TODO Auto-generated method stub
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jObject = new JSONObject(jsonData[0]);
                PathJSONParser parser = new PathJSONParser();
                routes = parser.parse(jObject);

                //Log.e("ro9ute", "inside route");


            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> routes) {

            PolylineOptions polyLineOptions = null;

            // traversing through routes
            for (int i = 0; i < routes.size(); i++) {
                points = new ArrayList<LatLng>();
                polyLineOptions = new PolylineOptions();
                List<HashMap<String, String>> path = routes.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                    Log.e("points", String.valueOf(points.get(j)));
                }

                polyLineOptions.addAll(points);
                polyLineOptions.width(10);
                polyLineOptions.color(Color.BLUE);
            }
            if(polyLineOptions != null)
                mMap.addPolyline(polyLineOptions);

            HashMap<Marker, String> str = new HashMap<>();

        }
    }

    private void mapTimer(){
        drawGeofence();
        final Handler h = new Handler();
        count = 0;
        h.postDelayed(new Runnable()
        {
            private long time = 0;

            @Override
            public void run()
            {
                // do stuff then
                // can call h again after work!
                time += 1000;
                mStreetViewPanorama.setPosition(mMarker.getPosition(), 150);
                CameraUpdate location = CameraUpdateFactory.newLatLngZoom(
                        points.get(count), 15);
                mMarker.setPosition(points.get(count));
                mMap.animateCamera(location);
                LatLng currentLatLng = points.get(count);
                //Location startLocation = new Location(currentLatLng);
                Location startLocation = new Location(LocationManager.GPS_PROVIDER);
                startLocation.setLatitude(currentLatLng.latitude);
                startLocation.setLongitude(currentLatLng.longitude);
                Location destination = new Location(LocationManager.GPS_PROVIDER);
                destination.setLatitude(12.9760);
                destination.setLongitude(80.2212);
                float distance = startLocation.distanceTo(destination);
                if(distance < 1000.0f){
                    Log.e("inside", "1000.0f");
                    geoFenceLimits.setFillColor(Color.argb(200, 45, 45, 215));
                } else if (distance < 500.0f) {
                    Log.e("inside", "500.0f");
                    geoFenceLimits.setFillColor(Color.argb(175, 45, 45, 215));
                } else if (distance < 15.0f) {
                    Log.e("inside", "15.0f");
                    geoFenceLimits.setFillColor(Color.argb(50, 45, 45, 215));
                }
                Log.e("distance", String.valueOf(startLocation.distanceTo(destination)));
                count++;
                Log.e("TimerExample", "Going for... " + time);
                if(count != points.size()){
                    h.postDelayed(this, 500);
                } else {
                    count = 0;
                    h.removeCallbacksAndMessages(null);
                }
            }
        }, 500);
    }

}
