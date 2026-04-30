import 'dart:convert';
import 'dart:io';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:image_picker/image_picker.dart';

class ApiService {
  // 10.0.2.2 is the localhost alias for Android Emulator
  // Se rodar no celular físico, mude para o IP da sua máquina
  static const String baseUrl = kIsWeb ? 'http://localhost:8080/api' : 'http://10.0.2.2:8080/api';
  final storage = const FlutterSecureStorage();

  Future<Map<String, dynamic>> register(String nome, String email, String senha, String codigoPrefeitura) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/auth/registrar'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'nome': nome, 'email': email, 'senha': senha, 'codigoPrefeitura': codigoPrefeitura}),
      );
      if (response.statusCode == 200) {
        return {'success': true, 'message': 'Conta criada com sucesso!'};
      } else if (response.statusCode == 400) {
        return {'success': false, 'message': 'Este e-mail já está cadastrado.'};
      } else {
        return {'success': false, 'message': 'Erro do servidor: ${response.statusCode}'};
      }
    } catch (e) {
      return {'success': false, 'message': 'Sem conexão com o servidor. Verifique se a API está rodando.'};
    }
  }

  Future<bool> login(String email, String senha) async {
    final response = await http.post(
      Uri.parse('$baseUrl/auth/login'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'email': email, 'senha': senha}),
    );

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      await storage.write(key: 'jwt', value: data['token']);
      return true;
    }
    return false;
  }

  Future<bool> createOcorrencia({
    required String descricao,
    String? endereco,
    double? latitude,
    double? longitude,
    XFile? imageFile,
  }) async {
    try {
      final token = await storage.read(key: 'jwt');

      var request = http.MultipartRequest('POST', Uri.parse('$baseUrl/ocorrencias'));
    request.headers['Authorization'] = 'Bearer $token';
    
    request.fields['descricao'] = descricao;
    
    if (endereco != null && endereco.isNotEmpty) {
      request.fields['endereco'] = endereco;
    }
    if (latitude != null && longitude != null) {
      request.fields['latitude'] = latitude.toString();
      request.fields['longitude'] = longitude.toString();
    }

      if (imageFile != null) {
        if (kIsWeb) {
          request.files.add(http.MultipartFile.fromBytes(
            'fotos',
            await imageFile.readAsBytes(),
            filename: imageFile.name,
          ));
        } else {
          request.files.add(await http.MultipartFile.fromPath('fotos', imageFile.path));
        }
      }

      var streamedResponse = await request.send();
      return streamedResponse.statusCode == 201;
    } catch (e) {
      print("Erro ao disparar api: \$e");
      return false;
    }
  }

  Future<List<dynamic>> getMinhasOcorrencias() async {
    final token = await storage.read(key: 'jwt');
    final response = await http.get(
      Uri.parse('$baseUrl/ocorrencias/minhas'),
      headers: {'Authorization': 'Bearer $token'},
    );
    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    }
    return [];
  }

  Future<List<dynamic>> getAllOcorrencias() async {
    final token = await storage.read(key: 'jwt');
    final response = await http.get(
      Uri.parse('$baseUrl/ocorrencias'),
      headers: {'Authorization': 'Bearer $token'},
    );
    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    }
    return [];
  }

  Future<List<dynamic>> getOcorrenciasPublicas() async {
    final token = await storage.read(key: 'jwt');
    final response = await http.get(
      Uri.parse('$baseUrl/ocorrencias/publicas'),
      headers: {'Authorization': 'Bearer $token'},
    );
    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    }
    return [];
  }

  Future<bool> updateStatus(int ocorrenciaId, String novoStatus) async {
    try {
      final token = await storage.read(key: 'jwt');
      final url = '$baseUrl/ocorrencias/$ocorrenciaId/status?status=$novoStatus';
      print('PATCH URL: $url');
      final response = await http.patch(
        Uri.parse(url),
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
      );
      print('Response status: ${response.statusCode}');
      print('Response body: ${response.body}');
      return response.statusCode == 200;
    } catch (e) {
      print('ERRO updateStatus: $e');
      return false;
    }
  }

  Future<void> logout() async {
    await storage.delete(key: 'jwt');
  }
}
