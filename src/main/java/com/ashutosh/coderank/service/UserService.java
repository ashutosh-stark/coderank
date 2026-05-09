package com.ashutosh.coderank.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ashutosh.coderank.Dto.UserDto;
import com.ashutosh.coderank.constant.UserConstant;
import com.ashutosh.coderank.exceptions.UsersExceptions;
import com.ashutosh.coderank.model.Users;
import com.ashutosh.coderank.repository.UserRepository;
import com.ashutosh.coderank.util.TokenUtil;

@Service
public class UserService {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TokenUtil tokenUtil;

    public Users saveUser(UserDto userDto) {

        if (userRepository.findByUserName(userDto.getUserName()).isPresent()) {
            throw new UsersExceptions("Username already exists");
        }

        Users user = new Users();
        user.setUserName(userDto.getUserName());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setEmail(userDto.getEmail());
        user.setCreated_at(LocalDateTime.now());
        user.setRole(UserConstant.ROLE_USER);
        userRepository.save(user);
        return user;
    }

    // Login API once the user registration as been completed

    public String loginUser(String userName, String password) {
        Optional<Users> users = userRepository.findByUserName(userName);

        if(!users.isPresent()){
            throw new UsersExceptions("Invalid username or password");
        }
        Users getUsers = users.get();

        if (passwordEncoder.matches(password, getUsers.getPassword())) {
            return tokenUtil.generateToken(getUsers);
        }
        throw new UsersExceptions("Invalid username or password");   
    }
}