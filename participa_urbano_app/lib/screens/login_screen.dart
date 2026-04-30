import 'package:flutter/material.dart';
import '../services/api_service.dart';
import 'home_screen.dart';
import 'register_screen.dart';

class LoginScreen extends StatefulWidget {
  @override
  _LoginScreenState createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _emailController = TextEditingController();
  final _senhaController = TextEditingController();
  final _apiService = ApiService();
  bool _isLoading = false;

  void _login() async {
    setState(() => _isLoading = true);
    bool success = await _apiService.login(
      _emailController.text,
      _senhaController.text,
    );
    setState(() => _isLoading = false);

    if (success) {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (_) => HomeScreen()),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Erro ao logar. Verifique credenciais.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Participa Urbano - Login'), centerTitle: true),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.location_city, size: 80, color: Colors.green),
            SizedBox(height: 20),
            TextField(
              controller: _emailController,
              decoration: InputDecoration(labelText: 'E-mail', border: OutlineInputBorder()),
            ),
            SizedBox(height: 16),
            TextField(
              controller: _senhaController,
              obscureText: true,
              decoration: InputDecoration(labelText: 'Senha', border: OutlineInputBorder()),
            ),
            SizedBox(height: 20),
            _isLoading 
              ? CircularProgressIndicator()
              : ElevatedButton(
                  onPressed: _login,
                  child: Text('Entrar'),
                  style: ElevatedButton.styleFrom(minimumSize: Size(double.infinity, 50)),
                ),
            TextButton(
              onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => RegisterScreen())),
              child: Text('Criar nova conta'),
            )
          ],
        ),
      ),
    );
  }
}
