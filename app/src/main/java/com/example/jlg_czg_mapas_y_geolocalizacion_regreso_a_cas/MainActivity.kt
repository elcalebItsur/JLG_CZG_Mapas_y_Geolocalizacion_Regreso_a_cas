package com.example.jlg_czg_mapas_y_geolocalizacion_regreso_a_cas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.jlg_czg_mapas_y_geolocalizacion_regreso_a_cas.ui.MapScreen
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configuración de OsmDroid (User Agent es obligatorio)
        Configuration.getInstance().userAgentValue = packageName
        
        setContent {
            MaterialTheme {
                Surface {
                    MapScreen()
                }
            }
        }
    }
}
