package com.notserious.auxilium

import android.Manifest
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import kotlinx.android.synthetic.main.activity_start.*
import com.android.volley.AuthFailureError
import com.android.volley.toolbox.Volley
import android.location.LocationManager
import android.content.pm.PackageManager
import android.location.Location
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.Toast


class StartActivity : AppCompatActivity() {

    var location: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        aed_id.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                action_login.isEnabled = s.toString().isNotEmpty()
                action_register.isEnabled = s.toString().isNotEmpty()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        action_login.setOnClickListener {
            val aiderName = aed_id.text.toString().trim()
            MainActivity.actionStart(baseContext, aiderName)
        }

        action_register.setOnClickListener {
            getLocationAndRegister()
        }
    }

    fun register(name: String, lat: Double?, long: Double?) {
        val queue = Volley.newRequestQueue(this)
        val sr = object : StringRequest(Request.Method.GET, Utils.API_HOST,
                Response.Listener {
                    response -> Log.e(TAG, "Success! response: " + response.toString())
                    Toast.makeText(this, "Register Successful!", Toast.LENGTH_SHORT).show() },
                Response.ErrorListener { error -> Log.e(TAG, "Error: " + error.toString()) }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["name"] = name
                params["latitude"] = lat.toString()
                params["longitude"] = long.toString()
                return params
            }

            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/x-www-form-urlencoded"
                return params
            }
        }
        queue.add(sr)
    }

    fun getLocationAndRegister() {
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                        PERMISSIONS_ACCESS_COARSE_LOCATION)
            }
        } else {
            // Permission has already been granted
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val longitude = location?.longitude
            val latitude = location?.latitude
            val aiderName = aed_id.text.toString().trim()
            Log.d(TAG, "latitude: $latitude, longitude: $longitude")
            register(aiderName, longitude, latitude)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_ACCESS_COARSE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    getLocationAndRegister()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }

        // Add other 'when' lines to check for other
        // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    companion object {
        const val TAG = "StartActivity"
        const val PERMISSIONS_ACCESS_COARSE_LOCATION: Int = 8
    }



}
