package com.example.habit_calendar

import android.app.Activity
import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.annotation.Keep
import androidx.core.content.res.ResourcesCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Keep
class ProgressWidgetConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val startDateCalendar = Calendar.getInstance()
    private val endDateCalendar = Calendar.getInstance().apply { add(Calendar.MONTH, 3) }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_progress_widget_config)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Load SF Pro Fonts safely
        val sfProBold = try {
            ResourcesCompat.getFont(this, R.font.sf_pro_bold)
        } catch (e: Exception) {
            null
        }

        val sfProRegular = try {
            ResourcesCompat.getFont(this, R.font.sf_pro_regular)
        } catch (e: Exception) {
            null
        }

        // Find existing UI views
        val editTitle = findViewById<EditText>(R.id.edit_goal_title)
        val btnStart = findViewById<Button>(R.id.btn_start_date)
        val btnEnd = findViewById<Button>(R.id.btn_end_date)
        val btnSave = findViewById<Button>(R.id.btn_save_widget)

        // Apply SF Pro typefaces directly to screen elements
        if (sfProRegular != null) {
            editTitle?.typeface = sfProRegular
            btnStart?.typeface = sfProRegular
            btnEnd?.typeface = sfProRegular
        }
        if (sfProBold != null) {
            btnSave?.typeface = sfProBold
        }

        btnStart.text = "Start: ${dateFormat.format(startDateCalendar.time)}"
        btnEnd.text = "End: ${dateFormat.format(endDateCalendar.time)}"

        btnStart.setOnClickListener {
            val dialog = DatePickerDialog(
                this,
                android.R.style.Theme_DeviceDefault_Dialog_Alert, // Restores the modern calendar view
                { _, year, month, day ->
                    startDateCalendar.set(year, month, day)
                    btnStart.text = "Start: ${dateFormat.format(startDateCalendar.time)}"
                },
                startDateCalendar.get(Calendar.YEAR),
                startDateCalendar.get(Calendar.MONTH),
                startDateCalendar.get(Calendar.DAY_OF_MONTH)
            )
            dialog.show()

            if (sfProRegular != null) {
                dialog.getButton(DatePickerDialog.BUTTON_POSITIVE)?.typeface = sfProBold
                dialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.typeface = sfProRegular
            }
        }

        btnEnd.setOnClickListener {
            val dialog = DatePickerDialog(
                this,
                android.R.style.Theme_DeviceDefault_Dialog_Alert, // Restores the modern calendar view
                { _, year, month, day ->
                    endDateCalendar.set(year, month, day)
                    btnEnd.text = "End: ${dateFormat.format(endDateCalendar.time)}"
                },
                endDateCalendar.get(Calendar.YEAR),
                endDateCalendar.get(Calendar.MONTH),
                endDateCalendar.get(Calendar.DAY_OF_MONTH)
            )
            dialog.show()

            if (sfProRegular != null) {
                dialog.getButton(DatePickerDialog.BUTTON_POSITIVE)?.typeface = sfProBold
                dialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.typeface = sfProRegular
            }
        }

        btnSave.setOnClickListener {
            val context = this@ProgressWidgetConfigActivity
            val title = editTitle.text.toString().ifEmpty { "Current Goal" }
            val prefs = context.getSharedPreferences("ProgressWidgetPrefs", Context.MODE_PRIVATE)

            prefs.edit()
                .putString("title_$appWidgetId", title)
                .putLong("start_$appWidgetId", startDateCalendar.timeInMillis)
                .putLong("end_$appWidgetId", endDateCalendar.timeInMillis)
                .apply()

            val appWidgetManager = AppWidgetManager.getInstance(context)
            ProgressWidgetProvider.updateWidget(context, appWidgetManager, appWidgetId)

            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}