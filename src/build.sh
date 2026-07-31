#!/bin/sh
# Склеивает игру из кусков в один файл play.html
set -e
cd "$(dirname "$0")/.."
cat src/shell.html > play.html
cat src/i18n_a.js src/i18n_b.js src/desc.js src/doc.js \
    src/padishah_ru.js src/padishah_en.js src/app.js src/engine.js src/ui.js >> play.html
printf '\n</script>\n</body>\n</html>\n' >> play.html
echo "play.html собран"
