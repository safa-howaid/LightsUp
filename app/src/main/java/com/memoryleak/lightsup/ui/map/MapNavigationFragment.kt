package com.memoryleak.lightsup.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.ui.NavigationViewOptions
import com.mapbox.navigation.ui.OnNavigationReadyCallback
import com.mapbox.navigation.ui.listeners.NavigationListener
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import com.memoryleak.lightsup.R
import com.memoryleak.lightsup.databinding.FragmentNavigationMapBinding

class MapNavigationFragment(route: DirectionsRoute, originPosition: Point) : Fragment(), OnNavigationReadyCallback, NavigationListener {

    private var _binding: FragmentNavigationMapBinding? = null
    private lateinit var navigationMapboxMap: NavigationMapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation
    private val route = route
    private val originPoint = originPosition

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Mapbox.getInstance(container!!.context, getString(R.string.mapbox_access_token))
        _binding = FragmentNavigationMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapNavigationView.onCreate(savedInstanceState)
        val initialPosition = CameraPosition.Builder()
            .target(LatLng(originPoint.longitude(), originPoint.latitude()))
            .zoom(10.0)
            .build()
        binding.mapNavigationView.initialize(this, initialPosition)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapNavigationView.onLowMemory()
    }

    override fun onStart() {
        super.onStart()
        binding.mapNavigationView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapNavigationView.onResume()
    }

    override fun onStop() {
        super.onStop()
        binding.mapNavigationView.onStop()
    }

    override fun onPause() {
        super.onPause()
        binding.mapNavigationView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapNavigationView.onDestroy()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        binding.mapNavigationView.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            binding.mapNavigationView.onRestoreInstanceState(savedInstanceState)
        }
    }

    override fun onNavigationReady(isRunning: Boolean) {
        if (!isRunning && !::navigationMapboxMap.isInitialized) {
            if (binding.mapNavigationView.retrieveNavigationMapboxMap() != null) {
                this.navigationMapboxMap = binding.mapNavigationView.retrieveNavigationMapboxMap()!!
                binding.mapNavigationView.retrieveMapboxNavigation()?.let { this.mapboxNavigation = it }
                val optionsBuilder = NavigationViewOptions.builder(requireContext())
                optionsBuilder.navigationListener(this)
                optionsBuilder.directionsRoute(route)
                optionsBuilder.shouldSimulateRoute(true)
                binding.mapNavigationView.startNavigation(optionsBuilder.build())
            }
        }
    }

    override fun onNavigationRunning() {
    }

    override fun onNavigationFinished() {
        if (activity != null) {
            val fragmentTransaction: FragmentTransaction = requireActivity()
                .supportFragmentManager.beginTransaction()
            fragmentTransaction.disallowAddToBackStack()
            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            fragmentTransaction.replace(R.id.nav_host_fragment_activity_main, MapFragment())
            fragmentTransaction.commit()
        }
    }

    override fun onCancelNavigation() {
        binding.mapNavigationView.stopNavigation()
        if (activity != null) {
            val fragmentTransaction: FragmentTransaction = requireActivity()
                .supportFragmentManager.beginTransaction()
            fragmentTransaction.disallowAddToBackStack()
            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            fragmentTransaction.replace(R.id.nav_host_fragment_activity_main, MapFragment())
            fragmentTransaction.commit()
        }
    }

    companion object {
        private const val TAG = "NavigationMapFragment"
    }
}