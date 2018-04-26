package com.cyberinov8.googlemaps

import android.Manifest
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.support.v4.app.ActivityCompat
import android.util.Log

import android.widget.Toast
import com.google.android.gms.maps.model.PolylineOptions
import java.net.URL
import org.jetbrains.anko.async
import org.jetbrains.anko.uiThread
import com.beust.klaxon.*
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLngBounds
import org.jetbrains.anko.toast

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback {


    private lateinit var mMap: GoogleMap

    private  var currentLatitude : Double?=null
    private  var currentLongitude: Double?=null

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var mPermissionDenied = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true)
        } else if (mMap != null) {
            mMap.isMyLocationEnabled = true
            currentLocation()
            val sydney = LatLng(currentLatitude!!, currentLongitude!!)
           val opera = LatLng(31.458394, 74.272632)
            mMap!!.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
            mMap!!.addMarker(MarkerOptions().position(opera).title("Opera House"))
        }


    }


    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true)
        } else if (mMap != null) {
        //    mMap.isMyLocationEnabled = true
            val request = LocationRequest()
            request.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            val client = LocationServices.getFusedLocationProviderClient(this)
            client.requestLocationUpdates(request, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    //                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path);
                    var location = locationResult!!.lastLocation
                    if (location != null) {
                        toast("" + location.altitude)
                         currentLatitude = location.altitude
                        currentLongitude = location.longitude

                    }
                }
            }, null)
            val LatLongB = LatLngBounds.Builder()
          val  sydney = LatLng(currentLatitude!!, currentLongitude!!)
          val  opera = LatLng(31.458394, 74.272632)
            mMap!!.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
            mMap!!.addMarker(MarkerOptions().position(opera).title("Opera House"))
            val options = PolylineOptions()
            options.color(Color.RED)
            options.width(5f)
            val url = getURL(sydney, opera)

            async {
                // Connect to URL, download content and convert into string asynchronously
                val result = URL(url).readText()
                uiThread {
                    // When API call is done, create parser and convert into JsonObjec
                    val parser: Parser = Parser()
                    val stringBuilder: StringBuilder = StringBuilder(result)
                    val json: JsonObject = parser.parse(stringBuilder) as JsonObject
                    // get to the correct element in JsonObject
                    val routes = json.array<JsonObject>("routes")
                    val points = routes!!["legs"]["steps"][0] as JsonArray<JsonObject>
                    // For every element in the JsonArray, decode the polyline string and pass all points to a List
                    val polypts = points.flatMap { decodePoly(it.obj("polyline")?.string("points")!!) }
                    // Add  points to polyline and bounds
                    options.add(sydney)
                    LatLongB.include(sydney)
                    for (point in polypts) {
                        options.add(point)
                        LatLongB.include(point)
                    }
                    options.add(opera)
                    LatLongB.include(opera)
                    // build bounds
                    val bounds = LatLongB.build()
                    // add polyline to the map
                    mMap!!.addPolyline(options)
                    // show map with route centered
                    mMap!!.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                }
            }

        }

        //    mMap.isMyLocationEnabled = true

    }

    @SuppressLint("MissingPermission")
    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show()
      //  enableMyLocation()

        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false
    }

    private fun getURL(from: LatLng, to: LatLng): String {
        val origin = "origin=" + from.latitude + "," + from.longitude
        val dest = "destination=" + to.latitude + "," + to.longitude
        val sensor = "sensor=false"
        val params = "$origin&$dest&$sensor"
        return "https://maps.googleapis.com/maps/api/directions/json?$params"
    }

    override fun onMyLocationClick(location: Location) {
        Toast.makeText(this, "Current location:\n$location", Toast.LENGTH_LONG).show()


    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation()
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true

        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError()
            mPermissionDenied = false
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private fun showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(supportFragmentManager, "dialog")
    }

    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5,
                    lng.toDouble() / 1E5)
            poly.add(p)
        }

        return poly
    }



    @SuppressLint("MissingPermission")
    private fun currentLocation(){
        val request = LocationRequest()
        request.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val client = LocationServices.getFusedLocationProviderClient(this)
        client.requestLocationUpdates(request, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                //                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path);
                var location = locationResult!!.lastLocation
                if (location != null) {
                    toast("" + location.altitude +"      "+ location.longitude)
                    currentLatitude = location.altitude
                    currentLongitude = location.longitude

                }
            }
        }, null)
    }
}
