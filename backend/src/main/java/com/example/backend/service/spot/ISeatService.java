package com.example.backend.service.spot;

import java.util.List;

import com.example.backend.model.spot.Seat;

public interface ISeatService {

    public Seat insert(Seat insertBean);

    public Seat update(Seat updateBean);

    public Seat selectById(Integer seatsId);

    public List<Seat> selectBySpotId(Integer spotId);

    public boolean deleteById(Integer seatsId);

    public List<Seat> selectAll();

    public List<Seat> findByCondition(String seatsName, String seatsType, String seatsStatus, Integer spotId,
            String serialNumber);

    public List<Seat> selectByKeyword(String keyword);

    public long countBySpotId(Integer spotId);

}