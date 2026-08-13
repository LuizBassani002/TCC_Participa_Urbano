import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:geolocator/geolocator.dart';
import 'dart:convert';
import 'package:http/http.dart' as http;
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
  
  XFile? _imageFile;
  Position? _currentPosition;
  bool _isLoading = false;

  Future<void> _takePhoto() async {
    final XFile? photo = await _picker.pickImage(source: ImageSource.camera);
    if (photo != null) {
      setState(() {
        _imageFile = photo;
      });
    }
  }

  Future<void> _getLocation() async {
    bool serviceEnabled;
    LocationPermission permission;

    serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Ative o serviço de GPS.')),
      );
      return;
    }

    permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) return;
    }

    if (permission == LocationPermission.deniedForever) return;

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Obtendo coordenadas do GPS com alta precisão...')),
    );

    try {
      // Configuração para forçar a melhor precisão disponível no dispositivo
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

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Obtendo endereço a partir do GPS...')),
      );

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
            SnackBar(content: Text('Endereço preenchido com sucesso!'), backgroundColor: Colors.green),
          );
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Não foi possível obter o endereço legível.')),
          );
        }
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erro no servidor de geolocalização.')),
        );
      }
    } catch (e) {
      print('Erro ao obter endereço/localização: $e');
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Erro ao obter localização ou converter endereço.')),
      );
    }
  }

  void _submit() async {
    if (_descricaoController.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Preencha a descrição!')),
      );
      return;
    }
    
    setState(() => _isLoading = true);
    
    bool success = await _apiService.createOcorrencia(
      descricao: _descricaoController.text,
      endereco: _enderecoController.text,
      latitude: _currentPosition?.latitude,
      longitude: _currentPosition?.longitude,
      imageFile: _imageFile,
    );
    
    setState(() => _isLoading = false);

    if (success) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Ocorrência salva com sucesso!')),
      );
      Navigator.pop(context);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Falha ao salvar.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Relatar Problema')),
      body: SingleChildScrollView(
        padding: EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextField(
              controller: _descricaoController,
              maxLines: 3,
              decoration: InputDecoration(
                labelText: 'Descrição do Problema',
                border: OutlineInputBorder(),
              ),
            ),
            SizedBox(height: 15),
            TextField(
              controller: _enderecoController,
              decoration: InputDecoration(
                labelText: 'Endereço (Opcional se usar GPS)', 
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.map),
              ),
              onChanged: (value) {
                if (_currentPosition != null) {
                  setState(() {
                    _currentPosition = null;
                  });
                }
              },
            ),
            SizedBox(height: 15),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                ElevatedButton.icon(
                  onPressed: _getLocation,
                  icon: Icon(Icons.gps_fixed),
                  label: Text(_currentPosition == null ? 'Pegar GPS' : 'GPS OK'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: _currentPosition == null ? Colors.blue : Colors.green,
                  ),
                ),
                ElevatedButton.icon(
                  onPressed: _takePhoto,
                  icon: Icon(Icons.camera_alt),
                  label: Text(_imageFile == null ? 'Tirar Foto' : 'Foto Anexada'),
                ),
              ],
            ),
            SizedBox(height: 30),
            _isLoading 
              ? Center(child: CircularProgressIndicator()) 
              : ElevatedButton(
                  onPressed: _submit,
                  child: Text('ENVIAR OCORRÊNCIA', style: TextStyle(fontSize: 16)),
                  style: ElevatedButton.styleFrom(
                    padding: EdgeInsets.symmetric(vertical: 15),
                    backgroundColor: Colors.green,
                  ),
                ),
          ],
        ),
      ),
    );
  }
}