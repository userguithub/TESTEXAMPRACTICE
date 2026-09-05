package com.menteestoica.enquiridion;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity implements View.OnClickListener {
    private static final int ID_ROTATION = 100;
    private static final int ID_CURRENT = 101;
    private static final int ID_ALL = 102;
    private static final int ID_APPLY = 103;
    private static final int ID_FIXED = 104;
    private static final int ID_UNLOCK_NEXT = 105;
    private static final int ID_UNLOCK_RANDOM = 106;
    private static final int ID_LOCK_NEXT = 107;
    private static final int ID_LOCK_RANDOM = 108;
    private static final int ID_BOTH_NEXT = 109;
    private static final int ID_BOTH_RANDOM = 110;
    private static final int ID_15 = 111;
    private static final int ID_1H = 112;
    private static final int ID_6H = 113;
    private static final int ID_24H = 114;
    private static final int ID_TARGET = 115;
    private static final int ID_STOP = 116;

    private TextView status;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
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
        title.setText("ENQUIRIDIÓN ESTOICO\nFONDO DINÁMICO · v1.1");
        title.setTextColor(Color.WHITE);
        title.setTextSize(25f);
        title.setTypeface(Typeface.create(mono, Typeface.BOLD));
        title.setPadding(0, 0, 0, 32);
        root.addView(title);

        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(16f);
        status.setTypeface(mono);
        status.setPadding(0, 8, 0, 32);
        root.addView(status);

        addButton(root, "Frases incluidas en la rotación", ID_ROTATION);
        addButton(root, "Usar TODAS las frases", ID_ALL);
        addButton(root, "Elegir frase actual", ID_CURRENT);
        addButton(root, "Aplicar frase actual ahora", ID_APPLY);

        addSection(root, "CAMBIO AUTOMÁTICO", mono);
        addButton(root, "Frase fija (sin rotación)", ID_FIXED);
        addButton(root, "Al DESBLOQUEAR · siguiente", ID_UNLOCK_NEXT);
        addButton(root, "Al DESBLOQUEAR · aleatoria", ID_UNLOCK_RANDOM);
        addButton(root, "Al BLOQUEAR · siguiente", ID_LOCK_NEXT);
        addButton(root, "Al BLOQUEAR · aleatoria", ID_LOCK_RANDOM);
        addButton(root, "Al BLOQUEAR + DESBLOQUEAR · siguiente", ID_BOTH_NEXT);
        addButton(root, "Al BLOQUEAR + DESBLOQUEAR · aleatoria", ID_BOTH_RANDOM);
        addButton(root, "Cada 15 minutos", ID_15);
        addButton(root, "Cada hora", ID_1H);
        addButton(root, "Cada 6 horas", ID_6H);
        addButton(root, "Cada 24 horas", ID_24H);

        addSection(root, "DESTINO", mono);
        addButton(root, "Inicio / bloqueo / ambos", ID_TARGET);
        addButton(root, "Detener automatización", ID_STOP);

        TextView note = new TextView(this);
        note.setText("Todas las máximas están activadas por defecto.\n\n" +
                "La versión 1.1 corrige dos puntos: aplica inicio y bloqueo por separado, " +
                "y detecta bloqueo/desbloqueo mediante SCREEN_OFF, USER_PRESENT y una comprobación del Keyguard del Pixel.\n\n" +
                "TIPOGRAFÍA: Courier New si Android la ofrece; si no, monoespaciada del sistema.");
        note.setTextColor(Color.LTGRAY);
        note.setTextSize(13f);
        note.setTypeface(mono);
        note.setPadding(0, 32, 0, 24);
        root.addView(note);

        setContentView(scroll);
        refresh();
    }

    private void addSection(LinearLayout root, String label, Typeface mono) {
        TextView t = new TextView(this);
        t.setText("\n" + label);
        t.setTextColor(Color.WHITE);
        t.setTextSize(15f);
        t.setTypeface(Typeface.create(mono, Typeface.BOLD));
        t.setPadding(0, 22, 0, 10);
        root.addView(t);
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

        if (id == ID_ROTATION) {
            showRotationDialog();
            return;
        }
        if (id == ID_CURRENT) {
            showCurrentDialog();
            return;
        }
        if (id == ID_ALL) {
            WallpaperRenderer.enableAll(this);
            toast("Todas las frases están activas");
            refresh();
            return;
        }
        if (id == ID_APPLY) {
            WallpaperRenderer.applyCurrent(this);
            toast("Fondo actualizado");
            return;
        }

        if (id == ID_FIXED) {
            setMode(WallpaperRenderer.MODE_FIXED);
            stopDynamic(false);
            WallpaperRenderer.applyCurrent(this);
        } else if (id == ID_UNLOCK_NEXT) {
            activateMode(WallpaperRenderer.MODE_UNLOCK_NEXT);
        } else if (id == ID_UNLOCK_RANDOM) {
            activateMode(WallpaperRenderer.MODE_UNLOCK_RANDOM);
        } else if (id == ID_LOCK_NEXT) {
            activateMode(WallpaperRenderer.MODE_LOCK_NEXT);
        } else if (id == ID_LOCK_RANDOM) {
            activateMode(WallpaperRenderer.MODE_LOCK_RANDOM);
        } else if (id == ID_BOTH_NEXT) {
            activateMode(WallpaperRenderer.MODE_LOCK_UNLOCK_NEXT);
        } else if (id == ID_BOTH_RANDOM) {
            activateMode(WallpaperRenderer.MODE_LOCK_UNLOCK_RANDOM);
        } else if (id == ID_15) {
            activateMode(WallpaperRenderer.MODE_15_MIN);
        } else if (id == ID_1H) {
            activateMode(WallpaperRenderer.MODE_1_HOUR);
        } else if (id == ID_6H) {
            activateMode(WallpaperRenderer.MODE_6_HOURS);
        } else if (id == ID_24H) {
            activateMode(WallpaperRenderer.MODE_24_HOURS);
        } else if (id == ID_TARGET) {
            int t = WallpaperRenderer.target(this);
            t = t == WallpaperRenderer.TARGET_HOME ? WallpaperRenderer.TARGET_LOCK :
                    (t == WallpaperRenderer.TARGET_LOCK ? WallpaperRenderer.TARGET_BOTH : WallpaperRenderer.TARGET_HOME);
            getSharedPreferences(WallpaperRenderer.PREFS, Context.MODE_PRIVATE)
                    .edit().putInt(WallpaperRenderer.KEY_TARGET, t).apply();
            WallpaperRenderer.applyCurrent(this);
            toast("Destino: " + targetName(t));
        } else if (id == ID_STOP) {
            stopDynamic(true);
            setMode(WallpaperRenderer.MODE_FIXED);
        }

        refresh();
    }

    private void showRotationDialog() {
        final boolean[] checked = new boolean[StoicQuotes.QUOTES.length];
        Set<Integer> enabled = WallpaperRenderer.enabledIndices(this);
        for (int i = 0; i < checked.length; i++) checked[i] = enabled.contains(i);

        new AlertDialog.Builder(this)
                .setTitle("Frases de la rotación")
                .setMultiChoiceItems(StoicQuotes.QUOTES, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    Set<Integer> selected = new HashSet<>();
                    for (int i = 0; i < checked.length; i++) if (checked[i]) selected.add(i);
                    if (selected.isEmpty()) {
                        WallpaperRenderer.enableAll(this);
                        toast("No puede quedar vacía: se han activado todas");
                    } else {
                        WallpaperRenderer.setEnabledIndices(this, selected);
                        toast(selected.size() + " frases activas");
                    }
                    refresh();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showCurrentDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Frase actual")
                .setSingleChoiceItems(StoicQuotes.QUOTES, WallpaperRenderer.currentIndex(this), (dialog, which) -> {
                    WallpaperRenderer.setIndex(this, which);
                    WallpaperRenderer.applyCurrent(this);
                    dialog.dismiss();
                    refresh();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void activateMode(int mode) {
        setMode(mode);
        Intent i = new Intent(this, StoicWallpaperService.class);
        startForegroundService(i);
        WallpaperRenderer.applyCurrent(this);
        toast("Automatización activada");
    }

    private void setMode(int mode) {
        getSharedPreferences(WallpaperRenderer.PREFS, Context.MODE_PRIVATE)
                .edit().putInt(WallpaperRenderer.KEY_MODE, mode).apply();
    }

    private void stopDynamic(boolean notify) {
        stopService(new Intent(this, StoicWallpaperService.class));
        if (notify) toast("Automatización detenida");
    }

    private String modeName(int mode) {
        switch (mode) {
            case WallpaperRenderer.MODE_UNLOCK_NEXT: return "desbloqueo · siguiente";
            case WallpaperRenderer.MODE_UNLOCK_RANDOM: return "desbloqueo · aleatoria";
            case WallpaperRenderer.MODE_LOCK_NEXT: return "bloqueo · siguiente";
            case WallpaperRenderer.MODE_LOCK_RANDOM: return "bloqueo · aleatoria";
            case WallpaperRenderer.MODE_LOCK_UNLOCK_NEXT: return "bloqueo + desbloqueo · siguiente";
            case WallpaperRenderer.MODE_LOCK_UNLOCK_RANDOM: return "bloqueo + desbloqueo · aleatoria";
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
                "\n\nFRASES ACTIVAS: " + WallpaperRenderer.enabledCount(this) + "/" + StoicQuotes.QUOTES.length +
                "\nMODO: " + modeName(WallpaperRenderer.mode(this)).toUpperCase() +
                "\nDESTINO: " + targetName(WallpaperRenderer.target(this)).toUpperCase());
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
