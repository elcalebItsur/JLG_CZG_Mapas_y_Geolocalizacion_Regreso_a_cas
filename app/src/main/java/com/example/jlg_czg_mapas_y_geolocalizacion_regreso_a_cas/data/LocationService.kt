package com.example.jlg_czg_mapas_y_geolocalizacion_regreso_a_cas.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

class LocationService(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        return try {
            client.lastLocation.await()
        } catch (e: Exception) {
            null
        }
    }
}
