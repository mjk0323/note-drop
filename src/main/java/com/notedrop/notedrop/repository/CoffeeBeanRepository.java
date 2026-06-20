package com.notedrop.notedrop.repository;

import com.notedrop.notedrop.entity.CoffeeBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CoffeeBeanRepository extends JpaRepository<CoffeeBean, Long> {

    @Query("SELECT cb FROM CoffeeBean cb WHERE " +
           "cb.acidityScore BETWEEN :acidityLow AND :acidityHigh AND " +
           "cb.sweetnessScore BETWEEN :sweetnessLow AND :sweetnessHigh")
    List<CoffeeBean> findByTasteScoreRange(
        @Param("acidityLow")    BigDecimal acidityLow,
        @Param("acidityHigh")   BigDecimal acidityHigh,
        @Param("sweetnessLow")  BigDecimal sweetnessLow,
        @Param("sweetnessHigh") BigDecimal sweetnessHigh
    );
}
