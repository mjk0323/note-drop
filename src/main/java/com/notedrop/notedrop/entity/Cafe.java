package com.notedrop.notedrop.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cafes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cafe extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String operatingHours;

    @Column(nullable = false, length = 500)
    private String mapUrl;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "cafe_coffee_beans",
        joinColumns = @JoinColumn(name = "cafe_id"),
        inverseJoinColumns = @JoinColumn(name = "coffee_bean_id")
    )
    private List<CoffeeBean> coffeeBeans = new ArrayList<>();
}