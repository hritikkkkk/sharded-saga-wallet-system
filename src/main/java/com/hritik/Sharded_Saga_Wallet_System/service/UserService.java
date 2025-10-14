package com.hritik.Sharded_Saga_Wallet_System.service;

import com.hritik.Sharded_Saga_Wallet_System.exceptions.ResourceNotFoundException;
import com.hritik.Sharded_Saga_Wallet_System.model.User;
import com.hritik.Sharded_Saga_Wallet_System.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User createUser(User user) {
        log.info("Creating user with email: {}", user.getEmail());

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("User email cannot be null or empty");
        }

        if (user.getName() == null || user.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be null or empty");
        }

        try {
            User newUser = userRepository.save(user);
            log.info("User created with id {} in database shard {}",
                    newUser.getId(), (newUser.getId() % 2 + 1));
            return newUser;

        } catch (DataAccessException e) {
            log.error("Database error while creating user", e);
            throw new RuntimeException("Failed to create user due to database error", e);
        }
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        log.debug("Fetching user with id {}", id);

        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<User> getUsersByName(String name) {
        log.debug("Fetching users with name containing '{}'", name);

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        return userRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll();
    }
}