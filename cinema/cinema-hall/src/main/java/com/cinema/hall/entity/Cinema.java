package com.cinema.hall.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * 影院表(Cinema)实体类
 *
 * @author makejava
 * @since 2026-02-18 16:00:50
 */
public class Cinema implements Serializable {
    private static final long serialVersionUID = 512105452752996634L;

    private Integer id;
/**
     * 影院名称
     */
    private String name;
/**
     * 地址
     */
    private String address;
/**
     * 电话
     */
    private String phone;
/**
     * 纬度
     */
    private Double latitude;
/**
     * 经度
     */
    private Double longitude;
/**
     * 城市
     */
    private String city;
/**
     * 区域
     */
    private String district;
/**
     * 状态：0-停业 1-营业
     */
    private Integer status;
/**
     * 创建时间
     */
    private Date createTime;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

}

