package com.order.databean.TimeCacheStructures;

import org.apache.log4j.Logger;

import java.util.*;

/**
 * reference: https://github.com/HQebupt/TimeCacheMap
 *
 * 实时过期List :
 *      expirationSecs: List中数据过期时间。
 *      TimeOutCallBack: 对过期的数据如何进行处理。（可以选择进行持久化操作。）
 *
 * 清理操作：
 *      1. 定时清理：每隔expirationSecs进行一次清理。
 *      2. 数据插入时清理：插入对于同一key下的过期数据进行清理。
 *
 * 说明：
 *      List中key唯一。相同的key会根据访问时间合并到key所对应的List中。
 *
 * Created by LiMingji on 2015/5/22.
 */
public class RealTimeCacheList<T> {

    private static Logger log = Logger.getLogger(RealTimeCacheList.class);

    public static interface TimeOutCallback<T> {
        public void expire(T value, LinkedList<Long> pvTimes);
    }

    private Map<T, LinkedList<Long>> oldList;
    private Map<T, LinkedList<Long>> currentList;

    protected final static Object LOCK = new Object();
    protected Thread cleaner = null;
    protected TimeOutCallback timeOutCallback = null;
    protected int expiratonSecs = 0;

    public RealTimeCacheList(int expiratonSecs) {
        this(expiratonSecs, null);
    }

    public RealTimeCacheList(int expirationSecs, final TimeOutCallback timeOutCallback) {
        oldList = new LinkedHashMap<T, LinkedList<Long>>();
        currentList = new LinkedHashMap<T, LinkedList<Long>>();

        this.timeOutCallback = timeOutCallback;
        this.expiratonSecs = expirationSecs;
        final long sleepTime = expirationSecs * 1000L;

        cleaner = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        cleaner.sleep(sleepTime);
                        if (timeOutCallback != null) {
                            Iterator<T> iterator = oldList.keySet().iterator();
                            while (iterator.hasNext()) {
                                T key = iterator.next();
                                timeOutCallback.expire(key, oldList.get(key));
                            }
                        }
                        Iterator<T> it = currentList.keySet().iterator();
                        while (it.hasNext()) {
                            T value = it.next();
                            removeExpiredData(value, System.currentTimeMillis());
                        }
                        oldList.clear();
                        oldList.putAll(currentList);
                        currentList.clear();
                    } catch (InterruptedException e) {
                        log.error(e);
                    }
                }
            }
        });
        cleaner.setDaemon(true);
        cleaner.start();
    }

    public int size() {
        synchronized (LOCK) {
            return currentList.size() + oldList.size();
        }
    }

    public void put(T value) {
        this.put(value, (new Date()).getTime());
    }

    public void put(T value, Long date) {
        synchronized (LOCK) {
            long currentTime;
            if (date == null) {
                currentTime = date;
            } else {
                currentTime = System.currentTimeMillis();
            }

            //clear expired data when insert data
            removeExpiredData(value, currentTime);

            if (oldList.containsKey(value)) {
                LinkedList<Long> clickTimes = oldList.get(value);
                LinkedList<Long> newClickTimes = new LinkedList<Long>();
                newClickTimes.addAll(clickTimes);
                newClickTimes.add(currentTime);
                currentList.put(value, newClickTimes);
                oldList.remove(value);
            } else {
                LinkedList<Long> clickTimes = new LinkedList<Long>();
                clickTimes.add(currentTime);
                currentList.put(value, clickTimes);
            }
        }
    }

    //对某个id下的过期数据进行清楚。
    private void removeExpiredData(T value, long currentTime) {
        synchronized (LOCK) {
            if (!oldList.containsKey(value)) {
                return;
            }
            long timeOutThreshold = currentTime - expiratonSecs * 1000L;
            Iterator<Long> it = oldList.get(value).iterator();
            while (it.hasNext()) {
                Long time = it.next();
                if (time < timeOutThreshold) {
                    it.remove();
                } else {
                    return;
                }
            }
        }
    }

    public void clear() {
        cleaner.interrupt();
    }
}