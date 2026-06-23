import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../data/auth_repository.dart';
import '../data/dto/auth_response.dart';
import '../data/dto/login_request.dart';
import '../data/dto/register_request.dart';

enum AuthStatus { initial, loading, authenticated, unauthenticated, error }

class AuthState {
  final AuthStatus status;
  final AuthResponse? authResponse;
  final String? errorMessage;

  const AuthState({
    this.status = AuthStatus.initial,
    this.authResponse,
    this.errorMessage,
  });

  AuthState copyWith({
    AuthStatus? status,
    AuthResponse? authResponse,
    String? errorMessage,
  }) {
    return AuthState(
      status: status ?? this.status,
      authResponse: authResponse ?? this.authResponse,
      errorMessage: errorMessage,
    );
  }
}

class AuthNotifier extends StateNotifier<AuthState> {
  final AuthRepository _repository;

  AuthNotifier(this._repository) : super(const AuthState());

  Future<void> checkAuthStatus() async {
  final isLoggedIn = await _repository.isLoggedIn();
  if (isLoggedIn) {
    final userId = await _repository.getUserId();
    final username = await _repository.getUsername();
    if (userId != null && username != null) {
      state = state.copyWith(
        status: AuthStatus.authenticated,
        authResponse: AuthResponse(
          accessToken: '', // Токен уже в storage
          refreshToken: '',
          expiresIn: 0,
          userId: int.parse(userId),
          username: username,
        ),
      );
    } else {
      state = const AuthState(status: AuthStatus.unauthenticated);
    }
  } else {
    state = const AuthState(status: AuthStatus.unauthenticated);
  }
}

  Future<void> login(String email, String password) async {
    state = state.copyWith(status: AuthStatus.loading, errorMessage: null);
    try {
      final response = await _repository.login(
        LoginRequest(email: email, password: password),
      );
      state = state.copyWith(
        status: AuthStatus.authenticated,
        authResponse: response,
      );
    } catch (e) {
      state = state.copyWith(
        status: AuthStatus.error,
        errorMessage: e.toString(),
      );
    }
  }

  Future<void> register({
    required String email,
    required String password,
    required String firstName,
    required String lastName,
    required String username,
  }) async {
    state = state.copyWith(status: AuthStatus.loading, errorMessage: null);
    try {
      final response = await _repository.register(
        RegisterRequest(
          email: email,
          password: password,
          firstName: firstName,
          lastName: lastName,
          username: username,
        ),
      );
      state = state.copyWith(
        status: AuthStatus.authenticated,
        authResponse: response,
      );
    } catch (e) {
      state = state.copyWith(
        status: AuthStatus.error,
        errorMessage: e.toString(),
      );
    }
  }

  Future<void> logout() async {
    await _repository.logout();
    state = const AuthState(status: AuthStatus.unauthenticated);
  }

  void clearError() {
    state = state.copyWith(errorMessage: null);
  }
}

final authProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  final repository = ref.watch(authRepositoryProvider);
  return AuthNotifier(repository);
});