package ru.vsu.cs.msgr_chat.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.vsu.cs.msgr_chat.entity.Chat;
import ru.vsu.cs.msgr_chat.entity.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByChatOrderByCreatedAtDesc(Chat chat, Pageable pageable);
}