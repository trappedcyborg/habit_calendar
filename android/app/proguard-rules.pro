# Keep Widget Provider and MainActivity from being stripped by R8 during release builds
-keep class com.example.habit_calendar.HabitCalendarWidgetProvider { *; }
-keep class com.example.habit_calendar.MainActivity { *; }
-keepclassmembers class * extends android.appwidget.AppWidgetProvider {
    public <init>();
}