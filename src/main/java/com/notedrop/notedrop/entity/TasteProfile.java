package com.notedrop.notedrop.entity;

import com.notedrop.notedrop.common.enums.UserLevel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "taste_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TasteProfile extends BaseEntity {

    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal acidityScore = BigDecimal.ZERO;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal bitternessScore = BigDecimal.ZERO;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal sweetnessScore = BigDecimal.ZERO;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal bodyScore = BigDecimal.ZERO;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal aromaScore = BigDecimal.ZERO;

    @Column(columnDefinition = "json")
    private String preferredOrigins;

    @Column(nullable = false)
    private int reviewCount = 0;

    @Enumerated(EnumType.STRING)
    @Column
    private UserLevel userLevel;

    @Column(nullable = false)
    private boolean onboardingComplete = false;

    // scores: [acidity, bitterness, sweetness, body, aroma]
    public static TasteProfile createFromOnboarding(User user, UserLevel level,
            double[] scores, String preferredOrigins) {
        TasteProfile p = new TasteProfile();
        p.user = user;
        p.userLevel = level;
        p.acidityScore    = bd(scores[0]);
        p.bitternessScore = bd(scores[1]);
        p.sweetnessScore  = bd(scores[2]);
        p.bodyScore       = bd(scores[3]);
        p.aromaScore      = bd(scores[4]);
        p.preferredOrigins = preferredOrigins;
        p.onboardingComplete = true;
        return p;
    }

    public void updateFromOnboarding(UserLevel level, double[] scores, String preferredOrigins) {
        this.userLevel = level;
        this.acidityScore    = bd(scores[0]);
        this.bitternessScore = bd(scores[1]);
        this.sweetnessScore  = bd(scores[2]);
        this.bodyScore       = bd(scores[3]);
        this.aromaScore      = bd(scores[4]);
        this.preferredOrigins = preferredOrigins;
        this.onboardingComplete = true;
    }

    private static BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }
}