package com.screenmotion.app.ui

import android.os.Bundle
import android.view.Gravity
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.screenmotion.app.R
import com.screenmotion.app.data.ThemeType

/**
 * In-app store-listing style showcase: hero, features, theme gallery.
 */
class ShowcaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_showcase)

        findViewById<TextView>(R.id.btnCloseShowcase).setOnClickListener { finish() }

        val gallery = findViewById<GridLayout>(R.id.themeGallery)
        gallery.removeAllViews()
        for (theme in ThemeType.entries) {
            val card = MaterialCardView(this).apply {
                radius = 16f * resources.displayMetrics.density
                setCardBackgroundColor(getColor(R.color.bg_card))
                strokeWidth = (1 * resources.displayMetrics.density).toInt()
                strokeColor = getColor(R.color.stroke)
                val lp = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }
                layoutParams = lp
            }
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(28, 28, 28, 28)
                gravity = Gravity.CENTER_HORIZONTAL
            }
            col.addView(TextView(this).apply {
                text = theme.emoji
                textSize = 36f
                gravity = Gravity.CENTER
            })
            col.addView(TextView(this).apply {
                text = theme.displayName
                setTextColor(getColor(R.color.text_primary))
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
                setPadding(0, 12, 0, 4)
            })
            col.addView(TextView(this).apply {
                text = theme.subtitle
                setTextColor(getColor(R.color.text_secondary))
                textSize = 11f
                gravity = Gravity.CENTER
            })
            card.addView(col)
            gallery.addView(card)
        }
    }
}
