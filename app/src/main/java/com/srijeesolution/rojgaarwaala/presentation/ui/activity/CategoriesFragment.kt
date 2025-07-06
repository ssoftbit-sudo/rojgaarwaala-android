package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.srijeesolution.rojgaarwaala.databinding.FragmentCategoriesBinding
import com.srijeesolution.rojgaarwaala.presentation.adaptor.CategoryGridAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.data.remote.model.Category
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoriesFragment : Fragment() {
    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    private lateinit var homePageViewModel: HomePageViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        homePageViewModel = ViewModelProvider(this)[HomePageViewModel::class.java]
        binding.categoriesRecyclerView.layoutManager = GridLayoutManager(requireContext(), 4)
        observeCategoriesData()
        callApi()
    }

    private fun callApi() {
        binding.progressBar.visibility = View.VISIBLE
        homePageViewModel.getCategoriesData()
    }

    private fun observeCategoriesData() {
        homePageViewModel.categoriesLiveData.observe(viewLifecycleOwner) { apiResponse ->
            when (apiResponse) {
                is ApiResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is ApiResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val data = apiResponse.data?.dataObj
                    val categoryList = data?.categories ?: emptyList<Category>()
                    binding.categoriesRecyclerView.adapter = CategoryGridAdapter(categoryList, onItemClick = { cat ->
                        Toast.makeText(requireContext(), cat.title ?: "Category", Toast.LENGTH_SHORT).show()
                    }, showViewAll = false)
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to load categories", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 