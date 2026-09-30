package ar.accesospng;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

public class ClockWeatherProvider extends AppWidgetProvider {
    static RemoteViews views(Context c) {
        SharedPreferences p = c.getSharedPreferences("weather", Context.MODE_PRIVATE);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.clock_weather);
        long age = System.currentTimeMillis() - p.getLong("updated", 0);
        String line = p.getString("line", p.getString("error", "Elegir ubicación"));
        if (p.contains("city") && !p.contains("line")) line = "Clima pendiente";
        if (p.contains("line") && age > 3600000) line += " · sin actualizar";
        v.setTextViewText(R.id.weather_line, line);
        v.setTextViewText(R.id.weather_city, p.getString("city", "Tocá para configurar el clima"));
        Intent edit = new Intent(c, WeatherConfigActivity.class);
        v.setOnClickPendingIntent(R.id.weather_root, PendingIntent.getActivity(c, 700, edit, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        return v;
    }
    static void updateAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, ClockWeatherProvider.class));
        if (ids.length > 0) m.updateAppWidget(ids, views(c));
    }
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        updateAll(c);
        WeatherJob.refresh(c);
    }
}
