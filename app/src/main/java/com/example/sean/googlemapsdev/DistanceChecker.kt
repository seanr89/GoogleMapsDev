package com.example.sean.googlemapsdev

import android.location.Location
import com.google.android.gms.maps.model.LatLng

object DistanceChecker
{
    private const val DISTANCE_LIMIT = 500
    /**
     * query and return if the current location is near the default office location
     * @param onComplete :
     */
    fun isNearLocation(currentLatLng : LatLng) : Boolean
    {
        var officeLatLng = getOfficeLocation()
        var selectedLocation = Location("")
        selectedLocation.latitude = officeLatLng.latitude
        selectedLocation.longitude = officeLatLng.longitude

        var currentLocation = Location("")
        currentLocation.latitude = currentLatLng.latitude
        currentLocation.longitude = currentLatLng.longitude

        val distance = currentLocation.distanceTo(selectedLocation)
        return distance < DISTANCE_LIMIT
    }

    fun getOfficeLocation() : LatLng
    {
        var latLng = LatLng(54.617106, -5.941409)
        return latLng
    }
}