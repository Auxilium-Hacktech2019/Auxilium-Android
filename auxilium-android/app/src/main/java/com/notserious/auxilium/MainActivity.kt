package com.notserious.auxilium

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import android.widget.Toast
import android.content.DialogInterface.*
import android.media.MediaPlayer
import android.support.v7.app.AlertDialog
import com.android.volley.AuthFailureError
import java.util.concurrent.atomic.AtomicBoolean


class MainActivity : AppCompatActivity() {

    var pollingThread: Thread? = null
    private val running = AtomicBoolean(false)
    var player: MediaPlayer? = null
    var aiderName: String? = null
    var alert = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        aiderName = intent.getStringExtra(KEY_AED_NAME)
        aed_info.text = String.format(getString(R.string.aed_info),
                aiderName)
        sos_status.text = getString(R.string.title_safe)
        action_dismiss.setOnClickListener {
            Toast.makeText(baseContext, getString(R.string.long_press_dismiss_tip), Toast.LENGTH_SHORT).show()
        }
        action_dismiss.setOnLongClickListener(object : View.OnLongClickListener {
            override fun onLongClick(v: View?): Boolean {
                showRemoveAlertDialog()
                return true
            }
        })
    }

    override fun onResume() {
        super.onResume()
        startPolling()
    }

    override fun onPause() {
        super.onPause()
        pausePolling()
    }

    override fun onBackPressed() {
        if (alert)
            showRemoveAlertDialog()
        else
            super.onBackPressed()
    }

    fun startPolling() {
        running.set(true)
        val pollingRunnable = Runnable {
            while (running.get()) {
                try {
                    if (!alert) {
                        poll()
                    }
                    Thread.sleep(3000)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    Log.e(TAG, "Polling thread interrupted. ")
                }
            }
        }
        pollingThread = Thread(pollingRunnable)
        pollingThread!!.start()
    }

    fun pausePolling() {
        running.set(false)
        pollingThread!!.interrupt()
    }

    fun poll() {
        val queue = Volley.newRequestQueue(this)
        val url = Utils.getLatestAidLink(aiderName!!)
        val stringRequest = object: StringRequest(Request.Method.GET, url,
                Response.Listener<String> { response ->
                    Log.d(TAG, response)
                    val jsonObject = JSONObject(response)
                    val patient_position = jsonObject.getString("patient_position")
                    if (patient_position != "null" || patient_position == "") {
                        val latlong = patient_position.split(',')
                        if (latlong[1].isNotEmpty()) {
                            val lat = latlong[1].toDouble()
                            val long = latlong[0].toDouble()
                            alert(lat, long)
                        }
                    }
                },
                Response.ErrorListener {
                    Log.e(TAG, "url = ${url}; polling failed: ${it.message}")
                    it.printStackTrace()
                }) {
//            override fun getHeaders(): MutableMap<String, String> {
//                val header = HashMap<String, String>()
//                header["Accept"] = "application/json"
//                return header
//            }
        }
        queue.add(stringRequest)
    }

    fun alert(lat: Double, long: Double) {
        alert = true
        TransitionManager.beginDelayedTransition(panel)
        location_info.visibility = View.VISIBLE
        map_view.visibility = View.VISIBLE
        action_dismiss.visibility = View.VISIBLE
        panel.setBackgroundColor(getColor(R.color.colorAccent))
        sos_status.setTextColor(getColor(R.color.colorWhite))
        location_info.setTextColor(getColor(R.color.colorWhite))
        sos_status.text = getString(R.string.title_sos)
        val queue = Volley.newRequestQueue(this)
        val url = Utils.getAddress(lat, long)
        val stringRequest = StringRequest(Request.Method.GET, url,
                Response.Listener<String> { response ->
                    try {
                        val jsonObject = JSONObject(response)
                        val formattedAddress = jsonObject
                                .getJSONArray("results")
                                .getJSONObject(0)
                                .getString("formatted_address")
                        location_info.text = formattedAddress
//                        Log.d(TAG, response)
                    } catch (e: Exception) {
                        Log.e(TAG, e.message)
                    }
                },
                Response.ErrorListener {
                    Log.e(TAG, "url = ${url}; get address from latlong failed: ${it.message}")
                })
        queue.add(stringRequest)
        Glide.with(this).load(Utils.getMapPictureLink(lat, long)).into(map_view)
        Log.d(TAG, Utils.getMapPictureLink(lat, long))

        map_view.setOnClickListener {
            Handler().postDelayed(Runnable {
                val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$long")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)
            }, 1000)
        }

        player = MediaPlayer.create(baseContext, R.raw.alarm)
        player!!.isLooping = true
        player!!.start()
    }

    fun removeAlert() {
        alert = false
        TransitionManager.beginDelayedTransition(panel)
        location_info.visibility = View.GONE
        map_view.visibility = View.GONE
        action_dismiss.visibility = View.GONE
        panel.setBackgroundColor(getColor(R.color.colorPrimary))
        sos_status.setTextColor(getColor(R.color.textDark))
        sos_status.text = getString(R.string.title_safe)

        if (player != null && player!!.isPlaying) {
            player!!.stop()
            player!!.release()
        }

        val queue = Volley.newRequestQueue(this)
        val sr = object : StringRequest(Request.Method.POST, Utils.API_REMOVE_ALERT,
                Response.Listener {
                    response -> Log.e(TAG, "Success! response: " + response.toString())
                    Toast.makeText(this, "Remove Alert Successful!", Toast.LENGTH_SHORT).show() },
                Response.ErrorListener { error ->
                    Log.e(TAG,"${Utils.API_REMOVE_ALERT}, Error: " + error.toString())
                    error.printStackTrace()
                }) {

            override fun getBodyContentType(): String {
                return "application/json"
            }

            override fun getBody(): ByteArray {
                val jsonString = "{\"name\": \"$aiderName\"}"
                Log.d(TAG, "body = $jsonString")
                return jsonString.toByteArray()
            }

            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["name"] = aiderName!!
                return params
            }
        }
        queue.add(sr)
    }

    fun showRemoveAlertDialog() {
        val alertDialog = AlertDialog.Builder(this@MainActivity).create()
        alertDialog.setTitle(getString(R.string.dismiss_caution_title))
        alertDialog.setMessage(getString(R.string.dismiss_caution))
        alertDialog.setButton(BUTTON_POSITIVE,
                getString(R.string.action_cancel),
                { dialog, which -> alertDialog.dismiss()})
        alertDialog.setButton(BUTTON_NEGATIVE,
                getString(R.string.action_dismiss),
                { dialog, which -> removeAlert() })
        alertDialog.show()
    }


    companion object {
        val TAG = "MainActivity"
        val KEY_AED_NAME = "KEY_AED_NAME"

        fun actionStart(context: Context, aiderName: String) {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(KEY_AED_NAME, aiderName)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }
}
