package com.ashutosh.coderank.Dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

@Data
public class SubmitedCodeDtoNew {

    @NotBlank(message = "Code cannot be blank")
    @Size(min = 10, max = 50000, message = "Code must be between 10 and 50000 characters")
    private String code;

    @NotBlank(message = "Language cannot be blank")
    @Pattern(regexp = "^(python|java|javascript|cpp|csharp)$", 
             message = "Language must be one of: python, java, javascript, cpp, csharp")
    private String language;

    @Size(max = 10000, message = "Stdin must be at most 10000 characters")
    private String stdin;
}
