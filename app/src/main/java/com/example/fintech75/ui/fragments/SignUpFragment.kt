package com.example.fintech75.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import androidx.navigation.fragment.findNavController
import com.example.fintech75.R
import com.example.fintech75.databinding.FragmentSignUpBinding
import com.example.fintech75.ui.activities.MainActivity

class SignUpFragment : Fragment(R.layout.fragment_sign_up) {
    private val fragmentName = this::class.java.toString()

    lateinit var binding: FragmentSignUpBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).hideBottomNavigation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSignUpBinding.bind(view)

        binding.bAddClient.setOnClickListener {
            Log.d(fragmentName, "Go to sign-up client")

            val action = SignUpFragmentDirections.actionSignUpFragmentToSignUpClientFragment()
            findNavController().navigate(action)
        }
        binding.bAddClient.isEnabled = true

        binding.bAddMarket.setOnClickListener {
            Log.d(fragmentName, "Go to sign-up market")

            val action = SignUpFragmentDirections.actionSignUpFragmentToSignUpMarketFragment()
            findNavController().navigate(action)
        }
        binding.bAddMarket.isEnabled = true
    }
}