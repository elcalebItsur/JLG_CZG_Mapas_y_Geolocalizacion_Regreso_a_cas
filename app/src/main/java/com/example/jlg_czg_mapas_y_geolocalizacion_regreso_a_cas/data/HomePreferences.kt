package com.example.jlg_czg_mapas_y_geolocalizacion_regreso_a_cas.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class HomePreferences(context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val KEY_HOME_LAT = doublePreferencesKey("home_lat")
        val KEY_HOME_LNG = doublePreferencesKey("home_lng")
    }

    val homeLocation: Flow<Pair<Double, Double>?> = dataStore.data.map { preferences ->
        val lat = preferences[KEY_HOME_LAT]
        val lng = preferences[KEY_HOME_LNG]
        if (lat != null && lng != null) lat to lng else null
    }

    suspend fun saveHomeLocation(lat: Double, lng: Double) {
        dataStore.edit { preferences ->
            preferences[KEY_HOME_LAT] = lat
            preferences[KEY_HOME_LNG] = lng
        }
    }
}
