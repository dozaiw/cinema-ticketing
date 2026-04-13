// cinema-ticketing/src/main/java/com/cinema/ticketing/mapper/OrderMapper.java
package com.cinema.ticketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cinema.ticketing.dto.HallSalesStats;
import com.cinema.ticketing.entity.Order;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.Date;
import java.util.List;

public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT MAX(CAST(SUBSTRING(order_no, 14) AS UNSIGNED)) " +
            "FROM cinema_order " +
            "WHERE order_no LIKE CONCAT('ORD', #{dateStr}, '%')")
    Integer selectMaxSequence(@Param("dateStr") String dateStr);

    /**
     * 查询影院销售统计 (通过影厅ID 关联到影院)
     */
    List<HallSalesStats> selectCinemaSalesStats(
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate
    );
}