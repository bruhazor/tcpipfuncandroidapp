# TCP/IP for PJLINK — Yerel Android sürümü

Bu sürüm Android'in yerel Java altyapısı ile hazırlandı. Kivy/Buildozer kullanmaz; Android SDK, AIDL ve NDK kaynaklı uzun Python paketleme sorunları yoktur.

## PJLink özellikleri

- Uygulama adı: **TCP/IP for PJLINK**
- PJLink Class 1 temel komutları: güç, giriş seçimi, AV mute, hata/lamba durumu, cihaz adı, üretici, model ve sınıf sorguları
- Her komut için **Kopyala** (CR sonlandırıcısı eklenir) ve **Gönder** düğmesi
- PJLink standart portu olan **4352** otomatik kullanılır
- Bağlantı başarılı/başarısız bilgisi ve projektörün PJLink yanıtı ekranda gösterilir
- Parola isteyen projektörlerde PJLink MD5 kimlik doğrulaması desteklenir
- PJLink sekmesinde minimal projektör illüstrasyonu

## GitHub üzerinden APK oluşturma

1. GitHub'da yeni ve boş bir depo oluşturun.
2. Bu klasörün içindeki dosyaları deponun ana dizinine yükleyin. `.github/workflows/build-apk.yml` dosyası da yüklenmelidir.
3. **Actions** → **Android APK oluştur** → **Run workflow** seçin.
4. İşlem sonunda **Artifacts** bölümünden `TCP-Hizli-Gonderim-Android-APK` dosyasını indirin. ZIP'i açınca APK dosyası görünür.

Bu yapı GitHub'ın sağladığı Gradle 8.7 ve Java 17 ile derlenir. Gradle'ın resmi GitHub eylemi belirtilen sürümü indirip çalıştırma yoluna ekler; ilk derleme genellikle birkaç dakika sürer.
