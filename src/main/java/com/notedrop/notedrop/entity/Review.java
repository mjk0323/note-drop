package com.notedrop.notedrop.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coffee_bean_id", nullable = false)
    private CoffeeBean coffeeBean;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal acidityRating;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal bitternessRating;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal sweetnessRating;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal bodyRating;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal aromaRating;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal overallRating;

    @Column(columnDefinition = "text")
    private String content;
}
