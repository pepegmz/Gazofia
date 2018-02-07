package com.gazofiadevelopers.gazofiaapp;

import android.Manifest;
import android.animation.Animator;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gazofiadevelopers.gazofiaapp.data.AsyncHttpClientManagement;
import com.gazofiadevelopers.gazofiaapp.data.CustomMark;
import com.gazofiadevelopers.gazofiaapp.data.Vars;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

public class GazofiaMain extends AppCompatActivity
        implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener{

    ArrayList<CustomMark> markers = new ArrayList<>();
    TextView info_magna, info_premium, info_diesel;

    ProgressDialog progressDialog;
    private boolean isOpen = false;
    private SlidingUpPanelLayout layoutMain;
    private RelativeLayout layoutButtons;
    private RelativeLayout layoutContent;

    private static final String TAG = GazofiaMain.class.getSimpleName();
    private GoogleMap mMap;
    private CameraPosition mCameraPosition;

    // The entry points to the Places API.
    private GeoDataClient mGeoDataClient;
    private PlaceDetectionClient mPlaceDetectionClient;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    // Used for selecting the current place.
    private static final int M_MAX_ENTRIES = 5;
    private String[] mLikelyPlaceNames;
    private String[] mLikelyPlaceAddresses;
    private String[] mLikelyPlaceAttributions;
    private LatLng[] mLikelyPlaceLatLngs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_gazofia_main);

        // Construct a GeoDataClient.
        mGeoDataClient = Places.getGeoDataClient(this, null);

        // Construct a PlaceDetectionClient.
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Build the map.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        layoutMain = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        layoutButtons = (RelativeLayout) findViewById(R.id.layoutButtons);
        layoutContent = (RelativeLayout) findViewById(R.id.layoutContent);
    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    /**
     * Sets up the options menu.
     *
     * @param menu The options menu.
     * @return Boolean.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.current_place_menu, menu);
        return true;
    }

    /**
     * Handles a click on the menu option to get a place.
     *
     * @param item The menu item to handle.
     * @return Boolean.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.option_get_place) {
            // showCurrentPlace();
            viewMenu();
        }
        return true;
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }

        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            // Return null here, so that getInfoContents() is called next.
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.mark_info_content,
                        (FrameLayout) findViewById(R.id.map), false);

                CustomMark mark = markers.get(Integer.parseInt(marker.getTag().toString()));

                info_magna = ((TextView) infoWindow.findViewById(R.id.info_green_price));
                info_premium = ((TextView) infoWindow.findViewById(R.id.info_red_price));
                info_diesel = ((TextView) infoWindow.findViewById(R.id.info_black_price));


                info_premium.setText(mark.getPremium());
                info_magna.setText(mark.getMagna());
                info_diesel.setText(mark.getDiesel());


                // TextView title = ((TextView) infoWindow.findViewById(R.id.title));
                // title.setText(marker.getTitle());

                // TextView snippet = ((TextView) infoWindow.findViewById(R.id.snippet));
                // snippet.setText(marker.getSnippet());



                return infoWindow;
            }
        });

        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();

        this.mMap.setOnMarkerClickListener(this);

        progressDialog = new ProgressDialog(GazofiaMain.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Cargando...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        getNegoVerificacion();
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            /*
                            mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng( 21.9197402, -102.3146227))
                                    .title("12.7")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.fuel_mini_green)));


                            drawMarker(new LatLng(21.9140667, -102.3045376));
                            */




                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }


    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                    updateLocationUI();
                }
            }
        }

    }

    /**
     * Prompts the user to select the current place from a list of likely places, and shows the
     * current place on the map - provided the user has granted location permission.
     */
    private void showCurrentPlace() {
        if (mMap == null) {
            return;
        }

        if (mLocationPermissionGranted) {
            // Get the likely places - that is, the businesses and other points of interest that
            // are the best match for the device's current location.
            @SuppressWarnings("MissingPermission") final Task<PlaceLikelihoodBufferResponse> placeResult =
                    mPlaceDetectionClient.getCurrentPlace(null);
            placeResult.addOnCompleteListener
                    (new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
                        @Override
                        public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                PlaceLikelihoodBufferResponse likelyPlaces = task.getResult();

                                // Set the count, handling cases where less than 5 entries are returned.
                                int count;
                                if (likelyPlaces.getCount() < M_MAX_ENTRIES) {
                                    count = likelyPlaces.getCount();
                                } else {
                                    count = M_MAX_ENTRIES;
                                }

                                int i = 0;
                                mLikelyPlaceNames = new String[count];
                                mLikelyPlaceAddresses = new String[count];
                                mLikelyPlaceAttributions = new String[count];
                                mLikelyPlaceLatLngs = new LatLng[count];

                                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                                    // Build a list of likely places to show the user.
                                    mLikelyPlaceNames[i] = (String) placeLikelihood.getPlace().getName();
                                    mLikelyPlaceAddresses[i] = (String) placeLikelihood.getPlace()
                                            .getAddress();
                                    mLikelyPlaceAttributions[i] = (String) placeLikelihood.getPlace()
                                            .getAttributions();
                                    mLikelyPlaceLatLngs[i] = placeLikelihood.getPlace().getLatLng();

                                    i++;
                                    if (i > (count - 1)) {
                                        break;
                                    }
                                }

                                // Release the place likelihood buffer, to avoid memory leaks.
                                likelyPlaces.release();

                                // Show a dialog offering the user the list of likely places, and add a
                                // marker at the selected place.
                                openPlacesDialog();

                            } else {
                                Log.e(TAG, "Exception: %s", task.getException());
                            }
                        }
                    });
        } else {
            // The user has not granted permission.
            Log.i(TAG, "The user did not grant location permission.");

            // Add a default marker, because the user hasn't selected a place.
            mMap.addMarker(new MarkerOptions()
                    .title(getString(R.string.default_info_title))
                    .position(mDefaultLocation)
                    .snippet(getString(R.string.default_info_snippet)));

            // Prompt the user for permission.
            getLocationPermission();
        }
    }

    /**
     * Displays a form allowing the user to select a place from a list of likely places.
     */
    private void openPlacesDialog() {
        // Ask the user to choose the place where they are now.
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // The "which" argument contains the position of the selected item.
                LatLng markerLatLng = mLikelyPlaceLatLngs[which];
                String markerSnippet = mLikelyPlaceAddresses[which];
                if (mLikelyPlaceAttributions[which] != null) {
                    markerSnippet = markerSnippet + "\n" + mLikelyPlaceAttributions[which];
                }

                // Add a marker for the selected place, with an info window
                // showing information about that place.
                mMap.addMarker(new MarkerOptions()
                        .title(mLikelyPlaceNames[which])
                        .position(markerLatLng)
                        .snippet(markerSnippet));

                // Position the map's camera at the location of the marker.
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerLatLng,
                        DEFAULT_ZOOM));
            }
        };

        // Display the dialog.
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.pick_place)
                .setItems(mLikelyPlaceNames, listener)
                .show();
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void drawMarker(LatLng position){
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bmp = Bitmap.createBitmap(110, 110, conf);
        Canvas canvas1 = new Canvas(bmp);

// paint defines the text color, stroke width and size
        Paint color = new Paint();
        color.setTextSize(35);
        color.setColor(Color.BLACK);

// modify canvas
        canvas1.drawBitmap(BitmapFactory.decodeResource(getResources(),
                R.drawable.fuel_green_mini), 0,0, color);
        canvas1.drawText("User Name!", 30, 40, color);

// add marker to Map
        mMap.addMarker(new MarkerOptions()
                .position(position)
                .icon(BitmapDescriptorFactory.fromBitmap(bmp))
                // Specifies the anchor to be at a particular point in the marker image.
                .anchor(0.5f, 1));
    }

    private String readFromFile(Context context, File file) {

        String ret = "";

        try {
            InputStream inputStream = null;

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }

    public static String getStringFromFile (File file) throws Exception {
        FileInputStream fin = new FileInputStream(file);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public void getNegoVerificacion(){
        progressDialog.setMessage("Cargando datos... Nego Verificacion");
        final String type = "NV";
        AsyncHttpClientManagement.get(Vars.NEGV, null, new FileAsyncHttpResponseHandler(getApplicationContext()) {

            @Override
            public void onSuccess(int statusCode, Header[] headers, File response) {

                try {

                    String result = getStringFromFile(file);
                    // Log.i("res", result);
                    JSONArray array = new JSONArray(result);

                    for(int i = 0;i<array.length();i++){
                        // Log.i("item", array.getJSONObject(i).getString("id"));




                        if (array.getJSONObject(i).getJSONObject("properties").get("MUNICIPIO").equals("Aguascalientes")){

                            String id = array.getJSONObject(i).getString("id");
                            LatLng latLng = new LatLng( Double.parseDouble(array.getJSONObject(i).getJSONObject("geometry").getJSONArray("coordinates").getString(1)), Double.parseDouble(array.getJSONObject(i).getJSONObject("geometry").getJSONArray("coordinates").getString(0)));
                            String magna = array.getJSONObject(i).getJSONObject("properties").getString("MAGNA");
                            String premium = array.getJSONObject(i).getJSONObject("properties").getString("PREMIUM");
                            String diesel = array.getJSONObject(i).getJSONObject("properties").getString("DIESEL");
                            String colonia = array.getJSONObject(i).getJSONObject("properties").getString("COLONIA");
                            String calle = array.getJSONObject(i).getJSONObject("properties").getString("CALLE");
                            String municipio = array.getJSONObject(i).getJSONObject("properties").getString("MUNICIPIO");
                            String estado = array.getJSONObject(i).getJSONObject("properties").getString("ESTADO");



                            mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title("M: " + magna + "\n" + "P: " + premium + "\n" + "D: " + diesel)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.fuel_orange_mini))).setTag(markers.size());

                            markers.add(new CustomMark(id, null, latLng, premium, magna, diesel, null, colonia, calle,municipio, null));
                        }
                    }

                    // progressDialog.dismiss();

                } catch (Exception e) {
                    progressDialog.dismiss();
                    Log.w("error-marks", e);
                    Toast.makeText(getApplicationContext(), "No es posible obtener los datos de Nego Verificacion", Toast.LENGTH_LONG).show();
                    e.printStackTrace();

                }

                getSinVerificar();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), "No es posible obtener los datos de Nego Verificacion status: " + statusCode, Toast.LENGTH_LONG).show();

            }


        });

    }

    public void getSinVerificar(){
        final String type = "SV";
        progressDialog.setMessage("Cargando datos... Sin Verificar");
        AsyncHttpClientManagement.get(Vars.SVER, null, new FileAsyncHttpResponseHandler(getApplicationContext()) {

            @Override
            public void onSuccess(int statusCode, Header[] headers, File response) {

                try {

                    String result = getStringFromFile(file);
                    // Log.i("res", result);

                    JSONArray array = new JSONArray(result);

                    for(int i = 0;i<array.length();i++){
                        // Log.i("item", array.getJSONObject(i).getString("id"));




                        if (array.getJSONObject(i).getJSONObject("properties").get("MUNICIPIO").equals("Aguascalientes")){

                            String id = array.getJSONObject(i).getString("id");
                            LatLng latLng = new LatLng( Double.parseDouble(array.getJSONObject(i).getJSONObject("geometry").getJSONArray("coordinates").getString(1)), Double.parseDouble(array.getJSONObject(i).getJSONObject("geometry").getJSONArray("coordinates").getString(0)));
                            String magna = array.getJSONObject(i).getJSONObject("properties").getString("MAGNA");
                            String premium = array.getJSONObject(i).getJSONObject("properties").getString("PREMIUM");
                            String diesel = array.getJSONObject(i).getJSONObject("properties").getString("DIESEL");
                            String colonia = array.getJSONObject(i).getJSONObject("properties").getString("COLONIA");
                            String calle = array.getJSONObject(i).getJSONObject("properties").getString("CALLE");
                            String municipio = array.getJSONObject(i).getJSONObject("properties").getString("MUNICIPIO");
                            String estado = array.getJSONObject(i).getJSONObject("properties").getString("ESTADO");



                            mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title("M: " + magna + "\n" + "P: " + premium + "\n" + "D: " + diesel)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.fuel_yellow_mini))).setTag(markers.size());

                            markers.add(new CustomMark(id, null, latLng, premium, magna, diesel, null, colonia, calle,municipio, null));
                        }
                    }

                    getConAnomalias();

                } catch (Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Error al obtener datos sin verificar", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), "No es posible obtener la informacion de Datos sin Verificar, status: " + statusCode, Toast.LENGTH_LONG).show();

            }


        });

    }

    public void getConAnomalias(){
        final String type = "CA";
        progressDialog.setMessage("Cargando datos... Con Anomalias...");
        AsyncHttpClientManagement.get(Vars.CANOM, null, new FileAsyncHttpResponseHandler(getApplicationContext()) {

            @Override
            public void onSuccess(int statusCode, Header[] headers, File response) {

                try {

                    String result = getStringFromFile(file);
                    // Log.i("res", result);

                     JSONArray array = new JSONArray(result);

                    for(int i = 0;i<array.length();i++){
                        // Log.i("item", array.getJSONObject(i).getString("id"));




                        if (array.getJSONObject(i).getJSONObject("properties").get("MUNICIPIO").equals("Aguascalientes")){

                            String id = array.getJSONObject(i).getString("id");
                            LatLng latLng = new LatLng( Double.parseDouble(array.getJSONObject(i).getJSONObject("geometry").getJSONArray("coordinates").getString(1)), Double.parseDouble(array.getJSONObject(i).getJSONObject("geometry").getJSONArray("coordinates").getString(0)));
                            String magna = array.getJSONObject(i).getJSONObject("properties").getString("MAGNA");
                            String premium = array.getJSONObject(i).getJSONObject("properties").getString("PREMIUM");
                            String diesel = array.getJSONObject(i).getJSONObject("properties").getString("DIESEL");
                            String colonia = array.getJSONObject(i).getJSONObject("properties").getString("COLONIA");
                            String calle = array.getJSONObject(i).getJSONObject("properties").getString("CALLE");
                            String municipio = array.getJSONObject(i).getJSONObject("properties").getString("MUNICIPIO");
                            String estado = array.getJSONObject(i).getJSONObject("properties").getString("ESTADO");



                            mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title("M: " + magna + "\n" + "P: " + premium + "\n" + "D: " + diesel)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.fuel_red_mini))).setTag(markers.size());

                            markers.add(new CustomMark(id, null, latLng, premium, magna, diesel, null, colonia, calle,municipio, null));
                        }
                    }

                    getSinAnomalias();

                } catch (Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "No es posbile cargar los datos Con Anomalias", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), "No es posible obtener los datos Con Anomalias, status: " + statusCode, Toast.LENGTH_LONG).show();

            }


        });

    }

    public void getSinAnomalias(){
        final String type = "SA";
        progressDialog.setMessage("Cargando datos... Sin Anomalias");
        AsyncHttpClientManagement.get(Vars.SANOM, null, new FileAsyncHttpResponseHandler(getApplicationContext()) {

            @Override
            public void onSuccess(int statusCode, Header[] headers, File response) {

                try {

                    String result = getStringFromFile(file);
                    // Log.i("res", result);


                    JSONArray array = new JSONArray(result);

                    for(int i = 0;i<array.length();i++){
                        // Log.i("item", array.getJSONObject(i).getString("id"));




                        if (array.getJSONObject(i).getJSONObject("properties").get("MUNICIPIO").equals("Aguascalientes")){

                            String id = array.getJSONObject(i).getString("id");
                            LatLng latLng = new LatLng( Double.parseDouble(array.getJSONObject(i).getJSONObject("geometry").getJSONArray("coordinates").getString(1)), Double.parseDouble(array.getJSONObject(i).getJSONObject("geometry").getJSONArray("coordinates").getString(0)));
                            String magna = array.getJSONObject(i).getJSONObject("properties").getString("MAGNA");
                            String premium = array.getJSONObject(i).getJSONObject("properties").getString("PREMIUM");
                            String diesel = array.getJSONObject(i).getJSONObject("properties").getString("DIESEL");
                            String colonia = array.getJSONObject(i).getJSONObject("properties").getString("COLONIA");
                            String calle = array.getJSONObject(i).getJSONObject("properties").getString("CALLE");
                            String municipio = array.getJSONObject(i).getJSONObject("properties").getString("MUNICIPIO");
                            String estado = array.getJSONObject(i).getJSONObject("properties").getString("ESTADO");



                            mMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title("M: " + magna + "\n" + "P: " + premium + "\n" + "D: " + diesel)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.fuel_green_mini))).setTag(markers.size());

                            markers.add(new CustomMark(id, null, latLng, premium, magna, diesel, null, colonia, calle,municipio, null));
                        }
                    }

                    progressDialog.dismiss();

                } catch (Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "No es posible Obtener los datos de Sin Anomalias", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), "No es posible obtener los datos de Sin Anomalias, status: " + statusCode, Toast.LENGTH_LONG).show();

            }


        });
    }

    private void viewMenu() {

        if (!isOpen) {

            int x = layoutContent.getRight();
            int y = layoutContent.getTop();

            int startRadius = 0;
            int endRadius = (int) Math.hypot(layoutMain.getWidth(), layoutMain.getHeight());

            // fab.setBackgroundTintList(ColorStateList.valueOf(ResourcesCompat.getColor(getResources(),android.R.color.white,null)));
            // fab.setImageResource(R.drawable.ic_close_grey);

            Animator anim = ViewAnimationUtils.createCircularReveal(layoutButtons, x, y, startRadius, endRadius);

            layoutButtons.setVisibility(View.VISIBLE);
            anim.start();

            isOpen = true;

        } else {

            int x = layoutButtons.getRight();
            int y = layoutButtons.getTop();

            int startRadius = Math.max(layoutContent.getWidth(), layoutContent.getHeight());
            int endRadius = 0;

            // fab.setBackgroundTintList(ColorStateList.valueOf(ResourcesCompat.getColor(getResources(),R.color.colorAccent,null)));
            // fab.setImageResource(R.drawable.ic_plus_white);

            Animator anim = ViewAnimationUtils.createCircularReveal(layoutButtons, x, y, startRadius, endRadius);
            anim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    layoutButtons.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });
            anim.start();

            isOpen = false;
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        // CustomMark mark = markers.get(Integer.parseInt(marker.getTag().toString()));

        marker.showInfoWindow();

        return true;
    }
}