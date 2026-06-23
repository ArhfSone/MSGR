package ru.vsu.cs.msgr_user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.vsu.cs.msgr_user.dto.UserResponse;
import ru.vsu.cs.msgr_user.entity.Contact;
import ru.vsu.cs.msgr_user.entity.User;
import ru.vsu.cs.msgr_user.repository.ContactRepository;
import ru.vsu.cs.msgr_user.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;

    public UserResponse addContact(Long userId, String contactUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        User contactUser = userRepository.findByUsername(contactUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь с таким username не найден"));

        if (user.getId().equals(contactUser.getId())) {
            throw new RuntimeException("Нельзя добавить себя в контакты");
        }

        if (contactRepository.existsByUserAndContact(user, contactUser)) {
            throw new RuntimeException("Контакт уже существует");
        }

        Contact contact = Contact.builder()
                .user(user)
                .contact(contactUser)
                .build();

        contactRepository.save(contact);

        return mapToUserResponse(contactUser);
    }

    public List<UserResponse> getContacts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        return contactRepository.findByUser(user).stream()
                .map(contact -> mapToUserResponse(contact.getContact()))
                .collect(Collectors.toList());
    }

    public void removeContact(Long userId, String contactUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        User contactUser = userRepository.findByUsername(contactUsername)
                .orElseThrow(() -> new RuntimeException("Пользователь с таким username не найден"));

        contactRepository.deleteByUserAndContact(user, contactUser);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .username(user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .isEmailVerified(user.getIsEmailVerified())
                .build();
    }
}