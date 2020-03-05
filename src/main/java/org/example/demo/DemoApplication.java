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
            Subscribe subscribe = new Subscribe("ws://127.0.0.1:18000", logsListener);

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
            Transfer transfer = new Transfer("http:127.0.0.1:36000");
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
            Transfer transfer = new Transfer("http:127.0.0.1:36000");
            Account account = service.loadAccount("12345678",
                    "/mnt/d/lianxiangcloud/UTC--2019-12-03T06-52-45.799046000Z--7b8b0cf6f0f3a83cff0291d3482d2179d8b1588c");
            //部署合约
            ResponseModel model = transfer.deployContract(account,
                    "0x61016060405260016080908152600260a0819052600560c052600a60e0526014610100526032610120526064610140526200003c91600762000274565b503480156200004a57600080fd5b5060405160408062002116833981018060405260408110156200006c57600080fd5b508051602091820151600080546001600160a01b0319166001600160a01b038416179055909182908290620000a7908390620000fb811b901c565b620000b881620000fb60201b60201c565b600080546040516001600160a01b0390911691907f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e0908290a350505050620002e9565b620001168160016200014d60201b62001b521790919060201c565b6040516001600160a01b038216907f22380c05984257a1cb900161c713dd71d39e74820f1aea43bd3f1bdd2096129990600090a250565b6200015f8282620001f160201b60201c565b15620001cc57604080517f08c379a000000000000000000000000000000000000000000000000000000000815260206004820152601f60248201527f526f6c65733a206163636f756e7420616c72656164792068617320726f6c6500604482015290519081900360640190fd5b6001600160a01b0316600090815260209190915260409020805460ff19166001179055565b60006001600160a01b03821662000254576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401808060200182810382526022815260200180620020f46022913960400191505060405180910390fd5b506001600160a01b03166000908152602091909152604090205460ff1690565b828054828255906000526020600020908101928215620002b7579160200282015b82811115620002b7578251829060ff1690559160200191906001019062000295565b50620002c5929150620002c9565b5090565b620002e691905b80821115620002c55760008155600101620002d0565b90565b611dfb80620002f96000396000f3fe6080604052600436106101355760003560e01c80636897e974116100ab5780638fec807e1161006f5780638fec807e1461066e5780639c65f309146106b75780639f02d3dc146106f7578063bb5f747b14610721578063e0773d6114610754578063f2fde38b146107bb57610135565b80636897e974146105125780637362d9c8146105455780638c255f22146105785780638da5cb5b146106285780638f32d59b1461065957610135565b806333f707d1116100fd57806333f707d1146103105780633bc141731461033a57806347e1d5501461038057806348346dc91461040d5780634ddc2fe21461043d57806357d28f29146104d457610135565b80630615a4a61461017e5780630687df10146101cb57806306cdbea1146101fb5780630a0c231f1461022e57806314777e6c1461026a575b61013e336107ee565b61017c57604051600160e51b62461bcd028152600401808060200182810382526040815260200180611d906040913960400191505060405180910390fd5b005b34801561018a57600080fd5b506101ae600480360360408110156101a157600080fd5b5080359060200135610809565b604080516001600160801b03199092168252519081900360200190f35b3480156101d757600080fd5b5061017c600480360360408110156101ee57600080fd5b5080359060200135610851565b34801561020757600080fd5b5061017c6004803603604081101561021e57600080fd5b508035906020013560ff16610a0c565b34801561023a57600080fd5b506102586004803603602081101561025157600080fd5b5035610ae7565b60408051918252519081900360200190f35b34801561027657600080fd5b506102a46004803603604081101561028d57600080fd5b50803590602001356001600160801b031916610b05565b604051808660a080838360005b838110156102c95781810151838201526020016102b1565b50505050905001856001600160a01b03166001600160a01b031681526020018481526020018360ff1660ff1681526020018281526020019550505050505060405180910390f35b34801561031c57600080fd5b5061017c6004803603602081101561033357600080fd5b5035610bc5565b34801561034657600080fd5b5061017c6004803603608081101561035d57600080fd5b508035906001600160601b03196020820135169060408101359060600135610ca4565b34801561038c57600080fd5b506103aa600480360360208110156103a357600080fd5b5035610d88565b604051808981526020018881526020018781526020018660028111156103cc57fe5b60ff1681526001600160601b031995861660208201529390941660408085019190915260608401929092526080830152519081900360a00195509350505050f35b34801561041957600080fd5b506102a46004803603604081101561043057600080fd5b5080359060200135610e4d565b34801561044957600080fd5b506104676004803603602081101561046057600080fd5b5035610f21565b604051808a815260200189815260200188815260200187600281111561048957fe5b60ff1681526001600160601b031996871660208201529490951660408086019190915260608501939093526080840191909152151560a0830152519081900360c00195509350505050f35b3480156104e057600080fd5b506104fe600480360360208110156104f757600080fd5b5035610f78565b604080519115158252519081900360200190f35b34801561051e57600080fd5b5061017c6004803603602081101561053557600080fd5b50356001600160a01b0316610fd1565b34801561055157600080fd5b5061017c6004803603602081101561056857600080fd5b50356001600160a01b0316611027565b34801561058457600080fd5b5061017c6004803603602081101561059b57600080fd5b8101906020810181356401000000008111156105b657600080fd5b8201836020820111156105c857600080fd5b803590602001918460208302840111640100000000831117156105ea57600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092955061107a945050505050565b34801561063457600080fd5b5061063d6110d4565b604080516001600160a01b039092168252519081900360200190f35b34801561066557600080fd5b506104fe6110e4565b34801561067a57600080fd5b5061017c6004803603608081101561069157600080fd5b508035906001600160801b03196020820135169060ff60408201351690606001356110f5565b3480156106c357600080fd5b5061017c600480360360608110156106da57600080fd5b50803590602081013590604001356001600160601b0319166113a2565b34801561070357600080fd5b506102586004803603602081101561071a57600080fd5b5035611503565b34801561072d57600080fd5b506104fe6004803603602081101561074457600080fd5b50356001600160a01b03166107ee565b61017c600480360361010081101561076b57600080fd5b6040805160a081810183528435946001600160801b031960208201351694810193909260e08401929091840190600590839083908082843760009201919091525091945050903591506115189050565b3480156107c757600080fd5b5061017c600480360360208110156107de57600080fd5b50356001600160a01b0316611850565b600061080160018363ffffffff6118a316565b90505b919050565b600082815260036020526040812060080180548390811061082657fe5b90600052602060002090600291828204019190066010029054906101000a900460801b905092915050565b61085a336107ee565b61089857604051600160e51b62461bcd028152600401808060200182810382526040815260200180611d906040913960400191505060405180910390fd5b60008281526003602052604090206002600382015460ff1660028111156108bb57fe5b146109105760408051600160e51b62461bcd02815260206004820152600f60248201527f67616d65206973206e6f7420656e640000000000000000000000000000000000604482015290519081900360640190fd5b600781015460ff161561096d5760408051600160e51b62461bcd02815260206004820152601660248201527f6f776e6572206861732077697468647261772066656500000000000000000000604482015290519081900360640190fd5b60078101805460ff191660011790556109846110d4565b6001600160a01b03166108fc839081150290604051600060405180830381858888f193505050501580156109bc573d6000803e3d6000fd5b507fb273ffbe5a7fd64dbf83e917e78fa5cad17273a621566f1aaf67581a0efa634f826109e76110d4565b604080519283526001600160a01b0390911660208301528051918290030190a1505050565b610a15336107ee565b610a5357604051600160e51b62461bcd028152600401808060200182810382526040815260200180611d906040913960400191505060405180910390fd5b600082815260036020526040812090600382015460ff166002811115610a7557fe5b1415610ac25760408051600160e51b62461bcd02815260206004820152601160248201526001607a1b7019d85b59481a5cc81b9bdd08195e1a5cdd02604482015290519081900360640190fd5b60038101805483919060ff19166001836002811115610add57fe5b0217905550505050565b60028181548110610af457fe5b600091825260209091200154905081565b610b0d611bd6565b60008381526003602081815260408084206001600160801b03198716855260090190915280832060018101546002820154938201546004830154845160a08101958690528796879687969586956001600160a01b0390911694929360ff90911692918690600590828b855b825461010083900a900460ff16815260206001928301818104948501949093039092029101808411610b785790505050505050945083935095509550955095509550509295509295909350565b610bcd6110e4565b610c0f5760408051600160e51b62461bcd0281526020600482018190526024820152600080516020611d4e833981519152604482015290519081900360640190fd5b3031811115610c605760408051600160e51b62461bcd0281526020600482015260126024820152600160731b710c4c2d8c2dcc6ca40dcdee840cadcdeeaced02604482015290519081900360640190fd5b610c686110d4565b6001600160a01b03166108fc829081150290604051600060405180830381858888f19350505050158015610ca0573d6000803e3d6000fd5b5050565b610cad336107ee565b610ceb57604051600160e51b62461bcd028152600401808060200182810382526040815260200180611d906040913960400191505060405180910390fd5b600084815260036020526040812090600382015460ff166002811115610d0d57fe5b1415610d5a5760408051600160e51b62461bcd02815260206004820152601160248201526001607a1b7019d85b59481a5cc81b9bdd08195e1a5cdd02604482015290519081900360640190fd5b6004810180546001600160a01b03191660609590951c94909417909355600583019190915560069091015550565b6000818152600360205260408120819081908190819081908190819081600382015460ff166002811115610db857fe5b1415610e055760408051600160e51b62461bcd02815260206004820152601160248201526001607a1b7019d85b59481a5cc81b9bdd08195e1a5cdd02604482015290519081900360640190fd5b805460018201546002830154600384015460048501546005860154600690960154949f939e50919c5060ff81169b506101009004606090811b9a501b97509195509350915050565b610e55611bd6565b6000806000806000610e678888610809565b60008981526003602081815260408084206001600160801b0319861685526009019091529182902060018101546002820154928201546004830154855160a08101909652959650919485946001600160a01b03909216939260ff16918560058282826020028201916000905b825461010083900a900460ff16815260206001928301818104948501949093039092029101808411610ed3579050505050505094508393509650965096509650965050509295509295909350565b60036020819052600091825260409091208054600182015460028301549383015460048401546005850154600686015460079096015494969395939460ff80851695610100909504606090811b9594901b93911689565b600254600090610f8a57506001610804565b60005b600254811015610fc85760028181548110610fa457fe5b9060005260206000200154831415610fc0576001915050610804565b600101610f8d565b50600092915050565b610fd96110e4565b61101b5760408051600160e51b62461bcd0281526020600482018190526024820152600080516020611d4e833981519152604482015290519081900360640190fd5b6110248161190d565b50565b61102f6110e4565b6110715760408051600160e51b62461bcd0281526020600482018190526024820152600080516020611d4e833981519152604482015290519081900360640190fd5b61102481611955565b611083336107ee565b6110c157604051600160e51b62461bcd028152600401808060200182810382526040815260200180611d906040913960400191505060405180910390fd5b8051610ca0906002906020840190611bf4565b6000546001600160a01b03165b90565b6000546001600160a01b0316331490565b6110fe336107ee565b61113c57604051600160e51b62461bcd028152600401808060200182810382526040815260200180611d906040913960400191505060405180910390fd5b60008481526003602052604090206002600382015460ff16600281111561115f57fe5b146111b45760408051600160e51b62461bcd02815260206004820152601660248201527f67616d6520737461747573206973206e6f7420454e4400000000000000000000604482015290519081900360640190fd5b6001600160801b031984166000908152600982016020526040902060018101546001600160a01b03166112315760408051600160e51b62461bcd02815260206004820152601260248201527f6f72646572206973206e6f742065786973740000000000000000000000000000604482015290519081900360640190fd5b600381015460ff161561128e5760408051600160e51b62461bcd02815260206004820152601760248201527f6f7264657220686173206265656e206469766964656e64000000000000000000604482015290519081900360640190fd5b30318311156112df5760408051600160e51b62461bcd0281526020600482015260126024820152600160731b710c4c2d8c2dcc6ca40dcdee840cadcdeeaced02604482015290519081900360640190fd5b60038101805460ff191660ff86161790556004810183905560018101546040516001600160a01b03909116906108fc8515029085906000818181858888f19350505050158015611333573d6000803e3d6000fd5b506001810154604080518881526001600160801b0319881660208201526001600160a01b039092168282015260ff8616606083015260808201859052517f1bd0d59a6627c0d64209473f6fec5bd9eed6ada45847c694e9daa0ea950e40c09181900360a00190a1505050505050565b6113ab336107ee565b6113e957604051600160e51b62461bcd028152600401808060200182810382526040815260200180611d906040913960400191505060405180910390fd5b600083815260036020526040812090600382015460ff16600281111561140b57fe5b146114605760408051600160e51b62461bcd02815260206004820152600d60248201527f67616d6520697320657869737400000000000000000000000000000000000000604482015290519081900360640190fd5b600083116114b85760408051600160e51b62461bcd02815260206004820152601560248201527f6265742076616c756520706172616d206572726f720000000000000000000000604482015290519081900360640190fd5b8381556001808201849055600382018054610100600160a81b031916610100606086901c02178082556000600285015560ff191682800217905550600701805460ff19169055505050565b60009081526003602052604090206008015490565b60008481526003602052604090206001600382015460ff16600281111561153b57fe5b146115905760408051600160e51b62461bcd02815260206004820152601160248201527f67616d65206973206e6f74207374617274000000000000000000000000000000604482015290519081900360640190fd5b6001600160801b031984166000908152600982016020526040902060018101546001600160a01b03161561160e5760408051600160e51b62461bcd02815260206004820152600e60248201527f6f72646572206973206578697374000000000000000000000000000000000000604482015290519081900360640190fd5b81600101548302341461166b5760408051600160e51b62461bcd02815260206004820152601360248201527f73656e642076616c75652069732077726f6e6700000000000000000000000000604482015290519081900360640190fd5b61167483610f78565b6116c85760408051600160e51b62461bcd02815260206004820152600e60248201527f62657420636e7473206572726f72000000000000000000000000000000000000604482015290519081900360640190fd5b6116d18461199d565b6117255760408051600160e51b62461bcd02815260206004820152601360248201527f636865636b206e756d62657273206572726f7200000000000000000000000000604482015290519081900360640190fd5b60088201805460018181018355600092835260209092206002808304909101805460808a901c6010948616949094026101000a9384026fffffffffffffffffffffffffffffffff90940219169290921790915590820180546001600160a01b03191633179055810183905561179c81856005611c3f565b5060028201546117b2908463ffffffff6119e116565b6002830155604080518781526001600160801b0319871660208201527f5ba60b499aef411cd34392901f883f8194a3f79d2ba8d099b8799db5c8ef2c4091889188918891889133919081018460a080838360005b8381101561181e578181015183820152602001611806565b505050509190910193845250506001600160a01b031660208201526040805191829003019350915050a1505050505050565b6118586110e4565b61189a5760408051600160e51b62461bcd0281526020600482018190526024820152600080516020611d4e833981519152604482015290519081900360640190fd5b61102481611a45565b60006001600160a01b0382166118ed57604051600160e51b62461bcd028152600401808060200182810382526022815260200180611d6e6022913960400191505060405180910390fd5b506001600160a01b03166000908152602091909152604090205460ff1690565b61191e60018263ffffffff611ae816565b6040516001600160a01b038216907f0a8eb35e5ca14b3d6f28e4abf2f128dbab231a58b56e89beb5d636115001e16590600090a250565b61196660018263ffffffff611b5216565b6040516001600160a01b038216907f22380c05984257a1cb900161c713dd71d39e74820f1aea43bd3f1bdd2096129990600090a250565b6000805b60058110156119d857600f8382600581106119b857fe5b602002015160ff1611156119d0576000915050610804565b6001016119a1565b50600192915050565b600082820183811015611a3e5760408051600160e51b62461bcd02815260206004820152601b60248201527f536166654d6174683a206164646974696f6e206f766572666c6f770000000000604482015290519081900360640190fd5b9392505050565b6001600160a01b038116611a8d57604051600160e51b62461bcd028152600401808060200182810382526026815260200180611d076026913960400191505060405180910390fd5b600080546040516001600160a01b03808516939216917f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e091a3600080546001600160a01b0319166001600160a01b0392909216919091179055565b611af282826118a3565b611b3057604051600160e51b62461bcd028152600401808060200182810382526021815260200180611d2d6021913960400191505060405180910390fd5b6001600160a01b0316600090815260209190915260409020805460ff19169055565b611b5c82826118a3565b15611bb15760408051600160e51b62461bcd02815260206004820152601f60248201527f526f6c65733a206163636f756e7420616c72656164792068617320726f6c6500604482015290519081900360640190fd5b6001600160a01b0316600090815260209190915260409020805460ff19166001179055565b6040518060a001604052806005906020820280388339509192915050565b828054828255906000526020600020908101928215611c2f579160200282015b82811115611c2f578251825591602001919060010190611c14565b50611c3b929150611cce565b5090565b600183019183908215611cc25791602002820160005b83821115611c9357835183826101000a81548160ff021916908360ff1602179055509260200192600101602081600001049283019260010302611c55565b8015611cc05782816101000a81549060ff0219169055600101602081600001049283019260010302611c93565b505b50611c3b929150611ce8565b6110e191905b80821115611c3b5760008155600101611cd4565b6110e191905b80821115611c3b57805460ff19168155600101611cee56fe4f776e61626c653a206e6577206f776e657220697320746865207a65726f2061646472657373526f6c65733a206163636f756e7420646f6573206e6f74206861766520726f6c654f776e61626c653a2063616c6c6572206973206e6f7420746865206f776e6572526f6c65733a206163636f756e7420697320746865207a65726f206164647265737357686974656c69737441646d696e526f6c653a2063616c6c657220646f6573206e6f742068617665207468652057686974656c69737441646d696e20726f6c65a165627a7a72305820b330d65737933c5be5aafc2b0288177a2e4db9f7ee2ea437a98e607f7aa93a3b0029526f6c65733a206163636f756e7420697320746865207a65726f2061646472657373");
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
