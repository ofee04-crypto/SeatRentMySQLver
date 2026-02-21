package com.example.backend.repository.rec;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.example.backend.model.rec.RentDetails;

import jakarta.persistence.criteria.Predicate;

public class RentDetailsSpecs {

    public static Specification<RentDetails> hasRecId(String recId) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(recId)) {
                return cb.conjunction(); // or cb.isTrue(cb.literal(true))
            }
            return cb.equal(root.get("recId"), recId);
        };
    }

    public static Specification<RentDetails> hasMemId(Integer memId) {
        return (root, query, cb) -> {
            if (memId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("memId"), memId);
        };
    }

    public static Specification<RentDetails> spotNameContains(String spotName) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(spotName)) {
                return cb.conjunction();
            }
            return cb.like(root.get("rentSpotName"), "%" + spotName + "%");
        };
    }

    public static Specification<RentDetails> memNameContains(String memName) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(memName)) {
                return cb.conjunction();
            }
            return cb.like(root.get("memName"), "%" + memName + "%");
        };
    }

    public static Specification<RentDetails> hasRecStatus(String recStatus) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(recStatus)) {
                return cb.conjunction();
            }
            return cb.equal(root.get("recStatus"), recStatus);
        };
    }

    public static Specification<RentDetails> hasSpotId(Integer spotId) {
        return (root, query, cb) -> {
            if (spotId == null) {
                return cb.conjunction();
            }
            // 搜尋租借站點ID或歸還站點ID
            Predicate rentSpotMatch = cb.equal(root.get("spotIdRent"), spotId);
            Predicate returnSpotMatch = cb.equal(root.get("spotIdReturn"), spotId);
            return cb.or(rentSpotMatch, returnSpotMatch);
        };
    }

    public static Specification<RentDetails> isWithinTimeRange(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            if (startDate == null || endDate == null) {
                return cb.conjunction();
            }
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

            Predicate rentTimeIn = cb.between(root.get("recRentDT2"), startDateTime, endDateTime);
            Predicate returnTimeIn = cb.between(root.get("recReturnDT2"), startDateTime, endDateTime);

            return cb.or(rentTimeIn, returnTimeIn);
        };
    }

}
