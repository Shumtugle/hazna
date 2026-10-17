package io.github.shumtugle.hazna;

/**
 * Lays the sacks out on the board and paints them. Pure Java over Pen, so
 * the board can be rendered to an image on a desktop for checking.
 *
 * Units are density-independent. A sack is a leather cuff with a drawstring
 * on top, and below it a mouth of leather walls holding a column of layers;
 * each layer shows one stone flanked by two smaller ones of the same kind.
 */
final class Painter {

    /* ---------- palette ---------- */

    static final int IVORY = 0xFFF4EBD8, BEIGE = 0xFFE3D5B8, BEIGE_DK = 0xFFCDBC98;
    static final int BRONZE = 0xFF8C6A3F, BRONZE_LT = 0xFFB58F55, BRONZE_DK = 0xFF5C4526;
    static final int LEATHER = 0xFF7A5636, LEATHER_LT = 0xFF9A7048, LEATHER_DK = 0xFF4E3521;
    static final int INK = 0xFF3A2E1E;

    /** Stone kinds: colour and cut. Twelve, which is also the most kinds an audit can hold. */
    static final int[] GEM_COLOR = {
        0xFFC62348, 0xFF1E8F63, 0xFF2A55B8, 0xFFE9A81C, 0xFF7B3FB8, 0xFF35A8C4,
        0xFF33313F, 0xFFEDE4D0, 0xFF7FA33A, 0xFFE4693C, 0xFFB8D14A, 0xFFE24D8C
    };
    static final int[] GEM_CUT = {0, 4, 9, 1, 3, 2, 8, 5, 7, 6, 10, 11};

    /* ---------- geometry ---------- */

    static final float CUFF = 11f;      // room above the mouth for the cuff
    static final float MOUTH_PAD = 8f;  // mouth height beyond its layers: the bottom wall and a little air
    static final float WALL = 5f;
    static final float FLOOR = 6f;
    static final float LIFT = 20f;
    static final float ROW_GAP = 18f, COL_GAP = 12f, CHEST_GAP = 30f, BLOCK_GAP = 12f;

    float u = 30f, w = 58f;
    int perRow = 4;
    float[] left = new float[0], top = new float[0], bottom = new float[0];
    private int laidFor = -1;
    private float laidW, laidH;
    private Game laidGame;

    /** Per-frame state of the animations, filled in by whoever owns the clock. */
    static final class Anim {
        float[] lift = new float[0];
        /** Closing flap, 0..1; 1 is fully closed. */
        float[] cover = new float[0];
        /** Wax stamp, 0..1. */
        float[] stamp = new float[0];
        boolean[] ring = new boolean[0];
        int flyKind = -1;
        float flyX0, flyY0, flyX1, flyY1, flyT;

        void size(int n) {
            if (lift.length == n) return;
            lift = new float[n];
            cover = new float[n];
            stamp = new float[n];
            ring = new boolean[n];
        }
    }

    float sackHeight(int cap) {
        return CUFF + cap * u + MOUTH_PAD;
    }

    /**
     * Picks the stone size and how many sacks share a row so that the
     * largest stones fit. The height comes from the space given to the board,
     * never from the board's own contents: measured from itself it would
     * inflate with every pass.
     */
    void layout(Game g, float width, float height) {
        if (g == laidGame && width == laidW && height == laidH && laidFor == g.sacks.length) return;
        laidGame = g; laidW = width; laidH = height; laidFor = g.sacks.length;

        int[] ord = g.order, ch = g.chestOrder;
        int maxChest = 0;
        for (int i : ch) maxChest = Math.max(maxChest, g.caps[i]);
        // a lifted sack in the top row must still fit, so the lift is kept free above
        float availH = height - 12 - LIFT;
        float availW = Math.min(width, 520) - 14;
        float bestU = -1, bestW = 44;
        int bestPr = 4;
        for (int pr = 3; pr <= 6; pr++) {
            if (pr > ord.length && pr > 3) break;
            float sw = (float) Math.floor(availW / pr) - 12;
            if (sw < 40) continue;
            int units = maxChest, rows = 0;
            for (int r = 0; r < ord.length; r += pr) {
                int m = 0;
                for (int k = r; k < Math.min(ord.length, r + pr); k++) m = Math.max(m, g.caps[ord[k]]);
                units += m;
                rows++;
            }
            int rowCount = rows + (ch.length > 0 ? 1 : 0);
            // everything in the column that is not stones: cuffs and floors, gaps between rows
            float fixed = rowCount * (CUFF + MOUTH_PAD) + Math.max(0, rows - 1) * ROW_GAP
                    + (ch.length > 0 && rows > 0 ? BLOCK_GAP : 0);
            float uu = (float) Math.floor((availH - fixed) / Math.max(1, units));
            uu = Math.min(uu, Math.min((float) Math.floor((sw - 18) / 1.5f), 46));
            if (uu > bestU) { bestU = uu; bestW = Math.min(sw, 92); bestPr = pr; }
        }
        if (bestU < 0) { bestU = 15; bestW = 44; bestPr = 4; }
        u = Math.max(bestU, 14);
        w = bestW;
        perRow = bestPr;

        int n = g.sacks.length;
        left = new float[n];
        top = new float[n];
        bottom = new float[n];

        float total = 0;
        int rows = (ord.length + perRow - 1) / perRow;
        float[] rowH = new float[rows];
        for (int r = 0; r < rows; r++) {
            for (int k = r * perRow; k < Math.min(ord.length, (r + 1) * perRow); k++)
                rowH[r] = Math.max(rowH[r], sackHeight(g.caps[ord[k]]));
            total += rowH[r];
        }
        total += ROW_GAP * Math.max(0, rows - 1);
        float chestH = 0;
        for (int i : ch) chestH = Math.max(chestH, sackHeight(g.caps[i]));
        if (ch.length > 0) total += BLOCK_GAP + chestH;

        float y = Math.max(6 + LIFT, (height - total + LIFT) / 2);
        for (int r = 0; r < rows; r++) {
            int from = r * perRow, to = Math.min(ord.length, (r + 1) * perRow);
            int cnt = to - from;
            float rowW = cnt * w + (cnt - 1) * COL_GAP;
            float x = (width - rowW) / 2;
            float base = y + rowH[r];
            for (int k = from; k < to; k++) {
                int i = ord[k];
                left[i] = x;
                bottom[i] = base;
                top[i] = base - sackHeight(g.caps[i]);
                x += w + COL_GAP;
            }
            y = base + ROW_GAP;
        }
        if (ch.length > 0) {
            y = y - ROW_GAP + (rows > 0 ? BLOCK_GAP : 0);
            float rowW = ch.length * w + (ch.length - 1) * CHEST_GAP;
            float x = (width - rowW) / 2;
            float base = y + chestH;
            for (int i : ch) {
                left[i] = x;
                bottom[i] = base;
                top[i] = base - sackHeight(g.caps[i]);
                x += w + CHEST_GAP;
            }
        }
    }

    void invalidate() {
        laidGame = null;
    }

    /** The sack under a touch; generous, so a fingertip between two sacks still lands. */
    int hit(Game g, Anim a, float x, float y) {
        int best = -1;
        float bestD = Float.MAX_VALUE;
        for (int i = 0; i < g.sacks.length && i < left.length; i++) {
            float lift = i < a.lift.length ? a.lift[i] : 0;
            float l = left[i] - COL_GAP / 2, r = left[i] + w + COL_GAP / 2;
            float t = top[i] - lift - 10, b = bottom[i] + 10;
            if (x < l || x > r || y < t || y > b) continue;
            float d = Math.abs(x - (left[i] + w / 2));
            if (d < bestD) { bestD = d; best = i; }
        }
        return best;
    }

    /** Where a flying stone takes off from or lands on: just above the sack's centre. */
    float anchorX(int i) {
        return left[i] + w / 2;
    }

    float anchorY(int i, float lift) {
        return top[i] - lift + 8;
    }

    /* ---------- painting ---------- */

    /** The page under everything: warm paper lit from above, with a faint vertical grain. */
    static void background(Pen p, float width, float height) {
        p.solid(BEIGE_DK);
        p.path(false);
        p.rect(0, 0, width, height);
        p.fill();
        float rx = width * 1.2f, ry = height * 0.7f;
        p.save();
        p.translate(width / 2, 0);
        p.scale(1, ry / rx);
        p.radial(0, 0, rx, new int[]{IVORY, BEIGE, BEIGE_DK}, new float[]{0f, 0.62f, 1f});
        p.path(false);
        p.rect(-width, 0, width, height * rx / ry);
        p.fill();
        p.restore();
        p.solid(0x0D8C6A3F);
        p.path(false);
        for (float x = 0; x < width; x += 3) p.rect(x, 0, x + 1, height);
        p.fill();
    }

    void draw(Pen p, Game g, Anim a) {
        for (int k = 0; k < g.order.length; k++) sack(p, g, a, g.order[k]);
        for (int k = 0; k < g.chestOrder.length; k++) sack(p, g, a, g.chestOrder[k]);
        if (a.flyKind >= 0) flight(p, a);
    }

    private void sack(Pen p, Game g, Anim a, int i) {
        float x = left[i], lift = a.lift[i];
        float y0 = top[i] - lift;
        int cap = g.caps[i];
        float mTop = y0 + CUFF, mBot = mTop + cap * u + MOUTH_PAD;
        float rxO = Math.min(26, w / 2), ryO = Math.min(20, (mBot - mTop) / 2);

        // shadow under the sack; deeper while it is lifted
        float sh = lift > 1 ? 0.22f : 0.14f;
        p.solid(argb(sh, 0x50, 0x3A, 0x1C));
        p.path(false);
        bottomRound(p, x, mTop + 4, x + w, mBot + 4 + lift * 0.3f, 2, rxO, ryO);
        p.fill();

        // cuff and drawstring
        p.linear(0, y0, 0, y0 + 15, new int[]{LEATHER_LT, LEATHER, LEATHER_DK}, new float[]{0f, 0.7f, 1f});
        p.path(false);
        p.round(x - 5, y0, x + w + 5, y0 + 15, 6, 6);
        p.fill();
        p.linear(x, 0, x + w, 0, new int[]{BRONZE_LT, 0xFFE2C489, BRONZE}, new float[]{0f, 0.5f, 1f});
        p.path(false);
        p.round(x, y0 + 7, x + w, y0 + 9, 1, 1);
        p.fill();

        // the inside: dark, translucent, with a shadow falling from the rim
        float iL = x + WALL, iR = x + w - WALL, iB = mBot - FLOOR;
        float rxI = Math.max(1, rxO - WALL), ryI = Math.max(1, ryO - FLOOR);
        p.linear(0, mTop, 0, iB, new int[]{argb(.55f, 78, 53, 33), argb(.28f, 78, 53, 33), argb(.5f, 78, 53, 33)},
                new float[]{0f, 0.22f, 1f});
        p.path(false);
        bottomRound(p, iL, mTop, iR, iB, 1, rxI, ryI);
        p.fill();
        p.linear(0, mTop, 0, mTop + 10, new int[]{argb(.55f, 40, 25, 10), 0x00281A0A}, new float[]{0f, 1f});
        p.path(false);
        p.rect(iL, mTop, iR, mTop + 10);
        p.fill();

        // stones
        p.save();
        p.path(false);
        bottomRound(p, iL, mTop, iR, iB, 1, rxI, ryI);
        p.clip();
        int[] b = g.sacks[i];
        float cx = x + w / 2;
        float floor = mTop + cap * u + 2;
        for (int k = 0; k < b.length; k++) {
            float cy = floor - (k + 0.5f) * u;
            int kind = b[k];
            float big = u * 0.66f, small = u * 0.40f;
            float off = big / 2 + 1 + small / 2;
            gem(p, kind, cx - off, cy, small, 0.94f);
            gem(p, kind, cx, cy, big, 1f);
            gem(p, kind, cx + off, cy, small, 0.94f);
        }
        // the closing flap of a sealed sack, sliding down from the rim
        float cover = a.cover[i];
        if (cover > 0) {
            p.save();
            p.path(false);
            p.rect(iL, mTop, iR, mTop + (iB - mTop) * cover);
            p.clip();
            float dx = (iB - mTop) * 0.577f;
            p.linear(cx - dx / 2, mTop, cx + dx / 2, iB, new int[]{LEATHER_LT, LEATHER, LEATHER_DK},
                    new float[]{0f, 0.45f, 1f});
            p.path(false);
            p.rect(iL, mTop, iR, iB);
            p.fill();
            p.restore();
        }
        p.restore();

        // the walls
        p.solid(LEATHER);
        p.path(true);
        bottomRound(p, x, mTop, x + w, mBot, 2, rxO, ryO);
        bottomRound(p, iL, mTop, iR, iB, 1, rxI, ryI);
        p.fill();
        p.save();
        p.path(false);
        p.rect(x - 1, iB - ryI * 0.35f, x + w + 1, mBot + 1);
        p.clip();
        p.solid(LEATHER_DK);
        p.path(true);
        bottomRound(p, x, mTop, x + w, mBot, 2, rxO, ryO);
        bottomRound(p, iL, mTop, iR, iB, 1, rxI, ryI);
        p.fill();
        p.restore();

        if (a.ring[i]) {
            p.solid(BRONZE_LT);
            p.path(false);
            bottomRound(p, x - 1, mTop - 1, x + w + 1, mBot + 1, 3, rxO + 1, ryO + 1);
            p.stroke(2);
        }

        // the wax seal
        float st = a.stamp[i];
        if (st > 0 && b.length > 0) {
            float d = w * 0.46f;
            float sy = y0 + (mBot - y0) * 0.36f + d / 2;
            float s = 1 + 1.2f * (1 - ease(st, .3f, 1.6f, .5f, 1f));
            float rot = -14 * (1 - st);
            p.save();
            p.alpha(Math.min(1, st * 1.6f));
            p.translate(cx, sy);
            p.rotate(rot);
            p.scale(s, s);
            p.solid(0x80321F0F);
            p.path(false);
            p.oval(-d / 2, -d / 2 + 2, d / 2, d / 2 + 2);
            p.fill();
            p.radial(-d * 0.16f, -d * 0.2f, d * 0.75f, new int[]{0xFFD8B471, BRONZE, BRONZE_DK},
                    new float[]{0f, 0.6f, 1f});
            p.path(false);
            p.oval(-d / 2, -d / 2, d / 2, d / 2);
            p.fill();
            p.solid(0x40FFFFFF);
            p.path(false);
            p.oval(-d / 2 + .5f, -d / 2 + .5f, d / 2 - .5f, d / 2 - .5f);
            p.stroke(1);
            float dot = w * 0.17f;
            p.solid(GEM_COLOR[b[0]]);
            p.path(false);
            p.oval(-dot / 2, -dot / 2, dot / 2, dot / 2);
            p.fill();
            p.solid(0x66000000);
            p.path(false);
            p.oval(-dot / 2 + .5f, -dot / 2 + .5f, dot / 2 - .5f, dot / 2 - .5f);
            p.stroke(1);
            p.restore();
        }
    }

    private void flight(Pen p, Anim a) {
        float t = ease(a.flyT, .4f, .1f, .6f, 1f);
        float dx = a.flyX1 - a.flyX0, dy = a.flyY1 - a.flyY0;
        float mx = dx * 0.5f, my = Math.min(dy, 0) - 32;
        float x, y, rot;
        if (t < 0.5f) {
            float k = t / 0.5f;
            x = mx * k; y = my * k; rot = 160 * k;
        } else {
            float k = (t - 0.5f) / 0.5f;
            x = mx + (dx - mx) * k; y = my + (dy - my) * k; rot = 160 + 160 * k;
        }
        p.save();
        p.translate(a.flyX0 + x, a.flyY0 + y);
        p.rotate(rot);
        gem(p, a.flyKind, 0, 0, 20, 1f - 0.7f * a.flyT);
        p.restore();
    }

    /* ---------- stones ---------- */

    /** One stone of a kind, centred on cx, cy, its box size by size. */
    static void gem(Pen p, int kind, float cx, float cy, float size, float alpha) {
        int cut = GEM_CUT[kind];
        float bw = size, bh = size;
        if (cut == 6) bh = size * 0.70f;
        float l = cx - bw / 2, t = cy - bh / 2, r = cx + bw / 2, b = cy + bh / 2;
        p.save();
        p.alpha(alpha);
        p.solid(0x55000000);
        p.path(false);
        cut(p, cut, l, t + 1, r, b + 1);
        p.fill();
        p.solid(GEM_COLOR[kind]);
        p.path(false);
        cut(p, cut, l, t, r, b);
        p.fill();
        p.save();
        p.path(false);
        cut(p, cut, l, t, r, b);
        p.clip();
        p.linear(0, t, 0, b, new int[]{0x80FFFFFF, 0x00FFFFFF, 0x00000000, 0x73000000},
                new float[]{0f, 0.22f, 0.62f, 1f});
        p.path(false);
        p.rect(l, t, r, b);
        p.fill();
        p.solid(0xB8FFFFFF);
        p.path(false);
        p.oval(l + bw * 0.16f, t + bh * 0.10f, l + bw * 0.46f, t + bh * 0.36f);
        p.fill();
        p.restore();
        p.solid(0x8C231405);
        p.path(false);
        cut(p, cut, l + .5f, t + .5f, r - .5f, b - .5f);
        p.stroke(1);
        p.restore();
    }

    /** The outline of a cut inside the box l, t, r, b. */
    static void cut(Pen p, int cut, float l, float t, float r, float b) {
        float W = r - l, H = b - t;
        switch (cut) {
            case 0: p.oval(l, t, r, b); break;
            case 1: blob(p, l, t, r, b, .403f, .5f, .403f, .5f); break;
            case 2: poly(p, l, t, W, H, 50, 0, 86, 50, 50, 100, 14, 50); break;
            case 3: blob(p, l, t, r, b, .439f, .368f, .456f, .632f); break;
            case 4: poly(p, l, t, W, H, 26, 0, 74, 0, 100, 26, 100, 74, 74, 100, 26, 100, 0, 74, 0, 26); break;
            case 5: blob(p, l, t, r, b, .5f, .62f, .34f, .38f); break;
            case 6: p.round(l, t, r, b, 2, 2); break;
            case 7: poly(p, l, t, W, H, 50, 4, 96, 88, 4, 88); break;
            case 8: poly(p, l, t, W, H, 25, 0, 75, 0, 100, 50, 75, 100, 25, 100, 0, 50); break;
            case 9: poly(p, l, t, W, H, 50, 0, 100, 50, 50, 100, 0, 50); break;
            case 10: poly(p, l, t, W, H, 50, 2, 62, 36, 97, 36, 69, 58, 80, 94, 50, 72, 20, 94, 31, 58, 3, 36, 38, 36); break;
            default: poly(p, l, t, W, H, 36, 0, 64, 0, 64, 36, 100, 36, 100, 64, 64, 64, 64, 100, 36, 100,
                    36, 64, 0, 64, 0, 36, 36, 36); break;
        }
    }

    private static void poly(Pen p, float l, float t, float W, float H, float... pct) {
        p.moveTo(l + W * pct[0] / 100, t + H * pct[1] / 100);
        for (int k = 2; k < pct.length; k += 2) p.lineTo(l + W * pct[k] / 100, t + H * pct[k + 1] / 100);
        p.close();
    }

    /**
     * A box with elliptical corners: top corners rx, ry and bottom corners
     * rx2, ry2, all as fractions of the box.
     */
    private static void blob(Pen p, float l, float t, float r, float b, float rx, float ry, float rx2, float ry2) {
        float W = r - l, H = b - t;
        corners(p, l, t, r, b, W * rx, H * ry, W * rx2, H * ry2);
    }

    /** A box whose top corners are nearly square and whose bottom corners are rounded wide. */
    private static void bottomRound(Pen p, float l, float t, float r, float b, float rTop, float rx, float ry) {
        corners(p, l, t, r, b, rTop, rTop, rx, ry);
    }

    private static final float K = 0.5523f;

    private static void corners(Pen p, float l, float t, float r, float b, float rx, float ry, float rx2, float ry2) {
        p.moveTo(l + rx, t);
        p.lineTo(r - rx, t);
        p.cubicTo(r - rx + rx * K, t, r, t + ry - ry * K, r, t + ry);
        p.lineTo(r, b - ry2);
        p.cubicTo(r, b - ry2 + ry2 * K, r - rx2 + rx2 * K, b, r - rx2, b);
        p.lineTo(l + rx2, b);
        p.cubicTo(l + rx2 - rx2 * K, b, l, b - ry2 + ry2 * K, l, b - ry2);
        p.lineTo(l, t + ry);
        p.cubicTo(l, t + ry - ry * K, l + rx - rx * K, t, l + rx, t);
        p.close();
    }

    /* ---------- helpers ---------- */

    static int argb(float a, int r, int g, int b) {
        return (Math.round(a * 255) << 24) | (r << 16) | (g << 8) | b;
    }

    /** A CSS-style cubic easing curve, evaluated at t. */
    static float ease(float t, float x1, float y1, float x2, float y2) {
        if (t <= 0) return 0;
        if (t >= 1) return 1;
        float lo = 0, hi = 1, s = t;
        for (int k = 0; k < 24; k++) {
            s = (lo + hi) / 2;
            float x = bez(s, x1, x2);
            if (x < t) lo = s; else hi = s;
        }
        return bez(s, y1, y2);
    }

    private static float bez(float s, float a, float b) {
        float m = 1 - s;
        return 3 * m * m * s * a + 3 * m * s * s * b + s * s * s;
    }
}
