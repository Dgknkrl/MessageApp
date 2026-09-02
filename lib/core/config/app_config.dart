final class AppConfig {
  const AppConfig._();

  static const telegramBotToken = String.fromEnvironment('TELEGRAM_BOT_TOKEN');
  static const telegramChatId = String.fromEnvironment('TELEGRAM_CHAT_ID');

  static bool get hasTelegramConfig =>
      telegramBotToken.isNotEmpty && telegramChatId.isNotEmpty;
}

