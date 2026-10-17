package io.github.shumtugle.hazna;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The one screen of the game. The board sits between a bronze header and a
 * row of four buttons; everything else (title card, end of an audit,
 * settings, about page) is a parchment card over a dimmed board.
 */
public final class MainActivity extends Activity implements BoardView.Listener {

    private static final int PICK_LANGUAGE = 1;
    private static final int ART_COUNT = 10;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Random random = new Random();

    private Ui u;
    private Store store;
    private Sound sound;
    private Words words = Words.english();

    private FrameLayout root;
    private LinearLayout column;
    private TextView stats, undoBtn, resetBtn, hintBtn, autoBtn;
    private BoardView board;
    private ImageView amulet;
    private FrameLayout splash, artSplash;
    private TextView splashTag;
    private FrameLayout menuVeil, winVeil, setupVeil, aboutVeil, askVeil;
    private int insetTop, insetBottom;

    private Game game;
    private boolean won;
    private boolean booted, splashGone;

    /** The auto-solver: idle, thinking, playing a path or winding back. */
    private int auto;
    private static final int IDLE = 0, THINKING = 1, PLAYING = 2, REWINDING = 3, HINTING = 4;
    /** Bumped whenever a background search stops being wanted. */
    private volatile int ticket;
    private Runnable autoStep;

    /** The start-over button waiting for its second tap, or null. */
    private TextView freshArmed;

    /* ---------- start ---------- */

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        u = new Ui(this);
        store = new Store(this);
        sound = new Sound(this);
        sound.setOn(store.sound);
        words = store.words(systemLanguage());
        edgeToEdge();
        build();
        applyWords();
        // recreated by the system: no second splash
        if (saved == null) showSplash(); else { splash.setVisibility(View.GONE); splashGone = true; }
        store.migrate(this, root, new Runnable() {
            @Override
            public void run() {
                booted = true;
                sound.setOn(store.sound);
                words = store.words(systemLanguage());
                applyWords();
                if (splashGone) showMenu();
                takeIntent(getIntent());
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (booted) takeIntent(intent);
    }

    @Override
    protected void onDestroy() {
        ticket++;
        worker.shutdownNow();
        ui.removeCallbacksAndMessages(null);
        board.removeCallbacks(null);
        sound.release();
        super.onDestroy();
    }

    private static String systemLanguage() {
        String l = Locale.getDefault().getLanguage();
        return "iw".equals(l) ? "he" : l;
    }

    @SuppressWarnings("deprecation")
    private void edgeToEdge() {
        Window w = getWindow();
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
        }
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        if (Build.VERSION.SDK_INT >= 23) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        w.getDecorView().setSystemUiVisibility(flags);
    }

    /* ---------- building the screen ---------- */

    private void build() {
        root = new FrameLayout(this);
        root.setBackground(new Ui.Paper(u.density));

        column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        root.addView(column, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable hb = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Painter.BRONZE_LT, Painter.BRONZE, Painter.BRONZE_DK});
        header.setBackground(hb);
        header.setElevation(u.dp(4));
        header.setPaddingRelative(u.dp(14), u.dp(11), u.dp(64), u.dp(11));
        TextView name = u.text(14, Painter.IVORY, Ui.BOLD);
        name.setText("HAZNA");
        name.setLetterSpacing(.24f);
        header.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        stats = u.text(13, Painter.IVORY, Ui.REGULAR);
        stats.setAlpha(.92f);
        stats.setFontFeatureSettings("tnum");
        header.addView(stats, new LinearLayout.LayoutParams(-2, -2));
        column.addView(header, new LinearLayout.LayoutParams(-1, -2));

        board = new BoardView(this);
        board.setListener(this);
        column.addView(board, new LinearLayout.LayoutParams(-1, 0, 1));

        undoBtn = u.button(16);
        resetBtn = u.button(16);
        hintBtn = u.button(16);
        autoBtn = u.button(16);
        undoBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { undo(); }
        });
        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (game != null) { stopAuto(); startLevel(game.level); } }
        });
        hintBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { hint(); }
        });
        autoBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { solve(); }
        });
        LinearLayout foot = u.grid(undoBtn, resetBtn, hintBtn, autoBtn);
        foot.setPadding(u.dp(14), u.dp(10), u.dp(14), u.dp(16));
        FrameLayout footHolder = new FrameLayout(this);
        footHolder.addView(foot, new FrameLayout.LayoutParams(Math.min(u.dp(430),
                getResources().getDisplayMetrics().widthPixels), -2, Gravity.CENTER_HORIZONTAL));
        column.addView(footHolder, new LinearLayout.LayoutParams(-1, -2));

        menuVeil = veil();
        winVeil = veil();
        setupVeil = veil();
        aboutVeil = veil();
        askVeil = veil();

        artSplash = new FrameLayout(this);
        artSplash.setBackgroundColor(Color.BLACK);
        artSplash.setVisibility(View.GONE);
        root.addView(artSplash, new FrameLayout.LayoutParams(-1, -1));

        splash = new FrameLayout(this);
        splash.setBackgroundColor(Color.BLACK);
        ImageView cover = new ImageView(this);
        cover.setScaleType(ImageView.ScaleType.FIT_CENTER);
        cover.setImageBitmap(asset("cover.jpg", 1200));
        splash.addView(cover, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout plate = new LinearLayout(this);
        plate.setOrientation(LinearLayout.VERTICAL);
        plate.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView big = u.text(38, 0xFFE7C88B, Ui.HEAVY);
        big.setText("HAZNA");
        big.setLetterSpacing(.30f);
        big.setShadowLayer(u.dp(10), 0, u.dp(2), 0xE6000000);
        plate.addView(big, new LinearLayout.LayoutParams(-2, -2));
        splashTag = u.text(12.5f, Painter.IVORY, Ui.REGULAR);
        splashTag.setLetterSpacing(.20f);
        splashTag.setAlpha(.72f);
        splashTag.setShadowLayer(u.dp(8), 0, u.dp(2), 0xE6000000);
        plate.addView(splashTag, new LinearLayout.LayoutParams(-2, -2));
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        splash.addView(plate, pp);
        splash.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { leaveSplash(); }
        });
        root.addView(splash, new FrameLayout.LayoutParams(-1, -1));

        amulet = new ImageView(this);
        amulet.setImageBitmap(asset("amulet.png", 240));
        amulet.setAdjustViewBounds(true);
        amulet.setElevation(u.dp(16));
        amulet.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openSetup(); }
        });
        FrameLayout.LayoutParams ap = new FrameLayout.LayoutParams(u.dp(46), -2, Gravity.TOP | Gravity.END);
        ap.setMarginEnd(u.dp(9));
        root.addView(amulet, ap);

        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            @SuppressWarnings("deprecation")
            public WindowInsets onApplyWindowInsets(View v, WindowInsets in) {
                insetTop = in.getSystemWindowInsetTop();
                insetBottom = in.getSystemWindowInsetBottom();
                column.setPadding(in.getSystemWindowInsetLeft(), insetTop, in.getSystemWindowInsetRight(), insetBottom);
                FrameLayout.LayoutParams a = (FrameLayout.LayoutParams) amulet.getLayoutParams();
                a.topMargin = insetTop + u.dp(6);
                amulet.setLayoutParams(a);
                FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) splash.getChildAt(1).getLayoutParams();
                p.bottomMargin = insetBottom + u.dp(26);
                splash.getChildAt(1).setLayoutParams(p);
                for (FrameLayout f : new FrameLayout[]{menuVeil, winVeil, setupVeil, aboutVeil, askVeil}) {
                    f.setPadding(u.dp(18), insetTop + u.dp(18), u.dp(18), insetBottom + u.dp(18));
                }
                return in;
            }
        });
        setContentView(root);
    }

    /** A dimmed layer over the board with a card in the middle; the card scrolls if tall. */
    private FrameLayout veil() {
        FrameLayout v = new FrameLayout(this);
        v.setBackgroundColor(0x8C3C2C16);
        v.setVisibility(View.GONE);
        v.setClickable(true);
        v.setPadding(u.dp(18), u.dp(18), u.dp(18), u.dp(18));
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setVerticalScrollBarEnabled(false);
        FrameLayout center = new FrameLayout(this);
        sv.addView(center, new ViewGroup.LayoutParams(-1, -2));
        v.addView(sv, new FrameLayout.LayoutParams(-1, -1));
        root.addView(v, new FrameLayout.LayoutParams(-1, -1));
        return v;
    }

    private void setCard(FrameLayout veil, View card) {
        FrameLayout center = (FrameLayout) ((ScrollView) veil.getChildAt(0)).getChildAt(0);
        center.removeAllViews();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Math.min(u.dp(380),
                getResources().getDisplayMetrics().widthPixels - u.dp(36)), -2, Gravity.CENTER);
        center.addView(card, lp);
        center.setMinimumHeight(root.getHeight() - insetTop - insetBottom - u.dp(36));
    }

    private void show(final FrameLayout v, boolean on) {
        if (on) {
            v.setAlpha(0f);
            v.setVisibility(View.VISIBLE);
            v.animate().alpha(1f).setDuration(250).start();
        } else if (v.getVisibility() == View.VISIBLE) {
            v.animate().alpha(0f).setDuration(200).withEndAction(new Runnable() {
                @Override public void run() { v.setVisibility(View.GONE); }
            }).start();
        }
    }

    private Bitmap asset(String path, int maxSide) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            InputStream in = getAssets().open(path);
            BitmapFactory.decodeStream(in, null, o);
            in.close();
            int s = 1;
            while (Math.max(o.outWidth, o.outHeight) / (s * 2) >= maxSide) s *= 2;
            BitmapFactory.Options d = new BitmapFactory.Options();
            d.inSampleSize = s;
            in = getAssets().open(path);
            Bitmap b = BitmapFactory.decodeStream(in, null, d);
            in.close();
            return b;
        } catch (IOException e) {
            return null;
        }
    }

    /* ---------- words ---------- */

    private String w(String key) {
        return words.get(key);
    }

    /** Capitals by the rules of the language on screen, not of the phone. */
    private String upper(String s) {
        return s.toUpperCase(new Locale(words.code));
    }

    private void applyWords() {
        int dir = words.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR;
        root.setLayoutDirection(dir);
        amulet.setContentDescription(w("settings"));
        undoBtn.setText(w("undo"));
        resetBtn.setText(w("restart"));
        if (auto == IDLE) {
            hintBtn.setText(w("hint"));
            autoBtn.setText(w("solve"));
        }
        splashTag.setText(upper(w("subtitle")));
        updateStats();
        if (menuVeil.getVisibility() == View.VISIBLE) fillMenu();
        if (setupVeil.getVisibility() == View.VISIBLE) fillSetup();
    }

    private void updateStats() {
        int level = game != null ? game.level : store.level;
        int moves = game != null ? game.moves : 0;
        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append(w("audit")).append(' ');
        int a = sb.length();
        sb.append(String.valueOf(level));
        sb.setSpan(new StyleSpan(Typeface.BOLD), a, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(" · ").append(w("moves")).append(' ');
        a = sb.length();
        sb.append(String.valueOf(moves));
        sb.setSpan(new StyleSpan(Typeface.BOLD), a, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        stats.setText(sb);
        Ui.enable(undoBtn, auto == IDLE && game != null && game.historySize() > 0);
    }

    /* ---------- splash and title card ---------- */

    private void showSplash() {
        splash.setVisibility(View.VISIBLE);
        ui.postDelayed(new Runnable() {
            @Override public void run() { leaveSplash(); }
        }, 5000);
    }

    private void leaveSplash() {
        if (splashGone) return;
        splashGone = true;
        splash.animate().alpha(0f).setDuration(400).withEndAction(new Runnable() {
            @Override public void run() { splash.setVisibility(View.GONE); }
        }).start();
        if (booted) showMenu();
    }

    private void showMenu() {
        if (game != null) return;
        fillMenu();
        show(menuVeil, true);
    }

    private void fillMenu() {
        LinearLayout card = u.card();
        card.addView(u.title("Hazna"));
        TextView sub = u.caps(12, .55f, .2f);
        sub.setTypeface(Ui.REGULAR);
        sub.setGravity(Gravity.CENTER);
        sub.setText(upper(w("subtitle")));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = u.dp(4);
        sp.bottomMargin = u.dp(14);
        card.addView(sub, sp);
        for (int i = 1; i <= 4; i++) {
            TextView p = u.text(14.5f, Painter.INK, Ui.REGULAR);
            p.setLineSpacing(0, 1.3f);
            p.setText(w("tale_" + i));
            p.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = u.dp(9);
            card.addView(p, lp);
        }
        TextView play = u.button(15);
        if (store.level > 1) {
            SpannableStringBuilder sb = new SpannableStringBuilder(w("continue"));
            sb.append('\n');
            int a = sb.length();
            sb.append(w("audit")).append(' ').append(String.valueOf(store.level));
            sb.setSpan(new AbsoluteSizeSpan(12, true), a, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new ForegroundColorSpan(0xBFF4EBD8), a, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            play.setText(sb);
        } else {
            play.setText(w("begin"));
        }
        play.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                show(menuVeil, false);
                startLevel(store.level);
            }
        });
        final TextView fresh = u.button(15);
        fresh.setText(w("start_over"));
        fresh.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { armFresh(fresh); }
        });
        final TextView snd = u.button(15);
        snd.setText(soundLabel());
        snd.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleSound(); snd.setText(soundLabel()); }
        });
        TextView lang = u.button(15);
        lang.setText(w("language"));
        lang.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openSetup(); }
        });
        for (TextView b : new TextView[]{play, fresh, snd, lang}) b.setMinHeight(u.dp(62));
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, -2);
        gp.topMargin = u.dp(6);
        card.addView(u.grid(play, fresh, snd, lang), gp);
        TextView credit = u.text(12.5f, Painter.INK, Ui.REGULAR);
        credit.setAlpha(.55f);
        credit.setText(w("credit"));
        credit.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        credit.setPadding(0, u.dp(12), 0, 0);
        View rule = new View(this);
        rule.setBackgroundColor(0x405C4526);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, u.dp(1));
        rp.topMargin = u.dp(14);
        card.addView(rule, rp);
        card.addView(credit, new LinearLayout.LayoutParams(-1, -2));
        setCard(menuVeil, card);
    }

    private String soundLabel() {
        return w("sound") + ": " + (store.sound ? w("on") : w("off"));
    }

    private void toggleSound() {
        store.sound = !store.sound;
        store.save();
        sound.setOn(store.sound);
        if (store.sound) sound.play(Sound.DROP);
    }

    private void armFresh(final TextView b) {
        if (freshArmed == b) {
            freshArmed = null;
            store.level = 1;
            store.sealed = 0;
            store.total = 0;
            store.save();
            show(setupVeil, false);
            show(menuVeil, false);
            show(winVeil, false);
            stopAuto();
            startLevel(1);
            return;
        }
        if (freshArmed != null) freshArmed.setText(w("start_over"));
        freshArmed = b;
        b.setText(w("erase_ask"));
        ui.postDelayed(new Runnable() {
            @Override public void run() {
                if (freshArmed == b) {
                    freshArmed = null;
                    b.setText(w("start_over"));
                }
            }
        }, 4000);
    }

    /* ---------- settings ---------- */

    private void openSetup() {
        // until old progress is read, settings would be overwritten by it
        if (!booted) return;
        if (!splashGone) leaveSplash();
        fillSetup();
        if (setupVeil.getVisibility() != View.VISIBLE) show(setupVeil, true);
    }

    private void fillSetup() {
        LinearLayout card = u.card();
        card.addView(u.title(w("settings")));
        TextView lbl = u.caps(12, .5f, .16f);
        lbl.setText(upper(w("language")));
        lbl.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = u.dp(14);
        lp.bottomMargin = u.dp(7);
        card.addView(lbl, lp);

        List<Words> mods = store.modules();
        int n = 2 + mods.size() + 1;
        TextView[] cells = new TextView[n];
        cells[0] = langButton(w("lang_auto"), "auto", null);
        cells[1] = langButton("English", Words.ENGLISH, null);
        for (int i = 0; i < mods.size(); i++) cells[2 + i] = langButton(mods.get(i).name, mods.get(i).code, mods.get(i));
        TextView load = langButton("+ " + w("lang_load"), null, null);
        cells[n - 1] = load;
        LinearLayout langs = new LinearLayout(this);
        langs.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < n; i += 3) {
            LinearLayout row = new LinearLayout(this);
            for (int k = i; k < Math.min(n, i + 3); k++) {
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -1, 1);
                if (k > i) cp.setMarginStart(u.dp(8));
                row.addView(cells[k], cp);
            }
            for (int k = n; k < i + 3; k++) row.addView(new View(this), new LinearLayout.LayoutParams(0, -1, 1));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
            if (i > 0) rp.topMargin = u.dp(8);
            langs.addView(row, rp);
        }
        card.addView(langs, new LinearLayout.LayoutParams(-1, -2));

        final TextView snd = u.button(15);
        snd.setText(soundLabel());
        snd.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleSound(); snd.setText(soundLabel()); }
        });
        final TextView fresh = u.button(15);
        fresh.setText(w("start_over"));
        fresh.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { armFresh(fresh); }
        });
        final TextView rules = u.button(15);
        rules.setText(w("rules") + ": " + (store.strict ? w("rules_strict") : w("rules_forgiving")));
        rules.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                store.strict = !store.strict;
                store.save();
                rules.setText(w("rules") + ": " + (store.strict ? w("rules_strict") : w("rules_forgiving")));
                sound.play(Sound.DROP);
                // the rules changed, so the audit starts over
                if (game != null) { stopAuto(); startLevel(game.level); }
            }
        });
        TextView about = u.button(15);
        about.setText(w("about_button"));
        about.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openAbout(); }
        });
        for (TextView b : new TextView[]{snd, fresh, rules, about}) b.setMinHeight(u.dp(62));
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, -2);
        gp.topMargin = u.dp(16);
        card.addView(u.grid(snd, fresh, rules, about), gp);
        TextView close = u.button(15);
        close.setText(w("close"));
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { show(setupVeil, false); }
        });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.topMargin = u.dp(10);
        card.addView(close, cp);
        TextView tally = u.text(13, Painter.INK, Ui.REGULAR);
        tally.setAlpha(.66f);
        tally.setLineSpacing(0, 1.3f);
        tally.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        tally.setText(w("sacks_sealed") + ": " + store.sealed + "\n" + w("moves_in_all") + ": " + store.total);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.topMargin = u.dp(14);
        card.addView(tally, tp);
        setCard(setupVeil, card);
    }

    /** A language choice; code null means the button that loads a module from a file. */
    private TextView langButton(String label, final String code, final Words module) {
        TextView b = u.button(14);
        b.setText(label);
        b.setMinHeight(u.dp(48));
        b.setPadding(u.dp(6), u.dp(8), u.dp(6), u.dp(8));
        b.setMaxLines(2);
        boolean on = code != null && code.equals(store.lang);
        if (!on) {
            b.setBackground(u.pale());
            b.setTextColor(Painter.INK);
            b.setElevation(0);
        }
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (code == null) { pickModule(); return; }
                store.lang = code;
                store.save();
                words = store.words(systemLanguage());
                applyWords();
            }
        });
        if (module != null) {
            b.setOnLongClickListener(new View.OnLongClickListener() {
                @Override public boolean onLongClick(View v) { askRemove(module); return true; }
            });
        }
        return b;
    }

    /* ---------- language modules ---------- */

    private void pickModule() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            startActivityForResult(i, PICK_LANGUAGE);
        } catch (ActivityNotFoundException e) {
            tell(w("lang_not"));
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == PICK_LANGUAGE && res == RESULT_OK && data != null && data.getData() != null) {
            offer(data.getData());
        }
    }

    /** A file opened with the game from elsewhere: if it is a module, offer to take it. */
    private void takeIntent(Intent i) {
        if (i == null || !Intent.ACTION_VIEW.equals(i.getAction()) || i.getData() == null) return;
        Uri uri = i.getData();
        i.setAction(Intent.ACTION_MAIN);
        i.setData(null);
        offer(uri);
    }

    private void offer(Uri uri) {
        final String text;
        final Words m;
        try {
            text = Store.read(getContentResolver().openInputStream(uri), 2 << 20);
            m = Words.parse(text);
        } catch (Exception e) {
            tell(w("lang_not"));
            return;
        }
        LinearLayout card = u.card();
        card.addView(u.title(m.name));
        TextView q = u.text(16.5f, Painter.INK, Ui.REGULAR);
        q.setGravity(Gravity.CENTER);
        q.setText(w("lang_add_ask") + "\n" + m.filled() + " / " + Words.total());
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(-1, -2);
        qp.topMargin = u.dp(8);
        qp.bottomMargin = u.dp(18);
        card.addView(q, qp);
        TextView add = u.button(15);
        add.setText(w("lang_add"));
        add.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    store.install(m, text);
                    store.lang = m.code;
                    store.save();
                    words = store.words(systemLanguage());
                    applyWords();
                    show(askVeil, false);
                    if (setupVeil.getVisibility() == View.VISIBLE) fillSetup();
                } catch (IOException e) {
                    tell(w("lang_not"));
                }
            }
        });
        TextView cancel = u.button(15);
        cancel.setText(w("cancel"));
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { show(askVeil, false); }
        });
        card.addView(u.grid(add, cancel), new LinearLayout.LayoutParams(-1, -2));
        setCard(askVeil, card);
        show(askVeil, true);
    }

    private void askRemove(final Words m) {
        LinearLayout card = u.card();
        card.addView(u.title(m.name));
        TextView q = u.text(16.5f, Painter.INK, Ui.REGULAR);
        q.setGravity(Gravity.CENTER);
        q.setText(w("lang_remove_ask"));
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(-1, -2);
        qp.topMargin = u.dp(8);
        qp.bottomMargin = u.dp(18);
        card.addView(q, qp);
        TextView rm = u.button(15);
        rm.setText(w("lang_remove"));
        rm.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                store.remove(m.code);
                if (m.code.equals(store.lang)) {
                    store.lang = "auto";
                    store.save();
                }
                words = store.words(systemLanguage());
                applyWords();
                show(askVeil, false);
                fillSetup();
            }
        });
        TextView cancel = u.button(15);
        cancel.setText(w("cancel"));
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { show(askVeil, false); }
        });
        card.addView(u.grid(rm, cancel), new LinearLayout.LayoutParams(-1, -2));
        setCard(askVeil, card);
        show(askVeil, true);
    }

    /** A refusal the player would not otherwise see. */
    private void tell(String s) {
        LinearLayout card = u.card();
        TextView q = u.text(16.5f, Painter.INK, Ui.REGULAR);
        q.setGravity(Gravity.CENTER);
        q.setText(s);
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(-1, -2);
        qp.bottomMargin = u.dp(18);
        card.addView(q, qp);
        TextView ok = u.button(15);
        ok.setText(w("close"));
        ok.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { show(askVeil, false); }
        });
        card.addView(ok, new LinearLayout.LayoutParams(-1, -2));
        setCard(askVeil, card);
        show(askVeil, true);
    }

    /* ---------- about ---------- */

    private void openAbout() {
        LinearLayout card = u.card();
        card.addView(u.title(w("doc_title")));
        TextView doc = u.text(13.5f, Painter.INK, Ui.REGULAR);
        doc.setLineSpacing(0, 1.3f);
        doc.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        doc.setText(aboutText());
        doc.setMovementMethod(LinkMovementMethod.getInstance());
        doc.setLinkTextColor(Painter.BRONZE_DK);
        ScrollView sv = new ScrollView(this);
        sv.addView(doc);
        int max = (int) (getResources().getDisplayMetrics().heightPixels * 0.56f);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2);
        dp.topMargin = u.dp(10);
        dp.bottomMargin = u.dp(16);
        doc.measure(View.MeasureSpec.makeMeasureSpec(Math.min(u.dp(340),
                getResources().getDisplayMetrics().widthPixels - u.dp(76)), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        if (doc.getMeasuredHeight() > max) dp.height = max;
        card.addView(sv, dp);
        TextView close = u.button(15);
        close.setText(w("close"));
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { show(aboutVeil, false); }
        });
        card.addView(close, new LinearLayout.LayoutParams(-1, -2));
        setCard(aboutVeil, card);
        show(aboutVeil, true);
    }

    private static final String[] DOC = {
        "doc_intro", "#head_hood", "doc_levels", "doc_hints", "doc_sound", "doc_progress",
        "#head_screens", "doc_screens", "#head_climb", "doc_climb", "#head_rules", "doc_strict",
        "doc_forgiving", "doc_switch", "doc_amulet", "doc_rhythm", "doc_languages", "#head_access", "doc_access",
        "#head_sources"
    };

    private CharSequence aboutText() {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        for (String k : DOC) {
            boolean head = k.startsWith("#");
            String key = head ? k.substring(1) : k;
            if (sb.length() > 0) sb.append(head ? "\n\n" : "\n");
            int a = sb.length();
            if (head) {
                sb.append(upper(w(key)));
                sb.setSpan(new AbsoluteSizeSpan(12, true), a, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new StyleSpan(Typeface.BOLD), a, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new ForegroundColorSpan(0x803A2E1E), a, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                appendBold(sb, w(key));
            }
        }
        sb.append('\n');
        source(sb, w("src_logic"), "claude.ai", "https://claude.ai", " — Claude Opus 5.5");
        source(sb, w("src_art"), "chatgpt.com", "https://chatgpt.com", " \u2014 ChatGPT");
        source(sb, w("src_code"), "github.com/Shumtugle/hazna", "https://github.com/Shumtugle/hazna", "");
        sb.append('\n');
        int a = sb.length();
        sb.append("generated by AI · Claude Opus 5.5 · v").append(versionName());
        sb.setSpan(new AbsoluteSizeSpan(12, true), a, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new ForegroundColorSpan(0x733A2E1E), a, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    /** Words between asterisks are set in bold. */
    private static void appendBold(SpannableStringBuilder sb, String s) {
        String[] parts = s.split("\\*", -1);
        for (int i = 0; i < parts.length; i++) {
            int a = sb.length();
            sb.append(parts[i]);
            if (i % 2 == 1 && i < parts.length - 1) {
                sb.setSpan(new StyleSpan(Typeface.BOLD), a, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (i % 2 == 1) {
                sb.insert(a, "*");
            }
        }
    }

    private void source(SpannableStringBuilder sb, String label, String shown, final String url, String tail) {
        sb.append('\n').append(label).append(": ");
        int a = sb.length();
        sb.append(shown);
        sb.setSpan(new ClickableSpan() {
            @Override public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (ActivityNotFoundException ignored) {
                    // nothing on the device opens links; the address stays readable
                }
            }

            @Override public void updateDrawState(TextPaint p) {
                p.setColor(Painter.BRONZE_DK);
                p.setUnderlineText(true);
            }
        }, a, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(tail);
    }

    /* ---------- an audit ---------- */

    private float screenDp() {
        int h = column.getHeight() - insetTop - insetBottom;
        if (h <= 0) h = getResources().getDisplayMetrics().heightPixels;
        return h / u.density;
    }

    private void startLevel(int n) {
        ticket++;
        if (autoStep != null) ui.removeCallbacks(autoStep);
        autoStep = null;
        auto = IDLE;
        won = false;
        game = new Game(n, store.strict, screenDp(), random);
        board.setGame(game);
        show(winVeil, false);
        resetButtons();
        updateStats();
    }

    @Override
    public void onSack(int i) {
        if (game == null || won || auto != IDLE) return;
        Game.Tap t = game.tap(i);
        switch (t) {
            case UNSEAL:
                sound.play(Sound.BACK);
                board.changed(false);
                break;
            case POUR:
                poured();
                break;
            case WIN:
                poured();
                won();
                break;
            default:
                board.changed(false);
                break;
        }
    }

    @Override
    public void onSeal() {
        sound.play(Sound.SEAL);
    }

    private void poured() {
        sound.play(Sound.DROP);
        board.changed(true);
        updateStats();
    }

    private void won() {
        won = true;
        final Game g = game;
        final int level = game.level, moves = game.moves;
        store.level = level + 1;
        store.sealed += 1;
        store.total += moves;
        store.save();
        ui.postDelayed(new Runnable() {
            @Override public void run() { if (game == g) showWin(level, moves); }
        }, 620);
    }

    private void showWin(final int level, int moves) {
        boolean royal = level % 10 == 0;
        LinearLayout card = u.card();
        card.addView(u.title(royal ? w("padishah_remarked") : w("audit_closed")));
        if (royal) {
            int count = Words.remarks();
            int k = ((level / 10 - 1) % count) + 1;
            TextView q = u.text(16.5f, Painter.INK, Typeface.create("serif", Typeface.ITALIC));
            q.setAlpha(.88f);
            q.setGravity(Gravity.CENTER);
            q.setLineSpacing(0, 1.3f);
            q.setText("«" + w(String.format(Locale.ROOT, "remark_%03d", k)) + "»");
            LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(-1, -2);
            qp.topMargin = u.dp(10);
            card.addView(q, qp);
        }
        TextView cnt = u.caps(12.5f, .6f, .06f);
        cnt.setTypeface(Ui.REGULAR);
        cnt.setGravity(Gravity.CENTER);
        cnt.setText(upper(w("audit") + " " + level + " · " + w("moves") + " " + moves));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.topMargin = u.dp(14);
        cp.bottomMargin = u.dp(18);
        card.addView(cnt, cp);
        TextView next = u.button(16);
        next.setText(w("next_chest"));
        next.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                show(winVeil, false);
                if (level % 30 == 0) showArt(level + 1);
                else startLevel(level + 1);
            }
        });
        card.addView(next, new LinearLayout.LayoutParams(-1, -2));
        setCard(winVeil, card);
        show(winVeil, true);
    }

    /**
     * Every thirtieth audit a painting is shown for five seconds. The deck
     * holds each painting once in random order, and a fresh deck never
     * starts with the painting that ended the last.
     */
    private void showArt(final int nextLevel) {
        int[] deck = store.artDeck();
        if (deck.length == 0) {
            deck = new int[ART_COUNT];
            for (int i = 0; i < ART_COUNT; i++) deck[i] = i + 1;
            Engine.shuffle(random, deck);
            if (deck[ART_COUNT - 1] == store.lastArt) {
                int t = deck[ART_COUNT - 1];
                deck[ART_COUNT - 1] = deck[0];
                deck[0] = t;
            }
        }
        int n = deck[deck.length - 1];
        store.setArtDeck(java.util.Arrays.copyOf(deck, deck.length - 1));
        store.lastArt = n;
        store.save();
        Bitmap b = asset(String.format(Locale.ROOT, "art/%02d.jpg", n), 1400);
        if (b == null) {
            startLevel(nextLevel);
            return;
        }
        artSplash.removeAllViews();
        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        img.setImageBitmap(b);
        artSplash.addView(img, new FrameLayout.LayoutParams(-1, -1));
        artSplash.setAlpha(0f);
        artSplash.setVisibility(View.VISIBLE);
        amulet.setVisibility(View.INVISIBLE);
        artSplash.animate().alpha(1f).setDuration(400).start();
        final boolean[] done = {false};
        final Runnable finish = new Runnable() {
            @Override public void run() {
                if (done[0]) return;
                done[0] = true;
                artSplash.setOnClickListener(null);
                amulet.setVisibility(View.VISIBLE);
                artSplash.animate().alpha(0f).setDuration(400).withEndAction(new Runnable() {
                    @Override public void run() {
                        artSplash.setVisibility(View.GONE);
                        artSplash.removeAllViews();
                    }
                }).start();
                startLevel(nextLevel);
            }
        };
        artSplash.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish.run(); }
        });
        ui.postDelayed(finish, 5000);
    }

    /* ---------- buttons ---------- */

    private void resetButtons() {
        hintBtn.setText(w("hint"));
        autoBtn.setText(w("solve"));
        Ui.enable(resetBtn, true);
        Ui.enable(hintBtn, true);
        Ui.enable(autoBtn, true);
        updateStats();
    }

    private void undo() {
        if (game == null || auto != IDLE || won) return;
        if (game.undo()) {
            board.changed(false);
            updateStats();
        }
    }

    private void hint() {
        if (game == null || auto != IDLE || won) return;
        auto = HINTING;
        final int my = ++ticket;
        hintBtn.setText(w("thinking"));
        final int[][] s = Engine.copy(game.sacks);
        final Game g = game;
        worker.execute(new Runnable() {
            @Override public void run() {
                List<int[]> found;
                try {
                    found = Engine.solve(s, g.caps, g.kinds, g.totals, g.sealsFinal, 40000, 700, cancelFor(my));
                } catch (RuntimeException e) {
                    found = null;
                }
                final List<int[]> path = found;
                ui.post(new Runnable() {
                    @Override public void run() {
                        if (my != ticket) return;
                        auto = IDLE;
                        if (path == null || path.isEmpty()) {
                            hintBtn.setText(w("no_way"));
                            ui.postDelayed(new Runnable() {
                                @Override public void run() {
                                    if (auto == IDLE) hintBtn.setText(w("hint"));
                                }
                            }, 1800);
                            return;
                        }
                        hintBtn.setText(w("hint"));
                        game.selected = -1;
                        board.changed(false);
                        board.hint(path.get(0)[0], path.get(0)[1]);
                    }
                });
            }
        });
    }

    private Engine.Cancel cancelFor(final int my) {
        return new Engine.Cancel() {
            @Override public boolean cancelled() { return my != ticket; }
        };
    }

    /**
     * Solve: find a path from here and play it. In a dead end, find the last
     * position from which the treasury still adds up, wind back to it, and
     * play on from there.
     */
    private void solve() {
        if (game == null || won) return;
        if (auto != IDLE) {
            stopAuto();
            return;
        }
        auto = THINKING;
        final int my = ++ticket;
        Ui.enable(undoBtn, false);
        Ui.enable(resetBtn, false);
        Ui.enable(hintBtn, false);
        game.selected = -1;
        board.changed(false);
        autoBtn.setText(w("thinking"));
        final Game g = game;
        final int[][][] past = g.positions();
        worker.execute(new Runnable() {
            @Override public void run() {
                List<int[]> found = null;
                Game.Way way = null;
                try {
                    found = Engine.solve(past[past.length - 1], g.caps, g.kinds, g.totals, g.sealsFinal, 60000, 900,
                            cancelFor(my));
                    if ((found == null || found.isEmpty()) && my == ticket) way = g.lastSolvable(past, cancelFor(my));
                } catch (RuntimeException e) {
                    found = null;
                    way = null;
                }
                final List<int[]> path = found;
                final Game.Way back = way;
                ui.post(new Runnable() {
                    @Override public void run() {
                        if (my != ticket || g != game) return;
                        if (path != null && !path.isEmpty()) {
                            playPath(path, my);
                        } else if (back == null) {
                            autoBtn.setText(w("no_way"));
                            ui.postDelayed(new Runnable() {
                                @Override public void run() { if (my == ticket) stopAuto(); }
                            }, 1800);
                        } else {
                            rewind(back, my);
                        }
                    }
                });
            }
        });
    }

    private void playPath(final List<int[]> path, final int my) {
        auto = PLAYING;
        autoBtn.setText(w("stop"));
        final int[] k = {0};
        autoStep = new Runnable() {
            @Override public void run() {
                if (my != ticket) return;
                if (k[0] >= path.size()) { stopAuto(); return; }
                int[] m = path.get(k[0]++);
                if (!game.play(m[0], m[1])) { stopAuto(); return; }
                poured();
                if (game.solved()) {
                    stopAuto();
                    won();
                    return;
                }
                ui.postDelayed(this, 430);
            }
        };
        ui.postDelayed(autoStep, 140);
    }

    private void rewind(final Game.Way back, final int my) {
        auto = REWINDING;
        autoBtn.setText(w("rewinding"));
        autoStep = new Runnable() {
            @Override public void run() {
                if (my != ticket) return;
                if (game.historySize() <= back.index) {
                    playPath(back.path, my);
                    return;
                }
                game.undo();
                sound.play(Sound.BACK);
                board.changed(false);
                updateStats();
                ui.postDelayed(this, 260);
            }
        };
        ui.postDelayed(autoStep, 140);
    }

    private void stopAuto() {
        ticket++;
        if (autoStep != null) ui.removeCallbacks(autoStep);
        autoStep = null;
        auto = IDLE;
        resetButtons();
    }

    /* ---------- back ---------- */

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        for (FrameLayout v : new FrameLayout[]{askVeil, aboutVeil, setupVeil}) {
            if (v.getVisibility() == View.VISIBLE) {
                show(v, false);
                return;
            }
        }
        super.onBackPressed();
    }

}
