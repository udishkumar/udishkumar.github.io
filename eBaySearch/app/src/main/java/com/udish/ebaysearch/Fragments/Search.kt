package com.udish.ebaysearch.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.udish.ebaysearch.R
import com.udish.ebaysearch.utils.SharedViewModel
import org.json.JSONObject

class Search : Fragment() {

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var radioBtnCurrent: RadioButton
    private lateinit var radioBtnOther: RadioButton
    private lateinit var textViewCurrent: TextView
    private lateinit var etKeyword: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnSearch: Button
    private lateinit var btnClear: Button
    private lateinit var checkConditionNew: CheckBox
    private lateinit var checkConditionUsed: CheckBox
    private lateinit var checkConditionUnspecified: CheckBox
    private lateinit var checkShippingLocal: CheckBox
    private lateinit var checkFreeShipping: CheckBox
    private lateinit var tvErrorKeyword: TextView
    private lateinit var tvErrorZipcode: TextView
    private lateinit var checkEnableSearch: CheckBox
    private lateinit var nearbySearchLayout: View
    private lateinit var actZipCode: AutoCompleteTextView
    private val MAX_CHAR_LIMIT = 5


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        etKeyword = view.findViewById(R.id.etKeyword)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        btnSearch = view.findViewById(R.id.btnSearch)
        btnClear = view.findViewById(R.id.btnClear)
        checkConditionNew = view.findViewById(R.id.checkConditionNew)
        checkConditionUsed = view.findViewById(R.id.checkConditionUsed)
        checkConditionUnspecified = view.findViewById(R.id.checkConditionUnspecified)
        checkShippingLocal = view.findViewById(R.id.checkShippingLocal)
        checkFreeShipping = view.findViewById(R.id.checkFreeShipping)
        tvErrorKeyword = view.findViewById(R.id.tvErrorKeyword)
        tvErrorZipcode = view.findViewById(R.id.tvErrorZipcode)
        checkEnableSearch = view.findViewById(R.id.checkEnableSearch)
        nearbySearchLayout = view.findViewById(R.id.nearbySearchLayout)
        radioBtnCurrent = view.findViewById(R.id.radioBtnCurrent)
        radioBtnOther = view.findViewById(R.id.radioBtnOther)
        textViewCurrent = view.findViewById(R.id.textViewCurrent)
        actZipCode = view.findViewById(R.id.actZipcode)
        actZipCode.isEnabled = false

        radioBtnCurrent.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                radioBtnOther.isChecked = false
                showLocationConfirmationDialog()
            }
        }

        radioBtnOther.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                textViewCurrent.text = ""
                radioBtnCurrent.isChecked = false
                actZipCode.isEnabled = true
                actZipCode.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        if (s != null) {
                            if (s.length > MAX_CHAR_LIMIT) {
                                // Truncate the excess characters
                                actZipCode.setText(s.subSequence(0, MAX_CHAR_LIMIT))
                                actZipCode.setSelection(MAX_CHAR_LIMIT) // Set cursor to the end of the text
                            } else if (s.length >= 1) {
                                suggestPostalCodes(s.toString().toInt())
                            }
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {}
                })
            }else{
                actZipCode.isEnabled = false
            }
        }

        setupClearButton(btnClear)
        setupCheckBox()
        setupSpinner()
        setupSearchButton()

        return view
    }

    private fun setupSpinner() {
        val categories = resources.getStringArray(R.array.category_array)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }

    private fun showLocationConfirmationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Location Permission")
        builder.setMessage("Do you want to provide your current location?")

        builder.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss()
            fetchPostalCode()
        }

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
            radioBtnCurrent.isChecked = false
        }

        builder.show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureActionBar()
    }

    private fun configureActionBar() {
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            title = "Product Search"  // Set the title as per your requirement
            setDisplayHomeAsUpEnabled(false)
            setDisplayShowHomeEnabled(false)
        }
    }


    private fun setupSearchButton() {
        btnSearch.setOnClickListener {
            if (etKeyword.text.isEmpty()) {
                tvErrorKeyword.text = getText(R.string.tvKeywordErrorMsg)
                tvErrorKeyword.visibility = View.VISIBLE
                tvErrorZipcode.text = getText(R.string.tvKeywordErrorMsg)
                tvErrorZipcode.visibility = View.VISIBLE
                showSimpleToast(getText(R.string.tvKeywordErrorToastMsg).toString())
            } else {
                tvErrorKeyword.visibility = View.GONE
                tvErrorZipcode.visibility = View.GONE
                val requestJson = generateRequestJson()
                sharedViewModel.searchData.value = requestJson
                fragmentManager?.beginTransaction()
                    ?.replace(R.id.activity_main, SearchResults())
                    ?.addToBackStack(null)
                    ?.commit()
                val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(btnSearch.windowToken, 0)
            }
        }
    }

    private fun setupClearButton(btnClear: Button) {
        btnClear.setOnClickListener {
            // Reset EditText
            etKeyword.setText("")

            // Reset Spinner to the first item
            spinnerCategory.setSelection(0)

            // Uncheck all CheckBoxes
            checkConditionNew.isChecked = false
            checkConditionUsed.isChecked = false
            checkConditionUnspecified.isChecked = false
            checkShippingLocal.isChecked = false
            checkFreeShipping.isChecked = false

            // Reset RadioButtons (if you have a default option, set it here)
            radioBtnCurrent.isChecked = false
            radioBtnOther.isChecked = false
            checkEnableSearch.isChecked = false
            actZipCode.setText("")

            // Hide or reset TextViews
            tvErrorKeyword.visibility = View.GONE
            textViewCurrent.text = ""

            // Reset any other UI components as required
        }
    }

    private fun setupCheckBox() {
        checkEnableSearch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                nearbySearchLayout.visibility = View.VISIBLE
                val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(checkEnableSearch.windowToken, 0)
            } else {
                nearbySearchLayout.visibility = View.GONE
            }
        }
    }

    private fun showSimpleToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun fetchPostalCode() {
        val url = "https://ipinfo.io/json?token=ea42c97831bf33"

        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                val postalCode = response.getString("postal")
                textViewCurrent.text = postalCode
            },
            { error ->
                // Handle error
                Log.i("error: ", "Error fetching postal codes: ${error.message}")
            }
        )
        Volley.newRequestQueue(context).add(jsonObjectRequest)
    }

    private fun suggestPostalCodes(zip: Int) {
        val url = "https://uk-hw3.wl.r.appspot.com/fetchPostalCodes?zipCodeStartsWith=$zip"

        val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                val postalCodes = ArrayList<String>()
                for (i in 0 until response.length()) {
                    postalCodes.add(response.getString(i))
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, postalCodes)
                actZipCode.setAdapter(adapter)
                adapter.notifyDataSetChanged()
            },
            { error ->
                // Handle error
                Log.i("error: ", "Error fetching postal codes: ${error.message}")
            }
        )

        Volley.newRequestQueue(context).add(jsonArrayRequest)
    }

    private fun generateRequestJson(): JSONObject {
        val requestJson = JSONObject()

        // Keyword
        requestJson.put("keywords", etKeyword.text.toString())

        // Category
        // Load categories from strings.xml
        val categories = resources.getStringArray(R.array.category_array)
        val categoryMap = mapOf(categories[1] to 550, categories[2] to 2984, categories[3] to 267,
            categories[4] to 11450, categories[5] to 58058, categories[6] to 26395,
            categories[7] to 11233, categories[8] to 1249)

        // Get selected category
        val selectedCategory = spinnerCategory.selectedItem.toString()

        if (selectedCategory != categories[0]) {
            requestJson.put("category", categoryMap[selectedCategory] ?: "")
        }

        // Condition
        val conditions = mutableListOf<String>()
        if (checkConditionNew.isChecked) conditions.add("1000")
        if (checkConditionUsed.isChecked) conditions.add("3000")
        if (checkConditionUnspecified.isChecked && conditions.isEmpty())
            conditions.addAll(listOf("1000", "3000"))
        requestJson.put("condition", conditions.joinToString(","))

        // Shipping Options
        requestJson.put("localPickup", checkShippingLocal.isChecked)
        requestJson.put("freeShipping", checkFreeShipping.isChecked)

        // Nearby Search Options
        if (checkEnableSearch.isChecked) {
            requestJson.put("maxDistance", view?.findViewById<EditText>(R.id.etDistance)?.text.toString())
            if (radioBtnCurrent.isChecked) {
                requestJson.put("zipCode", textViewCurrent.text.toString())
            } else if (radioBtnOther.isChecked) {
                requestJson.put("zipCode", actZipCode.text.toString())
            }
        }

        return requestJson
    }

    // You can add more methods if needed to handle other UI events
}