class AuthResponse {
  final String accessToken;
  final String refreshToken;
  final int expiresIn;
  final int userId;
  final String username;

  AuthResponse({
    required this.accessToken,
    required this.refreshToken,
    required this.expiresIn,
    required this.userId,
    required this.username,
  });

  factory AuthResponse.fromJson(Map<String, dynamic> json) => AuthResponse(
        accessToken: json['accessToken'] as String,
        refreshToken: json['refreshToken'] as String,
        expiresIn: json['expiresIn'] as int,
        userId: json['userId'] as int,
        username: json['username'] as String,
      );
}