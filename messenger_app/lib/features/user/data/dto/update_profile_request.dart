class UpdateProfileRequest {
  final String? firstName;
  final String? lastName;
  final String? username;
  final String? avatarUrl;

  UpdateProfileRequest({
    this.firstName,
    this.lastName,
    this.username,
    this.avatarUrl,
  });

  Map<String, dynamic> toJson() => {
        if (firstName != null) 'firstName': firstName,
        if (lastName != null) 'lastName': lastName,
        if (username != null) 'username': username,
        if (avatarUrl != null) 'avatarUrl': avatarUrl,
      };
}
