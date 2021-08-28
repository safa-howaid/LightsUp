package com.memoryleak.lightsup.ui.issues

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class IssueViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is issue log Fragment"
    }
    val text: LiveData<String> = _text
}