import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';
import '../services/api_service.dart';

class MapaCalorScreen extends StatefulWidget {
  @override
  _MapaCalorScreenState createState() => _MapaCalorScreenState();
}

class _MapaCalorScreenState extends State<MapaCalorScreen> {
  final ApiService _apiService = ApiService();
  late Future<List<dynamic>> _ocorrenciasFuture;

  @override
  void initState() {
    super.initState();
    _ocorrenciasFuture = _apiService.getAllOcorrencias();
  }

  // Método para construir o widget do mapa com as ocorrências de Blumenau
  Widget _buildMapa(List<dynamic> ocorrencias) {
    // Coordenadas centrais de Blumenau/SC
    const LatLng centroBlumenau = LatLng(-26.9166, -49.0717);

    // Mapeia todas as ocorrências que possuem latitude e longitude válidas
    List<Marker> markers = ocorrencias
        .where((oc) => oc['latitude'] != null && oc['longitude'] != null)
        .map((oc) {
      double lat = (oc['latitude'] as num).toDouble();
      double lng = (oc['longitude'] as num).toDouble();
      String prioridade = oc['prioridade'] ?? 'BAIXA';

      return Marker(
        point: LatLng(lat, lng),
        width: 40,
        height: 40,
        child: Tooltip(
          message: '${oc['descricao'] ?? "Ocorrência"} ($prioridade)',
          child: Icon(
            Icons.location_on,
            color: _getPrioridadeColor(prioridade),
            size: 38,
          ),
        ),
      );
    }).toList();

    // Centraliza na primeira ocorrência ou no centro de Blumenau
    LatLng centroInicial = markers.isNotEmpty ? markers.first.point : centroBlumenau;

    return Container(
      height: 300,
      margin: const EdgeInsets.symmetric(vertical: 12),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey.shade400),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(12),
        child: FlutterMap(
          options: MapOptions(
            initialCenter: centroInicial,
            initialZoom: 13.0,
          ),
          children: [
            TileLayer(
              urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
              userAgentPackageName: 'com.participa.urbano',
            ),
            MarkerLayer(markers: markers),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Mapa de Calor - Ocorrências'), backgroundColor: Colors.blue[800]),
      body: FutureBuilder<List<dynamic>>(
        future: _ocorrenciasFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return Center(child: CircularProgressIndicator());
          } else if (snapshot.hasError || !snapshot.hasData) {
            return Center(child: Text('Erro ao carregar dados.'));
          }

          final ocorrencias = snapshot.data!;

          // Agrupar por Categoria, Status e Prioridade
          Map<String, int> porCategoria = {};
          Map<String, int> porStatus = {};
          Map<String, int> porPrioridade = {};

          for (var oc in ocorrencias) {
            String cat = oc['categoria'] ?? 'SEM_CATEGORIA';
            String status = oc['status'] ?? 'ABERTA';
            String prio = oc['prioridade'] ?? 'BAIXA';

            porCategoria[cat] = (porCategoria[cat] ?? 0) + 1;
            porStatus[status] = (porStatus[status] ?? 0) + 1;
            porPrioridade[prio] = (porPrioridade[prio] ?? 0) + 1;
          }

          return SingleChildScrollView(
            padding: EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Resumo Geral
                Card(
                  color: Colors.blue[800],
                  child: Padding(
                    padding: EdgeInsets.all(20),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceAround,
                      children: [
                        _buildResumoItem('Total', ocorrencias.length.toString(), Icons.list_alt, Colors.white),
                        _buildResumoItem('Pendentes', (porStatus['PENDENTE'] ?? 0).toString(), Icons.hourglass_empty, Colors.white70),
                        _buildResumoItem('Abertas', (porStatus['ABERTA'] ?? 0).toString(), Icons.error_outline, Colors.yellowAccent),
                        _buildResumoItem('Em Andamento', (porStatus['EM_ANDAMENTO'] ?? 0).toString(), Icons.autorenew, Colors.orangeAccent),
                        _buildResumoItem('Resolvidas', (porStatus['RESOLVIDA'] ?? 0).toString(), Icons.check_circle, Colors.greenAccent),
                      ],
                    ),
                  ),
                ),
                SizedBox(height: 20),

                // Indicadores por Categoria
                Text('Ocorrências por Categoria', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                SizedBox(height: 10),
                ...porCategoria.entries.map((entry) {
                  double percentage = ocorrencias.isEmpty ? 0 : (entry.value / ocorrencias.length) * 100;
                  return Padding(
                    padding: EdgeInsets.symmetric(vertical: 4),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(entry.key, style: TextStyle(fontWeight: FontWeight.w500)),
                            Text('${entry.value} (${percentage.toStringAsFixed(0)}%)'),
                          ],
                        ),
                        SizedBox(height: 4),
                        LinearProgressIndicator(
                          value: percentage / 100,
                          backgroundColor: Colors.grey[300],
                          color: _getCategoriaColor(entry.key),
                          minHeight: 12,
                        ),
                      ],
                    ),
                  );
                }),
                SizedBox(height: 20),

                // Mapa de Localização das Ocorrências
                Text('Localização no Mapa (Blumenau)', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                _buildMapa(ocorrencias),

                SizedBox(height: 10),

                // Indicadores por Prioridade
                Text('Nível de Urgência', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                SizedBox(height: 10),
                Wrap(
                  spacing: 10,
                  runSpacing: 10,
                  children: porPrioridade.entries.map((entry) {
                    return Chip(
                      avatar: CircleAvatar(
                        backgroundColor: _getPrioridadeColor(entry.key),
                        child: Text(entry.value.toString(), style: TextStyle(color: Colors.white, fontSize: 12)),
                      ),
                      label: Text(entry.key),
                      backgroundColor: _getPrioridadeColor(entry.key).withOpacity(0.2),
                    );
                  }).toList(),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildResumoItem(String label, String value, IconData icon, Color color) {
    return Column(
      children: [
        Icon(icon, color: color, size: 30),
        SizedBox(height: 5),
        Text(value, style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Colors.white)),
        Text(label, style: TextStyle(color: Colors.white70, fontSize: 12)),
      ],
    );
  }

  Color _getCategoriaColor(String cat) {
    switch (cat) {
      case 'ILUMINACAO_PUBLICA': return Colors.amber;
      case 'LIMPEZA_PUBLICA': return Colors.brown;
      case 'INFRAESTRUTURA': return Colors.blueGrey;
      case 'SEGURANCA': return Colors.red;
      case 'MEIO_AMBIENTE': return Colors.green;
      default: return Colors.purple;
    }
  }

  Color _getPrioridadeColor(String prio) {
    switch (prio) {
      case 'CRITICA': return Colors.red;
      case 'ALTA': return Colors.orange;
      case 'MEDIA': return Colors.amber;
      default: return Colors.green;
    }
  }
}