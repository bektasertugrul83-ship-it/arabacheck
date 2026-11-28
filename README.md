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

## Yapay zekâ ile ilan okuma (isteğe bağlı)

Ekran görüntüleri Claude (Anthropic) ile okunabilir: marka, model, fiyat, km, boya/değişen şeması, Tramer, donanım ve satıcı notları.

- Uygulamada **"Yapay zekâ ayarı"** → kendi Anthropic API anahtarınızı girin (console.anthropic.com → API Keys). Anahtar yalnızca cihazda saklanır.
- Model: `claude-opus-5-5`, düşük düşünme seviyesi, JSON şemalı yapılandırılmış çıktı; güvenlik filtresi reddederse sunucu tarafında yedek model (`fallbacks: "default"`).
- Görüntüler uzun kenarı 1568 px JPEG'e küçültülüp gönderilir; okuma başına maliyet uygulamada gösterilir (yaklaşık birkaç sent).
- Anahtar yoksa, internet yoksa veya hata olursa cihazdaki yazı tanımaya (Tesseract) otomatik geçilir.
- SDK: `vendor/anthropic-sdk.js` — `@anthropic-ai/sdk` 1.8.0'ın esbuild ile hazırlanmış tarayıcı paketi.

**Not:** Anahtarı uygulamanın içine gömüp dağıtmayın. Uygulamayı herkese açacaksanız anahtarı tutan küçük bir sunucu (proxy) kullanın.
