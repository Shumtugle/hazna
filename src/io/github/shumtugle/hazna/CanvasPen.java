package io.github.shumtugle.hazna;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

/** Pen over a platform canvas. One instance is reused for every frame. */
final class CanvasPen implements Pen {

    private Canvas c;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF r = new RectF();
    private float alpha = 1f;
    private final float[] alphas = new float[64];
    private int depth;
    private int color = 0xFF000000;
    private Shader shader;

    CanvasPen() {
        paint.setDither(true);
    }

    /** Points the pen at a canvas; scale turns density-independent units into pixels. */
    void begin(Canvas canvas, float scale) {
        c = canvas;
        depth = 0;
        alpha = 1f;
        c.save();
        c.scale(scale, scale);
    }

    void end() {
        while (depth > 0) restore();
        c.restore();
        c = null;
    }

    @Override public void save() {
        c.save();
        alphas[Math.min(depth, alphas.length - 1)] = alpha;
        depth++;
    }

    @Override public void restore() {
        if (depth == 0) return;
        depth--;
        alpha = alphas[Math.min(depth, alphas.length - 1)];
        c.restore();
    }

    @Override public void translate(float x, float y) { c.translate(x, y); }
    @Override public void rotate(float d) { c.rotate(d); }
    @Override public void scale(float sx, float sy) { c.scale(sx, sy); }
    @Override public void alpha(float a) { alpha *= a; }

    @Override public void solid(int argb) {
        color = argb;
        shader = null;
    }

    @Override public void linear(float x0, float y0, float x1, float y1, int[] colors, float[] stops) {
        if (x0 == x1 && y0 == y1) y1 += 0.01f;
        shader = new LinearGradient(x0, y0, x1, y1, colors, stops, Shader.TileMode.CLAMP);
        color = 0xFF000000;
    }

    @Override public void radial(float cx, float cy, float rad, int[] colors, float[] stops) {
        shader = new RadialGradient(cx, cy, Math.max(rad, 0.01f), colors, stops, Shader.TileMode.CLAMP);
        color = 0xFF000000;
    }

    @Override public void path(boolean evenOdd) {
        path.reset();
        path.setFillType(evenOdd ? Path.FillType.EVEN_ODD : Path.FillType.WINDING);
    }

    @Override public void moveTo(float x, float y) { path.moveTo(x, y); }
    @Override public void lineTo(float x, float y) { path.lineTo(x, y); }
    @Override public void cubicTo(float a, float b, float cc, float d, float x, float y) { path.cubicTo(a, b, cc, d, x, y); }
    @Override public void close() { path.close(); }

    @Override public void rect(float l, float t, float rr, float b) { path.addRect(l, t, rr, b, Path.Direction.CW); }

    @Override public void oval(float l, float t, float rr, float b) {
        r.set(l, t, rr, b);
        path.addOval(r, Path.Direction.CW);
    }

    @Override public void round(float l, float t, float rr, float b, float rx, float ry) {
        r.set(l, t, rr, b);
        path.addRoundRect(r, rx, ry, Path.Direction.CW);
    }

    private void prepare(Paint.Style style) {
        paint.setStyle(style);
        paint.setShader(shader);
        paint.setColor(color);
        int a = Math.round(((color >>> 24) & 0xFF) * Math.max(0, Math.min(1, alpha)));
        paint.setAlpha(a);
    }

    @Override public void fill() {
        prepare(Paint.Style.FILL);
        c.drawPath(path, paint);
    }

    @Override public void stroke(float width) {
        prepare(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        c.drawPath(path, paint);
    }

    @Override public void clip() {
        c.clipPath(path);
    }
}
