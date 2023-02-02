package com.example.fintech75.ui.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import com.example.fintech75.R
import com.example.fintech75.ui.activities.MainActivity

class AddElementFragment : Fragment(R.layout.fragment_add_element) {

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (activity as MainActivity).showBottomNavigation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}