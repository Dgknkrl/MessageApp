# MessageApp

MessageApp, Android telefona gelen SMS'leri ve çağrı bildirimlerini native Kotlin katmanında yakalayıp kullanıcının kendi Telegram sohbetine ileten Flutter uygulamasıdır. Çağrılar telefon çalarken, varsa rehberdeki kayıtlı ad ve numarayla bildirilir. Bot token ve chat ID kaynak kodda bulunmaz; uygulama ekranından kullanıcı tarafından girilir ve cihazdaki özel uygulama tercihleri içinde saklanır.

## Özellikler

- Foreground service içinde dinamik `SMS_RECEIVED` receiver
- Parçalı SMS'leri tek mesaj olarak birleştirme
- Manifest kayıtlı `PHONE_STATE` receiver ile gelen çağrı bildirimi; çağrı geçmişi sorgulanmaz
- Rehberdeki ad, numara ve olay zamanı; cevaplandı veya cevapsız/reddedildi durum mesajları
- Tekrarlanan çağrı yayınlarını eleme ve süreç yeniden oluşturulduğunda çağrı durumunu koruma
- OkHttp ile Telegram Bot API'ye HTTPS POST
- Ağ hatalarında en fazla 100 mesajlık kalıcı kuyruk ve otomatik yeniden deneme
- Telefon açıldığında, daha önce etkinleştirilmişse servisi yeniden başlatma
- SMS, bildirim ve pil optimizasyonu izin akışı
- Token veya SMS metni içermeyen yerel işlem geçmişi
- Servisi kalıcı bildirimden veya uygulamadan durdurma

## Kullanım

1. Uygulamayı fiziksel Android telefona kurun ve açın.
2. BotFather'dan aldığınız bot token'ını ve hedef Telegram chat ID'yi girip **Ayarları kaydet** seçeneğine dokunun.
3. **Telegram bağlantısını test et** ile hedef sohbete test mesajı gönderin.
4. SMS ve bildirim izinlerini verin. Çağrılar için telefon (`READ_PHONE_STATE`), arayan numara için çağrı kaydı (`READ_CALL_LOG`), kayıtlı ad için rehber (`READ_CONTACTS`) izinlerini de onaylayın. Güncelleme sonrası uygulamayı açıp yeni izinleri verin.
5. Pil optimizasyonu muafiyetini onaylayın.
6. **Yönlendirmeyi başlat** seçeneğine dokunun.

Xiaomi, Huawei ve Samsung gibi cihazlarda sistem ayarlarından otomatik başlatmayı ayrıca açmanız ve uygulamanın arka plan kullanımını “kısıtlanmamış” yapmanız gerekebilir.

## Geliştirme ve doğrulama

```powershell
flutter pub get
flutter analyze
flutter test
flutter build apk --debug
```

Android çağrı durumu birim testleri ve lint kontrolü (proje kökünden):

```powershell
cd android
.\gradlew.bat app:testDebugUnitTest app:lintDebug
cd ..
```

Oluşturulan debug APK:

```text
build/app/outputs/flutter-apk/app-debug.apk
```

### Son doğrulama — 3 Eylül 2026

- Flutter statik analizi: sorun bulunmadı.
- Flutter testleri: 3 test başarılı.
- Android çağrı durumu birim testleri: 4 test başarılı.
- Android debug APK derlemesi: başarılı.
- Android lint: 0 hata, 14 engelleyici olmayan uyarı (Kotlin stil önerileri, senkron tercih kaydı, kaynak klasörü ve bağımlılık sürümü).
- Yayın öncesi staged kaynak dosyalarında ve Git geçmişinde bilinen gizli anahtar kalıpları tarandı; gerçek Telegram token/chat ID kaynak koduna eklenmedi.

Bu kontroller fiziksel cihazda uçtan uca SMS/çağrı teslimini doğrulamaz. Gerçek Telegram bilgileriyle aşağıdaki cihaz testlerini tamamlayın. Debug APK geliştirme imzası kullanır; mağaza yayını için uygun release imzası ve izin politikası değerlendirmesi gerekir.

Windows'ta Flutter tarafından oluşturulan `android/local.properties` lint sırasında `PropertyEscape` hatası verirse sürücü harfinden sonraki iki noktayı kaçırın (örneğin `C\:/Android/Sdk`). Dosyadaki kendi SDK yollarınızı koruyun ve bu yerel dosyayı commit etmeyin. Flutter dosyayı yeniden oluşturduğunda düzeltme tekrar gerekebilir.

## Android telefona yükleme

### 1. Telefonu hazırlayın

1. Telefonda **Ayarlar → Telefon hakkında** bölümünü açın.
2. **Yapım numarası** üzerine 7 kez dokunarak geliştirici seçeneklerini etkinleştirin.
3. **Geliştirici seçenekleri → USB hata ayıklama** ayarını açın.
4. Telefonu USB kablosuyla bilgisayara bağlayın.
5. Telefonda görünen USB hata ayıklama yetkilendirmesini onaylayın.

### 2. ADB bağlantısını doğrulayın

Komut `adb device` değil, `adb devices` şeklinde çoğuldur. ADB PATH içinde değilse Windows'ta Android SDK içindeki tam yolu kullanın:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
```

Çıktıda cihazın yanında `device` görünmelidir. `unauthorized` görünürse telefon ekranındaki yetkilendirme penceresini onaylayıp komutu yeniden çalıştırın.

ADB'yi yalnızca mevcut PowerShell oturumu için PATH'e eklemek isterseniz:

```powershell
$env:Path += ";$env:LOCALAPPDATA\Android\Sdk\platform-tools"
adb devices
```

### 3. APK'yı yükleyin

Proje klasöründeyken:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "C:\dev\MessageApp\build\app\outputs\flutter-apk\app-debug.apk"
```

ADB PATH'e eklendiyse kısa biçimi de kullanılabilir:

```powershell
adb install -r "C:\dev\MessageApp\build\app\outputs\flutter-apk\app-debug.apk"
```

Başarılı kurulum `Success` çıktısı verir. Uygulamayı komutla açmak için:

```powershell
adb shell monkey -p com.example.message_app -c android.intent.category.LAUNCHER 1
```

Alternatif olarak bağlı cihaza Flutter üzerinden kurabilirsiniz:

```powershell
flutter devices
flutter install
```

Kurulumdan sonra uygulamayı açın; Telegram bilgilerini girin, izinleri ve pil optimizasyonu muafiyetini onaylayın, ardından yönlendirmeyi başlatın.

Gerçek cihaz testi için başka bir telefondan SMS gönderin. Uygulama logları yalnızca “SMS alındı”, “iletildi” veya “kuyrukta” gibi operasyonel durumları gösterir; gönderen, mesaj metni, bot token ve chat ID loglanmaz.

### Çağrı doğrulaması

- Rehbere kayıtlı bir numarayla arayın: telefon çalarken Telegram'da kayıtlı ad, numara ve saat görünmeli; tek bir ilk bildirim gelmeli.
- Çağrıyı yanıtlayın: bir kez “Cevaplandı” mesajı gelmeli; kapatınca cevapsız mesajı gelmemeli.
- Başka bir çağrıyı yanıtlamayın veya reddedin: bir kez “Cevapsız / reddedildi” gelmeli. `PHONE_STATE` bu iki sonucu kesin olarak ayırmaz.
- Kayıtsız numara, gizli numara ve rehber izni reddi durumlarını deneyin. Ad bulunamazsa numara; numara yoksa “Gizli / bilinmeyen numara” kullanılır.
- İnterneti kapatıp arayın, ardından bağlantıyı açın: kuyruktaki bildirimler servis çalışırken yeniden denemede gönderilmeli. Gerçek gönderim zamanı ağ/kuyruk durumuna bağlıdır.
- Yönlendirmeyi durdurun: yeni çağrılar kuyruğa alınmamalı. Daha önce kuyruğa alınmış mesajlar mevcut yeniden deneme davranışına göre gönderilebilir.
- Ekran kapalıyken, uygulama son uygulamalardan kaldırılmışken ve telefon yeniden başlatıldıktan sonra deneyin. Android'in “Zorla durdur” işlemi sonrası uygulamayı tekrar açmak gerekir.

Telefon izni olmadan SMS yönlendirme kullanılabilir. Rehber izni olmadan ad çözümlenmez. `READ_CALL_LOG` yalnızca telefon durumu yayınındaki numara için istenir; geçmiş aramalar okunmaz. Bazı cihazların numara yayınını kısıtlaması halinde ad/numara alınamayabilir; numara içeren yayın hiç gelmezse bu akış çağrıyı kaçırabilir. Eşzamanlı çift SIM ve çağrı bekletme senaryoları tek çağrı durumundan kesin ayrıştırılmaz; hedef cihazda ayrıca test edilmelidir. WhatsApp/Telegram gibi internet aramaları kapsam dışıdır.

## Güvenlik ve dağıtım notu

Bu doğrudan Telegram yaklaşımı kişisel ve kontrollü dağıtım içindir. Bot token uygulama verilerinde tutulduğundan root erişimli veya ele geçirilmiş bir cihazdan çıkarılabilir. Daha geniş dağıtım için token'ı mobil uygulamada tutmayan, kimliği doğrulanmış bir backend/proxy tercih edilmelidir.

Google Play, SMS izinlerini yüksek hassasiyetli kabul eder. Mağaza dağıtımından önce güncel SMS/Call Log izin politikasına uygunluk ayrıca doğrulanmalıdır. Release dağıtımı için `android/app/build.gradle.kts` içindeki debug imzalama ayarı kendi release keystore yapılandırmanızla değiştirilmelidir.

### Public depoya kesinlikle eklenmemesi gerekenler

- Telegram bot token ve gerçek chat ID
- Yakalanmış SMS içerikleri, gönderen numaraları ve uygulamanın çalışma zamanı kuyruğu
- Rehber adları, arayan numaralar, çağrı durumları ve cihazdan dışa aktarılmış uygulama verileri
- `.env`, `config/local.json`, `android/local.properties` ve `android/key.properties`
- Android keystore dosyaları ve parolaları (`.jks`, `.keystore`)
- API anahtarları, GitHub tokenları, servis hesabı JSON dosyaları ve özel anahtarlar

Bilinen yerel ayar ve anahtar dosyaları `.gitignore` ile kapsam dışında tutulur. Bu koruma dosya adlarına dayanır; kaynak koda veya farklı adlı dosyalara yazılmış sırları otomatik olarak engellemez. Her yayın öncesi staged dosyalar ve Git geçmişi ayrıca taranmalıdır. Uygulamaya girilen Telegram bilgileri Git deposuna yazılmaz; yalnızca Android cihazın özel uygulama verisinde saklanır. Android yedekleme ve cihaz aktarım kuralları uygulama verilerini yedek dışında bırakır.
