package org.example.demo;

import org.lkchain.account.Account;
import org.lkchain.account.AccountService;
import org.lkchain.transaction.Helper;
import org.lkchain.transaction.Receipt;
import org.lkchain.transaction.ResponseModel;
import org.lkchain.transaction.Transfer;
import org.lkchain.websocket.FilterCriteria;
import org.lkchain.websocket.MessageListener;
import org.lkchain.websocket.Subscribe;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Bytes16;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

public class DemoApplication {
    private static String websocketURL = "ws://127.0.0.1:18000";
    private static String rpcURL = "http://127.0.0.1:16000";

    public static void main(String[] args) throws Exception {
        new DemoApplication().run();
    }

    private void run() throws Exception {
        subscribeDemo();
        transferDemo();
        queryContractDemo();
        callContractDemo();
        rpcRequest();
    }

    private void subscribeDemo() throws Exception {
        try{
            // 消息回调处理
            MessageListener logsListener = new LogsListener();
            // 享云链节点的rpc端口
            Subscribe subscribe = new Subscribe(websocketURL, logsListener);

            // 监听的合约地址
            String contractAddr = "0xa76386a1ad82a32e5557e8948e74b71a84666f7a";
            List<String> addrs = new ArrayList<String>();
            addrs.add(contractAddr);
            FilterCriteria filter = new FilterCriteria(null, null, addrs, null);

            subscribe.subscribeLogs(filter);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void transferDemo() throws Exception {
        try{
            AccountService service = new AccountService();
            Transfer transfer = new Transfer(rpcURL);
            // 已有密钥文件,直接将密钥文件加载进来
            Account account = service.loadAccount("12345678",
                    "/mnt/d/lianxiangcloud/UTC--2019-12-03T06-52-45.799046000Z--7b8b0cf6f0f3a83cff0291d3482d2179d8b1588c");
            // 如果沒有密钥文件,可以新建一个地址
            // 创建密钥时，需要指定密码和存储路径,密码不得小于8位
            // Account account = service.createAccount("12345678",
            // "/mnt/d/lianxiangcloud/");

            // 获取地址的链克余额,单位为wei,除以1e18即是链克数
            BigInteger balance = transfer.getRpc().GetBalance(account);
            BigInteger value = Helper.LianKe;
            ResponseModel rsp = transfer.transfer(account, "0x7b8b0cf6f0f3a83cff0291d3482d2179d8b1588c", value);
            if (rsp.getError() != null) {
                System.out.println(rsp.getError().toString());
                return;
            }
            // 获取交易hash
            if (rsp.getResult() != null) {
                System.out.println("transactionHash:" + rsp.getResult());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void queryContractDemo() throws Exception {
        try{
            Transfer transfer = new Transfer(rpcURL);
            String orderID = "35650b131d7b45c99138278ba2a238b1";
            byte[] byBuffer = Helper.Hex2ByteArray(orderID);
            // 生成合约调用的data
            List<Type> inputParameters = Arrays.asList(new Uint256(1), new Bytes16(byBuffer));
            List<TypeReference<?>> outputParameters = Arrays.asList(
                    new TypeReference<Address>() {
                    }, new TypeReference<Uint256>() {
                    }, new TypeReference<Uint256>() {
                    }, new TypeReference<DynamicArray<Uint256>>() {
                    });

            Function func = new Function("getOrder", inputParameters, outputParameters);
            String data = FunctionEncoder.encode(func);

            String contractAddr = "0x5b9a5641db0dff5dd750e02979294498012c55fa";
            ResponseModel model = transfer.getRpc().EthCall(contractAddr, data);
            System.out.println(model.toString());
            String rawInput = model.getResult().toString();
            List returnData = FunctionReturnDecoder.decode(rawInput, func.getOutputParameters());
            ListIterator<Type> iterator = returnData.listIterator();
            System.out.println("查询结果数据解析");
            while (iterator.hasNext()) {
                Type a = iterator.next();
                System.out.println(a.getTypeAsString() + ":" + a.getValue().toString());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    private static void callContractDemo() throws Exception {
        try {
            AccountService service = new AccountService();
            Transfer transfer = new Transfer("http:127.0.0.1:36000");
            Account account = service.loadAccount("12345678",
                    "/mnt/d/lianxiangcloud/0xc8c79562c818ed5337df13c636a8b38c55e78590");

            List<Type> inputParameters = Arrays.asList(new Uint256(10), new Utf8String("123"));
            List<TypeReference<?>> outputParameters = new ArrayList<>();
            Function func = new Function("bet", inputParameters, outputParameters);
            String betData = FunctionEncoder.encode(func);

            //这个合约部署在享云链沙盒环境
            String contractAddr = "0xfe8f4e74254cb69e2656f82b0925759b44eec0ba";
            BigInteger value = Helper.LianKe.multiply(BigInteger.valueOf(2l));
            ResponseModel model = transfer.callContract(account, contractAddr, value, betData);
            System.out.println(model.toString());
            // 根据交易hash获取交易收据
            if (model.getResult() != null) {
                Receipt receipt = transfer.getRpc().GetTransactionReceipt(model.getResult().toString());
                System.out.println(receipt.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void rpcRequest() throws Exception {
        Transfer transfer = new Transfer(rpcURL);
        List params = Arrays.asList(
                "latest",
                true
        );
        //调用其它rpc请求
        ResponseModel mode = transfer.getRpc()
                .sendRequest("eth_getBlockByNumber",params);
        System.out.println(mode.toString());
    }
}
