package sjsu.cmpelkk.myandroidmulti.Firebase

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.google.firebase.auth.FirebaseAuth

class LoginViewModel: ViewModel() {
    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED, INVALID_AUTHENTICATION
    }

    val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null) {
            AuthenticationState.AUTHENTICATED
        }else {
            AuthenticationState.UNAUTHENTICATED
        }
    }

    fun getUserInfo(): String {
        val currentuser = FirebaseAuth.getInstance().currentUser//FirebaseUserLiveData().firebaseAuth.currentUser
        val name = currentuser?.displayName
        val email = currentuser?.email
        val photoUrl = currentuser?.photoUrl

        // Check if user's email is verified
        val emailVerified = currentuser?.isEmailVerified

        // The user's ID, unique to the Firebase project. Do NOT use this value to
        // authenticate with your backend server, if you have one. Use
        // FirebaseUser.getToken() instead.
        val uid = currentuser?.uid

        val userinfo = "Username: ${name} \n ${email} "

        Log.i(
            "MainActivity",
            "LoginViewModel User info, name: ${name}; email: ${email} ${emailVerified}; photoUrl: ${photoUrl}; uid:${uid} ."
        )
        return userinfo
    }
}