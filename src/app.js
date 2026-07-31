/* ---------- камни: цвет + огранка ---------- */
const GEMS = [
  {c:"#C62348", g:0}, {c:"#1E8F63", g:4}, {c:"#2A55B8", g:9}, {c:"#E9A81C", g:1},
  {c:"#7B3FB8", g:3}, {c:"#35A8C4", g:2}, {c:"#33313F", g:8}, {c:"#EDE4D0", g:5},
  {c:"#7FA33A", g:7}, {c:"#E4693C", g:6}, {c:"#B8D14A", g:10}, {c:"#E24D8C", g:11}
];
const MAX_GEMS = GEMS.length;

/* ---------- язык ---------- */
let langPref = "auto", T = L.en, PADISHAH = PADISHAH_EN;
function resolveLang(){
  let code = langPref;
  if (code === "auto"){
    const sys = ((navigator.language || "en").slice(0,2)).toLowerCase();
    code = L[sys] ? sys : "en";
  }
  T = L[code] || L.en;
  PADISHAH = (code === "ru") ? PADISHAH_RU : PADISHAH_EN;   // реплики пока на двух языках
  document.documentElement.lang = code;
}

/* ---------- сохранение: только счёт, не расклад ---------- */
/* В APK окно window.storage отсутствует, работает localStorage. */
let save = {level:1, sealed:0, total:0, lang:"auto", sound:true};
async function loadSave(){
  let raw = null;
  try{
    if (window.storage){
      const r = await window.storage.get("purrsia");
      if (r && r.value) raw = r.value;
    }
  }catch(e){}
  if (raw === null){
    try{ raw = window.localStorage.getItem("purrsia"); }catch(e){}
  }
  if (raw){
    try{ Object.assign(save, JSON.parse(raw)); }catch(e){}
  }
  langPref = save.lang || "auto";
  audioOn = save.sound !== false;
  resolveLang();
}
async function writeSave(){
  const raw = JSON.stringify(save);
  try{ window.localStorage.setItem("purrsia", raw); }catch(e){}
  try{ if (window.storage) await window.storage.set("purrsia", raw); }catch(e){}
}

/* ---------- звук ---------- */
let audioOn = true, actx = null;
function beep(type){
  if (!audioOn) return;
  try{
    if (!actx) actx = new (window.AudioContext||window.webkitAudioContext)();
    const t = actx.currentTime, o = actx.createOscillator(), g = actx.createGain();
    if (type === "drop"){
      o.type = "triangle"; o.frequency.setValueAtTime(560, t);
      o.frequency.exponentialRampToValueAtTime(280, t+.09);
      g.gain.setValueAtTime(.08, t); g.gain.exponentialRampToValueAtTime(.001, t+.12);
      o.connect(g).connect(actx.destination); o.start(t); o.stop(t+.13);
    } else if (type === "back"){
      o.type = "sine"; o.frequency.setValueAtTime(240, t);
      o.frequency.exponentialRampToValueAtTime(430, t+.1);
      g.gain.setValueAtTime(.05, t); g.gain.exponentialRampToValueAtTime(.001, t+.13);
      o.connect(g).connect(actx.destination); o.start(t); o.stop(t+.14);
    } else {
      o.type = "square"; o.frequency.setValueAtTime(180, t);
      o.frequency.exponentialRampToValueAtTime(90, t+.16);
      g.gain.setValueAtTime(.09, t); g.gain.exponentialRampToValueAtTime(.001, t+.22);
      o.connect(g).connect(actx.destination); o.start(t); o.stop(t+.23);
    }
  }catch(e){}
}

/* ---------- состояние партии ---------- */
let state = null;
let level = 1, moves = 0, selected = -1, history = [], prevSealed = new Set(), autoTimer = null;
