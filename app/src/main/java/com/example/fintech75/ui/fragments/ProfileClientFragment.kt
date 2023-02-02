package com.example.fintech75.ui.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import com.example.fintech75.R
import com.example.fintech75.databinding.FragmentProfileClientBinding
import com.example.fintech75.ui.activities.MainActivity

class ProfileClientFragment : Fragment(R.layout.fragment_profile_client) {
    lateinit var binding: FragmentProfileClientBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).showBottomNavigation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProfileClientBinding.bind(view)
    }
}