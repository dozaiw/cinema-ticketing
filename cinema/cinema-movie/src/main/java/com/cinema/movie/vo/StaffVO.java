package com.cinema.movie.vo;

import lombok.Data;

@Data
public class StaffVO {
    private Integer id;
    private Integer movieId;
    private Integer actorId;
    private String actorName;
    private String avatarUrl;
    private String role;           // 主演、导演等
    private String characterName;  // 角色名
    private Integer orderIndex;    // 排序
}