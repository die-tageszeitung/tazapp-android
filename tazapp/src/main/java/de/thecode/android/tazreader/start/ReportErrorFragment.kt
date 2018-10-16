package de.thecode.android.tazreader.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import de.thecode.android.tazreader.R
import de.thecode.android.tazreader.data.TazSettings
import de.thecode.android.tazreader.preferences.PreferenceFragmentCompat
import de.thecode.android.tazreader.reporterror.ErrorReporter
import de.thecode.android.tazreader.reporterror.RequestFileLogDialog

class ReportErrorFragment : PreferenceFragmentCompat() {


    private val logFileListener = TazSettings.OnPreferenceChangeListener<Boolean> {
        logFileWritePreference.isChecked = it
    }

    val settings: TazSettings by lazy {
        TazSettings.getInstance(context)
    }

    private val logFileWritePreference by lazy {
        findPreference(getString(R.string.pref_key_log_file)) as SwitchPreferenceCompat
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        (activity as StartActivity).onUpdateDrawer(this)
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view?.setBackgroundColor(ContextCompat.getColor(inflater.context, R.color.start_fragment_background))
        return view
    }

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.report_error_preferences)

        val crashlyticsPreference = findPreference(getString(R.string.pref_key_crashlytics_always_send)) as SwitchPreferenceCompat
        crashlyticsPreference.isChecked = settings.crashlyticsAlwaysSend
        crashlyticsPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            settings.crashlyticsAlwaysSend = newValue as Boolean
            return@OnPreferenceChangeListener true
        }

        val reportErrorPreference = findPreference(getString(R.string.pref_key_report_error))
        reportErrorPreference.setOnPreferenceClickListener {
            if (settings.isWriteLogfile) context?.let { context -> ErrorReporter.sendErrorMail(context) }
            else RequestFileLogDialog.newInstance().show(fragmentManager, RequestFileLogDialog.DIALOG_FILELOG_REQUEST)
//            if (context != null) ErrorReporter.sendErrorMail(context!!)
            true
        }
    }

    override fun onStart() {
        super.onStart()
        settings.addOnPreferenceChangeListener(TazSettings.PREFKEY.LOGFILE, logFileListener )
    }

    override fun onStop() {
        settings.removeOnPreferenceChangeListener { logFileListener }
        super.onStop()
    }
}

