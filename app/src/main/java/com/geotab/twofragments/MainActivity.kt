package com.geotab.twofragments
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.geotab.mobile.sdk.DriveFragment
import com.geotab.mobile.sdk.module.Failure
import com.geotab.mobile.sdk.module.Success
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : FragmentActivity() {

    private val TAG: String = "c.g.m.Main"
    private val driveView = DriveFragment.newInstance()
    private val mapFrag = MapsFragment()
    private lateinit var driveLayout: ConstraintLayout
    private lateinit var drivingText: TextView
    private lateinit var dutyText: TextView
    private var userName: String? = null
    private var previousFrag: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initialize()

        supportFragmentManager.beginTransaction().add(R.id.main_layout, driveView).commit()

        driveView.setDriverActionNecessaryCallback {(isDriverActionNecessary, _) ->
            if(!isDriverActionNecessary) {
                startGetAvailability()
            }
        }
    }

    private fun getHOS() {
        var driving = ""
        var duty = ""

        userName?.let { userNameResult ->
            driveView.getAvailability(userNameResult) { result ->
                when (result) {
                    is Success -> {

                        JSONObject(result.value).let {
                            duty = it.getString("cycle")
                            driving = it.getString("driving")
                        }
                    }
                    is Failure -> {
                        duty = result.reason.toString()
                    }
                    else -> {
                        duty = "unknown result"
                    }
                }
                runOnUiThread {
                    dutyText.text = duty
                    drivingText.text = driving
                }
            }
        } ?: run {
            driveView.getAllUsers { result ->
                when (result) {
                    is Success -> {
                        JSONArray(result.value).let {
                            userName = it.getJSONObject(0).getString("name")
                        }
                    }
                    is Failure -> {
                        Log.d(TAG, "Unable to get user")
                    }
                    else -> {
                        Log.d(TAG, "Unknown Response")
                    }
                }
            }
        }

    }

    private fun initialize() {

        driveLayout =  findViewById(R.id.main_layout)
        drivingText = findViewById(R.id.txthos)
        dutyText = findViewById(R.id.txtdriving)

        findViewById<Button>(R.id.btnstarthos).setOnClickListener {
            showHideDriveLayout()
        }
    }

    private fun showHideDriveLayout() {

        val transaction = supportFragmentManager.beginTransaction()

        previousFrag = if(previousFrag == null) {
            transaction.add(R.id.main_layout, mapFrag).commit()
            mapFrag
        } else {

            if(previousFrag == mapFrag) {
                transaction.hide(mapFrag)
                transaction.show(driveView).commit()
                driveView
            } else {
                transaction.hide(driveView)
                transaction.show(mapFrag).commit()
                mapFrag
            }
        }
    }

    private fun startGetAvailability() {
        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            override fun run() {
                getHOS()
                mainHandler.postDelayed(this, 10000)
            }
        })
    }
}