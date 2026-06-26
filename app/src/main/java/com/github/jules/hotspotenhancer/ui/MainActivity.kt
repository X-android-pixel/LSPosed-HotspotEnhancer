package com.github.jules.hotspotenhancer.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.github.jules.hotspotenhancer.R
import com.github.jules.hotspotenhancer.hooks.FrameworkHooks
import com.github.jules.hotspotenhancer.utils.Prefs

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
        Prefs.makeWorldReadable(this)
    }

    class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
        private var clientsCategory: PreferenceCategory? = null

        private val clientReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val clients = intent.getStringArrayListExtra("clients")
                updateClientsList(clients)
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            clientsCategory = findPreference("connected_clients_category")
            preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onResume() {
            super.onResume()
            requireContext().registerReceiver(
                clientReceiver,
                IntentFilter(FrameworkHooks.ACTION_CLIENTS_UPDATED),
                Context.RECEIVER_EXPORTED
            )
        }

        override fun onPause() {
            super.onPause()
            requireContext().unregisterReceiver(clientReceiver)
        }

        override fun onDestroy() {
            super.onDestroy()
            preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            Prefs.makeWorldReadable(requireContext())
        }

        private fun updateClientsList(clients: List<String>?) {
            clientsCategory?.removeAll()
            if (clients.isNullOrEmpty()) {
                clientsCategory?.addPreference(Preference(requireContext()).apply {
                    title = "No devices connected"
                    isEnabled = false
                })
            } else {
                for (mac in clients) {
                    clientsCategory?.addPreference(Preference(requireContext()).apply {
                        title = mac
                        summary = "Connected"
                    })
                }
            }
        }
    }
}
