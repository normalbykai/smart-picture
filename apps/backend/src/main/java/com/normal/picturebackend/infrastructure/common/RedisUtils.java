package com.normal.picturebackend.infrastructure.common;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class RedisUtils {

    private static final String REDIS_KEY_PREFIX = "normal:picture:";

    /**
     * 永不过期 表示该key永不过期 (在一个key被标注为永不过期返回此值)
     */
    private static final Long NEVER_EXPIRE = -1L;


    /**
     * 表示系统没有这个缓存 （在对不存在的key获取的时间返回此值）
     */
    private static final Long NOT_KEY_EXPIRE = -2L;


    /**
     * ObjectMapper对象 方便二次开发
     */
    @Resource
    private ObjectMapper objectMapper;

    /**
     * object存储 string
     */
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * json存储
     */
    private RedisTemplate<String, Object> objectRedisTemplate;

    /**
     * 设置redisTemplate 格式为 json
     */
    @Resource
    public void setObjectRedisTemplate(RedisConnectionFactory redisConnectionFactory) {

        // 指定序列化方案
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

        // 通过反射获取 Mapper对象
        try{
            Field field = GenericJackson2JsonRedisSerializer.class.getDeclaredField("mapper");
            field.setAccessible(true);
            ObjectMapper objectMapper = (ObjectMapper) field.get(valueSerializer);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            this.objectMapper = objectMapper;
        } catch (Exception e) {
            System.err.println("获取mapper失败:" + e.getMessage());
        }

        // 构建ObjectRedisTemplate
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(keySerializer);
        redisTemplate.setValueSerializer(valueSerializer);
        redisTemplate.setHashKeySerializer(keySerializer);
        redisTemplate.setHashValueSerializer(valueSerializer);
        redisTemplate.afterPropertiesSet();
        if (this.objectRedisTemplate == null) {
            this.objectRedisTemplate = redisTemplate;
        }
    }

    /**
     * 通过key获取value
     */
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 设置key-value 并设置指定过期时间/秒
     */
    public void set(String key, String value, long timeoutSecond) {
        if (timeoutSecond == NEVER_EXPIRE) {
            stringRedisTemplate.opsForValue().set(key, value);
        }else {
            stringRedisTemplate.opsForValue().set(key, value, timeoutSecond, java.util.concurrent.TimeUnit.SECONDS);
        }
    }


    /**
     * 通过 key 获取过期时间
     * @param key key
     * @return 过期时间 永不过期为:(-1L) 没有则返回:(-2L)
     */
    public long getExpireTime(String key) {
        return stringRedisTemplate.getExpire(key);
    }

    /**
     * 更新指定key的value值 过期时间不变
     */
    public void update(String key, String value) {
        long expireTime = getExpireTime(key);
        if (expireTime == NOT_KEY_EXPIRE) {
            // 不存在该键 不做任何操作
            return;
        }else {
            this.set(key, value, expireTime);
        }
    }

    /**
     * 删除一个指定的key
     */
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 删除一个指定Key路径下的所有缓存
     */
    public void deleteByKeyPrefix(String keyPrefix) {
       stringRedisTemplate.keys(keyPrefix + "*").forEach(el -> stringRedisTemplate.delete(el));
    }


    /**
     * 根据key获取object 没有则返回空
     */
    public Object getObject(String key) {
        return objectRedisTemplate.opsForValue().get(key);
    }

    /**
     * 设置一个键值对, 并设置过期时间/秒
     */
    public void setObject(String key, Object object, long timeout) {
        if (timeout == NEVER_EXPIRE) {
            // 永不过期
        }else {
            objectRedisTemplate.opsForValue().set(key, object, timeout, java.util.concurrent.TimeUnit.SECONDS);
        }
    }







}
