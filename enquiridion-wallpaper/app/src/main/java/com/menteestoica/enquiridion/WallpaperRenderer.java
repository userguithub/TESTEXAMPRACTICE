package com.menteestoica.enquiridion;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.IOException;
import java.util.Random;

final class WallpaperRenderer {
    static final String PREFS = "stoic_prefs";
    static final String KEY_INDEX = "quote_index";
    static final String KEY_MODE = "mode";
    static final String KEY_TARGET = "target";

    static final int MODE_FIXED = 0;
    static final int MODE_UNLOCK_NEXT = 1;
    static final int MODE_UNLOCK_RANDOM = 2;
    static final int MODE_15_MIN = 3;
    static final int MODE_1_HOUR = 4;
    static final int MODE_6_HOURS = 5;
    static final int MODE_24_HOURS = 6;

    static final int TARGET_HOME = WallpaperManager.FLAG_SYSTEM;
    static final int TARGET_LOCK = WallpaperManager.FLAG_LOCK;
    static final int TARGET_BOTH = WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK;

    private WallpaperRenderer() {}

    static int currentIndex(Context c) {
        int i = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_INDEX, 0);
        if (i < 0 || i >= StoicQuotes.QUOTES.length) i = 0;
        return i;
    }

    static int target(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_TARGET, TARGET_BOTH);
    }

    static int mode(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_MODE, MODE_FIXED);
    }

    static void setIndex(Context c, int index) {
        int n = StoicQuotes.QUOTES.length;
        index = ((index % n) + n) % n;
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_INDEX, index).apply();
    }

    static int nextIndex(Context c) {
        int next = (currentIndex(c) + 1) % StoicQuotes.QUOTES.length;
        setIndex(c, next);
        return next;
    }

    static int randomDifferentIndex(Context c) {
        int current = currentIndex(c);
        if (StoicQuotes.QUOTES.length <= 1) return current;
        int next = current;
        Random random = new Random();
        while (next == current) next = random.nextInt(StoicQuotes.QUOTES.length);
        setIndex(c, next);
        return next;
    }

    static void applyCurrent(Context c) { apply(c, currentIndex(c)); }
    static void applyNext(Context c) { apply(c, nextIndex(c)); }
    static void applyRandomDifferent(Context c) { apply(c, randomDifferentIndex(c)); }

    static void apply(Context c, int index) {
        Bitmap bitmap = render(index);
        WallpaperManager wm = WallpaperManager.getInstance(c);
        try {
            wm.setBitmap(bitmap, null, true, target(c));
        } catch (IOException | SecurityException ignored) {
        } finally {
            bitmap.recycle();
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
