package com.example.masmovilcarapp

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.masmovilcarapp.databinding.ActivityMainBinding
import com.example.messagingservice.MessagingService
import com.example.messagingservice.models.EncryptedDataModel
import com.example.messagingservice.models.LoginData
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import helpers.EncryptionHelper
import helpers.MessagingBroadcastReceiver
import helpers.PollingTimer
import java.util.*


class MainActivity : AppCompatActivity(), OnClickListener/*, CompoundButton.OnCheckedChangeListener*/ {


    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var loginData: LoginData


    private val timer = Timer()
    private val pollingTimerTask = PollingTimer()

    private lateinit var intentMessagingService: Intent

    private lateinit var messagingService: MessagingService
    private var isMessagingServiceBound: Boolean = false
    private val messagingServiceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            messagingService = (binder as MessagingService.MessagingServiceBinder).service
            isMessagingServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isMessagingServiceBound = false
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val checkServiceRunnable = object: Runnable {
        override fun run() {
            if (MessagingService.sInstance?.isAlive() == false) {
                Log.i("Activity", "MessagingService dead, restarting it")
                this@MainActivity.startService(intentMessagingService)
            } else {
                Log.i("Activity", "MessagingService alive!")
            }
            handler.postDelayed(this, 40000)
        }
    }

    // Sign-in
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN: Int = 9001

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d("ResultLauncher", "Works!")
        Log.d("ResultLauncher", "resultCode: " + result.resultCode)
        if(result.resultCode == Activity.RESULT_OK) {
            Log.d("ResultLauncher", "Result_ok")
            val data: Intent? = result.data
            
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EncryptionHelper.createAndStoreSecretKey()

        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.oauth_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        intentMessagingService = Intent(this, MessagingService::class.java)
        this.startService(intentMessagingService)
        // This keeps the service bound to the MainActivity so it doesn't get killed by the system while the activity is running.
        bindService(intentMessagingService, messagingServiceConnection, Context.BIND_AUTO_CREATE)


        timer.schedule(pollingTimerTask, 0, 1000)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


        val statusButton: Button = findViewById(R.id.btStatus)
        

        findViewById<SignInButton>(R.id.btSingIn).setOnClickListener(this) 
        
        findViewById<Button>(R.id.signOutButton).setOnClickListener(this)

        findViewById<CheckBox>(R.id.cbElectrolineras).setOnCheckedChangeListener {
                buttonView, isChecked ->
                    var locationStatus = false
                    if (isChecked) {
                        locationStatus = true
                        if (ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            
                        } else {
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                                REQUEST_LOCATION_PERMISSION
                            )
                        }
                    }
                    val serviceIntent = Intent(this, MessagingService::class.java).apply {
                        action = "messagingservice.intent.action.LOCATION_CAMPAIGNS"
                        putExtra("locationStatus", locationStatus)
                        putExtra("campaign_id", 4)
                    }
                    startService(serviceIntent)

        }

        startMessagingServiceCheck()

    }

    override fun onDestroy() {
        
        timer.cancel()
        stopMessagingServiceCheck()
        this.unbindService(messagingServiceConnection)
        stopService(intentMessagingService)
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        var account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
    }

    /*
    This MaincActivity class extends OnClickListener. Then, overriding onClick method we can manage
    the button clicks actions from here.
     */
    override fun onClick(view: View) {
        when (view.id) {
            R.id.btSingIn -> {
                signIn()
            }
            R.id.signOutButton -> {
                signOut()
            }
        }
    }



    private fun startMessagingServiceCheck() {
        handler.post(checkServiceRunnable)
    }

    private fun stopMessagingServiceCheck() {
        handler.removeCallbacks(checkServiceRunnable)
    }


    fun resetNavController() {
        navController.navigate(R.id.navigation_home)
    }

    private fun signIn(){
        val signInIntent = this.mGoogleSignInClient.signInIntent
        resultLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount = completedTask.getResult(ApiException::class.java)

            loginData = LoginData(account.email, account.id, account.idToken)

            var encryptedLoginData = EncryptionHelper.encryptWithKeyStore(Gson().toJson(loginData))
            encryptedLoginData.dataType = "login"


            val serviceIntent = Intent(this, MessagingService::class.java).apply {
                action = "messagingservice.intent.action.LOGIN"
                putExtra("dataType", encryptedLoginData.dataType)
                putExtra("initVector", encryptedLoginData.initializationVector)
                putExtra("encryptedData", encryptedLoginData.encryptedData)
            }
            startService(serviceIntent)

            val signInButton = findViewById<SignInButton>(R.id.btSingIn)
            signInButton.isVisible = false
            val signOutButton = findViewById<Button>(R.id.signOutButton)
            signOutButton.isVisible = true


        } catch (e: ApiException) {

        }
    }

    private fun signOut() {
        mGoogleSignInClient.signOut()
            .addOnCompleteListener(this) {

                var encryptedLoginData = EncryptionHelper.encryptWithKeyStore(Gson().toJson(loginData))
                encryptedLoginData.dataType = "logout"

                val serviceIntent = Intent(this, MessagingService::class.java).apply {
                    action = "messagingservice.intent.action.LOGOUT"
                    putExtra("dataType", encryptedLoginData.dataType)
                    putExtra("initVector", encryptedLoginData.initializationVector)
                    putExtra("encryptedData", encryptedLoginData.encryptedData)
                }
                startService(serviceIntent)

                val signOutButton = findViewById<Button>(R.id.signOutButton)
                signOutButton.isVisible = false
                val signInButton = findViewById<SignInButton>(R.id.btSingIn)
                signInButton.isVisible = true
            }
    }



}