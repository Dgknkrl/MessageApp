import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'service_bridge.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen>
    with WidgetsBindingObserver {
  final _bridge = const ServiceBridge();
  final _tokenController = TextEditingController();
  final _chatIdController = TextEditingController();
  final _formKey = GlobalKey<FormState>();
  ServiceStatus _status = ServiceStatus.empty;
  bool _loading = true;
  bool _busy = false;
  bool _hideToken = true;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _refresh(fillFields: true);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _tokenController.dispose();
    _chatIdController.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _refresh();
  }

  Future<void> _refresh({bool fillFields = false}) async {
    try {
      final status = await _bridge.getStatus();
      if (!mounted) return;
      setState(() {
        _status = status;
        _loading = false;
        if (fillFields) {
          _tokenController.text = status.botToken;
          _chatIdController.text = status.chatId;
        }
      });
    } on PlatformException catch (error) {
      if (!mounted) return;
      setState(() => _loading = false);
      _showError(error.message ?? 'Android bağlantısı kurulamadı.');
    } on MissingPluginException {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _perform(
    Future<void> Function() operation, {
    required String successMessage,
  }) async {
    if (_busy) return;
    setState(() => _busy = true);
    try {
      await operation();
      await _refresh();
      if (mounted) _showMessage(successMessage);
    } on PlatformException catch (error) {
      if (mounted) _showError(error.message ?? 'İşlem tamamlanamadı.');
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _save() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    await _perform(
      () async {
        _status = await _bridge.saveSettings(
          _tokenController.text.trim(),
          _chatIdController.text.trim(),
        );
      },
      successMessage: 'Telegram ayarları kaydedildi.',
    );
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(context)
        .showSnackBar(SnackBar(content: Text(message)));
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: Colors.red.shade700),
    );
  }

  Future<void> _showLogs() async {
    final logs = await _bridge.getLogs();
    if (!mounted) return;
    await showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      isScrollControlled: true,
      builder: (context) => SafeArea(
        child: SizedBox(
          height: MediaQuery.sizeOf(context).height * .65,
          child: Column(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 0, 8, 8),
                child: Row(
                  children: [
                    const Expanded(
                      child: Text(
                        'İşlem geçmişi',
                        style: TextStyle(
                            fontSize: 20, fontWeight: FontWeight.w700),
                      ),
                    ),
                    TextButton(
                      onPressed: logs.isEmpty
                          ? null
                          : () async {
                              await _bridge.clearLogs();
                              if (context.mounted) Navigator.pop(context);
                            },
                      child: const Text('Temizle'),
                    ),
                  ],
                ),
              ),
              Expanded(
                child: logs.isEmpty
                    ? const Center(child: Text('Henüz işlem kaydı yok.'))
                    : ListView.separated(
                        padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
                        itemCount: logs.length,
                        separatorBuilder: (_, __) => const Divider(height: 24),
                        itemBuilder: (_, index) => Text(logs[index]),
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('SMS ve çağrılar → Telegram'),
        actions: [
          IconButton(
            tooltip: 'İşlem geçmişi',
            onPressed: _showLogs,
            icon: const Icon(Icons.receipt_long_outlined),
          ),
          IconButton(
            tooltip: 'Yenile',
            onPressed: _refresh,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
              children: [
                _ServiceCard(status: _status),
                const SizedBox(height: 16),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(20),
                    child: Form(
                      key: _formKey,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          const Text(
                            'Telegram ayarları',
                            style: TextStyle(
                                fontSize: 19, fontWeight: FontWeight.w700),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            'BotFather token’ını ve mesajların gideceği chat ID’yi girin.',
                            style: TextStyle(color: Colors.grey.shade700),
                          ),
                          const SizedBox(height: 18),
                          TextFormField(
                            controller: _tokenController,
                            obscureText: _hideToken,
                            autocorrect: false,
                            enableSuggestions: false,
                            decoration: InputDecoration(
                              labelText: 'Bot token',
                              hintText: '123456:ABC…',
                              suffixIcon: IconButton(
                                onPressed: () =>
                                    setState(() => _hideToken = !_hideToken),
                                icon: Icon(
                                  _hideToken
                                      ? Icons.visibility_outlined
                                      : Icons.visibility_off_outlined,
                                ),
                              ),
                            ),
                            validator: (value) =>
                                value == null || value.trim().isEmpty
                                    ? 'Bot token gerekli.'
                                    : null,
                          ),
                          const SizedBox(height: 14),
                          TextFormField(
                            controller: _chatIdController,
                            keyboardType: TextInputType.number,
                            decoration: const InputDecoration(
                              labelText: 'Chat ID',
                              hintText: '-100… veya kullanıcı chat ID’si',
                            ),
                            validator: (value) =>
                                value == null || value.trim().isEmpty
                                    ? 'Chat ID gerekli.'
                                    : null,
                          ),
                          const SizedBox(height: 16),
                          FilledButton.icon(
                            onPressed: _busy ? null : _save,
                            icon: const Icon(Icons.save_outlined),
                            label: const Text('Ayarları kaydet'),
                          ),
                          const SizedBox(height: 8),
                          OutlinedButton.icon(
                            onPressed: _busy
                                ? null
                                : () => _perform(
                                      _bridge.testTelegram,
                                      successMessage:
                                          'Telegram test mesajı gönderildi.',
                                    ),
                            icon: const Icon(Icons.send_outlined),
                            label: const Text('Telegram bağlantısını test et'),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        const Text(
                          'Cihaz hazırlığı',
                          style: TextStyle(
                              fontSize: 19, fontWeight: FontWeight.w700),
                        ),
                        const SizedBox(height: 12),
                        _SetupRow(
                          complete: _status.smsPermission &&
                              _status.notificationPermission,
                          title: 'SMS ve bildirim izinleri',
                          actionLabel: 'İzin ver',
                          onPressed: _busy
                              ? null
                              : () => _perform(
                                    () async {
                                      await _bridge.requestPermissions();
                                    },
                                    successMessage: 'İzin durumu güncellendi.',
                                  ),
                        ),
                        const Divider(height: 28),
                        _SetupRow(
                          complete: _status.phonePermission &&
                              _status.callLogPermission,
                          title: 'Telefon ve arayan numara izinleri',
                          actionLabel: 'İzin ver',
                          onPressed: _busy
                              ? null
                              : () => _perform(
                                    () async {
                                      await _bridge.requestPermissions();
                                    },
                                    successMessage: 'İzin durumu güncellendi.',
                                  ),
                        ),
                        const Divider(height: 28),
                        _SetupRow(
                          complete: _status.contactsPermission,
                          title: 'Rehberdeki adları göster',
                          actionLabel: 'İzin ver',
                          onPressed: _busy
                              ? null
                              : () => _perform(
                                    () async {
                                      await _bridge.requestPermissions();
                                    },
                                    successMessage: 'İzin durumu güncellendi.',
                                  ),
                        ),
                        const SizedBox(height: 12),
                        const Text(
                          'Telefon izni çağrıları algılar; çağrı kaydı izni arayan numarayı alır. '
                          'Rehber izniyle kayıtlı ad ve numara Telegram sohbetinize gönderilir. '
                          'Rehber izni yoksa yalnızca numara, numara alınamazsa bilinmeyen arayan gösterilir. '
                          'Çağrı geçmişi okunmaz. Reddedilen izinleri cihazın uygulama ayarlarından açabilirsiniz.',
                          style: TextStyle(fontSize: 13),
                        ),
                        const Divider(height: 28),
                        _SetupRow(
                          complete: _status.batteryExempt,
                          title: 'Pil optimizasyonu muafiyeti',
                          actionLabel: 'Ayarı aç',
                          onPressed:
                              _busy ? null : _bridge.requestBatteryExemption,
                        ),
                        const SizedBox(height: 14),
                        Text(
                          'Xiaomi, Huawei ve Samsung cihazlarda ayrıca otomatik başlatmayı açın ve arka plan kısıtlamasını kaldırın.',
                          style: TextStyle(
                              fontSize: 13, color: Colors.grey.shade700),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 18),
                if (_status.enabled)
                  FilledButton.tonalIcon(
                    onPressed: _busy
                        ? null
                        : () => _perform(
                              _bridge.stopService,
                              successMessage:
                                  'SMS ve çağrı yönlendirme durduruldu.',
                            ),
                    icon: const Icon(Icons.stop_circle_outlined),
                    label: const Padding(
                      padding: EdgeInsets.symmetric(vertical: 13),
                      child: Text('Yönlendirmeyi durdur'),
                    ),
                  )
                else
                  FilledButton.icon(
                    onPressed: _busy
                        ? null
                        : () => _perform(
                              _bridge.startService,
                              successMessage:
                                  'SMS ve çağrı yönlendirme başlatıldı.',
                            ),
                    icon: const Icon(Icons.play_circle_outline),
                    label: const Padding(
                      padding: EdgeInsets.symmetric(vertical: 13),
                      child: Text('Yönlendirmeyi başlat'),
                    ),
                  ),
                const SizedBox(height: 12),
                const Text(
                  'Bot token, SMS içeriği, arayan adı ve numarası işlem geçmişinde gösterilmez. Ayarlar yalnızca bu cihazda saklanır.',
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 12),
                ),
              ],
            ),
    );
  }
}

class _ServiceCard extends StatelessWidget {
  const _ServiceCard({required this.status});

  final ServiceStatus status;

  @override
  Widget build(BuildContext context) {
    final active = status.enabled && status.serviceRunning;
    final color = active ? Colors.green.shade700 : Colors.orange.shade800;
    return Card(
      color: active ? Colors.green.shade50 : Colors.orange.shade50,
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Row(
          children: [
            Icon(active ? Icons.check_circle : Icons.pause_circle,
                color: color, size: 34),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    active ? 'Yönlendirme aktif' : 'Yönlendirme kapalı',
                    style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w700,
                        color: color),
                  ),
                  const SizedBox(height: 3),
                  Text(active
                      ? (status.phonePermission
                          ? 'Gelen SMS ve çağrı bildirimleri Telegram’a iletilecek.'
                          : 'SMS yönlendirme açık. Çağrılar için telefon izni verin.')
                      : 'Kurulumu tamamlayıp servisi başlatın.'),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SetupRow extends StatelessWidget {
  const _SetupRow({
    required this.complete,
    required this.title,
    required this.actionLabel,
    required this.onPressed,
  });

  final bool complete;
  final String title;
  final String actionLabel;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(
          complete ? Icons.check_circle : Icons.error_outline,
          color: complete ? Colors.green : Colors.orange.shade800,
        ),
        const SizedBox(width: 10),
        Expanded(
            child: Text(title,
                style: const TextStyle(fontWeight: FontWeight.w600))),
        if (!complete)
          TextButton(onPressed: onPressed, child: Text(actionLabel)),
      ],
    );
  }
}
