package com.cinema.ticketing.util;// cinema-ticketing/src/main/java/com/cinema/ticketing/service/OrderNoGenerator.java


import com.cinema.ticketing.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class OrderNoGenerator {

    @Autowired
    private OrderMapper orderMapper;

    public String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Integer maxSeq = orderMapper.selectMaxSequence(dateStr);
        int seq = (maxSeq == null) ? 1 : maxSeq + 1;
        return "ORD" + dateStr + String.format("%06d", seq);
    }
}