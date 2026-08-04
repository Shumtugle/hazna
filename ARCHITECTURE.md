Architecture — Hazna
Notes on how the project is put together, for anyone reading the source or forking it. The game itself is described in README; this file covers the parts that are not obvious from the code.
Layout
Code
Generated, never edited directly: play.html and app/assets/. Edit src/*, then run sh src/build.sh.
Concatenation order in src/build.sh matters: i18n_a → i18n_b → desc → doc → padishah_ru → padishah_en → app → engine → ui. The language files must come before desc.js and doc.js, because those append to an already existing L object.
The engine
Model. A sack is an array, bottom to top. caps[i] is its capacity, chest[i] marks the chest. There are exactly as many stones of each kind as there are places in its sack. The chest holds one more place than the largest sack, which is what makes it impossible to seal.
Win condition. Every non-empty sack full and uniform, every kind placed, the chest empty. There is no auto-complete: the last stone is placed by the player.
Generation. A sealed treasury is built first, then taken apart with reverse moves. Every reverse move is invertible by construction — lift k stones of one kind from the top such that the forward move would return exactly those (either k is smaller than the group, or the sack empties), and drop them somewhere with a different kind on top. A way back therefore always exists, the generator needs no solver to vet itself, and a deal costs about 3 ms.
Under strict rules the way back must also avoid seals, so the generator refuses any reverse move that would leave a sack full and uniform — otherwise the forward path would have to reach into a sealed sack.
Solver. Priority search with an estimate of the work remaining (remaining: gaps inside sacks plus how far each kind is scattered). Budget 40–60 thousand nodes; the worst measured call is a fraction of a second.
Under forgiving rules a dominating rule applies: when a handful can be poured whole onto its own kind, only such moves are considered. Under strict rules that shortcut loses completeness — it can cut off the only path — so the search prunes traps instead: a move that seals a sack on less than the whole kind can never lead to a win. That prune is both sound and sharper, and the strict search runs about four times faster than the forgiving one.
Search is capped in time. Proving a position has no way out is the expensive case, since the search must exhaust the space. Every call carries a deadline, and the bisection runs one probe per frame, so the screen keeps breathing instead of freezing.
Dead ends. A position with no way out stays that way after any move — checked across forty random games, no violations. The history therefore splits cleanly in two, and lastSolvable() finds the boundary by bisection: six probes on a fifty-move game instead of fifty.
Strict and forgiving. canPour(..., strict) takes the rule set as an argument. Do not assume one of them is the real one and the other a variant — the solver's pruning differs between them, and mixing the two silently produces wrong hints.
Difficulty. Rises one step every three audits until it meets the ceiling of what the screen can hold. Every twentieth audit is one step easier, every thirtieth two. The rebate is applied after the ceiling clamp, not before — otherwise the breathers vanish the moment the raw curve outgrows the ceiling.
Sound is synthesised at runtime through the Web Audio API; no audio files ship with the app. Progress — audit number, chests sealed, total moves — lives in localStorage. The board itself is never saved.
Dead ends already explored
Approaches that were tried and abandoned. Kept here so they are not rediscovered.
Solver, first version: DFS storing the route in every stack node — ran out of heap on three chests.
Solver, second version: DFS with backtracking, a depth limit and a visited set — returned 260-move solutions where 30 exist, and took thirty seconds. Visited states and a depth limit break each other. Do not go back to DFS.
Generation by brute force with solver validation: up to 35 seconds at twelve kinds. Replaced by reversible teardown.
resources.arsc must be stored uncompressed or Android 11+ refuses to install the package. apktool compresses it, so build-apk.sh repacks the archive by hand.
setDomStorageEnabled(true) is required in the WebView; without it progress silently fails to save.
Outbound links need Links.smali (a WebViewClient), or a link opens inside the WebView and lands on a white screen — the game holds no internet permission.
Referencing a missing element ($("aboutText") after the markup was rebuilt) killed the whole startup: the splash never left, the amulet stopped responding. Every id used in code must exist in the markup.
Measuring the board's height from the board itself — it stretches to fit its contents, so the calculation inflated and the sacks no longer fit. Measure from window.innerHeight minus the header and the button bar.
Considered and rejected
A surrender mode with no undo, where a dead end ends the run.
A false stone that looks like a ruby but is only accepted by its own sack.
A cap on the number of pours.
Captions under the paintings shown every thirtieth audit — left untitled deliberately.
Checks before a build
Every id referenced in code exists in the markup.
Syntax: node --check on the extracted script.
Language completeness: all fourteen carry the same keys as en, and four paragraphs of desc each.
Generator and solver: 30 levels × 5 deals, zero unsolvable.
tools/smoke.js covers part of this. The rest still runs through throwaway scripts and should become permanent tools.
Build and signing
Sh
The signing key and its password stay offline and are never committed.
The signed release in the repository root is named after its version. When the version changes, update the download links in index.html and README.md to match.
