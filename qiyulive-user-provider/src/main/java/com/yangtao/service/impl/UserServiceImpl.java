package com.yangtao.service.impl;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Maps;
import com.yangtao.dao.mapper.UserMapper;
import com.yangtao.dao.po.UserPO;
import com.yangtao.dto.UserDTO;
import com.yangtao.framework.redis.key.UserProviderCacheKeyBuilder;
import com.yangtao.interfaces.ConvertBeanUtils;
import com.yangtao.service.IUserService;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements IUserService {

    private Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    @Resource
    private UserMapper userMapper;
    @Resource
    private RedisTemplate<String, UserDTO> redisTemplate;
    @Resource
    private UserProviderCacheKeyBuilder keyBuilder;
    @Autowired
    private MQProducer mqProducer;

    @Override
    public UserDTO getUserById(Long userId) {
        if (userId == null) {
            return new UserDTO();
        }
        String key = keyBuilder.buildUserInfoKey(userId);
        UserDTO dto = redisTemplate.opsForValue().get(key);
        if (dto != null) {
            return dto;
        }

        UserDTO userDTO = ConvertBeanUtils.convert(userMapper.selectById(userId), UserDTO.class);
        if (userDTO != null) {
            // 写入缓存当中 并且设置 过期时间
            redisTemplate.opsForValue().set(key, userDTO, 30, TimeUnit.MINUTES);
        }

        return userDTO;
    }

    @Override
    public Boolean updateById(UserDTO userDTO) {
        // 更新成功需要删除缓存，并且发送消息
        if (userDTO == null || userDTO.getUserId() == null) {
            return Boolean.FALSE;
        }

        userMapper.updateById(ConvertBeanUtils.convert(userDTO, UserPO.class));
        String userInfoKey = keyBuilder.buildUserInfoKey(userDTO.getUserId());
        // 删除缓存
        redisTemplate.delete(userInfoKey);

        // 发送延迟消息，进行延迟删除
        try {
            Message message = new Message();
            message.setBody(JSON.toJSONBytes(userDTO));
            message.setTopic("user-update-cache");
            // 设置消息的延迟级别，延迟1s
            message.setDelayTimeLevel(1);
            mqProducer.send(message);
        } catch (Exception e) {
            LOGGER.error("发送删除消息失败，userDTO: {}, {}", userDTO, e);
        }


        return  Boolean.TRUE;
    }

    @Override
    public Map<Long, UserDTO> getUserByIdList(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Maps.newHashMap();
        }

        // 1 multi-get from redis
        userIds = userIds.stream().filter(id -> id > 1000).collect(Collectors.toList());
        List<String> keyList = userIds.stream().map(id -> keyBuilder.buildUserInfoKey(id)).collect(Collectors.toList());

        List<UserDTO> userDTOSCache = redisTemplate.opsForValue().multiGet(keyList).stream().filter(x -> x != null).collect(Collectors.toList());
        if (!CollectionUtils.isNotEmpty(userDTOSCache) && userDTOSCache.size() == userIds.size()) {
            return userDTOSCache.stream().collect(Collectors.toMap(UserDTO::getUserId, Function.identity()));
        }

        List<Long> userIdsInCache = userDTOSCache.stream().map(UserDTO::getUserId).collect(Collectors.toList());
        List<Long> userIdsNotInCache = userIds.stream().filter(id -> !userIdsInCache.contains(id)).collect(Collectors.toList());
        // 2 cache hit，return； cache miss， query from db
        List<UserDTO> doQueryResult = new CopyOnWriteArrayList<>();

        Map<Long, List<Long>> paritionIdMap = userIdsNotInCache.stream().collect(Collectors.groupingBy(userId -> userId % 100));
        paritionIdMap.values().stream()
                .parallel()
                .forEach(ids -> {
                    List<UserPO> userPOList = userMapper.selectByIdList(ids);
                    doQueryResult.addAll(userPOList.stream().map(po -> ConvertBeanUtils.convert(po, UserDTO.class)).collect(Collectors.toList()));
                });
        // 3 write to cache again
        if (CollectionUtils.isNotEmpty(doQueryResult)) {
            // write to redis again
            Map<String, UserDTO> savedMap = doQueryResult.stream().collect(Collectors.toMap(dto -> keyBuilder.buildUserInfoKey(dto.getUserId()), Function.identity()));
            redisTemplate.opsForValue().multiSet(savedMap);
            // 批量设置过期时间
            redisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                    for (String redisKey : savedMap.keySet()) {
                        operations.expire((K) redisKey, createRandomTime(), TimeUnit.SECONDS);
                    }
                    return null;
                }
            });

            userDTOSCache.addAll(doQueryResult);
        }

        return userDTOSCache.stream().collect(Collectors.toMap(UserDTO::getUserId, Function.identity()));
    }

    private int createRandomTime() {
        int randomTime = ThreadLocalRandom.current().nextInt(10000);
        return randomTime + 30 * 60;
    }

}
