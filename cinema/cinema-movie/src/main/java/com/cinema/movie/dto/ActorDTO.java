package com.cinema.movie.dto;

import com.cinema.movie.entity.Actor;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

/**
 * 演员DTO（包含头像文件）
 *
 * @author makejava
 * @since 2026-02-15
 */
@Data
public class ActorDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 演员实体
     */
    private Actor actor;
    
    /**
     * 头像文件
     */
    private MultipartFile avatarFile;
    
    /**
     * 旧头像URL（用于修改时删除）
     */
    private String oldAvatarUrl;
}