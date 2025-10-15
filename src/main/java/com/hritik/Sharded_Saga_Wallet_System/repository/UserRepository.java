package com.hritik.Sharded_Saga_Wallet_System.repository;

import com.hritik.Sharded_Saga_Wallet_System.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByNameContainingIgnoreCase(String name);
}

