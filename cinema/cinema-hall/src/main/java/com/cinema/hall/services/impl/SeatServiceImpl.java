package com.cinema.hall.services.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cinema.hall.entity.Hall;
import com.cinema.hall.entity.Seat;
import com.cinema.hall.mapper.HallMapper;
import com.cinema.hall.mapper.SeatMapper;
import com.cinema.hall.services.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 座位表(Seat)表服务实现类
 *
 * @author makejava
 * @since 2026-01-31 21:24:42
 */
@Service("seatService")
public class SeatServiceImpl extends ServiceImpl<SeatMapper, Seat> implements SeatService {

    @Autowired
    private HallMapper hallMapper;
    @Autowired
    private SeatMapper seatMapper;

    @Override
    @Transactional
    /**
     * 初始化/更新影厅座位（支持影厅排数/列数修改后的增删补漏，保留有效座位原有状态）
     * @param hallId 影厅ID
     */
    public void initSeat(Integer hallId) {
        // 1. 查询影厅信息，校验影厅是否存在
        Hall hall = hallMapper.selectById(hallId);
        if (hall == null) {
            throw new IllegalArgumentException("影厅不存在");
        }
        int newRows = hall.getSeatRows(); // 影厅最新排数
        int newCols = hall.getSeatCols(); // 影厅最新列数

        // 2. 查询该影厅已存在的所有座位
        List<Seat> existingSeats = seatMapper.selectByHallId(hallId); // 需在mapper中添加该查询方法
        // 构建「已有座位的唯一标识集合」（hallId_rowNum_colNum，用于快速对比）
        Set<String> existingSeatKeys = new HashSet<>();
        for (Seat seat : existingSeats) {
            String seatKey = buildSeatKey(hallId, seat.getRowNum(), seat.getColNum());
            existingSeatKeys.add(seatKey);
        }

        // 3. 生成「影厅最新配置下的理想座位列表」
        List<Seat> idealSeats = new ArrayList<>(newRows * newCols);
        Set<String> idealSeatKeys = new HashSet<>();
        for (int i = 1; i <= newRows; i++) {      // 行号从1开始（更符合实际）
            for (int j = 1; j <= newCols; j++) {  // 列号从1开始
                Seat seat = new Seat();
                seat.setHallId(hallId);
                seat.setRowNum(i);
                seat.setColNum(j);
                seat.setStatus(0); // 新座位默认状态为0
                idealSeats.add(seat);

                // 构建理想座位的唯一标识
                String seatKey = buildSeatKey(hallId, i, j);
                idealSeatKeys.add(seatKey);
            }
        }

        // 4. 筛选需要「删除」的座位：已有座位中，不在理想座位列表中的（多余座位）
        List<Seat> seatsToDelete = new ArrayList<>();
        for (Seat existingSeat : existingSeats) {
            String seatKey = buildSeatKey(hallId, existingSeat.getRowNum(), existingSeat.getColNum());
            if (!idealSeatKeys.contains(seatKey)) {
                seatsToDelete.add(existingSeat);
            }
        }
        // 批量删除多余座位（若有）
        if (!seatsToDelete.isEmpty()) {
            // 注意：MyBatis-Plus的批量删除可使用removeByIds（需Seat有主键id）
            List<Integer> deleteIds = seatsToDelete.stream().map(Seat::getId).collect(Collectors.toList());
            super.removeByIds(deleteIds);
        }

        // 5. 筛选需要「新增」的座位：理想座位中，不在已有座位列表中的（缺失座位）
        List<Seat> seatsToAdd = new ArrayList<>();
        for (Seat idealSeat : idealSeats) {
            String seatKey = buildSeatKey(hallId, idealSeat.getRowNum(), idealSeat.getColNum());
            if (!existingSeatKeys.contains(seatKey)) {
                seatsToAdd.add(idealSeat);
            }
        }
        // 批量新增缺失座位（若有）
        if (!seatsToAdd.isEmpty()) {
            super.saveBatch(seatsToAdd);
        }
    }

    @Override
    public List<String> getSeatName(List<Integer> seatIds) {
        List<String> seatNames = new ArrayList<>();
        for (Integer seatId : seatIds) {
            Seat seat = seatMapper.selectById(seatId);
            seat.getColNum();
            seat.getRowNum();
            seatNames.add(seat.getRowNum() + "排" + seat.getColNum() + "座");
        }
        return seatNames;
    }

    private String buildSeatKey(Integer hallId, Integer rowNum, Integer colNum) {
        return hallId + "_" + rowNum + "_" + colNum;
    }
}
