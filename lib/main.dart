import 'package:flutter/material.dart';

void main() => runApp(const MessageApp());

class MessageApp extends StatelessWidget {
  const MessageApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'MessageApp',
      theme: ThemeData(colorSchemeSeed: Colors.blue, useMaterial3: true),
      home: const Scaffold(
        appBar: AppBar(title: Text('MessageApp')),
        body: Center(
          child: Padding(
            padding: EdgeInsets.all(24),
            child: Text(
              'SMS → Native Receiver → Foreground Service → Telegram',
              textAlign: TextAlign.center,
            ),
          ),
        ),
      ),
    );
  }
}

