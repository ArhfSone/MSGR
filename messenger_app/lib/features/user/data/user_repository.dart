import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/constants/api_constants.dart';
import '../../../core/network/dio_client.dart';
import 'dto/user_model.dart';

final userRepositoryProvider = Provider<UserRepository>((ref) {
  final dio = ref.watch(dioProvider);
  return UserRepository(dio: dio);
});

class UserRepository {
  final Dio dio;

  UserRepository({required this.dio});

  Future<List<UserModel>> searchUsers(String query) async {
    try {
      final response = await dio.get(
        '${ApiConstants.users}/search',
        queryParameters: {'query': query},
      );
      return (response.data as List<dynamic>)
          .map((json) => UserModel.fromJson(json as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<UserModel> getMe() async {
    try {
      final response = await dio.get(ApiConstants.me);
      return UserModel.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  String _handleError(DioException e) {
    if (e.response?.data is Map && e.response!.data.containsKey('error')) {
      return e.response!.data['error'] as String;
    }
    return 'Ошибка при загрузке пользователей';
  }
}