package io.github.shumtugle.hazna;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Progress and settings, and the language modules on the device. Only the
 * score is kept, never the board: an audit always starts from a fresh deal.
 */
final class Store {

    private static final String PREFS = "hazna";
    private final SharedPreferences sp;
    private final File langDir;

    int level = 1, sealed = 0, total = 0, lastArt = 0;
    boolean sound = true, strict = true;
    /** "auto", "en", or the code of a module. */
    String lang = "auto";
    private String deck = "";

    Store(Context c) {
        sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        langDir = new File(c.getFilesDir(), "lang");
        load();
    }

    private void load() {
        level = Math.max(1, sp.getInt("level", 1));
        sealed = sp.getInt("sealed", 0);
        total = sp.getInt("total", 0);
        lastArt = sp.getInt("lastArt", 0);
        sound = sp.getBoolean("sound", true);
        strict = sp.getBoolean("strict", true);
        lang = sp.getString("lang", "auto");
        deck = sp.getString("artDeck", "");
    }

    /** Paintings still to come in the current deck, last one next. */
    int[] artDeck() {
        if (deck.isEmpty()) return new int[0];
        String[] p = deck.split(",");
        int[] d = new int[p.length];
        try {
            for (int i = 0; i < p.length; i++) d[i] = Integer.parseInt(p[i]);
        } catch (NumberFormatException e) {
            return new int[0];
        }
        return d;
    }

    void setArtDeck(int[] d) {
        StringBuilder sb = new StringBuilder();
        for (int x : d) sb.append(sb.length() > 0 ? "," : "").append(x);
        deck = sb.toString();
    }

    void save() {
        sp.edit()
                .putInt("level", level)
                .putInt("sealed", sealed)
                .putInt("total", total)
                .putInt("lastArt", lastArt)
                .putBoolean("sound", sound)
                .putBoolean("strict", strict)
                .putString("lang", lang)
                .putString("artDeck", deck)
                .apply();
    }

    /* ---------- languages ---------- */

    List<Words> modules() {
        ArrayList<Words> out = new ArrayList<>();
        File[] fs = langDir.listFiles();
        if (fs == null) return out;
        java.util.Arrays.sort(fs);
        for (File f : fs) {
            if (!f.getName().endsWith(".txt")) continue;
            try {
                out.add(Words.parse(read(new FileInputStream(f), 2 << 20)));
            } catch (IOException | RuntimeException ignored) {
                // a damaged module is skipped, never fatal
            }
        }
        return out;
    }

    void install(Words w, String text) throws IOException {
        langDir.mkdirs();
        File f = new File(langDir, w.code + ".txt");
        File tmp = new File(langDir, w.code + ".part");
        FileOutputStream o = new FileOutputStream(tmp);
        try {
            o.write(text.getBytes("UTF-8"));
        } finally {
            o.close();
        }
        if (!tmp.renameTo(f)) throw new IOException("rename");
    }

    void remove(String code) {
        new File(langDir, code + ".txt").delete();
    }

    /** The language to speak: the chosen module, or for "auto" the system's when one is installed. */
    Words words(String systemCode) {
        List<Words> ms = modules();
        String want = lang;
        if ("auto".equals(want)) want = systemCode;
        if ("iw".equals(want)) want = "he";
        for (Words w : ms) if (w.code.equals(want)) return w;
        return Words.english();
    }

    static String read(InputStream in, int limit) throws IOException {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                bo.write(buf, 0, n);
                if (bo.size() > limit) throw new IOException("too large");
            }
            return new String(bo.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    /* ---------- progress from the earlier web-based release ---------- */

    /**
     * Releases before 2.0 kept progress in the web view's local storage. On
     * the first start after the update a hidden web view opens a blank page
     * at the same address, reads that one entry and hands it over. It runs
     * once, and only when such storage exists at all.
     */
    @SuppressLint("SetJavaScriptEnabled")
    void migrate(final Activity a, final android.view.ViewGroup parent, final Runnable done) {
        boolean legacy = new File(a.getApplicationInfo().dataDir, "app_webview").exists();
        int tries = sp.getInt("migrateTries", 0);
        if (sp.getBoolean("migrated", false) || !legacy || tries >= 3) {
            sp.edit().putBoolean("migrated", true).apply();
            done.run();
            return;
        }
        final Handler h = new Handler(Looper.getMainLooper());
        final WebView[] holder = new WebView[1];
        final boolean[] finished = {false};
        final boolean[] read = {false};
        sp.edit().putInt("migrateTries", tries + 1).apply();
        final Runnable finish = new Runnable() {
            @Override
            public void run() {
                if (finished[0]) return;
                finished[0] = true;
                // a page that never answered is tried again on a later start, a few times at most
                if (read[0]) sp.edit().putBoolean("migrated", true).apply();
                if (holder[0] != null) {
                    parent.removeView(holder[0]);
                    holder[0].destroy();
                    holder[0] = null;
                }
                done.run();
            }
        };
        try {
            WebView web = new WebView(a);
            holder[0] = web;
            // attached but invisible: some engines will not run a page that is in no window
            web.setAlpha(0f);
            parent.addView(web, new android.view.ViewGroup.LayoutParams(1, 1));
            web.getSettings().setJavaScriptEnabled(true);
            web.getSettings().setDomStorageEnabled(true);
            web.setWebViewClient(new WebViewClient() {
                @Override
                public boolean onRenderProcessGone(WebView view, android.webkit.RenderProcessGoneDetail d) {
                    // the hidden page died; the game must not die with it
                    if (holder[0] == view) {
                        parent.removeView(view);
                        holder[0] = null;
                    }
                    view.destroy();
                    h.post(finish);
                    return true;
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    view.evaluateJavascript("localStorage.getItem('purrsia')", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String v) {
                            adopt(v);
                            read[0] = true;
                            h.post(finish);
                        }
                    });
                }
            });
            web.loadUrl("file:///android_asset/index.html");
        } catch (RuntimeException e) {
            finish.run();
            return;
        }
        h.postDelayed(finish, 4000);
    }

    /** The value arrives as a JSON string holding JSON; null when there was nothing. */
    private void adopt(String v) {
        if (v == null || v.equals("null")) return;
        try {
            String raw = new org.json.JSONArray("[" + v + "]").getString(0);
            JSONObject o = new JSONObject(raw);
            level = Math.max(1, o.optInt("level", 1));
            sealed = o.optInt("sealed", 0);
            total = o.optInt("total", 0);
            lastArt = o.optInt("lastArt", 0);
            sound = o.optBoolean("sound", true);
            strict = o.optBoolean("hard", true);
            String l = o.optString("lang", "auto");
            lang = "iw".equals(l) ? "he" : l;
            save();
        } catch (Exception ignored) {
            // unreadable old progress is left behind; the game starts fresh
        }
    }
}
