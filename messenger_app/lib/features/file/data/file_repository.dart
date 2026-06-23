import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/constants/api_constants.dart';
import '../../../core/network/dio_client.dart';

// ==================== ПРОВАЙДЕР ====================

final fileRepositoryProvider = Provider<FileRepository>((ref) {
  final dio = ref.watch(dioProvider);
  return FileRepository(dio: dio);
});

// ==================== РЕПОЗИТОРИЙ ====================

class FileRepository {
  final Dio dio;

  FileRepository({required this.dio});

  /// Загрузка файла с прогрессом
  /// [onProgress] - callback для отслеживания прогресса (0.0 - 1.0)
  Future<FileUploadResponse> uploadFile(
    String filePath, {
    void Function(double progress)? onProgress,
  }) async {
    try {
      print('📤 Загрузка файла: $filePath');
      print('📤 URL: ${ApiConstants.files}/upload');
      print('📤 Заголовки: ${dio.options.headers}');

      final formData = FormData.fromMap({
        'file': await MultipartFile.fromFile(filePath),
      });

      final response = await dio.post(
        '${ApiConstants.files}/upload',
        data: formData,
        onSendProgress: (sent, total) {
          if (onProgress != null && total > 0) {
            onProgress(sent / total);
          }
        },
      );

      print('✅ Успех! Статус: ${response.statusCode}');
      print('✅ Ответ сервера: ${response.data}');

      return FileUploadResponse.fromJson(
          response.data as Map<String, dynamic>);
    } on DioException catch (e) {
      print('❌ Ошибка Dio: ${e.message}');
      print('❌ Тип ошибки: ${e.type}');
      print('❌ Статус: ${e.response?.statusCode}');
      print('❌ Ответ сервера: ${e.response?.data}');
      print('❌ Заголовки запроса: ${e.requestOptions.headers}');
      throw _handleError(e);
    } catch (e) {
      print('❌ Общая ошибка: $e');
      rethrow;
    }
  }

  /// Получение полного URL для скачивания файла
  String getFileDownloadUrl(int fileId) {
    return '${ApiConstants.files}/$fileId';
  }

  /// Получение полного URL для просмотра изображения
  String getFileViewUrl(int fileId) {
    return '${ApiConstants.files}/$fileId';
  }

  String _handleError(DioException e) {
    if (e.response?.data is Map && e.response!.data.containsKey('error')) {
      return e.response!.data['error'] as String;
    }
    if (e.response?.statusCode == 403) {
      return 'Доступ запрещён (403). Проверь токен и CORS на сервере.';
    }
    if (e.response?.statusCode == 401) {
      return 'Неавторизован (401). Токен истёк или невалиден.';
    }
    if (e.response?.statusCode == 413) {
      return 'Файл слишком большой (превышает 512 МБ).';
    }
    return 'Ошибка при загрузке файла: ${e.message}';
  }
}

// ==================== DTO ОТВЕТА ====================

class FileUploadResponse {
  final int id;
  final String originalName;
  final String mimeType;
  final int size;
  final String url;
  final int? uploadedById;
  final DateTime createdAt;

  FileUploadResponse({
    required this.id,
    required this.originalName,
    required this.mimeType,
    required this.size,
    required this.url,
    this.uploadedById,
    required this.createdAt,
  });

  factory FileUploadResponse.fromJson(Map<String, dynamic> json) =>
      FileUploadResponse(
        id: json['id'] as int,
        originalName: json['originalName'] as String,
        mimeType: json['mimeType'] as String,
        size: json['size'] as int,
        url: json['url'] as String,
        uploadedById: json['uploadedById'] as int?,
        createdAt: DateTime.parse(json['createdAt'] as String),
      );

  bool get isImage => mimeType.startsWith('image/');
}