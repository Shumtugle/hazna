package io.github.shumtugle.hazna;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Renders boards to images on a desktop, through the same painting code the
 * phone uses, so the look can be checked without a device.
 *
 *   java Preview <out-dir>
 */
public final class Preview {

    static final class AwtPen implements Pen {
        final Graphics2D g;
        private Paint paint = Color.BLACK;
        private Path2D.Float path = new Path2D.Float();
        private float alpha = 1f;
        private final ArrayDeque<Object[]> stack = new ArrayDeque<>();

        AwtPen(Graphics2D g) {
            this.g = g;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }

        @Override public void save() { stack.push(new Object[]{g.getTransform(), g.getClip(), alpha}); }

        @Override public void restore() {
            Object[] s = stack.pop();
            g.setTransform((java.awt.geom.AffineTransform) s[0]);
            g.setClip((Shape) s[1]);
            alpha = (Float) s[2];
        }

        @Override public void translate(float x, float y) { g.translate(x, y); }
        @Override public void rotate(float d) { g.rotate(Math.toRadians(d)); }
        @Override public void scale(float sx, float sy) { g.scale(sx, sy); }
        @Override public void alpha(float a) { alpha *= a; }

        private static Color c(int argb) { return new Color(argb, true); }

        @Override public void solid(int argb) { paint = c(argb); }

        @Override public void linear(float x0, float y0, float x1, float y1, int[] colors, float[] stops) {
            if (x0 == x1 && y0 == y1) y1 += 0.01f;
            Color[] cs = new Color[colors.length];
            for (int i = 0; i < cs.length; i++) cs[i] = c(colors[i]);
            paint = new LinearGradientPaint(new Point2D.Float(x0, y0), new Point2D.Float(x1, y1), fix(stops), cs,
                    MultipleGradientPaint.CycleMethod.NO_CYCLE);
        }

        @Override public void radial(float cx, float cy, float r, int[] colors, float[] stops) {
            Color[] cs = new Color[colors.length];
            for (int i = 0; i < cs.length; i++) cs[i] = c(colors[i]);
            paint = new RadialGradientPaint(new Point2D.Float(cx, cy), Math.max(r, 0.01f), fix(stops), cs);
        }

        private static float[] fix(float[] s) {
            float[] f = s.clone();
            for (int i = 1; i < f.length; i++) if (f[i] <= f[i - 1]) f[i] = Math.min(1f, f[i - 1] + 1e-4f);
            return f;
        }

        @Override public void path(boolean evenOdd) {
            path = new Path2D.Float(evenOdd ? Path2D.WIND_EVEN_ODD : Path2D.WIND_NON_ZERO);
        }

        @Override public void moveTo(float x, float y) { path.moveTo(x, y); }
        @Override public void lineTo(float x, float y) { path.lineTo(x, y); }
        @Override public void cubicTo(float a, float b, float c, float d, float x, float y) { path.curveTo(a, b, c, d, x, y); }
        @Override public void close() { path.closePath(); }
        @Override public void rect(float l, float t, float r, float b) { path.append(new Rectangle2D.Float(l, t, r - l, b - t), false); }
        @Override public void oval(float l, float t, float r, float b) { path.append(new Ellipse2D.Float(l, t, r - l, b - t), false); }
        @Override public void round(float l, float t, float r, float b, float rx, float ry) {
            path.append(new RoundRectangle2D.Float(l, t, r - l, b - t, rx * 2, ry * 2), false);
        }

        private void paintWith() {
            g.setPaint(paint);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, Math.min(1, alpha))));
        }

        @Override public void fill() { paintWith(); g.fill(path); }

        @Override public void stroke(float width) {
            paintWith();
            g.setStroke(new BasicStroke(width));
            g.draw(path);
        }

        @Override public void clip() { g.clip(path); }
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : ".");
        dir.mkdirs();
        int[][] shots = {{3, 800}, {12, 800}, {40, 800}, {40, 640}};
        Random r = new Random(5);
        for (int[] s : shots) {
            float W = 411, H = s[1] - 56 - 140;
            Game g = new Game(s[0], true, s[1], r);
            // play the first few solver moves so some sacks fill and seal
            java.util.List<int[]> path = g.solveFromHere(60000, 3000, null);
            int play = path == null ? 0 : path.size() * 2 / 3;
            for (int k = 0; k < play; k++) g.play(path.get(k)[0], path.get(k)[1]);
            Painter pt = new Painter();
            pt.layout(g, W, H);
            Painter.Anim a = new Painter.Anim();
            a.size(g.sacks.length);
            for (int i = 0; i < g.sacks.length; i++) if (g.shut(i)) { a.cover[i] = 1; a.stamp[i] = 1; }
            int lifted = -1;
            for (int i : g.order) if (g.sacks[i].length > 0 && !g.shut(i)) { lifted = i; break; }
            if (lifted >= 0) { a.lift[lifted] = Painter.LIFT; a.ring[lifted] = true; }
            int to = g.chestOrder.length > 0 ? g.chestOrder[0] : g.order[g.order.length - 1];
            if (lifted >= 0) {
                a.flyKind = Engine.topKind(g.sacks[lifted]);
                a.flyX0 = pt.anchorX(lifted); a.flyY0 = pt.anchorY(lifted, Painter.LIFT);
                a.flyX1 = pt.anchorX(to); a.flyY1 = pt.anchorY(to, 0); a.flyT = 0.45f;
            }
            float scale = 2.625f;
            BufferedImage img = new BufferedImage(Math.round(W * scale), Math.round(H * scale), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            AwtPen pen = new AwtPen(g2);
            pen.scale(scale, scale);
            Painter.background(pen, W, H);
            pt.draw(pen, g, a);
            g2.dispose();
            File f = new File(dir, "board-" + s[0] + "-" + s[1] + ".png");
            ImageIO.write(img, "png", f);
            System.out.println(f + "  kinds " + g.kinds + "  u " + pt.u + "  w " + pt.w + "  perRow " + pt.perRow);
        }
        // a sheet of every cut, large
        BufferedImage img = new BufferedImage(12 * 70, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        AwtPen pen = new AwtPen(g2);
        Painter.background(pen, 12 * 70, 80);
        for (int k = 0; k < 12; k++) Painter.gem(pen, k, 35 + k * 70, 40, 46, 1f);
        g2.dispose();
        ImageIO.write(img, "png", new File(dir, "cuts.png"));
    }
}
