package com.menteestoica.enquiridion;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

final class WallpaperRenderer {
    static final String PREFS = "stoic_prefs";
    static final String KEY_INDEX = "quote_index";
    static final String KEY_MODE = "mode";
    static final String KEY_TARGET = "target";
    static final String KEY_ENABLED = "enabled_quotes_v2";

    // Valores 0-6 conservados para que una actualización mantenga la configuración anterior.
    static final int MODE_FIXED = 0;
    static final int MODE_UNLOCK_NEXT = 1;
    static final int MODE_UNLOCK_RANDOM = 2;
    static final int MODE_15_MIN = 3;
    static final int MODE_1_HOUR = 4;
    static final int MODE_6_HOURS = 5;
    static final int MODE_24_HOURS = 6;

    static final int MODE_LOCK_NEXT = 7;
    static final int MODE_LOCK_RANDOM = 8;
    static final int MODE_LOCK_UNLOCK_NEXT = 9;
    static final int MODE_LOCK_UNLOCK_RANDOM = 10;

    static final int TARGET_HOME = WallpaperManager.FLAG_SYSTEM;
    static final int TARGET_LOCK = WallpaperManager.FLAG_LOCK;
    static final int TARGET_BOTH = WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK;

    private WallpaperRenderer() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static int currentIndex(Context c) {
        int i = prefs(c).getInt(KEY_INDEX, 0);
        if (i < 0 || i >= StoicQuotes.QUOTES.length) i = 0;
        return i;
    }

    static int target(Context c) {
        return prefs(c).getInt(KEY_TARGET, TARGET_BOTH);
    }

    static int mode(Context c) {
        return prefs(c).getInt(KEY_MODE, MODE_FIXED);
    }

    static void setIndex(Context c, int index) {
        int n = StoicQuotes.QUOTES.length;
        index = ((index % n) + n) % n;
        prefs(c).edit().putInt(KEY_INDEX, index).apply();
    }

    static Set<Integer> enabledIndices(Context c) {
        SharedPreferences p = prefs(c);
        Set<Integer> result = new HashSet<>();

        if (!p.contains(KEY_ENABLED)) {
            for (int i = 0; i < StoicQuotes.QUOTES.length; i++) result.add(i);
            return result;
        }

        Set<String> stored = p.getStringSet(KEY_ENABLED, null);
        if (stored != null) {
            for (String s : stored) {
                try {
                    int i = Integer.parseInt(s);
                    if (i >= 0 && i < StoicQuotes.QUOTES.length) result.add(i);
                } catch (NumberFormatException ignored) {}
            }
        }

        // Nunca permitir una rotación vacía.
        if (result.isEmpty()) {
            for (int i = 0; i < StoicQuotes.QUOTES.length; i++) result.add(i);
        }
        return result;
    }

    static void setEnabledIndices(Context c, Set<Integer> enabled) {
        Set<String> stored = new HashSet<>();
        if (enabled != null) {
            for (Integer i : enabled) {
                if (i != null && i >= 0 && i < StoicQuotes.QUOTES.length) {
                    stored.add(String.valueOf(i));
                }
            }
        }
        if (stored.isEmpty()) {
            for (int i = 0; i < StoicQuotes.QUOTES.length; i++) stored.add(String.valueOf(i));
        }
        prefs(c).edit().putStringSet(KEY_ENABLED, stored).apply();
    }

    static void enableAll(Context c) {
        prefs(c).edit().remove(KEY_ENABLED).apply();
    }

    static int enabledCount(Context c) {
        return enabledIndices(c).size();
    }

    private static List<Integer> enabledSorted(Context c) {
        Set<Integer> set = enabledIndices(c);
        List<Integer> list = new ArrayList<>(set);
        java.util.Collections.sort(list);
        return list;
    }

    static int nextIndex(Context c) {
        List<Integer> enabled = enabledSorted(c);
        int current = currentIndex(c);
        int pos = enabled.indexOf(current);
        int next;
        if (pos < 0 || pos == enabled.size() - 1) next = enabled.get(0);
        else next = enabled.get(pos + 1);
        setIndex(c, next);
        return next;
    }

    static int randomDifferentIndex(Context c) {
        List<Integer> enabled = enabledSorted(c);
        if (enabled.size() == 1) {
            setIndex(c, enabled.get(0));
            return enabled.get(0);
        }
        int current = currentIndex(c);
        Random random = new Random();
        int next = current;
        for (int tries = 0; tries < 20 && next == current; tries++) {
            next = enabled.get(random.nextInt(enabled.size()));
        }
        if (next == current) next = nextIndex(c);
        else setIndex(c, next);
        return next;
    }

    static void applyCurrent(Context c) { apply(c, currentIndex(c)); }
    static void applyNext(Context c) { apply(c, nextIndex(c)); }
    static void applyRandomDifferent(Context c) { apply(c, randomDifferentIndex(c)); }

    static void apply(Context c, int index) {
        Bitmap bitmap = render(index);
        WallpaperManager wm = WallpaperManager.getInstance(c);
        int which = target(c);

        try {
            // WallpaperManager no debe recibir FLAG_SYSTEM|FLAG_LOCK en una única llamada.
            // Para "ambos" se aplican por separado. Esto evita el fallo de la versión 1.
            if (which == TARGET_BOTH) {
                applyOne(wm, bitmap, WallpaperManager.FLAG_SYSTEM);
                applyOne(wm, bitmap, WallpaperManager.FLAG_LOCK);
            } else {
                applyOne(wm, bitmap, which);
            }
        } finally {
            bitmap.recycle();
        }
    }

    private static void applyOne(WallpaperManager wm, Bitmap bitmap, int which) {
        try {
            wm.setBitmap(bitmap, null, true, which);
        } catch (Exception ignored) {
            // Un destino puede no estar disponible temporalmente; el otro no debe bloquearse.
        }
    }

    static Bitmap render(int index) {
        final int width = 1080;
        final int height = 2424;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLACK);

        Typeface mono = Typeface.create("Courier New", Typeface.NORMAL);
        if (mono == null) mono = Typeface.MONOSPACE;

        TextPaint quotePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        quotePaint.setColor(Color.WHITE);
        quotePaint.setTextSize(58f);
        quotePaint.setTypeface(mono);

        int textWidth = 900;
        StaticLayout layout = StaticLayout.Builder
                .obtain(StoicQuotes.QUOTES[index], 0, StoicQuotes.QUOTES[index].length(), quotePaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setIncludePad(false)
                .setLineSpacing(10f, 1.12f)
                .build();

        float left = (width - textWidth) / 2f;
        float top = Math.max(430f, (height - layout.getHeight()) / 2f - 120f);
        canvas.save();
        canvas.translate(left, top);
        layout.draw(canvas);
        canvas.restore();

        TextPaint authorPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        authorPaint.setColor(Color.WHITE);
        authorPaint.setTextSize(31f);
        authorPaint.setTypeface(Typeface.create(mono, Typeface.BOLD));
        authorPaint.setTextAlign(android.graphics.Paint.Align.CENTER);
        canvas.drawText("— " + StoicQuotes.AUTHORS[index], width / 2f,
                Math.min(height - 260f, top + layout.getHeight() + 145f), authorPaint);

        return bitmap;
    }
}
