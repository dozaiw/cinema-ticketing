package com.cinema.movie.services.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cinema.auth.util.CosUtil;
import com.cinema.common.entity.BaseResponse;
import com.cinema.movie.dto.ActorDTO;
import com.cinema.movie.entity.Actor;
import com.cinema.movie.mapper.ActorMapper;
import com.cinema.movie.mapper.MovieStaffMapper;
import com.cinema.movie.services.ActorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 演员信息表(Actor)服务实现类
 *
 * @author makejava
 * @since 2026-02-15 10:17:31
 */
@Slf4j
@Service
public class ActorServiceImpl implements ActorService {

    @Autowired
    private ActorMapper actorMapper;

    @Autowired
    private MovieStaffMapper movieStaffMapper;

    @Autowired
    private CosUtil cosUtil;

    @Override
    public BaseResponse add(ActorDTO actorDTO) {
        try {
            Actor actor = actorDTO.getActor();
            MultipartFile avatarFile = actorDTO.getAvatarFile();

            // 参数校验
            if (actor == null || actor.getName() == null || actor.getName().trim().isEmpty()) {
                return BaseResponse.error(403, "演员姓名不能为空");
            }

            // 检查是否已存在同名演员
            LambdaQueryWrapper<Actor> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Actor::getName, actor.getName());
            Long count = actorMapper.selectCount(wrapper);
            if (count > 0) {
                return BaseResponse.error(403, "该演员已存在");
            }

            // ========== 上传头像 ==========
            if (avatarFile != null && !avatarFile.isEmpty()) {
                try {
                    String avatarUrl = cosUtil.uploadFile(avatarFile, "actor/avatar");
                    actor.setAvatarUrl(avatarUrl);
                    log.info(" 演员头像上传成功: {}", avatarUrl);
                } catch (Exception e) {
                    log.error("演员头像上传失败", e);
                    return BaseResponse.error(403, "头像上传失败");
                }
            }

            // 保存演员
            int result = actorMapper.insert(actor);
            if (result > 0) {
                log.info("新增演员成功: {}", actor.getName());
                return BaseResponse.success("添加成功", actor);
            } else {
                // 回滚：删除已上传的头像
                if (actor.getAvatarUrl() != null) {
                    try {
                        cosUtil.deleteFile(actor.getAvatarUrl());
                    } catch (Exception e) {
                        log.error("回滚删除头像失败", e);
                    }
                }
                return BaseResponse.error(403, "添加失败");
            }
        } catch (Exception e) {
            log.error("添加演员异常", e);
            return BaseResponse.error(403, "系统异常");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse delete(Integer id) {
        try {
            // 参数校验
            if (id == null || id <= 0) {
                return BaseResponse.error(403, "演员ID不能为空");
            }

            // 查询演员是否存在
            Actor actor = actorMapper.selectById(id);
            if (actor == null) {
                return BaseResponse.error(403, "演员不存在");
            }

            // 先删除 movie_staff 表中的关联记录
            int deletedRelations = movieStaffMapper.deleteByActorId(id);
            log.info("删除演员关联记录 {} 条", deletedRelations);

            // ========== 删除COS中的头像 ==========
            if (actor.getAvatarUrl() != null && !actor.getAvatarUrl().isEmpty()) {
                try {
                    cosUtil.deleteFile(actor.getAvatarUrl());
                    log.info("删除演员头像成功: {}", actor.getAvatarUrl());
                } catch (Exception e) {
                    log.error("删除演员头像失败", e);
                    // 不影响删除操作继续执行
                }
            }

            // 再删除演员
            int result = actorMapper.deleteById(id);
            if (result > 0) {
                log.info("删除演员成功: {}", actor.getName());
                Map<String, Object> data = new HashMap<>();
                data.put("actorId", id);
                data.put("deletedRelations", deletedRelations);
                return BaseResponse.success("删除成功", data);
            } else {
                return BaseResponse.error(403, "删除失败");
            }
        } catch (Exception e) {
            log.error("删除演员异常", e);
            return BaseResponse.error(403, "系统异常");
        }
    }

    @Override
    public BaseResponse update(ActorDTO actorDTO) {
        try {
            Actor actor = actorDTO.getActor();
            MultipartFile avatarFile = actorDTO.getAvatarFile();
            String oldAvatarUrl = actorDTO.getOldAvatarUrl();

            // 参数校验
            if (actor == null || actor.getId() == null) {
                return BaseResponse.error(403, "演员ID不能为空");
            }

            // 检查演员是否存在
            Actor existActor = actorMapper.selectById(actor.getId());
            if (existActor == null) {
                return BaseResponse.error(403, "演员不存在");
            }

            // 如果修改了姓名，检查是否与其他演员重复
            if (actor.getName() != null && !actor.getName().equals(existActor.getName())) {
                LambdaQueryWrapper<Actor> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Actor::getName, actor.getName());
                wrapper.ne(Actor::getId, actor.getId());
                Long count = actorMapper.selectCount(wrapper);
                if (count > 0) {
                    return BaseResponse.error(403, "该演员姓名已存在");
                }
            }

            // ========== 更新头像 ==========
            if (avatarFile != null && !avatarFile.isEmpty()) {
                try {
                    // 上传新头像
                    String newAvatarUrl = cosUtil.uploadFile(avatarFile, "actor/avatar");
                    actor.setAvatarUrl(newAvatarUrl);
                    log.info(" 演员头像更新成功: {}", newAvatarUrl);

                    // 删除旧头像
                    if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
                        try {
                            cosUtil.deleteFile(oldAvatarUrl);
                            log.info(" 删除旧头像成功: {}", oldAvatarUrl);
                        } catch (Exception e) {
                            log.error("删除旧头像失败", e);
                            // 不影响更新操作继续执行
                        }
                    }
                } catch (Exception e) {
                    log.error("演员头像更新失败", e);
                    return BaseResponse.error(403, "头像更新失败");
                }
            }

            // 更新演员
            int result = actorMapper.updateById(actor);
            if (result > 0) {
                log.info("更新演员成功: {}", actor.getId());
                return BaseResponse.success("更新成功", actor);
            } else {
                return BaseResponse.error(403, "更新失败");
            }
        } catch (Exception e) {
            log.error("更新演员异常", e);
            return BaseResponse.error(403, "系统异常");
        }
    }

    @Override
    public BaseResponse getById(Integer id) {
        try {
            if (id == null || id <= 0) {
                return BaseResponse.error(403, "演员ID不能为空");
            }

            Actor actor = actorMapper.selectById(id);
            if (actor == null) {
                return BaseResponse.error(403, "演员不存在");
            }

            return BaseResponse.success(actor);
        } catch (Exception e) {
            log.error("查询演员异常", e);
            return BaseResponse.error(403, "系统异常");
        }
    }

    @Override
    public BaseResponse get(String name) {
        try {
            if (name == null || name.trim().isEmpty()) {
                return BaseResponse.error(403, "演员姓名不能为空");
            }

            LambdaQueryWrapper<Actor> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(Actor::getName, name);
            List<Actor> actors = actorMapper.selectList(wrapper);

            if (actors.isEmpty()) {
                return BaseResponse.success("未找到相关演员", actors);
            }

            return BaseResponse.success(actors);
        } catch (Exception e) {
            log.error("查询演员异常", e);
            return BaseResponse.error(403, "系统异常");
        }
    }

    @Override
    public BaseResponse listAll() {
        try {
            List<Actor> actors = actorMapper.selectList(null);
            return BaseResponse.success(actors);
        } catch (Exception e) {
            log.error("查询所有演员异常", e);
            return BaseResponse.error(403, "系统异常");
        }
    }

    @Override
    public BaseResponse listPage(Integer pageNum, Integer pageSize) {
        try {
            pageNum = pageNum == null ? 1 : pageNum;
            pageSize = pageSize == null ? 10 : pageSize;

            Page<Actor> page = new Page<>(pageNum, pageSize);
            Page<Actor> result = actorMapper.selectPage(page, null);

            Map<String, Object> data = new HashMap<>();
            data.put("list", result.getRecords());
            data.put("total", result.getTotal());
            data.put("pageNum", pageNum);
            data.put("pageSize", pageSize);

            return BaseResponse.success(data);
        } catch (Exception e) {
            log.error("分页查询演员异常", e);
            return BaseResponse.error(403, "系统异常");
        }
    }

    @Override
  public BaseResponse listPageFiltered(Integer pageNum, Integer pageSize, String name) {
        try {
          if (pageNum == null || pageNum < 1) pageNum = 1;
          if (pageSize == null || pageSize < 1) pageSize = 10;
            pageSize = Math.min(pageSize, 50);

            log.info("筛选演员列表：pageNum={}, pageSize={}, name={}", pageNum, pageSize, name);

            // 构建查询条件
            LambdaQueryWrapper<Actor> queryWrapper = new LambdaQueryWrapper<>();
            
            // 姓名模糊查询
          if (name != null && !name.trim().isEmpty()) {
                queryWrapper.like(Actor::getName, name.trim());
            }

            // 分页查询
            Page<Actor> page = new Page<>(pageNum, pageSize);
            Page<Actor> result = actorMapper.selectPage(page, queryWrapper);

            Map<String, Object> data = new HashMap<>();
            data.put("list", result.getRecords());
            data.put("total", result.getTotal());
            data.put("pageNum", pageNum);
            data.put("pageSize", pageSize);

            log.info("筛选演员列表成功：总记录数={}, 当前页={}, 每页={}", 
                    result.getTotal(), pageNum, pageSize);

            return BaseResponse.success(data);
        } catch (Exception e) {
            log.error("筛选演员列表异常", e);
            return BaseResponse.error(403, "系统异常：" + e.getMessage());
        }
    }

}