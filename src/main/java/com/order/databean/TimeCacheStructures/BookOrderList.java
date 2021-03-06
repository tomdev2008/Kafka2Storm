package com.order.databean.TimeCacheStructures;

import com.order.constant.Constant;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by LiMingji on 2015/5/28.
 */
public class BookOrderList {

    private HashMap<String, RealTimeCacheList<Integer>> map = null;

    private static final Object LOCK = new Object();

    public BookOrderList() {
        map = new HashMap<String, RealTimeCacheList<Integer>>();
    }

    public void put(String bookId, int orderType, Long currentTime) {
        synchronized (LOCK) {
            if (map.containsKey(bookId)) {
                RealTimeCacheList<Integer> orderTypeList = map.get(bookId);
                orderTypeList.put(orderType, currentTime);
            } else {
                RealTimeCacheList<Integer> orderTypeList = null;
                if (orderType == 4) {
                    orderTypeList = new RealTimeCacheList<Integer>(Constant.THREE_MINUTES);
                } else {
                    orderTypeList = new RealTimeCacheList<Integer>(Constant.FIVE_MINUTES);
                }
                orderTypeList.put(orderType);
                map.put(bookId, orderTypeList);
            }
        }
    }

    //获取所有订购的图书本数
    public int sizeOfOrderBooks() {
        synchronized (LOCK) {
            return map.size();
        }
    }

    //特定图书的订购次数
    public int sizeOfBookOrderTimes(String id) {
        synchronized (LOCK) {
            if (!map.containsKey(id)) {
                return 0;
            }
            RealTimeCacheList<Integer> orderList = map.get(id);
            return orderList.size();
        }
    }

    //特定orderType下的图书订购次数
    public int sizeOfBookOrderTimesWithOrderType(String id, int orderType) {
        synchronized (LOCK) {
            if (!map.containsKey(id)) {
                return 0;
            }
            RealTimeCacheList<Integer> orderList = map.get(id);
            return orderList.sizeById(orderType);
        }
    }

    public Set<String> keySet() {
        return map.keySet();
    }
}
