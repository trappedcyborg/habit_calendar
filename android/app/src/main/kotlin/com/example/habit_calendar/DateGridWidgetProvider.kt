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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Keep
class DateGridWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)

        // Force an immediate update on placement
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(
            ComponentName(context, DateGridWidgetProvider::class.java)
        )
        for (id in ids) {
            updateWidget(context, appWidgetManager, id)
        }

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
                ComponentName(context, DateGridWidgetProvider::class.java)
            )
            for (id in ids) {
                updateWidget(context, appWidgetManager, id)
            }
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.habit_calendar_widget)

                val calendar = Calendar.getInstance()
                val isLeap = calendar.getActualMaximum(Calendar.DAY_OF_YEAR) == 366
                val totalDays = if (isLeap) 366 else 365
                val currentDay = calendar.get(Calendar.DAY_OF_YEAR)

                val bitmap = createWidgetBitmap(context, totalDays, currentDay)
                views.setImageViewBitmap(R.id.widget_grid_image, bitmap)

                try {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            0,
                            launchIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun scheduleMidnightUpdate(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, DateGridWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    1002, // Unique request code to avoid overlapping with Mini Grid Provider
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val midnight = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        midnight.timeInMillis,
                        pendingIntent
                    )
                } catch (se: Exception) {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        midnight.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun createWidgetBitmap(context: Context, totalDays: Int, currentDay: Int): Bitmap {
            val width = 1200
            val height = 800

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Safely load SF Pro fonts
            val sfProBold = try {
                ResourcesCompat.getFont(context, R.font.sf_pro_bold)
            } catch (e: Exception) { null }

            val sfProRegular = try {
                ResourcesCompat.getFont(context, R.font.sf_pro_regular)
            } catch (e: Exception) { null }

            // 1. Define Outer Padding for Clean Margins
            val paddingX = 64f     // Safe horizontal breathing room from outer edges
            val paddingTop = 64f    // Top padding above header text
            val paddingBottom = 54f // Bottom padding below grid

            val cols = 25
            val rows = 15
            val spacing = 7f      // Spacing between grid squares

            // 2. Available Drawing Dimensions
            val availableWidth = width - (paddingX * 2)
            val headerHeight = 70f // Dedicated height reserved for text header

            val availableGridHeight = height - paddingTop - headerHeight - paddingBottom

            // Calculate uniform box size based on available width/height
            val boxWidthFromWidth = (availableWidth - (spacing * (cols - 1))) / cols
            val boxWidthFromHeight = (availableGridHeight - (spacing * (rows - 1))) / rows
            val boxWidth = minOf(boxWidthFromWidth, boxWidthFromHeight)
            val boxHeight = boxWidth

            // Center grid horizontally within the padded canvas region
            val totalGridWidth = (cols * boxWidth) + ((cols - 1) * spacing)
            val gridStartX = paddingX + ((availableWidth - totalGridWidth) / 2f)
            val gridTopOffset = paddingTop + headerHeight

            // 3. Header Text: Date Title (Left Aligned with Grid Edge)
            val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.ENGLISH)
            val formattedDate = dateFormat.format(Date())

            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 42f
                isAntiAlias = true
                isSubpixelText = true
                if (sfProBold != null) typeface = sfProBold else isFakeBoldText = true
            }
            canvas.drawText(formattedDate, gridStartX, paddingTop + 32f, titlePaint)

            // Subtitle: "207/365 days passed"
            val daysPassed = (currentDay - 1).coerceAtLeast(0)
            val highlightText = "$daysPassed"
            val slashText = "/"
            val restText = "$totalDays days passed"

            // 1. Reduced textSize slightly to 37f so the numbers don't tower over lowercase letters
            val highlightPaint = Paint().apply {
                color = Color.parseColor("#FF5252")
                textSize = 37f
                isAntiAlias = true
                isSubpixelText = true
                if (sfProBold != null) typeface = sfProBold else isFakeBoldText = true
            }

            val restPaint = Paint().apply {
                color = Color.parseColor("#A0A0A0")
                textSize = 37f
                isAntiAlias = true
                isSubpixelText = true
                if (sfProRegular != null) typeface = sfProRegular
            }

            val totalRestWidth = restPaint.measureText(slashText + restText)
            val highlightWidth = highlightPaint.measureText(highlightText)
            val totalSubtitleWidth = highlightWidth + totalRestWidth

            val subtitleStartX = (gridStartX + totalGridWidth) - totalSubtitleWidth

            // 2. Adjust Y position slightly (+34f instead of +32f) so the smaller text sits centered with the Year title
            val subtitleY = paddingTop + 34f

            canvas.drawText(highlightText, subtitleStartX, subtitleY, highlightPaint)
            canvas.drawText(slashText + restText, subtitleStartX + highlightWidth, subtitleY, restPaint)

            // 5. Render Grid
            val paintPassed = Paint().apply {
                color = Color.WHITE
                isAntiAlias = true
            }

            val paintToday = Paint().apply {
                color = Color.parseColor("#FF5252")
                isAntiAlias = true
            }

            val paintFuture = Paint().apply {
                color = Color.parseColor("#2A2A2A")
                isAntiAlias = true
            }

            for (i in 0 until totalDays) {
                val dayNumber = i + 1

                val row = i / cols
                val col = i % cols

                val left = gridStartX + (col * (boxWidth + spacing))
                val top = gridTopOffset + (row * (boxHeight + spacing))

                val rect = RectF(
                    left,
                    top,
                    left + boxWidth,
                    top + boxHeight
                )

                val activePaint = when {
                    dayNumber == currentDay -> paintToday
                    dayNumber < currentDay -> paintPassed
                    else -> paintFuture
                }

                canvas.drawRoundRect(
                    rect,
                    boxWidth * 0.22f,
                    boxWidth * 0.22f,
                    activePaint
                )
            }

            return bitmap
        }
    }
}