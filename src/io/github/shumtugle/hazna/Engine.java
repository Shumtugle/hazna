package io.github.shumtugle.hazna;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Rules, solver and dealer. Pure Java: no platform classes, so the whole
 * engine runs and is checked on a desktop JVM.
 *
 * A sack is an int array of stone kinds, bottom to top, exactly as long as
 * the number of stones in it. caps[i] is its capacity. There are exactly as
 * many stones of each kind as there are places in that kind's sack; the
 * chest holds one place more than the largest sack, so it can never seal.
 *
 * Two independent switches, never to be confused:
 *  - sealsFinal: the game's strict rules. A full uniform sack is closed for
 *    good. Under forgiving rules a seal can be torn off.
 *  - searchMode: the solver's own restraint. It refuses to carry a whole
 *    sack into an empty one of the same size, a move that changes nothing.
 *    The player is never restrained this way.
 */
final class Engine {

    static final int MAX_KINDS = 12;

    private Engine() {}

    /* ---------- rules ---------- */

    static int topKind(int[] b) {
        return b.length == 0 ? -1 : b[b.length - 1];
    }

    static int topCount(int[] b) {
        if (b.length == 0) return 0;
        int c = b[b.length - 1], n = 1;
        for (int i = b.length - 2; i >= 0 && b[i] == c; i--) n++;
        return n;
    }

    static boolean uniform(int[] b) {
        for (int x : b) if (x != b[0]) return false;
        return true;
    }

    /** Full and of one kind. Under forgiving rules the player may still open it. */
    static boolean isSealed(int[] b, int cap) {
        return b.length > 0 && b.length == cap && uniform(b);
    }

    /** How many stones a pour from i to j would carry; zero when it is not allowed. */
    static int canPour(int[][] s, int[] caps, int i, int j, boolean searchMode, boolean sealsFinal) {
        if (i == j) return 0;
        int[] src = s[i], dst = s[j];
        if (src.length == 0) return 0;
        if (sealsFinal && isSealed(src, caps[i])) return 0;
        int free = caps[j] - dst.length;
        if (free <= 0) return 0;
        int kind = topKind(src), count = topCount(src);
        if (dst.length > 0 && topKind(dst) != kind) return 0;
        if (searchMode && dst.length == 0 && count == src.length && caps[i] == caps[j]) return 0;
        return Math.min(count, free);
    }

    static void pour(int[][] s, int i, int j, int n) {
        int kind = topKind(s[i]);
        s[i] = Arrays.copyOf(s[i], s[i].length - n);
        int[] d = Arrays.copyOf(s[j], s[j].length + n);
        for (int k = s[j].length; k < d.length; k++) d[k] = kind;
        s[j] = d;
    }

    static int[][] copy(int[][] s) {
        int[][] c = new int[s.length][];
        for (int i = 0; i < s.length; i++) c[i] = s[i].clone();
        return c;
    }

    /**
     * The audit is over when every non-empty sack is full and of one kind,
     * and every kind has a sack of its own. The chest must end empty, and it
     * does by construction: no kind is large enough to fill it.
     */
    static boolean solved(int[][] s, int kinds, int[] caps) {
        boolean[] used = new boolean[MAX_KINDS];
        int n = 0;
        for (int i = 0; i < s.length; i++) {
            int[] b = s[i];
            if (b.length == 0) continue;
            if (b.length != caps[i] || !uniform(b)) return false;
            if (used[b[0]]) return false;
            used[b[0]] = true;
            n++;
        }
        return n == kinds;
    }

    /** Estimate of the work left: breaks inside sacks plus how widely each kind is scattered. */
    static int remaining(int[][] s) {
        int h = 0;
        int[] spread = new int[MAX_KINDS];
        for (int[] b : s) {
            if (b.length == 0) continue;
            int seen = 0;
            for (int k = 0; k < b.length; k++) {
                if (k > 0 && b[k] != b[k - 1]) h++;
                if ((seen & (1 << b[k])) == 0) {
                    seen |= 1 << b[k];
                    spread[b[k]]++;
                }
            }
        }
        for (int v : spread) if (v > 1) h += v - 1;
        return h;
    }

    /** Position key that ignores the order of sacks of the same capacity. */
    static String key(int[][] s, int[] caps) {
        String[] parts = new String[s.length];
        for (int i = 0; i < s.length; i++) {
            char[] c = new char[s[i].length + 1];
            c[0] = (char) ('A' + caps[i]);
            for (int k = 0; k < s[i].length; k++) c[k + 1] = (char) ('a' + s[i][k]);
            parts[i] = new String(c);
        }
        Arrays.sort(parts);
        StringBuilder sb = new StringBuilder();
        for (String p : parts) sb.append(p).append('|');
        return sb.toString();
    }

    /* ---------- solver ---------- */

    /** Lets a caller stop a search that is no longer wanted. */
    interface Cancel {
        boolean cancelled();
    }

    private static final class Node {
        final int[][] s;
        final int g, f;
        final long seq;
        final Node parent;
        final int from, to;

        Node(int[][] s, int g, int f, long seq, Node parent, int from, int to) {
            this.s = s; this.g = g; this.f = f; this.seq = seq;
            this.parent = parent; this.from = from; this.to = to;
        }
    }

    /**
     * Priority search: positions closer to order are opened first, with the
     * estimate weighted twice the moves already made.
     *
     * Returns the moves as {from, to} pairs, an empty list when the position is
     * already solved, or null when nothing was found within the node budget or
     * the deadline. Proving there is no way out means exhausting the space, so
     * the deadline is what keeps a dead end from freezing the game.
     */
    static List<int[]> solve(int[][] start, int[] caps, int kinds, int[] totals,
                             boolean sealsFinal, int nodeLimit, long msLimit, Cancel cancel) {
        long deadline = System.currentTimeMillis() + msLimit;
        PriorityQueue<Node> open = new PriorityQueue<>(1024, new Comparator<Node>() {
            @Override
            public int compare(Node a, Node b) {
                if (a.f != b.f) return a.f < b.f ? -1 : 1;
                return Long.compare(a.seq, b.seq);
            }
        });
        HashSet<String> seen = new HashSet<>();
        int[][] s0 = copy(start);
        seen.add(key(s0, caps));
        long seq = 0;
        open.add(new Node(s0, 0, remaining(s0) * 2, seq++, null, -1, -1));
        int nodes = 0;
        int n = s0.length;
        int[] mi = new int[n * n], mj = new int[n * n], mc = new int[n * n];
        boolean[] merge = new boolean[n * n];

        while (!open.isEmpty()) {
            Node cur = open.poll();
            if (++nodes > nodeLimit) return null;
            if ((nodes & 1023) == 0) {
                if (System.currentTimeMillis() > deadline) return null;
                if (cancel != null && cancel.cancelled()) return null;
            }
            int[][] b = cur.s;
            if (solved(b, kinds, caps)) {
                ArrayList<int[]> path = new ArrayList<>();
                for (Node x = cur; x.from >= 0; x = x.parent) path.add(new int[]{x.from, x.to});
                java.util.Collections.reverse(path);
                return path;
            }
            int count = 0, merges = 0;
            for (int i = 0; i < n; i++) {
                if (b[i].length == 0) continue;
                int kind = topKind(b[i]), group = topCount(b[i]);
                for (int j = 0; j < n; j++) {
                    int c = canPour(b, caps, i, j, true, sealsFinal);
                    if (c == 0) continue;
                    // Under strict rules a move that seals a sack on fewer than all
                    // stones of its kind is a trap: the sack can never be opened
                    // again, so no win lies beyond it.
                    if (sealsFinal && b[j].length + c == caps[j] && uniformOf(b[j], kind)
                            && totals != null && b[j].length + c != totals[kind]) continue;
                    mi[count] = i; mj[count] = j; mc[count] = c;
                    merge[count] = b[j].length > 0 && c == group;
                    if (merge[count]) merges++;
                    count++;
                }
            }
            // Under forgiving rules, when a handful can join its own kind whole,
            // only such moves are tried. Under strict rules that shortcut can cut
            // off the only path, so every move is tried there.
            boolean onlyMerges = !sealsFinal && merges > 0;
            for (int m = 0; m < count; m++) {
                if (onlyMerges && !merge[m]) continue;
                int[][] nb = copy(b);
                pour(nb, mi[m], mj[m], mc[m]);
                String k = key(nb, caps);
                if (!seen.add(k)) continue;
                open.add(new Node(nb, cur.g + 1, cur.g + 1 + remaining(nb) * 2, seq++, cur, mi[m], mj[m]));
            }
        }
        return null;
    }

    /** True when every stone in b is of the given kind (an empty sack counts). */
    static boolean uniformOf(int[] b, int kind) {
        for (int x : b) if (x != kind) return false;
        return true;
    }

    /* ---------- dealer ---------- */

    /** One audit as dealt: capacities, which sacks are chests, the stones, the kinds. */
    static final class Deal {
        final int[] caps;
        final boolean[] chest;
        final int[][] sacks;
        final int kinds;
        final int[] totals;

        Deal(int[] caps, boolean[] chest, int[][] sacks, int kinds, int[] totals) {
            this.caps = caps; this.chest = chest; this.sacks = sacks;
            this.kinds = kinds; this.totals = totals;
        }
    }

    /**
     * Difficulty step. Rises once every three audits and stops at the ceiling
     * of what the screen can hold. After that clamp, not before it, every
     * twentieth audit is one step easier and every thirtieth two: taken from
     * the raw curve, the breathers would vanish once it outgrew the ceiling.
     */
    static int difficulty(int n, int ceiling) {
        int base = n <= 1 ? 1 : 2 + (n - 2) / 3;
        int step = Math.min(ceiling, base);
        if (n % 30 == 0) step -= 2;
        else if (n % 20 == 0) step -= 1;
        return Math.max(1, step);
    }

    /** How many kinds fit: a short screen gets a poorer treasury rather than tiny sacks. */
    static int roof(float screenHeightDp) {
        return screenHeightDp >= 780 ? MAX_KINDS : (screenHeightDp >= 660 ? 10 : 8);
    }

    static int rnd(Random r, int lo, int hi) {
        return lo + r.nextInt(hi - lo + 1);
    }

    static void shuffle(Random r, int[] a) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int t = a[i]; a[i] = a[j]; a[j] = t;
        }
    }

    /**
     * One reverse move: lift k stones of one kind from the top of a sack, so
     * that the forward move would bring back exactly those (either k is below
     * the group, or the sack empties), and drop them where another kind lies
     * on top. Under strict rules the forward move must not have to reach into
     * a sealed sack, so a drop that leaves a sack full and uniform is refused.
     */
    static boolean unmove(Random r, int[][] s, int[] caps) {
        int nonEmpty = 0;
        for (int[] b : s) if (b.length > 0) nonEmpty++;
        int[] from = new int[nonEmpty];
        for (int i = 0, k = 0; i < s.length; i++) if (s[i].length > 0) from[k++] = i;
        shuffle(r, from);
        int[] safe = new int[s.length];
        for (int i : from) {
            int kind = topKind(s[i]), group = topCount(s[i]);
            int maxK = group == s[i].length ? group : group - 1;
            if (maxK < 1) continue;
            int k = rnd(r, 1, maxK);
            int ns = 0;
            for (int j = 0; j < s.length; j++) {
                if (j == i) continue;
                if (caps[j] - s[j].length < k) continue;
                if (s[j].length > 0 && topKind(s[j]) == kind) continue;
                if (s[j].length + k == caps[j] && uniformOf(s[j], kind)) continue;
                safe[ns++] = j;
            }
            if (ns == 0) continue;
            int j = safe[rnd(r, 0, ns - 1)];
            pour(s, i, j, k);
            return true;
        }
        return false;
    }

    /**
     * Deals audit n. A sealed treasury is built first and taken apart with
     * reverse moves, every one of them invertible, so a way back always
     * exists and the dealer needs no solver to vet itself.
     */
    static Deal deal(int n, float screenHeightDp, Random r) {
        int roof = roof(screenHeightDp);
        int ceiling = roof - 3;
        int step = difficulty(n, ceiling);
        int base = Math.min(step + 3, roof);
        int[] jitter = {-1, 0, 0, 1};
        int kinds = Math.max(3, Math.min(roof, base + jitter[rnd(r, 0, 3)]));
        int lo = 3, hi = 4;
        if (step >= 3) hi = 5;
        if (step >= 5) hi = 6;
        if (step >= 7) lo = 4;
        int[] counts = new int[kinds];
        for (int i = 0; i < kinds; i++) counts[i] = rnd(r, lo, hi);
        shuffle(r, counts);
        int chestCap = 0;
        for (int c : counts) chestCap = Math.max(chestCap, c);
        chestCap++;
        int chests = kinds <= 6 ? 1 : 2;

        int total = kinds + chests;
        int[] caps = new int[total];
        boolean[] chest = new boolean[total];
        System.arraycopy(counts, 0, caps, 0, kinds);
        for (int i = kinds; i < total; i++) { caps[i] = chestCap; chest[i] = true; }

        int[][] best = null;
        int bestMess = -1;
        for (int attempt = 0; attempt < 12; attempt++) {
            int[][] s = new int[total][];
            for (int i = 0; i < total; i++) {
                s[i] = new int[i < kinds ? counts[i] : 0];
                Arrays.fill(s[i], i);
            }
            int steps = kinds * 14 + 30;
            for (int t = 0; t < steps; t++) if (!unmove(r, s, caps)) break;
            int sealedAtStart = 0;
            for (int i = 0; i < total; i++) if (isSealed(s[i], caps[i])) sealedAtStart++;
            int mess = remaining(s) - sealedAtStart * 4;
            if (mess > bestMess) { bestMess = mess; best = s; }
            if (sealedAtStart == 0 && mess >= kinds + 4) { best = s; break; }
        }
        return new Deal(caps, chest, best, kinds, counts.clone());
    }
}
