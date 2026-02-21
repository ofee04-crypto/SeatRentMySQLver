package com.example.backend.repository.rec;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.model.rec.RecRent;

@Repository
public interface RecRentRepository extends JpaRepository<RecRent, Integer> {
    // 新增紀錄
    RecRent save(RecRent recRent); // 新增單筆

    // 依據業務主鍵刪除
    void deleteByRecId(String recId); // 刪除單筆

    // 支援用業務主鍵 (Rxxxxxxxxx) 查詢
    RecRent findByRecId(String recId); // 查詢單筆
}