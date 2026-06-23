package ru.vsu.cs.msgr_file.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.vsu.cs.msgr_file.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}