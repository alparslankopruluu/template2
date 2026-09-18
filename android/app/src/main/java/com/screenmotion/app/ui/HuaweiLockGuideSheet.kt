package com.screenmotion.app.ui

import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.screenmotion.app.R

object HuaweiLockGuideSheet {
    fun show(context: Context) {
        val dialog = BottomSheetDialog(context, R.style.ThemeOverlay_ScreenMotion_BottomSheet)
        val view = LayoutInflater.from(context).inflate(R.layout.sheet_huawei_lock_guide, null)
        dialog.setContentView(view)
        view.findViewById<android.view.View>(R.id.btnHuaweiGuideClose).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
}
