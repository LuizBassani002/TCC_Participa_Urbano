import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import '../services/api_service.dart';

class OcorrenciaDetalhesScreen extends StatefulWidget {
  final Map<String, dynamic> ocorrencia;

  const OcorrenciaDetalhesScreen({Key? key, required this.ocorrencia}) : super(key: key);

  @override
  _OcorrenciaDetalhesScreenState createState() => _OcorrenciaDetalhesScreenState();
}

class _OcorrenciaDetalhesScreenState extends State<OcorrenciaDetalhesScreen> {
  String? _resolvedAddress;
  bool _isLoadingAddress = false;

  @override
  void initState() {
    super.initState();
    _initializeAddress();
  }

  void _initializeAddress() {
    final String endereco = widget.ocorrencia['endereco'] ?? '';
    final double? latitude = widget.ocorrencia['latitude'];
    final double? longitude = widget.ocorrencia['longitude'];

    if (endereco.isNotEmpty) {
      _resolvedAddress = endereco;
    } else if (latitude != null && longitude != null) {
      _loadAddressFromCoordinates(latitude, longitude);
    }
  }

  Future<void> _loadAddressFromCoordinates(double lat, double lng) async {
    setState(() {
      _isLoadingAddress = true;
    });

    try {
      final url = Uri.parse('https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lng');
      final response = await http.get(url, headers: {
        'User-Agent': 'ParticipaUrbanoApp/1.0',
      });

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        final addressMap = data['address'] as Map<String, dynamic>?;
        String? address;

        if (addressMap != null) {
          String road = addressMap['road'] ?? addressMap['pedestrian'] ?? addressMap['suburb'] ?? '';
          String number = addressMap['house_number'] ?? '';
          String city = addressMap['city'] ?? addressMap['town'] ?? addressMap['village'] ?? '';
          String state = addressMap['state'] ?? '';

          List<String> parts = [];
          if (road.isNotEmpty) parts.add(road);
          if (number.isNotEmpty) parts.add(number);
          if (city.isNotEmpty) parts.add(city);
          if (state.isNotEmpty) parts.add(state);

          if (parts.isNotEmpty) {
            address = parts.join(', ');
          }
        }

        address ??= data['display_name'] as String?;

        if (mounted) {
          setState(() {
            _resolvedAddress = address;
            _isLoadingAddress = false;
          });
        }
      } else {
        if (mounted) {
          setState(() {
            _resolvedAddress = 'Coordenadas ($lat, $lng)';
            _isLoadingAddress = false;
          });
        }
      }
    } catch (e) {
      print('Erro ao obter endereço nos detalhes: $e');
      if (mounted) {
        setState(() {
          _resolvedAddress = 'Coordenadas ($lat, $lng)';
          _isLoadingAddress = false;
        });
      }
    }
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

  String _getImageUrl(String path) {
    // Extrai apenas o nome do arquivo, independente de ser URL do MinIO ou caminho local
    String fileName = path.split('/').last.split('\\').last;
    // Usa o proxy do Spring Boot para evitar CORS ao carregar imagens do MinIO
    return '${ApiService.baseUrl}/imagens/$fileName';
  }

  @override
  Widget build(BuildContext context) {
    final String status = widget.ocorrencia['status'] ?? 'PENDENTE';
    final String descricao = widget.ocorrencia['descricao'] ?? 'Sem descrição';
    final String categoria = widget.ocorrencia['categoria'] ?? 'Sem categoria';

    final List<dynamic> fotos = widget.ocorrencia['fotos'] ?? [];
    final String? caminhoFoto = fotos.isNotEmpty ? fotos[0]['caminhoUrl'] : null;
    final String? imageUrl = caminhoFoto != null ? _getImageUrl(caminhoFoto) : null;

    final Color statusColor = _getStatusColor(status);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Detalhes da Ocorrência'),
        backgroundColor: Colors.green[700],
      ),
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Image Section
            if (imageUrl != null)
              Container(
                height: 250,
                decoration: BoxDecoration(
                  color: Colors.grey[200],
                ),
                child: Image.network(
                  imageUrl,
                  fit: BoxFit.cover,
                  errorBuilder: (context, error, stackTrace) {
                    return Container(
                      color: Colors.grey[300],
                      alignment: Alignment.center,
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.broken_image, size: 50, color: Colors.grey[600]),
                          const SizedBox(height: 8),
                          Text(
                            'Erro ao carregar a imagem',
                            style: TextStyle(color: Colors.grey[600]),
                          ),
                        ],
                      ),
                    );
                  },
                  loadingBuilder: (context, child, loadingProgress) {
                    if (loadingProgress == null) return child;
                    return Center(
                      child: CircularProgressIndicator(
                        value: loadingProgress.expectedTotalBytes != null
                            ? loadingProgress.cumulativeBytesLoaded / loadingProgress.expectedTotalBytes!
                            : null,
                      ),
                    );
                  },
                ),
              )
            else
              Container(
                height: 180,
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: [Colors.green[200]!, Colors.green[100]!],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                ),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.image_not_supported, size: 60, color: Colors.green[800]),
                    const SizedBox(height: 8),
                    Text(
                      'Sem imagem anexada',
                      style: TextStyle(
                        color: Colors.green[800],
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
              ),

            Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Status & Category Row
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Chip(
                        avatar: Icon(
                          _getStatusIcon(status),
                          color: Colors.white,
                          size: 16,
                        ),
                        label: Text(
                          status,
                          style: const TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        backgroundColor: statusColor,
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                        decoration: BoxDecoration(
                          color: Colors.grey[200],
                          borderRadius: BorderRadius.circular(20),
                        ),
                        child: Text(
                          categoria,
                          style: TextStyle(
                            color: Colors.grey[800],
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 20),

                  // Description Card
                  const Text(
                    'Descrição do Problema',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Card(
                    elevation: 0,
                    color: Colors.grey[100],
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Padding(
                      padding: const EdgeInsets.all(16.0),
                      child: Text(
                        descricao,
                        style: const TextStyle(fontSize: 16, height: 1.4),
                      ),
                    ),
                  ),
                  const SizedBox(height: 24),

                  // Location Section
                  const Text(
                    'Localização',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Card(
                    elevation: 0,
                    color: Colors.grey[50],
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                      side: BorderSide(color: Colors.grey[200]!),
                    ),
                    child: Padding(
                      padding: const EdgeInsets.all(16.0),
                      child: _isLoadingAddress
                          ? const Center(
                              child: Padding(
                                padding: EdgeInsets.all(16.0),
                                child: CircularProgressIndicator(),
                              ),
                            )
                          : _resolvedAddress != null
                              ? Row(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    const Icon(Icons.location_on, color: Colors.green),
                                    const SizedBox(width: 12),
                                    Expanded(
                                      child: Text(
                                        _resolvedAddress!,
                                        style: const TextStyle(fontSize: 15),
                                      ),
                                    ),
                                  ],
                                )
                              : Row(
                                  children: [
                                    Icon(Icons.location_off, color: Colors.grey[400]),
                                    const SizedBox(width: 12),
                                    Text(
                                      'Nenhuma informação de localização.',
                                      style: TextStyle(color: Colors.grey[600]),
                                    ),
                                  ],
                                ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
