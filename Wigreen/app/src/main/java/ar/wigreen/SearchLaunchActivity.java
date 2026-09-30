package ar.wigreen;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Toast;
public class SearchLaunchActivity extends Activity {
    static final String GOOGLE="com.google.android.googlequicksearchbox";
    static Intent target(Context c,String action) {
        if("lens".equals(action)) return new Intent(Intent.ACTION_VIEW,Uri.parse("googleapp://lens")).setPackage(GOOGLE);
        if("voice".equals(action)) return new Intent(RecognizerIntent.ACTION_WEB_SEARCH).setPackage(GOOGLE).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH).putExtra(RecognizerIntent.EXTRA_WEB_SEARCH_ONLY,true);
        if("google".equals(action)) { Intent home=c.getPackageManager().getLaunchIntentForPackage(GOOGLE); if(home!=null) return home; }
        return new Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH).setPackage(GOOGLE).putExtra(SearchManager.QUERY,"").putExtra(SearchManager.EXTRA_SELECT_QUERY,true);
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try { startActivity(target(this,getIntent().getAction())); }
        catch(RuntimeException e) {
            if("voice".equals(getIntent().getAction()) || "lens".equals(getIntent().getAction())) Toast.makeText(this,"Esta función necesita la app Google habilitada.",Toast.LENGTH_LONG).show();
            else try {startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com")));} catch(RuntimeException ignored){Toast.makeText(this,"No se encontró Google ni un navegador.",Toast.LENGTH_LONG).show();}
        }
        finish();
    }
}


