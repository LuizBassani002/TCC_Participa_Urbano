import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:geolocator/geolocator.dart';
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
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ative o serviço de GPS.')));
      return;
    }

    permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) return;
    }

    if (permission == LocationPermission.deniedForever) return;

    Position position = await Geolocator.getCurrentPosition();
    setState(() {
      _currentPosition = position;
    });
    
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Localização capturada!')));
  }

  void _submit() async {
    if (_descricaoController.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Preencha a descrição!')));
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
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Ocorrência salva com sucesso!')));
      Navigator.pop(context);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Falha ao salvar.')));
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
              decoration: InputDecoration(labelText: 'Descrição do Problema', border: OutlineInputBorder()),
            ),
            SizedBox(height: 15),
            TextField(
              controller: _enderecoController,
              decoration: InputDecoration(
                labelText: 'Endereço (Opcional se usar GPS)', 
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.map)
              ),
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
                    backgroundColor: _currentPosition == null ? Colors.blue : Colors.green
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
                    backgroundColor: Colors.green
                  ),
                )
          ],
        ),
      ),
    );
  }
}
