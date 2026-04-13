// com.cinema.ai.tool.MovieTool.java
package com.cinema.ai.Tool;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.cinema.ai.client.MovieFeignClient;
import com.cinema.ai.entity.Movie;
import com.cinema.ai.entity.Genre;
import com.cinema.auth.util.UserContextUtil;
import com.cinema.common.entity.BaseResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MovieTool {

    @Resource
    private MovieFeignClient movieFeignClient;
    
    @Resource
    private UserContextUtil userContextUtil;
    
    //  添加初始化检查
    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("MovieTool 初始化成功");
        log.info("MovieFeignClient 注入状态：{}", movieFeignClient != null ? "成功" : "失败");
        log.info("UserContextUtil 注入状态：{}", userContextUtil != null ? "成功" : "失败");
    }

    /**
     * 搜索电影（支持标题/类型/演员）
     */
    @Tool(
        name = "searchMovies",
        description = "搜索电影库，支持按标题、类型、演员名称筛选。返回电影列表（ID/标题/海报/时长/简介）"
    )
    public String searchMovies(
            @ToolParam(description = "电影标题") String title,
            @ToolParam(description = "电影类型") String genre,
            @ToolParam(description = "演员名称") String actor) {
        
        try {
            BaseResponse response;
            
            if (StrUtil.isNotBlank(title)) {
                response = movieFeignClient.findMoviesByName(title, 1, 10);
            } else if (StrUtil.isNotBlank(genre)) {
                response = movieFeignClient.findMoviesByGenre(genre, 1, 10);
            } else if (StrUtil.isNotBlank(actor)) {
                response = movieFeignClient.findMoviesByName(actor, 1, 10);
            } else {
                response = movieFeignClient.getHotMovies(1, 10);
            }
            
            if (response.getCode() != 200 || response.getData() == null) {
                return " 搜索失败：" + response.getMsg();
            }
            
            //  将 data 作为 Object 处理，然后提取 List
            Object dataObj = response.getData();
            List<Map<String, Object>> movies = extractMovies(dataObj);
            
            if (movies.isEmpty()) {
                return "未找到相关电影，换个关键词试试吧~";
            }
            
            // 返回精简信息给 AI
            return JSONUtil.toJsonStr(movies.stream()
                .limit(5)
                .map(m -> Map.of(
                    "id", m.get("id"),
                    "title", m.get("title").toString(),
                    "poster", m.get("poster") != null ? m.get("poster").toString() : "",
                    "duration", m.get("duration") != null ? m.get("duration") + "分钟" : "未知",
                    "description", StrUtil.sub(m.get("description").toString(), 0, 60) + "..."
                ))
                .collect(Collectors.toList()));
                
        } catch (Exception e) {
            log.error("搜索电影异常", e);
            return "服务繁忙，请稍后重试";
        }
    }
    
    /**
     * 辅助方法：从响应数据中提取电影列表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractMovies(Object dataObj) {
        if (dataObj == null) {
            return new ArrayList<>();
        }
        
        try {
            // 情况 1：直接是 List
            if (dataObj instanceof List) {
                return (List<Map<String, Object>>) dataObj;
            }
            
            // 情况 2：PageResult 结构（包含 records 字段）
            if (dataObj instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                Object recordsObj = dataMap.get("records");
                if (recordsObj instanceof List) {
                    return (List<Map<String, Object>>) recordsObj;
                }
            }
            
            // 其他情况
            return new ArrayList<>();
            
        } catch (Exception e) {
            log.error("提取电影列表失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 查询用户收藏的电影
     */
    @Tool(
        name = "getFavoriteMovies",
        description = "查询**当前登录用户**的收藏电影列表。无需传参。支持按标题/类型筛选收藏"
    )
    public String getFavoriteMovies(
            @ToolParam(description = "在收藏中按标题筛选") String title,
            @ToolParam(description = "在收藏中按类型筛选") String genre) {
        
        try {
            Integer userId = userContextUtil.getUserId();
            
            log.info("查询用户收藏，userId={}", userId);
            
            // 🔑 使用 raw type BaseResponse
            BaseResponse response = movieFeignClient.getUserFavoriteMovies();
            
            log.info("Feign 返回：code={}, msg={}", response.getCode(), response.getMsg());
            
            if (response.getCode() != 200 || response.getData() == null) {
                log.error("查询收藏失败：{}", response.getMsg());
                return "查询收藏失败：" + response.getMsg();
            }
            
            // 提取电影列表
            Object dataObj = response.getData();
            List<Map<String, Object>> movies = extractMovies(dataObj);
            
            if (movies.isEmpty()) {
                return "📭 您还没有收藏任何电影哦~\n💡 去【电影列表】发现好电影吧！🎬";
            }

            // 3. 内存过滤
            if (StrUtil.isNotBlank(title) || StrUtil.isNotBlank(genre)) {
                final List<Map<String, Object>> filteredMovies = new ArrayList<>(movies);
                movies = filteredMovies.stream()
                        .filter(m -> {
                            boolean titleMatch = StrUtil.isBlank(title) ||
                                    m.get("title").toString().contains(title);

                            boolean genreMatch = StrUtil.isBlank(genre) ||
                                    m.get("description").toString().contains(genre) ||
                                    m.get("title").toString().contains(genre);

                            return titleMatch && genreMatch;
                        })
                        .collect(Collectors.toList());

                if (movies.isEmpty()) {
                    return "📭 您的收藏中没有符合条件的电影~";
                }
            }
            
            //  4. 返回精简信息
            return JSONUtil.toJsonStr(movies.stream()
                .limit(5)
                .map(m -> Map.of(
                    "id", m.get("id"),
                    "title", m.get("title").toString(),
                    "poster", m.get("poster") != null ? m.get("poster").toString() : "",
                    "duration", m.get("duration") != null ? m.get("duration") + "分钟" : "未知",
                    "description", StrUtil.sub(m.get("description").toString(), 0, 60) + "..."
                ))
                .collect(Collectors.toList()));
                
        } catch (Exception e) {
            log.error("查询收藏异常", e);
            return "❌ 服务繁忙，请稍后重试";
        }
    }

    /**
     * 查询某电影是否被当前用户收藏
     */
    @Tool(
        name = "checkFavoriteStatus",
        description = "检查**当前用户**是否收藏了指定电影。返回 true/false"
    )
    public String checkFavoriteStatus(
            @ToolParam(description = "电影 ID") Integer movieId) {
        
        try {
            // 使用 raw type BaseResponse
            BaseResponse response = movieFeignClient.getFavoriteStatus(movieId);
            
            if (response.getCode() != 200 || response.getData() == null) {
                return "false";
            }
            
            // 从 Object 中提取 Boolean
            Object dataObj = response.getData();
            Boolean isFavorite = false;
            if (dataObj instanceof Boolean) {
                isFavorite = (Boolean) dataObj;
            } else if (dataObj instanceof String) {
                isFavorite = Boolean.parseBoolean(dataObj.toString());
            } else if (dataObj instanceof Number) {
                isFavorite = ((Number) dataObj).intValue() != 0;
            }
            
            return isFavorite ? "true" : "false";
            
        } catch (Exception e) {
            log.error("查询收藏状态异常", e);
            return "false";
        }
    }

    /**
     * 获取所有电影类型
     */
    @Tool(
        name = "getAllGenres",
        description = "获取所有电影类型列表（如：喜剧/动作/爱情/科幻...），用于帮助用户筛选"
    )
    public String getAllGenres() {
        try {
            // 使用 raw type BaseResponse
            BaseResponse response = movieFeignClient.getAllGenres();
            
            if (response.getCode() != 200 || response.getData() == null) {
                return "❌ 获取类型失败";
            }
            
            //  从 Object 中提取类型列表
            Object dataObj = response.getData();
            List<String> genreNames = new ArrayList<>();
            
            if (dataObj instanceof List) {
                List<Map<String, Object>> genres = (List<Map<String, Object>>) dataObj;
                genreNames = genres.stream()
                        .map(g -> {
                            Object nameObj = g.get("name");
                            return nameObj != null ? nameObj.toString() : "";
                        })
                        .filter(name -> !name.isEmpty())
                        .collect(Collectors.toList());
            }
            
            if (genreNames.isEmpty()) {
                return " 暂无电影类型";
            }
            
            return JSONUtil.toJsonStr(Map.of("genres", genreNames));
            
        } catch (Exception e) {
            log.error("获取类型异常", e);
            return " 服务繁忙";
        }
    }
}