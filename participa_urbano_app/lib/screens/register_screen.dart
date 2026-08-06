import 'package:flutter/material.dart';
import '../services/api_service.dart';

class RegisterScreen extends StatefulWidget {
  final bool isGestor;

  const RegisterScreen({Key? key, this.isGestor = false}) : super(key: key);

  @override
  _RegisterScreenState createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _nomeController = TextEditingController();
  final _emailController = TextEditingController();
  final _senhaController = TextEditingController();
  final _codigoPrefeituraController = TextEditingController();
  final _apiService = ApiService();
  bool _isLoading = false;

  void _register() async {
    if (_nomeController.text.isEmpty || _emailController.text.isEmpty || _senhaController.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Preencha Nome, E-mail e Senha!')));
      return;
    }

    if (widget.isGestor && _codigoPrefeituraController.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('O código da prefeitura é obrigatório para gestores!')));
      return;
    }

    setState(() => _isLoading = true);

    Map<String, dynamic> result = await _apiService.register(
      _nomeController.text,
      _emailController.text,
      _senhaController.text,
      widget.isGestor ? _codigoPrefeituraController.text : '',
    );

    setState(() => _isLoading = false);

    if (result['success'] == true) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(result['message']), backgroundColor: Colors.green));
      Navigator.pop(context);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(result['message']), backgroundColor: Colors.red));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.isGestor ? 'Cadastro de Gestor' : 'Cadastro de Cidadão'),
        backgroundColor: widget.isGestor ? Colors.blue[700] : Colors.green[700],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: SingleChildScrollView(
          child: Column(
            children: [
              Icon(
                widget.isGestor ? Icons.admin_panel_settings : Icons.person,
                size: 80,
                color: widget.isGestor ? Colors.blue : Colors.green,
              ),
              SizedBox(height: 20),
              TextField(controller: _nomeController, decoration: InputDecoration(labelText: 'Nome Completo', border: OutlineInputBorder())),
              SizedBox(height: 12),
              TextField(controller: _emailController, decoration: InputDecoration(labelText: 'E-mail', border: OutlineInputBorder())),
              SizedBox(height: 12),
              TextField(controller: _senhaController, obscureText: true, decoration: InputDecoration(labelText: 'Senha', border: OutlineInputBorder())),
              if (widget.isGestor) ...[
                SizedBox(height: 12),
                TextField(
                  controller: _codigoPrefeituraController, 
                  decoration: InputDecoration(
                    labelText: 'Código da Prefeitura', 
                    helperText: 'Código fornecido pela administração.',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.vpn_key),
                  ),
                ),
              ],
              SizedBox(height: 20),
              _isLoading
                ? CircularProgressIndicator()
                : ElevatedButton(
                    onPressed: _register,
                    child: Text(widget.isGestor ? 'Registrar como Gestor' : 'Registrar como Cidadão'),
                    style: ElevatedButton.styleFrom(
                      minimumSize: Size(double.infinity, 50),
                      backgroundColor: widget.isGestor ? Colors.blue : Colors.green,
                    ),
                  )
            ],
          ),
        ),
      ),
    );
  }
}
