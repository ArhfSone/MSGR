import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../user/data/dto/update_profile_request.dart';
import '../../user/data/dto/user_model.dart';
import '../../user/data/user_repository.dart';

final currentUserProvider = FutureProvider<UserModel>((ref) async {
  final repository = ref.watch(userRepositoryProvider);
  return repository.getMe();
});

class ProfileNotifier extends StateNotifier<AsyncValue<UserModel?>> {
  final UserRepository _repository;

  ProfileNotifier(this._repository) : super(const AsyncValue.loading()) {
    loadProfile();
  }

  Future<void> loadProfile() async {
    state = const AsyncValue.loading();
    try {
      final user = await _repository.getMe();
      state = AsyncValue.data(user);
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  Future<void> updateProfile(UpdateProfileRequest request) async {
    state = const AsyncValue.loading();
    try {
      final user = await _repository.updateProfile(request);
      state = AsyncValue.data(user);
    } catch (e, st) {
      state = AsyncValue.error(e, st);
      rethrow;
    }
  }
}

final profileNotifierProvider =
    StateNotifierProvider<ProfileNotifier, AsyncValue<UserModel?>>((ref) {
  final repository = ref.watch(userRepositoryProvider);
  return ProfileNotifier(repository);
});
