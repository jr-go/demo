package org.example.demo;

import org.lkchain.websocket.LogsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.protocol.websocket.events.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

/**
 * @program: demo
 * @description: 事件处理
 * @author: JR
 * @create: 2020-02-20 13:23
 */
public class MyListener implements LogsListener {

    private static final Logger logger = LoggerFactory.getLogger(org.lkchain.sample.MyListener.class);
    private static Event event = new Event("event_bet", Arrays.asList(new TypeReference<Address>() {
    }, // 按event中input参数的顺序定义
            new TypeReference<Utf8String>() {
            }));

    @Override
    public void onError(Exception e) {
        logger.error("subscription got an err:" + e.getMessage());
    }

    @Override
    public void onLogs(Log log) {
        try {
            // 事件数据解析,非index修饰的数据存储在log的data中
            List<Type> nonIndexedValues = FunctionReturnDecoder.decode(log.getData(), event.getNonIndexedParameters());
            ListIterator<Type> iterator = nonIndexedValues.listIterator();
            while (iterator.hasNext()) {
                Type a = iterator.next();
                logger.info(a.getTypeAsString() + ":" + a.getValue().toString());
            }
            // index修饰的数据存储在log的topics中
            List<Type> indexedValues = new ArrayList<>();
            List<TypeReference<Type>> indexedParameters = event.getIndexedParameters();
            List<String> topics = log.getTopics();
            for (int i = 0; i < indexedParameters.size(); i++) {
                Type value = FunctionReturnDecoder.decodeIndexedValue(topics.get(i + 1), indexedParameters.get(i));
                logger.info(value.getTypeAsString() + ":" + value.getValue().toString());
                indexedValues.add(value);
            }
            // TODO::业务逻辑
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        logger.warn("subscribe closed");
    }
}
