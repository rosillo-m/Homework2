package com.example.homework2

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.homework2.ui.theme.Homework2Theme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import android.app.Activity
import com.google.firebase.Firebase


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this) // Ensure Firebase is initialized
        enableEdgeToEdge()

        setContent {
            Homework2Theme {
                val navController = rememberNavController()
                val authenticationManager = remember { AuthenticationManager(this) }

                NavHost(navController = navController, startDestination = "login") {
                    composable("login") {
                        LoginScreen(navController, authenticationManager)
                    }
                    composable("githubLogin") {
                        GitHubLoginScreen(navController, authenticationManager)
                    }
                    composable("home/{userName}") { backStackEntry ->
                        val userName = backStackEntry.arguments?.getString("userName")
                        HomeScreen(userName ?: "User", navController, authenticationManager)
                    }
                    composable("about") {
                        AboutScreen(navController)
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    navController: NavHostController,
    authenticationManager: AuthenticationManager
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Hey there!",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Please sign-in to continue",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { newValue -> email = newValue },
            placeholder = { Text(text = "Email") },
            leadingIcon = { Icon(imageVector = Icons.Rounded.Email, contentDescription = null) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { newValue -> password = newValue },
            placeholder = { Text(text = "Password") },
            leadingIcon = { Icon(imageVector = Icons.Rounded.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // LOGIN BUTTON
        Button(
            onClick = {
                authenticationManager.loginWithEmail(email, password)
                    .onEach { response ->
                        if (response is AuthResponse.Success) {
                            navController.navigate("home/$email")
                        } else if (response is AuthResponse.Error) {
                            Toast.makeText(
                                context,
                                "Login failed: ${response.message}",
                                Toast.LENGTH_LONG
                            ).show()

                            authenticationManager.createAccountWithEmail(email, password)
                                .onEach { createResponse ->
                                    if (createResponse is AuthResponse.Success) {
                                        navController.navigate("home/$email")
                                    } else if (createResponse is AuthResponse.Error) {
                                        Toast.makeText(
                                            context,
                                            "Account creation failed: ${createResponse.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                                .launchIn(coroutineScope)
                        }
                    }
                    .launchIn(coroutineScope)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Sign-In",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "or continue with")
        }

        // GITHUB SIGN-IN BUTTON
        OutlinedButton(
            onClick = {
                navController.navigate("githubLogin")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.github),
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "   Sign-in with GitHub",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun GitHubLoginScreen(
    navController: NavHostController,
    authenticationManager: AuthenticationManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "GitHub Login",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // GITHUB SIGN-IN BUTTON
        Button(
            onClick = {
                authenticationManager.signInWithGitHub()
                    .onEach { response ->
                        if (response is AuthResponse.Success) {
                            val signedInUser = authenticationManager.getSignInUser()
                            val displayName = signedInUser?.displayName ?: "User"
                            navController.navigate("home/$displayName")
                        } else if (response is AuthResponse.Error) {
                            Toast.makeText(context, "Sign in failed: ${response.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                    .launchIn(coroutineScope)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.github),
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "   Sign in with GitHub",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { navController.popBackStack() }, // Go back to login screen
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Back to Login",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(userName: String, navController: NavHostController, authenticationManager: AuthenticationManager) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Home Screen") },
                actions = {
                    IconButton(onClick = { navController.navigate("about") }) {
                        Icon(Icons.Default.Info, contentDescription = "About")
                    }
                }
            )
        }
    ) { // This is the trailing lambda for the `content` of Scaffold
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Welcome $userName",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Click on the i-icon to navigate to the about page!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Light
                )

                Button(
                    onClick = {
                        authenticationManager.logout()
                        navController.navigate("login")
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Text(text = "Logout", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AboutScreen(navController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "About This App",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "This page explains what this app is about :)" +
                        "\nThis app was created with the intention of learning how to do authentication using Firebase.",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text(text = "Back to Home", fontWeight = FontWeight.Bold)
            }
        }
    }
}

class AuthenticationManager(private val context: Context) {
    private val auth = Firebase.auth

    fun createAccountWithEmail(email: String, password: String): Flow<AuthResponse> = callbackFlow {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    trySend(AuthResponse.Success)
                } else {
                    trySend(AuthResponse.Error(task.exception?.message ?: "Error occurred"))
                }
            }
        awaitClose()
    }

    fun loginWithEmail(email: String, password: String): Flow<AuthResponse> = callbackFlow {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    trySend(AuthResponse.Success)
                } else {
                    val errorCode = (task.exception as? FirebaseAuthException)?.errorCode
                    Log.e("LoginError", "Error: ${task.exception?.message}")
                    when (errorCode) {
                        "ERROR_INVALID_EMAIL" -> trySend(AuthResponse.Error("Invalid email format"))
                        "ERROR_WRONG_PASSWORD" -> trySend(AuthResponse.Error("Incorrect password"))
                        "ERROR_USER_NOT_FOUND" -> trySend(AuthResponse.Error("User not found"))
                        else -> trySend(AuthResponse.Error("Authentication failed: ${task.exception?.message}"))
                    }
                }
            }
        awaitClose()
    }

    fun getSignInUser(): FirebaseUser? = auth.currentUser

    // GitHub OAuth sign-in method
    fun signInWithGitHub(): Flow<AuthResponse> = callbackFlow {
        val provider = OAuthProvider.newBuilder("github.com")

        val pending = auth.pendingAuthResult
        if (pending != null) {
            pending.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    trySend(AuthResponse.Success)
                } else {
                    trySend(AuthResponse.Error(task.exception?.message ?: "GitHub Auth failed"))
                }
            }
        } else {
            auth.startActivityForSignInWithProvider(context as Activity, provider.build())
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        trySend(AuthResponse.Success)
                    } else {
                        val exception = task.exception
                        trySend(AuthResponse.Error(exception?.message ?: "GitHub Auth failed"))
                    }
                }
        }
        awaitClose()
    }

    fun logout() {
        auth.signOut()
    }
}

interface AuthResponse {
    object Success : AuthResponse
    data class Error(val message: String) : AuthResponse
}
