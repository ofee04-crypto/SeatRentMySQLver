package com.example.backend.service.spot;

import java.util.List;
import java.util.Map;

import com.example.backend.model.spot.RentalSpot;

public interface IRentalSpotService {

    public RentalSpot insert(RentalSpot insertBean);

    public RentalSpot update(RentalSpot updateBean);

    public RentalSpot selectById(Integer spotId);

    public boolean deleteById(Integer spotId);

    public List<RentalSpot> selectAll();

    public List<RentalSpot> findByCondition(String spotCode, String spotName, String spotStatus, Integer merchantId);

    public List<RentalSpot> selectByKeyword(String keyword);

    public List<Map<String, Object>> findAllMerchantsForSelect();

    public List<com.example.backend.repository.projection.AnalyzeProjections.SpotWithSeats> selectAllWithSeatCount();

}
