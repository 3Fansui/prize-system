package com.test.prizesystem.model.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PrizeVO {
    private Integer id;
    private String name;
    private BigDecimal price;
    private String imageUrl;
}
