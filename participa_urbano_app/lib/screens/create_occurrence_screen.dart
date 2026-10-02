import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart'; // tirar foto da câmera
import 'package:geolocator/geolocator.dart'; // pegar GPS do celular
import 'dart:convert';
import 'package:http/http.dart' as http; // fazer requisições HTTP
import '../services/api_service.dart';

class CreateOccurrenceScreen extends StatefulWidget {
  @override
  _CreateOccurrenceScreenState createState() => _CreateOccurrenceScreenState();
}

class _CreateOccurrenceScreenState extends State<CreateOccurrenceScreen> {
  final _descricaoController = TextEditingController();
  final _enderecoController = TextEditingController();
  final _apiService = ApiService();
  final ImagePicker _picker = ImagePicker();
  
  XFile? _imageFile; // guarda a foto tirada.
  Position? _currentPosition; // guarda a posição GPS obtida.
  bool _isLoading = false;

  Future<void> _takePhoto() async {
    final XFile? photo = await _picker.pickImage(source: ImageSource.camera);
    if (photo != null) {
      setState(() {
        _imageFile = photo;
      });
    }
  }

  // BUSCA AS COORDENADAS EXATAS (LAT/LON) A PARTIR DO TEXTO DO ENDEREÇO
  Future<Position?> _getCoordinatesFromAddress(String enderecoDigitado) async {
    String enderecoTexto = enderecoDigitado.trim();

    if (enderecoTexto.isEmpty) return null;

    // Adiciona a cidade de referência padrão para garantir que encontre na região correta
    if (!enderecoTexto.toLowerCase().contains('blumenau')) {
      enderecoTexto = "$enderecoTexto, Blumenau, SC";
    }

    try { //API pública Nominatim (OpenStreetMap)
      final encodedAddress = Uri.encodeComponent(enderecoTexto);
      final url = Uri.parse(
        'https://nominatim.openstreetmap.org/search?format=json&q=$encodedAddress&limit=1',
      );

      final response = await http.get(url, headers: {
        'User-Agent': 'ParticipaUrbanoApp/1.0',
      });

      if (response.statusCode == 200) {
        final List data = jsonDecode(response.body);

        if (data.isNotEmpty) {
          double lat = double.parse(data[0]['lat']);
          double lon = double.parse(data[0]['lon']);

          return Position(
            latitude: lat,
            longitude: lon,
            timestamp: DateTime.now(),
            accuracy: 0.0,
            altitude: 0.0,
            heading: 0.0,
            speed: 0.0,
            speedAccuracy: 0.0,
            altitudeAccuracy: 0.0,
            headingAccuracy: 0.0,
          );
        }
      }
    } catch (e) {
      print('Erro ao obter coordenadas do endereço digitado: $e');
    }
    return null;
  }

  // PEGA ENDEREÇO E COORDENADAS DO GPS DO APARELHO
  Future<void> _getLocation() async {
    bool serviceEnabled;
    LocationPermission permission;
    //Verifica se o serviço de localização do celular está ativado
    serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Ative o serviço de GPS.')),
      );
      return;
    }
    //Verifica/solicita permissão de localização ao usuário.
    permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) return;
    }

    if (permission == LocationPermission.deniedForever) return;

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Obtendo coordenadas do GPS...')),
    );

    try {
      LocationSettings locationSettings = const LocationSettings(
        accuracy: LocationAccuracy.best,
        timeLimit: Duration(seconds: 15),
      );

      Position position = await Geolocator.getCurrentPosition(
        locationSettings: locationSettings,
      );

      setState(() {
        _currentPosition = position;
      });

      final url = Uri.parse(
        'https://nominatim.openstreetmap.org/reverse?format=json&lat=${position.latitude}&lon=${position.longitude}',
      );
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

        if (address != null && address.isNotEmpty) {
          setState(() {
            _enderecoController.text = address!;
          });
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Endereço obtido via GPS!'), backgroundColor: Colors.green),
          );
        }
      }
    } catch (e) {
      print('Erro ao obter localização: $e');
    }
  }

  void _submit() async { //Envio da ocorrênciaEnvio da ocorrência
    if (_descricaoController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Preencha a descrição!')),
      );
      return;
    }

    setState(() => _isLoading = true);

    double? finalLatitude = _currentPosition?.latitude;
    double? finalLongitude = _currentPosition?.longitude;
    String enderecoTexto = _enderecoController.text.trim();

    // REGRA DE PRIORIDADE:
    // Se o usuário digitou ou alterou o endereço manualmente, ignora o sensor de GPS do celular
    // e busca a coordenada exata da rua digitada.
    if (enderecoTexto.isNotEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Buscando localização exata do endereço informado...')),
      );

      Position? posDoEndereco = await _getCoordinatesFromAddress(enderecoTexto);
      
      if (posDoEndereco != null) {
        finalLatitude = posDoEndereco.latitude;
        finalLongitude = posDoEndereco.longitude;
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Aviso: Não foi possível calcular a coordenada exata da rua. Usando dados básicos.'),
            backgroundColor: Colors.orange,
          ),
        );
      }
    }

    bool success = await _apiService.createOcorrencia(
      descricao: _descricaoController.text,
      endereco: enderecoTexto,
      latitude: finalLatitude,
      longitude: finalLongitude,
      imageFile: _imageFile,
    );
    
    setState(() => _isLoading = false);

    if (success) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Ocorrência salva com sucesso!'), backgroundColor: Colors.green),
      );
      Navigator.pop(context);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Falha ao salvar ocorrência.'), backgroundColor: Colors.red),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Relatar Problema')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextField(
              controller: _descricaoController,
              maxLines: 3,
              decoration: const InputDecoration(
                labelText: 'Descrição do Problema',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 15),
            TextField(
              controller: _enderecoController,
              decoration: const InputDecoration(
                labelText: 'Endereço / Nome da Rua', 
                hintText: 'Ex: Rua 7 de Setembro, 1000',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.map),
              ),
            ),
            const SizedBox(height: 15),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                ElevatedButton.icon(
                  onPressed: _getLocation,
                  icon: const Icon(Icons.gps_fixed),
                  label: const Text('Usar GPS do Celular'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.blue,
                  ),
                ),
                ElevatedButton.icon(
                  onPressed: _takePhoto,
                  icon: const Icon(Icons.camera_alt),
                  label: Text(_imageFile == null ? 'Tirar Foto' : 'Foto Anexada'),
                ),
              ],
            ),
            const SizedBox(height: 30),
            _isLoading 
              ? const Center(child: CircularProgressIndicator()) 
              : ElevatedButton(
                  onPressed: _submit,
                  style: ElevatedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 15),
                    backgroundColor: Colors.green,
                  ),
                  child: const Text('ENVIAR OCORRÊNCIA', style: TextStyle(fontSize: 16, color: Colors.white)),
                ),
          ],
        ),
      ),
    );
  }
}