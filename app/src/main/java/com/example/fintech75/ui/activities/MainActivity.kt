package com.example.fintech75.ui.activities

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.fintech75.R
import com.example.fintech75.core.GlobalSettings
import com.example.fintech75.data.remote.RemoteDataSource
import com.example.fintech75.data.remote.RetrofitClient
import com.example.fintech75.databinding.ActivityMainBinding
import com.example.fintech75.presentation.*
import com.example.fintech75.repository.StartRepositoryImpl
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private val activityName = this::class.java.toString()
    private val userViewModel: UserViewModel by viewModels {
        UserViewModelFactory( StartRepositoryImpl(
            RemoteDataSource(RetrofitClient.webService)
        ))
    }

    private val startViewModel: StartViewModel by viewModels {
        StartViewModelFactory( StartRepositoryImpl(
            RemoteDataSource(RetrofitClient.webService)
        ))
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNavigation = binding.bnvMenu
        setupBottomNav()
        currentUserListener()
        getInputMethodManager()
    }

    private fun currentUserListener() {
        userViewModel.getCurrentUser().observe(this){ user ->
            when(user.typeUser) {
                "market" -> {
                    Log.d(activityName, "New market logged")
                    bottomNavigation.menu.clear()
                    bottomNavigation.inflateMenu(R.menu.menu_market)
                }
                "client" -> {
                    Log.d(activityName, "New client logged")
                    bottomNavigation.menu.clear()
                    bottomNavigation.inflateMenu(R.menu.menu_client)
                }
                else -> {
                    Log.d(activityName, "No client/market logged")
                    bottomNavigation.menu.clear()
                    bottomNavigation.visibility = View.GONE
                }
            }
        }
    }

    private fun setupBottomNav() {
        val navHostFragment = binding.startFragment.getFragment<Fragment>() as NavHostFragment
        navController = navHostFragment.navController

        // Setup the bottom navigation view with navController
        bottomNavigation.setupWithNavController(navController)
    }

    private fun getInputMethodManager() {
        GlobalSettings.inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    fun showBottomNavigation() {
        bottomNavigation.visibility = View.VISIBLE
    }

    fun hideBottomNavigation() {
        bottomNavigation.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()

        userViewModel.setDefaultUser()
        startViewModel.setDefaultValues()
    }
}