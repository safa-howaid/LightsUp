package com.memoryleak.lightsup.ui.learn

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LearnViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is learn more Fragment"
    }
    val text: LiveData<String> = _text
}