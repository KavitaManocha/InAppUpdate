package com.example.inappupdates

import android.content.ContentValues
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

//import com.google.android.play.core.appupdate.AppUpdateManager
//import com.google.android.play.core.appupdate.AppUpdateManagerFactory
//import com.google.android.play.core.install.InstallState
//import com.google.android.play.core.install.InstallStateUpdatedListener
//import com.google.android.play.core.install.model.AppUpdateType
//import com.google.android.play.core.install.model.InstallStatus
//import com.google.android.play.core.install.model.UpdateAvailability

class MainActivity : AppCompatActivity() {

    private var mAppUpdateManager: AppUpdateManager? = null
    private val RC_APP_UPDATE = 11
    var name: EditText? = null
    var mail: EditText? = null
    var next: Button?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        name = findViewById(R.id.edt_name)
        mail = findViewById(R.id.edt_mail)
        next = findViewById(R.id.btn_next)
        next?.setOnClickListener {
            if (name?.text?.isEmpty()!!){
                Toast.makeText(applicationContext,"Enter Name",Toast.LENGTH_SHORT).show()
            }
            else if (mail?.text?.isEmpty()!!){
                Toast.makeText(applicationContext,"Enter Email",Toast.LENGTH_SHORT).show()
            }
            else{
                val intent = Intent(this, Welcome::class.java)
                startActivity(intent)
            }
        }

        supportActionBar?.title= "Login Details"
    }

    override fun onStart() {
        super.onStart()
        mAppUpdateManager = AppUpdateManagerFactory.create(this)
        mAppUpdateManager?.registerListener(installStateUpdatedListener)
        mAppUpdateManager?.getAppUpdateInfo()?.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() === UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE /*AppUpdateType.IMMEDIATE*/)
            ) {
                try {
                    mAppUpdateManager?.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE /*AppUpdateType.IMMEDIATE*/,
                        this,
                        RC_APP_UPDATE
                    )
                } catch (e: SendIntentException) {
                    e.printStackTrace()
                }
            } else if (appUpdateInfo.installStatus() === InstallStatus.DOWNLOADED) {
                //CHECK THIS if AppUpdateType.FLEXIBLE, otherwise you can skip
                popupSnackbarForCompleteUpdate()
            } else {
                Log.e(ContentValues.TAG, "checkForAppUpdateAvailability: something else")
            }
        }
    }

    var installStateUpdatedListener: InstallStateUpdatedListener =
        object : InstallStateUpdatedListener {
            override fun onStateUpdate(state: InstallState) {
                if (state.installStatus() === InstallStatus.DOWNLOADED) {
                    //CHECK THIS if AppUpdateType.FLEXIBLE, otherwise you can skip
                    popupSnackbarForCompleteUpdate()
                } else if (state.installStatus() === InstallStatus.INSTALLED) {
                    if (mAppUpdateManager != null) {
                        mAppUpdateManager?.unregisterListener(this)
                    }
                } else {
                    Log.i(
                        ContentValues.TAG,
                        "InstallStateUpdatedListener: state: " + state.installStatus()
                    )
                }
            }
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_APP_UPDATE) {
            if (resultCode != RESULT_OK) {
                Log.e(ContentValues.TAG, "onActivityResult: app download failed")
            }
        }
    }

    private fun popupSnackbarForCompleteUpdate() {
        val snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            "New app is ready!",
            Snackbar.LENGTH_INDEFINITE
        )
        snackbar.setAction("Install") { view: View? ->
            if (mAppUpdateManager != null) {
                mAppUpdateManager?.completeUpdate()
            }
        }
        snackbar.setActionTextColor(resources.getColor(R.color.teal_200))
        snackbar.show()
    }

    override fun onStop() {
        super.onStop()
        if (mAppUpdateManager != null) {
            mAppUpdateManager?.unregisterListener(installStateUpdatedListener)
        }
    }

    override fun onResume() {
        super.onResume()

        mAppUpdateManager
            ?.appUpdateInfo
            ?.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability()
                    == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    // If an in-app update is already running, resume the update.
                    mAppUpdateManager?.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this,
                        RC_APP_UPDATE
                    );
                }
            }
    }
}