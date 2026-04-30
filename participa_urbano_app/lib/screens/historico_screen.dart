import 'package:flutter/material.dart';
import '../services/api_service.dart';

class HistoricoScreen extends StatefulWidget {
  @override
  _HistoricoScreenState createState() => _HistoricoScreenState();
}

class _HistoricoScreenState extends State<HistoricoScreen> {
  final ApiService _apiService = ApiService();
  late Future<List<dynamic>> _ocorrenciasFuture;

  @override
  void initState() {
    super.initState();
    _ocorrenciasFuture = _apiService.getMinhasOcorrencias();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Meus Chamados'), backgroundColor: Colors.green),
      body: FutureBuilder<List<dynamic>>(
        future: _ocorrenciasFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return Center(child: CircularProgressIndicator(color: Colors.green));
          } else if (snapshot.hasError) {
            return Center(child: Text('Erro ao carregar histórico: ${snapshot.error}'));
          } else if (!snapshot.hasData || snapshot.data!.isEmpty) {
            return Center(child: Text('Você não tem nenhuma ocorrência registrada.'));
          }

          final ocorrencias = snapshot.data!;
          return ListView.builder(
            itemCount: ocorrencias.length,
            itemBuilder: (context, index) {
              final oc = ocorrencias[index];
              return Card(
                margin: EdgeInsets.all(8),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10), side: BorderSide(color: Colors.green, width: 1)),
                child: ListTile(
                  leading: Icon(Icons.report, color: Colors.green, size: 40),
                  title: Text(oc['descricao'] ?? 'Sem descrição', style: TextStyle(fontWeight: FontWeight.bold)),
                  subtitle: Text('Status: ${oc['status']} | Prioridade: ${oc['prioridade']}'),
                ),
              );
            },
          );
        },
      ),
    );
  }
}
