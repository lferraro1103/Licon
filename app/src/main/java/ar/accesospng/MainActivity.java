package ar.accesospng;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_PNG = 10;
    private static final int BG = 0xFF10121B, PANEL = 0xFF1D2030, INK = 0xFFF5F3FF, MUTED = 0xFFA5A9BC;
    private static final ExecutorService worker = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private ComponentName selected;
    private String appLabel = "";
    private Bitmap png;
    private int scale = 85;
    private Button appButton, pngButton, createButton;
    private EditText label;
    private TextView sizeLabel, status;
    private ImageView preview;
    private boolean busy;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("draft", MODE_PRIVATE);
        selected = ComponentName.unflattenFromString(prefs.getString("component", ""));
        appLabel = prefs.getString("appLabel", "");
        scale = prefs.getInt("scale", 85);
        if (state != null) {
            selected = ComponentName.unflattenFromString(state.getString("component", ""));
            appLabel = state.getString("appLabel", "");
            scale = state.getInt("scale", 85);
        }
        scale = Math.max(25, Math.min(100, scale));
        buildUi();
        label.setText(state == null ? prefs.getString("label", appLabel) : state.getString("label", appLabel));
        File cached = new File(getFilesDir(), "icon.png");
        {
            busy = true; refresh();
            worker.execute(() -> {
                Bitmap restored = null;
                try { if (cached.exists()) restored = decode(cached); } catch (IOException | RuntimeException ignored) { }
                final Bitmap result = restored;
                runOnUiThread(() -> { if (isDestroyed()) return; png = result; busy = false; refresh(); });
            });
        }
        refresh();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        LinearLayout outer = new LinearLayout(this); outer.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(24), dp(24), dp(24), dp(28));
        int width = Math.min(getResources().getDisplayMetrics().widthPixels, dp(560));
        outer.addView(body, new LinearLayout.LayoutParams(width, -2));
        scroll.addView(outer); setContentView(scroll); scroll.requestApplyInsets();
        TextView eyebrow = text("TU INICIO, A TU MANERA", 12, 0xFFB6A2FF); body.addView(eyebrow);
        TextView title = text("Licon", 32, INK); title.setTypeface(null, android.graphics.Typeface.BOLD); body.addView(title);
        TextView intro = text("Elegí una app y dale tu propio icono.", 16, MUTED); intro.setPadding(0, dp(6), 0, dp(24)); body.addView(intro);
        body.addView(text("1  ·  Aplicación", 16, INK));
        appButton = button("Seleccionar app"); body.addView(appButton); appButton.setOnClickListener(v -> chooseApp());
        TextView pngHeading = text("2  ·  Icono PNG", 16, INK); pngHeading.setPadding(0, dp(20), 0, 0); body.addView(pngHeading);
        pngButton = button("Elegir archivo PNG"); body.addView(pngButton);
        pngButton.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/png");
            try { startActivityForResult(pick, PICK_PNG); } catch (RuntimeException e) { message("No se encontró un selector de archivos."); }
        });
        TextView nameHeading = text("Nombre del acceso", 14, MUTED); nameHeading.setPadding(0, dp(16), 0, 0); body.addView(nameHeading);
        label = new EditText(this); label.setSingleLine(true); label.setTextColor(INK); label.setHintTextColor(MUTED); label.setHint("Nombre de la app"); label.setTextSize(16);
        label.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(60)});
        label.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO); body.addView(label);
        sizeLabel = text("", 16, INK); sizeLabel.setPadding(0, dp(20), 0, dp(8)); body.addView(sizeLabel);
        SeekBar slider = new SeekBar(this); slider.setMax(75); slider.setProgress(scale - 25); slider.setContentDescription("Tamaño del icono, de 25 a 100 por ciento"); body.addView(slider);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int n, boolean user) { scale = n + 25; refreshPreview(); }
            public void onStartTrackingTouch(SeekBar s) { }
            public void onStopTrackingTouch(SeekBar s) { saveDraft(); }
        });
        LinearLayout tile = new LinearLayout(this); tile.setOrientation(LinearLayout.VERTICAL); tile.setGravity(Gravity.CENTER); tile.setPadding(dp(16), dp(20), dp(16), dp(16)); tile.setBackground(round(PANEL));
        LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(-1, -2); tileParams.topMargin = dp(12); body.addView(tile, tileParams);
        preview = new ImageView(this); preview.setContentDescription("Vista previa del icono"); preview.setScaleType(ImageView.ScaleType.FIT_CENTER); tile.addView(preview, new LinearLayout.LayoutParams(dp(96), dp(96)));
        TextView previewText = text("Vista previa", 12, MUTED); previewText.setPadding(0, dp(10), 0, 0); tile.addView(previewText);
        TextView limits = text("PNG transparente, sin marco ni insignia. Se agrega como un widget de 1×1 que abre la app.", 13, MUTED); limits.setPadding(0, dp(12), 0, dp(16)); body.addView(limits);
        createButton = button("Agregar al inicio"); createButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFB6A2FF)); createButton.setTextColor(BG); body.addView(createButton); createButton.setOnClickListener(v -> pin());
        status = text("Seleccioná una app y un PNG para empezar.", 13, MUTED); status.setPadding(0, dp(12), 0, 0); status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); body.addView(status);
        Button clock = button("Widget de hora, fecha y clima"); body.addView(clock);
        clock.setOnClickListener(v -> startActivity(new Intent(this, WeatherConfigActivity.class)));
        label.addTextChangedListener(watcher(() -> saveDraft()));
    }

    private void chooseApp() {
        status.setText("Buscando aplicaciones…"); appButton.setEnabled(false);
        worker.execute(() -> {
            List<AppEntry> apps = new ArrayList<>();
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            for (ResolveInfo info : getPackageManager().queryIntentActivities(query, 0)) {
                if (info.activityInfo == null || !info.activityInfo.exported || !info.activityInfo.enabled || info.activityInfo.packageName.equals(getPackageName())) continue;
                apps.add(new AppEntry(info.loadLabel(getPackageManager()).toString(), new ComponentName(info.activityInfo.packageName, info.activityInfo.name)));
            }
            apps.sort((a,b) -> a.name.compareToIgnoreCase(b.name));
            runOnUiThread(() -> { if (isDestroyed()) return; appButton.setEnabled(true); status.setText("Elegí la app que querés abrir."); showApps(apps); });
        });
    }

    private void showApps(List<AppEntry> apps) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(16), 0, dp(16), 0);
        EditText search = new EditText(this); search.setSingleLine(true); search.setHint("Buscar aplicación"); box.addView(search);
        ListView list = new ListView(this); box.addView(list, new LinearLayout.LayoutParams(-1, dp(330)));
        TextView empty = text("No se encontraron apps", 14, MUTED); box.addView(empty); list.setEmptyView(empty);
        List<AppEntry> visible = new ArrayList<>(apps);
        ArrayAdapter<AppEntry> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, visible); list.setAdapter(adapter);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Seleccionar app").setView(box).setNegativeButton("Cancelar", null).create();
        search.addTextChangedListener(watcher(() -> {
            String term = search.getText().toString().toLowerCase(Locale.ROOT);
            adapter.clear(); for (AppEntry app : apps) if (app.name.toLowerCase(Locale.ROOT).contains(term) || app.component.getPackageName().toLowerCase(Locale.ROOT).contains(term)) adapter.add(app);
            adapter.notifyDataSetChanged();
        }));
        list.setOnItemClickListener((p,v,pos,id) -> {
            AppEntry app = adapter.getItem(pos); if (app == null) return;
            String oldName = appLabel; selected = app.component; appLabel = app.name;
            if (label.getText().toString().trim().isEmpty() || label.getText().toString().equals(oldName)) label.setText(appLabel);
            saveDraft(); refresh(); dialog.dismiss();
        });
        dialog.show();
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_PNG || result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData(); busy = true; status.setText("Leyendo PNG…"); refresh();
        worker.execute(() -> {
            File temp = new File(getFilesDir(), "incoming.png");
            try {
                try (InputStream input = getContentResolver().openInputStream(uri); OutputStream output = new FileOutputStream(temp)) {
                    if (input == null) throw new IOException("No se pudo abrir el archivo.");
                    byte[] buf = new byte[8192]; int n; long total = 0;
                    while ((n = input.read(buf)) != -1) { total += n; if (total > 24L * 1024 * 1024) throw new IOException("El PNG debe pesar menos de 24 MB."); output.write(buf, 0, n); }
                }
                byte[] magic = new byte[8];
                try (DataInputStream input = new DataInputStream(new FileInputStream(temp))) { input.readFully(magic); }
                if (!Arrays.equals(magic, new byte[]{(byte)137,80,78,71,13,10,26,10})) throw new IOException("Elegí un archivo PNG válido.");
                Bitmap imported = decode(temp);
                File cached = new File(getFilesDir(), "icon.png");
                java.nio.file.Files.move(temp.toPath(), cached.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                runOnUiThread(() -> { if (isDestroyed()) return; png = imported; busy = false; status.setText("PNG listo. Ajustá el tamaño y agregalo al inicio."); refresh(); });
            } catch (IOException | RuntimeException e) {
                temp.delete();
                runOnUiThread(() -> { if (isDestroyed()) return; busy = false; refresh(); message(e.getMessage() == null ? "No se pudo leer el PNG." : e.getMessage()); });
            }
        });
    }

    private Bitmap decode(File file) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options(); options.inJustDecodeBounds = true; BitmapFactory.decodeFile(file.getPath(), options);
        if (options.outWidth <= 0 || options.outHeight <= 0 || options.outWidth > 32768 || options.outHeight > 32768) throw new IOException("Dimensiones de PNG no admitidas.");
        options.inSampleSize = 1;
        while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 1024) options.inSampleSize *= 2;
        options.inJustDecodeBounds = false; options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap result = BitmapFactory.decodeFile(file.getPath(), options);
        if (result == null) throw new IOException("El PNG está dañado o no se pudo decodificar.");
        return result;
    }

    private Bitmap scaledIcon(int edge) { return scaledIcon(edge, 1f); }
    private Bitmap scaledIcon(int edge, float canvasFraction) {
        Bitmap out = Bitmap.createBitmap(edge, edge, Bitmap.Config.ARGB_8888);
        float ratio = (edge * canvasFraction * scale / 100f) / Math.max(png.getWidth(), png.getHeight());
        float w = png.getWidth() * ratio, h = png.getHeight() * ratio;
        new Canvas(out).drawBitmap(png, null, new RectF((edge-w)/2, (edge-h)/2, (edge+w)/2, (edge+h)/2), new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        return out;
    }

    private void pin() {
        if (busy || selected == null || png == null) return;
        android.appwidget.AppWidgetManager manager = android.appwidget.AppWidgetManager.getInstance(this);
        if (!manager.isRequestPinAppWidgetSupported()) { message("Tu launcher no permite agregar estos accesos. Probá con One UI Home."); return; }
        String name = label.getText().toString().trim(); if (name.isEmpty()) name = appLabel;
        try {
            android.content.pm.ActivityInfo info = getPackageManager().getActivityInfo(selected, 0);
            if (!info.enabled || !info.exported || !info.applicationInfo.enabled) { message("La app ya no está disponible. Seleccioná otra."); return; }
            String id = UUID.randomUUID().toString();
            Bitmap bitmap = scaledIcon(384);
            try (OutputStream output = new FileOutputStream(new File(getFilesDir(), "widget-" + id + ".png"))) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, output); }
            PngWidgetProvider.store(this).edit().putString(id + ".component", selected.flattenToString()).putString(id + ".name", name).commit();
            Bundle extras = new Bundle();
            extras.putParcelable(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, PngWidgetProvider.views(this, bitmap, selected, name, id.hashCode()));
            Intent callback = new Intent(this, PinReceiver.class).setAction("ar.accesospng.WIDGET_PINNED").setData(Uri.parse("accesospng://widget/" + id)).putExtra("token", id);
            int mutability = android.os.Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0;
            PendingIntent pending = PendingIntent.getBroadcast(this, 0, callback, PendingIntent.FLAG_UPDATE_CURRENT | mutability);
            boolean accepted = manager.requestPinAppWidget(new ComponentName(this, PngWidgetProvider.class), extras, pending);
            status.setText(accepted ? "Confirmá “Agregar” en el diálogo del launcher." : "El launcher rechazó la solicitud. Intentá de nuevo.");
            saveDraft();
        } catch (Exception e) { message("No se pudo crear el acceso. Revisá que la app siga instalada e intentá de nuevo."); }
    }

    private void refresh() {
        appButton.setText(selected == null ? "Seleccionar app" : appLabel + "  ·  Cambiar");
        pngButton.setText(busy ? "Leyendo PNG…" : png == null ? "Elegir archivo PNG" : "PNG cargado  ·  Cambiar");
        pngButton.setEnabled(!busy); createButton.setEnabled(!busy && selected != null && png != null); createButton.setAlpha(createButton.isEnabled() ? 1f : .45f); refreshPreview();
    }
    private void refreshPreview() {
        if (sizeLabel == null) return;
        sizeLabel.setText("3  ·  Tamaño del icono: " + scale + "%");
        if (png != null) preview.setImageBitmap(scaledIcon(192)); else { preview.setImageResource(ar.accesospng.R.drawable.app_icon); preview.setAlpha(.5f); }
        if (png != null) preview.setAlpha(1f);
    }
    private void saveDraft() {
        if (prefs == null || label == null) return;
        prefs.edit().putString("component", selected == null ? "" : selected.flattenToString()).putString("appLabel", appLabel).putString("label", label.getText().toString()).putInt("scale", scale).apply();
    }
    @Override protected void onPause() { saveDraft(); super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("component", selected == null ? "" : selected.flattenToString()); state.putString("appLabel", appLabel); state.putString("label", label.getText().toString()); state.putInt("scale", scale); super.onSaveInstanceState(state);
    }
    private void message(String s) { status.setText(s); Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private TextView text(String s, int size, int color) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); return t; }
    private Button button(String s) { Button b = new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(16); b.setMinHeight(dp(52)); b.setTextColor(INK); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL)); return b; }
    private GradientDrawable round(int color) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(20)); return d; }
    private TextWatcher watcher(Runnable callback) { return new TextWatcher() { public void beforeTextChanged(CharSequence s,int start,int count,int after) { } public void onTextChanged(CharSequence s,int start,int before,int count) { } public void afterTextChanged(Editable e) { callback.run(); } }; }
    private static class AppEntry { final String name; final ComponentName component; AppEntry(String n,ComponentName c) { name=n;component=c; } public String toString() { return name; } }
}
