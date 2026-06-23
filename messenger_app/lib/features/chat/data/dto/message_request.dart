class MessageRequest {
  final String? content;
  final String? fileUrl;
  final String type;
  final int? replyToId;

  MessageRequest({
    this.content,
    this.fileUrl,
    this.type = 'TEXT',
    this.replyToId,
  });

  Map<String, dynamic> toJson() => {
        if (content != null) 'content': content,
        if (fileUrl != null) 'fileUrl': fileUrl,
        'type': type,
        if (replyToId != null) 'replyToId': replyToId,
      };
}