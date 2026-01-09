package vn.unlimit.vpngate.adapter

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import vn.unlimit.vpngate.R
import vn.unlimit.vpngate.models.ExcludedApp

class AppSelectionAdapter(initialApps: List<ExcludedApp>) :
    RecyclerView.Adapter<AppSelectionAdapter.AppViewHolder>() {

    interface SelectionChangeListener {
        fun onSelectionChanged()
    }

    private var apps: List<ExcludedApp> = initialApps
    private val selectedApps = mutableSetOf<String>()
    private var selectionListener: SelectionChangeListener? = null

    init {
        // Pre-select already excluded apps (all apps in the list are excluded)
        apps.forEach { selectedApps.add(it.packageName) }
    }

    fun updateApps(newApps: List<ExcludedApp>) {
        // Preserve selections when updating the app list
        val previouslySelected = selectedApps.toSet()
        apps = newApps
        // Re-apply selections for apps that are still in the list
        selectedApps.clear()
        apps.filter { previouslySelected.contains(it.packageName) }
            .forEach { selectedApps.add(it.packageName) }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_selection, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.bind(app)
    }

    override fun getItemCount(): Int = apps.size

    fun getSelectedApps(): List<ExcludedApp> {
        return apps.filter { selectedApps.contains(it.packageName) }
    }

    fun setSelectionChangeListener(listener: SelectionChangeListener?) {
        selectionListener = listener
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appIcon: ImageView = itemView.findViewById(R.id.app_icon)
        private val appName: TextView = itemView.findViewById(R.id.app_name)
        private val appPackage: TextView = itemView.findViewById(R.id.app_package)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_select)

        fun bind(app: ExcludedApp) {
            appName.text = app.appName
            appPackage.text = app.packageName
            checkBox.isChecked = selectedApps.contains(app.packageName)

            // Load app icon
            try {
                val pm = itemView.context.packageManager
                val appInfo = pm.getApplicationInfo(app.packageName, 0)
                appIcon.setImageDrawable(pm.getApplicationIcon(appInfo))
            } catch (e: PackageManager.NameNotFoundException) {
                appIcon.setImageResource(R.drawable.ic_setting_black)
            }

            itemView.setOnClickListener {
                if (selectedApps.contains(app.packageName)) {
                    selectedApps.remove(app.packageName)
                } else {
                    selectedApps.add(app.packageName)
                }
                checkBox.isChecked = selectedApps.contains(app.packageName)
                selectionListener?.onSelectionChanged()
            }

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedApps.add(app.packageName)
                } else {
                    selectedApps.remove(app.packageName)
                }
                selectionListener?.onSelectionChanged()
            }
        }
    }
}
