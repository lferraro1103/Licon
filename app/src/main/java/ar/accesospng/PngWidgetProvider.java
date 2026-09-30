package ar.accesospng;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.RemoteViews;
import java.io.File;

public class PngWidgetProvider extends AppWidgetProvider {
    static SharedPreferences store(Context c) { return c.getSharedPreferences("widgets", Context.MODE_PRIVATE); }
    static RemoteViews views(Context c, Bitmap bitmap, ComponentName component, String name, int id) {
        RemoteViews views = new RemoteViews(c.getPackageName(), R.layout.png_widget);
        if (bitmap != null) views.setImageViewBitmap(R.id.widget_image, bitmap);
        views.setContentDescription(R.id.widget_image, name);
        Intent launch = component == null ? new Intent(c, MainActivity.class) : new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(component);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        PendingIntent click = PendingIntent.getActivity(c, id, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, click);
        return views;
    }
    static void update(Context c, int id) {
        SharedPreferences p = store(c);
        String token = p.getString("widget." + id, "");
        Bitmap bitmap = token.isEmpty() ? null : BitmapFactory.decodeFile(new File(c.getFilesDir(), "widget-" + token + ".png").getPath());
        ComponentName component = ComponentName.unflattenFromString(p.getString(token + ".component", ""));
        String name = p.getString(token + ".name", "Licon");
        AppWidgetManager.getInstance(c).updateAppWidget(id, views(c, bitmap, component, name, id));
    }
    @Override public void onUpdate(Context c, AppWidgetManager manager, int[] ids) { for (int id : ids) update(c,id); }
    @Override public void onDeleted(Context c, int[] ids) {
        SharedPreferences p = store(c);
        for (int id : ids) {
            String token = p.getString("widget." + id, "");
            if (!token.isEmpty()) {
                new File(c.getFilesDir(), "widget-" + token + ".png").delete();
                p.edit().remove("widget." + id).remove(token + ".component").remove(token + ".name").apply();
            }
        }
    }
}
