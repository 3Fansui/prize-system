package com.test.prizesystem.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrawResponse {
    private Boolean success;
    private String message;
    private PrizeVO prize;

    public DrawResponse(Boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
