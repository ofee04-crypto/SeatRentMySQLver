package com.example.backend.service.spot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;

import com.example.backend.model.spot.RentalSpot;
import com.example.backend.file.FileStorageService;
import com.example.backend.repository.spot.RentalSpotRepository;
// [注意] 請確認以下 Merchant 相關的 package 路徑是否正確
import com.example.backend.repository.merchantAndCoupon.MerchantRepository;
import com.example.backend.model.merchantAndCoupon.MerchantBean;

@Service
@Transactional
public class RentalSpotService implements IRentalSpotService {

    private final RentalSpotRepository rentalSpotRepository;
    private final FileStorageService fileStorageService;
    private final MerchantRepository merchantRepository;

    public RentalSpotService(RentalSpotRepository rentalSpotRepository, FileStorageService fileStorageService,
            MerchantRepository merchantRepository) {
        this.rentalSpotRepository = rentalSpotRepository;
        this.fileStorageService = fileStorageService;
        this.merchantRepository = merchantRepository;
    }

    /**
     * 新增據點，並在儲存前檢查代碼 (spotCode) 是否重複。
     * 
     * @param rentalSpot 要新增的據點物件
     * @return 儲存後的據點物件
     * @throws IllegalArgumentException 如果代碼已存在
     */
    @Override
    public RentalSpot insert(RentalSpot rentalSpot) {
        // 1. 檢查 spotCode 是否為空或空字串
        if (StringUtils.hasText(rentalSpot.getSpotCode())) {

            // 2. 呼叫 Repository 的 existsBySpotCode 方法進行檢查
            if (rentalSpotRepository.existsBySpotCode(rentalSpot.getSpotCode())) {

                // 3. 如果代碼已存在，拋出業務邏輯異常
                throw new IllegalArgumentException("據點代碼 (Spot Code) '" + rentalSpot.getSpotCode() + "' 已存在，請使用不同的代碼。");
            }
        }

        // 4. 執行儲存
        return rentalSpotRepository.save(rentalSpot);
    }

    @Override
    public RentalSpot update(RentalSpot rentalSpot) {
        // [修正] 確保 Update 操作有 ID，避免變成 Insert
        if (rentalSpot.getSpotId() == null) {
            throw new IllegalArgumentException("更新失敗：據點 ID (spotId) 不能為空");
        }

        // [優化] 檔案清理機制：如果更新了圖片，刪除舊的實體檔案
        rentalSpotRepository.findById(rentalSpot.getSpotId()).ifPresent(oldSpot -> {
            String oldImage = oldSpot.getSpotImage();
            String newImage = rentalSpot.getSpotImage();

            // 當資料庫原本有圖，且新傳入的圖與舊圖不同（代表更換了或是原本有現在要刪除）
            if (StringUtils.hasText(oldImage) && !oldImage.equals(newImage)) {
                fileStorageService.delete(oldImage);
            }
        });

        // 更新時通常不檢查代碼重複 (除非允許修改代碼且改到跟別人一樣)，直接儲存
        return rentalSpotRepository.save(rentalSpot);
    }

    @Override
    public boolean deleteById(Integer spotId) {
        if (spotId == null) {
            return false;
        }
        RentalSpot spot = rentalSpotRepository.findById(spotId).orElse(null);
        if (spot != null) {
            rentalSpotRepository.delete(spot);
            // 若該據點有圖片，則一併刪除實體檔案
            if (StringUtils.hasText(spot.getSpotImage())) {
                fileStorageService.delete(spot.getSpotImage());
            }
            return true;
        }
        return false;
    }

    @Override
    public RentalSpot selectById(Integer spotId) {
        if (spotId == null) {
            return null;
        }
        return rentalSpotRepository.findById(spotId).orElse(null);
    }

    @Override
    public List<RentalSpot> selectAll() {
        return rentalSpotRepository.findAll();
    }

    @Override
    public List<RentalSpot> selectByKeyword(String keyword) {
        return rentalSpotRepository.findByKeyword(keyword);
    }

    @Override
    public List<RentalSpot> findByCondition(String spotCode, String spotName, String spotStatus, Integer merchantId) {
        return rentalSpotRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(spotCode)) {
                predicates.add(cb.like(root.get("spotCode"), "%" + spotCode + "%"));
            }
            if (StringUtils.hasText(spotName)) {
                predicates.add(cb.like(root.get("spotName"), "%" + spotName + "%"));
            }
            if (StringUtils.hasText(spotStatus)) {
                predicates.add(cb.equal(root.get("spotStatus"), spotStatus));
            }
            if (merchantId != null) {
                predicates.add(cb.equal(root.get("merchantId"), merchantId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }

    @Override
    public List<Map<String, Object>> findAllMerchantsForSelect() {
        // 取得所有商家
        List<MerchantBean> merchants = merchantRepository.findAll();

        // 轉換為簡單的 Map 列表 (只包含 ID 和 名稱)
        return merchants.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("merchantId", m.getMerchantId());
            map.put("merchantName", m.getMerchantName());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<com.example.backend.repository.projection.AnalyzeProjections.SpotWithSeats> selectAllWithSeatCount() {
        return rentalSpotRepository.findAllSpotsWithSeatCount();
    }
}