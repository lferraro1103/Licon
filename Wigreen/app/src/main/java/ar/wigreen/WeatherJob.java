package ar.wigreen;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.List;

public class WeatherJob extends JobService {
    static void refresh(Context c) {
        JobScheduler scheduler = (JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.schedule(new JobInfo.Builder(701, new ComponentName(c, WeatherJob.class)).setOverrideDeadline(0).build());
    }
    static boolean read(Context c) {
        SharedPreferences p = c.getSharedPreferences("weather", MODE_PRIVATE);
        String key = p.getString("key", "");
        try {
            List<SamsungWeather.City> cities = SamsungWeather.cities(c);
            for (SamsungWeather.City city : cities) if (city.key.equals(key)) {
                p.edit().putString("city", city.name).putString("line", city.line()).putString("source", city.source).putLong("updated", city.updated).remove("error").apply();
                ClockWeatherProvider.updateAll(c); return true;
            }
            p.edit().putString("error", key.isEmpty() ? "Elegir ubicación" : "Ubicación no disponible").apply();
        } catch (SecurityException e) {
            p.edit().putString("error", "Permitir acceso al clima").apply();
        } catch (RuntimeException e) {
            p.edit().putString("error", "Clima no disponible").apply();
        }
        ClockWeatherProvider.updateAll(c); return false;
    }
    @Override public boolean onStartJob(JobParameters params) {
        new Thread(() -> { read(this); jobFinished(params, false); }, "Wigreen-weather").start();
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) { return true; }
}


