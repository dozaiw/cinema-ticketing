package com.cinema.ticketing.services.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cinema.auth.util.CosUtil;
import com.cinema.auth.util.UserContextUtil;
import com.cinema.common.entity.BaseResponse;
import com.cinema.common.entity.ResultCode;
import com.cinema.ticketing.client.SandBoxAccountFeignClient;
import com.cinema.ticketing.client.SeatScheduleFeignClient;
import com.cinema.ticketing.dto.HallSalesStats;
import com.cinema.ticketing.dto.MovieSalesStats;
import com.cinema.ticketing.dto.OrderStatusStats;
import com.cinema.ticketing.entity.*;
import com.cinema.ticketing.mapper.OrderMapper;
import com.cinema.ticketing.services.OrderService;
import com.cinema.ticketing.services.SeatScheduleService;
import com.cinema.ticketing.util.OrderNoGenerator;
import com.cinema.ticketing.util.QrCodeGenerator;
import com.cinema.ticketing.util.QrCodeUtil;
import com.cinema.ticketing.vo.ScheduleVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference; // ✅ 修复TypeReference
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    // ========== 常量配置 ==========
    private static final DateTimeFormatter SHOW_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String ORDER_STATUS_PENDING = "PENDING"; // 仅生成订单，待支付状态
    private static final String ORDER_STATUS_TEXT_PENDING = "待支付";
    private static final int ORDER_EXPIRE_MINUTES = 15; // 订单过期时间（分钟）
    private static final String ORDER_STATUS_PAID = "PAID";
    private static final String ORDER_STATUS_USED = "USED";
    private static final String ORDER_STATUS_CANCELED = "CANCELED";
    private static final String ORDER_STATUS_EXPIRED = "EXPIRED";
    private static final String ORDER_STATUS_REFUND = "REFUND";

    // ========== 移除不需要的依赖（支付/二维码） ==========
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderNoGenerator orderNoGenerator;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private SeatScheduleFeignClient seatScheduleFeignClient;
    @Autowired
    private UserContextUtil userContextUtil;
    @Autowired
    private QrCodeGenerator qrCodeGenerator;
    @Autowired
    private QrCodeUtil qrCodeUtil;
    @Autowired
    private SeatScheduleServiceImpl seatScheduleServiceImpl;
    @Autowired
    private CosUtil cosUtil;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private SandBoxAccountFeignClient sandBoxAccountFeignClient;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public BaseResponse<OrderResponse> createOrder(CreateOrderRequest request) {
        // ========== 1. 入参全量判空（新增price校验） ==========
        if (request == null) {
            log.error("创建订单失败：请求参数为空");
            return BaseResponse.error(400, "请求参数不能为空");
        }
        if (request.getUserId() == null) {
            log.error("创建订单失败：用户ID为空");
            return BaseResponse.error(400, "用户ID不能为空");
        }
        if (request.getScheduleId() == null) {
            log.error("创建订单失败：场次ID为空");
            return BaseResponse.error(400, "场次ID不能为空");
        }
        if (CollectionUtils.isEmpty(request.getSeatIds())) {
            log.error("创建订单失败：座位ID列表为空");
            return BaseResponse.error(400, "座位ID列表不能为空");
        }
        if (!StringUtils.hasText(request.getShowTime())) {
            log.error("创建订单失败：放映时间为空");
            return BaseResponse.error(400, "放映时间不能为空");
        }
        // 新增price非空校验（解决空指针）
        if (request.getPrice() == null) {
            log.error("创建订单失败：订单价格为空");
            return BaseResponse.error(400, "订单价格不能为空");
        }

        try {
            // ========== 2. 座位合法性验证 ==========
            ValidSeatDto validSeatDto = new ValidSeatDto();
            validSeatDto.setSeatIds(request.getSeatIds());
            validSeatDto.setUserId(request.getUserId());
            validSeatDto.setScheduleId(request.getScheduleId());

            Boolean seatValid;
            try {
                seatValid = seatScheduleFeignClient.valid(validSeatDto);
            } catch (FeignException e) {
                log.error("创建订单失败：座位服务调用异常，status={}, message={}", e.status(), e.getMessage());
                return BaseResponse.error(503, "座位服务暂时不可用，请稍后重试");
            } catch (Exception e) {
                log.error("创建订单失败：座位验证异常", e);
                return BaseResponse.error(400, "座位验证异常：" + e.getMessage());
            }

            if (!Boolean.TRUE.equals(seatValid)) {
                log.error("创建订单失败：座位验证不通过，userId={}, scheduleId={}, seatIds={}",
                        request.getUserId(), request.getScheduleId(), request.getSeatIds());
                return BaseResponse.error(400, "座位验证失败：可能是座位未预占、预占用户不匹配或座位已售出");
            }

            // ========== 3. 生成唯一订单号（防重复） ==========
            String orderNo;
            int retryCount = 3;
            do {
                orderNo = orderNoGenerator.generateOrderNo();
                Long count = orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
                if (count == 0) break;
                log.warn("订单号{}已存在，重试生成", orderNo);
                retryCount--;
            } while (retryCount > 0);
            if (retryCount <= 0) {
                log.error("创建订单失败：多次生成订单号均重复");
                return BaseResponse.error(500, "订单号生成失败，请稍后重试");
            }
            log.info("生成订单号: {}", orderNo);

            // ========== 4. 计算订单金额 ==========
            Integer amount = request.getPrice() * 100 * request.getSeatIds().size(); // 元转分
            log.info("计算订单金额: {}分（{}元）", amount, amount / 100.0);

            // ========== 5. 解析放映时间 ==========
            LocalDateTime showTime;
            try {
                showTime = LocalDateTime.parse(request.getShowTime(), SHOW_TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                log.error("创建订单失败：放映时间格式错误，time={}", request.getShowTime(), e);
                return BaseResponse.error(400, "放映时间格式错误，需为yyyy-MM-dd HH:mm:ss");
            }

            // ========== 6. 核心：通过 Feign 获取 seatNames（传入 seatIds） ==========
            List<String> seatNames;
            try {
                seatNames = seatScheduleFeignClient.getSeatName(request.getSeatIds());
                // 校验返回的座位名称非空
                if (CollectionUtils.isEmpty(seatNames) || seatNames.size() != request.getSeatIds().size()) {
                    log.error("创建订单失败：座位名称获取异常，seatIds={}, 返回 seatNames={}", request.getSeatIds(), seatNames);
                    return BaseResponse.error(400, "座位名称获取失败");
                }
                log.info("通过 Feign 获取座位名称：{}", seatNames);
            } catch (Exception e) {
                log.error("创建订单失败：获取座位名称异常", e);
                return BaseResponse.error(503, "获取座位名称失败，请稍后重试");
            }

            // 通过 Feign 获取影院名称和影厅名称
            ScheduleVO scheduleVO = seatScheduleFeignClient.getCinemaNameAndHallNameByScheduleId(request.getScheduleId());
            String cinemaName = scheduleVO.getCinemaName();
            String hallName = scheduleVO.getHallName();
            
            // ========== 7. 构建订单实体 ==========
            Order order = new Order();
            order.setOrderNo(orderNo);
            order.setUserId(request.getUserId().longValue());
            order.setScheduleId(request.getScheduleId().longValue());
            order.setHallName(hallName);
            order.setMovieName(request.getMovieName());
            order.setShowTime(showTime);
            order.setSeatIds(convertToJson(request.getSeatIds())); // 座位 ID 转 JSON
            order.setSeatNames(convertToJson(seatNames)); // Feign 获取的座位名称转 JSON
            
            order.setTotalAmount(amount);
            order.setStatus(ORDER_STATUS_PENDING); // 仅生成订单，待支付状态
            order.setUserPhone(maskPhone(request.getUserPhone()));
            order.setExpireTime(LocalDateTime.now().plusMinutes(ORDER_EXPIRE_MINUTES)); // 15分钟过期
            order.setCreateTime(LocalDateTime.now());

            // ========== 8. 保存订单到数据库 ==========
            int insertResult = orderMapper.insert(order);
            if (insertResult <= 0) {
                log.error("创建订单失败：订单保存到数据库失败，orderNo={}", orderNo);
                throw new RuntimeException("订单保存失败"); // 触发事务回滚
            }
            log.info("订单生成成功（仅创建，未支付）: orderId={}, orderNo={}", order.getId(), orderNo);

            // 8.1修改数据库场次位置消息为售出
            seatScheduleFeignClient.changeSeatStatus(request.getSeatIds(),1
                    ,request.getScheduleId(), Math.toIntExact(order.getId()));

            // ========== 9. 构建返回结果（仅订单基础信息，无支付/二维码） ==========
            OrderResponse response = OrderResponse.builder()
                    .orderId(order.getId())
                    .orderNo(orderNo)
                    .scheduleId(request.getScheduleId().longValue())
                    .hallName(hallName)
                    .cinemaName(cinemaName)
                    .movieName(request.getMovieName())
                    .showTime(order.getShowTime())
                    .seatIds(request.getSeatIds().stream().map(Integer::longValue).collect(Collectors.toList()))
                    .seatNames(seatNames) // Feign 获取的座位名称
                    .userId(request.getUserId().longValue())
                    .amount(amount)
                    .status(ORDER_STATUS_PENDING)
                    .statusText(ORDER_STATUS_TEXT_PENDING)
                    .expireTime(order.getExpireTime())
                    .createTime(order.getCreateTime())
                    .userPhone(maskPhone(request.getUserPhone()))
                    .build();

            return BaseResponse.success(response);

        } catch (RuntimeException e) {
            log.error("创建订单失败：业务异常", e);
            return BaseResponse.error(400, "创建订单失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("创建订单失败：系统异常", e);
            return BaseResponse.error(ResultCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse payOrder(Integer orderId,String passWord) {
        Integer userId = userContextUtil.getUserId();

        // ========== 1. 验证订单 ==========
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getId, orderId));

        if (order == null) {
          throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId.longValue())) {
          throw new RuntimeException("无权限支付该订单");
        }
        if (!ORDER_STATUS_PENDING.equals(order.getStatus())) {
          throw new RuntimeException("订单不可支付，当前状态：" + order.getStatus());
        }

        log.info("订单验证通过：orderId={}, userId={}", orderId, userId);

        // ========== 2. 生成唯一验票码 ==========
        String verifyCode = generateVerifyCode(orderId, userId);
        log.info("生成验票码：{}", verifyCode);

        // ========== 3. 更新订单状态 ==========
        order.setStatus(ORDER_STATUS_PAID);
        order.setPayTime(LocalDateTime.now());
        order.setExpireTime(null); // 永久有效
        order.setVerifyCode(verifyCode);
        orderMapper.updateById(order);
        log.info("订单状态更新成功：orderId={}", orderId);

        // ========== 4. 生成二维码内容 ==========
        String qrContent= qrCodeGenerator.generateQrCodeContent(order);
        log.info("二维码内容生成：{}", qrContent);

        // ========== 5. 内存生成二维码 + 直传 COS（无本地文件） ==========
        byte[] qrCodeBytes = qrCodeUtil.generateQrCodeBytes(qrContent, order.getOrderNo());
        String qrCodeFileName = "qr_" + order.getOrderNo() + ".png";
        String qrCodeUrl = cosUtil.uploadFileFromBytes(qrCodeBytes, qrCodeFileName, "qrCode");

        // 更新订单二维码 URL
        order.setQrCodeUrl(qrCodeUrl);
        orderMapper.updateById(order);
        log.info("二维码直传 COS 成功：{}", qrCodeUrl);

        // 更新订单二维码路径
        order.setQrCodeUrl(qrCodeUrl);
        orderMapper.updateById(order);
        log.info(" 二维码生成成功：{}", qrCodeUrl);

        // 账户扣款（将分转换为元）
        Double amountInYuan = order.getTotalAmount() / 100.0;
        log.info("开始扣款：orderId={}, 金额={}元（原始{}分）", orderId, amountInYuan, order.getTotalAmount());
        
       BaseResponse baseResponse = sandBoxAccountFeignClient.deductBalance(amountInYuan, passWord);
        if (baseResponse.getCode() != 200) {
            log.error("支付失败：账户扣款失败，原因={}", baseResponse.getMsg());
           // 直接抛出 RuntimeException，Controller 会捕获并返回 200
          throw new RuntimeException(baseResponse.getMsg());
        }
        
        // ========== 6. 返回结果 ==========
        Map<String, Object> result = new HashMap<>();
     result.put("orderId", orderId);
     result.put("orderNo", order.getOrderNo());
     result.put("verifyCode", verifyCode);
     result.put("qrCodeUrl", qrCodeUrl);
     result.put("qrCodeContent", qrContent);
     result.put("payTime", order.getPayTime());
     result.put("movieName", order.getMovieName());
     result.put("hallName", order.getHallName());
     result.put("showTime", order.getShowTime());
     result.put("seatInfo", order.getSeatNames());
     result.put("totalAmount", order.getTotalAmount());
     
     // 通过 Feign 获取影院名称
     try {
         ScheduleVO scheduleVO = seatScheduleFeignClient.getCinemaNameAndHallNameByScheduleId(order.getScheduleId().intValue());
         result.put("cinemaName", scheduleVO.getCinemaName());
     } catch (Exception e) {
         log.error("获取影院名称失败：scheduleId={}", order.getScheduleId(), e);
     }

        log.info("支付成功：orderId={}, orderNo={}", orderId, order.getOrderNo());
        //删除 redis 缓存
        stringRedisTemplate.delete("seat:schedule:" + order.getScheduleId());
        //重新加载缓存
        seatScheduleFeignClient.querySeatCondition(Math.toIntExact(order.getScheduleId()));
     return BaseResponse.success(result);
    }

    /**
     * 生成唯一验票码（防伪造）
     */
    private String generateVerifyCode(Integer orderId, Integer userId) {
        // 格式: TICKET_12345_1001_1739456789012_a3f9b2
        String secretKey = "cinema_secret_2026"; // 配置文件中管理
        String raw = orderId + "_" + userId + "_" + System.currentTimeMillis() + "_" + secretKey;
        String hash = DigestUtils.md5Hex(raw).substring(0, 6).toUpperCase();
        return "TICKET_" + orderId + "_" + userId + "_" + System.currentTimeMillis() + "_" + hash;
    }

    /**
     * 验票接口
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse verifyTicket(String verifyCode) {
        // 1. 根据验票码查询订单
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getVerifyCode, verifyCode)
        );

        if (order == null) {
            return BaseResponse.error(404, "无效验票码");
        }
        if (!ORDER_STATUS_PAID.equals(order.getStatus())) {
            return BaseResponse.error(400, "订单状态异常，当前状态：" + order.getStatus());
        }

        // 2. 标记为已使用（更新状态为 USED）
        order.setStatus(ORDER_STATUS_USED);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 3. 返回票务信息
        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("movieName", order.getMovieName());
        result.put("hallName", order.getHallName());
        result.put("showTime", order.getShowTime());
        result.put("seatInfo", order.getSeatNames());
        
        // 通过 Feign 获取影院名称
        try {
            ScheduleVO scheduleVO = seatScheduleFeignClient.getCinemaNameAndHallNameByScheduleId(order.getScheduleId().intValue());
            result.put("cinemaName", scheduleVO.getCinemaName());
        } catch (Exception e) {
            log.error("获取影院名称失败：scheduleId={}", order.getScheduleId(), e);
        }

        log.info("验票成功：verifyCode={}", verifyCode);
        return BaseResponse.success(result);
    }

    @Override
    @Transactional
    public BaseResponse cancelOrder(Integer orderId) {
        Integer userId = userContextUtil.getUserId();
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getUserId, userId));
        if (order == null) {
            return BaseResponse.error(400, "无此订单");
        }
        if(!Objects.equals(order.getStatus(), ORDER_STATUS_PENDING)) {
            return BaseResponse.error(400, "订单不可取消，当前状态: " + order.getStatus());
        }
        releaseSeatsAndRefreshCache(order);
        //修改订单状态
        order.setStatus(ORDER_STATUS_CANCELED);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return BaseResponse.success();
    }

    @Override
    public BaseResponse queryAllOrder() {
        try{
            Integer userId = userContextUtil.getUserId();
            List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>().eq(Order::getUserId, userId));
            return BaseResponse.success(orders);
        }
        catch (Exception e) {
            log.error("查询所有订单失败", e);
            return BaseResponse.error(ResultCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public BaseResponse queryTodayOrderCount(String dateStr) {
        try {
            // 解析日期字符串（格式：yyyy-MM-dd）
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);  // 严格模式，不允许非法日期

            Date startDate = sdf.parse(dateStr);  // 当天 00:00:00

            //  计算当天结束时间（次日 00:00:00）
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            Date endDate = calendar.getTime();

            //  范围查询（>= 当天 00:00:00 AND < 次日 00:00:00）
            Long count = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                    .ge(Order::getCreateTime, startDate)
                    .lt(Order::getCreateTime, endDate));

            return BaseResponse.success(count);
        } catch (ParseException e) {
            log.error("日期格式错误：{}", dateStr, e);
            return BaseResponse.error(403, "日期格式错误，请使用 yyyy-MM-dd 格式");
        } catch (Exception e) {
            log.error("查询今日订单数量失败", e);
            return BaseResponse.error(ResultCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public BaseResponse queryUserOrders(Integer userId) {
        try {
            List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>().eq(Order::getUserId, userId));
            return BaseResponse.success(orders);
        } catch (Exception e) {
            log.error("查询用户订单失败", e);
            return BaseResponse.error(ResultCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public BaseResponse getOrderDetail(Integer orderId, Integer userId) {
        try {
            Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                    .eq(Order::getId, orderId)
                    .eq(Order::getUserId, userId));
            if (order ==  null) {
                return BaseResponse.error(400, "无此订单");
            }
            
            // 通过 Feign 获取影院名称并添加到返回数据
            try {
                ScheduleVO scheduleVO = seatScheduleFeignClient.getCinemaNameAndHallNameByScheduleId(order.getScheduleId().intValue());
                // 将影院名称设置到 remark 字段返回
                order.setRemark(scheduleVO.getCinemaName());
            } catch (Exception e) {
                log.error("获取影院名称失败：scheduleId={}", order.getScheduleId(), e);
            }
            
            return BaseResponse.success(order);
        }
        catch (Exception e) {
            log.error("查询订单详情失败", e);
            return BaseResponse.error(500,"订单查询失败");
        }
    }

    /**
     * 解析座位ID列表
     */
    private List<Integer> parseSeatIds(String seatIdsJson) {
        if (seatIdsJson == null || seatIdsJson.isEmpty() || "[]".equals(seatIdsJson)) {
            return null;
        }
        try {
            return objectMapper.readValue(seatIdsJson, new TypeReference<List<Integer>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析座位ID失败: {}", seatIdsJson, e);
            return null;
        }
    }

    // ==================== 工具方法 ====================
    /**
     * 手机号脱敏
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 列表转JSON（空列表返回[]）
     */
    private String convertToJson(List<?> list) throws JsonProcessingException {
        if (CollectionUtils.isEmpty(list)) {
            return "[]";
        }
        return objectMapper.writeValueAsString(list);
    }

    //管理员查询所有订单
    @Override
    public BaseResponse adminQueryAllOrder() {
        return BaseResponse.success(orderMapper.selectList(null));
    }


    @Override
    public BaseResponse refundOrder(Integer orderId , Integer userId) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getId, orderId).eq(Order::getUserId, userId));
        if (order == null) {
            return BaseResponse.error(404, "无此订单");
        }
        if (!Objects.equals(order.getStatus(), ORDER_STATUS_PAID)) {
            return BaseResponse.error(400, "订单状态错误");
        }

        try {
            Double amountInYuan = order.getTotalAmount() / 100.0;
            BaseResponse baseResponse = sandBoxAccountFeignClient.refundBalance(amountInYuan);
            if (baseResponse == null || baseResponse.getCode() != 200) {
                String errorMsg = baseResponse != null ? baseResponse.getMsg() : "退款服务异常";
                log.error("订单退款失败，orderId={}, 退款金额：{}元，原因：{}", orderId, amountInYuan, errorMsg);
                return BaseResponse.error(500, errorMsg);
            }

            releaseSeatsAndRefreshCache(order);

            order.setStatus(ORDER_STATUS_REFUND);
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);

            log.info("订单退款成功，orderId={}, 退款金额：{}元", orderId, amountInYuan);
            return BaseResponse.success("订单退款成功");
        } catch (RuntimeException e) {
            log.error("订单退款失败，orderId={}", orderId, e);
            return BaseResponse.error(500, e.getMessage());
        } catch (Exception e) {
            log.error("订单退款失败，orderId={}", orderId, e);
            return BaseResponse.error(500, "订单退款失败");
        }
    }

    @Override
    public BaseResponse getMovieSalesStats(String dateStr) {
        try {
            // 解析日期
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            Date startDate = sdf.parse(dateStr);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            Date endDate = calendar.getTime();

            // 查询当日所有订单
            List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                    .ge(Order::getCreateTime, startDate)
                    .lt(Order::getCreateTime, endDate));

            if (CollectionUtils.isEmpty(orders)) {
                return BaseResponse.success(new ArrayList<>());
            }

            // 按电影名称分组统计
            Map<String, List<Order>> movieOrdersMap = orders.stream()
                    .filter(this::isSalesOrder)
                    .collect(Collectors.groupingBy(Order::getMovieName));

            // 计算总销售额
            Long totalSales = movieOrdersMap.values().stream()
                    .flatMap(List::stream)
                    .mapToLong(Order::getTotalAmount)
                    .sum();

            // 构建统计结果
            List<MovieSalesStats> statsList = movieOrdersMap.entrySet().stream()
                    .map(entry -> {
                        String movieName = entry.getKey();
                        List<Order> orderList = entry.getValue();
                        Long amount = orderList.stream()
                                .mapToLong(Order::getTotalAmount)
                                .sum();
                        Long count = (long) orderList.size();
                        Double percentage = totalSales > 0 ? (double) Math.round(amount * 10000 / totalSales) / 100.0 : 0.0;
                        return new MovieSalesStats(movieName, amount, count, percentage);
                    })
                    .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                    .collect(Collectors.toList());

            return BaseResponse.success(statsList);
        } catch (ParseException e) {
            log.error("日期格式错误：{}", dateStr, e);
            return BaseResponse.error(400, "日期格式错误，请使用 yyyy-MM-dd 格式");
        } catch (Exception e) {
            log.error("获取电影销售统计失败", e);
            return BaseResponse.error(ResultCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public BaseResponse getHallSalesStats(String dateStr) {
        try {
            // 解析日期
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            Date startDate = sdf.parse(dateStr);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            Date endDate = calendar.getTime();

            // 使用 XML 方式直接连表查询影院销售统计
            // 查询路径：cinema_order -> seat_schedule -> hall -> cinema
            List<HallSalesStats> statsList = orderMapper.selectCinemaSalesStats(startDate, endDate);

            if (CollectionUtils.isEmpty(statsList)) {
                return BaseResponse.success(new ArrayList<>());
            }

            // 计算总销售额
            Long totalSales = statsList.stream()
                    .mapToLong(HallSalesStats::getTotalAmount)
                    .sum();

            // 计算每个影院的占比
            statsList.forEach(stats -> {
                Double percentage = totalSales > 0 ? 
                    (double) Math.round(stats.getTotalAmount() * 10000 / totalSales) / 100.0 : 0.0;
                stats.setPercentage(percentage);
                // hallName 设为 null 或空，因为这是影院级别的统计
                stats.setHallName(null);
            });

            return BaseResponse.success(statsList);
        } catch (ParseException e) {
            log.error("日期格式错误：{}", dateStr, e);
            return BaseResponse.error(400, "日期格式错误，请使用 yyyy-MM-dd 格式");
        } catch (Exception e) {
            log.error("获取影院销售统计失败", e);
            return BaseResponse.error(ResultCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public BaseResponse getOrderStatusStats(String dateStr) {
        try {
            // 解析日期
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            Date startDate = sdf.parse(dateStr);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            Date endDate = calendar.getTime();

            // 查询当日所有订单（包括所有状态）
            List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                    .ge(Order::getCreateTime, startDate)
                    .lt(Order::getCreateTime, endDate));

            if (CollectionUtils.isEmpty(orders)) {
                return BaseResponse.success(new ArrayList<>());
            }

            // 按订单状态分组统计
            Map<String, Long> statusCountMap = orders.stream()
                    .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

            // 总订单数
            Long totalCount = (long) orders.size();

            // 状态文本映射
            Map<String, String> statusTextMap = new HashMap<>();
            statusTextMap.put(ORDER_STATUS_PENDING, "待支付");
            statusTextMap.put(ORDER_STATUS_PAID, "已支付");
            statusTextMap.put(ORDER_STATUS_USED, "已使用");
            statusTextMap.put(ORDER_STATUS_CANCELED, "已取消");
            statusTextMap.put(ORDER_STATUS_EXPIRED, "已过期");
            statusTextMap.put(ORDER_STATUS_REFUND, "已退款");

            // 构建统计结果
            List<OrderStatusStats> statsList = statusCountMap.entrySet().stream()
                    .map(entry -> {
                        String status = entry.getKey();
                        Long count = entry.getValue();
                        String statusText = statusTextMap.getOrDefault(status, status);
                        Double percentage = totalCount > 0 ? (double) Math.round(count * 10000 / totalCount) / 100.0 : 0.0;
                        return new OrderStatusStats(status, statusText, count, percentage);
                    })
                    .sorted((a, b) -> b.getOrderCount().compareTo(a.getOrderCount()))
                    .collect(Collectors.toList());

            return BaseResponse.success(statsList);
        } catch (ParseException e) {
            log.error("日期格式错误：{}", dateStr, e);
            return BaseResponse.error(400, "日期格式错误，请使用 yyyy-MM-dd 格式");
        } catch (Exception e) {
            log.error("获取订单状态统计失败", e);
            return BaseResponse.error(ResultCode.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isSalesOrder(Order order) {
        return ORDER_STATUS_PAID.equals(order.getStatus()) || ORDER_STATUS_USED.equals(order.getStatus());
    }

    private void releaseSeatsAndRefreshCache(Order order) {
        List<Integer> seatIds = parseSeatIds(order.getSeatIds());
        if (CollectionUtils.isEmpty(seatIds)) {
            throw new RuntimeException("订单座位信息异常");
        }

        BaseResponse releaseResponse = seatScheduleServiceImpl.initSeatSchedule(
                seatIds,
                0,
                Math.toIntExact(order.getScheduleId()),
                Math.toIntExact(order.getId())
        );
        if (releaseResponse == null || releaseResponse.getCode() != 200) {
            String errorMsg = releaseResponse != null ? releaseResponse.getMsg() : "座位释放失败";
            throw new RuntimeException(errorMsg);
        }

        try {
            stringRedisTemplate.delete("seat:schedule:" + order.getScheduleId());
            seatScheduleFeignClient.querySeatCondition(Math.toIntExact(order.getScheduleId()));
        } catch (Exception e) {
            log.warn("刷新座位缓存失败，scheduleId={}", order.getScheduleId(), e);
        }
    }
}
