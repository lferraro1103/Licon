package ar.wigreen;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextClock;
import android.widget.TextView;
import java.util.List;

/** Checks the live Samsung interface without publishing or changing any locations. */
public class WeatherInstrumentation extends Instrumentation {
    private Bundle args;
    @Override public void onCreate(Bundle b) { super.onCreate(b); args=b; start(); }
    @Override public void onStart() {
        Bundle result=new Bundle();
        try {
            List<SamsungWeather.City> cities=SamsungWeather.cities(getTargetContext());
            if(cities.isEmpty()) throw new AssertionError("Samsung returned no weather");
            for(SamsungWeather.City city:cities) {
                if(city.key.isEmpty() || city.name.isEmpty() || !Double.isFinite(city.temperature) || city.temperature < -100 || city.temperature > 70) throw new AssertionError("weather schema");
            }
            runOnMainSync(() -> {
                View root=ClockWeatherProvider.views(getTargetContext()).apply(getTargetContext(),null);
                if(!(root.getBackground() instanceof ColorDrawable) || ((ColorDrawable)root.getBackground()).getColor()!=Color.TRANSPARENT) throw new AssertionError("widget transparency");
                if(!(root.findViewById(R.id.weather_clock) instanceof TextClock)) throw new AssertionError("live clock");
                if(((TextView)root.findViewById(R.id.weather_clock)).getCurrentTextColor()!=0xFF687860) throw new AssertionError("sage palette");
            });
            java.util.Map<String, ?> before = getTargetContext().getSharedPreferences("weather",0).getAll();
            android.os.CancellationSignal stopped = new android.os.CancellationSignal(); stopped.cancel();
            if (WeatherJob.read(getTargetContext(), stopped)) throw new AssertionError("canceled weather read");
            if (!before.equals(getTargetContext().getSharedPreferences("weather",0).getAll())) throw new AssertionError("canceled job changed preferences");
            result.putString("stream","PASS: Samsung weather readable with declared permission; valid temperature/schema; live TextClock; transparent background; sage palette. No location data logged.\n");
            if(args!=null && "true".equals(args.getString("open"))) startActivitySync(new Intent(getTargetContext(),WeatherConfigActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream","FAIL: "+e+"\n");finish(Activity.RESULT_CANCELED,result);}
    }
}



