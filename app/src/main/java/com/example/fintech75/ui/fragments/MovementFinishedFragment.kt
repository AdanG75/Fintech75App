package com.example.fintech75.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.fintech75.R
import com.example.fintech75.data.model.UserCredential
import com.example.fintech75.data.remote.RemoteDataSource
import com.example.fintech75.data.remote.RetrofitClient
import com.example.fintech75.databinding.FragmentMovementFinishedBinding
import com.example.fintech75.presentation.UserViewModel
import com.example.fintech75.presentation.UserViewModelFactory
import com.example.fintech75.repository.StartRepositoryImpl
import com.example.fintech75.ui.activities.MainActivity

class MovementFinishedFragment : Fragment(R.layout.fragment_movement_finished) {
    private val fragmentName = this::class.java.toString()
    private val args: MovementFinishedFragmentArgs by navArgs()
    private val userViewModel: UserViewModel by activityViewModels {
        UserViewModelFactory( StartRepositoryImpl(
            RemoteDataSource(RetrofitClient.webService)
        ))
    }

    lateinit var binding: FragmentMovementFinishedBinding

    private lateinit var currentUser: UserCredential
    private lateinit var action: NavDirections

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).hideBottomNavigation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentMovementFinishedBinding.bind(view)

        setup()
    }

    private fun setup() {
        currentUserListener()

        action = if (currentUser.userID == -1) {
            MovementFinishedFragmentDirections.actionMovementFinishedFragmentToLoginFragment()
        } else {
            MovementFinishedFragmentDirections.actionMovementFinishedFragmentToCreditsFragment()
        }

//        binding.bMovementFinishedOk.setOnClickListener {
//            Log.d(fragmentName, "Finish movement's flow")
//            findNavController().navigate(action)
//        }
        binding.bMovementFinishedOk .setOnClickListener{
            if (currentUser.userID == -1) {
                goToLogin()
            } else {
                goToCredits()
            }
        }
        binding.bMovementFinishedOk.isEnabled = true

        backPressedListener()

        if (args.isSuccessful) {
            binding.ivResult.setImageResource(R.drawable.ic_check_24)
            binding.tvMovementFinishedMsg.text = getString(R.string.ok_result_movement)
        } else {
            binding.ivResult.setImageResource(R.drawable.ic_bad_24)
            binding.tvMovementFinishedMsg.text = getString(R.string.bad_result_movement)
        }
    }

    private fun currentUserListener() {
        currentUser = userViewModel.getCurrentUser().value ?: UserCredential(
            token = "N/A",
            userID = -1,
            typeUser = "N/A",
            idType = "N/A",
            email = "N/A"
        )
    }

    private fun backPressedListener() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                Log.d(fragmentName, "Finish movement's flow")
                findNavController().navigate(action)
            }
        })
    }

    private fun goToLogin() {
        Log.d(fragmentName, "Go to Login...")
        findNavController().popBackStack(R.id.loginFragment, false)
    }

    private fun goToCredits() {
        Log.d(fragmentName, "Go to Credits...")
        findNavController().popBackStack(R.id.creditsFragment, false)
    }

}