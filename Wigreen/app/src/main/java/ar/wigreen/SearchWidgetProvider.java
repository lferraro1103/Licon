package ar.wigreen;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
public class SearchWidgetProvider extends AppWidgetProvider {
    static RemoteViews views(Context c) {
        RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.search_widget);
        link(c,v,R.id.search_google,"google"); link(c,v,R.id.search_space,"search");
        link(c,v,R.id.search_magnifier,"search"); link(c,v,R.id.search_voice,"voice"); link(c,v,R.id.search_lens,"lens"); return v;
    }
    private static void link(Context c,RemoteViews v,int id,String action) {
        Intent i=new Intent(c,SearchLaunchActivity.class).setAction(action);
        v.setOnClickPendingIntent(id,PendingIntent.getActivity(c,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
    }
    @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids) { m.updateAppWidget(ids,views(c)); }
}


