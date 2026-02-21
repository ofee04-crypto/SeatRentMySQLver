package com.example.backend.service.spot;

import java.util.ArrayList;
import java.util.List; // 確認這裡是 java.util.List

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.model.spot.Seat;
import com.example.backend.repository.spot.SeatRepository;

import jakarta.persistence.criteria.Predicate;

@Service
@Transactional
public class SeatService implements ISeatService {

    private final SeatRepository seatRepository;

    public SeatService(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    /**
     * 新增一個座位，並在儲存前檢查序號是否重複。
     * 
     * @param seat 要新增的座位物件
     * @return 儲存後的座位物件
     * @throws IllegalArgumentException 如果序號已存在
     */
    @Override
    public Seat insert(Seat seat) {
        // 1. [優化] Serial Number 改由資料庫 Computed Column 自動生成
        // Hibernate @Generated 標註會確保 insert/update 後自動撈回數值

        // 2. 執行儲存
        return seatRepository.save(seat);
    }

    @Override
    public Seat update(Seat seat) {
        // save(): 這裡是修改。
        // [修正] 確保 Update 操作有 ID，避免變成 Insert
        if (seat.getSeatsId() == null) {
            throw new IllegalArgumentException("更新失敗：座位 ID 不能為空");
        }
        return seatRepository.save(seat);
    }

    @Override
    public boolean deleteById(Integer seatsId) {
        if (seatsId == null) {
            return false;
        }
        // 先檢查是否存在，再刪除。
        if (seatRepository.existsById(seatsId)) {
            seatRepository.deleteById(seatsId);
            return true;
        }
        return false;
    }

    @Override
    public Seat selectById(Integer seatsId) {
        if (seatsId == null) {
            return null;
        }
        // findById(): 根據主鍵查詢。
        return seatRepository.findById(seatsId).orElse(null);
    }

    @Override
    public List<Seat> selectAll() {
        // findAll(): 查詢全部。
        return seatRepository.findAll();
    }

    @Override
    public List<Seat> selectBySpotId(Integer spotId) {
        if (spotId == null) {
            return new ArrayList<>();
        }
        return seatRepository.findBySpotId(spotId);
    }

    @Override
    public List<Seat> findByCondition(String seatsName, String seatsType, String seatsStatus, Integer spotId,
            String serialNumber) {
        // 使用 Specification 進行動態查詢，避免手寫 SQL/HQL。
        return seatRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>(); // 確認 Predicate 後面沒有 <...>

            if (seatsName != null && !seatsName.isBlank()) {
                predicates.add(cb.like(root.get("seatsName"), "%" + seatsName + "%"));
            }
            if (seatsType != null && !seatsType.isBlank()) {
                predicates.add(cb.equal(root.get("seatsType"), seatsType));
            }
            if (seatsStatus != null && !seatsStatus.isBlank()) {
                predicates.add(cb.equal(root.get("seatsStatus"), seatsStatus));
            }
            if (spotId != null) {
                predicates.add(cb.equal(root.get("spotId"), spotId));
            }
            if (serialNumber != null && !serialNumber.isBlank()) {
                predicates.add(cb.like(root.get("serialNumber"), "%" + serialNumber + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }

    @Override
    public List<Seat> selectByKeyword(String keyword) {
        return seatRepository.findByKeyword(keyword);
    }

    @Override
    public long countBySpotId(Integer spotId) {
        return seatRepository.countBySpotId(spotId);
    }
}