package com.cinema.movie.services.impl;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cinema.auth.config.CosConfig;
import com.cinema.auth.util.CosUtil;
import com.cinema.common.entity.BaseResponse;
import com.cinema.common.entity.PageResult;
import com.cinema.common.entity.ResultCode;
import com.cinema.movie.dto.MovieGenreDTO;
import com.cinema.movie.entity.*;
import com.cinema.movie.mapper.*;
import com.cinema.movie.services.MovieService;
import com.cinema.movie.services.MovieStaffService;
import com.cinema.movie.vo.MovieStaffVO;
import com.cinema.movie.vo.MovieVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MovieServiceImpl extends ServiceImpl<MovieMapper, Movie> implements MovieService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MovieMapper movieMapper;

    @Autowired
    private GenreMapper genreMapper;

    @Autowired
    private MovieGenreMapper movieGenreMapper;

    @Autowired
    private CosUtil cosUtil;

    @Autowired
    private CosConfig cosConfig;

    @Autowired
    private ActorMapper actorMapper;

    @Autowired
    private MovieStaffService movieStaffService;

    @Autowired
    private SeatScheduleMapper seatScheduleMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private MovieFavoriteMapper movieFavoriteMapper;

    @Value("${movie.cache.hot-movies.expire}")
    private long hotMovieExpireTime;

    /**
     * 为电影列表填充演员信息
     */
    private void fillStaffInfo(List<MovieVO> movieVOs) {
        if (movieVOs == null || movieVOs.isEmpty()) {
            return;
        }

        // 为每个电影查询演员信息
        for (MovieVO movieVO : movieVOs) {
            BaseResponse response = movieStaffService.getByMovieId(movieVO.getId());
            if (response.getCode() == 200 && response.getData() instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) response.getData();
                List<MovieStaffVO> staffList = (List<MovieStaffVO>) data.get("staffList");
                movieVO.setStaffList(staffList != null ? staffList : new ArrayList<>());
            } else {
                movieVO.setStaffList(new ArrayList<>());
            }
        }
    }

    /**
     * 将 Movie 列表转换为 MovieVO 列表并填充演员信息
     */
    private List<MovieVO> convertToVOWithStaff(List<Movie> movies) {
        if (movies == null || movies.isEmpty()) {
            return new ArrayList<>();
        }

        List<MovieVO> movieVOs = movies.stream()
                .map(MovieVO::fromMovie)
                .collect(Collectors.toList());

        fillStaffInfo(movieVOs);
        return movieVOs;
    }

    @Override
    public BaseResponse getHotMovies(Integer pageNum, Integer pageSize) {
        try {
            // 1. 参数校验和默认值
            if (pageNum == null || pageNum < 1) pageNum = 1;
            if (pageSize == null || pageSize < 1) pageSize = 10;
            pageSize = Math.min(pageSize, 50);

            // 2. 从 Redis 获取完整列表
            String cacheKey = "hotMovies";
            List<Movie> allMovies = (List<Movie>) redisTemplate.opsForValue().get(cacheKey);

            if (allMovies == null) {
                log.info("从数据库获取热门电影");
                allMovies = movieMapper.selectList(
                        new LambdaQueryWrapper<Movie>().eq(Movie::getStatus, 1)
                );
                redisTemplate.opsForValue().set(cacheKey, allMovies, hotMovieExpireTime, TimeUnit.SECONDS);
            } else {
                log.info("从缓存获取热门电影");
            }

            // 3. 内存分页
            PageResult<Movie> pageResult = paginate(allMovies, pageNum, pageSize);

            // 4. 转换为 VO 并填充演员信息
            List<MovieVO> movieVOs = convertToVOWithStaff(pageResult.getRecords());

            PageResult<MovieVO> voPageResult = new PageResult<>(
                    movieVOs,
                    pageResult.getTotal(),
                    pageResult.getPageNum(),
                    pageResult.getPageSize()
            );

            return BaseResponse.success(voPageResult);

        } catch (Exception e) {
            log.error("获取热门电影失败", e);
            return BaseResponse.error(403, "获取热门电影失败");
        }
    }

    @Override
    public BaseResponse getWaitMovies(Integer pageNum, Integer pageSize) {
        try {
            // 1. 参数校验和默认值
            if (pageNum == null || pageNum < 1) pageNum = 1;
            if (pageSize == null || pageSize < 1) pageSize = 10;
            pageSize = Math.min(pageSize, 50);

            // 2. 从 Redis 获取完整列表
            String cacheKey = "waitMovies";
            List<Movie> allMovies = (List<Movie>) redisTemplate.opsForValue().get(cacheKey);

            if (allMovies == null) {
                log.info("从数据库获取待映电影");
                allMovies = movieMapper.selectList(
                        new LambdaQueryWrapper<Movie>().eq(Movie::getStatus, 2)
                );
                redisTemplate.opsForValue().set(cacheKey, allMovies, hotMovieExpireTime, TimeUnit.SECONDS);
            } else {
                log.info("从缓存获取待映电影");
            }

            // 3. 内存分页
            PageResult<Movie> pageResult = paginate(allMovies, pageNum, pageSize);

            // 4.  转换为 VO 并填充演员信息
            List<MovieVO> movieVOs = convertToVOWithStaff(pageResult.getRecords());

            PageResult<MovieVO> voPageResult = new PageResult<>(
                    movieVOs,
                    pageResult.getTotal(),
                    pageResult.getPageNum(),
                    pageResult.getPageSize()
            );

            return BaseResponse.success(voPageResult);

        } catch (Exception e) {
            log.error("获取待映电影失败", e);
            return BaseResponse.error(403, "获取待映电影失败");
        }
    }

    private <T> PageResult<T> paginate(List<T> list, int pageNum, int pageSize) {
        if (list == null || list.isEmpty()) {
            return new PageResult<>(new ArrayList<>(), 0L, pageNum, pageSize);
        }

        int total = list.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        if (fromIndex >= total) {
            return new PageResult<>(new ArrayList<>(), (long) total, pageNum, pageSize);
        }

        List<T> pageData = list.subList(fromIndex, toIndex);
        return new PageResult<>(pageData, (long) total, pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse addMovie(MovieGenreDTO movieGenreDTO) {
        try {
            Movie movie = movieGenreDTO.getMovie();
            List<Genre> genres = movieGenreDTO.getGenres();
            MultipartFile posterFile = movieGenreDTO.getPosterFile();
            MultipartFile trailerFile = movieGenreDTO.getTrailerFile();

            Date now = new Date();
            Date offlineDate = movie.getOfflineDate();
            Date releaseDate = movie.getReleaseDate();

            if (releaseDate == null) {
                return BaseResponse.error(ResultCode.USER_ADD_MOVIE_FAILED);
            }
            if (releaseDate.after(now)) {
                movie.setStatus(2);
            } else if (offlineDate != null && offlineDate.before(now)) {
                movie.setStatus(3);
            } else {
                movie.setStatus(1);
            }

            // 上传海报
            if (posterFile != null && !posterFile.isEmpty()) {
                try {
                    String posterUrl = cosUtil.uploadFile(posterFile, "poster");
                    movie.setPoster(posterUrl);
                    log.info(" 海报上传成功：{}", posterUrl);
                } catch (Exception e) {
                    log.error("海报上传失败", e);
                    return BaseResponse.error(403, "海报上传失败");
                }
            }

            // 上传预告片
            if (trailerFile != null && !trailerFile.isEmpty()) {
                try {
                    String trailerUrl = cosUtil.uploadFile(trailerFile, "trailer");
                    movie.setTrailerUrl(trailerUrl);
                    log.info(" 预告片上传成功：{}", trailerUrl);
                } catch (Exception e) {
                    if (movie.getPoster() != null) {
                        cosUtil.deleteFile(movie.getPoster());
                    }
                    log.error("预告片上传失败", e);
                    return BaseResponse.error(403, "预告片上传失败");
                }
            }

            this.movieMapper.insert(movie);
            Integer id = movie.getId();

            for (Genre genre : genres) {
                MovieGenre movieGenre = new MovieGenre();
                movieGenre.setMovieId(id);
                movieGenre.setGenreId(genre.getId());
                movieGenreMapper.insert(movieGenre);
            }

            return BaseResponse.success("添加成功");
        } catch (Exception e) {
            log.error("添加电影失败", e);
            return BaseResponse.error(ResultCode.USER_ADD_MOVIE_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse changeMovie(MovieGenreDTO movieGenreDTO) {
        Movie movie = movieGenreDTO.getMovie();
        List<Genre> genres = movieGenreDTO.getGenres();
        MultipartFile posterFile = movieGenreDTO.getPosterFile();
        MultipartFile trailerFile = movieGenreDTO.getTrailerFile();
        String oldPosterUrl = movieGenreDTO.getOldPosterUrl();
        String oldTrailerUrl = movieGenreDTO.getOldTrailerUrl();

        try {
            Date now = new Date();
            Date offlineDate = movie.getOfflineDate();
            Date releaseDate = movie.getReleaseDate();

            if (releaseDate == null) {
                return BaseResponse.error(ResultCode.USER_CHANGE_MOVIE_FAILED);
            }
            if (releaseDate.after(now)) {
                movie.setStatus(2);
            } else if (offlineDate != null && offlineDate.before(now)) {
                movie.setStatus(3);
            } else {
                movie.setStatus(1);
            }

            // 更新海报
            if (posterFile != null && !posterFile.isEmpty()) {
                String newPosterUrl = cosUtil.uploadFile(posterFile, "poster");
                movie.setPoster(newPosterUrl);

                if (oldPosterUrl != null && !oldPosterUrl.isEmpty()) {
                    cosUtil.deleteFile(oldPosterUrl);
                }
            }

            // 更新预告片
            if (trailerFile != null && !trailerFile.isEmpty()) {
                String newTrailerUrl = cosUtil.uploadFile(trailerFile, "trailer");
                movie.setTrailerUrl(newTrailerUrl);

                if (oldTrailerUrl != null && !oldTrailerUrl.isEmpty()) {
                    cosUtil.deleteFile(oldTrailerUrl);
                }
            }

            this.movieMapper.updateById(movie);
            movieGenreMapper.delete(new LambdaQueryWrapper<MovieGenre>().eq(MovieGenre::getMovieId, movie.getId()));

            for (Genre genre : genres) {
                MovieGenre movieGenre = new MovieGenre();
                movieGenre.setMovieId(movie.getId());
                movieGenre.setGenreId(genre.getId());
                movieGenreMapper.insert(movieGenre);
            }

            return BaseResponse.success("修改成功");
        } catch (Exception e) {
            log.error("修改电影失败", e);
            return BaseResponse.error(ResultCode.USER_CHANGE_MOVIE_FAILED);
        }
    }

    @Override
    public BaseResponse changeState(Integer movieId, Integer state) {
        try {
            this.movieMapper.update(null,
                    new LambdaUpdateWrapper<Movie>()
                            .eq(Movie::getId, movieId)
                            .set(Movie::getStatus, state)
            );
            return BaseResponse.success(ResultCode.USER_CHANGE_STATE_SUCCESS);
        } catch (Exception e) {
            return BaseResponse.error(ResultCode.USER_CHANGE_STATE_FAILED);
        }
    }

    @Override
    public BaseResponse getAllMovie(Integer pageNum, Integer pageSize) {
        try {
            // 1. 参数校验和默认值
            if (pageNum == null || pageNum < 1) pageNum = 1;
            if (pageSize == null || pageSize < 1) pageSize = 10;
            pageSize = Math.min(pageSize, 50);

            // 2. 使用 MyBatis-Plus 分页查询
            Page<Movie> page = new Page<>(pageNum, pageSize);
            LambdaQueryWrapper<Movie> queryWrapper = new LambdaQueryWrapper<Movie>()
                    .in(Movie::getStatus, 1, 2, 3)
                    .orderByDesc(Movie::getReleaseDate);

            Page<Movie> result = movieMapper.selectPage(page, queryWrapper);

            // 3. 转换为 VO 并填充演员信息
            List<MovieVO> movieVOs = convertToVOWithStaff(result.getRecords());

            PageResult<MovieVO> voPageResult = new PageResult<>(
                    movieVOs,
                    result.getTotal(),
                    pageNum,
                    pageSize
            );

            return BaseResponse.success(voPageResult);

        } catch (Exception e) {
            log.error("获取所有电影失败", e);
            return BaseResponse.error(ResultCode.USER_GET_ALL_MOVIE_FAILED);
        }
    }

    @Override
    public BaseResponse findByName(String name, Integer pageNum, Integer pageSize) {
        try {
            // 1. 参数校验和默认值
            if (name == null || name.trim().isEmpty()) {
                return BaseResponse.error(403, "搜索关键词不能为空");
            }
            if (pageNum == null || pageNum < 1) pageNum = 1;
            if (pageSize == null || pageSize < 1) pageSize = 10;
            pageSize = Math.min(pageSize, 50);

            // 2. 构建缓存 Key
            String cacheKey = "movie_search_" + name.trim();
            List<Movie> allMovies = (List<Movie>) redisTemplate.opsForValue().get(cacheKey);

            if (allMovies == null) {
                log.info("从数据库搜索电影：{}", name);
                allMovies = movieMapper.selectList(
                        new LambdaQueryWrapper<Movie>()
                                .like(Movie::getTitle, name.trim())
                                .in(Movie::getStatus, 1, 2)
                                .orderByDesc(Movie::getReleaseDate)
                );
                redisTemplate.opsForValue().set(cacheKey, allMovies, 300, TimeUnit.SECONDS);
            } else {
                log.info("从缓存搜索电影：{}", name);
            }

            // 3. 内存分页
            PageResult<Movie> pageResult = paginate(allMovies, pageNum, pageSize);

            // 4. 转换为 VO 并填充演员信息
            List<MovieVO> movieVOs = convertToVOWithStaff(pageResult.getRecords());

            PageResult<MovieVO> voPageResult = new PageResult<>(
                    movieVOs,
                    pageResult.getTotal(),
                    pageResult.getPageNum(),
                    pageResult.getPageSize()
            );

            return BaseResponse.success(voPageResult);

        } catch (Exception e) {
            log.error("搜索电影失败", e);
            return BaseResponse.error(403, "搜索电影失败");
        }
    }

    @Override
    public BaseResponse findByGenre(String genre, Integer pageNum, Integer pageSize) {
        try {
            // 1. 参数校验和默认值
            if (genre == null || genre.trim().isEmpty()) {
                return BaseResponse.error(403, "类型名称不能为空");
            }
            if (pageNum == null || pageNum < 1) pageNum = 1;
            if (pageSize == null || pageSize < 1) pageSize = 10;
            pageSize = Math.min(pageSize, 50);

            String cacheKey = "movie_genre_" + genre.trim();
            List<Movie> allMovies;

            try {
                allMovies = (List<Movie>) redisTemplate.opsForValue().get(cacheKey);
                if (allMovies != null) {
                    log.info("从缓存获取类型电影：{}", genre);
                    //  转换为 VO 并填充演员信息
                    List<MovieVO> movieVOs = convertToVOWithStaff(allMovies);
                    PageResult<MovieVO> pageResult = paginate(movieVOs, pageNum, pageSize);
                    return BaseResponse.success(pageResult);
                }
            } catch (Exception e) {
                log.error("查询 Redis 缓存失败", e);
            }

            // 缓存未命中，查询数据库
            log.info("从数据库获取类型电影：{}", genre);

            Genre genreObj = genreMapper.selectOne(
                    new LambdaQueryWrapper<Genre>().eq(Genre::getName, genre.trim())
            );

            if (genreObj == null) {
                return BaseResponse.error(ResultCode.USER_GET_MOVIE_BY_GENRE_FAILED);
            }

            List<MovieGenre> movieGenres = movieGenreMapper.selectList(
                    new LambdaQueryWrapper<MovieGenre>().eq(MovieGenre::getGenreId, genreObj.getId())
            );

            if (CollectionUtils.isEmpty(movieGenres)) {
                allMovies = Collections.emptyList();
                try {
                    redisTemplate.opsForValue().set(cacheKey, allMovies, 300, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("缓存空列表失败", e);
                }
                PageResult<MovieVO> pageResult = new PageResult<>(
                        new ArrayList<>(), 0L, pageNum, pageSize
                );
                return BaseResponse.success(pageResult);
            }

            List<Integer> movieIds = movieGenres.stream()
                    .map(MovieGenre::getMovieId)
                    .distinct()
                    .collect(Collectors.toList());

            allMovies = movieMapper.selectList(
                    new LambdaQueryWrapper<Movie>()
                            .in(Movie::getId, movieIds)
                            .in(Movie::getStatus, 1, 2)
                            .orderByDesc(Movie::getReleaseDate)
            );

            try {
                redisTemplate.opsForValue().set(cacheKey, allMovies, 300, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("缓存电影列表失败", e);
            }

            // 转换为 VO 并填充演员信息
            List<MovieVO> movieVOs = convertToVOWithStaff(allMovies);
            PageResult<MovieVO> pageResult = paginate(movieVOs, pageNum, pageSize);
            return BaseResponse.success(pageResult);

        } catch (Exception e) {
            log.error("按类型查询电影失败", e);
            return BaseResponse.error(ResultCode.USER_GET_MOVIE_BY_GENRE_FAILED);
        }
    }

    @Override
    public BaseResponse getHotMovieCount() {
        try {
            Long count = movieMapper.selectCount(
                    new LambdaQueryWrapper<Movie>().eq(Movie::getStatus, 1)
            );
            return BaseResponse.success(count);
        } catch (Exception e) {
            log.error("获取热门电影数量失败", e);
            return BaseResponse.error(403, "获取热门电影数量失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse deleteMovie(Integer movieId) {
        try {
            log.info("删除电影：movieId={}", movieId);

            //  删除电影类型关联
            movieGenreMapper.delete(new LambdaQueryWrapper<MovieGenre>()
                    .eq(MovieGenre::getMovieId, movieId));

            //  删除电影演员关联
            movieStaffService.deleteByMovieId(movieId);

            //  删除相关排片及排片座位
            List<Schedule> schedules = scheduleMapper.selectList(
                    new LambdaQueryWrapper<Schedule>().eq(Schedule::getMovieId, movieId));

            //  删除电影收藏关联
            movieFavoriteMapper.delete(new LambdaQueryWrapper<MovieFavorite>().eq(MovieFavorite::getMovieId, movieId));

            // 只有当有排片时才删除座位数据
            if (!schedules.isEmpty()) {
                List<Integer> scheduleIds = schedules.stream()
                        .map(Schedule::getId)
                        .collect(Collectors.toList());

                seatScheduleMapper.delete(new LambdaQueryWrapper<SeatSchedule>()
                        .in(SeatSchedule::getScheduleId, scheduleIds));

                log.info("删除座位数据：{} 条", scheduleIds.size());
            } else {
                log.debug("电影 {} 无排片，跳过座位数据删除", movieId);
            }

            // 删除排片主表
            scheduleMapper.delete(new LambdaQueryWrapper<Schedule>()
                    .eq(Schedule::getMovieId, movieId));

            //删除电影主表
            movieMapper.deleteById(movieId);

            // TODO: 对未使用的排片进行退款
            // refundService.processRefundForMovie(movieId);

            log.info("电影删除成功：movieId={}", movieId);
            return BaseResponse.success("删除成功");

        } catch (Exception e) {
            log.error("删除电影失败：movieId={}", movieId, e);
            // 建议返回具体错误信息，方便排查
            return BaseResponse.error(403, "删除失败：" + e.getMessage());
        }
    }

    @Override
    public BaseResponse getMovieDetail(Integer movieId) {
        try {
            // 1. 参数校验
            if (movieId == null || movieId < 1) {
                return BaseResponse.error(403, "电影 ID 无效");
            }

            // 2. 构建缓存 Key
            String cacheKey = "movie_detail_" + movieId;

            // 3. 尝试从 Redis 获取完整详情
            MovieVO cachedVO = (MovieVO) redisTemplate.opsForValue().get(cacheKey);
            if (cachedVO != null) {
                log.info("从缓存获取电影详情：movieId={}", movieId);
                return BaseResponse.success(cachedVO);
            }

            // 4. 缓存未命中，查询数据库
            log.info("从数据库获取电影详情：movieId={}", movieId);
            Movie movie = movieMapper.selectById(movieId);

            if (movie == null) {
                return BaseResponse.error(403,"电影不存在");
            }

            // 5. 转换为 VO
            MovieVO movieVO = MovieVO.fromMovie(movie);

            // 6. 查询演职人员信息
            BaseResponse staffResponse = movieStaffService.getByMovieId(movieId);
            if (staffResponse.getCode() == 200 && staffResponse.getData() instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) staffResponse.getData();
                List<MovieStaffVO> staffList = (List<MovieStaffVO>) data.get("staffList");
                movieVO.setStaffList(staffList != null ? staffList : new ArrayList<>());

                // 补充电影标题（如果 VO 中没有）
                String movieTitle = (String) data.get("movieTitle");
                if (movieTitle != null && movieVO.getTitle() == null) {
                    movieVO.setTitle(movieTitle);
                }
            } else {
                movieVO.setStaffList(new ArrayList<>());
            }

            // 7. 查询电影类型
            List<MovieGenre> movieGenres = movieGenreMapper.selectList(
                    new LambdaQueryWrapper<MovieGenre>().eq(MovieGenre::getMovieId, movieId)
            );

            if (!movieGenres.isEmpty()) {
                List<Integer> genreIds = movieGenres.stream()
                        .map(MovieGenre::getGenreId)
                        .collect(Collectors.toList());

                List<Genre> genres = genreMapper.selectList(
                        new LambdaQueryWrapper<Genre>().in(Genre::getId, genreIds)
                );

                movieVO.setGenres(genres != null ? genres : new ArrayList<>());
            } else {
                movieVO.setGenres(new ArrayList<>());
            }

            // 8. 存入 Redis 缓存（设置过期时间）
            try {
                redisTemplate.opsForValue().set(cacheKey, movieVO, hotMovieExpireTime, TimeUnit.SECONDS);
                log.info("电影详情已缓存：movieId={}", movieId);
            } catch (Exception e) {
                log.error("缓存电影详情失败：movieId={}", movieId, e);
                // 缓存失败不影响返回结果
            }

            return BaseResponse.success(movieVO);

        } catch (Exception e) {
            log.error("获取电影详情失败：movieId={}", movieId, e);
            return BaseResponse.error(403, "获取电影详情失败");
        }
    }
}