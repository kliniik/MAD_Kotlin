package es.upm.btb.madproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.android.gms.common.api.ApiException

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // set status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.backgroundDark)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.backgroundDark)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize views after layout is set
        firstNameEditText = findViewById(R.id.firstNameEditText)
        lastNameEditText = findViewById(R.id.lastNameEditText)

        // Initialize Google Sign-In
        configureGoogleSignIn()

        val googleRegisterButton = findViewById<Button>(R.id.googleRegisterButton)
        val emailRegisterButton = findViewById<Button>(R.id.emailRegisterButton)
        val loginLink = findViewById<TextView>(R.id.loginLink)

        // Google registration button
        googleRegisterButton.setOnClickListener {
            signInWithGoogle()
        }

        // Email registration button
        emailRegisterButton.setOnClickListener {
            // Get email and password and sign up
            val email = findViewById<EditText>(R.id.emailEditText).text.toString()
            val password = findViewById<EditText>(R.id.passwordEditText).text.toString()
            createAccountWithEmail(email, password)
        }

        // Link to login screen
        loginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    // Configure Google Sign-In
    private fun configureGoogleSignIn() {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))  // Your Web Client ID here
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
    }

    // Google sign-in process
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    // Handle Google sign-in result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w("RegisterActivity", "Google sign-in failed", e)
                Toast.makeText(baseContext, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                updateUI(null)
            }
        }
    }

    // Authenticate with Firebase using Google
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    val displayName = user?.displayName
                    Log.d("RegisterActivity", "User name: $displayName")
                    updateUI(user)
                } else {
                    Log.w("RegisterActivity", "signInWithCredential:failure", task.exception)
                    updateUI(null)
                }
            }
    }

    // Email and password registration
    private fun createAccountWithEmail(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Set the user's display name
                    val user = FirebaseAuth.getInstance().currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName("${firstNameEditText.text} ${lastNameEditText.text}")
                        .build()

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                updateUI(user)
                            } else {
                                Log.w("RegisterActivity", "updateProfile:failure", profileTask.exception)
                            }
                        }
                } else {
                    Log.w("RegisterActivity", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    // Update UI based on user state
    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // Redirect to the MainActivity or another screen after successful registration
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
