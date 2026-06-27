import 'dart:async';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../data/chat_repository.dart';
import '../data/dto/chat_response.dart';
import '../data/dto/message_response.dart';
import '../data/dto/message_request.dart';

const int autoRefreshIntervalMs = 500;

// ==================== ПРОВАЙДЕРЫ ДАННЫХ ====================

final chatListProvider = FutureProvider<List<ChatResponse>>((ref) async {
  final repository = ref.watch(chatRepositoryProvider);
  return await repository.getUserChats();
});

final chatProvider =
    FutureProvider.family<ChatResponse, int>((ref, chatId) async {
  final repository = ref.watch(chatRepositoryProvider);
  return await repository.getChat(chatId);
});

final messagesProvider =
    FutureProvider.family<List<MessageResponse>, int>((ref, chatId) async {
  final repository = ref.watch(chatRepositoryProvider);
  return await repository.getMessages(chatId);
});

// ==================== NOTIFIER ДЛЯ ОТПРАВКИ ====================

class MessageNotifier extends StateNotifier<AsyncValue<void>> {
  final ChatRepository _repository;
  final int chatId;

  MessageNotifier(this._repository, this.chatId)
      : super(const AsyncValue.data(null));

  /// Отправка текстового сообщения
  Future<void> sendMessage(String content, {int? replyToId}) async {
    state = const AsyncValue.loading();
    try {
      final request = MessageRequest(
        content: content,
        type: 'TEXT',
        replyToId: replyToId,
      );
      await _repository.sendMessage(chatId, request);
      state = const AsyncValue.data(null);
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  /// Отправка сообщения с файлом
  Future<void> sendFileMessage(String fileUrl, String type,
      {int? replyToId}) async {
    state = const AsyncValue.loading();
    try {
      final request = MessageRequest(
        fileUrl: fileUrl,
        type: type,
        replyToId: replyToId,
      );
      await _repository.sendMessage(chatId, request);
      state = const AsyncValue.data(null);
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  /// Редактирование текстового сообщения
  Future<void> editMessage(int messageId, String content) async {
    state = const AsyncValue.loading();
    try {
      await _repository.editMessage(chatId, messageId, content);
      state = const AsyncValue.data(null);
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  /// Удаление сообщения
  Future<void> deleteMessage(int messageId) async {
    state = const AsyncValue.loading();
    try {
      await _repository.deleteMessage(chatId, messageId);
      state = const AsyncValue.data(null);
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }
}

final messageNotifierProvider =
    StateNotifierProvider.family<MessageNotifier, AsyncValue<void>, int>(
  (ref, chatId) {
    final repository = ref.watch(chatRepositoryProvider);
    return MessageNotifier(repository, chatId);
  },
);

// ==================== АВТООБНОВЛЕНИЕ ====================

final chatAutoRefreshProvider = Provider<void>((ref) {
  final timer = Timer.periodic(
    const Duration(milliseconds: autoRefreshIntervalMs),
    (_) {
      ref.invalidate(chatListProvider);
    },
  );

  ref.onDispose(() {
    timer.cancel();
  });
});

final messageAutoRefreshProvider = Provider.family<void, int>((ref, chatId) {
  final timer = Timer.periodic(
    const Duration(milliseconds: autoRefreshIntervalMs),
    (_) {
      ref.invalidate(messagesProvider(chatId));
    },
  );

  ref.onDispose(() {
    timer.cancel();
  });
});