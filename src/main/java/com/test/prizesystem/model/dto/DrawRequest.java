package com.test.prizesystem.model.dto;


import lombok.Data;

@Data
public class DrawRequest {
    private Integer userId;
    private Integer activityId;
}