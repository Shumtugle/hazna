#!/bin/sh
set -e
cd "$(dirname "$0")"

sh src/build.sh
node tools/smoke.js || { echo "smoke test failed - build aborted"; exit 1; }

rm -rf app/assets && mkdir -p app/assets/art
cp play.html app/assets/index.html
cp cover.jpg amulet.png app/assets/
cp art/*.jpg app/assets/art/

rm -f hazna-unsigned.apk hazna.apk
java -jar apktool.jar b app -o hazna-unsigned.apk

rm -rf .zipwork && mkdir .zipwork && cd .zipwork
unzip -q ../hazna-unsigned.apk
zip -q -X -0 ../hazna.apk resources.arsc
zip -q -X -9 -r ../hazna.apk AndroidManifest.xml classes.dex res assets -x resources.arsc
cd .. && rm -rf .zipwork hazna-unsigned.apk

java -jar uber-apk-signer.jar -a hazna.apk --ks shumtugle.keystore --ksAlias shumtugle --out release

echo "done: release/"
