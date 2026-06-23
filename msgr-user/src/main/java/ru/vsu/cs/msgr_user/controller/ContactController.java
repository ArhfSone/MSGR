package ru.vsu.cs.msgr_user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.vsu.cs.msgr_user.dto.UserResponse;
import ru.vsu.cs.msgr_user.service.ContactService;
import java.util.List;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    public ResponseEntity<UserResponse> addContact(
            Authentication authentication,
            @RequestParam String username) {
        Long userId = (Long) authentication.getPrincipal();
        UserResponse response = contactService.addContact(userId, username);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getContacts(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<UserResponse> contacts = contactService.getContacts(userId);
        return ResponseEntity.ok(contacts);
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Void> removeContact(
            Authentication authentication,
            @PathVariable String username) {
        Long userId = (Long) authentication.getPrincipal();
        contactService.removeContact(userId, username);
        return ResponseEntity.noContent().build();
    }
}