package com.srijeesolution.rojgaarwaala.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainToolbarViewModel @Inject constructor() : ViewModel() {

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _selectedLocation = MutableLiveData("")
    val selectedLocation: LiveData<String> = _selectedLocation

    fun setSearchQuery(query: String) {
        if (_searchQuery.value != query) {
            _searchQuery.value = query
        }
    }

    fun setSelectedLocation(location: String) {
        if (_selectedLocation.value != location) {
            _selectedLocation.value = location
        }
    }
}
