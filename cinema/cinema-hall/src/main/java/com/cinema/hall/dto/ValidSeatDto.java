package com.cinema.hall.dto;


import lombok.Data;

import java.util.List;

@Data
public class ValidSeatDto {
    Integer userId;
    List<Integer> seatIds;
    Integer scheduleId;
}
