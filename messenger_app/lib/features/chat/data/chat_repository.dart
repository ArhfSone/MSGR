import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/constants/api_constants.dart';
import '../../../core/network/dio_client.dart';
import 'dto/chat_response.dart';
import 'dto/message_response.dart';
import 'dto/message_request.dart';

// ⬇️ ЭТО ДОЛЖНО БЫТЬ В ФАЙЛЕ
final chatRepositoryProvider = Provider<ChatRepository>((ref) {
  final dio = ref.watch(dioProvider);
  return ChatRepository(dio: dio);
});

class ChatRepository {
  final Dio dio;

  ChatRepository({required this.dio});

  Future<List<ChatResponse>> getUserChats() async {
    try {
      final response = await dio.get(ApiConstants.chats);
      return (response.data as List<dynamic>)
          .map((json) => ChatResponse.fromJson(json as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<ChatResponse> getChat(int chatId) async {
    try {
      final response = await dio.get('${ApiConstants.chats}/$chatId');
      return ChatResponse.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<List<MessageResponse>> getMessages(int chatId,
      {int page = 0, int size = 50}) async {
    try {
      final response = await dio.get(
        '${ApiConstants.chats}/$chatId/messages',
        queryParameters: {'page': page, 'size': size},
      );
      return (response.data as List<dynamic>)
          .map((json) => MessageResponse.fromJson(json as Map<String, dynamic>))
          .toList();
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<MessageResponse> sendMessage(int chatId, MessageRequest request) async {
    try {
      final response = await dio.post(
        '${ApiConstants.chats}/$chatId/messages',
        data: request.toJson(),
      );
      return MessageResponse.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<MessageResponse> editMessage(
      int chatId, int messageId, String content) async {
    try {
      final response = await dio.put(
        '${ApiConstants.chats}/$chatId/messages/$messageId',
        data: {'content': content},
      );
      return MessageResponse.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<void> deleteMessage(int chatId, int messageId) async {
    try {
      await dio.delete('${ApiConstants.chats}/$chatId/messages/$messageId');
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<ChatResponse> createPrivateChat(String targetUsername) async {
    try {
      final response = await dio.post(
        ApiConstants.chats,
        data: {'targetUsername': targetUsername},
      );
      return ChatResponse.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<ChatResponse> createGroup(
      String name, List<String> memberUsernames,
      {String? avatarUrl}) async {
    try {
      final data = <String, dynamic>{
        'name': name,
        'memberUsernames': memberUsernames,
      };
      if (avatarUrl != null) {
        data['avatarUrl'] = avatarUrl;
      }

      final response = await dio.post(
        '${ApiConstants.chats}/group',
        data: data,
      );
      return ChatResponse.fromJson(response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  String _handleError(DioException e) {
    if (e.response?.data is Map && e.response!.data.containsKey('error')) {
      return e.response!.data['error'] as String;
    }
    return 'Ошибка при загрузке данных';
  }
}