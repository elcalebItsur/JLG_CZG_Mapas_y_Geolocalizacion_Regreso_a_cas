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
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val locationService = remember { LocationService(context) }
    val homePreferences = remember { HomePreferences(context) }
    val viewModel: MapViewModel = viewModel(factory = MapViewModelFactory(locationService, homePreferences))
    
    val currentLocation by viewModel.currentLocation.collectAsState()
    val homeLocation by viewModel.homeLocation.collectAsState()
    val routePoints by viewModel.routePoints.collectAsState()
    val routeSummary by viewModel.routeSummary.collectAsState()
    val isLoadingRoute by viewModel.isLoadingRoute.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
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

    // Actualizar marcadores y ruta cuando cambian las ubicaciones
    LaunchedEffect(currentLocation, homeLocation, routePoints) {
        mapView.overlays.clear()
        
        // Receptor de eventos para clic largo
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean = false
            override fun longPressHelper(p: GeoPoint?): Boolean {
                p?.let {
                    viewModel.saveHome(it.latitude, it.longitude)
                }
                return true
            }
        }
        mapView.overlays.add(MapEventsOverlay(mapEventsReceiver))

        // Dibujar ruta primero (debajo de los marcadores)
        if (routePoints.isNotEmpty()) {
            val polyline = Polyline()
            polyline.setPoints(routePoints)
            polyline.outlinePaint.color = android.graphics.Color.BLUE
            polyline.outlinePaint.strokeWidth = 10f
            mapView.overlays.add(polyline)
        }

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
            // Usar icono de casa si es posible o simplemente un color distinto
            marker.icon = ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.marker_default)
            marker.icon?.setTint(android.graphics.Color.RED)
            mapView.overlays.add(marker)
        }
        
        mapView.invalidate()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Indicador de carga
        if (isLoadingRoute) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(50.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Resumen de ruta (Distancia y Tiempo)
        routeSummary?.let { summary ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(summary, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        // Mensaje de error
        errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
            ) {
                Text(error)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Botón para ir a casa
            FloatingActionButton(
                onClick = { 
                    homeLocation?.let { 
                        mapView.controller.animateTo(it)
                    } ?: run {
                        // Opcional: mostrar tooltip indicando cómo guardar casa
                    }
                },
                modifier = Modifier.padding(bottom = 8.dp),
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Default.Home, contentDescription = "Ir a Casa")
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
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onDetach()
        }
    }
}
