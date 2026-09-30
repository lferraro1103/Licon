package ar.wigreen;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.*;
import java.util.List;

public class WeatherConfigActivity extends Activity {
    private LinearLayout choices;
    private TextView status;
    private FrameLayout preview;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(24 * getResources().getDisplayMetrics().density);
        box.setPadding(pad,pad*2,pad,pad); box.setBackgroundColor(0xFFF4F4ED);
        TextView title = text("Hora, fecha y clima", 28); box.addView(title);
        preview = new FrameLayout(this); box.addView(preview, new LinearLayout.LayoutParams(-1, (int)(200 * getResources().getDisplayMetrics().density)));
        showPreview();
        box.addView(text("Elegí una de tus ubicaciones de Samsung Clima. Tocá el widget para cambiarla.", 16));
        choices = new LinearLayout(this); choices.setOrientation(LinearLayout.VERTICAL); box.addView(choices);
        Button original = new Button(this); original.setText("Abrir Samsung Clima"); box.addView(original);
        original.setOnClickListener(v -> {
            Intent detail = new Intent("com.samsung.android.weather.intent.action.DETAIL").setPackage("com.sec.android.daemonapp");
            try { startActivity(detail); }
            catch (RuntimeException e) {
                Intent launch = getPackageManager().getLaunchIntentForPackage("com.sec.android.daemonapp");
                try { if (launch != null) startActivity(launch); else status.setText("Abrí la app Clima desde tu inicio."); }
                catch (RuntimeException error) { status.setText("Abrí la app Clima desde tu inicio."); }
            }
        });
        Button add = new Button(this); add.setText("Agregar o cambiar ubicación"); box.addView(add);
        add.setOnClickListener(v -> {
            Intent i = new Intent("com.samsung.android.weather.intent.action.CITYLIST").setPackage("com.sec.android.daemonapp");
            try { startActivity(i); } catch (RuntimeException e) {
                Intent launch = getPackageManager().getLaunchIntentForPackage("com.sec.android.daemonapp");
                try { if (launch != null) startActivity(launch); else status.setText("No se encontró Samsung Clima."); }
                catch (RuntimeException error) { status.setText("Abrí Samsung Clima y agregá allí la ciudad."); }
            }
        });
        Button permit = new Button(this); permit.setText("Permitir lectura del clima"); box.addView(permit);
        permit.setOnClickListener(v -> requestPermissions(new String[]{SamsungWeather.PERMISSION}, 701));
        Button pin = new Button(this); pin.setText("Agregar widget al inicio"); box.addView(pin);
        pin.setOnClickListener(v -> {
            if (!getSharedPreferences("weather", MODE_PRIVATE).contains("key")) { status.setText("Primero elegí una ubicación."); return; }
            AppWidgetManager m = AppWidgetManager.getInstance(this);
            if (m.isRequestPinAppWidgetSupported()) m.requestPinAppWidget(new ComponentName(this, ClockWeatherProvider.class), null, null);
            else status.setText("Agregalo desde Widgets → Wigreen en tu pantalla de inicio.");
        });
        status = text("", 14); box.addView(status);
        box.addView(text("Usa los datos de Samsung Clima (The Weather Channel cuando es su proveedor). Todo queda en el teléfono. Para refrescar datos antiguos, abrí Samsung Clima.", 13));
        scroll.addView(box); setContentView(scroll);
    }
    private TextView text(String s, int size) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(0xFF687860); t.setPadding(0,12,0,12); return t; }
    @Override public void onResume() { super.onResume(); load(); }
    @Override public void onRequestPermissionsResult(int r, String[] permissions, int[] grants) { super.onRequestPermissionsResult(r,permissions,grants); load(); }
    private void load() {
        if (choices == null) return;
        choices.removeAllViews();
        if (checkSelfPermission(SamsungWeather.PERMISSION) != PackageManager.PERMISSION_GRANTED) { status.setText("Permití leer el clima para ver tus ubicaciones."); return; }
        new Thread(() -> {
            try {
                List<SamsungWeather.City> cities = SamsungWeather.cities(this);
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    choices.removeAllViews();
                    for (SamsungWeather.City city : cities) {
                        Button b = new Button(this); b.setText(city.name + " · " + city.line()); choices.addView(b);
                        b.setOnClickListener(v -> {
                            getSharedPreferences("weather", MODE_PRIVATE).edit().clear().putString("key",city.key).putString("city",city.name).putString("line",city.line()).putLong("updated",city.updated).putString("source",city.source).apply();
                            ClockWeatherProvider.updateAll(this); status.setText("Ubicación guardada: " + city.name);
                            showPreview();
                        });
                    }
                    status.setText(cities.isEmpty() ? "Agregá una ciudad en Samsung Clima y volvé acá." : "Elegí tu ubicación. La selección queda guardada.");
                    WeatherJob.storeCities(this, cities); showPreview();
                });
            } catch (RuntimeException e) { runOnUiThread(() -> { if (!isDestroyed()) status.setText("No se pudo leer Samsung Clima."); }); }
        },"Wigreen-cities").start();
    }
    private void showPreview() {
        preview.removeAllViews(); preview.addView(ClockWeatherProvider.views(this).apply(this, preview));
    }
}



