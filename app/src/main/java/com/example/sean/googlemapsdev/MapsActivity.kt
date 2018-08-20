package com.example.sean.googlemapsdev

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException
import java.util.*

/*
A few useful links
https://developers.google.com/maps/documentation/android-sdk/current-place-tutorial
https://www.raywenderlich.com/230-introduction-to-google-maps-api-for-android-with-kotlin
https://github.com/googlemaps/android-samples/blob/master/tutorials/CurrentPlaceDetailsOnMap/app/src/main/java/com/example/currentplacedetailsonmap/MapsActivityCurrentPlace.java
 */

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val TAG = "MapsActivity"

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    //default zoom to use when location permission is not granted
    private val DEFAULT_ZOOM = 10.0F
    //Sydney
    private val mDefaultLocation = LatLng(-33.8523341, 151.2106085)
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    private var mLocationPermissionGranted: Boolean = false
    private lateinit var lastLocation: Location

    lateinit var geofencingClient: GeofencingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //To access the location APIs, you need to create an instance of the Geofencing client
        geofencingClient = LocationServices.getGeofencingClient(this)

        //Key is required to be gotten from google apis - checkout the google_maps_api.xml file in this
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        //set listener for LONG click events!
        mMap.setOnMapLongClickListener {
            if(mLocationPermissionGranted)
                onMapClicked(it)
        }

        mMap.uiSettings.isZoomControlsEnabled = true

        //Draw a marker on the map
        mMap.addMarker(MarkerOptions().position(mDefaultLocation).title("Default Locale"))
        //Move the map camera to over the marker!!
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM))

        setupMap()
    }

    /**
     * Handle on map click event to display the current country location information
     * @param latLng : nullable latitude and longitude data
     */
    private fun onMapClicked(latLng: LatLng?) {
        Log.d(TAG, object {}.javaClass.enclosingMethod.name)

        val geocoder = Geocoder(this, Locale.getDefault())
        var addresses: List<Address> = emptyList()
        try
        {
            addresses = geocoder.getFromLocation(
                    latLng!!.latitude,
                    latLng!!.longitude,
                    // In this sample, we get just a single address.
                    1)
        }
        catch(ioException: IOException)
        {
            // Catch network or other I/O problems.
            Log.e(TAG, ioException.message)
        }
        catch(illegalArgumentException: IllegalArgumentException)
        {
            // Catch invalid latitude or longitude values.
            Log.e(TAG, illegalArgumentException.message)
        }
        if (addresses.isNotEmpty()) {
            val address = addresses[0]
            Toast.makeText(this, "Clicked : ${address.countryName}", Toast.LENGTH_LONG).show()
        }

        //Now drop the pin/marker on the map!!
        dropAPin(latLng)
    }

    /**
     * Initialise map settings including checking for permissions and requesting device location if allowed
     */
    private fun setupMap()
    {
        //1. Checks if the app has been granted the ACCESS_FINE_LOCATION permission. If it hasn’t, then request it from the user.
        getLocationPermission()

        //2 Turn on the My Location layer and the related control on the map.
        //i.e. go to my location
        updateLocationUI()

        //3 - check and see if fine location is enabled then attempt to get the last known position
        getDeviceLocation()
    }

    /**
     * Check and prompt user to give access to location
     */
    private fun getLocationPermission()
    {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                        android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            mLocationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    /**
     * Overridden
     * Handles the result of the request for location permissions.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] === PackageManager.PERMISSION_GRANTED)
                {
                    mLocationPermissionGranted = true
                    getDeviceLocation()
                }
            }
        }
        updateLocationUI()
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private fun getDeviceLocation()
    {
        /*
        * Get the best and most recent location of the device, which may be null in rare
        * cases when a location is not available.
        */
        try {
            if(mLocationPermissionGranted)
            fusedLocationClient.lastLocation.addOnCompleteListener()
            {
                val location = it.result
                if(location != null)
                {
                    lastLocation = location
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM))

                    requestCurrentCountry()
                }
            }
        }
        catch (e: SecurityException)
        {
            Log.e("Exception: %s", e.message)
        }
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    @SuppressLint("MissingPermission")
    private fun updateLocationUI()
    {
        if (mMap == null) {
            return
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.isMyLocationEnabled = true
                mMap.uiSettings.isMyLocationButtonEnabled = true
            }
            else{
                mMap.isMyLocationEnabled = false
                mMap.uiSettings.isMyLocationButtonEnabled = false
                getLocationPermission()
            }
        }
        catch(e : SecurityException)
        {
            Log.e("Exception: %s", e.message)
        }
    }

    /**
     * request and display the current country location as a toast message!
     */
    private fun requestCurrentCountry()
    {
        Log.d(TAG,  object{}.javaClass.enclosingMethod.name)

        //if permission has not been granted
        if(!mLocationPermissionGranted)
        {
            return
        }

        var addresses: List<Address> = emptyList()

        try {

            val geocoder = Geocoder(this, Locale.getDefault())

            addresses = geocoder.getFromLocation(
                    lastLocation.latitude,
                    lastLocation.longitude,
                    // In this sample, we get just a single address.
                    1)
        }
        catch(ioException: IOException)
        {
            // Catch network or other I/O problems.
            Log.e(TAG, ioException.message)
        }
        catch(illegalArgumentException: IllegalArgumentException)
        {
            // Catch invalid latitude or longitude values.
            Log.e(TAG, illegalArgumentException.message)
        }

        // Handle case where no address was found.
        if(addresses.isEmpty())
        {
            Log.d(TAG, "No Addresses")
        }
        else
        {
            //get the current address
            val address = addresses[0]
            Toast.makeText(this, "Country : ${address.countryName}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * handle the dropping of a new pin on the map!!
     * @param latLng
     */
    private fun dropAPin(latLng: LatLng?)
    {
        Log.d(TAG,  object{}.javaClass.enclosingMethod.name)

        if(latLng != null)
        {
            mMap.addMarker(MarkerOptions().position(latLng).title(createPinInfoFromLocation(latLng)))
        }
    }

    /**
     * test code
     */
    private fun createPinInfoFromLocation(latLng: LatLng?) : String
    {
        Log.d(TAG,  object{}.javaClass.enclosingMethod.name)

        var result = "Unknown"

        val address =  getAddressFromLatLng(latLng)
        if(address != null)
        {
            result = "Country : ${address.countryName} - Code : ${address.countryCode}"
        }

        return result
    }

    /**
     * test code
     */
    private fun getAddressFromLatLng(latLng: LatLng?) : Address
    {
        Log.d(TAG,  object{}.javaClass.enclosingMethod.name)

        if(latLng == null)
        {
            var addresses: List<Address> = emptyList()

            try {

                val geocoder = Geocoder(this, Locale.getDefault())

                addresses = geocoder.getFromLocation(
                        latLng!!.latitude,
                        latLng!!.longitude,
                        // In this sample, we get just a single address.
                        1)
            }
            catch(ioException: IOException)
            {
                // Catch network or other I/O problems.
                Log.e(TAG, ioException.message)
            }
            catch(illegalArgumentException: IllegalArgumentException)
            {
                // Catch invalid latitude or longitude values.
                Log.e(TAG, illegalArgumentException.message)
            }

            if(addresses.isNotEmpty())
            {
                return addresses[0]
            }
        }
        return null!!
    }

    /**
     * create a geofence, setting the desired radius, duration, and transition types for the geofence
     */
    private fun initialiseGeofenceItems()
    {
        Log.d(TAG,  object{}.javaClass.enclosingMethod.name)

        var geoFenceList : ArrayList<Geofence> = arrayListOf()
//
//        geoFenceList.add(Geofence.Builder()
//                // Set the request ID of the geofence. This is a string to identify this
//                // geofence.
//                .setRequestId(entry.key)
//
//                // Set the circular region of this geofence.
//                .setCircularRegion(
//                        entry.value.latitude,
//                        entry.value.longitude,
//                        GEOFENCE_RADIUS_IN_METERS
//        )
    }

    //static companion object data!!
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

        private const val GEOFENCE_RADIUS_IN_METERS = 100
    }
}
