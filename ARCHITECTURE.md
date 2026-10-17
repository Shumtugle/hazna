# Architecture — Hazna

Notes on how the project is put together, for anyone reading the source or forking it. The game itself is described in [README](README.md); this file covers the parts that are not obvious from the code.

---

## Layout

Since 2.0 the game is native Java. Twelve classes in one package, no libraries.

**Pure Java, checked on a desktop:** `Engine`, `Game`, `Words`, `English`, `Painter`, `Pen`. They use no platform classes, so `tools/Check.java` plays them headless before every build and `tools/Preview.java` paints boards to images.

**Platform side:** `MainActivity` (the one screen), `BoardView` (clock and touch), `CanvasPen`, `Store`, `Sound`, `Ui`.

The page (`index.html`) and the frozen browser version (`play.html`, 1.2.1) live in the root with the pictures they share with the APK; `build.sh` copies the pictures into the package.

---

## The engine

**Model.** A sack is an int array, bottom to top, exactly as long as its contents. `caps[i]` is its capacity, `chest[i]` marks the chest. There are exactly as many stones of each kind as there are places in its sack. The chest holds one more place than the largest sack, which is what makes it impossible to seal.

**Win condition.** Every non-empty sack full and uniform, every kind placed, the chest empty. There is no auto-complete: the last stone is placed by the player.

**Two switches with similar names.** `sealsFinal` is the game's strict rule set: a full uniform sack is closed for good. `searchMode` is the solver's own restraint: it never carries a whole sack into an empty one of the same size, a move that changes nothing. The player is never restrained that way. They were one global and one argument in 1.x, both called "strict"; keep them apart.

**Generation.** A sealed treasury is built first, then taken apart with reverse moves. Every reverse move is invertible by construction — lift `k` stones of one kind from the top such that the forward move would return exactly those (either `k` is smaller than the group, or the sack empties), and drop them somewhere with a different kind on top. A way back therefore always exists, the dealer needs no solver, and a deal costs a few milliseconds. Under strict rules the dealer also refuses any reverse move that would leave a sack full and uniform — otherwise the forward path would have to reach into a sealed sack.

**Solver.** Priority search, `f = g + 2·remaining`, where `remaining` counts breaks inside sacks plus how widely each kind is scattered. Ties go to the older node. Budget 40–60 thousand nodes and a deadline; the worst call measured on a desktop is about a tenth of a second. Under forgiving rules a dominating rule applies: when a handful can be poured whole onto its own kind, only such moves are considered. Under strict rules that shortcut loses completeness, so the search prunes traps instead: a move that seals a sack on fewer than all stones of its kind can never lead to a win.

**Search runs off the screen's thread.** Hint and Solve hand a copy of the board — and, for Solve, a copy of the whole history — to a single worker thread. Every search carries a ticket; any tap on Stop, a new deal or a restart bumps the ticket, and both the search and its answer are dropped. The history is copied before the hand-off because Undo may change it while the search runs.

**Dead ends.** A position with no way out stays that way after any move. The history therefore splits cleanly in two, and `lastSolvable()` finds the boundary by bisection. `Check` plays careless games into thirty dead ends and verifies both halves of that claim every build.

**Difficulty.** Rises one step every three audits until it meets the ceiling of what the screen can hold. Every twentieth audit is one step easier, every thirtieth two. The rebate is applied *after* the ceiling clamp, not before — otherwise the breathers vanish the moment the raw curve outgrows the ceiling. The screen height that sets the ceiling is the window between the system bars, in density-independent units, as in 1.x.

**Cross-checked against 1.x.** The Java engine was run on 360 deals made by the 1.x engine: every one solved, every move legal under the 1.x rules, the same average solution length.

---

## The board

`Painter` lays out and paints in density-independent units through `Pen`, a dozen drawing calls. On the phone `CanvasPen` scales them to pixels; on a desktop `Preview` implements the same calls over an image. The look is the 1.x stylesheet translated: cuff and drawstring, translucent mouth with leather walls, three stones a layer, a flap that slides down and a wax stamp when a sack closes.

**Layout** follows the 1.x fit: try three to six sacks a row, pick the arrangement with the largest stones, cap the stone at 46 units. Room for a lifted sack is kept above the top row; without it the sack lifts off the board.

**Animation** lives in `BoardView`: a lift with a slight overshoot, the flap and the stamp, a stone flying in an arc. The clock is the frame time; the view stops asking for frames when nothing moves.

---

## Languages

English is the code (`English.java`, in template order). Every other language is a module file: a recognition line, `module` and `code` lines, then for each key a comment with the English source and `key<TAB>value`. Empty values fall back to English. The rules for translators are in `modules/README.md`.

- `Words.template()` writes the form to fill; `Check --template` saves it. The template is never edited by hand, and the build fails when it is stale.
- Installed modules are copies in the app's own files. "Automatic" picks the module whose code matches the phone's language, or English.
- The direction of the screen follows the module's code.
- The about page was in two languages and the remarks in two in 1.x; they stay so. Twelve modules leave those keys empty.

---

## Progress from 1.x

1.x kept progress as one JSON entry in the web view's local storage. On the first start of 2.0, only if the web view's data directory exists, a hidden web view is attached to the window, opens a blank page at the address 1.x used (so the storage origin is the same), reads that entry and hands it to the preferences. It runs once, has a four-second deadline, and the title card waits for it.

---

## Dead ends already explored

Approaches that were tried and abandoned. Kept here so they are not rediscovered.

- **Solver, first version:** depth-first search storing the route in every stack node — ran out of heap on three chests.
- **Solver, second version:** depth-first with backtracking, a depth limit and a visited set — returned 260-move solutions where 30 exist, and took thirty seconds. Visited states and a depth limit break each other. Do not go back to depth-first search.
- **Generation by brute force with solver validation:** up to 35 seconds at twelve kinds. Replaced by reversible teardown.
- **Measuring the board's height from the board itself** — it stretches to fit its contents, so the calculation inflated and the sacks no longer fit. Measure from the space given to the board.
- **Lambdas and method references** — the dexer in use cannot translate dynamic call sites, and the app would die on the first one. `build.sh` refuses to package if any are present.
- **The resource table** must be stored uncompressed or newer systems refuse to install the package; `build.sh` packages it with compression off for that extension.
- **One password file for both keystore and key passwords** — the signer reads them as successive lines of the same file and fails at its end. Give it the keystore password only; the key password is the same.
- **Renaming the activity** would orphan launcher icons on update. It stays `MainActivity`.

## Considered and rejected

- A surrender mode with no undo, where a dead end ends the run.
- A false stone that looks like a ruby but is only accepted by its own sack.
- A cap on the number of pours.
- Captions under the paintings shown every thirtieth audit — left untitled deliberately.
- Shipping the modules inside the APK. They live on the project page and in the repository; the game holds only English.

---

## Checks before a build

`tools/Check.java`, run by `build.sh`:

1. 900 deals (audits 1–30, five deals, two rule sets, three screen heights) solved within the Solve budget, every solution replayed move by move.
2. Thirty dead ends: every move out of one stays dead, and the bisection lands on the true boundary.
3. Forty random games of three hundred taps keep every stone; undo returns to the start; a solver path played as taps wins.
4. The template is current; every module has every key in order, its English comments unchanged, no tabs, paired asterisks.

`build.sh` also refuses classes with dynamic call sites. Kept outside: `LC_ALL=C.UTF-8 grep -rnP '[\x{0400}-\x{04FF}]' src tools build.sh AndroidManifest.xml res` must find nothing — Cyrillic lives only in `modules/` and the frozen `play.html`.

---

## Build and signing

```sh
SDK=path/to/android-34.jar KEYSTORE=path/to/release.keystore KS_PASS_FILE=path/to/password.txt sh build.sh
```

The signing key and its password stay offline and are never committed. The signed release in the repository root is named after its version; when the version changes, update the manifest, the links in `index.html` and the line in `README.md`.
