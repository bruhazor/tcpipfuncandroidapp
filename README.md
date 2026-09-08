# Android sürümü

Bu klasör, Windows uygulamasından bağımsız Python/Kivy mobil kaynak kodunu içerir. Android'de aynı özellikler bulunur: manuel TCP gönderimi, birden çok IP'li sabit hızlı buton, düzenleme ve silme.

## APK oluşturma: GitHub ile, Linux gerektirmeden

1. GitHub'da **New repository** ile boş bir depo oluşturun. Depo adı örneğin `tcp-hizli-gonderim-android` olabilir.
2. Bu `android_tcp_hizli_gonderim` klasörünün **içindeki tüm dosyaları**, özellikle gizli `.github` klasörünü de koruyarak, GitHub deposunun ana dizinine yükleyin.
3. GitHub'da deponun **Actions** sekmesine girin ve sol taraftan **Android APK oluştur** iş akışını seçin.
4. **Run workflow** düğmesine basın. Derleme GitHub'ın bulut bilgisayarında yaklaşık 10-25 dakika sürer.
5. İşlem bittiğinde aynı sayfanın en altındaki **Artifacts** bölümünden `TCP-Hizli-Gonderim-APK` dosyasını indirin. ZIP dosyasını açınca APK içinde olur.
6. APK'yı Android telefonunuza aktarın ve yükleyin. Gerekirse Android, kullandığınız dosya yöneticisine “bilinmeyen uygulama yükleme” izni vermenizi ister.

> İlk derleme daha uzun sürebilir. GitHub'ın ücretsiz kotası kişisel ve açık depolar için bu küçük uygulama açısından genellikle yeterlidir.

## APK oluşturma: WSL/Linux alternatifi

APK üretimi Windows'ta doğrudan desteklenmez; WSL Ubuntu veya bir Linux bilgisayarda aşağıdakileri çalıştırın:

```bash
sudo apt update
sudo apt install -y python3-pip openjdk-17-jdk git zip unzip
pip install buildozer cython
buildozer android debug
```

Oluşan APK `bin/` klasöründe olur. Telefona yüklemek için Android ayarlarından ilgili dosya yöneticisi için “bilinmeyen uygulama yükleme” iznini açın.

`buildozer.spec` içinde `android.permissions = INTERNET` tanımlıdır; TCP bağlantıları için gereklidir.
