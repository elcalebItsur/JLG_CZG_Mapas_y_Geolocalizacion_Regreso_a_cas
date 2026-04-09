package com.example.jlg_czg_mapas_y_geolocalizacion_regreso_a_cas.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jlg_czg_mapas_y_geolocalizacion_regreso_a_cas.data.HomePreferences
import com.example.jlg_czg_mapas_y_geolocalizacion_regreso_a_cas.data.LocationService
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val locationService = remember { LocationService(context) }
    val homePreferences = remember { HomePreferences(context) }
    val viewModel: MapViewModel = viewModel(factory = MapViewModelFactory(locationService, homePreferences))
    
    val currentLocation by viewModel.currentLocation.collectAsState()
    val homeLocation by viewModel.homeLocation.collectAsState()
    
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(20.6597, -103.3496))
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            viewModel.updateLocation()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            viewModel.updateLocation()
        } else {
            launcher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    // Actualizar marcadores cuando cambian las ubicaciones
    LaunchedEffect(currentLocation, homeLocation) {
        mapView.overlays.clear()
        
        currentLocation?.let {
            val marker = Marker(mapView)
            marker.position = it
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = "Tu ubicación"
            mapView.overlays.add(marker)
        }
        
        homeLocation?.let {
            val marker = Marker(mapView)
            marker.position = it
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = "Casa"
            // Diferenciar el marcador de casa
            marker.icon = ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.marker_default)
            // Podríamos usar un color diferente si tuviéramos assets, por ahora se queda por defecto o tintado
            mapView.overlays.add(marker)
        }
        
        mapView.invalidate()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Botón para ir a casa (o establecerla si no existe)
            FloatingActionButton(
                onClick = { 
                    if (homeLocation == null) {
                        // Usar las coordenadas proporcionadas por el usuario
                        viewModel.saveHome(20.13956086886095, -101.15067370665088)
                    } else {
                        homeLocation?.let { mapView.controller.animateTo(it) }
                    }
                },
                modifier = Modifier.padding(bottom = 8.dp),
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Default.Home, contentDescription = "Casa")
            }

            // Botón Mi Ubicación
            FloatingActionButton(
                onClick = { 
                    viewModel.updateLocation()
                    currentLocation?.let { mapView.controller.animateTo(it) }
                }
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación")
            }
        }
        
        // Indicador de "Casa no establecida"
        if (homeLocation == null) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    "Pulsa el botón de casa para establecer destino",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onDetach()
        }
    }
}

class MapViewModelFactory(
    private val locationService: LocationService,
    private val homePreferences: HomePreferences
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return MapViewModel(locationService, homePreferences) as T
    }
}
