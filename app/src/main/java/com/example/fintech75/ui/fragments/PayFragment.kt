package com.example.fintech75.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import androidx.navigation.fragment.navArgs
import com.example.fintech75.R
import com.example.fintech75.ui.activities.MainActivity

class PayFragment : Fragment(R.layout.fragment_pay) {
    private val fragmentName = this::class.java.toString()
    private val args: PayFragmentArgs by navArgs()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).hideBottomNavigation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(fragmentName, "Credit ID: ${args.currentCreditId}, Credit type: ${args.currentCreditType}, Market ID: ${args.marketId}")
    }
}