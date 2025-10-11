package com.hritik.Sharded_Saga_Wallet_System.repository;

import com.hritik.Sharded_Saga_Wallet_System.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByNameContainingIgnoreCase(String name);
}

