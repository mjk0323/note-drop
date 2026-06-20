package com.notedrop.notedrop.repository;

import com.notedrop.notedrop.entity.Cafe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CafeRepository extends JpaRepository<Cafe, Long> {

    @Query("SELECT DISTINCT c FROM Cafe c JOIN FETCH c.coffeeBeans")
    List<Cafe> findAllWithCoffeeBeans();
}
