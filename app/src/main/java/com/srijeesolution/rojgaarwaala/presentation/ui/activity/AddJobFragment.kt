package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.srijeesolution.rojgaarwaala.R

class AddJobFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_job, container, false)
        
        // Navigate to AddJobActivity when this fragment is created
        val intent = Intent(requireContext(), AddJobActivity::class.java)
        startActivity(intent)
        
        return view
    }
} 