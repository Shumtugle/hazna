package io.github.shumtugle.hazna;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/** The board: owns the animation clock and turns touches into sack taps. */
final class BoardView extends View {

    interface Listener {
        void onSack(int index);

        void onSeal();
    }

    private static final long LIFT_MS = 180, COVER_MS = 340, STAMP_DELAY = 220, STAMP_MS = 280, FLY_MS = 280;

    private final Painter painter = new Painter();
    private final Painter.Anim anim = new Painter.Anim();
    private final CanvasPen pen = new CanvasPen();
    private final float density;
    private final int slop;
    private Listener listener;
    private Game game;

    private float[] liftFrom = new float[0], liftTo = new float[0];
    private long[] liftAt = new long[0], sealAt = new long[0];
    private boolean[] shown = new boolean[0];
    private int hintA = -1, hintB = -1;
    private Runnable hintSecond, hintClear;
    private long flyAt;
    private float downX, downY;
    private boolean pressed;

    BoardView(Context c) {
        super(c);
        density = c.getResources().getDisplayMetrics().density;
        slop = ViewConfiguration.get(c).getScaledTouchSlop();
        setWillNotDraw(false);
    }

    void setListener(Listener l) {
        listener = l;
    }

    /** A fresh deal: seals already closed at the start appear closed, without ceremony. */
    void setGame(Game g) {
        game = g;
        int n = g.sacks.length;
        anim.size(n);
        liftFrom = new float[n];
        liftTo = new float[n];
        liftAt = new long[n];
        sealAt = new long[n];
        shown = new boolean[n];
        anim.flyKind = -1;
        cancelHint();
        for (int i = 0; i < n; i++) {
            shown[i] = g.shut(i);
            anim.cover[i] = shown[i] ? 1 : 0;
            anim.stamp[i] = shown[i] ? 1 : 0;
            anim.lift[i] = 0;
        }
        painter.invalidate();
        requestLayout();
        invalidate();
    }

    /**
     * The game changed. After a pour the stone flies and a sack that has just
     * closed is sealed with a stamp and a sound; after an undo or a rewind
     * seals simply are what they are.
     */
    void changed(boolean poured) {
        if (game == null) return;
        long now = SystemClock.uptimeMillis();
        if (poured && game.lastFrom >= 0 && painter.left.length == game.sacks.length) {
            int f = game.lastFrom, t = game.lastTo;
            anim.flyKind = game.lastKind;
            anim.flyX0 = painter.anchorX(f);
            anim.flyY0 = painter.anchorY(f, anim.lift[f]);
            anim.flyX1 = painter.anchorX(t);
            anim.flyY1 = painter.anchorY(t, 0);
            anim.flyT = 0;
            flyAt = now;
        }
        boolean sealed = false;
        for (int i = 0; i < game.sacks.length; i++) {
            boolean s = game.shut(i);
            if (s && !shown[i]) {
                if (poured) {
                    sealAt[i] = now;
                    sealed = true;
                } else {
                    sealAt[i] = 0;
                    anim.cover[i] = 1;
                    anim.stamp[i] = 1;
                }
            } else if (!s) {
                sealAt[i] = 0;
                anim.cover[i] = 0;
                anim.stamp[i] = 0;
            }
            shown[i] = s;
        }
        if (sealed && listener != null) listener.onSeal();
        retarget(now);
        invalidate();
    }

    /** Lifts the two sacks of a suggested move, one after the other. */
    void hint(final int from, final int to) {
        cancelHint();
        hintA = from;
        retarget(SystemClock.uptimeMillis());
        invalidate();
        hintSecond = new Runnable() {
            @Override
            public void run() {
                hintB = to;
                retarget(SystemClock.uptimeMillis());
                invalidate();
            }
        };
        hintClear = new Runnable() {
            @Override
            public void run() {
                hintA = hintB = -1;
                retarget(SystemClock.uptimeMillis());
                invalidate();
            }
        };
        postDelayed(hintSecond, 220);
        postDelayed(hintClear, 1100);
    }

    private void cancelHint() {
        if (hintSecond != null) removeCallbacks(hintSecond);
        if (hintClear != null) removeCallbacks(hintClear);
        hintSecond = hintClear = null;
        hintA = hintB = -1;
    }

    private void retarget(long now) {
        if (game == null) return;
        for (int i = 0; i < game.sacks.length; i++) {
            boolean up = i == game.selected || i == hintA || i == hintB;
            anim.ring[i] = up;
            float target = up ? Painter.LIFT : 0;
            if (target != liftTo[i]) {
                liftFrom[i] = anim.lift[i];
                liftTo[i] = target;
                liftAt[i] = now;
            }
        }
    }

    private boolean tick(long now) {
        boolean more = false;
        for (int i = 0; i < anim.lift.length; i++) {
            if (liftAt[i] > 0) {
                float t = (now - liftAt[i]) / (float) LIFT_MS;
                if (t >= 1) {
                    anim.lift[i] = liftTo[i];
                    liftAt[i] = 0;
                } else {
                    anim.lift[i] = liftFrom[i] + (liftTo[i] - liftFrom[i]) * Painter.ease(t, .4f, 1.4f, .5f, 1f);
                    more = true;
                }
            }
            if (sealAt[i] > 0) {
                long dt = now - sealAt[i];
                anim.cover[i] = Painter.ease(Math.min(1f, dt / (float) COVER_MS), 0f, 0f, .58f, 1f);
                anim.stamp[i] = Math.max(0f, Math.min(1f, (dt - STAMP_DELAY) / (float) STAMP_MS));
                if (dt >= STAMP_DELAY + STAMP_MS && dt >= COVER_MS) {
                    anim.cover[i] = 1;
                    anim.stamp[i] = 1;
                    sealAt[i] = 0;
                } else {
                    more = true;
                }
            }
        }
        if (anim.flyKind >= 0) {
            anim.flyT = (now - flyAt) / (float) FLY_MS;
            if (anim.flyT >= 1) anim.flyKind = -1;
            else more = true;
        }
        return more;
    }

    /** Height of the board area in density-independent units. */
    float heightDp() {
        return getHeight() / density;
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        painter.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (game == null || getWidth() == 0) return;
        painter.layout(game, getWidth() / density, getHeight() / density);
        boolean more = tick(SystemClock.uptimeMillis());
        pen.begin(canvas, density);
        painter.draw(pen, game, anim);
        pen.end();
        if (more) postInvalidateOnAnimation();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                pressed = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(e.getX() - downX) > slop || Math.abs(e.getY() - downY) > slop) pressed = false;
                return true;
            case MotionEvent.ACTION_UP:
                if (pressed) {
                    pressed = false;
                    performClick();
                    if (game != null && listener != null) {
                        int i = painter.hit(game, anim, e.getX() / density, e.getY() / density);
                        if (i >= 0) listener.onSack(i);
                    }
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressed = false;
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
