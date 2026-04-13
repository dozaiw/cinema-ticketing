package com.cinema.common.util;

import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 通用Bean拷贝工具类
 * 支持：单对象（VO/Entity/DTO）拷贝、List集合拷贝
 */
public class BeanCopyUtil {

    /**
     * 私有构造方法，禁止实例化
     */
    private BeanCopyUtil() {
    }

    /**
     * 单个对象拷贝
     * @param source 源对象（如User实体）
     * @param targetClass 目标类（如UserVO.class）
     * @return 目标对象实例
     */
    public static <T, R> R copyBean(T source, Class<R> targetClass) {
        // 源对象为空，直接返回null
        if (Objects.isNull(source)) {
            return null;
        }
        // 目标对象实例化
        R target;
        try {
            target = targetClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Bean拷贝-实例化目标类失败", e);
        }
        // 拷贝属性（忽略null值，避免覆盖目标类默认值）
        BeanUtils.copyProperties(source, target);
        return target;
    }

    /**
     * List集合拷贝（基于单对象拷贝扩展）
     * @param sourceList 源List（如List<User>）
     * @param targetClass 目标类（如UserVO.class）
     * @return 目标List（如List<UserVO>）
     */
    public static <T, R> List<R> copyList(List<T> sourceList, Class<R> targetClass) {
        // 源List为空/空集合，返回空List（避免NPE）
        if (CollectionUtils.isEmpty(sourceList)) {
            return new ArrayList<>();
        }
        // 遍历源List，逐个拷贝并收集到目标List
        List<R> targetList = new ArrayList<>(sourceList.size());
        for (T source : sourceList) {
            R target = copyBean(source, targetClass);
            if (Objects.nonNull(target)) {
                targetList.add(target);
            }
        }
        return targetList;
    }
}