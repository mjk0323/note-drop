package com.notedrop.notedrop.service;

import com.notedrop.notedrop.common.util.VectorUtils;
import com.notedrop.notedrop.dto.response.BeanRecommendationItem;
import com.notedrop.notedrop.dto.response.CafeRecommendationItem;
import com.notedrop.notedrop.dto.response.RecommendationResponse;
import com.notedrop.notedrop.entity.Cafe;
import com.notedrop.notedrop.entity.CoffeeBean;
import com.notedrop.notedrop.entity.TasteProfile;
import com.notedrop.notedrop.exception.BusinessException;
import com.notedrop.notedrop.exception.ErrorCode;
import com.notedrop.notedrop.repository.CafeRepository;
import com.notedrop.notedrop.repository.CoffeeBeanRepository;
import com.notedrop.notedrop.repository.TasteProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final BigDecimal FILTER_WINDOW = new BigDecimal("1.5");
    private static final int MAX_BEAN_RESULTS = 10;
    private static final int MAX_CAFE_RESULTS = 5;

    private final TasteProfileRepository tasteProfileRepository;
    private final CoffeeBeanRepository coffeeBeanRepository;
    private final CafeRepository cafeRepository;

    @Transactional(readOnly = true)
    public RecommendationResponse getRecommendations(Long userId) {
        TasteProfile profile = tasteProfileRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.TASTE_PROFILE_NOT_FOUND));

        double[] userVector = {
            profile.getAcidityScore().doubleValue(),
            profile.getBitternessScore().doubleValue(),
            profile.getSweetnessScore().doubleValue(),
            profile.getBodyScore().doubleValue(),
            profile.getAromaScore().doubleValue()
        };

        BigDecimal acidityLow    = profile.getAcidityScore().subtract(FILTER_WINDOW).max(BigDecimal.ZERO);
        BigDecimal acidityHigh   = profile.getAcidityScore().add(FILTER_WINDOW);
        BigDecimal sweetnessLow  = profile.getSweetnessScore().subtract(FILTER_WINDOW).max(BigDecimal.ZERO);
        BigDecimal sweetnessHigh = profile.getSweetnessScore().add(FILTER_WINDOW);

        List<CoffeeBean> candidates = coffeeBeanRepository.findByTasteScoreRange(
            acidityLow, acidityHigh, sweetnessLow, sweetnessHigh);

        Map<Long, Double> beanScoreMap = candidates.stream()
            .collect(Collectors.toMap(
                CoffeeBean::getId,
                bean -> VectorUtils.cosineSimilarity(userVector, bean.toVector())
            ));

        List<BeanRecommendationItem> beanItems = candidates.stream()
            .map(bean -> BeanRecommendationItem.builder()
                .id(bean.getId())
                .name(bean.getName())
                .origin(bean.getOrigin())
                .processingMethod(bean.getProcessingMethod().name())
                .roastLevel(bean.getRoastLevel().name())
                .similarityScore(beanScoreMap.get(bean.getId()))
                .build())
            .sorted(Comparator.comparingDouble(BeanRecommendationItem::getSimilarityScore).reversed())
            .limit(MAX_BEAN_RESULTS)
            .collect(Collectors.toList());

        List<Cafe> allCafes = cafeRepository.findAllWithCoffeeBeans();

        List<CafeRecommendationItem> cafeItems = allCafes.stream()
            .map(cafe -> {
                double maxScore = cafe.getCoffeeBeans().stream()
                    .mapToDouble(bean -> beanScoreMap.getOrDefault(bean.getId(), 0.0))
                    .max()
                    .orElse(0.0);
                return Map.entry(cafe, maxScore);
            })
            .filter(e -> e.getValue() > 0.0)
            .sorted(Map.Entry.<Cafe, Double>comparingByValue().reversed())
            .limit(MAX_CAFE_RESULTS)
            .map(e -> CafeRecommendationItem.builder()
                .id(e.getKey().getId())
                .name(e.getKey().getName())
                .address(e.getKey().getAddress())
                .operatingHours(e.getKey().getOperatingHours())
                .mapUrl(e.getKey().getMapUrl())
                .maxSimilarityScore(e.getValue())
                .build())
            .collect(Collectors.toList());

        return RecommendationResponse.builder()
            .beans(beanItems)
            .cafes(cafeItems)
            .build();
    }
}
