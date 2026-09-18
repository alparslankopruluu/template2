package com.screenmotion.app.ui

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.screenmotion.app.R
import com.screenmotion.app.data.ConfigRepository

/**
 * Mock paywall — yearly primary, monthly secondary.
 * Sets is_pro = true on tap (no Play Billing).
 */
object PaywallSheet {
    fun show(context: Context, onPurchased: (() -> Unit)? = null) {
        val dialog = BottomSheetDialog(context, R.style.ThemeOverlay_ScreenMotion_BottomSheet)
        val view = LayoutInflater.from(context).inflate(R.layout.sheet_paywall, null)
        dialog.setContentView(view)

        val repo = ConfigRepository.get(context)

        view.findViewById<MaterialButton>(R.id.btnPaywallYearly).setOnClickListener {
            mockPurchase(context, repo, onPurchased)
            dialog.dismiss()
        }
        view.findViewById<MaterialButton>(R.id.btnPaywallMonthly).setOnClickListener {
            mockPurchase(context, repo, onPurchased)
            dialog.dismiss()
        }
        view.findViewById<TextView>(R.id.btnPaywallClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun mockPurchase(
        context: Context,
        repo: ConfigRepository,
        onPurchased: (() -> Unit)?
    ) {
        repo.isPro = true
        Toast.makeText(context, R.string.paywall_mock_success, Toast.LENGTH_SHORT).show()
        onPurchased?.invoke()
    }
}
