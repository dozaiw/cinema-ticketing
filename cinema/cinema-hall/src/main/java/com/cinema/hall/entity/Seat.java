package com.cinema.hall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serializable;

/**
 * 座位表(Seat)实体类
 *
 * @author makejava
 * @since 2026-01-31 21:23:14
 */
public class Seat implements Serializable {
    private static final long serialVersionUID = 420667364417215004L;

    @TableId(type = IdType.AUTO)
    private Integer id;
/**
     * 影厅ID
     */
    private Integer hallId;
/**
     * 行号
     */
    private Integer rowNum;
/**
     * 列号
     */
    private Integer colNum;
/**
     * 0:空闲 1:已售
     */
    private Integer status;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getHallId() {
        return hallId;
    }

    public void setHallId(Integer hallId) {
        this.hallId = hallId;
    }

    public Integer getRowNum() {
        return rowNum;
    }

    public void setRowNum(Integer rowNum) {
        this.rowNum = rowNum;
    }

    public Integer getColNum() {
        return colNum;
    }

    public void setColNum(Integer colNum) {
        this.colNum = colNum;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

}

