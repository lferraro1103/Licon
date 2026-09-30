package ar.accesospng;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.widget.EditText;
import java.io.*;
import java.lang.reflect.*;

/** On-device integration tests, dependency-free, same signature as release. */
public class SmokeInstrumentation extends Instrumentation {
    private Activity activity;
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            activity = startActivitySync(new Intent(Intent.ACTION_MAIN).setClassName(getTargetContext(), "ar.accesospng.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            Bitmap sample = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888); sample.eraseColor(Color.RED);
            // Real PNG decode and persistence, not mocked graphics.
            File fixture = new File(getTargetContext().getFilesDir(), "smoke.png");
            try (OutputStream out = new FileOutputStream(fixture)) { sample.compress(Bitmap.CompressFormat.PNG, 100, out); }
            Method decode = method("decode", File.class);
            Bitmap decoded = (Bitmap) decode.invoke(activity, fixture);
            check(decoded.getWidth() == 400 && decoded.getHeight() == 200, "PNG dimensions");
            File bad = new File(getTargetContext().getFilesDir(), "bad.png");
            try (OutputStream out = new FileOutputStream(bad)) { out.write(new byte[]{1,2,3}); }
            boolean rejected = false;
            try { decode.invoke(activity, bad); } catch (InvocationTargetException e) { rejected = e.getCause() instanceof IOException; }
            check(rejected, "corrupt PNG rejected");
            int apps = getTargetContext().getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0).size();
            check(apps > 1, "launcher apps visible");
            check(getTargetContext().getSystemService(ShortcutManager.class).isRequestPinShortcutSupported(), "launcher supports pinning");
            runOnMainSync(() -> {
                try {
                    field("png").set(activity, decoded);
                    field("scale").setInt(activity, 25);
                    Bitmap small = (Bitmap) method("scaledIcon", int.class).invoke(activity, 192);
                    check(alphaBounds(small)[0] == 48 && alphaBounds(small)[1] == 24, "25% scale and aspect");
                    check(Color.alpha(small.getPixel(0,0)) == 0, "transparent canvas");
                    field("scale").setInt(activity, 100);
                    Bitmap big = (Bitmap) method("scaledIcon", int.class).invoke(activity, 192);
                    check(alphaBounds(big)[0] == 192 && alphaBounds(big)[1] == 96, "100% scale and aspect");
                    Bitmap adaptive = (Bitmap) method("scaledIcon", int.class, float.class).invoke(activity, 288, 2f/3f);
                    check(alphaBounds(adaptive)[0] == 192, "adaptive inset");
                    check(Icon.createWithAdaptiveBitmap(adaptive).getType() == Icon.TYPE_ADAPTIVE_BITMAP, "adaptive type");
                    Intent settings = getTargetContext().getPackageManager().getLaunchIntentForPackage("com.android.settings");
                    check(settings != null, "settings launch target");
                    field("selected").set(activity, settings.getComponent()); field("appLabel").set(activity, "Ajustes"); field("scale").setInt(activity, 85);
                    ((EditText)field("label").get(activity)).setText("Ajustes PNG");
                    method("saveDraft").invoke(activity);
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            check(getTargetContext().getSharedPreferences("draft", 0).getInt("scale", 0) == 85, "draft scale saved");
            check(getTargetContext().getSharedPreferences("draft", 0).getString("label", "").equals("Ajustes PNG"), "draft name saved");
            // Remove temporary fixture and draft; leave user app fresh.
            fixture.delete(); bad.delete();
            getTargetContext().getSharedPreferences("draft", 0).edit().clear().commit();
            runOnMainSync(() -> activity.finish());
            result.putString("stream", "PASS: PNG decode; corrupt PNG; app visibility; pin support; 25/100% bounds; alpha; adaptive inset/type; draft persistence.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "FAIL: " + error + "\n"); finish(Activity.RESULT_CANCELED, result);
        }
    }
    private Field field(String name) throws Exception { Field f = activity.getClass().getDeclaredField(name); f.setAccessible(true); return f; }
    private Method method(String name, Class<?>... types) throws Exception { Method m = activity.getClass().getDeclaredMethod(name, types); m.setAccessible(true); return m; }
    private static void check(boolean condition, String name) { if (!condition) throw new AssertionError(name); }
    private static int[] alphaBounds(Bitmap bitmap) {
        int left=bitmap.getWidth(), right=-1, top=bitmap.getHeight(), bottom=-1;
        for(int y=0;y<bitmap.getHeight();y++) for(int x=0;x<bitmap.getWidth();x++) if(Color.alpha(bitmap.getPixel(x,y)) > 0) { left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y); }
        return new int[]{right-left+1,bottom-top+1};
    }
}
