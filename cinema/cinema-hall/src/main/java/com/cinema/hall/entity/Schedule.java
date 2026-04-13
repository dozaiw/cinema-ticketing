package com.cinema.hall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.io.Serializable;

/**
 * 排片表(Schedule)实体类
 *
 * @author makejava
 * @since 2026-01-30 21:39:13
 */
public class Schedule implements Serializable {
    private static final long serialVersionUID = 792652230911648897L;


    /** 0-未开始：当前时间 < (startTime - 5分钟) */
    public static final Integer STATUS_NOT_STARTED = 0;
    /** 1-进行中：(startTime - 5分钟) <= 当前时间 <= endTime */
    public static final Integer STATUS_ONGOING = 1;
    /** 2-已结束：当前时间 > endTime */
    public static final Integer STATUS_FINISHED = 2;

    @TableId(type = IdType.AUTO)
    private Integer id;
/**
     * 影片ID
     */
    private Integer movieId;
/**
     * 影厅ID
     */
    private Integer hallId;
/**
     * 开始时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;
/**
     * 结束时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;
/**
     * 票价
     */
    private Double price;

    private Integer status;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getMovieId() {
        return movieId;
    }

    public void setMovieId(Integer movieId) {
        this.movieId = movieId;
    }

    public Integer getHallId() {
        return hallId;
    }

    public void setHallId(Integer hallId) {
        this.hallId = hallId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}

