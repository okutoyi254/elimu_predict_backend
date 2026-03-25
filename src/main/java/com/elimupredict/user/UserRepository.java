package com.elimupredict.user;

import com.elimupredict.common.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String userName);
    boolean existsByUsername(String userName);
    List<User> findByRole(Role role);
    List<User>findByIsActiveTrue();
}
