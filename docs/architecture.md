# Mimari

## Çalışma akışı

1. Flutter ekranı token, chat ID, izinler ve etkinlik durumunu `MethodChannel` üzerinden native katmana iletir.
2. Kullanıcı yönlendirmeyi açınca `SmsForwardingService` foreground olarak başlar ve kalıcı bildirim gösterir.
3. Servis `SmsReceiver` bileşenini çalışma zamanında kaydeder.
4. Receiver, gelen intent içindeki parçalı SMS'leri birleştirip `TelegramSender` kuyruğuna ekler.
5. `TelegramSender`, OkHttp ile Telegram Bot API `sendMessage` endpoint'ine form POST gönderir.
6. Ağ veya API hatasında kayıt cihazdaki sınırlı kuyrukta kalır ve 60 saniye sonra yeniden denenir.
7. `BootReceiver`, kullanıcı daha önce yönlendirmeyi etkin bırakmışsa telefon açıldığında servisi yeniden başlatır.

## Anlık çağrı akışı

1. Manifest kayıtlı `PhoneStateBroadcastReceiver`, `PHONE_STATE` yayınını alır. Kullanıcı yönlendirmeyi açmamışsa hiçbir çağrı işlenmez.
2. `RINGING` anında `EXTRA_INCOMING_NUMBER` kullanılır; CallLog/ContentObserver sorgusu yapılmaz. `TelephonyCallback` numara sağlamadığı için ikinci bir çağrı dinleyicisi kullanılmaz.
3. Android, telefon ve çağrı kaydı izinleri birlikte verildiğinde aynı durumu iki kez, belirsiz sırada yayınlar. Numaralı izin varsa `RINGING` için extra anahtarı bulunmayan kopya atlanır; anahtarın mevcut ama değerinin boş olması gizli/bilinmeyen numara olarak işlenir. Çağrı kaydı izni yoksa numarasız yayın işlenir.
4. `goAsync()` ile ana iş parçacığından çıkılır; tek bir alıcı işçisi olay sırasını korur. `ContactResolver`, `PhoneLookup.CONTENT_FILTER_URI` üzerinden sadece arayan numaranın kayıtlı adını sorgular. İzin yoksa veya sorgu başarısızsa numaraya geri dönülür.
5. `CallStateTracker` tekrarları eler. Durum ve kişi bilgisi `call_state` tercihlerinde süreç ömründen bağımsız tutulur. Çağrı sona erince kişi bilgisi temizlenir; yönlendirme durdurulunca ve cihaz açılınca durum sıfırlanır.
6. İlk mesaj “📞 Gelen arama”, kayıtlı ad (varsa), numara ve olay zamanını içerir. `RINGING → OFFHOOK` için “✅ Cevaplandı”; `RINGING → IDLE` için “❌ Cevapsız / reddedildi” gönderilir. Giden çağrılar bildirilmez.
7. Çağrı mesajı alıcı tamamlanmadan kalıcı Telegram kuyruğuna yazılır. Ağ gönderimi ayrı işçide yürür; önceki mesajlar/ağ sorunları gerçek teslimi geciktirebilir. Kuyruk mevcut servis başlatma ve yeniden deneme mekanizmasını kullanır; uygulama süreci öldürülürse sonraki servis başlangıcı/olay gelene kadar yeniden deneme garanti değildir.

Kaynaklar: [Android telefon durumu yayın sözleşmesi](https://developer.android.com/reference/android/telephony/TelephonyManager#ACTION_PHONE_STATE_CHANGED), [rehber numarası eşleştirme](https://developer.android.com/reference/android/provider/ContactsContract.PhoneLookup).

### Sınırlar

- Reddedilme ile cevapsızlık telefon durumu yayınından kesin olarak ayırt edilemez.
- Tek etkin çağrı durumu izlenir; eşzamanlı çift SIM/çağrı bekletme için hat bazlı ayrıştırma yapılmaz.
- Numaralı yayını hiç sağlamayan üretici sürümlerinde çağrı kaçabilir; geçmişteki son aramadan numara tahmini yapılmaz (yanlış kişiyi bildirebilir).
- Kuyruk ve çağrı durumu ayrı tercihlerdedir; ikisinin yazılması arasındaki süreç çökmesinde tam-bir-kez teslim garantisi yoktur. Telegram isteğinin ulaşıp cevabın kaybolduğu ağ hataları da yeniden denemede tekrar oluşturabilir.

## Veri sınırları

- `app_prefs`: bot token, chat ID ve kullanıcının etkinleştirme tercihi.
- `telegram_queue`: henüz iletilememiş en fazla 100 SMS/çağrı bildirimi; çağrı adı ve numarası içerebilir. Başarılı gönderimden sonra silinir. Yeni girdiler `id`/`text` biçimindedir; eski `sender`/`body` girdileri gönderilirken okunmaya devam eder.
- `call_state`: etkin çağrının son durumu, numarası ve çözümlenen adı. `IDLE`, kullanıcı durdurması ve yeniden başlatmada temizlenir.
- `app_logs`: en fazla 100 operasyonel kayıt. SMS göndereni/içeriği, arayan adı/numarası, token ve chat ID içermez.
- Tüm Telegram trafiği HTTPS üzerinden gider; cleartext trafik manifestte kapalıdır.

## Android bileşenleri

- `MainActivity.kt`: MethodChannel, runtime izinleri ve pil optimizasyonu ekranı.
- `SmsForwardingService.kt`: foreground yaşam döngüsü, bildirim ve dinamik receiver kaydı.
- `SmsReceiver.kt`: SMS intent doğrulama ve multipart birleştirme.
- `PhoneStateBroadcastReceiver.kt`: manifest üzerinden çağrı durumunu alma ve arka planda işleme.
- `CallStateTracker.kt` / `CallStateStore.kt`: test edilebilir geçişler, tekrar eleme ve kalıcı çağrı durumu.
- `ContactResolver.kt`: rehber izni kontrolü ve numara üzerinden kayıtlı ad sorgusu.
- `TelegramSender.kt`: HTTPS gönderimi, kalıcı kuyruk ve retry.
- `BootReceiver.kt`: yeniden başlatma sonrası servis devamlılığı.
- `AppPreferences.kt` / `LogStore.kt`: cihaz içi ayar ve gizlilik odaklı log depoları.
