import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/constants/api_constants.dart';
import '../../../core/theme/app_theme.dart';
import '../../auth/providers/auth_provider.dart';
import '../../user/data/dto/user_model.dart';
import '../providers/profile_provider.dart';
import 'edit_profile_screen.dart';

class ProfileScreen extends ConsumerWidget {
  const ProfileScreen({super.key});

  String? _resolveAvatarUrl(String? url) {
    if (url == null || url.isEmpty) return null;
    if (url.startsWith('http')) return url;
    return '${ApiConstants.fileUrl}$url';
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final profileAsync = ref.watch(currentUserProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Профиль'),
        actions: [
          IconButton(
            icon: const Icon(Icons.edit_outlined),
            tooltip: 'Редактировать',
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const EditProfileScreen()),
              ).then((_) => ref.invalidate(currentUserProvider));
            },
          ),
          IconButton(
            icon: const Icon(Icons.logout),
            tooltip: 'Выйти',
            onPressed: () => _logout(context, ref),
          ),
        ],
      ),
      body: profileAsync.when(
        data: (user) => _ProfileContent(
          user: user,
          avatarUrl: _resolveAvatarUrl(user.avatarUrl),
        ),
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text('Ошибка: $error'),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: () => ref.invalidate(currentUserProvider),
                child: const Text('Повторить'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _logout(BuildContext context, WidgetRef ref) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Выход'),
        content: const Text('Вы уверены, что хотите выйти?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Отмена'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Выйти'),
          ),
        ],
      ),
    );
    if (confirm == true && context.mounted) {
      await ref.read(authProvider.notifier).logout();
      if (context.mounted) {
        Navigator.of(context).popUntil((route) => route.isFirst);
      }
    }
  }
}

class _ProfileContent extends StatelessWidget {
  final UserModel user;
  final String? avatarUrl;

  const _ProfileContent({
    required this.user,
    this.avatarUrl,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 500),
          child: Column(
            children: [
              CircleAvatar(
                radius: 60,
                backgroundColor: AppTheme.primaryColor,
                backgroundImage:
                    avatarUrl != null ? CachedNetworkImageProvider(avatarUrl!) : null,
                child: avatarUrl == null
                    ? Text(
                        user.username.isNotEmpty
                            ? user.username[0].toUpperCase()
                            : '?',
                        style: const TextStyle(fontSize: 48, color: Colors.white),
                      )
                    : null,
              ),
              const SizedBox(height: 24),
              Text(
                user.fullName.isNotEmpty ? user.fullName : '@${user.username}',
                style: const TextStyle(fontSize: 28, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Text(
                '@${user.username}',
                style: const TextStyle(
                  color: AppTheme.primaryColor,
                  fontSize: 16,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                'ID: ${user.id}',
                style: const TextStyle(color: AppTheme.textSecondary, fontSize: 14),
              ),
              const SizedBox(height: 32),
              Card(
                child: Column(
                  children: [
                    ListTile(
                      leading: const Icon(Icons.email_outlined),
                      title: const Text('Email'),
                      subtitle: Text(user.email),
                    ),
                    ListTile(
                      leading: Icon(
                        user.isEmailVerified ? Icons.verified : Icons.warning_amber,
                        color: user.isEmailVerified ? Colors.green : Colors.orange,
                      ),
                      title: Text(
                        user.isEmailVerified
                            ? 'Email подтверждён'
                            : 'Email не подтверждён',
                      ),
                    ),
                    if (user.firstName.isNotEmpty)
                      ListTile(
                        leading: const Icon(Icons.person_outline),
                        title: const Text('Имя'),
                        subtitle: Text(user.firstName),
                      ),
                    if (user.lastName.isNotEmpty)
                      ListTile(
                        leading: const Icon(Icons.person_outline),
                        title: const Text('Фамилия'),
                        subtitle: Text(user.lastName),
                      ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
