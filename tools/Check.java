package io.github.shumtugle.hazna;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Checks run before every build, on a desktop JVM:
 *
 *  1. Dealer and solver: audits 1..30, five deals each, under both rule sets
 *     and three screen heights; every deal must be solved within the budget
 *     the Solve button gets.
 *  2. Dead ends: random strict games are played into a dead end; a dead
 *     position must stay dead after every move, and the bisection must land
 *     on the true boundary.
 *  3. A played game: random taps keep every stone accounted for, and undo
 *     returns to the start.
 *  4. Language modules: the template is current, and every module has every
 *     key in template order, with the English source unchanged above it.
 *
 *   java Check <project-root>               run the checks
 *   java Check <project-root> --template    rewrite the template from the dictionary
 */
public final class Check {

    private static int failures;

    private static void fail(String s) {
        failures++;
        System.out.println("  FAIL " + s);
    }

    public static void main(String[] args) throws Exception {
        File root = new File(args.length > 0 ? args[0] : ".");
        if (args.length > 1 && args[1].equals("--template")) {
            Files.write(new File(root, "modules/template.txt").toPath(), Words.template().getBytes(StandardCharsets.UTF_8));
            System.out.println("template written");
            return;
        }
        deals();
        deadEnds();
        play();
        modules(new File(root, "modules"));
        if (failures > 0) {
            System.out.println("checks: " + failures + " failed");
            System.exit(1);
        }
        System.out.println("checks: all passed");
    }

    /* ---------- 1 ---------- */

    private static void deals() {
        Random r = new Random(1);
        int n = 0;
        long worst = 0;
        for (boolean strict : new boolean[]{true, false}) {
            for (float h : new float[]{600, 700, 820}) {
                for (int level = 1; level <= 30; level++) {
                    for (int d = 0; d < 5; d++) {
                        Engine.Deal deal = Engine.deal(level, h, r);
                        stonesOk(deal.sacks, deal.totals, "deal " + level);
                        long t = System.currentTimeMillis();
                        List<int[]> p = Engine.solve(deal.sacks, deal.caps, deal.kinds, deal.totals, strict, 60000, 900, null);
                        worst = Math.max(worst, System.currentTimeMillis() - t);
                        n++;
                        if (p == null) {
                            fail("unsolved: audit " + level + " height " + h + " strict " + strict);
                            continue;
                        }
                        int[][] s = Engine.copy(deal.sacks);
                        for (int[] m : p) {
                            int c = Engine.canPour(s, deal.caps, m[0], m[1], false, strict);
                            if (c == 0) { fail("illegal solver move at audit " + level); break; }
                            Engine.pour(s, m[0], m[1], c);
                        }
                        if (!Engine.solved(s, deal.kinds, deal.caps)) fail("solution does not solve audit " + level);
                    }
                }
            }
        }
        System.out.println("deals: " + n + " solved, slowest " + worst + " ms");
    }

    private static void stonesOk(int[][] s, int[] totals, String where) {
        int[] seen = new int[Engine.MAX_KINDS];
        for (int[] b : s) for (int x : b) seen[x]++;
        for (int k = 0; k < totals.length; k++) if (seen[k] != totals[k]) fail("stone count of kind " + k + " in " + where);
    }

    /* ---------- 2 ---------- */

    private static void deadEnds() {
        Random r = new Random(2);
        int games = 0, stuck = 0, moves = 0;
        for (int g = 0; g < 400 && stuck < 30; g++) {
            Game game = new Game(10 + r.nextInt(40), true, 820, r);
            games++;
            int[][][] past = null;
            // careless random play until no way is left
            for (int step = 0; step < 200; step++) {
                List<int[]> legal = legal(game);
                if (legal.isEmpty()) break;
                int[] m = legal.get(r.nextInt(legal.size()));
                game.play(m[0], m[1]);
                if (game.solved()) break;
                if (step % 5 == 4 && solve(game.sacks, game) == null) {
                    past = game.positions();
                    break;
                }
            }
            if (past == null) continue;
            stuck++;
            int[][] dead = past[past.length - 1];
            // a dead position stays dead after any move
            for (int[] m : legal(game)) {
                int[][] s = Engine.copy(dead);
                Engine.pour(s, m[0], m[1], Engine.canPour(s, game.caps, m[0], m[1], false, true));
                moves++;
                if (solve(s, game) != null) fail("a move out of a dead end found a way");
            }
            Game.Way w = game.lastSolvable(past, null);
            if (w == null) { fail("no solvable position in the history"); continue; }
            if (solve(past[w.index], game) == null) fail("boundary position is not solvable");
            if (w.index + 1 < past.length && solve(past[w.index + 1], game) != null) fail("position after the boundary is solvable");
        }
        System.out.println("dead ends: " + stuck + " of " + games + " games, " + moves + " moves out of them tried");
        if (stuck < 10) fail("too few dead ends reached to check: " + stuck);
    }

    private static List<int[]> solve(int[][] s, Game g) {
        return Engine.solve(s, g.caps, g.kinds, g.totals, true, 400000, 20000, null);
    }

    private static List<int[]> legal(Game g) {
        ArrayList<int[]> out = new ArrayList<>();
        for (int i = 0; i < g.sacks.length; i++)
            for (int j = 0; j < g.sacks.length; j++)
                if (Engine.canPour(g.sacks, g.caps, i, j, false, true) > 0) out.add(new int[]{i, j});
        return out;
    }

    /* ---------- 3 ---------- */

    private static void play() {
        Random r = new Random(3);
        int taps = 0, pours = 0, wins = 0;
        for (int g = 0; g < 40; g++) {
            Game game = new Game(1 + r.nextInt(60), r.nextBoolean(), 760, r);
            int[][] start = Engine.copy(game.sacks);
            for (int t = 0; t < 300; t++) {
                Game.Tap tap = game.tap(r.nextInt(game.sacks.length));
                taps++;
                if (tap == Game.Tap.POUR || tap == Game.Tap.WIN) pours++;
                stonesOk(game.sacks, game.totals, "play");
                if (tap == Game.Tap.WIN) { wins++; break; }
            }
            while (game.undo()) taps++;
            if (!Arrays.deepEquals(start, game.sacks) || game.moves != 0) fail("undo did not return to the start");
        }
        // and the solver's own path, played through taps, wins
        Game game = new Game(25, true, 820, r);
        List<int[]> p = game.solveFromHere(60000, 5000, null);
        Game.Tap last = Game.Tap.NOTHING;
        for (int[] m : p) {
            game.tap(m[0]);
            last = game.tap(m[1]);
        }
        if (last != Game.Tap.WIN) fail("solver path played by taps did not win");
        System.out.println("play: " + taps + " taps, " + pours + " pours, " + wins + " random wins");
    }

    /* ---------- 4 ---------- */

    private static void modules(File dir) throws Exception {
        String template = Words.template();
        File tf = new File(dir, "template.txt");
        String onDisk = tf.exists() ? new String(Files.readAllBytes(tf.toPath()), StandardCharsets.UTF_8) : "";
        if (!onDisk.equals(template)) fail("modules/template.txt is out of date; write it from Words.template()");

        List<String> keys = new ArrayList<>();
        for (String k : Words.keys()) keys.add(k);
        File[] fs = dir.listFiles();
        if (fs == null) fs = new File[0];
        Arrays.sort(fs);
        int n = 0;
        for (File f : fs) {
            String name = f.getName();
            if (!name.endsWith(".txt") || name.equals("template.txt")) continue;
            n++;
            String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            Words w;
            try {
                w = Words.parse(text);
            } catch (IllegalArgumentException e) {
                fail(name + ": " + e.getMessage());
                continue;
            }
            if (!name.equals(w.code + ".txt")) fail(name + ": file name does not match code " + w.code);
            if (text.indexOf('\r') >= 0) fail(name + ": CR line endings");
            String[] lines = text.split("\n", -1);
            int k = 0;
            String comment = null;
            for (String line : lines) {
                if (line.startsWith("# ")) { comment = line.substring(2); continue; }
                int tab = line.indexOf('\t');
                if (tab < 0) continue;
                String key = line.substring(0, tab);
                String value = line.substring(tab + 1);
                if (k >= keys.size() || !keys.get(k).equals(key)) {
                    fail(name + ": key " + key + " out of place (expected " + (k < keys.size() ? keys.get(k) : "end") + ")");
                    break;
                }
                if (comment == null || !comment.equals(Words.source(key))) fail(name + ": English source above " + key + " drifted");
                if (value.indexOf('\t') >= 0) fail(name + ": tab inside " + key);
                if (!value.equals(value.trim())) fail(name + ": spaces around " + key);
                if (key.startsWith("doc_") && count(value, '*') % 2 != 0) fail(name + ": unpaired asterisk in " + key);
                comment = null;
                k++;
            }
            if (k != keys.size()) fail(name + ": " + k + " keys, template has " + keys.size());
            System.out.println("module " + w.code + " (" + w.name + "): " + w.filled() + "/" + Words.total() + " filled");
        }
        System.out.println("modules: " + n + " checked against " + keys.size() + " keys");
    }

    private static int count(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
        return n;
    }
}
