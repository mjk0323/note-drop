package com.notedrop.notedrop.service;

import com.notedrop.notedrop.common.enums.UserLevel;
import com.notedrop.notedrop.dto.request.OnboardingRequest;
import com.notedrop.notedrop.dto.response.OnboardingQuestionResponse;
import com.notedrop.notedrop.dto.response.OnboardingQuestionResponse.Option;
import com.notedrop.notedrop.dto.response.OnboardingQuestionResponse.Question;
import com.notedrop.notedrop.entity.TasteProfile;
import com.notedrop.notedrop.entity.User;
import com.notedrop.notedrop.exception.BusinessException;
import com.notedrop.notedrop.exception.ErrorCode;
import com.notedrop.notedrop.repository.TasteProfileRepository;
import com.notedrop.notedrop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    // [acidity, bitterness, sweetness, body, aroma]
    private static final Map<String, double[]> BRAND_PROFILES = Map.of(
        "스타벅스",    new double[]{1.5, 4.0, 2.0, 4.0, 3.0},
        "투썸플레이스", new double[]{2.5, 3.0, 2.5, 3.0, 3.0},
        "폴바셋",     new double[]{3.5, 2.0, 3.5, 2.5, 4.0},
        "블루보틀",    new double[]{4.0, 1.5, 3.0, 2.5, 4.5},
        "이디야",     new double[]{2.0, 3.5, 2.0, 3.5, 2.5},
        "메가커피",    new double[]{1.5, 4.0, 2.0, 3.5, 2.5},
        "할리스",     new double[]{2.5, 3.0, 2.5, 3.0, 3.0},
        "커피빈",     new double[]{2.0, 3.5, 2.5, 3.5, 3.0}
    );

    private static final Map<String, double[]> ENTHUSIAST_FLAVOR = Map.of(
        "A", new double[]{4.0, 1.5, 3.0, 2.0, 4.0},  // 과일향·베리류
        "B", new double[]{3.5, 1.5, 3.0, 2.0, 4.5},  // 꽃향기·플로랄
        "C", new double[]{2.0, 3.0, 3.5, 3.5, 3.0},  // 초콜릿·넛트
        "D", new double[]{2.5, 2.5, 4.0, 3.0, 3.0},  // 카라멜·브라운슈거
        "E", new double[]{1.0, 4.0, 1.5, 4.0, 3.0}   // 스파이시·스모키
    );

    private static final Map<String, String> ORIGIN_MAP = Map.of(
        "A", "에티오피아", "B", "콜롬비아", "C", "브라질", "D", "케냐", "E", "과테말라"
    );

    private final TasteProfileRepository tasteProfileRepository;
    private final UserRepository userRepository;

    public OnboardingQuestionResponse getQuestions() {
        return OnboardingQuestionResponse.builder()
            .beginner(beginnerQuestions())
            .enthusiast(enthusiastQuestions())
            .brands(List.copyOf(BRAND_PROFILES.keySet()))
            .build();
    }

    @Transactional
    public void completeOnboarding(Long userId, OnboardingRequest request) {
        Optional<TasteProfile> existing = tasteProfileRepository.findById(userId);
        if (existing.isPresent() && existing.get().isOnboardingComplete()) {
            throw new BusinessException(ErrorCode.ONBOARDING_ALREADY_COMPLETE);
        }

        double[] scores = request.getLevel() == UserLevel.BEGINNER
            ? computeBeginnerScores(request.getAnswers())
            : computeEnthusiastScores(request.getAnswers());

        scores = blendWithBrand(scores, request.getPreferredBrand());

        String preferredOrigins = request.getLevel() == UserLevel.ENTHUSIAST
            ? buildPreferredOrigins(request.getAnswers().getOrDefault("q6", List.of()))
            : null;

        if (existing.isPresent()) {
            existing.get().updateFromOnboarding(request.getLevel(), scores, preferredOrigins);
        } else {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            tasteProfileRepository.save(
                TasteProfile.createFromOnboarding(user, request.getLevel(), scores, preferredOrigins));
        }
    }

    private double[] computeBeginnerScores(Map<String, List<String>> answers) {
        double[] s = {2.5, 2.5, 2.5, 2.5, 2.5}; // [acidity, bitterness, sweetness, body, aroma]

        String q1 = first(answers, "q1");
        if ("A".equals(q1)) { s[2] = 4.0; s[1] = 1.5; }       // 단맛
        else if ("B".equals(q1)) { s[2] = 2.5; s[1] = 2.5; }  // 둘 다
        else if ("C".equals(q1)) { s[2] = 1.5; s[1] = 4.0; }  // 쓴맛

        String q2 = first(answers, "q2");
        if ("A".equals(q2)) s[0] = 4.5;       // 좋아함
        else if ("B".equals(q2)) s[0] = 2.5;
        else if ("C".equals(q2)) s[0] = 1.0;  // 싫어함

        String q3 = first(answers, "q3");
        if ("A".equals(q3)) s[3] = 4.5;       // 진하고 묵직
        else if ("B".equals(q3)) s[3] = 3.0;
        else if ("C".equals(q3)) s[3] = 1.5;  // 가볍고 깔끔

        String q4 = first(answers, "q4");
        if ("A".equals(q4)) s[4] = 4.5;       // 향 중요
        else if ("B".equals(q4)) s[4] = 3.0;
        else if ("C".equals(q4)) s[4] = 1.5;

        return s;
    }

    private double[] computeEnthusiastScores(Map<String, List<String>> answers) {
        double[] s = {2.5, 2.5, 2.5, 2.5, 2.5};

        // Q1: 플레이버 계열 (복수 선택) → 평균으로 sweetness, aroma 초기값 산출
        List<String> flavors = answers.getOrDefault("q1", List.of());
        if (!flavors.isEmpty()) {
            double[] sum = new double[5];
            for (String key : flavors) {
                double[] p = ENTHUSIAST_FLAVOR.getOrDefault(key, s);
                for (int i = 0; i < 5; i++) sum[i] += p[i];
            }
            for (int i = 0; i < 5; i++) s[i] = sum[i] / flavors.size();
        }

        // Q3~Q5: 직접 선택값으로 acidity, body, bitterness 덮어씀
        String q3 = first(answers, "q3");
        if (q3 != null) s[0] = Double.parseDouble(q3);

        String q4 = first(answers, "q4");
        if (q4 != null) s[3] = Double.parseDouble(q4);

        String q5 = first(answers, "q5");
        if (q5 != null) s[1] = Double.parseDouble(q5);

        return s;
    }

    private double[] blendWithBrand(double[] scores, String brand) {
        if (brand == null || !BRAND_PROFILES.containsKey(brand)) return scores;
        double[] bp = BRAND_PROFILES.get(brand);
        double[] result = new double[5];
        for (int i = 0; i < 5; i++) result[i] = scores[i] * 0.7 + bp[i] * 0.3;
        return result;
    }

    private String buildPreferredOrigins(List<String> keys) {
        if (keys.isEmpty()) return null;
        List<String> origins = keys.stream()
            .map(k -> ORIGIN_MAP.getOrDefault(k, k))
            .toList();
        return "[\"" + String.join("\",\"", origins) + "\"]";
    }

    private String first(Map<String, List<String>> answers, String id) {
        List<String> list = answers.get(id);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    private List<Question> beginnerQuestions() {
        return List.of(
            q("q1", "어떤 맛의 커피를 선호하세요?", false,
                o("A", "단맛이 느껴지는 커피"),
                o("B", "둘 다 괜찮아요"),
                o("C", "쓴맛이 강한 커피")),
            q("q2", "신맛(산미)을 즐기시나요?", false,
                o("A", "상큼한 신맛이 좋아요"),
                o("B", "약간의 신맛은 괜찮아요"),
                o("C", "신맛은 별로예요")),
            q("q3", "커피의 질감은 어떤 게 좋으세요?", false,
                o("A", "진하고 묵직한 커피"),
                o("B", "중간 정도면 좋아요"),
                o("C", "가볍고 깔끔한 커피")),
            q("q4", "커피 향이 얼마나 중요한가요?", false,
                o("A", "향이 진한 커피가 좋아요"),
                o("B", "보통이에요"),
                o("C", "향보다 맛이 중요해요"))
        );
    }

    private List<Question> enthusiastQuestions() {
        return List.of(
            q("q1", "선호하는 플레이버 계열을 모두 선택해주세요. (SCAA 플레이버 휠 기준)", true,
                o("A", "과일향·베리류 (Fruity)"),
                o("B", "꽃향기·플로랄 (Floral)"),
                o("C", "초콜릿·넛트 (Chocolatey/Nutty)"),
                o("D", "카라멜·브라운슈거 (Sweet/Caramel)"),
                o("E", "스파이시·스모키 (Roasted/Spicy)")),
            q("q2", "선호하는 로스팅 레벨은?", false,
                o("A", "라이트 (Light)"),
                o("B", "미디엄라이트 (Medium Light)"),
                o("C", "미디엄 (Medium)"),
                o("D", "미디엄다크 (Medium Dark)"),
                o("E", "다크 (Dark)")),
            q("q3", "산미(Acidity) 선호도는 어느 정도인가요?", false,
                o("1", "전혀 좋지 않아요"),
                o("2", "별로예요"),
                o("3", "보통이에요"),
                o("4", "좋아해요"),
                o("5", "매우 좋아해요")),
            q("q4", "바디감(Body) 선호도는 어느 정도인가요?", false,
                o("1", "매우 가벼운 게 좋아요"),
                o("2", "가벼운 편이 좋아요"),
                o("3", "중간이 좋아요"),
                o("4", "진한 편이 좋아요"),
                o("5", "매우 진한 게 좋아요")),
            q("q5", "쓴맛(Bitterness) 허용 수준은 어느 정도인가요?", false,
                o("1", "쓴맛은 전혀 싫어요"),
                o("2", "약간의 쓴맛은 괜찮아요"),
                o("3", "보통 수준은 괜찮아요"),
                o("4", "쓴맛이 있는 편이 좋아요"),
                o("5", "강한 쓴맛을 즐겨요")),
            q("q6", "선호하는 원산지를 모두 선택해주세요. (선택 사항)", true,
                o("A", "에티오피아 (Ethiopia) - 과일향·높은 산미"),
                o("B", "콜롬비아 (Colombia) - 밸런스·카라멜"),
                o("C", "브라질 (Brazil) - 저산미·초콜릿·묵직"),
                o("D", "케냐 (Kenya) - 강한 산미·베리류"),
                o("E", "과테말라 (Guatemala) - 초콜릿·중간 산미"))
        );
    }

    private Question q(String id, String text, boolean multiSelect, Option... options) {
        return Question.builder().id(id).text(text).multiSelect(multiSelect)
            .options(List.of(options)).build();
    }

    private Option o(String key, String label) {
        return Option.builder().key(key).label(label).build();
    }
}
