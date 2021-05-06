package sjsu.cmpelkk.myandroidmulti

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.nav_header.view.*
import sjsu.cmpelkk.myandroidmulti.Firebase.LoginViewModel
import sjsu.cmpelkk.myandroidmulti.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var headerView: View

    //get the reference of the login ViewModel
    private val loginviewModel by viewModels<LoginViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        drawerLayout = binding.drawerLayout

        navController = Navigation.findNavController(this, R.id.myNavHostFragment)
        NavigationUI.setupActionBarWithNavController(this,navController,drawerLayout)


        NavigationUI.setupWithNavController(binding.navView, navController)

        //Access the header view
        headerView = binding.navView.getHeaderView(0)
        //headerView.navheadertextView.text = "No account"
        observeAuthenticationState()
        headerView.loginbutton.setOnClickListener { launchSignInFlow() }



    }

//    override fun onSupportNavigateUp(): Boolean {
//        navController = Navigation.findNavController(this, R.id.myNavHostFragment)
//        return navController.navigateUp(navController, drawerLayout)
//    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = Navigation.findNavController(this, R.id.myNavHostFragment)
        return NavigationUI.navigateUp(navController, drawerLayout)
    }

    private fun observeAuthenticationState() {
        //changes the UI based on the authentication state
        loginviewModel.authenticationState.observe(this, Observer { authenticationState ->
            when (authenticationState) {
                LoginViewModel.AuthenticationState.AUTHENTICATED -> {
                    headerView.navheadertextView.text = loginviewModel.getUserInfo()//"Get user name" //
                    headerView.loginbutton.text = "SignOut"
                    headerView.loginbutton.setOnClickListener {
                        //do signout
                        AuthUI.getInstance().signOut(this).addOnCompleteListener {
                            Log.i(TAG, "Sign out successful")
                        }
                    }
                }
                else -> {
                    headerView.navheadertextView.text = "No Account"
                    headerView.loginbutton.text = "SignIn"
                    headerView.loginbutton.setOnClickListener {
                        //launch signin
                        launchSignInFlow()
                    }
                }
            }

        })
    }

    private fun launchSignInFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()
        )
        startActivityForResult(
            AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(
                providers
            ).build(), Companion.SIGN_IN_RESULT_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SIGN_IN_RESULT_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                Log.i(TAG,"Successfully signed in user" + "${FirebaseAuth.getInstance().currentUser?.displayName}")
            }else {
                Log.i(TAG, "Sign in unsuccessful ${response?.error?.errorCode}")
            }
        }
    }

    companion object {
        const val SIGN_IN_RESULT_CODE = 1001
        const val TAG = "MainActivity"
    }


}
