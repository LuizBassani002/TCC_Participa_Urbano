import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:jwt_decoder/jwt_decoder.dart';
import '../services/api_service.dart';
import 'login_screen.dart';
import 'create_occurrence_screen.dart';
import 'historico_screen.dart';
import 'gestao_dashboard_screen.dart';
import 'mapa_calor_screen.dart';
import 'ocorrencias_publicas_screen.dart';

class HomeScreen extends StatefulWidget {
  @override
  _HomeScreenState createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final ApiService _apiService = ApiService();
  final storage = const FlutterSecureStorage();
  
  String _userRole = '';
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadUserRole();
  }

  void _loadUserRole() async {
    String? token = await storage.read(key: 'jwt');
    if (token != null) {
      Map<String, dynamic> decodedToken = JwtDecoder.decode(token);
      setState(() {
        _userRole = decodedToken['role']; // Will be ROLE_CIDADAO or ROLE_GESTOR
        _isLoading = false;
      });
    } else {
      _logout();
    }
  }

  void _logout() async {
    await _apiService.logout();
    Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => LoginScreen()));
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(_userRole == 'ROLE_GESTOR' ? 'Painel do Gestor' : 'Portal do Cidadão'),
        backgroundColor: _userRole == 'ROLE_GESTOR' ? Colors.blue[800] : Colors.green,
        actions: [
          IconButton(icon: Icon(Icons.logout), onPressed: _logout),
        ],
      ),
      body: _userRole == 'ROLE_GESTOR' ? _buildGestorDashboard() : _buildCitizenDashboard(context),
    );
  }

  Widget _buildCitizenDashboard(BuildContext context) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.person, size: 100, color: Colors.green),
          SizedBox(height: 20),
          Text('Seja um Cidadão Ativo!', style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
          SizedBox(height: 40),
          ElevatedButton.icon(
            icon: Icon(Icons.warning),
            label: Text('Novo Relato Ocorrência'),
            style: ElevatedButton.styleFrom(
              padding: EdgeInsets.symmetric(horizontal: 30, vertical: 15),
              backgroundColor: Colors.green,
            ),
            onPressed: () {
              Navigator.push(context, MaterialPageRoute(builder: (_) => CreateOccurrenceScreen()));
            },
          ),
          SizedBox(height: 15),
          OutlinedButton.icon(
            icon: Icon(Icons.history),
            label: Text('Meus Chamados'),
            onPressed: () {
              Navigator.push(context, MaterialPageRoute(builder: (_) => HistoricoScreen()));
            },
          ),
          SizedBox(height: 15),
          OutlinedButton.icon(
            icon: Icon(Icons.public),
            label: Text('Ocorrências da Cidade'),
            onPressed: () {
              Navigator.push(context, MaterialPageRoute(builder: (_) => OcorrenciasPublicasScreen()));
            },
          )
        ],
      ),
    );
  }

  Widget _buildGestorDashboard() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.dashboard, size: 100, color: Colors.blue[800]),
          SizedBox(height: 20),
          Text('Dashboard de Gestão', style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
          SizedBox(height: 40),
          ElevatedButton.icon(
            icon: Icon(Icons.list_alt),
            label: Text('Lista de Prioridades da Cidade'),
            style: ElevatedButton.styleFrom(
              padding: EdgeInsets.symmetric(horizontal: 30, vertical: 15),
              backgroundColor: Colors.blue[800],
            ),
            onPressed: () {
              Navigator.push(context, MaterialPageRoute(builder: (_) => GestaoDashboardScreen()));
            },
          ),
          SizedBox(height: 15),
          OutlinedButton.icon(
            icon: Icon(Icons.map),
            label: Text('Mapa de Calor'),
            onPressed: () {
              Navigator.push(context, MaterialPageRoute(builder: (_) => MapaCalorScreen()));
            },
          )
        ],
      ),
    );
  }
}
