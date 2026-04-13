package com.cinema.hall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serializable;

/**
 * 影厅表(Hall)实体类
 *
 * @author makejava
 * @since 2026-01-30 20:25:00
 */
public class Hall implements Serializable {
    private static final long serialVersionUID = -87341148057348470L;

    @TableId(type = IdType.AUTO)
    private Integer id;
/**
     * 所属影院
     */
    private Integer cinemaId;
/**
     * 影厅名称
     */
    private String name;
/**
     * 座位行数
     */
    private Integer seatRows;
/**
     * 座位列数
     */
    private Integer seatCols;

    private Integer status;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCinemaId() {
        return cinemaId;
    }

    public void setCinemaId(Integer cinemaId) {
        this.cinemaId = cinemaId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSeatRows() {
        return seatRows;
    }

    public void setSeatRows(Integer seatRows) {
        this.seatRows = seatRows;
    }

    public Integer getSeatCols() {
        return seatCols;
    }

    public void setSeatCols(Integer seatCols) {
        this.seatCols = seatCols;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}

