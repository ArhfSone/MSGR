class UserModel {
  final int id;
  final String email;
  final String firstName;
  final String lastName;
  final String username;
  final String? avatarUrl;
  final bool isEmailVerified;

  UserModel({
    required this.id,
    required this.email,
    required this.firstName,
    required this.lastName,
    required this.username,
    this.avatarUrl,
    required this.isEmailVerified,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) => UserModel(
        id: json['id'] as int,
        email: json['email'] as String,
        firstName: json['firstName'] as String,
        lastName: json['lastName'] as String,
        username: json['username'] as String,
        avatarUrl: json['avatarUrl'] as String?,
        isEmailVerified: json['isEmailVerified'] as bool,
      );

  String get fullName => '$firstName $lastName';
}