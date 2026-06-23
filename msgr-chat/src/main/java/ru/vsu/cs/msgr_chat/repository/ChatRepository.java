package ru.vsu.cs.msgr_chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.vsu.cs.msgr_chat.entity.Chat;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Query("SELECT c FROM Chat c JOIN c.members m WHERE m.user.id = :userId")
    List<Chat> findByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM Chat c WHERE c.type = 'PRIVATE' " +
            "AND c.id IN (SELECT cm.chat.id FROM ChatMember cm WHERE cm.user.id = :user1Id) " +
            "AND c.id IN (SELECT cm.chat.id FROM ChatMember cm WHERE cm.user.id = :user2Id)")
    Optional<Chat> findPrivateChatBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
}