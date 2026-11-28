#!/usr/bin/env bash
# ArabaCheck APK derleme betiği (Gradle gerektirmez).
# Gerekenler: JDK 17+, Android SDK (platforms;android-35, build-tools;35.0.0).
set -euo pipefail
cd "$(dirname "$0")"
SDK="${ANDROID_HOME:-/opt/android-sdk}"
BT="$SDK/build-tools/35.0.0"
JAR="$SDK/platforms/android-35/android.jar"
OUT=build
rm -rf "$OUT" && mkdir -p "$OUT/classes" "$OUT/dex"

# Web sayfasının en güncel hâlini uygulamaya kopyala
cp ../index.html assets/index.html
rm -rf assets/ocr && cp -r ../ocr assets/ocr
rm -rf assets/vendor && cp -r ../vendor assets/vendor

"$BT/aapt2" compile --dir res -o "$OUT/res.zip"
"$BT/aapt2" link -o "$OUT/unsigned.apk" -I "$JAR" --manifest AndroidManifest.xml \
  -A assets -0 gz -0 wasm --java "$OUT/gen" "$OUT/res.zip" --auto-add-overlay
javac -nowarn -source 17 -target 17 -classpath "$JAR" -d "$OUT/classes" \
  $(find src "$OUT/gen" -name '*.java') 2> >(grep -v -e '^warning' -e 'deprecat' -e '^Note:' -e '^1 warning' >&2)
"$BT/d8" --release --min-api 24 --lib "$JAR" --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')
cp "$OUT/unsigned.apk" "$OUT/with-dex.apk"
(cd "$OUT/dex" && zip -q -j ../with-dex.apk classes.dex)
"$BT/zipalign" -f -p 4 "$OUT/with-dex.apk" "$OUT/aligned.apk"

KS="${KEYSTORE:-arabacheck.keystore}"
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -storepass arabacheck -keypass arabacheck -alias arabacheck \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=ArabaCheck, C=TR" >/dev/null 2>&1
fi
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:arabacheck --key-pass pass:arabacheck \
  --out ../ArabaCheck.apk "$OUT/aligned.apk"
"$BT/apksigner" verify ../ArabaCheck.apk
echo "Hazır: $(cd .. && pwd)/ArabaCheck.apk"
