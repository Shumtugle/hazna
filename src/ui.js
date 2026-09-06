const shelf = document.getElementById("shelf");
const chests = document.getElementById("chests");
const $ = id => document.getElementById(id);

function rowsFor(caps, perRow){
  const rows = [];
  for (let i=0;i<caps.length;i+=perRow) rows.push(caps.slice(i, i+perRow));
  return rows;
}

function fitSizes(){
  const ordered = state.order.map(i => state.caps[i]);
  const chestCaps = state.chestOrder.map(i => state.caps[i]);
  const maxChest = chestCaps.length ? Math.max.apply(null, chestCaps) : 0;
  const head = document.querySelector("header").offsetHeight;
  const foot = document.querySelector("footer").offsetHeight;
  const availH = window.innerHeight - head - foot - 18;
  const availW = Math.min(window.innerWidth, 520) - 14;
  const ROW_PAD = 24;

  let best = null;
  for (let pr = 3; pr <= 6; pr++){
    if (pr > ordered.length && pr > 3) break;
    const w = Math.floor(availW / pr) - 12;
    if (w < 40) continue;
    const rows = rowsFor(ordered, pr);
    const units = rows.reduce((sum, r) => sum + Math.max.apply(null, r), 0) + maxChest;
    const rowCount = rows.length + (chestCaps.length ? 1 : 0);
    let u = Math.floor((availH - rowCount * ROW_PAD) / Math.max(1, units));
    u = Math.min(u, Math.floor((w - 18) / 1.5), 46);
    if (!best || u > best.u) best = {u, w: Math.min(w, 92), perRow: pr};
  }
  if (!best) best = {u:15, w:44, perRow:4};
  best.u = Math.max(best.u, 14);
  document.documentElement.style.setProperty("--u", best.u+"px");
  document.documentElement.style.setProperty("--w", best.w+"px");
  shelf.style.maxWidth = (best.perRow * (best.w + 12)) + "px";
}

function gemEl(idx, cls){
  const g = document.createElement("span");
  g.className = "gem " + cls + " g" + GEMS[idx].g;
  g.style.setProperty("--c", GEMS[idx].c);
  return g;
}

function render(){
  shelf.innerHTML = ""; chests.innerHTML = "";
  const nowSealed = new Set();
  state.order.concat(state.chestOrder).forEach(idx => {
    const b = state.bots[idx], cap = state.caps[idx];
    const shut = isSealed(b, cap) && (hardMode || !opened.has(idx));
    if (shut) nowSealed.add(idx);

    const el = document.createElement("div");
    el.className = "sack" + (idx===selected ? " sel" : "") + (shut ? " sealed" : "");
    if (shut && !prevSealed.has(idx)) { el.classList.add("sealing"); beep("seal"); }
    el.dataset.i = idx;
    el.style.setProperty("--cap", cap);

    const cuff = document.createElement("div"); cuff.className = "cuff";
    const mouth = document.createElement("div"); mouth.className = "mouth";
    b.forEach(c => {
      const layer = document.createElement("div");
      layer.className = "layer";
      layer.appendChild(gemEl(c, "sml"));
      layer.appendChild(gemEl(c, "big"));
      layer.appendChild(gemEl(c, "sml"));
      mouth.appendChild(layer);
    });
    const seal = document.createElement("div"); seal.className = "seal";
    if (b.length){ const dot = document.createElement("i"); dot.style.setProperty("--sc", GEMS[b[0]].c); seal.appendChild(dot); }

    el.appendChild(cuff); el.appendChild(mouth); el.appendChild(seal);
    (state.chest[idx] ? chests : shelf).appendChild(el);
  });
  prevSealed = nowSealed;
  $("lvl").textContent = level;
  $("mv").textContent = moves;
  $("undo").disabled = autoTimer !== null || history.length === 0;
}

function flyGem(fromEl, toEl, gi){
  const a = fromEl.getBoundingClientRect(), b = toEl.getBoundingClientRect();
  const d = document.createElement("div");
  d.className = "fly g" + GEMS[gi].g;
  d.style.background = GEMS[gi].c;
  d.style.left = (a.left + a.width/2 - 10) + "px";
  d.style.top = (a.top - 2) + "px";
  document.body.appendChild(d);
  const dx = (b.left + b.width/2) - (a.left + a.width/2);
  const dy = (b.top - 2) - (a.top - 2);
  d.animate(
    [{transform:"translate(0,0) rotate(0deg)"},
     {transform:"translate("+(dx*0.5)+"px,"+(Math.min(dy,0)-32)+"px) rotate(160deg)", offset:.5},
     {transform:"translate("+dx+"px,"+dy+"px) rotate(320deg)", opacity:.3}],
    {duration:280, easing:"cubic-bezier(.4,.1,.6,1)"}
  ).onfinish = () => d.remove();
}

function sackEl(i){ return document.querySelector('.sack[data-i="'+i+'"]'); }

function doMove(i, j, n){
  const gi = state.bots[i][state.bots[i].length-1];
  const from = sackEl(i), to = sackEl(j);
  if (from && to) flyGem(from, to, gi);
  beep("drop");
  history.push({bots: state.bots.map(x=>x.slice()), moves, opened: Array.from(opened)});
  opened.delete(i); opened.delete(j);
  applyPour(state.bots, i, j, n);
  moves++; selected = -1;
  render();
  return solved(state.bots, state.colors, state.caps);
}

function tap(i){
  if (autoTimer) return;
  if (isSealed(state.bots[i], state.caps[i]) && (hardMode || !opened.has(i))){
    if (hardMode) return;
    opened.add(i);
    selected = i;
    beep("back");
    render();
    return;
  }
  if (selected === -1){
    if (!state.bots[i].length) return;
    selected = i; render(); return;
  }
  if (selected === i){ selected = -1; render(); return; }
  const n = canPour(state.bots, state.caps, selected, i, false);
  if (!n){ selected = state.bots[i].length ? i : -1; render(); return; }
  if (doMove(selected, i, n)) setTimeout(win, 620);
}

document.addEventListener("click", e => {
  const el = e.target.closest(".sack");
  if (el) tap(parseInt(el.dataset.i, 10));
});

function stopAuto(){
  if (autoTimer){ clearTimeout(autoTimer); autoTimer = null; }
  $("auto").textContent = T.solve;
  ["undo","reset","hint"].forEach(id => $(id).disabled = false);
  $("undo").disabled = history.length === 0;
}
function bagsAt(k){ return k < history.length ? history[k].bots : state.bots; }
function probe(k){ return solve(bagsAt(k).map(x=>x.slice()), state.caps, state.colors, 60000, state.totals, 300); }

function lastSolvable(done){
  const p0 = probe(0);
  if (!p0){ done(null); return; }
  let lo = 0, hi = history.length, best = {index:0, path:p0};
  const step = () => {
    if (lo + 1 >= hi){ autoTimer = null; done(best); return; }
    const mid = (lo + hi) >> 1;
    const p = probe(mid);
    if (p){ lo = mid; best = {index:mid, path:p}; } else hi = mid;
    autoTimer = setTimeout(step, 0);
  };
  autoTimer = setTimeout(step, 0);
}
function playPath(path){
  $("auto").textContent = T.stop;
  let k = 0;
  const step = () => {
    if (k >= path.length){ stopAuto(); return; }
    const [i,j] = path[k++];
    const n = canPour(state.bots, state.caps, i, j, false);
    if (!n){ stopAuto(); return; }
    if (doMove(i, j, n)){ autoTimer = null; stopAuto(); setTimeout(win, 620); return; }
    autoTimer = setTimeout(step, 430);
  };
  autoTimer = setTimeout(step, 140);
}
function rewindTo(target, then){
  $("auto").textContent = T.rewinding;
  const step = () => {
    if (history.length <= target){ autoTimer = null; then(); return; }
    const h = history.pop();
    state.bots = h.bots; moves = h.moves; selected = -1;
    opened = new Set(h.opened || []);
    prevSealed = new Set();
    state.bots.forEach((b,i)=>{ if (isSealed(b, state.caps[i]) && !opened.has(i)) prevSealed.add(i); });
    beep("back"); render();
    autoTimer = setTimeout(step, 260);
  };
  autoTimer = setTimeout(step, 140);
}

$("undo").onclick = () => {
  if (autoTimer) return;
  const h = history.pop();
  if (!h) return;
  state.bots = h.bots; moves = h.moves; selected = -1;
  opened = new Set(h.opened || []);
  prevSealed = new Set();
  state.bots.forEach((b,i)=>{ if (isSealed(b, state.caps[i]) && !opened.has(i)) prevSealed.add(i); });
  render();
};
$("reset").onclick = () => { stopAuto(); startLevel(level); };
$("hint").onclick = () => {
  if (autoTimer) return;
  const btn = $("hint");
  btn.textContent = T.thinking;
  const path = solve(state.bots.map(x=>x.slice()), state.caps, state.colors, 40000, state.totals, 700);
  if (!path || !path.length){
    btn.textContent = T.noWay;
    setTimeout(()=>btn.textContent = T.hint, 1800);
    return;
  }
  btn.textContent = T.hint;
  const [i,j] = path[0];
  sackEl(i).classList.add("sel");
  setTimeout(()=>{ const e=sackEl(j); if(e) e.classList.add("sel"); }, 220);
  setTimeout(render, 1100);
};
$("auto").onclick = () => {
  const btn = $("auto");
  if (autoTimer){ stopAuto(); return; }
  ["undo","reset","hint"].forEach(id => $(id).disabled = true);
  selected = -1;
  btn.textContent = T.thinking;

  autoTimer = setTimeout(() => {
    autoTimer = null;
    const path = solve(state.bots.map(x=>x.slice()), state.caps, state.colors, 60000, state.totals, 900);
    if (path && path.length){ playPath(path); return; }
    lastSolvable(back => {
      if (!back){
        btn.textContent = T.noWay;
        setTimeout(stopAuto, 1800);
        return;
      }
      rewindTo(back.index, () => playPath(back.path));
    });
  }, 60);
};

function startLevel(n){
  if (autoTimer){ clearTimeout(autoTimer); autoTimer = null; }
  level = n; moves = 0; selected = -1; history = []; prevSealed = new Set(); opened = new Set();
  state = buildLevel(n);
  state.order = state.caps.map((c,i)=>i).filter(i=>!state.chest[i])
                          .sort((a,b)=>state.caps[b]-state.caps[a]);
  state.chestOrder = state.caps.map((c,i)=>i).filter(i=>state.chest[i]);
  state.bots.forEach((b,i)=>{ if (isSealed(b, state.caps[i])) prevSealed.add(i); });
  $("veil").classList.remove("on");
  stopAuto();
  render(); fitSizes(); render();
}

function win(){
  const royal = (level % 10 === 0);
  $("veilTitle").textContent = royal ? T.royal : T.sealed;
  $("veilQuote").textContent = royal ? "\u00AB" + PADISHAH[(level/10 - 1) % PADISHAH.length] + "\u00BB" : "";
  $("veilQuote").style.display = royal ? "" : "none";
  $("veilCount").textContent = T.levelLbl + " " + level + " · " + T.movesLbl + " " + moves;
  $("next").textContent = T.next;
  $("veil").classList.add("on");
  save.level = level + 1;
  save.sealed = (save.sealed || 0) + 1;
  save.total = (save.total || 0) + moves;
  writeSave();
}
const ART_COUNT = 10;

let artDeck = [];
function nextArt(){
  if (!artDeck.length){
    const last = save.lastArt;
    for (let i=1;i<=ART_COUNT;i++) artDeck.push(i);
    for (let i=artDeck.length-1;i>0;i--){
      const j = Math.floor(Math.random()*(i+1));
      const t = artDeck[i]; artDeck[i] = artDeck[j]; artDeck[j] = t;
    }
    if (artDeck.length > 1 && artDeck[artDeck.length-1] === last){
      const t = artDeck[artDeck.length-1];
      artDeck[artDeck.length-1] = artDeck[0]; artDeck[0] = t;
    }
  }
  const n = artDeck.pop();
  save.lastArt = n; writeSave();
  return n;
}
function showArt(then){
  const n = nextArt();
  const box = $("artSplash"), img = $("artImg");
  let done = false;
  const finish = () => {
    if (done) return;
    done = true;
    box.classList.remove("on");
    box.onclick = null;
    then();
  };
  img.onerror = () => { img.onerror = null; finish(); };
  img.src = "art/" + (n < 10 ? "0" + n : n) + ".jpg";
  box.classList.add("on");
  box.onclick = finish;
  setTimeout(finish, 5000);
}
$("next").onclick = () => {
  const done = level;
  $("veil").classList.remove("on");
  if (done % 30 === 0) showArt(() => startLevel(done + 1));
  else startLevel(done + 1);
};
window.addEventListener("resize", () => { if (state){ fitSizes(); render(); } });

function applyLang(){
  resolveLang();
  document.documentElement.dir = RTL.indexOf(document.documentElement.lang) >= 0 ? "rtl" : "ltr";
  $("lvlLbl").textContent = T.levelLbl;
  $("mvLbl").textContent = T.movesLbl;
  $("undo").textContent = T.undo;
  $("reset").textContent = T.reset;
  $("hint").textContent = T.hint;
  $("auto").textContent = T.solve;
  $("menuSub").textContent = T.sub;
  $("splashTag").textContent = T.sub;
  $("descText").innerHTML = T.desc.map(p => "<p>" + p + "</p>").join("");
  $("playBtn").innerHTML = save.level > 1
      ? T.play + "<small>" + T.levelLbl + " " + save.level + "</small>"
      : T.playNew;
  $("menuSound").textContent = T.sound + ": " + (audioOn ? T.on : T.off);
  $("menuFresh").textContent = T.fresh;
  $("menuLang").textContent = T.language;
  $("aboutMenu").textContent = T.about;
  $("aboutBtn").textContent = T.aboutBtn;
  $("rulesBtn").textContent = T.rules + ": " + (hardMode ? T.hard : T.soft);
  $("aboutTitle").textContent = docFor(document.documentElement.lang).title;
  $("aboutBody").innerHTML = docFor(document.documentElement.lang).body;
  $("closeAbout").textContent = T.close;
  $("setupTitle").textContent = T.settings;
  $("langLbl").textContent = T.language;
  $("soundBtn").textContent = T.sound + ": " + (audioOn ? T.on : T.off);
  $("freshBtn").textContent = T.fresh;
  $("closeSetup").textContent = T.close;
  $("tally").textContent = T.stats + ": " + (save.sealed||0) + "\n" + T.total + ": " + (save.total||0);
  $("next").textContent = T.next;
}

function buildFlags(){
  const box = $("flags");
  box.innerHTML = "";
  LANGS.forEach(l => {
    const b = document.createElement("button");
    b.textContent = l.flag;
    if (l.code === langPref) b.classList.add("on");
    b.onclick = () => {
      langPref = l.code; save.lang = l.code; writeSave();
      applyLang(); buildFlags();
    };
    box.appendChild(b);
  });
}
function openSetup(){ applyLang(); buildFlags(); $("setup").classList.add("on"); }
$("amulet").onclick = e => { e.stopPropagation(); openSetup(); };
$("amulet").onerror = () => { $("amulet").classList.add("hidden"); };
$("closeSetup").onclick = () => $("setup").classList.remove("on");
$("aboutBtn").onclick = () => { $("about").classList.add("on"); };
$("rulesBtn").onclick = () => {
  hardMode = !hardMode; save.hard = hardMode; writeSave();
  $("rulesBtn").textContent = T.rules + ": " + (hardMode ? T.hard : T.soft);
  beep("drop");
  startLevel(level);
};
$("closeAbout").onclick = () => $("about").classList.remove("on");
function toggleSound(){
  audioOn = !audioOn; save.sound = audioOn; writeSave();
  const label = T.sound + ": " + (audioOn ? T.on : T.off);
  $("soundBtn").textContent = label;
  $("menuSound").textContent = label;
  if (audioOn) beep("drop");
}
$("soundBtn").onclick = toggleSound;
$("menuSound").onclick = toggleSound;
function armFresh(btn){
  if (btn.dataset.armed){
    save.level = 1; save.sealed = 0; save.total = 0;
    writeSave();
    delete btn.dataset.armed;
    $("setup").classList.remove("on");
    $("menu").classList.remove("on");
    $("veil").classList.remove("on");
    applyLang();
    startLevel(1);
    return;
  }
  btn.dataset.armed = "1";
  btn.textContent = T.freshAsk;
  setTimeout(()=>{ if (btn.dataset.armed){ delete btn.dataset.armed; btn.textContent = T.fresh; } }, 4000);
}
$("freshBtn").onclick = () => armFresh($("freshBtn"));
$("menuFresh").onclick = () => armFresh($("menuFresh"));
$("menuLang").onclick = openSetup;

(async function boot(){
  const splash = $("splash");
  let shown = false;
  const leaveSplash = () => {
    if (shown) return;
    shown = true;
    splash.classList.remove("on");
    setTimeout(()=>{ splash.style.display = "none"; }, 400);
    $("menu").classList.add("on");
  };
  splash.onclick = leaveSplash;
  $("splashImg").onerror = leaveSplash;
  setTimeout(leaveSplash, 5000);

  await loadSave();
  applyLang();
  buildFlags();

  $("playBtn").onclick = () => {
    $("menu").classList.remove("on");
    startLevel(save.level || 1);
  };

  $("splashName").textContent = "HAZNA";
  $("splashTag").textContent = T.sub;
})();
