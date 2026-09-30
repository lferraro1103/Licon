package ar.wigreen;

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
    static void updateIfChanged(Context c) { update(c, false); }
    static void updateAll(Context c) { update(c, true); }
    private static void update(Context c, boolean force) {
        SharedPreferences p = c.getSharedPreferences("weather", Context.MODE_PRIVATE);
        String line = p.getString("line", p.getString("error", "Elegir ubicación"));
        if (p.contains("city") && !p.contains("line")) line = "Clima pendiente";
        if (p.contains("line") && System.currentTimeMillis()-p.getLong("updated",0)>3600000) line += " · sin actualizar";
        String city = p.getString("city", "Tocá para configurar el clima");
        String visual = line + "\n" + city;
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, ClockWeatherProvider.class));
        SharedPreferences.Editor edit = p.edit();
        boolean changed = false;
        RemoteViews remote = null;
        for (int id : ids) {
            String marker = "render." + id;
            if (force || !visual.equals(p.getString(marker, null))) {
                if (remote == null) remote = views(c);
                m.updateAppWidget(id, remote);
                edit.putString(marker, visual); changed = true;
            }
        }
        if (changed) edit.apply();
    }
    @Override public void onDeleted(Context c, int[] ids) {
        SharedPreferences.Editor edit = c.getSharedPreferences("weather", Context.MODE_PRIVATE).edit();
        for (int id : ids) edit.remove("render." + id);
        edit.apply();
    }
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        updateIfChanged(c);
        WeatherJob.refresh(c);
    }
}




