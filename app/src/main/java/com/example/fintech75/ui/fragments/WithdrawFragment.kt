package com.example.fintech75.ui.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import com.example.fintech75.R
import com.example.fintech75.ui.activities.MainActivity

class WithdrawFragment : Fragment(R.layout.fragment_withdraw) {
    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).hideBottomNavigation()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}