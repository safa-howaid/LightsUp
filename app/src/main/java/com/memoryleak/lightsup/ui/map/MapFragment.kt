package com.memoryleak.lightsup.ui.map

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.MapboxMap.OnCameraMoveListener
import com.mapbox.mapboxsdk.maps.MapboxMap.OnMapClickListener
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.maps.Style.OnStyleLoaded
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.VectorSource
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.ui.NavigationViewOptions
import com.mapbox.navigation.ui.OnNavigationReadyCallback
import com.mapbox.navigation.ui.listeners.NavigationListener
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.memoryleak.lightsup.R
import com.memoryleak.lightsup.databinding.FragmentMapBinding
import com.memoryleak.lightsup.databinding.FragmentNavigationMapBinding
import java.util.*

class MapFragment : Fragment(), OnMapReadyCallback, PermissionsListener {

    private lateinit var mapViewModel: MapViewModel
    private var _binding: FragmentMapBinding? = null
    private val ZOOM_THRESHOLD = 13
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var mapboxMap: MapboxMap
    private lateinit var originPosition: Point
    private lateinit var destinationPosition: Point
    private var destinationMarker: Marker? = null
    private lateinit var mapboxNavigation: MapboxNavigation

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mapViewModel =
            ViewModelProvider(this).get(MapViewModel::class.java)

        Mapbox.getInstance(container!!.context, getString(R.string.mapbox_access_token))
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this);
        val navigationOptions = MapboxNavigation
            .defaultNavigationOptionsBuilder(requireContext(), getString(R.string.mapbox_access_token))
            .build()
        mapboxNavigation = MapboxNavigationProvider.create(navigationOptions)

        return binding.root
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(
            Style.DARK
        ) { style ->
            enableLocationComponent(style)
            val vectorSource = VectorSource(
                "lights-source",
                "https://api.mapbox.com/v4/safa-howaid.bgp8x03h.json?access_token=" + getString(R.string.mapbox_access_token)
            )
            style.addSource(vectorSource)
            val circleLayer = CircleLayer("lights-style", "lights-source")
            circleLayer.setSourceLayer("Street_Lights-3rqope")
            circleLayer.withProperties(
                PropertyFactory.circleOpacity(0.2f),
                PropertyFactory.circleColor(Color.parseColor("#FFC300")),
                PropertyFactory.circleRadius(5f)
            )
            style.addLayer(circleLayer)

            binding.lightsLayerToggle.setOnClickListener {
                toggleLayer()
            }

            mapboxMap.addOnCameraMoveListener {
                val lightsLayer = style.getLayer("lights-style")
                if (mapboxMap.cameraPosition.zoom > this.ZOOM_THRESHOLD) {
                    if (lightsLayer != null) {
                        circleLayer.setProperties(
                            PropertyFactory.circleRadius(5f)
                        )
                    }
                } else {
                    if (lightsLayer != null) {
                        circleLayer.setProperties(
                            PropertyFactory.circleRadius(2.5f)
                        )
                    }
                }
            }

            mapboxMap.addOnMapClickListener { point ->
                if (destinationMarker != null) {
                    mapboxMap.removeMarker(destinationMarker!!)
                }
                destinationMarker = mapboxMap.addMarker(MarkerOptions().position(point))
                destinationPosition =
                    Point.fromLngLat(point.longitude, point.latitude)
                val lastKnownLocation = mapboxMap.locationComponent.lastKnownLocation
                originPosition = Point.fromLngLat(
                    lastKnownLocation!!.longitude,
                    lastKnownLocation.latitude
                )
//                getRoute(originPosition, destinationPosition)
                println("ORIGIN: $originPosition")
                println("DESTINATION: $destinationPosition")
                binding.startNavigation.visibility = View.VISIBLE
                true
            }
        }
    }

    private fun getRoute(origin: Point, destination: Point) {
        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .accessToken(getString(R.string.mapbox_access_token))
                .coordinates(listOf(origin, destination))
                .build(),
            object : RoutesRequestCallback {

                override fun onRoutesReady(routes: List<DirectionsRoute>) {
                    println("routes ready")
                }

                override fun onRoutesRequestFailure(
                    throwable: Throwable,
                    routeOptions: RouteOptions
                ) {
                    println("fail")
                }

                override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
                    println("cancelled")
                }
            }
        )
    }

    private fun toggleLayer() {
        mapboxMap!!.getStyle { style ->
            val layer = style.getLayer("lights-style")
            if (layer != null) {
                if (Property.VISIBLE == layer.visibility.getValue()) {
                    layer.setProperties(PropertyFactory.visibility(Property.NONE))
                } else {
                    layer.setProperties(PropertyFactory.visibility(Property.VISIBLE))
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(requireContext())) {
            val locationComponentOptions = LocationComponentOptions.builder(requireContext())
                .foregroundTintColor(Color.YELLOW)
                .bearingTintColor(Color.YELLOW)
                .accuracyColor(Color.YELLOW)
                .pulseEnabled(true)
                .pulseColor(Color.YELLOW)
                .build()
            val locationComponentActivationOptions = LocationComponentActivationOptions
                .builder(requireContext(), loadedMapStyle)
                .locationComponentOptions(locationComponentOptions)
                .build()

            // Get an instance of the component
            val locationComponent = mapboxMap.locationComponent

            // Activate with options
            locationComponent.activateLocationComponent(locationComponentActivationOptions)

            // Enable to make component visible
            locationComponent.isLocationComponentEnabled = true

            // Set the component's camera mode
            locationComponent.cameraMode = CameraMode.TRACKING

            // Set the component's render mode
            locationComponent.renderMode = RenderMode.COMPASS
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(activity)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager!!.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String?>?) {
        Toast.makeText(requireContext(), R.string.user_location_permission_explanation, Toast.LENGTH_LONG)
            .show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            mapboxMap!!.getStyle { style -> enableLocationComponent(style) }
        } else {
            Toast.makeText(requireContext(), R.string.user_location_permission_not_granted, Toast.LENGTH_LONG)
                .show()
        }
    }
    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.onDestroy()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        binding.mapView.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val TAG = "MapFragment"
    }
}