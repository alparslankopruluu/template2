package com.screenmotion.app.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import com.screenmotion.app.R
import com.screenmotion.app.audio.SfxKind
import com.screenmotion.app.audio.SfxPlayer
import com.screenmotion.app.data.ConfigRepository
import com.screenmotion.app.data.ThemeType
import com.screenmotion.app.data.VehicleType
import com.screenmotion.app.wallpaper.InteractiveWallpaperService
import com.screenmotion.app.wallpaper.LockWallpaperApplier
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var repo: ConfigRepository
    private lateinit var preview: PreviewSurfaceView
    private lateinit var onboarding: View
    private lateinit var themeRow: LinearLayout
    private lateinit var vehicleRow: LinearLayout
    private lateinit var vehicleLabel: TextView
    private lateinit var vehicleScroll: HorizontalScrollView
    private lateinit var btnMute: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repo = ConfigRepository.get(this)
        SfxPlayer.init(this)

        preview = findViewById(R.id.previewSurface)
        onboarding = findViewById(R.id.onboardingOverlay)
        themeRow = findViewById(R.id.themeRow)
        vehicleRow = findViewById(R.id.vehicleRow)
        vehicleLabel = findViewById(R.id.vehicleLabel)
        vehicleScroll = findViewById(R.id.vehicleScroll)
        btnMute = findViewById(R.id.btnMute)

        findViewById<TextView>(R.id.titleText).text = getString(R.string.app_name)
        findViewById<TextView>(R.id.subtitleText).text = getString(R.string.tagline)

        setupThemeCards()
        setupVehicleChips()
        updateVehicleVisibility(repo.selectedTheme)
        updateMuteIcon()
        preview.setTheme(repo.selectedTheme)

        btnMute.setOnClickListener {
            repo.soundMuted = !repo.soundMuted
            updateMuteIcon()
            lightHaptic()
            if (!repo.soundMuted) SfxPlayer.play(this, SfxKind.THEME_SELECT)
        }

        findViewById<Button>(R.id.btnSetWallpaper).setOnClickListener {
            lightHaptic()
            SfxPlayer.play(this, SfxKind.APPLY_SUCCESS)
            applyWallpaperHomeAndLock()
        }
        findViewById<Button>(R.id.btnShowcase).setOnClickListener {
            lightHaptic()
            SfxPlayer.play(this, SfxKind.THEME_SELECT)
            startActivity(Intent(this, ShowcaseActivity::class.java))
        }
        findViewById<View>(R.id.btnHuaweiGuide).setOnClickListener {
            lightHaptic()
            HuaweiLockGuideSheet.show(this)
        }
        findViewById<Button>(R.id.btnDismissOnboarding).setOnClickListener {
            lightHaptic()
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

    private fun updateMuteIcon() {
        btnMute.setImageResource(
            if (repo.soundMuted) android.R.drawable.ic_lock_silent_mode
            else android.R.drawable.ic_lock_silent_mode_off
        )
        btnMute.alpha = if (repo.soundMuted) 0.55f else 1f
    }

    private fun setupThemeCards() {
        themeRow.removeAllViews()
        for (theme in ThemeType.entries) {
            val card = layoutInflater.inflate(R.layout.item_theme_card, themeRow, false) as MaterialCardView
            card.findViewById<TextView>(R.id.themeEmoji).text = theme.emoji
            card.findViewById<TextView>(R.id.themeName).text = theme.displayName
            card.findViewById<TextView>(R.id.themeSubtitle).text = theme.subtitle
            updateCardSelection(card, theme == repo.selectedTheme)
            card.setOnClickListener {
                lightHaptic()
                SfxPlayer.play(this, SfxKind.THEME_SELECT)
                repo.selectedTheme = theme
                preview.setTheme(theme)
                updateVehicleVisibility(theme)
                for (i in 0 until themeRow.childCount) {
                    val c = themeRow.getChildAt(i) as MaterialCardView
                    val t = ThemeType.entries[i]
                    updateCardSelection(c, t == theme)
                }
                card.animate()
                    .scaleX(1.05f).scaleY(1.05f)
                    .setDuration(110)
                    .withEndAction {
                        card.animate().scaleX(1f).scaleY(1f).setDuration(140)
                            .setInterpolator(DecelerateInterpolator())
                            .start()
                    }.start()
            }
            card.alpha = 0f
            card.translationY = 16f
            themeRow.addView(card)
            card.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay((theme.ordinal * 60).toLong())
                .setDuration(320)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun setupVehicleChips() {
        vehicleRow.removeAllViews()
        for (v in VehicleType.entries) {
            val chip = layoutInflater.inflate(R.layout.item_vehicle_chip, vehicleRow, false) as MaterialCardView
            chip.findViewById<TextView>(R.id.vehicleEmoji).text = v.emoji
            chip.findViewById<TextView>(R.id.vehicleName).text = v.displayName
            val thumb = chip.findViewById<android.widget.ImageView>(R.id.vehicleThumb)
            thumb.setImageResource(vehicleThumbRes(v))
            thumb.clipToOutline = true
            updateVehicleChip(chip, v == repo.selectedVehicle)
            chip.setOnClickListener {
                lightHaptic()
                SfxPlayer.play(this, SfxKind.THEME_SELECT)
                repo.selectedVehicle = v
                preview.setVehicle(v)
                for (i in 0 until vehicleRow.childCount) {
                    val c = vehicleRow.getChildAt(i) as MaterialCardView
                    updateVehicleChip(c, VehicleType.entries[i] == v)
                }
            }
            vehicleRow.addView(chip)
        }
    }

    private fun vehicleThumbRes(v: VehicleType): Int = when (v) {
        VehicleType.SPORTS_CAR -> R.drawable.vehicle_thumb_sports
        VehicleType.TRUCK -> R.drawable.vehicle_thumb_truck
        VehicleType.MOTORCYCLE -> R.drawable.vehicle_thumb_bike
        VehicleType.HELICOPTER -> R.drawable.vehicle_thumb_heli
    }

    private fun updateVehicleVisibility(theme: ThemeType) {
        val show = theme == ThemeType.VEHICLE
        vehicleLabel.isVisible = show
        vehicleScroll.isVisible = show
    }

    private fun updateVehicleChip(card: MaterialCardView, selected: Boolean) {
        card.strokeWidth = if (selected) 2 else 1
        card.strokeColor = getColor(if (selected) R.color.accent else R.color.stroke)
        card.alpha = if (selected) 1f else 0.75f
    }

    private fun updateCardSelection(card: MaterialCardView, selected: Boolean) {
        card.strokeWidth = if (selected) 2 else 1
        card.strokeColor = getColor(if (selected) R.color.accent else R.color.stroke)
        card.alpha = if (selected) 1f else 0.82f
        card.cardElevation = 0f
    }

    private fun lightHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(18)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun hideOnboarding() {
        onboarding.animate()
            .alpha(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { onboarding.isVisible = false }
            .start()
    }

    /**
     * Home: live wallpaper chooser. Lock: render (or static asset) → FLAG_LOCK;
     * on failure save PNG + open picker; then show Huawei guide.
     */
    private fun applyWallpaperHomeAndLock() {
        openLiveWallpaperChooser()
        val appCtx = applicationContext
        Executors.newSingleThreadExecutor().execute {
            val result = LockWallpaperApplier.applyLockForCurrentTheme(appCtx)
            runOnUiThread {
                if (result.lockSet) {
                    Toast.makeText(this, R.string.wallpaper_lock_ok, Toast.LENGTH_SHORT).show()
                } else if (result.savedUri != null || result.error != null) {
                    Toast.makeText(this, R.string.wallpaper_lock_fallback, Toast.LENGTH_LONG).show()
                }
                HuaweiLockGuideSheet.show(this)
            }
        }
    }

    private fun openLiveWallpaperChooser() {
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
        updateVehicleVisibility(repo.selectedTheme)
        updateMuteIcon()
    }
}
