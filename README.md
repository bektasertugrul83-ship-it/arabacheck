# ArabaCheck

Türkiye ikinci el araç piyasası için ekspertiz ön raporu: fiyat analizi, kronik sorunlar, satın alma riski, yıllık maliyet ve araç karşılaştırma.

| Klasör | İçerik |
|---|---|
| `index.html` | Uygulamanın tamamı (tek dosyalık web sayfası) |
| `android/` | Android uygulaması (WebView). `./build.sh` → `ArabaCheck.apk` |
| `desktop/` | Windows / Mac / Linux uygulaması (Electron). `npm install` → `npm run dist:win` |

Web sayfasını değiştirdikten sonra Android ve masaüstü derlemeleri `index.html` dosyasını otomatik olarak kopyalar.

**Android derleme gereksinimleri:** JDK 17+, Android SDK (`platforms;android-35`, `build-tools;35.0.0`), `ANDROID_HOME` ortam değişkeni.

**İmza anahtarı:** `android/arabacheck.keystore` depoya eklenmez. Aynı anahtarla imzalanmayan yeni bir APK, telefondaki eski sürümün üzerine kurulamaz; anahtarı ayrıca yedekleyin.
