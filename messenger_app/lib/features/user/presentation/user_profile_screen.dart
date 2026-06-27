import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/constants/api_constants.dart';
import '../../../core/theme/app_theme.dart';
import '../data/dto/user_model.dart';
import '../data/user_repository.dart';

class UserProfileScreen extends ConsumerWidget {
  final String username;
  final String? fallbackFirstName;
  final String? fallbackLastName;

  const UserProfileScreen({
    super.key,
    required this.username,
    this.fallbackFirstName,
    this.fallbackLastName,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final userAsync = ref.watch(_userProfileProvider(username));

    return Scaffold(
      appBar: AppBar(
        title: const Text('Профиль'),
      ),
      body: userAsync.when(
        data: (user) => _ProfileBody(user: user),
        loading: () => _ProfileBody(
          user: UserModel(
            id: 0,
            email: '',
            firstName: fallbackFirstName ?? '',
            lastName: fallbackLastName ?? '',
            username: username,
            isEmailVerified: false,
          ),
          isLoading: true,
        ),
        error: (_, __) => _ProfileBody(
          user: UserModel(
            id: 0,
            email: '',
            firstName: fallbackFirstName ?? '',
            lastName: fallbackLastName ?? '',
            username: username,
            isEmailVerified: false,
          ),
        ),
      ),
    );
  }
}

final _userProfileProvider =
    FutureProvider.family<UserModel, String>((ref, username) async {
  final repository = ref.watch(userRepositoryProvider);
  final user = await repository.getUserByUsername(username);
  if (user == null) {
    throw Exception('Пользователь не найден');
  }
  return user;
});

class _ProfileBody extends StatelessWidget {
  final UserModel user;
  final bool isLoading;

  const _ProfileBody({
    required this.user,
    this.isLoading = false,
  });

  String? get _avatarUrl {
    final url = user.avatarUrl;
    if (url == null || url.isEmpty) return null;
    if (url.startsWith('http')) return url;
    return '${ApiConstants.fileUrl}$url';
  }

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 500),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (isLoading)
                const Padding(
                  padding: EdgeInsets.only(bottom: 16),
                  child: LinearProgressIndicator(),
                ),
              _buildAvatar(),
              const SizedBox(height: 24),
              Text(
                user.fullName.isNotEmpty ? user.fullName : '@${user.username}',
                style: const TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                '@${user.username}',
                style: const TextStyle(
                  color: AppTheme.primaryColor,
                  fontSize: 16,
                ),
              ),
              if (user.email.isNotEmpty) ...[
                const SizedBox(height: 32),
                Card(
                  child: Column(
                    children: [
                      ListTile(
                        leading: const Icon(Icons.email_outlined),
                        title: const Text('Email'),
                        subtitle: Text(user.email),
                      ),
                      if (user.isEmailVerified)
                        const ListTile(
                          leading: Icon(Icons.verified, color: Colors.green),
                          title: Text('Email подтверждён'),
                        ),
                    ],
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildAvatar() {
    final url = _avatarUrl;
    if (url != null) {
      return CircleAvatar(
        radius: 60,
        backgroundColor: AppTheme.primaryColor,
        backgroundImage: CachedNetworkImageProvider(url),
      );
    }
    return CircleAvatar(
      radius: 60,
      backgroundColor: AppTheme.primaryColor,
      child: Text(
        user.username.isNotEmpty ? user.username[0].toUpperCase() : '?',
        style: const TextStyle(fontSize: 48, color: Colors.white),
      ),
    );
  }
}
