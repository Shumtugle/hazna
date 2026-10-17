package io.github.shumtugle.hazna;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A language: the built-in English, or a module read from one plain text
 * file. Pure Java, so modules are checked on a desktop JVM.
 *
 * A module looks like this:
 *
 *   # a language for hazna
 *   # fill the empty lines and load the file back
 *
 *   module Suomi
 *   code fi
 *
 *   # Undo
 *   undo<TAB>Kumoa
 *
 * The first line is how a module is recognised and is never translated.
 * "module" names the language in the language itself, "code" is its short
 * tag. Every entry is a comment with the English source, then the key, a
 * tab and the value. An empty or missing value falls back to English, so a
 * half-finished module is already usable. Unknown keys are ignored.
 */
final class Words {

    static final String HEADER = "# a language for hazna";
    static final String ENGLISH = "en";

    private static final Map<String, String> BASE = new LinkedHashMap<>();
    private static final String[] RTL = {"ar", "he", "iw", "ur", "fa", "ps", "yi", "dv", "sd", "ug", "ckb"};

    static {
        for (String[] w : English.WORDS) BASE.put(w[0], w[1]);
    }

    final String name;
    final String code;
    private final Map<String, String> own;

    private Words(String name, String code, Map<String, String> own) {
        this.name = name;
        this.code = code;
        this.own = own;
    }

    static Words english() {
        return new Words("English", ENGLISH, new HashMap<String, String>());
    }

    /** The word for a key: the module's own when it has one, English otherwise. */
    String get(String key) {
        String v = own.get(key);
        if (v != null && !v.isEmpty()) return v;
        v = BASE.get(key);
        return v != null ? v : key;
    }

    boolean has(String key) {
        String v = own.get(key);
        return v != null && !v.isEmpty();
    }

    boolean rtl() {
        for (String c : RTL) if (c.equals(code)) return true;
        return false;
    }

    /** Keys this language fills, out of all the game knows. */
    int filled() {
        int n = 0;
        for (String k : BASE.keySet()) if (has(k)) n++;
        return n;
    }

    static int total() {
        return BASE.size();
    }

    static int remarks() {
        int n = 0;
        for (String k : BASE.keySet()) if (k.startsWith("remark_")) n++;
        return n;
    }

    static boolean looksLikeModule(String text) {
        return firstLine(text).equals(HEADER);
    }

    private static String firstLine(String text) {
        if (text.startsWith("﻿")) text = text.substring(1);
        int nl = text.indexOf('\n');
        String line = nl < 0 ? text : text.substring(0, nl);
        return line.trim();
    }

    /** Reads a module. Throws when the text is not one. */
    static Words parse(String text) {
        if (!looksLikeModule(text)) throw new IllegalArgumentException("not a module");
        String name = null, code = null;
        Map<String, String> own = new HashMap<>();
        for (String raw : text.split("\n")) {
            String line = raw.endsWith("\r") ? raw.substring(0, raw.length() - 1) : raw;
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("﻿")) continue;
            int tab = line.indexOf('\t');
            if (tab < 0) {
                if (line.startsWith("module ")) name = line.substring(7).trim();
                else if (line.startsWith("code ")) code = line.substring(5).trim().toLowerCase(java.util.Locale.ROOT);
                continue;
            }
            String key = line.substring(0, tab).trim();
            if (BASE.containsKey(key)) own.put(key, line.substring(tab + 1).trim());
        }
        if (name == null || name.isEmpty() || code == null || !code.matches("[a-z]{2,3}(-[a-z0-9]{2,8})?")) {
            throw new IllegalArgumentException("module or code line missing");
        }
        if (code.equals(ENGLISH)) throw new IllegalArgumentException("English is built in");
        return new Words(name, code, own);
    }

    /** The form a translator fills: every key with its English source and an empty value. */
    static String template() {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append('\n');
        sb.append("# fill the empty lines and load the file back\n\n");
        sb.append("module untitled\n");
        sb.append("code xx\n");
        for (Map.Entry<String, String> e : BASE.entrySet()) {
            sb.append('\n').append("# ").append(e.getValue()).append('\n');
            sb.append(e.getKey()).append('\t').append('\n');
        }
        return sb.toString();
    }

    /** Every key in template order, for the checks. */
    static Iterable<String> keys() {
        return BASE.keySet();
    }

    static String source(String key) {
        return BASE.get(key);
    }
}
