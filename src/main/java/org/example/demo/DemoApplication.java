package org.example.demo;

import org.lkchain.account.Account;
import org.lkchain.account.AccountService;
import org.lkchain.sample.MyListener;
import org.lkchain.transaction.Helper;
import org.lkchain.transaction.Receipt;
import org.lkchain.transaction.ResponseModel;
import org.lkchain.transaction.Transfer;
import org.lkchain.websocket.FilterCriteria;
import org.lkchain.websocket.LogsListener;
import org.lkchain.websocket.Subscribe;
import org.web3j.abi.EventEncoder;
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
         //事件订阅demo
         subscribeDemo();
        //转账demo
         transferDemo();
         //合约查询demo
         queryContractDemo();
         //合约调用demo
         callContractDemo();
         //部署合约
        deployContractDemo();
        //调用其它rpc请求
         rpcRequest();
    }

    private void subscribeDemo() throws Exception {
        try {
            // 日志事件处理
            LogsListener logsListener = new MyListener();
            // 享云链节点的websocket端口
            Subscribe subscribe = new Subscribe(websocketURL, logsListener);

            // 监听的合约地址
            String contractAddr = "0xc7e712ee23788ed250298cc31de7dba009858fb1";
            List<String> addrs = new ArrayList<String>();
            addrs.add(contractAddr);
            FilterCriteria filter = new FilterCriteria(null, null, addrs);
            // 要监听的事件event
            Event event = new Event("event_bet",
                    Arrays.asList(
                    new TypeReference<Address>() {}, // 按event中input参数的顺序定义
                    new TypeReference<Utf8String>() {
                    }));
            //生成event事件的topic
            String topic = EventEncoder.encode(event);
            //指定监听该事件
            filter.addSingleTopic(topic);
            subscribe.subscribeLogs(filter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void transferDemo() throws Exception {
        try {
            AccountService service = new AccountService();
            Transfer transfer = new Transfer(rpcURL);
            // 已有密钥文件,直接将密钥文件加载进来
            Account account = service.loadAccount("12345678",
                    "/mnt/d/lianxiangcloud/UTC--2019-12-03T06-52-45.799046000Z--7b8b0cf6f0f3a83cff0291d3482d2179d8b1588c");
            // 如果沒有密钥文件,可以新建一个地址
            // 创建密钥时，需要指定密码和存储路径,密码不得小于8位
//             Account account = service.createAccount("12345678",
//             "/mnt/d/lianxiangcloud/");

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void queryContractDemo() throws Exception {
        try {
            Transfer transfer = new Transfer(rpcURL);
            String orderID = "35650b131d7b45c99138278ba2a238b1";
            byte[] byBuffer = Helper.Hex2ByteArray(orderID);
            // 根据函数abi 生成合约调用的data
            List<Type> inputParameters = Arrays.asList(new Uint256(1), new Bytes16(byBuffer));
            List<TypeReference<?>> outputParameters = Arrays.asList(new TypeReference<Address>() {
            }, new TypeReference<Uint256>() {
            }, new TypeReference<Uint256>() {
            }, new TypeReference<DynamicArray<Uint256>>() {
            });

            Function func = new Function("getOrder", inputParameters, outputParameters);
            String data = FunctionEncoder.encode(func);

            String contractAddr = "0x5b9a5641db0dff5dd750e02979294498012c55fa";
            //执行合约查询,data也可以用其它工具生成
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void callContractDemo() throws Exception {
        try {
            AccountService service = new AccountService();
            Transfer transfer = new Transfer(websocketURL);
            Account account = service.loadAccount("12345678",
                    "/mnt/d/lianxiangcloud/0xc8c79562c818ed5337df13c636a8b38c55e78590");

            List<Type> inputParameters = Arrays.asList(new Uint256(10), new Utf8String("123"));
            List<TypeReference<?>> outputParameters = new ArrayList<>();
            Function func = new Function("bet", inputParameters, outputParameters);
            String betData = FunctionEncoder.encode(func);

            // 这个合约部署在享云链沙盒环境
            String contractAddr = "0xfe8f4e74254cb69e2656f82b0925759b44eec0ba";
            BigInteger value = Helper.LianKe.multiply(BigInteger.valueOf(2l));
            //执行合约调用,data也可以用其它工具生成
            ResponseModel model = transfer.callContract(account, contractAddr, value, betData);
            System.out.println(model.toString());
            // 根据交易hash获取交易收据
            if (model.getResult() != null) {
                //休眠5秒待peer同步到区块
                Thread.sleep(1000 * 5);
                Receipt receipt = transfer.getRpc().GetTransactionReceipt(model.getResult().toString());
                System.out.println(receipt.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void deployContractDemo() throws Exception {
        try {
            AccountService service = new AccountService();
            Transfer transfer = new Transfer(rpcURL);
            Account account = service.loadAccount("12345678",
                    "/mnt/d/lianxiangcloud/UTC--2019-12-03T06-52-45.799046000Z--7b8b0cf6f0f3a83cff0291d3482d2179d8b1588c");
            //部署合约,此处部署的是一个石头剪刀布合约
            ResponseModel model = transfer.deployContract(account,
                    "0x608060405234801561001057600080fd5b5033600160006101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550610601806100616000396000f3fe60806040526004361061006d576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680632e1a7d4d146100725780638da5cb5b146100a0578063915eb32f146100f757806392d0d153146101bc578063bec082bb146101e7575b600080fd5b61009e6004803603602081101561008857600080fd5b8101908080359060200190929190505050610235565b005b3480156100ac57600080fd5b506100b5610392565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b6101ba6004803603604081101561010d57600080fd5b81019080803590602001909291908035906020019064010000000081111561013457600080fd5b82018360208201111561014657600080fd5b8035906020019184600183028401116401000000008311171561016857600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600081840152601f19601f8201169050808301925050505050505091929192905050506103b8565b005b3480156101c857600080fd5b506101d1610493565b6040518082815260200191505060405180910390f35b610233600480360360408110156101fd57600080fd5b81019080803573ffffffffffffffffffffffffffffffffffffffff16906020019092919080359060200190929190505050610499565b005b600160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614151561029157600080fd5b60003073ffffffffffffffffffffffffffffffffffffffff16319050808211151515610325576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260128152602001807f62616c616e6365206e6f7420656e6f756768000000000000000000000000000081525060200191505060405180910390fd5b600160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff166108fc839081150290604051600060405180830381858888f1935050505015801561038d573d6000803e3d6000fd5b505050565b600160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b816000819055507f64a2cb2f4618d2a9a1b9cf17f12149c8c583b543ad49f160dd542b47d9c560073382604051808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200180602001828103825283818151815260200191508051906020019080838360005b83811015610454578082015181840152602081019050610439565b50505050905090810190601f1680156104815780820380516001836020036101000a031916815260200191505b50935050505060405180910390a15050565b60005481565b600160009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff161415156104f557600080fd5b60003073ffffffffffffffffffffffffffffffffffffffff16319050808211151515610589576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260128152602001807f62616c616e6365206e6f7420656e6f756768000000000000000000000000000081525060200191505060405180910390fd5b8273ffffffffffffffffffffffffffffffffffffffff166108fc839081150290604051600060405180830381858888f193505050501580156105cf573d6000803e3d6000fd5b5050505056fea165627a7a723058200fbb2d9ffb5b78e6e7e8e395441e184a10c17ede6c5da30dfe10081b8824e24e0029");
            System.out.println(model.toString());
            // 根据交易hash获取交易收据
            if (model.getResult() != null) {
                //休眠5秒待peer同步到区块
                Thread.sleep(1000 * 5);
                Receipt receipt = transfer.getRpc().GetTransactionReceipt(model.getResult().toString());
                System.out.println(receipt.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void rpcRequest() throws Exception {
        Transfer transfer = new Transfer(rpcURL);
        List params = Arrays.asList("latest", true);
        // 调用其它rpc请求
        ResponseModel mode = transfer.getRpc().sendRequest("eth_getBlockByNumber", params);
        System.out.println(mode.toString());
    }
}
