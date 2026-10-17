#!/bin/sh
# Builds the signed APK with the platform's own command-line tools only:
# checks, compile against the platform jar, convert to dex, package
# resources and assets, align, sign.
#
#   SDK            platform jar to compile against (API 34; the packager
#                  cannot read the resource table of newer ones)
#   KEYSTORE       signing keystore
#   KS_PASS_FILE   file holding its password, so the password never
#                  appears on a command line
#   KS_ALIAS       key alias, default shumtugle
#
# The version lives in the manifest and nowhere else in the code.
set -eu

cd "$(dirname "$0")"
ROOT=$PWD
SDK=${SDK:-$HOME/sdk/android-34.jar}
KS_ALIAS=${KS_ALIAS:-shumtugle}
DX=${DX:-dalvik-exchange}
: "${KEYSTORE:?set KEYSTORE}"
: "${KS_PASS_FILE:?set KS_PASS_FILE}"

VERSION=$(sed -n 's/.*android:versionName="\([^"]*\)".*/\1/p' AndroidManifest.xml)
OUT=$ROOT/out
B=$ROOT/build
rm -rf "$B" "$OUT"
mkdir -p "$B/jvm" "$B/gen" "$B/classes" "$B/assets/art" "$OUT"

# 1. checks on a desktop JVM: engine, a played game, language modules
javac -nowarn -encoding UTF-8 -d "$B/jvm" \
  src/io/github/shumtugle/hazna/Engine.java src/io/github/shumtugle/hazna/Game.java \
  src/io/github/shumtugle/hazna/English.java src/io/github/shumtugle/hazna/Words.java \
  tools/Check.java
[ "${SKIP_CHECKS:-}" = 1 ] || java -Dstdout.encoding=UTF-8 -cp "$B/jvm" io.github.shumtugle.hazna.Check "$ROOT"

# 2. assets: the pictures, and a blank page that lets 2.0 read progress left by 1.x
cp cover.jpg amulet.png "$B/assets/"
cp art/*.jpg "$B/assets/art/"
printf '<!doctype html><meta charset="utf-8"><title></title>\n' > "$B/assets/index.html"

# 3. code
aapt package -f -m -J "$B/gen" -M AndroidManifest.xml -S res -I "$SDK"
javac -nowarn -Xlint:-options -source 8 -target 8 -encoding UTF-8 \
  -bootclasspath "$SDK" -classpath "$SDK" -d "$B/classes" \
  $(find src "$B/gen" -name '*.java')
# the old dexer cannot translate dynamic call sites; there must be none
if javap -c -p $(find "$B/classes" -name '*.class') | grep -q invokedynamic; then
  echo "invokedynamic found: no lambdas or method references in the app" >&2
  exit 1
fi
"$DX" --dex --min-sdk-version=24 --output="$B/classes.dex" "$B/classes"

# 4. package; the resource table stays uncompressed or new systems refuse to install
aapt package -f -0 arsc -M AndroidManifest.xml -S res -A "$B/assets" -I "$SDK" -F "$B/base.apk"
(cd "$B" && aapt add -f base.apk classes.dex >/dev/null)
zipalign -f -p 4 "$B/base.apk" "$B/aligned.apk"
apksigner sign --ks "$KEYSTORE" --ks-key-alias "$KS_ALIAS" \
  --ks-pass "file:$KS_PASS_FILE" \
  --out "$OUT/hazna-$VERSION.apk" "$B/aligned.apk"
rm -f "$OUT/hazna-$VERSION.apk.idsig"
apksigner verify --print-certs "$OUT/hazna-$VERSION.apk" | grep 'SHA-256 digest'
ls -l "$OUT/hazna-$VERSION.apk"
