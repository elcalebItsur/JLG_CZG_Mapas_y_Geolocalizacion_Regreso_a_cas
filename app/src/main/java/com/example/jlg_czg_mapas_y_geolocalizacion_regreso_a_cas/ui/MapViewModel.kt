package com.example.jlg_czg_mapas_y_geolocalizacion_regreso_a_cas.ui

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jlg_czg_mapas_y_geolocalizacion_regreso_a_cas.data.HomePreferences
import com.example.jlg_czg_mapas_y_geolocalizacion_regreso_a_cas.data.LocationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class MapViewModel(
    private val locationService: LocationService,
    private val homePreferences: HomePreferences
) : ViewModel() {
    
    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    val homeLocation = homePreferences.homeLocation.map { pair ->
        pair?.let { GeoPoint(it.first, it.second) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateLocation() {
        viewModelScope.launch {
            val location: Location? = locationService.getCurrentLocation()
            location?.let {
                _currentLocation.value = GeoPoint(it.latitude, it.longitude)
            }
        }
    }

    fun saveHome(lat: Double, lng: Double) {
        viewModelScope.launch {
            homePreferences.saveHomeLocation(lat, lng)
        }
    }
}
