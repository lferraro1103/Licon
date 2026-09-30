package ar.wigreen;
import android.app.job.*;
import android.content.*;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import java.util.List;

public class WeatherJob extends JobService {
    private CancellationSignal active;
    static void refresh(Context c) {
        if (!c.getSharedPreferences("weather", MODE_PRIVATE).contains("key")) return;
        ((JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).schedule(new JobInfo.Builder(701,new ComponentName(c,WeatherJob.class)).setOverrideDeadline(0).build());
    }
    static boolean storeCities(Context c,List<SamsungWeather.City> cities) {
        SharedPreferences p=c.getSharedPreferences("weather",MODE_PRIVATE);
        String key=p.getString("key","");
        for(SamsungWeather.City city:cities) if(city.key.equals(key)) {
            if (!city.name.equals(p.getString("city","")) || !city.line().equals(p.getString("line","")) || !city.source.equals(p.getString("source","")) || city.updated!=p.getLong("updated",0) || p.contains("error"))
                p.edit().putString("city",city.name).putString("line",city.line()).putString("source",city.source).putLong("updated",city.updated).remove("error").apply();
            ClockWeatherProvider.updateIfChanged(c);return true;
        }
        return false;
    }
    static boolean read(Context c,CancellationSignal cancel) {
        SharedPreferences p=c.getSharedPreferences("weather",MODE_PRIVATE);
        if (!p.contains("key")) return false;
        String error;
        try {
            List<SamsungWeather.City> cities=SamsungWeather.cities(c,cancel);
            if(cancel!=null)cancel.throwIfCanceled();
            if(storeCities(c,cities))return true;
            error="Ubicación no disponible";
        } catch(android.os.OperationCanceledException e) { return false; }
        catch(SecurityException e) {error="Permitir acceso al clima";}
        catch(RuntimeException e) {error="Clima no disponible";}
        if(cancel!=null && cancel.isCanceled())return false;
        if(!error.equals(p.getString("error","")))p.edit().putString("error",error).apply();
        ClockWeatherProvider.updateIfChanged(c);return false;
    }
    @Override public boolean onStartJob(JobParameters params) {
        final CancellationSignal signal=new CancellationSignal();active=signal;
        new Thread(()->{try{read(this,signal);}finally{new Handler(Looper.getMainLooper()).post(()->{if(!signal.isCanceled())jobFinished(params,false);if(active==signal)active=null;});}},"Wigreen-weather").start();return true;
    }
    @Override public boolean onStopJob(JobParameters params) {if(active!=null){active.cancel();active=null;}return true;}
}
