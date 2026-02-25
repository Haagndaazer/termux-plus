package com.termux.app.plus.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.MaterialToolbar
import com.termux.R
import com.termux.app.activities.SettingsActivity

class TermuxPlusSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_termux_plus_settings)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, TermuxPlusSettingsFragment())
                .commit()
        }
    }

    class TermuxPlusSettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.termux_plus_preferences, rootKey)

            // Set version summary
            findPreference<Preference>("version")?.summary =
                requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName

            // Open Termux settings
            findPreference<Preference>("termux_settings")?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), SettingsActivity::class.java))
                true
            }
        }
    }
}
