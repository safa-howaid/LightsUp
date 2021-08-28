package com.memoryleak.lightsup.ui.learn

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.memoryleak.lightsup.databinding.FragmentLearnMoreBinding

class LearnFragment : Fragment() {

    private lateinit var learnViewModel: LearnViewModel
    private var _binding: FragmentLearnMoreBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        learnViewModel =
            ViewModelProvider(this).get(LearnViewModel::class.java)

        _binding = FragmentLearnMoreBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textLearnMore
        learnViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}