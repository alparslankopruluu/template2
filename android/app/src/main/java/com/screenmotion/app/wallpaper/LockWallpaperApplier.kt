package com.screenmotion.app.wallpaper

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.content.FileProvider
import com.screenmotion.app.data.ConfigRepository
import com.screenmotion.app.data.ThemeConfig
import com.screenmotion.app.data.ThemeType
import com.screenmotion.app.motion.MotionState
import com.screenmotion.app.render.SceneFactory
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Huawei/Honor gap: live wallpaper often drives HOME only.
 * Also push a high-res static frame to FLAG_LOCK so lock + AOD "Tam ekran" look correct.
 */
object LockWallpaperApplier {

    private const val TAG = "LockWallpaperApplier"

    data class Result(
        val lockSet: Boolean,
        val usedStaticFallback: Boolean,
        val savedUri: Uri? = null,
        val error: String? = null
    )

    fun applyLockForCurrentTheme(context: Context): Result {
        val repo = ConfigRepository.get(context)
        val theme = repo.selectedTheme
        var usedStatic = false
        val vehicle = repo.selectedVehicle
        // Prefer photoreal vehicle lock frames when Vehicle theme is selected.
        val bitmap = try {
            val staticFirst = if (theme == ThemeType.VEHICLE) {
                loadVehicleLockBitmap(context, vehicle) ?: loadStaticLockBitmap(context, theme)
            } else {
                null
            }
            if (staticFirst != null) {
                usedStatic = true
                scaleToWallpaper(context, staticFirst)
            } else {
                renderThemeBitmap(context, theme, vehicle.name)
                    ?: loadStaticLockBitmap(context, theme)?.also { usedStatic = true }
                        ?.let { scaleToWallpaper(context, it) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "render failed, trying static", e)
            usedStatic = true
            (loadVehicleLockBitmap(context, vehicle) ?: loadStaticLockBitmap(context, theme))
                ?.let { scaleToWallpaper(context, it) }
        }

        if (bitmap == null) {
            return Result(false, usedStatic, error = "no_bitmap")
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val wm = WallpaperManager.getInstance(context)
                wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                Result(lockSet = true, usedStaticFallback = usedStatic)
            } else {
                @Suppress("DEPRECATION")
                WallpaperManager.getInstance(context).setBitmap(bitmap)
                Result(lockSet = true, usedStaticFallback = usedStatic)
            }
        } catch (e: Exception) {
            Log.w(TAG, "setBitmap FLAG_LOCK failed", e)
            val uri = saveBitmapToPictures(context, bitmap, theme)
            if (uri != null) {
                openWallpaperPicker(context, uri)
            }
            Result(lockSet = false, usedStaticFallback = usedStatic, savedUri = uri, error = e.message)
        } finally {
            if (!bitmap.isRecycled) {
                // Keep if system may still hold ref briefly; recycle when we created a copy only.
            }
        }
    }

    fun renderThemeBitmap(
        context: Context,
        theme: ThemeType,
        vehicleName: String? = null
    ): Bitmap? {
        val (w, h) = wallpaperSize(context)
        val config = ThemeConfig.forTheme(theme)
        val vehicle = com.screenmotion.app.data.VehicleType.fromName(vehicleName)
        val renderer = SceneFactory.create(theme, vehicle)
        renderer.onSizeChanged(w, h)
        renderer.reset()
        val motion = MotionState()
        // Settle a few frames so particles/stars populate
        repeat(8) {
            renderer.update(1f / 30f, motion, config)
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        renderer.draw(canvas, motion, config)
        return bmp
    }

    fun loadStaticLockBitmap(context: Context, theme: ThemeType): Bitmap? {
        val assetName = when (theme) {
            ThemeType.SPACE -> "themes/theme_space_lock.png"
            ThemeType.AQUARIUM -> "themes/theme_aquarium_lock.png"
            ThemeType.NATURE -> "themes/theme_nature_lock.png"
            ThemeType.VEHICLE -> "themes/theme_vehicle_lock.png"
        }
        return decodeAsset(context, assetName) ?: decodeDrawable(context, when (theme) {
            ThemeType.SPACE -> com.screenmotion.app.R.drawable.theme_space_lock
            ThemeType.AQUARIUM -> com.screenmotion.app.R.drawable.theme_aquarium_lock
            ThemeType.NATURE -> com.screenmotion.app.R.drawable.theme_nature_lock
            ThemeType.VEHICLE -> com.screenmotion.app.R.drawable.theme_vehicle_lock
        })
    }

    fun loadVehicleLockBitmap(
        context: Context,
        vehicle: com.screenmotion.app.data.VehicleType
    ): Bitmap? {
        val path = com.screenmotion.app.render.VehiclePainter.lockAssetNameFor(vehicle)
        return decodeAsset(context, path)
            ?: decodeAsset(context, "themes/vehicles/" + com.screenmotion.app.render.VehiclePainter.assetNameFor(vehicle))
    }

    private fun decodeAsset(context: Context, path: String): Bitmap? =
        try {
            context.assets.open(path).use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }

    private fun decodeDrawable(context: Context, resId: Int): Bitmap? =
        try {
            BitmapFactory.decodeResource(context.resources, resId)
        } catch (_: Exception) {
            null
        }

    /** Center-crop / letterbox source into phone wallpaper size. */
    fun scaleToWallpaper(context: Context, src: Bitmap): Bitmap {
        val (tw, th) = wallpaperSize(context)
        if (src.width == tw && src.height == th) return src
        val out = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(0xFF000000.toInt())
        val scale = maxOf(tw.toFloat() / src.width, th.toFloat() / src.height)
        val dw = src.width * scale
        val dh = src.height * scale
        val left = (tw - dw) / 2f
        val top = (th - dh) / 2f
        val dst = android.graphics.RectF(left, top, left + dw, top + dh)
        canvas.drawBitmap(src, null, dst, null)
        return out
    }

    private fun wallpaperSize(context: Context): Pair<Int, Int> {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = wm.currentWindowMetrics.bounds
            // Prefer portrait phone wallpaper size; cap for memory
            val w = b.width().coerceIn(720, 1440)
            val h = b.height().coerceIn(1280, 3200)
            w to h
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            dm.widthPixels.coerceIn(720, 1440) to dm.heightPixels.coerceIn(1280, 3200)
        }
    }

    fun saveBitmapToPictures(context: Context, bitmap: Bitmap, theme: ThemeType): Uri? {
        val name = "ScreenMotion_${theme.name.lowercase()}_lock_${System.currentTimeMillis()}.png"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ScreenMotion")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return null
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } else {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ScreenMotion")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, name)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "save failed", e)
            // Cache fallback for FileProvider share
            try {
                val cache = File(context.cacheDir, "lock_wallpapers").apply { mkdirs() }
                val file = File(cache, name)
                FileOutputStream(file).use { out: OutputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (e2: Exception) {
                Log.e(TAG, "cache save failed", e2)
                null
            }
        }
    }

    fun openWallpaperPicker(context: Context, uri: Uri) {
        val grantFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        // Prefer crop-and-set when available; extras help Huawei/EMUI pickers.
        val crop = Intent("android.service.wallpaper.CROP_AND_SET_WALLPAPER").apply {
            setDataAndType(uri, "image/*")
            addFlags(grantFlags or Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("mimeType", "image/*")
            putExtra("wallpaper", true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                putExtra("which", WallpaperManager.FLAG_LOCK)
                putExtra("fromWallpaper", true)
            }
        }
        val attach = Intent(Intent.ACTION_ATTACH_DATA).apply {
            setDataAndType(uri, "image/*")
            addFlags(grantFlags or Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("mimeType", "image/*")
            putExtra("setWallpaper", true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                putExtra("which", WallpaperManager.FLAG_LOCK)
            }
        }
        val huawei = Intent().apply {
            setClassName(
                "com.android.thememanager",
                "com.android.thememanager.activity.HwWallpaperPreviewActivity"
            )
            setDataAndType(uri, "image/*")
            addFlags(grantFlags or Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("wallpaperType", 1) // often lock on EMUI
        }
        val chooser = Intent.createChooser(attach, context.getString(com.screenmotion.app.R.string.lock_picker_title)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(crop, huawei))
        }
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            try {
                context.startActivity(attach)
            } catch (e: Exception) {
                Log.e(TAG, "picker open failed", e)
            }
        }
    }
}
