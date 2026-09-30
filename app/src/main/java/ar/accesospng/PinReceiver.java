package ar.accesospng;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class PinReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if ("ar.accesospng.WIDGET_PINNED".equals(intent.getAction())) {
            int id = intent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            String token = intent.getStringExtra("token");
            android.content.SharedPreferences p = PngWidgetProvider.store(context);
            if (id < 0 || token == null || !p.contains(token + ".component")) return;
            boolean owned = false;
            for (int ownId : android.appwidget.AppWidgetManager.getInstance(context).getAppWidgetIds(new android.content.ComponentName(context, PngWidgetProvider.class))) if (ownId == id) owned = true;
            if (!owned) return;
            p.edit().putString("widget." + id, token).commit();
            PngWidgetProvider.update(context, id);
            Toast.makeText(context, "Acceso transparente agregado, sin insignia", Toast.LENGTH_LONG).show();
            return;
        }
        if ("ar.accesospng.PINNED".equals(intent.getAction())) {
            Toast.makeText(context, "Acceso agregado al inicio", Toast.LENGTH_LONG).show();
        }
    }
}
