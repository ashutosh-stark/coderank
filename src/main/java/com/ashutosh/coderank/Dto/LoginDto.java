package com.ashutosh.coderank.Dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class LoginDto {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50)
    private String userName;

    @NotBlank(message = "Password cannot be blank")
    private String password;
}
