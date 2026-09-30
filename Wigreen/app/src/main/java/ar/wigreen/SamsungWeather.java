package ar.wigreen;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

final class SamsungWeather {
    static final String PERMISSION = "com.samsung.android.weather.permission.READ_DANGEROUS_PROVIDER";
    static final String AUTHORITY = "com.samsung.android.weather.content.provider.level.dangerous";
    static final Uri URI = Uri.parse("content://" + AUTHORITY + "/weatherinfo");
    static final class City {
        final String key, name, phrase, source;
        final double temperature;
        final long updated;
        City(Cursor c) {
            key = text(c, "COL_WEATHER_KEY"); name = text(c, "COL_WEATHER_NAME");
            phrase = text(c, "COL_WEATHER_WEATHER_TEXT"); source = text(c, "COL_WEATHER_PROVIDER_NAME");
            temperature = c.getDouble(c.getColumnIndexOrThrow("COL_WEATHER_CURRENT_TEMP"));
            updated = c.getLong(c.getColumnIndexOrThrow("COL_WEATHER_UPDATE_TIME"));
        }
        String line() { return Math.round(temperature) + "°C · " + phrase; }
    }
    private static String text(Cursor c, String col) {
        int i = c.getColumnIndex(col); return i < 0 || c.isNull(i) ? "" : c.getString(i);
    }
    static List<City> cities(Context c) { return cities(c, null); }
    static List<City> cities(Context c, android.os.CancellationSignal cancel) {
        List<City> cities = new ArrayList<>();
        try (Cursor rows = c.getContentResolver().query(URI, null, null, null, null, cancel)) {
            if (rows != null) while (rows.moveToNext()) cities.add(new City(rows));
        }
        return cities;
    }
}



