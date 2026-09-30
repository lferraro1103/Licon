package ar.wigreen;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int d=(int)(24*getResources().getDisplayMetrics().density);body.setPadding(d,d,d,d);body.setBackgroundColor(0xFFF4F4ED);
        TextView title=new TextView(this);title.setText("Wigreen");title.setTextSize(36);title.setTextColor(0xFF485840);body.addView(title);
        TextView intro=new TextView(this);intro.setText("Tus widgets en verde salvia.");intro.setTextSize(17);intro.setTextColor(0xFF687860);body.addView(intro);
        Button weather=new Button(this);weather.setText("Hora, fecha y clima");body.addView(weather);weather.setOnClickListener(v->startActivity(new Intent(this,WeatherConfigActivity.class)));
        Button search=new Button(this);search.setText("Barra Google del tema");body.addView(search);search.setOnClickListener(v->startActivity(new Intent(this,SearchConfigActivity.class)));
        setContentView(body);
    }
}

