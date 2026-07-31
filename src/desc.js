/* ---------- описание игры ----------
   Не перевод, а один и тот же рассказ, сказанный на четырнадцати языках. */

const DESC = {

en: [
"Wealth that does not know its own edges is blind. Padishah Murr ad-Din put no faith in the calligraphy of his scribes: paper endures any lie, provided the lie is tidy. He wanted the limit, not the sum — something a paw could rest on.",
"The seals were broken. Eleven years of disorder spilled out of the dark: ruby pressed under the dull weight of onyx, pearl rubbed raw among spinel, and on the sapphires a hollow left by whoever had slept there while the realm was called rich. Drawing the border of all this splendour falls to you.",
"The matter itself sets the rules. Every sack is cut to the count of its own kind — no slack, no crowding. Like to like, filled to the brim, drawstring pulled tight. The chest in the middle of the table is a purgatory: it takes in what has no place yet, but it wears no seal, and by the last knot it must stand empty.",
"The mercy of this puzzle is its memory. If the count locks into a dead knot, time can be broken open. The game does not rewrite the game from the start; it winds back to the fork where your move was still sound, and from that point carries the audit through, coldly and without error."
],

ru: [
"Богатство, не знающее своих границ, слепо. Падишах Мурр ад-Дин не поверил каллиграфии писцов: бумага стерпит любую ложь, если ложь опрятна. Ему нужна была не сумма, а предел — то, на что можно положить лапу.",
"Печати сорвали. Из темноты вывалилось одиннадцать лет беспорядка: рубин под глухим весом оникса, жемчуг, затёртый шпинелью, и на сапфирах ложбина от того, кто спал на них, пока держава считалась богатой. Назначить границу этому великолепию выпало тебе.",
"Правила диктует сама материя. Мешок скроен по числу камней своей породы: ни пустоты, ни тесноты. Родственное к родственному, доверху, и шнур затянуть. Ларь посреди стола — чистилище: он принимает то, чему пока нет места, но печати не носит и к последнему узлу обязан стоять пустым.",
"Милосердие этой головоломки — в памяти. Если счёт сойдётся в мёртвый узел, время можно надломить. Игра не переписывает партию с начала: она отматывает до развилки, где твой ход ещё был верен, и оттуда доводит ревизию до конца — холодно и без ошибки."
],

de: [
"Reichtum, der seine Grenzen nicht kennt, ist blind. Padischah Murr ad-Din traute der Kalligraphie seiner Schreiber nicht: Papier erträgt jede Lüge, sofern sie ordentlich ist. Er wollte nicht die Summe, sondern die Grenze — etwas, worauf sich eine Pfote legen lässt.",
"Die Siegel wurden aufgebrochen. Elf Jahre Unordnung fielen aus dem Dunkel: Rubin unter dem dumpfen Gewicht des Onyx, Perlen wundgerieben zwischen Spinellen, und auf den Saphiren eine Mulde von dem, der darauf schlief, während das Reich als reich galt. Diesem Glanz eine Grenze zu ziehen, fällt dir zu.",
"Die Regeln gibt der Stoff selbst. Jeder Sack ist nach der Zahl seiner Art geschnitten: kein leerer Platz, keine Enge. Gleiches zu Gleichem, bis oben, die Schnur fest zugezogen. Die Truhe in der Mitte ist ein Fegefeuer: sie nimmt auf, was noch keinen Platz hat, doch sie trägt kein Siegel und muss beim letzten Knoten leer dastehen.",
"Die Gnade dieses Rätsels liegt in seinem Gedächtnis. Verknotet sich die Rechnung tödlich, lässt sich die Zeit aufbrechen. Das Spiel schreibt die Partie nicht neu; es spult zurück bis zur Gabelung, an der dein Zug noch richtig war, und führt die Prüfung von dort zu Ende, kühl und fehlerlos."
],

fr: [
"Une richesse qui ignore ses bords est aveugle. Le padischah Murr ad-Din ne crut pas la calligraphie de ses scribes : le papier supporte tous les mensonges, pourvu qu'ils soient bien rangés. Il voulait la limite, non la somme — quelque chose où poser la patte.",
"On brisa les sceaux. Onze ans de désordre tombèrent de l'obscurité : le rubis sous le poids sourd de l'onyx, la perle usée parmi les spinelles, et sur les saphirs le creux laissé par celui qui y dormait pendant qu'on disait l'empire riche. Tracer la bordure de cette splendeur te revient.",
"C'est la matière qui dicte les règles. Chaque sac est taillé au nombre de sa propre espèce : ni vide, ni presse. Le semblable au semblable, jusqu'au bord, cordon bien serré. Le coffre au milieu de la table est un purgatoire : il accueille ce qui n'a pas encore de place, mais il ne porte pas de sceau, et au dernier nœud il doit rester vide.",
"La clémence de cette énigme tient à sa mémoire. Si le compte se noue à mort, on peut fendre le temps. Le jeu ne réécrit pas la partie depuis le début : il rembobine jusqu'à la bifurcation où ton coup était encore juste, et de là mène l'audit à son terme, froidement et sans faute."
],

es: [
"La riqueza que ignora sus bordes es ciega. El padishah Murr ad-Din no creyó en la caligrafía de sus escribas: el papel soporta cualquier mentira, con tal de que esté ordenada. Quería el límite, no la suma; algo sobre lo que apoyar la zarpa.",
"Rompieron los sellos. Once años de desorden cayeron de la oscuridad: rubí bajo el peso sordo del ónice, perla desgastada entre espinelas, y en los zafiros el hueco de quien durmió allí mientras al reino lo llamaban rico. Ponerle borde a ese esplendor te toca a ti.",
"Las reglas las dicta la materia. Cada saco está cortado al número de su propia especie: ni vacío de más, ni estrechez. Lo semejante con lo semejante, hasta el borde, y el cordón bien apretado. El arca en mitad de la mesa es un purgatorio: acoge lo que aún no tiene sitio, pero no lleva sello, y al último nudo ha de quedar vacía.",
"La clemencia de este juego está en su memoria. Si la cuenta se anuda de muerte, el tiempo puede quebrarse. No reescribe la partida desde el principio: rebobina hasta la bifurcación donde tu jugada aún era buena y desde allí lleva la auditoría hasta el final, con frialdad y sin error."
],

sv: [
"Rikedom som inte känner sina kanter är blind. Padishah Murr ad-Din trodde inte på skrivarnas kalligrafi: papper tål vilken lögn som helst, bara den är prydlig. Han ville ha gränsen, inte summan — något att lägga tassen på.",
"Sigillen bröts. Elva år av oordning föll ut ur mörkret: rubin under onyxens dova tyngd, pärlor nötta bland speneller, och på safirerna en grop efter den som sov där medan riket kallades rikt. Att dra gränsen kring all denna prakt faller på dig.",
"Reglerna ges av materialet självt. Varje påse är skuren efter antalet av sin egen sort: ingen glapp, ingen trängsel. Lika till lika, ända upp, och snöret hårt åtdraget. Kistan mitt på bordet är en skärseld: den tar emot det som ännu saknar plats, men bär inget sigill, och vid sista knuten ska den stå tom.",
"Barmhärtigheten i den här gåtan ligger i dess minne. Knyter sig räkningen dödligt kan tiden brytas upp. Spelet skriver inte om partiet från början; det spolar tillbaka till gaffeln där ditt drag ännu höll, och för därifrån revisionen i mål, kallt och utan fel."
],

fi: [
"Rikkaus, joka ei tunne rajojaan, on sokea. Padishah Murr ad-Din ei uskonut kirjuriensa kaunokirjoitusta: paperi kestää minkä tahansa valheen, kunhan se on siisti. Hän halusi rajan eikä summaa — jotain, jolle voi laskea tassun.",
"Sinetit murrettiin. Yksitoista vuotta epäjärjestystä valui pimeästä: rubiini onyksin kumean painon alla, helmi hankautuneena spinellien seassa, ja safiireissa kuoppa siitä, joka nukkui niiden päällä sillä aikaa kun valtakuntaa sanottiin rikkaaksi. Rajan vetäminen tälle loistolle jää sinulle.",
"Säännöt sanelee aine itse. Jokainen pussi on leikattu oman lajinsa lukumäärän mukaan: ei väljyyttä, ei ahtautta. Samanlainen samanlaisen luo, täyteen asti, ja nyöri tiukalle. Pöydän keskellä oleva arkku on kiirastuli: se ottaa vastaan sen, jolla ei vielä ole paikkaa, mutta sinettiä se ei kanna, ja viimeisen solmun kohdalla sen on seisottava tyhjänä.",
"Tämän pulman armo on sen muistissa. Jos laskelma kiristyy kuolleeksi solmuksi, ajan voi murtaa. Peli ei kirjoita erää alusta uusiksi: se kelaa siihen haaraan, jossa siirtosi oli vielä oikea, ja vie tarkastuksen sieltä loppuun, kylmästi ja virheettä."
],

uk: [
"Багатство, що не знає своїх меж, сліпе. Падишах Мурр ад-Дін не повірив каліграфії писарів: папір стерпить будь-яку брехню, аби вона була охайна. Йому потрібна була не сума, а межа — те, на що можна покласти лапу.",
"Печаті зірвали. З темряви випало одинадцять років безладу: рубін під глухою вагою оніксу, перли, затерті шпінеллю, і на сапфірах улоговина від того, хто спав на них, поки державу звали багатою. Провести межу цій пишноті випало тобі.",
"Правила диктує сама матерія. Мішок скроєно за числом каменів своєї породи: ні порожнечі, ні тісноти. Споріднене до спорідненого, доверху, і шнур затягнути. Скриня посеред столу — чистилище: вона приймає те, чому ще немає місця, але печатки не носить і до останнього вузла має стояти порожньою.",
"Милосердя цієї головоломки — у пам'яті. Якщо рахунок зійдеться в мертвий вузол, час можна надламати. Гра не переписує партію з початку: вона відмотує до розвилки, де твій хід іще був вірний, і звідти доводить ревізію до кінця — холодно й безпомилково."
],

ar: [
"الثروة التي لا تعرف حدودها عمياء. لم يصدّق الباديشاه مُرّ الدين خطّ كتبته: الورق يحتمل كل كذبة ما دامت مرتّبة. أراد الحدّ لا المقدار، شيئًا يمكن أن تستقرّ عليه الكفّ.",
"كُسرت الأختام. فانسكب من الظلام إحدى عشرة سنة من الفوضى: ياقوت تحت ثقل العقيق الأخرس، ولؤلؤ انسحق بين الإسبينل، وفي الياقوت الأزرق حفرة تركها من نام عليه بينما كانت المملكة تُدعى غنية. ورسم حدّ هذا البهاء صار إليك.",
"المادة نفسها تضع القواعد. كل كيس مفصّل على عدد نوعه: لا فراغ ولا ازدحام. الشبيه إلى شبيهه، حتى الحافة، ثم يُشدّ الرباط. أما الصندوق في وسط الطاولة فمَطهر: يقبل ما لا مكان له بعد، لكنه لا يحمل ختمًا، وعند العقدة الأخيرة يجب أن يبقى فارغًا.",
"رحمة هذا اللغز في ذاكرته. إذا انعقد الحساب عقدة ميتة أمكن كسر الزمن. اللعبة لا تعيد كتابة الدور من أوله، بل تعود إلى المفترق الذي كانت نقلتك فيه سليمة، ومن هناك تُتمّ التدقيق ببرود ودون خطأ."
],

iw: [
"עושר שאינו יודע את גבולותיו הוא עיוור. הפאדישאה מור א-דין לא האמין לכתב היד של לבלריו: הנייר סובל כל שקר, ובלבד שיהיה מסודר. הוא רצה את הגבול, לא את הסכום — משהו שאפשר להניח עליו כף.",
"החותמות נפרצו. אחת עשרה שנות אי-סדר נשפכו מן החושך: אודם תחת משקלו האטום של השוהם, פנינה שנשחקה בין הספינלים, ובספירים גומה של מי שישן עליהם בעוד הממלכה נקראת עשירה. לשרטט גבול להוד הזה נפל עליך.",
"את הכללים מכתיב החומר עצמו. כל שק נגזר לפי מניין מינו: לא רווח ולא צפיפות. דומה אל דומה, עד הסוף, והחוט נמשך היטב. התיבה שבמרכז השולחן היא כור מצרף: היא קולטת את מה שאין לו עדיין מקום, אך אינה נושאת חותם, ובקשר האחרון עליה לעמוד ריקה.",
"החסד שבחידה הזאת הוא זיכרונה. אם החשבון נקשר בקשר מת, אפשר לשבור את הזמן. המשחק אינו כותב את המערכה מחדש: הוא חוזר עד לפיצול שבו המהלך שלך עוד היה נכון, ומשם מוליך את הביקורת עד סופה, בקור רוח ובלי טעות."
],

ur: [
"وہ دولت جو اپنی حدیں نہیں جانتی، اندھی ہے۔ بادشاہ مر الدین نے اپنے منشیوں کی خوشنویسی پر یقین نہ کیا: کاغذ ہر جھوٹ سہہ لیتا ہے، بشرطیکہ جھوٹ سلیقے سے ہو۔ اُسے مقدار نہیں، حد چاہیے تھی — ایسی چیز جس پر پنجہ رکھا جا سکے۔",
"مہریں توڑی گئیں۔ اندھیرے سے گیارہ برس کی بے ترتیبی نکل پڑی: عقیقِ سیاہ کے بھاری بوجھ تلے یاقوت، اسپنل کے بیچ رگڑا ہوا موتی، اور نیلم پر اُس کا گڑھا جو اُن پر سوتا رہا جب سلطنت کو امیر کہا جاتا تھا۔ اِس شان کی حد کھینچنا اب تمہارے ذمے ہے۔",
"قاعدے خود مادّہ طے کرتا ہے۔ ہر تھیلی اپنی قسم کی گنتی کے مطابق کاٹی گئی ہے: نہ خالی جگہ، نہ تنگی۔ ہم جنس ہم جنس کے ساتھ، اوپر تک، اور ڈوری کَس دو۔ میز کے بیچ رکھا صندوق برزخ ہے: جس چیز کی ابھی جگہ نہیں، اُسے سنبھال لیتا ہے، مگر مہر نہیں اٹھاتا، اور آخری گرہ تک اُسے خالی کھڑا ہونا ہے۔",
"اِس معمّے کا رحم اُس کی یادداشت میں ہے۔ اگر حساب مردہ گرہ بن جائے تو وقت کو توڑا جا سکتا ہے۔ کھیل بازی کو سرے سے نہیں لکھتا: وہ اُس موڑ تک واپس لے جاتا ہے جہاں تمہاری چال ابھی درست تھی، اور وہیں سے جانچ کو ٹھنڈے دل اور بغیر غلطی کے انجام تک پہنچاتا ہے۔"
],

kk: [
"Шегін білмейтін байлық — соқыр байлық. Падишах Мұрр ад-Дин хатшыларының әдемі жазуына сенбеді: қағаз кез келген өтірікті көтереді, тек ол ұқыпты болса болғаны. Оған мөлшер емес, шек керек еді — алақан қоятын нәрсе.",
"Мөрлер бұзылды. Қараңғыдан он бір жылдық ретсіздік төгілді: оникстің тұйық салмағы астындағы лағыл, шпинельдің арасында мұқалған інжу, ал сапфирлерде — патшалық бай саналып тұрғанда солардың үстінде ұйықтаған біреудің ойығы. Осы салтанатқа шек қоюды саған тапсырды.",
"Ережені материяның өзі айтады. Әр қалта өз тұқымының санына шақ пішілген: артық орын да, қысылу да жоқ. Тектесті тектеске, аузына дейін, бауын тартып байла. Үстелдің ортасындағы сандық — аралық орын: әзірге орны жоқ нәрсені қабылдайды, бірақ мөр көтермейді, ал соңғы түйін түйілгенде бос тұруға тиіс.",
"Бұл жұмбақтың мейірімі — жадында. Есеп өлі түйінге айналса, уақытты сындыруға болады. Ойын партияны басынан жазбайды: жүрісің әлі дұрыс болған айырыққа шегінеді де, тексерісті сол жерден суық қандылықпен, қатесіз аяқтайды."
],

ky: [
"Чегин билбеген байлык — сокур байлык. Падыша Мурр ад-Дин катчыларынын кооз жазуусуна ишенген жок: кагаз кандай гана калпты болбосун көтөрөт, эгер ал тыкан болсо. Ага өлчөм эмес, чек керек эле — алаканын коё турган нерсе.",
"Мөөрлөр сынды. Караңгыдан он бир жылдык баш аламандык төгүлдү: оникстин дүлөй салмагы астындагы лаал, шпинелдин арасында жышылган бермет, ал эми сапфирлерде — мамлекет бай деп аталып турганда алардын үстүндө уктаган бирөөнүн изи. Ушул салтанатка чек коюу сага тийди.",
"Эрежени материянын өзү айтат. Ар бир баштык өз тукумунун санына так жаратылган: ашыкча орун да, кысылуу да жок. Тектешти тектешке, оозуна чейин, боосун тартып байла. Үстөлдүн ортосундагы сандык — аралык жай: азырынча орду жок нерсени кабыл алат, бирок мөөр көтөрбөйт, акыркы түйүн түйүлгөндө бош турушу керек.",
"Бул табышмактын ырайымы — эсинде. Эсеп өлүү түйүнгө айланса, убакытты сындырууга болот. Оюн партияны башынан жазбайт: жүрүшүң дагы эле туура болгон айрылышка кайтат да, текшерүүнү ошол жерден муздак кандуулук менен, катасыз аяктайт."
],

zh: [
"不知边界的财富是盲目的。帕迪沙穆尔·阿德丁不信书吏的漂亮字迹：只要谎撒得整齐，纸张什么都受得住。他要的不是总数，而是边界——一处可以按下爪子的地方。",
"封缄被撬开。十一年的失序从黑暗里倾出：红宝石压在缟玛瑙沉闷的分量之下，珍珠在尖晶石间磨得发涩，蓝宝石上还留着一处凹陷——当王国被称作富庶时，有谁一直睡在上面。为这份华美划出边界的事，落到了你头上。",
"规矩由材料自己定下。每只袋子按本族石头的数目裁成：不多一分空，不少一分位。同类归同类，装到袋口，把绳抽紧。桌子中央那只箱子是炼狱：它收容暂时无处安放之物，却不佩封印，等最后一道结打好时，它必须空着。",
"这道谜题的慈悲在于它的记忆。若账目绞成死结，时间是可以掰开的。它不会把这一局从头重写，而是倒回你最后一步仍然正确的岔口，再从那里把清点冷静无误地做完。"
]

};

for (const code in DESC) if (L[code]) L[code].desc = DESC[code];
