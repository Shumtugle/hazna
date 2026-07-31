/* Стенд: собирает игру, подсовывает ей поддельный DOM и по-настоящему играет.
   Ловит то, что не видит проверка синтаксиса: обращения к несуществующим элементам,
   необъявленные переменные, падения при отрисовке.
   Запуск:  node tools/smoke.js  */

const fs = require('fs');
const path = require('path');
const vm = require('vm');

const root = path.join(__dirname, '..');
const html = fs.readFileSync(path.join(root, 'play.html'), 'utf8');
// целостность файла: склейка обязана заканчиваться закрывающими тегами
for (const tag of ['</script>', '</body>', '</html>']){
  if (!html.includes(tag)){
    console.log('✗ файл оборван: нет', tag, '— пересоберите через src/build.sh');
    process.exit(1);
  }
}
const js = html.slice(html.indexOf('<script>') + 8, html.lastIndexOf('</script>'));

// id, которые есть в разметке — всё остальное вернёт null, как настоящий браузер
const markup = html.slice(0, html.indexOf('<script>'));
const KNOWN = new Set([...markup.matchAll(/id="([^"]+)"/g)].map(m => m[1]));

const made = new Map();
function node(id){
  if (!made.has(id)) made.set(id, {
    id, onclick:null, onerror:null, textContent:'', innerHTML:'', src:'',
    dataset:{}, disabled:false, clientHeight:640, offsetHeight:48,
    children:[], style:{ setProperty(){}, maxWidth:'', display:'' },
    classList:{ set:new Set(),
      add(c){ this.set.add(c); }, remove(c){ this.set.delete(c); }, contains(c){ return this.set.has(c); } },
    appendChild(n){ this.children.push(n); },
    getBoundingClientRect:() => ({left:0, top:0, width:50, height:50}),
    animate:() => ({ onfinish:null }),
    remove(){},
    closest(){ return null; }
  });
  return made.get(id);
}

const timers = [];
const sandbox = {
  console,
  navigator:{ language:'ru' },
  setTimeout:(fn, ms) => { timers.push({fn, ms}); return timers.length; },
  clearTimeout:() => {},
  document:{
    getElementById:(id) => KNOWN.has(id) ? node(id) : null,
    querySelector:(s) => node('q:' + s),
    querySelectorAll:() => [],
    createElement:() => node('new:' + Math.random()),
    addEventListener:() => {},
    documentElement:{ style:{ setProperty(){} }, lang:'ru', dir:'' },
    body:{ appendChild(){} }
  },
  window:{
    innerWidth:400, innerHeight:790,
    addEventListener:() => {},
    localStorage:{ store:{},
      getItem(k){ return this.store[k] ?? null; },
      setItem(k, v){ this.store[k] = v; } }
  }
};
sandbox.window.document = sandbox.document;
sandbox.globalThis = sandbox;

const ctx = vm.createContext(sandbox);
// let-переменные не попадают в глобальный объект — открываем к ним доступ явно
vm.runInContext(js + '\nglobalThis.__peek = () => ({state, opened, history, level, moves, hardMode});\nglobalThis.__setHard = (v) => { hardMode = v; };', ctx, { filename:'play.html' });

// запуск: прогоняем отложенные вызовы, как это сделал бы браузер
let guard = 0;
while (timers.length && guard++ < 200) timers.shift().fn();

const fail = [];
function check(name, fn){
  try { fn(); console.log('  ✓', name); }
  catch (e){ fail.push(name); console.log('  ✗', name, '—', e.message); }
}

console.log('Запуск и отрисовка:');
check('уровень строится и рисуется', () => ctx.startLevel(1));
check('перерисовка после хода', () => { ctx.render(); });
check('размеры считаются', () => ctx.fitSizes());
check('смена языка', () => { ctx.langPref = 'ar'; ctx.applyLang(); ctx.langPref = 'ru'; ctx.applyLang(); });
check('настройки открываются', () => ctx.openSetup());
check('флаги строятся', () => ctx.buildFlags());

for (const hard of [true, false]){
console.log('\nИгра сотней случайных ходов на десяти ревизиях, правила ' + (hard ? 'строгие' : 'мягкие') + ':');
ctx.__setHard(hard);
check('ходы, пломбы, откат', () => {
  for (let lvl = 1; lvl <= 10; lvl++){
    ctx.startLevel(lvl);
    for (let step = 0; step < 100; step++){
      const st = ctx.__peek().state;
      const opts = [];
      for (let i = 0; i < st.bots.length; i++)
        for (let j = 0; j < st.bots.length; j++){
          const n = ctx.canPour(st.bots, st.caps, i, j, false);
          if (n) opts.push([i, j, n]);
        }
      if (!opts.length) break;
      const [i, j, n] = opts[Math.floor(Math.random() * opts.length)];
      ctx.doMove(i, j, n);
      if (step % 7 === 0) ctx.tap(i);        // тапаем — в том числе по опечатанным
    }
  }
});
}

const peek = ctx.__peek();
console.log('\nСостояние после игры: ревизия', peek.level, '· ходов', peek.moves,
            '· сорванных пломб', peek.opened.size, '· в истории', peek.history.length,
            '· правила', peek.hardMode ? 'строгие' : 'мягкие');
console.log('\nВывод:', fail.length ? ('ПАДЕНИЙ ' + fail.length + ': ' + fail.join(', ')) : 'всё живо');
process.exit(fail.length ? 1 : 0);
