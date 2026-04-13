package com.cinema.ticketing.util;// cinema-ticketing/src/main/java/com/cinema/ticketing/service/QrCodeGenerator.java


import com.cinema.ticketing.entity.Order;
import org.springframework.stereotype.Component;

@Component
public class QrCodeGenerator {

    public String generateQrCodeContent(Order order) {
        return String.format(
                "%s|%s|%s|%s|%s",
                order.getOrderNo(),
                maskPhone(order.getUserPhone()),
                order.getSeatNames().replaceAll("[\\[\\]\"]", ""),
                order.getShowTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                order.getVerifyCode()
        );
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}