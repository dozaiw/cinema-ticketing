package com.cinema.movie.services.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cinema.common.entity.BaseResponse;
import com.cinema.movie.entity.Movie;
import com.cinema.movie.entity.MovieStaff;
import com.cinema.movie.entity.Actor;
import com.cinema.movie.mapper.ActorMapper;
import com.cinema.movie.mapper.MovieMapper;
import com.cinema.movie.mapper.MovieStaffMapper;
import com.cinema.movie.services.MovieStaffService;
import com.cinema.movie.vo.MovieStaffVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 电影人员关联表(MovieStaff)服务实现类
 *
 * @author makejava
 * @since 2026-02-15 10:17:31
 */
@Slf4j
@Service
public class MovieStaffServiceImpl implements MovieStaffService {

    @Autowired
    private MovieStaffMapper movieStaffMapper;

    @Autowired
    private ActorMapper actorMapper;

    @Autowired
    private MovieMapper movieMapper;

    @Override
    public BaseResponse add(MovieStaff movieStaff) {
        try {
            // 参数校验
            if (movieStaff == null) {
                return BaseResponse.error(403, "参数不能为空");
            }

            // 检查电影是否存在
            if (movieStaff.getMovieId() == null) {
                return BaseResponse.error(403, "电影ID不能为空");
            }
            Movie movie = movieMapper.selectById(movieStaff.getMovieId());
            if (movie == null) {
                return BaseResponse.error(403, "电影不存在");
            }

            // 检查演员是否存在
            if (movieStaff.getActorId() == null) {
                return BaseResponse.error(403, "演员ID不能为空");
            }
            Actor actor = actorMapper.selectById(movieStaff.getActorId());
            if (actor == null) {
                return BaseResponse.error(403, "演员不存在");
            }

            // 检查职务是否为空
            if (movieStaff.getRole() == null || movieStaff.getRole().trim().isEmpty()) {
                return BaseResponse.error(403, "职务不能为空");
            }

            // 检查是否已存在相同关联
            LambdaQueryWrapper<MovieStaff> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MovieStaff::getMovieId, movieStaff.getMovieId())
                    .eq(MovieStaff::getActorId, movieStaff.getActorId())
                    .eq(MovieStaff::getRole, movieStaff.getRole());
            Long count = movieStaffMapper.selectCount(wrapper);
            if (count > 0) {
                return BaseResponse.error(403, "该人员在该电影中的职务已存在");
            }

            // 保存关联
            int result = movieStaffMapper.insert(movieStaff);
            if (result > 0) {
                log.info("新增电影人员关联成功: 电影ID={}, 演员ID={}, 职务={}",
                        movieStaff.getMovieId(), movieStaff.getActorId(), movieStaff.getRole());
                return BaseResponse.success("添加成功", movieStaff);
            } else {
                return BaseResponse.error(403, "添加失败");
            }
        } catch (Exception e) {
            log.error("添加电影人员关联异常", e);
            return BaseResponse.error(403, "系统异常");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse batchAdd(List<MovieStaff> list) {
        try {
            if (list == null || list.isEmpty()) {
                return BaseResponse.error(403, "批量添加列表不能为空");
            }

            // 校验每一条数据
            for (MovieStaff item : list) {
                if (item.getMovieId() == null || item.getActorId() == null || item.getRole() == null) {
                    return BaseResponse.error(403, "批量数据中存在无效记录");
                }

                // 检查电影是否存在
                Movie movie = movieMapper.selectById(item.getMovieId());
                if (movie == null) {
                    return BaseResponse.error(403, "电影不存在: ID=" + item.getMovieId());
                }

                // 检查演员是否存在
                Actor actor = actorMapper.selectById(item.getActorId());
                if (actor == null) {
                    return BaseResponse.error(403, "演员不存在: ID=" + item.getActorId());
                }
            }

            // 批量插入
            int result = movieStaffMapper.batchInsert(list);
            if (result > 0) {
                log.info("批量添加电影人员关联成功: {} 条", result);
                Map<String, Object> data = new HashMap<>();
                data.put("count", result);
                return BaseResponse.success("批量添加成功", data);
            } else {
                return BaseResponse.error(403, "批量添加失败");
            }
        } catch (Exception e) {
            log.error("批量添加电影人员关联异常", e);
            return BaseResponse.error(403, "系统异常");
        }
    }

    @Override
    public BaseResponse delete(Integer id) {
        try {
            if (id == null || id <= 0) {
                return BaseResponse.error(403, "关联ID不能为空");
            }

            // 检查关联是否存在
            MovieStaff exist = movieStaffMapper.selectById(id);
            if (exist == null) {
                return BaseResponse.error(403, "关联记录不存在");
            }

            // 删除关联
            int result = movieStaffMapper.deleteById(id);
            if (result > 0) {
                log.info("删除电影人员关联成功: ID={}", id);
                return BaseResponse.success("删除成功");
            } else {
                return BaseResponse.error(403, "删除失败");
            }
        } catch (Exception e) {
            log.error("删除电影人员关联异常", e);
            return BaseResponse.error(403, "系统异常");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse deleteByMovieIdAndActorId(Integer movieId, Integer actorId) {
        try {
            if (movieId == null || actorId == null) {
                return BaseResponse.error(403, "参数不能为空");
            }

            // 检查电影是否存在
            Movie movie = movieMapper.selectById(movieId);
            if (movie == null) {
                return BaseResponse.error(403, "电影不存在");
            }

            // 检查演员是否存在
            Actor actor = actorMapper.selectById(actorId);
            if (actor == null) {
                return BaseResponse.error(403, "演员不存在");
            }

            // 删除关联
            int result = movieStaffMapper.deleteByMovieIdAndActorId(movieId, actorId);
            if (result > 0) {
                log.info("删除电影人员关联成功: 电影ID={}, 演员ID={}", movieId, actorId);
                Map<String, Object> data = new HashMap<>();
                data.put("deletedCount", result);
                return BaseResponse.success("删除成功", data);
            } else {
                return BaseResponse.error(403, "关联记录不存在");
            }
        } catch (Exception e) {
            log.error("删除电影人员关联异常", e);
            return BaseResponse.error(403, "系统异常");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse deleteByMovieId(Integer movieId) {
        try {
            if (movieId == null) {
                return BaseResponse.error(403, "电影ID不能为空");
            }

            // 检查电影是否存在
            Movie movie = movieMapper.selectById(movieId);
            if (movie == null) {
                return BaseResponse.error(403, "电影不存在");
            }

            // 删除所有关联
            int result = movieStaffMapper.deleteByMovieId(movieId);
            log.info("删除电影所有人员关联成功: 电影ID={}, 共 {} 条", movieId, result);

            Map<String, Object> data = new HashMap<>();
            data.put("movieId", movieId);
            data.put("deletedCount", result);
            return BaseResponse.success("删除成功", data);
        } catch (Exception e) {
            log.error("删除电影人员关联异常", e);
            return BaseResponse.error(403, "系统异常");
        }
    }

    @Override
    public BaseResponse update(MovieStaff movieStaff) {
        try {
            if (movieStaff == null || movieStaff.getId() == null) {
                return BaseResponse.error(403, "关联ID不能为空");
            }

            // 检查关联是否存在
            MovieStaff exist = movieStaffMapper.selectById(movieStaff.getId());
            if (exist == null) {
                return BaseResponse.error(403, "关联记录不存在");
            }

            // 更新关联
            int result = movieStaffMapper.updateById(movieStaff);
            if (result > 0) {
                log.info("更新电影人员关联成功: ID={}", movieStaff.getId());
                return BaseResponse.success("更新成功", movieStaff);
            } else {
                return BaseResponse.error(403, "更新失败");
            }
        } catch (Exception e) {
            log.error("更新电影人员关联异常", e);
            return BaseResponse.error(403, "系统异常");
        }
    }

    @Override
    public BaseResponse getById(Integer id) {
        try {
            if (id == null || id <= 0) {
                return BaseResponse.error(403, "关联ID不能为空");
            }

            MovieStaff movieStaff = movieStaffMapper.selectById(id);
            if (movieStaff == null) {
                return BaseResponse.error(403, "关联记录不存在");
            }

            return BaseResponse.success(movieStaff);
        } catch (Exception e) {
            log.error("查询电影人员关联异常", e);
            return BaseResponse.error(403, "系统异常");
        }
    }

    @Override
    public BaseResponse getByMovieId(Integer movieId) {
        try {
            if (movieId == null) {
                return BaseResponse.error(403, "电影ID不能为空");
            }

            // 检查电影是否存在
            Movie movie = movieMapper.selectById(movieId);
            if (movie == null) {
                return BaseResponse.error(403, "电影不存在");
            }

            // 查询所有人员（带演员信息）
            List<MovieStaffVO> list = movieStaffMapper.selectByMovieId(movieId);

            // 按职务分组
            Map<String, Object> data = new HashMap<>();
            data.put("movieId", movieId);
            data.put("movieTitle", movie.getTitle());
            data.put("totalCount", list.size());
            data.put("staffList", list);

            return BaseResponse.success(data);
        } catch (Exception e) {
            log.error("查询电影人员异常", e);
            return BaseResponse.error(403, "系统异常");
        }
    }

    @Override
    public BaseResponse getByActorId(Integer actorId) {
        try {
            if (actorId == null) {
                return BaseResponse.error(403, "演员ID不能为空");
            }

            // 检查演员是否存在
            Actor actor = actorMapper.selectById(actorId);
            if (actor == null) {
                return BaseResponse.error(403, "演员不存在");
            }

            // 查询所有参与的电影（带电影信息）
            List<MovieStaffVO> list = movieStaffMapper.selectByActorId(actorId);

            Map<String, Object> data = new HashMap<>();
            data.put("actorId", actorId);
            data.put("actorName", actor.getName());
            data.put("totalCount", list.size());
            data.put("movieList", list);

            return BaseResponse.success(data);
        } catch (Exception e) {
            log.error("查询演员参与电影异常", e);
            return BaseResponse.error(403, "系统异常");
        }
    }

    @Override
    public BaseResponse getByMovieIdAndRole(Integer movieId, String role) {
        try {
            if (movieId == null || role == null || role.trim().isEmpty()) {
                return BaseResponse.error(403, "参数不能为空");
            }

            // 检查电影是否存在
            Movie movie = movieMapper.selectById(movieId);
            if (movie == null) {
                return BaseResponse.error(403, "电影不存在");
            }

            List<MovieStaffVO> list = movieStaffMapper.selectByMovieIdAndRole(movieId, role);

            Map<String, Object> data = new HashMap<>();
            data.put("movieId", movieId);
            data.put("role", role);
            data.put("totalCount", list.size());
            data.put("staffList", list);

            return BaseResponse.success(data);
        } catch (Exception e) {
            log.error("查询电影指定职务人员异常", e);
            return BaseResponse.error(403, "系统异常");
        }
    }

    @Override
    public BaseResponse listAll() {
        try {
            List<MovieStaff> list = movieStaffMapper.selectList(null);
            return BaseResponse.success(list);
        } catch (Exception e) {
            log.error("查询所有电影人员关联异常", e);
            return BaseResponse.error(403, "系统异常");
        }
    }
}