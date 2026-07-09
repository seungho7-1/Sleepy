package com.sleepyproject.sleepy_backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //상품명
    private String name;
    //가격
    private int price;
    //이미지저장주소
    private String imageUrl;

    //상품 설명
    private String description;

    //등록시간
    private LocalDateTime createdAt;
}