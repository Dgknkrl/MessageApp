# MessageApp

MessageApp, Android cihaza gelen SMS mesajlarını yerel olarak yakalayıp kullanıcının kendi Telegram hesabına iletmeyi amaçlayan Flutter + Android Native başlangıç projesidir.

## Hedef akış

```text
SMS_RECEIVED
    ↓
Android BroadcastReceiver
    ↓
Foreground Service
    ↓ HTTPS POST
Telegram Bot API
    ↓
Kullanıcının Telegram sohbeti
```

## Mimari

- `lib/`: Flutter arayüzü, izin durumu ve uygulama ayarları.
- `android/app/src/main/kotlin/.../sms/`: SMS yayınını alan native katman.
- `android/app/src/main/kotlin/.../service/`: Mesajı foreground service içinde işleyen katman.
- `android/app/src/main/kotlin/.../telegram/`: Telegram Bot API istemcisi.
- `docs/architecture.md`: Akış, güvenlik sınırları ve geliştirme notları.
- `config/example.json`: Repoya eklenebilen, sahte değerli yerel ayar şablonu.

## Güvenli yerel yapılandırma

Gerçek bot token veya chat ID hiçbir zaman commit edilmemelidir. Örnek dosyayı kopyalayın:

```powershell
Copy-Item config/example.json config/local.json
```

`config/local.json`, `.env`, `android/local.properties` ve imzalama dosyaları `.gitignore` içindedir. CI/CD kullanılırsa değerleri GitHub Actions Secrets içinde saklayın.

> Önemli: Bir sır doğrudan Android uygulamasına gömülürse APK içinden çıkarılabilir. Üretim için önerilen tasarım, bot token'ını yalnızca güvenilir bir backend/proxy üzerinde tutmak ve mobil uygulamanın bu servise kimliği doğrulanmış istek göndermesidir. Doğrudan Telegram yaklaşımı yalnızca kişisel, kontrollü dağıtım için düşünülmelidir.

## Başlangıç

1. Flutter SDK ve Android Studio'yu kurun.
2. `config/example.json` dosyasını `config/local.json` olarak kopyalayıp yerel değerleri girin.
3. Android tarafında `RECEIVE_SMS`, bildirim ve foreground service izinlerini çalışma zamanında yönetin.
4. Uygulamayı fiziksel bir Android cihazda test edin; emülatörde SMS davranışı cihazdan farklı olabilir.

```powershell
flutter pub get
flutter run --dart-define-from-file=config/local.json
```

## Android izinleri ve mağaza politikası

SMS izinleri yüksek hassasiyetlidir. Google Play dağıtımı planlanıyorsa güncel SMS/Call Log izin politikaları incelenmeli ve uygulamanın uygun kullanım kategorisine girdiği doğrulanmalıdır. Kullanıcıdan açık rıza alınmalı; mesaj içeriği loglara veya analiz servislerine yazılmamalıdır.

## Durum

Bu depo başlangıç mimarisidir. Native sınıflar sorumlulukları ve entegrasyon noktalarını gösterir; gerçek cihaz izin akışı, kalıcı bildirim kanalı, hata/yeniden deneme politikası ve güvenli backend seçimi sonraki geliştirme adımlarıdır.

