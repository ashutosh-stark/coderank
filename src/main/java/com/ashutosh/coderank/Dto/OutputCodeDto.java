package com.ashutosh.coderank.Dto;

import lombok.Data;

@Data
public class OutputCodeDto {
    
    private String stdout;
    private String stderr;
    private String compile_output;
    private String message;
    
}
