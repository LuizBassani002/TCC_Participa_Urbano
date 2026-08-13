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

  int _getPesoPrioridade(String? prioridade) {
    if (prioridade == null) return 1;
    switch (prioridade.toUpperCase()) {
      case 'CRITICA': return 4;
      case 'ALTA': return 3;
      case 'MEDIA': return 2;
      case 'BAIXA': return 1;
      default: return 1;
    }
  }

  Color _getPrioridadeColor(String prio) {
    switch (prio.toUpperCase()) {
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

  // DIÁLOGO PARA EXCLUIR OCORRÊNCIA
  void _showDeleteConfirmDialog(dynamic ocorrencia) {
    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          title: const Text('Excluir Ocorrência'),
          content: Text('Tem certeza de que deseja excluir a ocorrência #${ocorrencia['id']}? Esta ação não pode ser desfeita.'),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Cancelar'),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
              onPressed: () async {
                Navigator.pop(ctx);
                bool success = await _apiService.deleteOcorrencia(ocorrencia['id']);
                if (success) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Ocorrência excluída com sucesso!'),
                      backgroundColor: Colors.green,
                    ),
                  );
                  setState(() { _loadOcorrencias(); });
                } else {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Erro ao excluir ocorrência.'),
                      backgroundColor: Colors.red,
                    ),
                  );
                }
              },
              child: const Text('Excluir', style: TextStyle(color: Colors.white)),
            ),
          ],
        );
      },
    );
  }

  // DIÁLOGO PARA ALTERAR STATUS
  void _showChangeStatusDialog(dynamic ocorrencia) {
    final List<String> statusOptions = ['PENDENTE', 'ABERTA', 'EM_ANDAMENTO', 'RESOLVIDA'];
    String currentStatus = ocorrencia['status'] ?? 'PENDENTE';

    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          title: const Text('Alterar Status'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Ocorrência #${ocorrencia['id']}', style: const TextStyle(fontWeight: FontWeight.bold)),
              const SizedBox(height: 5),
              Text(ocorrencia['descricao'] ?? '', style: TextStyle(fontSize: 13, color: Colors.grey[700])),
              const SizedBox(height: 15),
              Text('Status atual: $currentStatus'),
              const SizedBox(height: 15),
              const Text('Selecione o novo status:'),
              const SizedBox(height: 10),
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
                        const SnackBar(content: Text('Erro ao alterar status.'), backgroundColor: Colors.red),
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
      appBar: AppBar(title: const Text('Ocorrências da Cidade'), backgroundColor: Colors.blue[800]),
      body: FutureBuilder<List<dynamic>>(
        future: _ocorrenciasFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          } else if (snapshot.hasError) {
            return const Center(child: Text('Erro ao carregar.'));
          } else if (!snapshot.hasData || snapshot.data!.isEmpty) {
            return const Center(child: Text('Nenhuma ocorrência registrada.'));
          }

          final ocorrencias = List<dynamic>.from(snapshot.data!);

          // Ordenação por prioridade
          ocorrencias.sort((a, b) => 
            _getPesoPrioridade(b['prioridade']).compareTo(_getPesoPrioridade(a['prioridade']))
          );

          return ListView.builder(
            itemCount: ocorrencias.length,
            itemBuilder: (context, index) {
              final oc = ocorrencias[index];
              final String prio = oc['prioridade'] ?? 'BAIXA';
              final String status = oc['status'] ?? 'ABERTA';

              return Card(
                color: _getPrioridadeColor(prio).withOpacity(0.15),
                margin: const EdgeInsets.all(8),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                child: ListTile(
                  leading: CircleAvatar(
                    backgroundColor: _getPrioridadeColor(prio),
                    child: Text(
                      (index + 1).toString(), // Exibe a posição da ordem (1, 2, 3...)
                      style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
                    ),
                  ),
                  title: Text(oc['descricao'] ?? '', style: const TextStyle(fontWeight: FontWeight.bold)),
                  subtitle: Text('ID: ${oc['id']} | Status: $status | Cat: ${oc['categoria']}'),
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text(
                            prio,
                            style: TextStyle(fontWeight: FontWeight.bold, color: _getPrioridadeColor(prio), fontSize: 12),
                          ),
                          const SizedBox(height: 4),
                          Icon(Icons.edit, size: 18, color: Colors.blue[800]),
                        ],
                      ),
                      const SizedBox(width: 8),
                      // BOTÃO DE LIXEIRA PARA EXCLUIR
                      IconButton(
                        icon: const Icon(Icons.delete_outline, color: Colors.red),
                        onPressed: () => _showDeleteConfirmDialog(oc),
                        tooltip: 'Excluir Ocorrência',
                      ),
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