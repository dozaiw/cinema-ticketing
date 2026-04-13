package com.cinema.auth.services.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cinema.auth.entity.User;
import com.cinema.auth.mapper.UserMapper;
import com.cinema.auth.services.SandboxAccountService;
import com.cinema.auth.services.UserService;
import com.cinema.auth.vo.UserVO;
import com.cinema.common.entity.BaseResponse;
import com.cinema.common.entity.PageResult;
import com.cinema.common.entity.ResultCode;
import com.cinema.common.util.BeanCopyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// 仅标注为userService，专注业务逻辑，不再实现UserDetailsService
@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private SandboxAccountService sandboxAccountService;

    @Override
    public BaseResponse regist(User user) {
        if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
            if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
                LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper();
                queryWrapper.eq(User::getPhone, user.getPhone());
                User existUser = this.userMapper.selectOne(queryWrapper);
                if (existUser != null) {
                    return BaseResponse.error(ResultCode.USER_REGISTER_FAILED.getCode(), "手机号已注册");
                } else {
                    String encryptedPassword = this.passwordEncoder.encode(user.getPassword());
                    user.setPassword(encryptedPassword);
                    user.setStatus(1);
                    if (user.getRole() == null) {
                        user.setRole(1);
                    }
                    if (user.getUsername() == null) {
                        user.setUsername(user.getPhone());
                    }
                    int insertResult = this.userMapper.insert(user);
                    // 进行资金账号创建
                    if (insertResult > 0 && user.getId() != null) {
                        sandboxAccountService.createAccount(user.getId());
                        return BaseResponse.success(ResultCode.USER_REGISTER_SUCCESS.getMsg(), null);
                    } else {
                        return BaseResponse.error(ResultCode.USER_REGISTER_FAILED);
                    }
                }
            } else {
                return BaseResponse.error(ResultCode.PARAM_ERROR);
            }
        } else {
            return BaseResponse.error(ResultCode.PARAM_ERROR);
        }
    }

    @Override
    public BaseResponse getUserInfo(String username) {
        User user = this.userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        return user != null ? BaseResponse.success(user) : BaseResponse.error(ResultCode.USER_NOT_EXIST);
    }

    @Override
    public BaseResponse getUserList(Integer pageNum, Integer pageSize) {
        try {
            // 1. 参数校验和默认值
           if (pageNum == null || pageNum < 1) pageNum = 1;
           if (pageSize == null || pageSize < 1) pageSize = 10;
            // 限制最大页大小
            pageSize = Math.min(pageSize, 50);

            log.info("查询用户列表：pageNum={}, pageSize={}", pageNum, pageSize);

            // 2. 使用 MyBatis-Plus 分页查询
            Page<User> page = new Page<>(pageNum, pageSize);

            IPage<User> result = userMapper.selectPage(page,null);

            // 3. 转换为 VO
            List<UserVO> userVOList = new ArrayList<>();
           if (result.getRecords() != null && !result.getRecords().isEmpty()) {
                userVOList = BeanCopyUtil.copyList(result.getRecords(), UserVO.class);
            }

            // 4. 构建分页结果
            PageResult<UserVO> pageResult = new PageResult<>(
                    userVOList,
                    result.getTotal(),
                    pageNum,
                    pageSize
            );

            log.info("用户列表查询成功：总记录数={}, 当前页={}, 每页={}",
                    result.getTotal(), pageNum, pageSize);

            return BaseResponse.success(pageResult);

        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return BaseResponse.error(403,"获取用户列表失败：");
        }
    }

    @Override
   public BaseResponse getFilteredUserList(Integer pageNum, Integer pageSize, String nickname, Integer status, Integer role) {
        try {
            // 1. 参数校验和默认值
           if (pageNum == null || pageNum < 1) pageNum = 1;
           if (pageSize == null || pageSize < 1) pageSize = 10;
            pageSize = Math.min(pageSize, 50);

            log.info("筛选用户列表：pageNum={}, pageSize={}, nickname={}, status={}, role={}", 
                    pageNum, pageSize, nickname, status, role);

            // 2. 构建查询条件
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            
            // 昵称模糊查询
           if (nickname != null && !nickname.trim().isEmpty()) {
                queryWrapper.like(User::getNickname, nickname.trim());
            }
            
            // 状态精确匹配
           if (status != null) {
                queryWrapper.eq(User::getStatus, status);
            }
            
            // 角色精确匹配
           if (role != null) {
                queryWrapper.eq(User::getRole, role);
            }

            // 3. 分页查询
            Page<User> page = new Page<>(pageNum, pageSize);
            IPage<User> result = userMapper.selectPage(page, queryWrapper);

            // 4. 转换为 VO
            List<UserVO> userVOList = new ArrayList<>();
           if (result.getRecords() != null && !result.getRecords().isEmpty()) {
                userVOList = BeanCopyUtil.copyList(result.getRecords(), UserVO.class);
            }

            // 5. 构建分页结果
            PageResult<UserVO> pageResult = new PageResult<>(
                    userVOList,
                    result.getTotal(),
                    pageNum,
                    pageSize
            );

            log.info("筛选用户列表成功：总记录数={}, 当前页={}, 每页={}",
                    result.getTotal(), pageNum, pageSize);

            return BaseResponse.success(pageResult);

        } catch (Exception e) {
            log.error("筛选用户列表失败", e);
            return BaseResponse.error(403, "筛选用户列表失败：" + e.getMessage());
        }
    }

    @Override
    public BaseResponse changeState(String username, Integer state) {
        try {
            User user = this.userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
            user.setStatus(state);
            int updateResult = this.userMapper.updateById(user);
            if (updateResult > 0) {
                return BaseResponse.success(ResultCode.USER_CHANGE_STATE_SUCCESS.getMsg());
            }
        } catch (Exception var5) {
            return BaseResponse.error(ResultCode.USER_CHANGE_STATE_FAILED);
        }
        return BaseResponse.error(ResultCode.USER_NOT_EXIST);
    }

    @Override
    public User findByPhone(String phone) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
    }
}