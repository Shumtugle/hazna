package io.github.shumtugle.hazna;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * One audit in progress: the deal, the hand, the history and the seals the
 * player has torn off. Pure Java, so a whole game can be played on a desktop
 * JVM by the checks.
 */
final class Game {

    /** What a tap did, so the screen knows which sound to make and what to animate. */
    enum Tap { NOTHING, LIFT, DROP, UNSEAL, POUR, WIN }

    private static final class Snap {
        final int[][] sacks;
        final int moves;
        final int[] opened;

        Snap(int[][] sacks, int moves, int[] opened) {
            this.sacks = sacks; this.moves = moves; this.opened = opened;
        }
    }

    final int level;
    final boolean sealsFinal;
    final int[] caps;
    final boolean[] chest;
    final int kinds;
    final int[] totals;
    /** Sack indexes in drawing order: shelf sacks largest first, then the chests. */
    final int[] order;
    final int[] chestOrder;

    int[][] sacks;
    int moves;
    int selected = -1;
    /** Last pour, for the flying stone: from, to, kind. */
    int lastFrom = -1, lastTo = -1, lastKind = -1;
    private final ArrayList<Snap> history = new ArrayList<>();
    private final HashSet<Integer> opened = new HashSet<>();

    Game(int level, boolean sealsFinal, float screenHeightDp, Random r) {
        this(level, sealsFinal, Engine.deal(level, screenHeightDp, r));
    }

    Game(int level, boolean sealsFinal, Engine.Deal d) {
        this.level = level;
        this.sealsFinal = sealsFinal;
        caps = d.caps;
        chest = d.chest;
        kinds = d.kinds;
        totals = d.totals;
        sacks = d.sacks;
        int shelf = 0;
        for (boolean c : chest) if (!c) shelf++;
        Integer[] idx = new Integer[shelf];
        chestOrder = new int[caps.length - shelf];
        for (int i = 0, a = 0, b = 0; i < caps.length; i++) {
            if (chest[i]) chestOrder[b++] = i; else idx[a++] = i;
        }
        java.util.Arrays.sort(idx, new java.util.Comparator<Integer>() {
            @Override
            public int compare(Integer x, Integer y) {
                return caps[y] - caps[x];
            }
        });
        order = new int[shelf];
        for (int i = 0; i < shelf; i++) order[i] = idx[i];
    }

    /** Shown closed: full and uniform, and not torn open by the player. */
    boolean shut(int i) {
        return Engine.isSealed(sacks[i], caps[i]) && (sealsFinal || !opened.contains(i));
    }

    boolean solved() {
        return Engine.solved(sacks, kinds, caps);
    }

    int historySize() {
        return history.size();
    }

    Tap tap(int i) {
        if (shut(i)) {
            if (sealsFinal) return Tap.NOTHING;
            opened.add(i);
            selected = i;
            return Tap.UNSEAL;
        }
        if (selected == -1) {
            if (sacks[i].length == 0) return Tap.NOTHING;
            selected = i;
            return Tap.LIFT;
        }
        if (selected == i) {
            selected = -1;
            return Tap.DROP;
        }
        int n = Engine.canPour(sacks, caps, selected, i, false, sealsFinal);
        if (n == 0) {
            selected = sacks[i].length > 0 ? i : -1;
            return selected == -1 ? Tap.DROP : Tap.LIFT;
        }
        return move(selected, i, n) ? Tap.WIN : Tap.POUR;
    }

    /** Plays a solver move. Returns false when the move is no longer legal. */
    boolean play(int from, int to) {
        int n = Engine.canPour(sacks, caps, from, to, false, sealsFinal);
        if (n == 0) return false;
        move(from, to, n);
        return true;
    }

    private boolean move(int i, int j, int n) {
        lastFrom = i; lastTo = j; lastKind = Engine.topKind(sacks[i]);
        history.add(new Snap(Engine.copy(sacks), moves, openedArray()));
        opened.remove(i);
        opened.remove(j);
        Engine.pour(sacks, i, j, n);
        moves++;
        selected = -1;
        return solved();
    }

    boolean undo() {
        if (history.isEmpty()) return false;
        restore(history.remove(history.size() - 1));
        return true;
    }

    private void restore(Snap h) {
        sacks = h.sacks;
        moves = h.moves;
        selected = -1;
        opened.clear();
        for (int x : h.opened) opened.add(x);
    }

    private int[] openedArray() {
        int[] a = new int[opened.size()];
        int k = 0;
        for (int x : opened) a[k++] = x;
        return a;
    }

    List<int[]> solveFromHere(int nodeLimit, long ms, Engine.Cancel cancel) {
        return Engine.solve(sacks, caps, kinds, totals, sealsFinal, nodeLimit, ms, cancel);
    }

    /** Every position of this audit so far, oldest first, the current one last; safe to hand to another thread. */
    int[][][] positions() {
        int[][][] p = new int[history.size() + 1][][];
        for (int k = 0; k < history.size(); k++) p[k] = Engine.copy(history.get(k).sacks);
        p[history.size()] = Engine.copy(sacks);
        return p;
    }

    private List<int[]> probe(int[][] position, Engine.Cancel cancel) {
        return Engine.solve(position, caps, kinds, totals, sealsFinal, 60000, 300, cancel);
    }

    /** Result of the search for a way out of a dead end. */
    static final class Way {
        final int index;
        final List<int[]> path;

        Way(int index, List<int[]> path) {
            this.index = index; this.path = path;
        }
    }

    /**
     * A position with no way out stays that way after any move, so the
     * history splits cleanly in two and the boundary is found by bisection:
     * a handful of probes instead of one per move.
     */
    Way lastSolvable(int[][][] past, Engine.Cancel cancel) {
        List<int[]> p0 = probe(past[0], cancel);
        if (p0 == null) return null;
        int lo = 0, hi = past.length - 1;
        Way best = new Way(0, p0);
        while (lo + 1 < hi) {
            if (cancel != null && cancel.cancelled()) return null;
            int mid = (lo + hi) >>> 1;
            List<int[]> p = probe(past[mid], cancel);
            if (p != null) { lo = mid; best = new Way(mid, p); } else hi = mid;
        }
        return best;
    }
}
