package com.example.jlg_czg_mapas_y_geolocalizacion_regreso_a_cas.ui

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jlg_czg_mapas_y_geolocalizacion_regreso_a_cas.data.HomePreferences
import com.example.jlg_czg_mapas_y_geolocalizacion_regreso_a_cas.data.LocationService
import com.example.jlg_czg_mapas_y_geolocalizacion_regreso_a_cas.data.repository.RouteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

class MapViewModel(
    private val locationService: LocationService,
    private val homePreferences: HomePreferences,
    private val routeRepository: RouteRepository = RouteRepository()
) : ViewModel() {
    
    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    val homeLocation = homePreferences.homeLocation.map { pair ->
        pair?.let { GeoPoint(it.first, it.second) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _routePoints = MutableStateFlow<List<GeoPoint>>(emptyList())
    val routePoints = _routePoints.asStateFlow()

    private val _routeSummary = MutableStateFlow<String?>(null)
    val routeSummary = _routeSummary.asStateFlow()

    private val _isLoadingRoute = MutableStateFlow(false)
    val isLoadingRoute = _isLoadingRoute.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun updateLocation() {
        viewModelScope.launch {
            val location: Location? = locationService.getCurrentLocation()
            location?.let {
                val newPoint = GeoPoint(it.latitude, it.longitude)
                _currentLocation.value = newPoint
                calculateRouteIfPossible(newPoint, homeLocation.value)
            }
        }
    }

    fun saveHome(lat: Double, lng: Double) {
        viewModelScope.launch {
            homePreferences.saveHomeLocation(lat, lng)
            calculateRouteIfPossible(currentLocation.value, GeoPoint(lat, lng))
        }
    }

    private var lastStart: GeoPoint? = null
    private var lastEnd: GeoPoint? = null

    private fun calculateRouteIfPossible(start: GeoPoint?, end: GeoPoint?) {
        if (start != null && end != null) {
            // Evitar recalcular si los puntos no han cambiado significativamente (o al menos son los mismos)
            if (start == lastStart && end == lastEnd) return
            
            lastStart = start
            lastEnd = end

            viewModelScope.launch {
                _isLoadingRoute.value = true
                _errorMessage.value = null
                try {
                    val response = routeRepository.getRoute(
                        start.latitude, start.longitude,
                        end.latitude, end.longitude
                    )
                    if (response != null && response.features.isNotEmpty()) {
                        val feature = response.features.first()
                        val points = feature.geometry.coordinates.map { GeoPoint(it[1], it[0]) }
                        _routePoints.value = points
                        
                        val distanceKm = feature.properties.summary.distance / 1000.0
                        val durationMin = feature.properties.summary.duration / 60.0
                        _routeSummary.value = String.format("%.2f km - %.0f min", distanceKm, durationMin)
                    } else {
                        _errorMessage.value = "No se pudo encontrar una ruta."
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "Error al conectar con el servidor de rutas."
                } finally {
                    _isLoadingRoute.value = false
                }
            }
        }
    }
}

class MapViewModelFactory(
    private val locationService: LocationService,
    private val homePreferences: HomePreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapViewModel(locationService, homePreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
