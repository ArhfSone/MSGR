import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../../../core/constants/api_constants.dart';
import '../../../core/network/dio_client.dart';
import 'dto/auth_response.dart';
import 'dto/login_request.dart';
import 'dto/register_request.dart';

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  final dio = ref.watch(dioProvider);
  final storage = ref.watch(secureStorageProvider);
  return AuthRepository(dio: dio, storage: storage);
});

class AuthRepository {
  final Dio dio;
  final FlutterSecureStorage storage;

  AuthRepository({required this.dio, required this.storage});

  Future<AuthResponse> register(RegisterRequest request) async {
    try {
      final response = await dio.post(
        ApiConstants.register,
        data: request.toJson(),
      );
      final authResponse = AuthResponse.fromJson(response.data);
      await _saveTokens(authResponse);
      return authResponse;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<AuthResponse> login(LoginRequest request) async {
    try {
      final response = await dio.post(
        ApiConstants.login,
        data: request.toJson(),
      );
      final authResponse = AuthResponse.fromJson(response.data);
      await _saveTokens(authResponse);
      return authResponse;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<void> logout() async {
    await storage.delete(key: 'access_token');
    await storage.delete(key: 'refresh_token');
  }

  Future<bool> isLoggedIn() async {
    final token = await storage.read(key: 'access_token');
    return token != null;
  }

  Future<String?> getAccessToken() async {
    return await storage.read(key: 'access_token');
  }

  Future<void> _saveTokens(AuthResponse response) async {
    await storage.write(key: 'access_token', value: response.accessToken);
    await storage.write(key: 'refresh_token', value: response.refreshToken);
    await storage.write(key: 'user_id', value: response.userId.toString());
    await storage.write(key: 'username', value: response.username);
  }

  String _handleError(DioException e) {
    if (e.response?.data is Map && e.response!.data.containsKey('error')) {
      return e.response!.data['error'] as String;
    }
    switch (e.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.receiveTimeout:
        return 'Превышено время ожидания. Проверьте подключение.';
      case DioExceptionType.connectionError:
        return 'Не удалось подключиться к серверу';
      default:
        return 'Произошла ошибка. Попробуйте позже.';
    }
  }

  Future<String?> getUserId() async {
  return await storage.read(key: 'user_id');
}

Future<String?> getUsername() async {
  return await storage.read(key: 'username');
}
}