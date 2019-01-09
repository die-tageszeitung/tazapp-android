package de.thecode.android.tazreader.dialognew

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class HelpDialog: DialogFragment() {

    companion object {
        const val TAG = "helpDialog"
        const val ARG_SHOW_ACCOUNT = "showAccount"
        fun create(bundleFactory:() -> Bundle, title:String? = null): HelpDialog{
            val fragment = HelpDialog()
            fragment.arguments = bundleFactory()
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {




        return super.onCreateDialog(savedInstanceState)

    }
}