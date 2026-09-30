package ar.wigreen;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
public class SearchInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        for(String action:new String[]{"google","search","voice","lens"}){
            ResolveInfo r=getTargetContext().getPackageManager().resolveActivity(SearchLaunchActivity.target(getTargetContext(),action),0);
            if(r==null || !r.activityInfo.exported)throw new AssertionError("Unavailable action: "+action);
        }
        Bitmap image=Bitmap.createBitmap(1000,180,Bitmap.Config.ARGB_8888);
        runOnMainSync(()->{View root=SearchWidgetProvider.views(getTargetContext()).apply(getTargetContext(),null);root.measure(View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(180,View.MeasureSpec.EXACTLY));root.layout(0,0,1000,180);root.draw(new Canvas(image));});
        if(Color.alpha(image.getPixel(0,0))!=0)throw new AssertionError("Corner transparency");
        try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"search-preview.png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}
        result.putString("stream","PASS: Google, search, voice and Lens resolve to exported activities; RemoteViews renders; corners transparent.\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream","FAIL: "+e+"\n");finish(Activity.RESULT_CANCELED,result);}}
}

