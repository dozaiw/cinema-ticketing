package com.cinema.movie.controller;

import com.cinema.common.entity.BaseResponse;
import com.cinema.movie.dto.ActorDTO;
import com.cinema.movie.entity.Actor;
import com.cinema.movie.services.ActorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 演员信息表(Actor)表控制层
 *
 * @author makejava
 * @since 2026-02-15 10:17:31
 */
@Slf4j
@RestController
@RequestMapping("/actor")
public class ActorController {

    @Autowired
    private ActorService actorService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 添加演员（支持头像上传）
     */
    @PostMapping("/add")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse addActor(
            @RequestPart("actor") String actorJson,  // 改为 String 接收
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile) {

        try {
            // 手动解析 JSON
            Actor actor = objectMapper.readValue(actorJson, Actor.class);

            ActorDTO actorDTO = new ActorDTO();
            actorDTO.setActor(actor);
            actorDTO.setAvatarFile(avatarFile);
            return actorService.add(actorDTO);

        } catch (IOException e) {
            log.error("添加演员失败，JSON解析异常", e);
            //  错误码修正：400 参数错误 / 500 服务器内部错误
            return BaseResponse.error(400, "请求参数解析失败: " + e.getMessage());
        }
    }

    /**
     * 删除演员（级联删除关联和头像）
     */
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse deleteActor(@PathVariable("id") Integer id) {

        return actorService.delete(id);
    }

    /**
     * 修改演员（支持头像更新）
     */
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('admin')")
    public BaseResponse updateActor(
            @RequestPart("actor") String actorJson,  //  改为 String 接收
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile,
            @RequestPart(value = "oldAvatarUrl", required = false) String oldAvatarUrl) {

        try {
            // 手动解析 JSON
            Actor actor = objectMapper.readValue(actorJson, Actor.class);

            ActorDTO actorDTO = new ActorDTO();
            actorDTO.setActor(actor);
            actorDTO.setAvatarFile(avatarFile);
            actorDTO.setOldAvatarUrl(oldAvatarUrl);
            return actorService.update(actorDTO);

        } catch (IOException e) {
            log.error("修改演员失败，JSON解析异常", e);
            return BaseResponse.error(400, "请求参数解析失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID查询演员
     */
    @GetMapping("/get/{id}")
    public BaseResponse getActorById(@PathVariable("id") Integer id) {

        return actorService.getById(id);
    }

    /**
     * 根据姓名查询演员（模糊查询）
     */
    @GetMapping("/search")
    public BaseResponse searchActor(@RequestParam("name") String name) {
        return actorService.get(name);
    }

    /**
     * 查询所有演员列表
     */
    @GetMapping("/list")
    public BaseResponse listAllActors() {
        return actorService.listAll();
    }

    /**
     * 分页查询演员
     */
    @GetMapping("/page")
  public BaseResponse listActorsPage(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return actorService.listPage(pageNum, pageSize);
    }

    /**
     * 根据姓名筛选演员（支持分页）
     */
    @GetMapping("/page/filtered")
   public BaseResponse listFilteredActorsPage(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "name", required = false) String name) {
        return actorService.listPageFiltered(pageNum, pageSize, name);
    }

}