package ru.vsu.cs.msgr_user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.vsu.cs.msgr_user.entity.Contact;
import ru.vsu.cs.msgr_user.entity.User;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByUser(User user);
    Optional<Contact> findByUserAndContact(User user, User contact);
    boolean existsByUserAndContact(User user, User contact);
    void deleteByUserAndContact(User user, User contact);
}