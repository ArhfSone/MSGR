class ChatResponse {
  final int id;
  final String type;
  final String? name;
  final String? avatarUrl;
  final int? createdById;
  final String? createdByUsername;
  final List<ChatMemberResponse> members;
  final DateTime createdAt;
  final DateTime updatedAt;

  ChatResponse({
    required this.id,
    required this.type,
    this.name,
    this.avatarUrl,
    this.createdById,
    this.createdByUsername,
    required this.members,
    required this.createdAt,
    required this.updatedAt,
  });

  factory ChatResponse.fromJson(Map<String, dynamic> json) => ChatResponse(
        id: json['id'] as int,
        type: json['type'] as String,
        name: json['name'] as String?,
        avatarUrl: json['avatarUrl'] as String?,
        createdById: json['createdById'] as int?,
        createdByUsername: json['createdByUsername'] as String?,
        members: (json['members'] as List<dynamic>)
            .map((m) => ChatMemberResponse.fromJson(m as Map<String, dynamic>))
            .toList(),
        createdAt: DateTime.parse(json['createdAt'] as String),
        updatedAt: DateTime.parse(json['updatedAt'] as String),
      );

  // Для личного чата получаем имя собеседника
  String getDisplayName(int currentUserId) {
    if (type == 'PRIVATE') {
      final otherMember = members.firstWhere(
        (m) => m.userId != currentUserId,
        orElse: () => members.first,
      );
      return otherMember.username;
    }
    return name ?? 'Группа';
  }
}

class ChatMemberResponse {
  final int userId;
  final String username;
  final String firstName;
  final String lastName;
  final String role;

  ChatMemberResponse({
    required this.userId,
    required this.username,
    required this.firstName,
    required this.lastName,
    required this.role,
  });

  factory ChatMemberResponse.fromJson(Map<String, dynamic> json) =>
      ChatMemberResponse(
        userId: json['userId'] as int,
        username: json['username'] as String,
        firstName: json['firstName'] as String,
        lastName: json['lastName'] as String,
        role: json['role'] as String,
      );
}