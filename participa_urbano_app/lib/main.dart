import 'package:flutter/material.dart';
import 'screens/login_screen.dart';

void main() {
  runApp(ParticipaUrbanoApp());
}

class ParticipaUrbanoApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Participa Urbano',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        primarySwatch: Colors.green,
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.green),
        useMaterial3: true,
      ),
      home: LoginScreen(),
    );
  }
}
