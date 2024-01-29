package com.yangtao.id.generate.provider.service.impl;

import com.yangtao.id.generate.provider.dao.mapper.IdBuilderMapper;
import com.yangtao.id.generate.provider.dao.po.IdBuilderPO;
import com.yangtao.id.generate.provider.service.IdBuilderService;
import com.yangtao.id.generate.provider.service.bo.LocalSeqIdBO;
import com.yangtao.id.generate.provider.service.bo.LocalUnSeqIdBO;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class IdBuilderServiceImpl implements IdBuilderService, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdBuilderServiceImpl.class);

    @Resource
    private IdBuilderMapper idBuilderMapper;
    // 内存中的map，存放 db id 和 区间段的对应关系
    // LocalSeqIdBO 区间段对象，代表一个可用区间
    private final Map<Integer, LocalSeqIdBO> localSeqIdBOMap = new ConcurrentHashMap<>();
    private final Map<Integer, LocalUnSeqIdBO> localUnSeqIdBOMap = new ConcurrentHashMap<>();
    private final Map<Integer, Semaphore> semaphoreMap = new ConcurrentHashMap<>();
    private static ThreadPoolExecutor ayncExecutor = new ThreadPoolExecutor(8,
            16,
            3,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("id-generate-thread-" + ThreadLocalRandom.current().nextInt(1000));
                    return thread;
                }
            });
    private static final double UPDATE_RATE = 0.75;
    private static final int IS_SEQ = 1;

    @Override
    public Long increaseSeqId(Integer code) {
        if (code == null) {
            LOGGER.error("[getSeqlId] code is error, code is {}", code);
            return null;
        }
        LocalSeqIdBO localSeqIdBO = localSeqIdBOMap.get(code);
        if (localSeqIdBO == null) {
            LOGGER.error("[getSeqlId] localSeqIdBO is error, localSeqIdBO is {}", localSeqIdBO);
            return null;
        }
        refreshLocalSeqId(localSeqIdBO);
        long id = localSeqIdBO.getCurrentValue().incrementAndGet();

        if (id > localSeqIdBO.getNextThreshold()) {
            LOGGER.error("[getSeqId] is error, {} is over limit", id);
            // 快速失败 而不是同步刷新，同步刷新可能会打爆dubbo 的线程池
            return null;
        }
        return id;
    }

    @Override
    public Long increaseUnSeqId(Integer code) {
        if (code == null) {
            LOGGER.error("[getUnSeqlId] code is error, code is {}", code);
            return null;
        }
        LocalUnSeqIdBO localUnSeqIdBO = localUnSeqIdBOMap.get(code);
        if (localUnSeqIdBO == null) {
            LOGGER.error("[getUnSeqId] error, localUnSeqIdBo is {}", localUnSeqIdBO);
            return null;
        }
        Long returnId = localUnSeqIdBO.getIdQueue().poll();
        if (returnId == null) {
            LOGGER.error("[getUnSeqid] error, returnId is {}", returnId);
            return null;
        }
        this.refreshLocalUnSeqId(localUnSeqIdBO);
        return returnId;
    }

    @Override
    public String increaseSeqStrId(Integer code) {
        return null;
    }

    /**
     * 不能等到 达到阈值之后 再去更新区间段，这样会导致可能有大量的线程在等待IO 操作完成，
     * 可能会导致 业务雪崩
     * @param localSeqIdBO
     */
    private void refreshLocalSeqId(LocalSeqIdBO localSeqIdBO) {
        if (localSeqIdBO.getCurrentValue().get() - localSeqIdBO.getCurrentStart() > localSeqIdBO.getStep() * UPDATE_RATE) {
            Semaphore semaphore = semaphoreMap.get(localSeqIdBO.getId());
            if (semaphore == null) {
                LOGGER.error("semaphore is null, id is {}", localSeqIdBO.getId());
                return;
            }
            boolean acquire = semaphore.tryAcquire();
            if (acquire) {
                ayncExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        LOGGER.info("开始尝试进行本地id段的同步操作");
                        // 异步执行 IO逻辑
                        try {
                            IdBuilderPO idBuilderPO = idBuilderMapper.selectById(localSeqIdBO.getId());
                            tryUpdateMySQLRecord(idBuilderPO);
                        } catch (Exception e) {
                            LOGGER.error("[refreshLocalSeqId] error is {}", e);
                        } finally {
                            semaphoreMap.get(localSeqIdBO.getId()).release();
                            LOGGER.info("本地有序id段同步完成,id is {}", localSeqIdBO.getId());
                        }
                    }
                });
            }
        }
    }

    private void refreshLocalUnSeqId(LocalUnSeqIdBO localUnSeqIdBO) {
        long remainSize = localUnSeqIdBO.getIdQueue().size();
        long start = localUnSeqIdBO.getCurrentStart();
        long end = localUnSeqIdBO.getNextThreshold();
        if (remainSize < (1 - UPDATE_RATE) * (end - start)) {
            Semaphore semaphore = semaphoreMap.get(localUnSeqIdBO.getId());
            if (semaphore == null) {
                LOGGER.error("[refreshLocalUnSeqId] semaphore is null, id is {}", localUnSeqIdBO.getId());
                return;
            }
            boolean acquire = semaphore.tryAcquire();
            if (acquire) {
                ayncExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        LOGGER.info("开始尝试进行本地id段的同步操作");
                        // 异步执行 IO逻辑
                        try {
                            IdBuilderPO idBuilderPO = idBuilderMapper.selectById(localUnSeqIdBO.getId());
                            tryUpdateMySQLRecord(idBuilderPO);
                        } catch (Exception e) {
                            LOGGER.error("[refreshLocalUnSeqId] error is {}", e);
                        } finally {
                            semaphoreMap.get(localUnSeqIdBO.getId()).release();
                            LOGGER.info("本地无序id段同步完成,id is {}", localUnSeqIdBO.getId());
                        }
                    }
                });
            }
        }
    }



    // 回调，填充localSeqIdBOMap
    @Override
    public void afterPropertiesSet() throws Exception {
        List<IdBuilderPO> idBuilderPOS = idBuilderMapper.selectAll();
        for (IdBuilderPO idBuilderPO : idBuilderPOS) {
            // 抢占 id 段
            LOGGER.info("服务刚启动，抢占新的id段");
            tryUpdateMySQLRecord(idBuilderPO);
            semaphoreMap.put(idBuilderPO.getId(), new Semaphore(1));
        }
    }

    // 抢占响应的id段，抢占失败 进行重试
    // 需要和数据库 进行交互，耗时IO 操作
    private void tryUpdateMySQLRecord(IdBuilderPO idBuilderPO) {
        // 首先进行尝试
        int updateCount = idBuilderMapper.updateNewIdCountAndVersion(idBuilderPO.getId(), idBuilderPO.getVersion());
        // 抢占成功
        if (updateCount > 0) {
            buildLocalBo(idBuilderPO);
            return;
        }

        // 抢占失败，重试三次
        for (int i = 0; i < 3; i++) {
            idBuilderPO = idBuilderMapper.selectById(idBuilderPO.getId());
            int update = idBuilderMapper.updateNewIdCountAndVersion(idBuilderPO.getId(), idBuilderPO.getVersion());
            if (update > 0) {
                buildLocalBo(idBuilderPO);
                return;
            }
        }

        // 抢占失败
        throw new RuntimeException("表id段占用失败，竞争过于激烈，id is " + idBuilderPO.getId());
    }

    private void buildLocalBo(IdBuilderPO idBuilderPO) {
        // build map
        long currentStart = idBuilderPO.getCurrentStart();
        long nextThreshold = idBuilderPO.getNextThreshold();
        int step = idBuilderPO.getStep();
        Integer id = idBuilderPO.getId();

        if (idBuilderPO.getIsSeq() == IS_SEQ) {
            LocalSeqIdBO localSeqIdBO = new LocalSeqIdBO();
            localSeqIdBO.setCurrentStart(currentStart);
            localSeqIdBO.setNextThreshold(nextThreshold);
            localSeqIdBO.setId(id);
            localSeqIdBO.setStep(step);
            localSeqIdBO.setCurrentValue(new AtomicLong(currentStart));
            localSeqIdBO.setDesc(idBuilderPO.getRemark());
            localSeqIdBOMap.put(idBuilderPO.getId(), localSeqIdBO);
        } else {
            LocalUnSeqIdBO localUnSeqIdBO = new LocalUnSeqIdBO();
            localUnSeqIdBO.setCurrentStart(currentStart);
            localUnSeqIdBO.setNextThreshold(nextThreshold);
            localUnSeqIdBO.setStep(step);
            localUnSeqIdBO.setId(id);
            // set id queue
            List<Long> idList = new ArrayList<>();
            for (long i = currentStart; i < nextThreshold; i++) {
                idList.add(i);
            }
            Collections.shuffle(idList);
            ConcurrentLinkedQueue<Long> idQueue = new ConcurrentLinkedQueue<>();
            idQueue.addAll(idList);
            localUnSeqIdBO.setIdQueue(idQueue);
            localUnSeqIdBOMap.put(idBuilderPO.getId(), localUnSeqIdBO);
        }
    }
}
