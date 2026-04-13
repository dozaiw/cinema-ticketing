package com.cinema.auth.filter;

import com.cinema.auth.entity.User;
import com.cinema.auth.util.JwtUtil;
import com.cinema.common.entity.BaseResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final StringRedisTemplate redisTemplate; // 新增Redis依赖

    // 定义JWT过期阈值：比如剩余时间小于30分钟，就自动刷新JWT
    private static final long REFRESH_THRESHOLD = 30 * 60 * 1000L;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            username = jwtUtil.getUsernameFromToken(token);
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            User user = (User) userDetails;
            String redisKey = "user:token:" + user.getId();

            // 优先校验Redis中的Token是否有效
            String redisToken = redisTemplate.opsForValue().get(redisKey);
            if (redisToken == null || !redisToken.equals(token)) {
                // Redis中无Token/Token不匹配 → 直接放行（后续会因JWT校验失败拒绝）
                filterChain.doFilter(request, response);
                return ;
            }

            // 刷新Redis中Token的过期时间
            redisTemplate.opsForValue().set(redisKey, token, jwtUtil.getExpireTime(), TimeUnit.MILLISECONDS);

            // 校验JWT是否有效，或是否需要刷新
            boolean isJwtValid = false;
            boolean needRefreshJwt = false;
            try {
                isJwtValid = jwtUtil.validateToken(token, userDetails);
                // 检查JWT剩余时间：小于阈值则需要刷新
                long remainingTime = jwtUtil.parseToken(token).getExpiration().getTime() - System.currentTimeMillis();
                if (remainingTime < REFRESH_THRESHOLD) {
                    needRefreshJwt = true;
                }
            } catch (Exception e) {
                // JWT已过期，但Redis有效 → 也需要刷新JWT
                needRefreshJwt = true;
            }

            // 步骤4：JWT有效 或 Redis有效（即使JWT过期）→ 封装认证信息
            if (isJwtValid || needRefreshJwt) {
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        user, null, userDetails.getAuthorities()
                );
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                // 步骤5：自动刷新JWT
                if (needRefreshJwt) {
                    String newToken = jwtUtil.generateToken(user);
                    // 更新Redis中的Token
                    redisTemplate.opsForValue().set(redisKey, newToken, jwtUtil.getExpireTime(), TimeUnit.MILLISECONDS);
                    // 将新Token返回给前端（前端需监听此响应头，替换本地Token）
                    response.setHeader("X-Refresh-Token", newToken);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}