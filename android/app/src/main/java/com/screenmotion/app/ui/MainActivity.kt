package com.screenmotion.app.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import com.screenmotion.app.R
import com.screenmotion.app.data.ConfigRepository
import com.screenmotion.app.data.ThemeType
import com.screenmotion.app.wallpaper.InteractiveWallpaperService

class MainActivity : AppCompatActivity() {

    private lateinit var repo: ConfigRepository
    private lateinit var preview: PreviewSurfaceView
    private lateinit var onboarding: View
    private lateinit var themeRow: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repo = ConfigRepository.get(this)

        preview = findViewById(R.id.previewSurface)
        onboarding = findViewById(R.id.onboardingOverlay)
        themeRow = findViewById(R.id.themeRow)

        findViewById<TextView>(R.id.titleText).text = getString(R.string.app_name)
        findViewById<TextView>(R.id.subtitleText).text = getString(R.string.tagline)

        setupThemeCards()
        preview.setTheme(repo.selectedTheme)

        findViewById<Button>(R.id.btnSetWallpaper).setOnClickListener { setLiveWallpaper() }
        findViewById<Button>(R.id.btnDismissOnboarding).setOnClickListener {
            repo.onboardingDone = true
            hideOnboarding()
        }

        if (!repo.onboardingDone) {
            onboarding.isVisible = true
            onboarding.alpha = 0f
            onboarding.animate().alpha(1f).setDuration(500).start()
        } else {
            onboarding.isVisible = false
        }
    }

    private fun setupThemeCards() {
        themeRow.removeAllViews()
        for (theme in ThemeType.entries) {
            val card = layoutInflater.inflate(R.layout.item_theme_card, themeRow, false) as MaterialCardView
            card.findViewById<TextView>(R.id.themeEmoji).text = theme.emoji
            card.findViewById<TextView>(R.id.themeName).text = theme.displayName
            updateCardSelection(card, theme == repo.selectedTheme)
            card.setOnClickListener {
                repo.selectedTheme = theme
                preview.setTheme(theme)
                for (i in 0 until themeRow.childCount) {
                    val c = themeRow.getChildAt(i) as MaterialCardView
                    val t = ThemeType.entries[i]
                    updateCardSelection(c, t == theme)
                }
                // Micro bounce
                card.animate()
                    .scaleX(1.06f).scaleY(1.06f)
                    .setDuration(120)
                    .withEndAction {
                        card.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    }.start()
            }
            themeRow.addView(card)
        }
    }

    private fun updateCardSelection(card: MaterialCardView, selected: Boolean) {
        card.strokeWidth = if (selected) 3 else 0
        card.strokeColor = getColor(R.color.accent)
        card.alpha = if (selected) 1f else 0.72f
    }

    private fun hideOnboarding() {
        onboarding.animate()
            .alpha(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { onboarding.isVisible = false }
            .start()
    }

    private fun setLiveWallpaper() {
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, InteractiveWallpaperService::class.java)
                )
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
            } catch (e2: Exception) {
                Toast.makeText(this, R.string.wallpaper_error, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        preview.setTheme(repo.selectedTheme)
    }
}
