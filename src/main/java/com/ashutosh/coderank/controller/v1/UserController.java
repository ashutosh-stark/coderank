package com.ashutosh.coderank.controller.v1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ashutosh.coderank.Dto.UserDto;
import com.ashutosh.coderank.model.Users;
import com.ashutosh.coderank.service.UserService;

@RestController
@RequestMapping("/auth/v1")
public class UserController {

    // Need to inject the userService
    @Autowired
    UserService userService;

    // user registration
    @PostMapping("/register")
    public ResponseEntity<Users> registerUser(@jakarta.validation.Valid @RequestBody UserDto userDto) {
        // Logic to save user details to the database
       Users users = userService.saveUser(userDto);
       return new ResponseEntity<>(users,HttpStatus.CREATED);
    }

    // login API once the user registration as been completed
    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@jakarta.validation.Valid @RequestBody com.ashutosh.coderank.Dto.LoginDto loginDto) {
        String token = userService.loginUser(loginDto.getUserName(), loginDto.getPassword());
        return new ResponseEntity<>(token, HttpStatus.OK);
    }

}
