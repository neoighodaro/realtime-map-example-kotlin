package com.example.android.realtimemapkotlin

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.LatLng
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.MediaType
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import okhttp3.RequestBody
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Marker

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var markerOptions:MarkerOptions
    private lateinit var marker:Marker
    private lateinit var cameraPosition:CameraPosition
    var defaultLongitude = -122.088426
    var defaultLatitude  = 37.388064
    lateinit var googleMap:GoogleMap
    lateinit var pusher:Pusher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        markerOptions = MarkerOptions()
        val latLng = LatLng(defaultLatitude,defaultLongitude)
        markerOptions.position(latLng)

        cameraPosition = CameraPosition.Builder()
                .target(latLng)
                .zoom(17f).build()

        simulateButton.setOnClickListener {
            callServerToSimulate()
        }

        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupPusher()
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        this.googleMap = googleMap!!
        marker = googleMap.addMarker(markerOptions)
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun callServerToSimulate() {

        val jsonObject = JSONObject()
        jsonObject.put("latitude",defaultLatitude)
        jsonObject.put("longitude",defaultLongitude)

        val body = RequestBody.create(MediaType.parse("application/json"), jsonObject.toString())

        getRetrofitObject().sendCoordinates(body).enqueue(object:Callback<String>{
            override fun onResponse(call: Call<String>?, response: Response<String>?) {
                Log.d("TAG",response!!.body().toString())

            }

            override fun onFailure(call: Call<String>?, t: Throwable?) {
                Log.d("TAG",t!!.message)
            }

        })

    }


    private fun setupPusher() {

        val options = PusherOptions()
        options.setCluster("PUSHER_CLUSTER")
        pusher = Pusher("PUSHER_API_KEY", options)

        val channel = pusher.subscribe("my-channel")

        channel.bind("new-values") { channelName, eventName, data ->
            val jsonObject = JSONObject(data)
            Log.d("TAG",jsonObject.toString())

            val lat:Double = jsonObject.getString("latitude").toDouble()
            val lon:Double = jsonObject.getString("longitude").toDouble()

            runOnUiThread {
                val newLatLng = LatLng(lat, lon)
                marker.position = newLatLng
                cameraPosition = CameraPosition.Builder()
                        .target(newLatLng)
                        .zoom(17f).build()
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

            }


        }

    }

    override fun onResume() {
        super.onResume()
        pusher.connect()
    }

    override fun onPause() {
        super.onPause()
        pusher.disconnect()
    }

    fun getRetrofitObject(): ApiInterface {
        val httpClient = OkHttpClient.Builder()
        val builder = Retrofit.Builder()
                .baseUrl("http://10.0.3.2:4000/")
                //.baseUrl("http://10.0.2.2:4000/")
                .addConverterFactory(ScalarsConverterFactory.create())

        val retrofit = builder
                .client(httpClient.build())
                .build()
        return retrofit.create(ApiInterface::class.java)
    }

}
