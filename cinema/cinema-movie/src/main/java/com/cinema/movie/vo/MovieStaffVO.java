// src/main/java/com/cinema/movie/vo/MovieStaffVO.java
package com.cinema.movie.vo;

import lombok.Data;

@Data
public class MovieStaffVO {
    private Integer id;
    private Integer movieId;
    private Integer actorId;
    private String actorName;      // 从 Actor 表关联查询
    private String avatarUrl;      // 从 Actor 表关联查询
    private String role;
    private String characterName;
    private Integer orderIndex;
}