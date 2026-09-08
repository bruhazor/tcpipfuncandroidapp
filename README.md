# TCP/IP Hızlı Gönderim — Yerel Android sürümü

Bu sürüm Android'in yerel Java altyapısı ile hazırlandı. Kivy/Buildozer kullanmaz; Android SDK, AIDL ve NDK kaynaklı uzun Python paketleme sorunları yoktur.

## GitHub üzerinden APK oluşturma

1. GitHub'da yeni ve boş bir depo oluşturun.
2. Bu klasörün içindeki dosyaları deponun ana dizinine yükleyin. `.github/workflows/build-apk.yml` dosyası da yüklenmelidir.
3. **Actions** → **Android APK oluştur** → **Run workflow** seçin.
4. İşlem sonunda **Artifacts** bölümünden `TCP-Hizli-Gonderim-Android-APK` dosyasını indirin. ZIP'i açınca APK dosyası görünür.

Bu yapı GitHub'ın sağladığı Gradle 8.7 ve Java 17 ile derlenir. Gradle'ın resmi GitHub eylemi belirtilen sürümü indirip çalıştırma yoluna ekler; ilk derleme genellikle birkaç dakika sürer.
