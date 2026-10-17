# Language modules

Hazna speaks English. Every other language is a module: one plain text file
that gives each word of the game in that language. The game carries no
translations of its own.

A module is taken in one of two ways:

- open the file with Hazna from a file manager or a download;
- Settings → Load from file.

Either way the game shows the language's name, how many words it fills, and
asks before adding it. A long press on a language in Settings removes it.

A word left empty stays English, so a half-finished module is already usable.

## The files here

- `template.txt` — every key with its English source and an empty value.
  This is the form to fill. It is written from the dictionary in the code,
  never by hand.
- `ru.txt` — complete, every key filled.
- `ar.txt de.txt es.txt fi.txt fr.txt he.txt kk.txt ky.txt sv.txt uk.txt ur.txt zh.txt`
  — the interface and the tale on the title card. Their about page and the
  padishah's remarks are still empty and show in English.

## Rules for a module

**Encoding and lines.** UTF-8, lines ending in LF. One value per line. No
tabs inside a value. No quotation marks around values.

**The first line stays exactly as it is:**

```
# a language for hazna
```

The game recognises a module by this line. Do not translate it.

**Two header lines** follow it:

```
module Suomi
code fi
```

`module` names the language in the language itself; it is what the
language button shows. `code` is the language's short tag. It decides the
file name, which language "Automatic" picks from the phone's settings, and
the direction of the screen: `ar`, `he`, `ur`, `fa` and a few others turn
it right to left.

**Each entry** is a comment with the English source, then the key, a tab,
and the value:

```
# Undo
undo	Kumoa
```

- Never change or translate a key.
- Keep every key, in the order of the template.
- Keep the comment lines as they are; they are the source, not decoration.
- A value may be left empty; the game shows the English then.

**Groups of keys.**

- Interface words come first. Most sit on buttons: keep them as short as the
  language allows.
- `tale_1` … `tale_4` — the four paragraphs on the title card. Not a
  translation of the English: the same tale, told the way the language
  would tell it.
- `doc_title`, `doc_…`, `head_…`, `src_…` — the about page. In `doc_`
  paragraphs a word between asterisks is set in bold: `*Strict* (default):`.
- `remark_001` … `remark_100` — the padishah's remarks, one for every
  tenth audit. Short, dry, in his voice.

**Questions.** Values whose source ends in a question mark are questions put
to the player; keep the question.

**Numbers.** Where the source has none, add none. Figures are always drawn
by the game.

## File names

`<code>.txt`, the code as in the `code` line: `fi.txt`, `sv.txt`, `he.txt`.

## Checking a module

The build checks every module here and stops when one is off:

- the first line is unchanged, and `module` and `code` are present;
- the file name matches the code;
- every key is present, in template order, and no other;
- the English comment above each key matches the source in the code;
- no value holds a tab or loose spaces at its ends;
- asterisks in `doc_` paragraphs come in pairs.
