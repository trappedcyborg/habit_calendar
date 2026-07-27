package com.example.habit_calendar

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.widget.RemoteViews
import androidx.annotation.Keep
import androidx.core.content.res.ResourcesCompat
import java.util.Calendar

@Keep
class ProgressWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("ProgressWidgetPrefs", Context.MODE_PRIVATE).edit()
        for (appWidgetId in appWidgetIds) {
            prefs.remove("title_$appWidgetId")
            prefs.remove("start_$appWidgetId")
            prefs.remove("end_$appWidgetId")
        }
        prefs.apply()
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleMidnightUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_DATE_CHANGED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, ProgressWidgetProvider::class.java)
            )
            for (id in ids) {
                updateWidget(context, appWidgetManager, id)
            }
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            try {
                val views = RemoteViews(context.packageName, R.layout.progress_widget)
                val prefs = context.getSharedPreferences("ProgressWidgetPrefs", Context.MODE_PRIVATE)

                val title = prefs.getString("title_$appWidgetId", "Current Goal") ?: "Current Goal"
                val startMillis = prefs.getLong("start_$appWidgetId", System.currentTimeMillis())
                val endMillis = prefs.getLong("end_$appWidgetId", System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000))

                val now = System.currentTimeMillis()
                val totalDuration = (endMillis - startMillis).coerceAtLeast(1)
                val elapsed = (now - startMillis).coerceIn(0, totalDuration)
                val percentage = ((elapsed.toDouble() / totalDuration.toDouble()) * 100).toInt().coerceIn(0, 100)

                // Pass context, percentage, and title so the entire layout is rendered in SF Pro
                val bitmap = createWidgetBitmap(context, percentage, title)
                views.setImageViewBitmap(R.id.progress_widget_image, bitmap)

                try {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    if (launchIntent != null) {
                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            appWidgetId,
                            launchIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.progress_widget_container, pendingIntent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun scheduleMidnightUpdate(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, ProgressWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val midnight = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                try {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, midnight.timeInMillis, pendingIntent)
                } catch (se: Exception) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, midnight.timeInMillis, pendingIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun createWidgetBitmap(context: Context, percentage: Int, title: String): Bitmap {
            val width = 720
            val height = 720
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 0. Draw Dark Rounded Background Card
            val bgPaint = Paint().apply {
                color = Color.parseColor("#141414")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val cornerRadius = 64f
            val bgRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, bgPaint)

            // Load SF Pro Bold
            val customTypeface = try {
                ResourcesCompat.getFont(context, R.font.sf_pro_bold)
            } catch (e: Exception) {
                null
            }

            // 1. Title Text
            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 52f
                textAlign = Paint.Align.CENTER
                isSubpixelText = true
                isAntiAlias = true
                if (customTypeface != null) {
                    typeface = customTypeface
                } else {
                    isFakeBoldText = true
                }
            }
            canvas.drawText(title, width / 2f, 95f, titlePaint)

            // 2. Scaled Down Ring Bounds for extra padding
            val strokeWidth = 44f       // Slightly slimmer stroke (was 54f)
            val topOffset = 140f
            val ringSize = 480f         // Scaled down ring size (was 600f)
            val leftMargin = (width - ringSize) / 2f + strokeWidth / 2f
            val topMargin = topOffset + strokeWidth / 2f
            val rect = RectF(
                leftMargin,
                topMargin,
                leftMargin + ringSize - strokeWidth,
                topMargin + ringSize - strokeWidth
            )

            // 3. Track & Progress Paints
            val trackPaint = Paint().apply {
                color = Color.parseColor("#2A2A2A")
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                isAntiAlias = true
            }

            val progressPaint = Paint().apply {
                color = Color.parseColor("#FF5252")
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                strokeCap = Paint.Cap.ROUND
                isAntiAlias = true
            }

            // Percentage Text scaled to match smaller ring
            val percentagePaint = Paint().apply {
                color = Color.WHITE
                textSize = 108f        // Proportional text size (was 132f)
                textAlign = Paint.Align.CENTER
                isSubpixelText = true
                isAntiAlias = true
                if (customTypeface != null) {
                    typeface = customTypeface
                } else {
                    isFakeBoldText = true
                }
            }

            // Draw Track
            canvas.drawArc(rect, 0f, 360f, false, trackPaint)

            // Draw Progress Arc
            val sweepAngle = (percentage / 100f) * 360f
            canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint)

            // Draw Percentage Text centered in ring
            val ringCenterY = topOffset + (ringSize / 2f)
            val textY = ringCenterY - ((percentagePaint.descent() + percentagePaint.ascent()) / 2f)
            canvas.drawText("$percentage%", width / 2f, textY, percentagePaint)

            return bitmap
        }
    }
}