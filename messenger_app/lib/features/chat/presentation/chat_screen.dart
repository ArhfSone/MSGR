import 'package:cached_network_image/cached_network_image.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../core/constants/api_constants.dart';
import '../../../core/theme/app_theme.dart';
import '../../auth/providers/auth_provider.dart';
import '../../file/data/file_repository.dart';
import '../../user/presentation/user_profile_screen.dart';
import '../data/dto/chat_response.dart';
import '../data/dto/message_response.dart';
import '../providers/chat_provider.dart';

class ChatScreen extends ConsumerStatefulWidget {
  final int chatId;

  const ChatScreen({super.key, required this.chatId});

  @override
  ConsumerState<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends ConsumerState<ChatScreen> {
  final _messageController = TextEditingController();
  final _scrollController = ScrollController();
  final _messageFocusNode = FocusNode();
  bool _isUploading = false;
  double _uploadProgress = 0.0;
  MessageResponse? _replyToMessage;
  MessageResponse? _editingMessage;

  @override
  void dispose() {
    _messageController.dispose();
    _scrollController.dispose();
    _messageFocusNode.dispose();
    super.dispose();
  }

  void _cancelReply() => setState(() => _replyToMessage = null);

  void _startReply(MessageResponse message) {
    setState(() {
      _replyToMessage = message;
      _editingMessage = null;
    });
    _messageFocusNode.requestFocus();
  }

  void _startEdit(MessageResponse message) {
    setState(() {
      _editingMessage = message;
      _replyToMessage = null;
      _messageController.text = message.content ?? '';
    });
    _messageFocusNode.requestFocus();
  }

  void _cancelEdit() {
    setState(() => _editingMessage = null);
    _messageController.clear();
  }

  Future<void> _sendOrSave() async {
    final text = _messageController.text.trim();
    if (text.isEmpty) return;

    final notifier = ref.read(messageNotifierProvider(widget.chatId).notifier);

    if (_editingMessage != null) {
      await notifier.editMessage(_editingMessage!.id, text);
      if (mounted) {
        _cancelEdit();
        ref.invalidate(messagesProvider(widget.chatId));
      }
      return;
    }

    await notifier.sendMessage(text, replyToId: _replyToMessage?.id);
    _messageController.clear();
    _cancelReply();
  }

  Future<void> _confirmDelete(MessageResponse message) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Удалить сообщение?'),
        content: const Text('Это действие нельзя отменить.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Отмена'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: TextButton.styleFrom(foregroundColor: Colors.red),
            child: const Text('Удалить'),
          ),
        ],
      ),
    );

    if (confirm == true && mounted) {
      await ref
          .read(messageNotifierProvider(widget.chatId).notifier)
          .deleteMessage(message.id);
      ref.invalidate(messagesProvider(widget.chatId));
    }
  }

  Future<void> _pickAndUploadFile() async {
    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['jpg', 'jpeg', 'png'],
        allowMultiple: false,
      );

      if (result == null || result.files.isEmpty) return;

      final file = result.files.first;
      if (file.path == null) {
        _showError('Не удалось получить путь к файлу');
        return;
      }

      setState(() {
        _isUploading = true;
        _uploadProgress = 0.0;
      });

      final fileRepo = ref.read(fileRepositoryProvider);
      final uploadResponse = await fileRepo.uploadFile(
        file.path!,
        onProgress: (progress) {
          if (mounted) setState(() => _uploadProgress = progress);
        },
      );

      final type = uploadResponse.isImage ? 'IMAGE' : 'FILE';
      final fileUrl = '${ApiConstants.fileUrl}${uploadResponse.url}';

      await ref.read(messageNotifierProvider(widget.chatId).notifier).sendFileMessage(
            fileUrl,
            type,
            replyToId: _replyToMessage?.id,
          );

      if (mounted) {
        setState(() {
          _isUploading = false;
          _uploadProgress = 0.0;
        });
        _cancelReply();
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isUploading = false;
          _uploadProgress = 0.0;
        });
        _showError('Ошибка загрузки: $e');
      }
    }
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: Colors.red),
    );
  }

  void _showImageDialog(String imageUrl) {
    showDialog(
      context: context,
      builder: (context) => Dialog(
        backgroundColor: Colors.black,
        child: Stack(
          children: [
            InteractiveViewer(
              child: CachedNetworkImage(
                imageUrl: imageUrl,
                fit: BoxFit.contain,
                placeholder: (context, url) => const Center(
                  child: CircularProgressIndicator(color: Colors.white),
                ),
                errorWidget: (context, url, error) => const Center(
                  child: Icon(Icons.broken_image, color: Colors.red, size: 100),
                ),
              ),
            ),
            Positioned(
              top: 10,
              right: 10,
              child: IconButton(
                icon: const Icon(Icons.close, color: Colors.white, size: 30),
                onPressed: () => Navigator.pop(context),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _openChatProfile(ChatResponse chat, int currentUserId) {
    if (chat.type == 'PRIVATE') {
      final other = chat.members.firstWhere(
        (m) => m.userId != currentUserId,
        orElse: () => chat.members.first,
      );
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => UserProfileScreen(
            username: other.username,
            fallbackFirstName: other.firstName,
            fallbackLastName: other.lastName,
          ),
        ),
      );
    } else {
      showModalBottomSheet(
        context: context,
        backgroundColor: AppTheme.surfaceColor,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
        ),
        builder: (ctx) => _GroupMembersSheet(chat: chat),
      );
    }
  }

  void _showMessageMenu(MessageResponse message, bool isMe, Offset position) {
    final items = <PopupMenuEntry<String>>[
      const PopupMenuItem(value: 'reply', child: Text('Ответить')),
      if (isMe && message.isText)
        const PopupMenuItem(value: 'edit', child: Text('Редактировать')),
      if (isMe)
        const PopupMenuItem(
          value: 'delete',
          child: Text('Удалить', style: TextStyle(color: Colors.red)),
        ),
    ];

    showMenu<String>(
      context: context,
      position: RelativeRect.fromLTRB(
        position.dx,
        position.dy,
        position.dx + 1,
        position.dy + 1,
      ),
      items: items,
    ).then((value) {
      if (value == null) return;
      switch (value) {
        case 'reply':
          _startReply(message);
        case 'edit':
          _startEdit(message);
        case 'delete':
          _confirmDelete(message);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final messagesAsync = ref.watch(messagesProvider(widget.chatId));
    final chatAsync = ref.watch(chatProvider(widget.chatId));
    final authState = ref.watch(authProvider);
    final currentUserId = authState.authResponse?.userId ?? 0;

    ref.watch(messageAutoRefreshProvider(widget.chatId));

    ref.listen<AsyncValue<void>>(messageNotifierProvider(widget.chatId),
        (previous, next) {
      if (next.hasError) {
        _showError('Ошибка: ${next.error}');
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: chatAsync.when(
          data: (chat) => GestureDetector(
            onTap: () => _openChatProfile(chat, currentUserId),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  chat.getDisplayName(currentUserId),
                  style: const TextStyle(fontSize: 18),
                ),
                Text(
                  chat.type == 'PRIVATE'
                      ? 'Нажмите для просмотра профиля'
                      : '${chat.members.length} участников',
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppTheme.textSecondary,
                    fontWeight: FontWeight.normal,
                  ),
                ),
              ],
            ),
          ),
          loading: () => const Text('Чат'),
          error: (_, __) => const Text('Чат'),
        ),
        actions: [
          chatAsync.whenOrNull(
            data: (chat) => IconButton(
              icon: const Icon(Icons.info_outline),
              tooltip: 'Информация',
              onPressed: () => _openChatProfile(chat, currentUserId),
            ),
          ) ?? const SizedBox.shrink(),
        ],
      ),
      body: Column(
        children: [
          if (_isUploading) _UploadProgressBar(progress: _uploadProgress),
          Expanded(
            child: messagesAsync.when(
              data: (messages) {
                if (messages.isEmpty) {
                  return const Center(
                    child: Text(
                      'Нет сообщений. Начните переписку!',
                      style: TextStyle(color: Colors.grey),
                    ),
                  );
                }

                final messageMap = {for (final m in messages) m.id: m};
                final reversedMessages = messages.reversed.toList();

                return ListView.builder(
                  controller: _scrollController,
                  padding: const EdgeInsets.all(16),
                  itemCount: reversedMessages.length,
                  itemBuilder: (context, index) {
                    final message = reversedMessages[index];
                    final isMe = message.senderId == currentUserId;
                    return _MessageBubble(
                      message: message,
                      isMe: isMe,
                      replyToMessage: message.replyToId != null
                          ? messageMap[message.replyToId]
                          : null,
                      onImageTap: _showImageDialog,
                      onContextMenu: (pos) =>
                          _showMessageMenu(message, isMe, pos),
                    );
                  },
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (error, stack) => Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.error_outline, size: 60, color: Colors.red),
                    const SizedBox(height: 16),
                    Text('Ошибка: $error'),
                    const SizedBox(height: 16),
                    ElevatedButton(
                      onPressed: () =>
                          ref.invalidate(messagesProvider(widget.chatId)),
                      child: const Text('Повторить'),
                    ),
                  ],
                ),
              ),
            ),
          ),
          if (_replyToMessage != null)
            _ActionPreviewBar(
              icon: Icons.reply,
              title: 'Ответ на сообщение',
              subtitle: _replyPreviewText(_replyToMessage!),
              onClose: _cancelReply,
            ),
          if (_editingMessage != null)
            _ActionPreviewBar(
              icon: Icons.edit,
              title: 'Редактирование',
              subtitle: _editingMessage!.content ?? '',
              onClose: _cancelEdit,
              accentColor: Colors.orange,
            ),
          _MessageInputBar(
            controller: _messageController,
            focusNode: _messageFocusNode,
            isUploading: _isUploading,
            isEditing: _editingMessage != null,
            onSend: _sendOrSave,
            onAttach: _pickAndUploadFile,
          ),
        ],
      ),
    );
  }

  String _replyPreviewText(MessageResponse message) {
    if (message.content != null && message.content!.isNotEmpty) {
      return message.content!;
    }
    if (message.isImage) return '📷 Изображение';
    if (message.isFile) return '📎 Файл';
    return '';
  }
}

class _UploadProgressBar extends StatelessWidget {
  final double progress;

  const _UploadProgressBar({required this.progress});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(8),
      color: AppTheme.surfaceColor,
      child: Row(
        children: [
          const Icon(Icons.cloud_upload, color: AppTheme.primaryColor),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Загрузка файла...'),
                const SizedBox(height: 4),
                LinearProgressIndicator(
                  value: progress,
                  backgroundColor: Colors.grey[700],
                  valueColor: const AlwaysStoppedAnimation<Color>(
                    AppTheme.primaryColor,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 12),
          Text('${(progress * 100).toInt()}%'),
        ],
      ),
    );
  }
}

class _ActionPreviewBar extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onClose;
  final Color accentColor;

  const _ActionPreviewBar({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onClose,
    this.accentColor = AppTheme.primaryColor,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: BoxDecoration(
        color: AppTheme.surfaceColor,
        border: Border(
          left: BorderSide(color: accentColor, width: 3),
        ),
      ),
      child: Row(
        children: [
          Icon(icon, color: accentColor, size: 20),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: TextStyle(
                    color: accentColor,
                    fontWeight: FontWeight.bold,
                    fontSize: 13,
                  ),
                ),
                Text(
                  subtitle,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontSize: 13),
                ),
              ],
            ),
          ),
          IconButton(
            icon: const Icon(Icons.close, size: 20),
            onPressed: onClose,
          ),
        ],
      ),
    );
  }
}

class _MessageInputBar extends StatelessWidget {
  final TextEditingController controller;
  final FocusNode focusNode;
  final bool isUploading;
  final bool isEditing;
  final VoidCallback onSend;
  final VoidCallback onAttach;

  const _MessageInputBar({
    required this.controller,
    required this.focusNode,
    required this.isUploading,
    required this.isEditing,
    required this.onSend,
    required this.onAttach,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        color: AppTheme.surfaceColor,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 4,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: Row(
        children: [
          IconButton(
            icon: const Icon(Icons.attach_file, color: AppTheme.primaryColor),
            tooltip: 'Прикрепить изображение',
            onPressed: isUploading ? null : onAttach,
          ),
          Expanded(
            child: TextField(
              controller: controller,
              focusNode: focusNode,
              maxLines: null,
              decoration: InputDecoration(
                hintText: isEditing ? 'Измените сообщение...' : 'Введите сообщение...',
                border: InputBorder.none,
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 12,
                ),
              ),
              onSubmitted: (_) => onSend(),
            ),
          ),
          IconButton(
            icon: Icon(
              isEditing ? Icons.check : Icons.send,
              color: AppTheme.primaryColor,
            ),
            onPressed: onSend,
          ),
        ],
      ),
    );
  }
}

class _MessageBubble extends StatelessWidget {
  final MessageResponse message;
  final bool isMe;
  final MessageResponse? replyToMessage;
  final Function(String) onImageTap;
  final void Function(Offset position) onContextMenu;

  const _MessageBubble({
    required this.message,
    required this.isMe,
    this.replyToMessage,
    required this.onImageTap,
    required this.onContextMenu,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onSecondaryTapUp: (details) => onContextMenu(details.globalPosition),
      onLongPress: () {
        final box = context.findRenderObject() as RenderBox?;
        final pos = box?.localToGlobal(Offset.zero) ?? Offset.zero;
        onContextMenu(pos);
      },
      child: Align(
        alignment: isMe ? Alignment.centerRight : Alignment.centerLeft,
        child: Container(
          margin: const EdgeInsets.symmetric(vertical: 4),
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
          constraints: BoxConstraints(
            maxWidth: MediaQuery.of(context).size.width * 0.7,
          ),
          decoration: BoxDecoration(
            color: isMe ? AppTheme.primaryColor : AppTheme.surfaceColor,
            borderRadius: BorderRadius.circular(12),
          ),
          child: Column(
            crossAxisAlignment:
                isMe ? CrossAxisAlignment.end : CrossAxisAlignment.start,
            children: [
              if (!isMe)
                Padding(
                  padding: const EdgeInsets.only(bottom: 4),
                  child: Text(
                    message.senderUsername,
                    style: const TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                      color: AppTheme.primaryColor,
                    ),
                  ),
                ),
              if (replyToMessage != null) _ReplyQuote(replyToMessage!, isMe),
              if (message.isImage && message.fileUrl != null)
                GestureDetector(
                  onTap: () => onImageTap(message.fileUrl!),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(8),
                    child: CachedNetworkImage(
                      imageUrl: message.fileUrl!,
                      fit: BoxFit.cover,
                      width: 200,
                      height: 200,
                      placeholder: (context, url) => Container(
                        width: 200,
                        height: 200,
                        color: Colors.grey[800],
                        child: const Center(
                          child: CircularProgressIndicator(color: Colors.white),
                        ),
                      ),
                      errorWidget: (context, url, error) => Container(
                        width: 200,
                        height: 200,
                        color: Colors.grey[800],
                        child: const Icon(
                          Icons.broken_image,
                          color: Colors.red,
                          size: 50,
                        ),
                      ),
                    ),
                  ),
                ),
              if (message.isFile && message.fileUrl != null)
                InkWell(
                  onTap: () => _openFile(context),
                  borderRadius: BorderRadius.circular(8),
                  child: Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.black.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: const Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.insert_drive_file, color: Colors.white),
                        SizedBox(width: 8),
                        Text('Файл', style: TextStyle(color: Colors.white)),
                        SizedBox(width: 4),
                        Icon(Icons.open_in_new, size: 16, color: Colors.white70),
                      ],
                    ),
                  ),
                ),
              if (message.content != null && message.content!.isNotEmpty)
                Text(message.content!, style: const TextStyle(fontSize: 15)),
              const SizedBox(height: 4),
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    _formatTime(message.createdAt),
                    style: TextStyle(
                      fontSize: 11,
                      color: isMe ? Colors.white70 : Colors.grey,
                    ),
                  ),
                  if (message.isEdited) ...[
                    const SizedBox(width: 6),
                    Text(
                      'изм.',
                      style: TextStyle(
                        fontSize: 11,
                        color: isMe ? Colors.white70 : Colors.grey,
                        fontStyle: FontStyle.italic,
                      ),
                    ),
                  ],
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _openFile(BuildContext context) async {
    if (message.fileUrl == null) return;
    try {
      final uri = Uri.parse(message.fileUrl!);
      if (await canLaunchUrl(uri)) {
        await launchUrl(uri, mode: LaunchMode.externalApplication);
      } else if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Не удалось открыть файл')),
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Не удалось открыть файл: $e')),
        );
      }
    }
  }

  String _formatTime(DateTime date) {
    return '${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}';
  }
}

class _ReplyQuote extends StatelessWidget {
  final MessageResponse replyTo;
  final bool isMe;

  const _ReplyQuote(this.replyTo, this.isMe);

  @override
  Widget build(BuildContext context) {
    final preview = replyTo.content?.isNotEmpty == true
        ? replyTo.content!
        : replyTo.isImage
            ? '📷 Изображение'
            : replyTo.isFile
                ? '📎 Файл'
                : '';

    return Container(
      margin: const EdgeInsets.only(bottom: 6),
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: Colors.black.withOpacity(0.15),
        borderRadius: BorderRadius.circular(6),
        border: Border(
          left: BorderSide(
            color: isMe ? Colors.white70 : AppTheme.primaryColor,
            width: 3,
          ),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            replyTo.senderUsername,
            style: TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.bold,
              color: isMe ? Colors.white : AppTheme.primaryColor,
            ),
          ),
          Text(
            preview,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(
              fontSize: 12,
              color: isMe ? Colors.white70 : AppTheme.textSecondary,
            ),
          ),
        ],
      ),
    );
  }
}

class _GroupMembersSheet extends StatelessWidget {
  final ChatResponse chat;

  const _GroupMembersSheet({required this.chat});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            chat.name ?? 'Группа',
            style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 8),
          Text(
            '${chat.members.length} участников',
            style: const TextStyle(color: AppTheme.textSecondary),
          ),
          const SizedBox(height: 16),
          Flexible(
            child: ListView.builder(
              shrinkWrap: true,
              itemCount: chat.members.length,
              itemBuilder: (context, index) {
                final member = chat.members[index];
                return ListTile(
                  leading: CircleAvatar(
                    backgroundColor: AppTheme.primaryColor,
                    child: Text(
                      member.username[0].toUpperCase(),
                      style: const TextStyle(color: Colors.white),
                    ),
                  ),
                  title: Text(
                    member.firstName.isNotEmpty
                        ? '${member.firstName} ${member.lastName}'.trim()
                        : '@${member.username}',
                  ),
                  subtitle: Text('@${member.username} • ${member.role}'),
                  onTap: () {
                    Navigator.pop(context);
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => UserProfileScreen(
                          username: member.username,
                          fallbackFirstName: member.firstName,
                          fallbackLastName: member.lastName,
                        ),
                      ),
                    );
                  },
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
