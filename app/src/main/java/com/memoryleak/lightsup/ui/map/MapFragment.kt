package com.memoryleak.lightsup.ui.map

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Color.parseColor
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression.*
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.Property.LINE_CAP_ROUND
import com.mapbox.mapboxsdk.style.layers.Property.LINE_JOIN_ROUND
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.style.sources.VectorSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.memoryleak.lightsup.R
import com.memoryleak.lightsup.databinding.FragmentMapBinding


class MapFragment : Fragment(), OnMapReadyCallback, PermissionsListener, MapboxMap.OnMapLongClickListener {

    private val ORIGIN_COLOR = "#32a852" // Green
    private val DESTINATION_COLOR = "#F84D4D" // Red
    private val ZOOM_THRESHOLD = 13

    private var _binding: FragmentMapBinding? = null
    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var originPosition: Point
    private lateinit var destinationPosition: Point
    private lateinit var route: DirectionsRoute

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Mapbox.getInstance(container!!.context, getString(R.string.mapbox_access_token))
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this);
        val navigationOptions = MapboxNavigation
            .defaultNavigationOptionsBuilder(requireContext(), getString(R.string.mapbox_access_token))
            .build()
        mapboxNavigation = MapboxNavigationProvider.create(navigationOptions)

        binding.startNavigation.setOnClickListener {
            val fragmentTransaction: FragmentTransaction = requireActivity()
                .supportFragmentManager.beginTransaction()
            fragmentTransaction.disallowAddToBackStack()
            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            fragmentTransaction.replace(R.id.nav_host_fragment_activity_main, MapNavigationFragment(route, originPosition))
            fragmentTransaction.commit()
        }

        binding.lightsLayerToggle.setOnClickListener {
            toggleLayer()
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.DARK) { style ->
            setMapStyle(style)
        }
    }

    private fun setMapStyle(style: Style) {
        enableLocationComponent(style)

        // Add the destination marker image
        style.addImage(
            "ICON_ID",
            BitmapUtils.getBitmapFromDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.mapbox_marker_icon_default
                )
            )!!
        )

        // Street Lights Layer
        val vectorSource = VectorSource(
            "lights-source",
            "https://api.mapbox.com/v4/safa-howaid.bgp8x03h.json?access_token=" + getString(R.string.mapbox_access_token)
        )
        style.addSource(vectorSource)
        val circleLayer = CircleLayer("LIGHTS_LAYER_ID", "lights-source")
        circleLayer.sourceLayer = "Street_Lights-3rqope"
        circleLayer.withProperties(
            circleOpacity(0.2f),
            circleColor(parseColor("#FFC300")),
            circleRadius(5f)
        )
        style.addLayer(circleLayer)

        style.addLayerAbove(
            LineLayer("ROUTE_LAYER_ID", "ROUTE_LINE_SOURCE_ID")
                .withProperties(
                    lineCap(LINE_CAP_ROUND),
                    lineJoin(LINE_JOIN_ROUND),
                    lineWidth(6f),
                    lineGradient(
                        interpolate(
                            linear(),
                            lineProgress(),
                            stop(0f, color(parseColor(ORIGIN_COLOR))),
                            stop(1f, color(parseColor(DESTINATION_COLOR)))
                        )
                    )
                ), "LIGHTS_LAYER_ID"
        )

        // Add the Symbol Layer to display the destination marker
        style.addLayerAbove(
            SymbolLayer("CLICK_LAYER", "CLICK_SOURCE")
                .withProperties(
                    iconImage("ICON_ID")
                ), "ROUTE_LAYER_ID"
        )

        // Add the click and route sources
        style.addSource(GeoJsonSource("CLICK_SOURCE"))
        style.addSource(
            GeoJsonSource(
                "ROUTE_LINE_SOURCE_ID",
                GeoJsonOptions().withLineMetrics(true)
            )
        )

        mapboxMap.addOnCameraMoveListener {
            val lightsLayer = style.getLayer("LIGHTS_LAYER_ID")
            if (mapboxMap.cameraPosition.zoom > this.ZOOM_THRESHOLD) {
                if (lightsLayer != null) {
                    circleLayer.setProperties(
                        circleRadius(5f)
                    )
                }
            } else {
                if (lightsLayer != null) {
                    circleLayer.setProperties(
                        circleRadius(2.5f)
                    )
                }
            }
        }

        mapboxMap.addOnMapLongClickListener(this)
        Snackbar.make(binding.root, R.string.msg_long_press_map_to_place_waypoint, LENGTH_SHORT)
            .show()
    }

    override fun onMapLongClick(latLng: LatLng): Boolean {
        binding.routeRetrievalProgressSpinner.visibility = VISIBLE
        // Place the destination marker at the map long click location
        mapboxMap.getStyle {
            val clickPointSource = it.getSourceAs<GeoJsonSource>("CLICK_SOURCE")
            clickPointSource?.setGeoJson(Point.fromLngLat(latLng.longitude, latLng.latitude))
        }
        mapboxMap.locationComponent.lastKnownLocation?.let { originLocation ->
            originPosition = Point.fromLngLat(originLocation.longitude, originLocation.latitude)
            destinationPosition = Point.fromLngLat(latLng.longitude, latLng.latitude)
            mapboxNavigation.requestRoutes(
                RouteOptions.builder().applyDefaultParams()
                    .accessToken(getString(R.string.mapbox_access_token))
                    .coordinates(listOf(originPosition, destinationPosition))
                    .alternatives(true)
                    .profile(DirectionsCriteria.PROFILE_WALKING)
                    .build(),
                routesReqCallback
            )
        }
        binding.startNavigation.visibility = VISIBLE
        return true
    }

    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                route = routes[0]

                mapboxMap.getStyle {
                    val clickPointSource = it.getSourceAs<GeoJsonSource>("ROUTE_LINE_SOURCE_ID")
                    val routeLineString = LineString.fromPolyline(
                        routes[0].geometry()!!,
                        6
                    )
                    clickPointSource?.setGeoJson(routeLineString)
                }
                binding.routeRetrievalProgressSpinner.visibility = INVISIBLE
            } else {
                Snackbar.make(binding.root, R.string.no_routes, LENGTH_SHORT).show()
            }
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            Log.d(TAG, "route request failure $throwable")
            Snackbar.make(binding.root, R.string.route_request_failed, LENGTH_SHORT).show()
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            Log.d(TAG, "route request canceled")
        }
    }

    private fun toggleLayer() {
        mapboxMap.getStyle { style ->
            val layer = style.getLayer("LIGHTS_LAYER_ID")
            if (layer != null) {
                if (Property.VISIBLE == layer.visibility.getValue()) {
                    layer.setProperties(visibility(Property.NONE))
                    binding.lightsLayerToggle.setBackgroundColor(getColor(requireContext(), R.color.yellow_primary))
                } else {
                    layer.setProperties(visibility(Property.VISIBLE))
                    binding.lightsLayerToggle.setBackgroundColor(getColor(requireContext(), R.color.black))
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(requireContext())) {
            val locationComponentOptions = LocationComponentOptions.builder(requireContext())
                .foregroundTintColor(Color.BLACK)
                .backgroundTintColor(getColor(requireContext(), R.color.yellow_primary))
                .bearingTintColor(getColor(requireContext(), R.color.yellow_primary))
                .accuracyColor(getColor(requireContext(), R.color.yellow_primary))
                .pulseEnabled(true)
                .pulseColor(getColor(requireContext(), R.color.yellow_primary))
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
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String?>?) {
        Toast.makeText(requireContext(), R.string.user_location_permission_explanation, Toast.LENGTH_LONG)
            .show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            onMapReady(mapboxMap)
        } else {
            Toast.makeText(requireContext(), R.string.user_location_permission_not_granted, Toast.LENGTH_LONG)
                .show()
            requireActivity().finish()
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