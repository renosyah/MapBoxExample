package com.syahputrareno975.mapboxexample

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.location.*
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import java.lang.Exception
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.location.Location
import android.media.Image
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.mapbox.android.core.location.*
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.plugins.markerview.MarkerView
import com.mapbox.mapboxsdk.plugins.markerview.MarkerViewManager
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    lateinit var context :Context
    lateinit var mapView: MapView
    lateinit var mapboxMap: MapboxMap

    lateinit var locationComponent: LocationComponent
    lateinit var permissionsManager : PermissionsManager
    lateinit var markerViewManager : MarkerViewManager
    lateinit var locationEngine :LocationEngine

    lateinit var getCurrentLocation : ImageView
    lateinit var setCurrentLocation  :ImageView

    var isInTrackingMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(applicationContext, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_main)
        initWidget()
    }

    fun initWidget(){
        this.context = this@MainActivity

        getCurrentLocation = findViewById(R.id.get_current_location)
        setCurrentLocation = findViewById(R.id.set_current_location)

        mapView = findViewById(R.id.mapView)
        mapView.getMapAsync(onMapCallback)

    }

    val onMapCallback = object : OnMapReadyCallback {
        override fun onMapReady(b: MapboxMap) {
            mapboxMap = b
            markerViewManager = MarkerViewManager(mapView, mapboxMap)

            mapboxMap.setStyle(Style.LIGHT) { style ->
                enableLocationComponent(style)
            }

            mapboxMap.addOnMapClickListener(object : MapboxMap.OnMapClickListener {
                override fun onMapClick(point: LatLng): Boolean {
                    mapboxMap.clear()
                    mapboxMap.addMarker(createSimpleMarker(point))
                    return true
                }
            })

            mapboxMap.setOnMarkerClickListener(object : MapboxMap.OnMarkerClickListener {
                override fun onMarkerClick(marker: Marker): Boolean {

                    Toast.makeText(context,"${marker.title}",Toast.LENGTH_SHORT).show()
                    AlertDialog.Builder(context)
                        .setTitle("My Marker")
                        .setMessage(confirmMarker(marker.snippet))
                        .setPositiveButton("Close",null)
                        .create()
                        .show()

                    // mapboxMap.removeMarker(marker)

                    return true
                }
            })
        }
    }



    fun addCustomMarker(point: LatLng) {
        val customView = LayoutInflater.from(context).inflate(R.layout.marker_view_bubble, null)
        customView.layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        val title :TextView = customView.findViewById(R.id.marker_window_title)
        title.text = "Marker"

        val des :TextView = customView.findViewById(R.id.marker_window_snippet)
        des.text = "New Marker Set"

        val marker = MarkerView(point, customView)
        marker.let {
            markerViewManager.addMarker(it)
        }
    }

    fun createSimpleMarker(point: LatLng) : MarkerOptions {
        return MarkerOptions()
            .setTitle("Marker")
            .setSnippet("ABC")
            .setPosition(point)
    }

    fun confirmMarker(id : String) : String {
        if (id == "ABC") {
            return "My New Marker"
        }
        return ""
    }

    fun initLocationEngine(){

        locationEngine = LocationEngineProvider.getBestLocationEngine(context)
        val request = LocationEngineRequest.Builder(1000L)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setMaxWaitTime(1000L * 5).build()

        locationEngine.requestLocationUpdates(request,object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult?) {
                //Toast.makeText(context,"User Location change : ${result!!.lastLocation!!.latitude} ${result.lastLocation!!.longitude}",Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(exception: Exception) {

            }

        }, Looper.getMainLooper())

    }

    fun enableLocationComponent(loadedMapStyle : Style) {
        if (PermissionsManager.areLocationPermissionsGranted(context)) {

            val customLocationComponentOptions = LocationComponentOptions.builder(context)
                .elevation(5f)
                .accuracyAlpha(.6f)
                .accuracyColor(Color.RED)
                .foregroundDrawable(android.R.drawable.ic_menu_mylocation)
                .build()

            locationComponent = mapboxMap.locationComponent

            val locationComponentActivationOptions = LocationComponentActivationOptions.builder(context, loadedMapStyle)
                    .locationComponentOptions(customLocationComponentOptions)
                    .build()

            initLocationEngine()

            locationComponent.activateLocationComponent(locationComponentActivationOptions)
            locationComponent.setLocationEngine(locationEngine)
            locationComponent.setLocationComponentEnabled(true)
            locationComponent.setCameraMode(CameraMode.TRACKING)
            locationComponent.setRenderMode(RenderMode.COMPASS)
            locationComponent.addOnLocationClickListener(onLocationClick)
            locationComponent.addOnCameraTrackingChangedListener(onOnCameraTrackingChanged)

            getCurrentLocation.setOnClickListener{
                if (!isInTrackingMode) {
                    isInTrackingMode = true
                    locationComponent.setCameraMode(CameraMode.TRACKING)
                    locationComponent.zoomWhileTracking(16.0)
                    Toast.makeText(context, "enable tracking", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "tracking already enable", Toast.LENGTH_SHORT).show()
                }
            }
            setCurrentLocation.setOnClickListener{
                mapboxMap.clear()
                mapboxMap.addMarker(createSimpleMarker(mapboxMap.cameraPosition.target))
            }

        } else {
            permissionsManager = PermissionsManager(onPermissionRequest)
            permissionsManager.requestLocationPermissions(this)
        }

    }

    val onLocationClick = object : OnLocationClickListener {
        override fun onLocationComponentClick() {
            if (locationComponent.getLastKnownLocation() != null) {
                Toast.makeText(context, "location on ${locationComponent.lastKnownLocation!!.latitude} ${locationComponent.lastKnownLocation!!.longitude}", Toast.LENGTH_LONG).show()
            }
        }

    }

    val onOnCameraTrackingChanged = object : OnCameraTrackingChangedListener {
        override fun onCameraTrackingChanged(currentMode: Int) {

        }

        override fun onCameraTrackingDismissed() {
            isInTrackingMode = false
        }

    }

    val onPermissionRequest = object : PermissionsListener {
        override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {

        }

        override fun onPermissionResult(granted: Boolean) {
            if (granted) {
                mapboxMap.getStyle { style -> enableLocationComponent(style) }
            } else {
                Toast.makeText(context, "permission not granted", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
}
