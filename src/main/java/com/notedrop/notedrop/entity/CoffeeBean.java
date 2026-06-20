package com.notedrop.notedrop.entity;

import com.notedrop.notedrop.common.enums.ProcessingMethod;
import com.notedrop.notedrop.common.enums.RoastLevel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "coffee_beans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoffeeBean extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 100)
    private String origin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingMethod processingMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoastLevel roastLevel;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal acidityScore;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal bitternessScore;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal sweetnessScore;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal bodyScore;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal aromaScore;

    @OneToMany(mappedBy = "coffeeBean", fetch = FetchType.LAZY)
    private List<Product> products = new ArrayList<>();

    public double[] toVector() {
        return new double[]{
            acidityScore.doubleValue(),
            bitternessScore.doubleValue(),
            sweetnessScore.doubleValue(),
            bodyScore.doubleValue(),
            aromaScore.doubleValue()
        };
    }
}