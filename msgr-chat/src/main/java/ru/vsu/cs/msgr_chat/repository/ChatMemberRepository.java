package ru.vsu.cs.msgr_chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.vsu.cs.msgr_chat.entity.Chat;
import ru.vsu.cs.msgr_chat.entity.ChatMember;
import ru.vsu.cs.msgr_chat.entity.User;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {
    List<ChatMember> findByChat(Chat chat);
    Optional<ChatMember> findByChatAndUser(Chat chat, User user);
    boolean existsByChatAndUser(Chat chat, User user);
}