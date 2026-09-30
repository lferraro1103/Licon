package ar.accesospng;

import android.app.Activity;
import android.app.Instrumentation;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RemoteViews;
import java.io.*;

/** Widget regression test that does not read or change the user's draft. */
public class WidgetInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            Bitmap icon = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(icon);
            android.graphics.Paint paint = new android.graphics.Paint(); paint.setColor(Color.RED);
            canvas.drawRect(72,72,120,120,paint);
            File fixture = new File(getTargetContext().getCacheDir(), "widget-regression.png");
            try (OutputStream out = new FileOutputStream(fixture)) { icon.compress(Bitmap.CompressFormat.PNG,100,out); }
            Bitmap stored = android.graphics.BitmapFactory.decodeFile(fixture.getPath());
            check(Color.alpha(stored.getPixel(0,0)) == 0, "stored alpha");
            ComponentName settings = getTargetContext().getPackageManager().getLaunchIntentForPackage("com.android.settings").getComponent();
            RemoteViews remote = PngWidgetProvider.views(getTargetContext(), stored, settings, "Widget test", -998);
            runOnMainSync(() -> {
                View root = remote.apply(getTargetContext(), null);
                check(root.getBackground() instanceof ColorDrawable && ((ColorDrawable)root.getBackground()).getColor() == Color.TRANSPARENT, "transparent root");
                check(((ViewGroup)root).getChildCount() == 1, "no badge overlay");
                ImageView image = root.findViewById(R.id.widget_image);
                check(image.getBackground() instanceof ColorDrawable && ((ColorDrawable)image.getBackground()).getColor() == Color.TRANSPARENT, "transparent image background");
                Bitmap rendered = Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);
                root.measure(View.MeasureSpec.makeMeasureSpec(192,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(192,View.MeasureSpec.EXACTLY)); root.layout(0,0,192,192); root.draw(new android.graphics.Canvas(rendered));
                check(Color.alpha(rendered.getPixel(0,0)) == 0 && Color.alpha(rendered.getPixel(60,60)) == 0, "rendered margin transparency");
                check(rendered.getPixel(96,96) == Color.RED, "rendered PNG content");
            });
            check(AppWidgetManager.getInstance(getTargetContext()).isRequestPinAppWidgetSupported(), "One UI pin widget support");
            fixture.delete();
            result.putString("stream", "PASS: persisted alpha; transparent widget root/image; no badge overlay; rendered transparent margins; PNG content; One UI widget pin support. User draft unchanged.\n");
            finish(Activity.RESULT_OK,result);
        } catch(Throwable e) { result.putString("stream", "FAIL: "+e+"\n"); finish(Activity.RESULT_CANCELED,result); }
    }
    private static void check(boolean condition,String name) { if (!condition) throw new AssertionError(name); }
}
