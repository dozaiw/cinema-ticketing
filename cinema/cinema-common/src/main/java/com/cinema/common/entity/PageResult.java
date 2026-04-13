package com.cinema.common.entity;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records;      // 当前页数据
    private Long total;           // 总记录数
    private Integer pageNum;      // 当前页码
    private Integer pageSize;     // 每页大小
    private Long pages;           // 总页数

    public PageResult() {}

    public PageResult(List<T> records, Long total, Integer pageNum, Integer pageSize) {
        this.records = records;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = (total + pageSize - 1) / pageSize;
    }
}