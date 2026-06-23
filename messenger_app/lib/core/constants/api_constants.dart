class ApiConstants {
  // Базовые URL микросервисов
  static const String gatewayUrl = 'http://localhost:8080';
  static const String authUrl = 'http://localhost:8081';
  static const String userUrl = 'http://localhost:8082';
  static const String chatUrl = 'http://localhost:8083';
  static const String fileUrl = 'http://localhost:8084';

  // Auth endpoints
  static const String register = '$authUrl/api/auth/register';
  static const String login = '$authUrl/api/auth/login';
  static const String refresh = '$authUrl/api/auth/refresh';

  // User endpoints
  static const String me = '$userUrl/api/users/me';
  static const String users = '$userUrl/api/users';
  static const String contacts = '$userUrl/api/contacts';

  // Chat endpoints
  static const String chats = '$chatUrl/api/chats';

  // File endpoints
  static const String files = '$fileUrl/api/files';

  // Таймауты
  static const int connectTimeout = 15000;
  static const int receiveTimeout = 15000;
}