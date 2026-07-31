/* ---------- модель, солвер, генератор ---------- */

function topGroup(b){
  if (!b.length) return null;
  const c = b[b.length-1];
  let n = 1;
  for (let i=b.length-2;i>=0 && b[i]===c;i--) n++;
  return {color:c, count:n};
}
/* strict=true — правила поиска: солвер не тратит время на переезды с места на место.
   strict=false — правила игрока: переложить можно всё, что физически влезает. */
function canPour(bots, caps, i, j, strict){
  if (i===j) return 0;
  const src = bots[i], dst = bots[j];
  if (!src.length) return 0;
  if (hardMode && isSealed(src, caps[i])) return 0;   // строгие правила: пломба окончательна
  const free = caps[j] - dst.length;
  if (free <= 0) return 0;
  const g = topGroup(src);
  if (dst.length && dst[dst.length-1] !== g.color) return 0;
  if (strict && !dst.length && g.count === src.length && caps[i] === caps[j]) return 0;
  return Math.min(g.count, free);
}
function applyPour(bots, i, j, n){
  const c = bots[i][bots[i].length-1];
  for (let k=0;k<n;k++){ bots[i].pop(); bots[j].push(c); }
}
/* Полный однородный мешок затягивается сам — но эту пломбу можно сорвать тапом.
   Окончательна лишь та, где собран весь вид целиком: срывать её незачем. */
function isSealed(b, cap){
  if (!b.length || b.length !== cap) return false;
  const c = b[0];
  for (const x of b) if (x !== c) return false;
  return true;
}
function isFinal(b, cap, totals){
  return isSealed(b, cap) && (!totals || b.length === totals[b[0]]);
}
/* Ревизия закончена только когда каждый непустой мешок полон, однороден и опечатан.
   Ларь на шесть мест опечатать нельзя — он рабочее место, к концу должен быть пуст. */
function solved(bots, colors, caps){
  const used = new Set();
  for (let i=0;i<bots.length;i++){
    const b = bots[i];
    if (!b.length) continue;
    if (b.length !== caps[i]) return false;
    const c = b[0];
    for (const x of b) if (x !== c) return false;
    if (used.has(c)) return false;
    used.add(c);
  }
  return used.size === colors;
}
function key(bots, caps){ return bots.map((b,i)=>caps[i]+":"+b.join(",")).sort().join("|"); }

/* ---------- солвер ---------- */
/* Поиск по приоритету: сначала разбираются позиции, до порядка которым ближе.
   Оценка остатка — число разрывов в мешках плюс раскиданность видов. */
function Heap(){ this.a = []; }
Heap.prototype.push = function(x){
  const a=this.a; a.push(x); let i=a.length-1;
  while(i>0){ const p=(i-1)>>1; if(a[p].f<=a[i].f) break; const t=a[p]; a[p]=a[i]; a[i]=t; i=p; }
};
Heap.prototype.pop = function(){
  const a=this.a, top=a[0], last=a.pop();
  if(a.length){ a[0]=last; let i=0;
    for(;;){ const l=2*i+1, r=l+1; let m=i;
      if(l<a.length&&a[l].f<a[m].f) m=l;
      if(r<a.length&&a[r].f<a[m].f) m=r;
      if(m===i) break; const t=a[m]; a[m]=a[i]; a[i]=t; i=m; }
  }
  return top;
};
Heap.prototype.size = function(){ return this.a.length; };

function remaining(b){
  let h = 0;
  const spread = new Map();
  for (const bag of b){
    if (!bag.length) continue;
    let groups = 1;
    for (let k=1;k<bag.length;k++) if (bag[k] !== bag[k-1]) groups++;
    h += groups - 1;
    const seenHere = new Set(bag);
    seenHere.forEach(c => spread.set(c, (spread.get(c)||0) + 1));
  }
  spread.forEach(v => { h += v - 1; });
  return h;
}

let SOLVE_TOTALS = null;
function totalsFor(c){ return SOLVE_TOTALS ? SOLVE_TOTALS[c] : Infinity; }

function solve(bots0, caps, colors, nodeLimit, totals){
  SOLVE_TOTALS = totals || null;
  const start = bots0.map(x=>x.slice());
  const heap = new Heap();
  const seen = new Set([key(start, caps)]);
  heap.push({b:start, g:0, f:remaining(start)*2, parent:null, move:null});
  let nodes = 0;

  while (heap.size()){
    const cur = heap.pop();
    if (++nodes > nodeLimit) return null;
    if (solved(cur.b, colors, caps)){
      const path = [];
      let n = cur;
      while (n.move){ path.push(n.move); n = n.parent; }
      return path.reverse();
    }
    const b = cur.b;
    const list = [], merges = [];
    for (let i=0;i<b.length;i++){
      const g = topGroup(b[i]);
      if (!g) continue;
      for (let j=0;j<b.length;j++){
        const n = canPour(b, caps, i, j, true);
        if (!n) continue;
        // при строгих правилах ход, запирающий мешок не на всех камнях вида, — ловушка:
        // достать оттуда уже нельзя, значит к победе он привести не может
        if (hardMode && b[j].length + n === caps[j]
            && b[j].every(x => x === g.color)                 // мешок станет однородным, то есть опечатается
            && b[j].length + n !== totalsFor(g.color)) continue;   // но вид собран не весь — ловушка
        const m = {i,j,n};
        list.push(m);
        if (b[j].length && n === g.count) merges.push(m);   // горсть уходит к своим целиком
      }
    }
    // при строгих правилах доминирующее правило теряет полноту: слияние может отрезать
    // единственный путь, поэтому там перебираем все ходы
    const moves = (!hardMode && merges.length) ? merges : list;
    for (const m of moves){
      const nb = b.map(x=>x.slice());
      applyPour(nb, m.i, m.j, m.n);
      const k = key(nb, caps);
      if (seen.has(k)) continue;
      seen.add(k);
      heap.push({b:nb, g:cur.g+1, f:cur.g+1+remaining(nb)*2, parent:cur, move:[m.i,m.j]});
    }
  }
  return null;
}

/* ---------- генератор ---------- */
/* Добыча и тара считаются вместе: сколько камней вида, столько мест в его мешке.
   Ларь на одно место больше самого крупного мешка — опечатать его нельзя никогда.
   Раздача строится обратным ходом из опечатанной казны: каждый шаг обратим,
   значит путь назад существует всегда. Солвер при раздаче не нужен. */
function rnd(lo, hi){ return lo + Math.floor(Math.random()*(hi-lo+1)); }
function shuffle(a){
  for (let i=a.length-1;i>0;i--){ const j=Math.floor(Math.random()*(i+1)); [a[i],a[j]]=[a[j],a[i]]; }
  return a;
}

function levelPlan(n){
  // на маленьком экране казна беднее — иначе мешки пришлось бы мельчить
  const room = (typeof window !== "undefined" && window.innerHeight) ? window.innerHeight : 700;
  const ceiling = room >= 780 ? MAX_GEMS : (room >= 660 ? 10 : 8);
  const base = Math.min(4 + Math.floor((n-1)/2), ceiling);
  const colors = Math.max(3, Math.min(ceiling, base + [-1,0,0,1][rnd(0,3)]));
  let lo = 3, hi = 4;
  if (n >= 5) hi = 5;
  if (n >= 9) hi = 6;
  if (n >= 13) lo = 4;
  const counts = [];
  for (let i=0;i<colors;i++) counts.push(rnd(lo, hi));
  shuffle(counts);
  const chestCap = Math.max.apply(null, counts) + 1;
  const chestN = colors <= 6 ? 1 : 2;
  return {colors, counts, chestCap, chestN};
}

/* один обратный шаг: снять k верхних камней одного вида и положить туда,
   откуда прямой ход вернёт ровно их же */
function unmove(bots, caps){
  const from = [];
  for (let i=0;i<bots.length;i++) if (bots[i].length) from.push(i);
  shuffle(from);
  for (const i of from){
    const g = topGroup(bots[i]);
    const maxK = (g.count === bots[i].length) ? g.count : g.count - 1;  // либо опустошаем, либо оставляем свой вид сверху
    if (maxK < 1) continue;
    const k = rnd(1, maxK);
    const to = [];
    for (let j=0;j<bots.length;j++){
      if (j === i) continue;
      if (caps[j] - bots[j].length < k) continue;
      if (bots[j].length && bots[j][bots[j].length-1] === g.color) continue;  // иначе горсти слипнутся
      to.push(j);
    }
    if (!to.length) continue;
    // прямой ход пойдёт из j обратно в i: значит после укладки мешок j не должен оказаться
    // полным и однородным, иначе при строгих правилах он опечатается и путь назад пропадёт
    const safe = to.filter(j => !(bots[j].length + k === caps[j] && bots[j].every(x => x === g.color)));
    if (!safe.length) continue;
    const j = safe[rnd(0, safe.length-1)];
    for (let t=0;t<k;t++){ bots[i].pop(); bots[j].push(g.color); }
    return true;
  }
  return false;
}

function buildLevel(n){
  const {colors, counts, chestCap, chestN} = levelPlan(n);
  const caps = counts.slice();
  const chest = caps.map(()=>false);
  for (let i=0;i<chestN;i++){ caps.push(chestCap); chest.push(true); }

  let best = null, bestMess = -1;
  for (let attempt=0; attempt<12; attempt++){
    const bots = caps.map(()=>[]);
    for (let c=0;c<colors;c++) for (let k=0;k<counts[c];k++) bots[c].push(c);
    const steps = colors * 14 + 30;
    for (let s=0;s<steps;s++) if (!unmove(bots, caps)) break;

    let sealedAtStart = 0;
    bots.forEach((b,i)=>{ if (isSealed(b, caps[i])) sealedAtStart++; });
    const mess = remaining(bots) - sealedAtStart*4;
    if (mess > bestMess){ bestMess = mess; best = bots; }
    if (!sealedAtStart && mess >= colors + 4) return {caps, chest, bots, colors, totals:counts.slice()};
  }
  return {caps, chest, bots: best, colors, totals:counts.slice()};
}

