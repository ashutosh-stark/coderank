package com.ashutosh.coderank.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ashutosh.coderank.model.Users;

@Repository
public interface UserRepository extends JpaRepository<Users, UUID> {

    Optional<Users> findByUserName(String userName);
}
