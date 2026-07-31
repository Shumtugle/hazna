#!/bin/sh
# Сборка APK без Android Studio и Gradle.
# Нужны: java, apktool.jar, uber-apk-signer.jar (оба качаются с GitHub Releases).
set -e
cd "$(dirname "$0")"

sh src/build.sh

# ассеты живут в корне репозитория, здесь только копия для упаковки
rm -rf app/assets && mkdir -p app/assets/art
cp play.html app/assets/index.html
cp cover.jpg amulet.png app/assets/
cp art/*.jpg app/assets/art/

rm -f hazna-unsigned.apk hazna.apk
java -jar apktool.jar b app -o hazna-unsigned.apk

# resources.arsc обязан лежать несжатым, иначе Android 11+ откажется ставить
rm -rf .zipwork && mkdir .zipwork && cd .zipwork
unzip -q ../hazna-unsigned.apk
zip -q -X -0 ../hazna.apk resources.arsc
zip -q -X -9 -r ../hazna.apk AndroidManifest.xml classes.dex res assets -x resources.arsc
cd .. && rm -rf .zipwork hazna-unsigned.apk

# подпись: пароль спросят в консоли
java -jar uber-apk-signer.jar -a hazna.apk --ks shumtugle.keystore --ksAlias shumtugle --out release

echo "готово: release/"
