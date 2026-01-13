package vn.unlimit.vpngate.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import vn.unlimit.vpngate.App
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.adapter.AppSelectionAdapter
import vn.unlimit.vpngate.models.ExcludedApp

class AppSelectionDialog : DialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInput: EditText
    private lateinit var loadingProgress: ProgressBar
    private lateinit var excludedCountLabel: TextView
    private lateinit var btnCancel: Button
    private lateinit var btnAdd: Button
    private lateinit var appSelectionAdapter: AppSelectionAdapter
    private var listener: AppSelectionListener? = null
    private var excludedApps: List<ExcludedApp> = emptyList()
    private var allApps: List<ExcludedApp> = emptyList()
    private var originalExcludedApps: List<ExcludedApp> = emptyList()
    private var isLoadingCancelled = false

    interface AppSelectionListener {
        fun onAppsSelected(apps: List<ExcludedApp>)
    }

    fun setAppSelectionListener(listener: AppSelectionListener) {
        this.listener = listener
    }

    fun setExcludedApps(excludedApps: List<ExcludedApp>) {
        this.excludedApps = excludedApps
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle(getString(R.string.add_apps_to_exclude))
        // Make dialog permanent - don't dismiss when tapping outside
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_app_selection, container, false)

        recyclerView = view.findViewById(R.id.recycler_view_apps)
        searchInput = view.findViewById(R.id.search_input)
        loadingProgress = view.findViewById(R.id.loading_progress)
        excludedCountLabel = view.findViewById(R.id.excluded_count_label)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnAdd = view.findViewById(R.id.btn_add)

        setupSearchInput()
        setupRecyclerView()
        setupButtons()
        loadApps()

        return view
    }

    override fun onStart() {
        super.onStart()
        // Set dialog width to 90% of screen width
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupRecyclerView() {
        appSelectionAdapter = AppSelectionAdapter(emptyList())
        appSelectionAdapter.setSelectionChangeListener(object : AppSelectionAdapter.SelectionChangeListener {
            override fun onSelectionChanged() {
                updateCountLabel()
                updateApplyButtonState()
            }
        })
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = appSelectionAdapter
    }

    private fun setupButtons() {
        btnCancel.setOnClickListener {
            isLoadingCancelled = true
            dismiss()
        }
        btnAdd.setOnClickListener {
            val selectedApps = appSelectionAdapter.getSelectedApps()
            listener?.onAppsSelected(selectedApps)
            dismiss()
        }
    }

    private fun setupSearchInput() {
        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun loadApps() {
        // Show loading state
        showLoading(true)
        isLoadingCancelled = false

        // Load apps on background thread
        Thread {
            try {
                val apps = getInstalledApps()

                // Check if fragment is still attached and loading hasn't been cancelled
                if (!isLoadingCancelled && isAdded && activity != null) {
                    requireActivity().runOnUiThread {
                        // Double-check after getting to main thread
                        if (!isLoadingCancelled && isAdded && activity != null) {
                            allApps = apps
                            originalExcludedApps = excludedApps.toList() // Store original state
                            // Initialize adapter with all apps and pre-selected excluded apps
                            appSelectionAdapter.initializeWithPreSelectedApps(apps, excludedApps)
                            updateCountLabel()
                            updateApplyButtonState()
                            showLoading(false)
                            // Force layout refresh to fix checkbox positioning
                            recyclerView.post {
                                appSelectionAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Check if fragment is still attached before showing error
                if (!isLoadingCancelled && isAdded && activity != null) {
                    requireActivity().runOnUiThread {
                        // Double-check after getting to main thread
                        if (!isLoadingCancelled && isAdded && activity != null) {
                            showLoading(false)
                            Toast.makeText(context, "Error loading apps", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }.start()
    }

    private fun filterApps(query: String) {
        val filteredApps = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter { app ->
                app.appName.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)
            }
        }
        appSelectionAdapter.updateApps(filteredApps)
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            loadingProgress.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            searchInput.visibility = View.GONE
            searchInput.isEnabled = false
            btnCancel.isEnabled = true  // Keep cancel enabled during loading
            btnAdd.visibility = View.GONE  // Hide apply button during loading
        } else {
            loadingProgress.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            searchInput.visibility = View.VISIBLE
            searchInput.isEnabled = true
            btnCancel.isEnabled = true
            btnAdd.visibility = View.VISIBLE  // Show apply button after loading
            // Apply button state will be set by updateApplyButtonState()
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun getInstalledApps(): List<ExcludedApp> {
        val pm = requireContext().packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return apps.filter { appInfo ->
            // Filter out only the current app, show all system and user apps
            appInfo.packageName != requireContext().packageName
        }.map { appInfo ->
            ExcludedApp(
                packageName = appInfo.packageName,
                appName = pm.getApplicationLabel(appInfo).toString()
            )
        }.sortedBy { it.appName }
    }

    private fun updateCountLabel() {
        val selectedCount = appSelectionAdapter.getSelectedApps().size
        excludedCountLabel.text = getString(R.string.exclude_apps_text, selectedCount)
    }

    private fun updateApplyButtonState() {
        val currentSelectedApps = appSelectionAdapter.getSelectedApps()

        // Compare current selections with original selections
        val originalPackageNames = originalExcludedApps.map { it.packageName }.toSet()
        val currentPackageNames = currentSelectedApps.map { it.packageName }.toSet()

        val hasChanges = originalPackageNames != currentPackageNames
        btnAdd.isEnabled = hasChanges
        btnAdd.alpha = if (hasChanges) 1.0f else 0.5f
    }
}
