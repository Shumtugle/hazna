package io.github.shumtugle.hazna;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** The look: bronze buttons, parchment cards, warm paper. Built in code, no layouts. */
final class Ui {

    final Context c;
    final float density;

    Ui(Context c) {
        this.c = c;
        density = c.getResources().getDisplayMetrics().density;
    }

    int dp(float v) {
        return Math.round(v * density);
    }

    static final Typeface REGULAR = Typeface.create("sans-serif", Typeface.NORMAL);
    static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    static final Typeface BOLD = Typeface.create("sans-serif", Typeface.BOLD);
    static final Typeface HEAVY = Typeface.create("sans-serif-black", Typeface.NORMAL);

    TextView text(float sp, int color, Typeface face) {
        TextView t = new TextView(c);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(face);
        t.setIncludeFontPadding(true);
        return t;
    }

    /* ---------- buttons ---------- */

    GradientDrawable bronze() {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Painter.BRONZE_LT, Painter.BRONZE});
        d.setCornerRadius(dp(9));
        d.setStroke(dp(1), Painter.BRONZE_DK);
        return d;
    }

    GradientDrawable pale() {
        GradientDrawable d = new GradientDrawable();
        d.setColor(0x248C6A3F);
        d.setCornerRadius(dp(9));
        d.setStroke(dp(1), 0x595C4526);
        return d;
    }

    @SuppressLint("ClickableViewAccessibility")
    TextView button(float sp) {
        final TextView b = text(sp, Painter.IVORY, MEDIUM);
        b.setGravity(Gravity.CENTER);
        b.setBackground(bronze());
        b.setPadding(dp(14), dp(12), dp(14), dp(12));
        b.setMinHeight(dp(52));
        b.setElevation(dp(2));
        b.setClickable(true);
        b.setFocusable(true);
        b.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (!v.isEnabled()) return false;
                int a = e.getActionMasked();
                if (a == MotionEvent.ACTION_DOWN) {
                    v.setScaleX(.98f);
                    v.setScaleY(.98f);
                    v.setTranslationY(dp(1));
                } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                    v.setScaleX(1f);
                    v.setScaleY(1f);
                    v.setTranslationY(0);
                }
                return false;
            }
        });
        return b;
    }

    static void enable(View v, boolean on) {
        v.setEnabled(on);
        v.setAlpha(on ? 1f : .4f);
    }

    /** Two buttons side by side, or more wrapped in rows of two. */
    LinearLayout grid(View... cells) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < cells.length; i += 2) {
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int k = i; k < Math.min(cells.length, i + 2); k++) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
                if (k > i) lp.setMarginStart(dp(10));
                row.addView(cells[k], lp);
            }
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
            if (i > 0) rp.topMargin = dp(10);
            col.addView(row, rp);
        }
        return col;
    }

    /* ---------- cards ---------- */

    LinearLayout card() {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Painter.IVORY, Painter.BEIGE});
        d.setCornerRadius(dp(6));
        d.setStroke(dp(2), Painter.BRONZE);
        card.setBackground(d);
        card.setElevation(dp(12));
        card.setPadding(dp(20), dp(24), dp(20), dp(18));
        return card;
    }

    TextView title(String s) {
        TextView t = text(26, Painter.BRONZE_DK, HEAVY);
        t.setGravity(Gravity.CENTER);
        t.setText(s);
        return t;
    }

    /** A spaced-out label; the caller sets its text already in capitals. */
    TextView caps(float sp, float alpha, float spacing) {
        TextView t = text(sp, Painter.INK, BOLD);
        t.setLetterSpacing(spacing);
        t.setAlpha(alpha);
        return t;
    }

    /** The page under the whole window. */
    static final class Paper extends Drawable {
        private final CanvasPen pen = new CanvasPen();
        private final float density;

        Paper(float density) {
            this.density = density;
        }

        @Override
        public void draw(Canvas canvas) {
            pen.begin(canvas, density);
            Painter.background(pen, getBounds().width() / density, getBounds().height() / density);
            pen.end();
        }

        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(ColorFilter cf) {}
        @Override public int getOpacity() { return PixelFormat.OPAQUE; }
    }
}
