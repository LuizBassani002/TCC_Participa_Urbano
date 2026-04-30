import 'package:flutter/material.dart';
import '../services/api_service.dart';

class GestaoDashboardScreen extends StatefulWidget {
  @override
  _GestaoDashboardScreenState createState() => _GestaoDashboardScreenState();
}

class _GestaoDashboardScreenState extends State<GestaoDashboardScreen> {
  final ApiService _apiService = ApiService();
  late Future<List<dynamic>> _ocorrenciasFuture;

  @override
  void initState() {
    super.initState();
    _loadOcorrencias();
  }

  void _loadOcorrencias() {
    _ocorrenciasFuture = _apiService.getAllOcorrencias();
  }

  Color _getPrioridadeColor(String prio) {
    switch (prio) {
      case 'CRITICA': return Colors.red;
      case 'ALTA': return Colors.orange;
      case 'MEDIA': return Colors.amber;
      default: return Colors.green;
    }
  }

  IconData _getStatusIcon(String status) {
    switch (status) {
      case 'RESOLVIDA': return Icons.check_circle;
      case 'EM_ANDAMENTO': return Icons.autorenew;
      default: return Icons.error_outline;
    }
  }

  void _showChangeStatusDialog(dynamic ocorrencia) {
    final List<String> statusOptions = ['PENDENTE', 'ABERTA', 'EM_ANDAMENTO', 'RESOLVIDA'];
    String currentStatus = ocorrencia['status'] ?? 'PENDENTE';

    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          title: Text('Alterar Status'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Ocorrência #${ocorrencia['id']}', style: TextStyle(fontWeight: FontWeight.bold)),
              SizedBox(height: 5),
              Text(ocorrencia['descricao'] ?? '', style: TextStyle(fontSize: 13, color: Colors.grey[700])),
              SizedBox(height: 15),
              Text('Status atual: $currentStatus'),
              SizedBox(height: 15),
              Text('Selecione o novo status:'),
              SizedBox(height: 10),
              ...statusOptions.map((status) {
                return ListTile(
                  leading: Icon(
                    _getStatusIcon(status),
                    color: status == 'RESOLVIDA' ? Colors.green : (status == 'EM_ANDAMENTO' ? Colors.orange : Colors.blue),
                  ),
                  title: Text(status),
                  selected: status == currentStatus,
                  onTap: () async {
                    Navigator.pop(ctx);
                    bool success = await _apiService.updateStatus(ocorrencia['id'], status);
                    if (success) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text('Status alterado para $status!'), backgroundColor: Colors.green),
                      );
                      setState(() { _loadOcorrencias(); });
                    } else {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text('Erro ao alterar status.'), backgroundColor: Colors.red),
                      );
                    }
                  },
                );
              }),
            ],
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Ocorrências da Cidade'), backgroundColor: Colors.blue[800]),
      body: FutureBuilder<List<dynamic>>(
        future: _ocorrenciasFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return Center(child: CircularProgressIndicator());
          } else if (snapshot.hasError) {
            return Center(child: Text('Erro ao carregar.'));
          } else if (!snapshot.hasData || snapshot.data!.isEmpty) {
            return Center(child: Text('Nenhuma ocorrência registrada.'));
          }

          final ocorrencias = snapshot.data!;
          return ListView.builder(
            itemCount: ocorrencias.length,
            itemBuilder: (context, index) {
              final oc = ocorrencias[index];
              final String prio = oc['prioridade'] ?? 'BAIXA';
              final String status = oc['status'] ?? 'ABERTA';

              return Card(
                color: _getPrioridadeColor(prio).withOpacity(0.15),
                margin: EdgeInsets.all(8),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                child: ListTile(
                  leading: CircleAvatar(backgroundColor: _getPrioridadeColor(prio), child: Text(oc['id'].toString(), style: TextStyle(color: Colors.white))),
                  title: Text(oc['descricao'] ?? '', style: TextStyle(fontWeight: FontWeight.bold)),
                  subtitle: Text('Status: $status | Cat: ${oc['categoria']}'),
                  trailing: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(prio, style: TextStyle(fontWeight: FontWeight.bold, color: _getPrioridadeColor(prio), fontSize: 12)),
                      Icon(Icons.edit, size: 18, color: Colors.blue[800]),
                    ],
                  ),
                  onTap: () => _showChangeStatusDialog(oc),
                ),
              );
            },
          );
        },
      ),
    );
  }
}
