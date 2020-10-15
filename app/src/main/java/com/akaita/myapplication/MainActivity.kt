package com.akaita.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.okta.oidc.*
import com.okta.oidc.clients.sessions.SessionClient
import com.okta.oidc.clients.web.WebAuthClient
import com.okta.oidc.storage.security.DefaultEncryptionManager
import com.okta.oidc.storage.security.EncryptionManager
import com.okta.oidc.storage.security.GuardedEncryptionManager
import com.okta.oidc.util.AuthorizationException
import kotlinx.android.synthetic.main.activity_main.*



class MainActivity : AppCompatActivity() {

    /**
     * Authorization client using chrome custom tab as a user agent.
     */
    private lateinit var webAuth: WebAuthClient

    /**
     * The authorized client to interact with Okta's endpoints.
     */
    private lateinit var sessionClient: SessionClient

    private var currentEncryptionManager: EncryptionManager? = null
    private var keyguardEncryptionManager: GuardedEncryptionManager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupWebAuth()
        setupWebAuthCallback(webAuth)

        signIn.setOnClickListener {
            val payload = AuthenticationPayload.Builder()
                    .build()
            webAuth.signIn(this, payload)
        }
    }

    private fun setupWebAuthCallback(webAuth: WebAuthClient) {
        val callback: ResultCallback<AuthorizationStatus, AuthorizationException> =
                object : ResultCallback<AuthorizationStatus, AuthorizationException> {
                    override fun onSuccess(status: AuthorizationStatus) {
                        if (status == AuthorizationStatus.AUTHORIZED) {
                            Log.d("MainActivity", "AUTHORIZED")
                            Toast.makeText(this@MainActivity, "Authorized", Toast.LENGTH_SHORT).show()
                        } else if (status == AuthorizationStatus.SIGNED_OUT) {
                            Log.d("MainActivity", "SIGNED_OUT")
                            Toast.makeText(this@MainActivity, "Signed out", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancel() {
                        Log.d("MainActivity", "CANCELED")
                        Toast.makeText(this@MainActivity, "Cancelled", Toast.LENGTH_SHORT).show()
                    }

                    override fun onError(msg: String?, error: AuthorizationException?) {
                        Log.d("MainActivity", "${error?.error} onError", error)
                        Toast.makeText(this@MainActivity, error?.toJsonString(), Toast.LENGTH_SHORT).show()
                    }
                }
        webAuth.registerCallback(callback, this)
    }

    override fun onResume() {
        super.onResume()

        // Check whether the user has already authenticated using the device's authentication method. If it hasn't, ask them to do it
        if (currentEncryptionManager?.isUserAuthenticatedOnDevice?.not() == true) {
            showKeyguard()
        }
    }

    private fun showKeyguard() {
        Biometric(
                fragmentActivity = this,
                onSuccessListener = {
                    Log.d("MainActivity", "Biometric authentication succeeded")
                    Toast.makeText(this, "Biometric authentication succeeded", Toast.LENGTH_SHORT).show()
                },
                onCancelListener = {
                    runOnUiThread {
                        Log.d("MainActivity", "Biometric authentication cancelled")
                        Toast.makeText(this, "Biometric authentication cancelled", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                },
                onErrorListener = { code, message ->
                    runOnUiThread {
                        Log.d("MainActivity", "Biometric authentication failed")
                        Toast.makeText(this, "Biometric authentication failed", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }).show()
    }

    private fun setupWebAuth() {
        keyguardEncryptionManager = GuardedEncryptionManager(this, Int.MAX_VALUE)

        val oidcConfig = OIDCConfig.Builder()
                .clientId("0oa9aqujkIgPPaSE35d5")
                .redirectUri("com.okta.dev-9361400:/callback")
                .endSessionRedirectUri("com.okta.dev-9361400:/logout")
                .scopes("openid", "profile", "offline_access")
                .discoveryUri("https://dev-9361400.okta.com")
                .create()

        webAuth = Okta.WebAuthBuilder()
                .withConfig(oidcConfig)
                .withContext(applicationContext)
                .withCallbackExecutor(null)
                .withEncryptionManager(keyguardEncryptionManager)
                .create()
        sessionClient = webAuth.sessionClient
    }

}