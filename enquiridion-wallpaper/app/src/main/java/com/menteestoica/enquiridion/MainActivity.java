package com.menteestoica.enquiridion;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements View.OnClickListener, DialogInterface.OnClickListener {
    private static final int ID_CHOOSE = 100;
    private static final int ID_APPLY = 101;
    private static final int ID_FIXED = 102;
    private static final int ID_UNLOCK_NEXT = 103;
    private static final int ID_UNLOCK_RANDOM = 104;
    private static final int ID_15 = 105;
    private static final int ID_1H = 106;
    private static final int ID_6H = 107;
    private static final int ID_24H = 108;
    private static final int ID_TARGET = 109;
    private static final int ID_STOP = 110;

    private TextView status;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
        }

        Typeface mono = Typeface.create("Courier New", Typeface.NORMAL);
        if (mono == null) mono = Typeface.MONOSPACE;

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 56, 48, 56);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("ENQUIRIDIÓN ESTOICO\nFONDO DINÁMICO");
        title.setTextColor(Color.WHITE);
        title.setTextSize(25f);
        title.setTypeface(Typeface.create(mono, Typeface.BOLD));
        title.setPadding(0, 0, 0, 32);
        root.addView(title);

        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(17f);
        status.setTypeface(mono);
        status.setPadding(0, 8, 0, 32);
        root.addView(status);

        addButton(root, "Elegir máxima", ID_CHOOSE);
        addButton(root, "Aplicar ahora", ID_APPLY);
        addButton(root, "Modo: frase fija", ID_FIXED);
        addButton(root, "Cambiar en cada desbloqueo", ID_UNLOCK_NEXT);
        addButton(root, "Desbloqueo aleatorio", ID_UNLOCK_RANDOM);
        addButton(root, "Cambiar cada 15 minutos", ID_15);
        addButton(root, "Cambiar cada hora", ID_1H);
        addButton(root, "Cambiar cada 6 horas", ID_6H);
        addButton(root, "Cambiar cada 24 horas", ID_24H);
        addButton(root, "Destino: inicio / bloqueo / ambos", ID_TARGET);
        addButton(root, "Detener cambio automático", ID_STOP);

        TextView note = new TextView(this);
        note.setText("TIPOGRAFÍA: Courier New si Android la ofrece; si no, se usa la fuente monoespaciada del sistema.\n\nSin Internet, anuncios ni analítica.");
        note.setTextColor(Color.LTGRAY);
        note.setTextSize(13f);
        note.setTypeface(mono);
        note.setPadding(0, 32, 0, 24);
        root.addView(note);

        setContentView(scroll);
        refresh();
    }

    private void addButton(LinearLayout root, String label, int id) {
        Button b = new Button(this);
        b.setText(label);
        b.setId(id);
        b.setOnClickListener(this);
        root.addView(b);
    }

    @Override public void onClick(View v) {
        int id = v.getId();
        if (id == ID_CHOOSE) {
            new AlertDialog.Builder(this)
                    .setTitle("Elige una máxima")
                    .setSingleChoiceItems(StoicQuotes.QUOTES, WallpaperRenderer.currentIndex(this), this)
                    .setNegativeButton("Cerrar", null)
                    .show();
            return;
        }
        if (id == ID_APPLY) {
            WallpaperRenderer.applyCurrent(this);
            toast("Fondo actualizado");
        } else if (id == ID_FIXED) {
            setMode(WallpaperRenderer.MODE_FIXED);
            stopDynamic();
            WallpaperRenderer.applyCurrent(this);
        } else if (id == ID_UNLOCK_NEXT) {
            setMode(WallpaperRenderer.MODE_UNLOCK_NEXT);
            startDynamic();
        } else if (id == ID_UNLOCK_RANDOM) {
            setMode(WallpaperRenderer.MODE_UNLOCK_RANDOM);
            startDynamic();
        } else if (id == ID_15) {
            setMode(WallpaperRenderer.MODE_15_MIN);
            startDynamic();
        } else if (id == ID_1H) {
            setMode(WallpaperRenderer.MODE_1_HOUR);
            startDynamic();
        } else if (id == ID_6H) {
            setMode(WallpaperRenderer.MODE_6_HOURS);
            startDynamic();
        } else if (id == ID_24H) {
            setMode(WallpaperRenderer.MODE_24_HOURS);
            startDynamic();
        } else if (id == ID_TARGET) {
            int t = WallpaperRenderer.target(this);
            t = t == WallpaperRenderer.TARGET_HOME ? WallpaperRenderer.TARGET_LOCK :
                    (t == WallpaperRenderer.TARGET_LOCK ? WallpaperRenderer.TARGET_BOTH : WallpaperRenderer.TARGET_HOME);
            getSharedPreferences(WallpaperRenderer.PREFS, Context.MODE_PRIVATE).edit().putInt(WallpaperRenderer.KEY_TARGET, t).apply();
            WallpaperRenderer.applyCurrent(this);
        } else if (id == ID_STOP) {
            stopDynamic();
            setMode(WallpaperRenderer.MODE_FIXED);
        }
        refresh();
    }

    @Override public void onClick(DialogInterface dialog, int which) {
        WallpaperRenderer.setIndex(this, which);
        WallpaperRenderer.applyCurrent(this);
        dialog.dismiss();
        refresh();
    }

    private void setMode(int mode) {
        getSharedPreferences(WallpaperRenderer.PREFS, Context.MODE_PRIVATE).edit().putInt(WallpaperRenderer.KEY_MODE, mode).apply();
    }

    private void startDynamic() {
        Intent i = new Intent(this, StoicWallpaperService.class);
        startForegroundService(i);
        WallpaperRenderer.applyCurrent(this);
        toast("Cambio automático activado");
    }

    private void stopDynamic() {
        stopService(new Intent(this, StoicWallpaperService.class));
        toast("Cambio automático detenido");
    }

    private String modeName(int mode) {
        switch (mode) {
            case WallpaperRenderer.MODE_UNLOCK_NEXT: return "desbloqueo secuencial";
            case WallpaperRenderer.MODE_UNLOCK_RANDOM: return "desbloqueo aleatorio";
            case WallpaperRenderer.MODE_15_MIN: return "cada 15 minutos";
            case WallpaperRenderer.MODE_1_HOUR: return "cada hora";
            case WallpaperRenderer.MODE_6_HOURS: return "cada 6 horas";
            case WallpaperRenderer.MODE_24_HOURS: return "cada 24 horas";
            default: return "frase fija";
        }
    }

    private String targetName(int target) {
        if (target == WallpaperRenderer.TARGET_HOME) return "pantalla de inicio";
        if (target == WallpaperRenderer.TARGET_LOCK) return "pantalla de bloqueo";
        return "inicio + bloqueo";
    }

    private void refresh() {
        int i = WallpaperRenderer.currentIndex(this);
        status.setText("“" + StoicQuotes.QUOTES[i] + "”\n\n— " + StoicQuotes.AUTHORS[i] +
                "\n\nMODO: " + modeName(WallpaperRenderer.mode(this)).toUpperCase() +
                "\nDESTINO: " + targetName(WallpaperRenderer.target(this)).toUpperCase());
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
