package com.example.jlg_czg_mapas_y_geolocalizacion_regreso_a_cas.ui

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jlg_czg_mapas_y_geolocalizacion_regreso_a_cas.data.LocationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class MapViewModel(private val locationService: LocationService) : ViewModel() {
    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    fun updateLocation() {
        viewModelScope.launch {
            val location: Location? = locationService.getCurrentLocation()
            location?.let {
                _currentLocation.value = GeoPoint(it.latitude, it.longitude)
            }
        }
    }
}
