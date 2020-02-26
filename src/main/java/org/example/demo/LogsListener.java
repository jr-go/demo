package org.example.demo;

import com.google.gson.Gson;
import org.lkchain.websocket.Message;
import org.lkchain.websocket.MessageListener;
import org.lkchain.websocket.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes16;
import org.web3j.abi.datatypes.generated.Uint256;
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
public class LogsListener implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(org.lkchain.websocket.LogsListener.class);
    private static final Gson gson = new Gson();
    private static Event event = new Event("UserClockIn", Arrays.asList(new TypeReference<Uint256>() {
    }, new TypeReference<Bytes16>() {
    }, new TypeReference<Uint256>() {
    }, new TypeReference<Uint256>() {
    }, new TypeReference<Address>() {
    }));

    @Override
    public void onError(Exception e) {
        logger.error("subscription got an err:" + e.getMessage());
    }

    @Override
    public void onMessage(String s) {
        try {
            Message msg = gson.fromJson(s, Message.class);
            // 判断是否是订阅的消息
            if (msg != null && Subscribe.SubscribeMethod.equals(msg.getMethod())) {
                // 获取订阅消息中的log数据
                Log log = msg.getParams().getResult();
                // 事件数据解析,非index修饰的数据存储在log的data中
                List<Type> nonIndexedValues = FunctionReturnDecoder.decode(log.getData(),
                        event.getNonIndexedParameters());
                ListIterator<Type> iterator = nonIndexedValues.listIterator();
                while(iterator.hasNext()){
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
                // TODO::处理事件数据
                return;
            }
            logger.info(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        logger.warn("subscribe closed");
    }
}

