class MessageResponse {
  final int id;
  final int chatId;
  final int senderId;
  final String senderUsername;
  final String? content;
  final String type;
  final String? fileUrl;
  final int? replyToId;
  final bool isEdited;
  final DateTime createdAt;
  final DateTime updatedAt;

  MessageResponse({
    required this.id,
    required this.chatId,
    required this.senderId,
    required this.senderUsername,
    this.content,
    required this.type,
    this.fileUrl,
    this.replyToId,
    required this.isEdited,
    required this.createdAt,
    required this.updatedAt,
  });

  factory MessageResponse.fromJson(Map<String, dynamic> json) =>
      MessageResponse(
        id: json['id'] as int,
        chatId: json['chatId'] as int,
        senderId: json['senderId'] as int,
        senderUsername: json['senderUsername'] as String,
        content: json['content'] as String?,
        type: json['type'] as String,
        fileUrl: json['fileUrl'] as String?,
        replyToId: json['replyToId'] as int?,
        isEdited: json['isEdited'] as bool,
        createdAt: DateTime.parse(json['createdAt'] as String),
        updatedAt: DateTime.parse(json['updatedAt'] as String),
      );

  bool get isImage => type == 'IMAGE';
  bool get isFile => type == 'FILE';
  bool get isText => type == 'TEXT';
}