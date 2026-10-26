package com.normal.picturebackend.shared.ws.disruptor;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 图片编辑事件队列配置
 *
 * @author: zhangocean
 * @Date: 2020/5/16 17:01
 */
@Configuration
@Slf4j
public class PictureEditEventDisruptorConfig {

    /**
     * 事件消费者处理
     */
    @Resource
    private PictureEditEventConsumer pictureEditEventConsumer;

    /**
     * 环形缓冲大小
     */
    public static final int RING_BUFFER_SIZE = 1024 * 256;

    @Bean("pictureEditDisruptor")
    public Disruptor<PictureEditEvent> messageModelRingBuffer() {

        // 设置事件队列线程池进行处理生产者的事件
        Disruptor<PictureEditEvent> disruptor = new Disruptor<PictureEditEvent>(
                PictureEditEvent::new,
                RING_BUFFER_SIZE,
                ThreadFactoryBuilder.create().setNamePrefix("pictureEditDisruptor").build()
        );

        // 设置消费者
        disruptor.handleEventsWithWorkerPool(pictureEditEventConsumer);

        // 开启disruptor
        disruptor.start();
        return disruptor;
    }






}
