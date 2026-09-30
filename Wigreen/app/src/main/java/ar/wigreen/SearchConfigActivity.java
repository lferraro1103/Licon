package ar.wigreen;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Bundle;
import android.widget.*;
public class SearchConfigActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setGravity(android.view.Gravity.CENTER_VERTICAL); int d=(int)(24*getResources().getDisplayMetrics().density); body.setPadding(d,d,d,d);body.setBackgroundColor(0xFFF4F4ED);
        TextView title=new TextView(this);title.setText("Barra Google · Salvia");title.setTextSize(26);title.setTextColor(0xFF485840);body.addView(title);
        FrameLayout preview=new FrameLayout(this);body.addView(preview,new LinearLayout.LayoutParams(-1,d*4));preview.addView(SearchWidgetProvider.views(this).apply(this,preview));
        TextView info=new TextView(this);info.setText("Google, búsqueda, voz y Lens. Ajustá el ancho desde tu pantalla de inicio.");info.setTextColor(0xFF687860);body.addView(info);
        Button add=new Button(this);add.setText("Agregar barra al inicio");body.addView(add);add.setOnClickListener(v->{AppWidgetManager m=AppWidgetManager.getInstance(this);if(m.isRequestPinAppWidgetSupported())m.requestPinAppWidget(new ComponentName(this,SearchWidgetProvider.class),null,null);else Toast.makeText(this,"Agregala desde Widgets → Wigreen",Toast.LENGTH_LONG).show();});setContentView(body);
    }
}


