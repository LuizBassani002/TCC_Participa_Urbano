import 'package:flutter/material.dart';
import '../services/api_service.dart';

class OcorrenciasPublicasScreen extends StatefulWidget {
  @override
  _OcorrenciasPublicasScreenState createState() => _OcorrenciasPublicasScreenState();
}

class _OcorrenciasPublicasScreenState extends State<OcorrenciasPublicasScreen> {
  final ApiService _apiService = ApiService();
  late Future<List<dynamic>> _ocorrenciasFuture;

  @override
  void initState() {
    super.initState();
    _ocorrenciasFuture = _apiService.getOcorrenciasPublicas();
  }

  Color _getStatusColor(String status) {
    switch (status) {
      case 'RESOLVIDA': return Colors.green;
      case 'EM_ANDAMENTO': return Colors.orange;
      case 'ABERTA': return Colors.blue;
      default: return Colors.grey;
    }
  }

  IconData _getStatusIcon(String status) {
    switch (status) {
      case 'RESOLVIDA': return Icons.check_circle;
      case 'EM_ANDAMENTO': return Icons.autorenew;
      case 'ABERTA': return Icons.info;
      default: return Icons.hourglass_empty;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Ocorrências da Cidade'), backgroundColor: Colors.green),
      body: FutureBuilder<List<dynamic>>(
        future: _ocorrenciasFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return Center(child: CircularProgressIndicator(color: Colors.green));
          } else if (snapshot.hasError) {
            return Center(child: Text('Erro ao carregar.'));
          } else if (!snapshot.hasData || snapshot.data!.isEmpty) {
            return Center(child: Text('Nenhuma ocorrência aprovada ainda.'));
          }

          final ocorrencias = snapshot.data!;
          return ListView.builder(
            itemCount: ocorrencias.length,
            itemBuilder: (context, index) {
              final oc = ocorrencias[index];
              final String status = oc['status'] ?? 'ABERTA';
              final String categoria = oc['categoria'] ?? '';

              return Card(
                margin: EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                child: ListTile(
                  leading: Icon(_getStatusIcon(status), color: _getStatusColor(status), size: 35),
                  title: Text(oc['descricao'] ?? '', style: TextStyle(fontWeight: FontWeight.bold)),
                  subtitle: Text('Categoria: $categoria\nStatus: $status'),
                  trailing: Chip(
                    label: Text(status, style: TextStyle(color: Colors.white, fontSize: 10)),
                    backgroundColor: _getStatusColor(status),
                  ),
                ),
              );
            },
          );
        },
      ),
    );
  }
}
