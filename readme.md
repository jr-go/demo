### 享云链JAVA sdk 使用demo

#### 一 环境准备

参考以下链接,搭建一个本地的享云链节点

[https://github.com/lianxiangcloud/linkchain/blob/release/v0.1.3/doc/compile_and_run_node.md]()

#### 二 获取websocket及http服务地址

按照github上的指示编译好docker 镜像后,执行以下脚本进入docker容器

```shell
sudo docker run -ti --net=host lkbuilder
```

相比较github上的指示多加了一个--net=host的参数,这个参数的意思是该容器使用主机网络,即docker容器中监听的端口跟主机端口是一致的。执行完此命令后会进入到容器中,此时我们选择启动一个链接到现网环境的节点(当然你也可以选择起一个单节点的本地测试网络,不过这样的话就需要自己部署合约)

启动节点后,在容器内执行以下命令获取websocket服务地址和rpc服务地址,websocket服务可以用来订阅事件,rpc服务则是用来发送交易和读写合约

```shell
ps -ef | grep lkchain
```

![websocketport](https://github.com/jr-go/demo/raw/master/img/service.png)

   其中 --rpc.ws_endpoint 参数后面的就是节点的websocket服务地址,--rpc.http_endpoint就是rpc服务地址

#### 三 事件监听

1. 设置要监听的合约地址和事件(不设置的话默认会监听链上所有的合约事件)

   打开享云链浏览器,找到剪刀石头布这个合约,点击源代码

   https://explorer.lkchain.org/#/address/0xc7e712ee23788ed250298cc31de7dba009858fb1

   ![sourcecode](https://github.com/jr-go/demo/raw/master/img/sourcecode.png)

   找到该合约的abi,点击复制

   ![abi](https://github.com/jr-go/demo/raw/master/img/abi.png)

   得到该合约的abi

   ```json
   [{"constant":false,"inputs":[{"name":"amount","type":"uint256"}],"name":"withdraw","outputs":[],"payable":true,"stateMutability":"payable","type":"function"},{"constant":true,"inputs":[],"name":"owner","outputs":[{"name":"","type":"address"}],"payable":false,"stateMutability":"view","type":"function"},{"constant":false,"inputs":[{"name":"_t","type":"uint256"},{"name":"orderId","type":"string"}],"name":"bet","outputs":[],"payable":true,"stateMutability":"payable","type":"function"},{"constant":true,"inputs":[],"name":"t","outputs":[{"name":"","type":"uint256"}],"payable":false,"stateMutability":"view","type":"function"},{"constant":false,"inputs":[{"name":"other","type":"address"},{"name":"amount","type":"uint256"}],"name":"transferToOther","outputs":[],"payable":true,"stateMutability":"payable","type":"function"},{"inputs":[],"payable":false,"stateMutability":"nonpayable","type":"constructor"},{"anonymous":false,"inputs":[{"indexed":false,"name":"user","type":"address"},{"indexed":false,"name":"orderId","type":"string"}],"name":"event_bet","type":"event"}]
   ```

   ​	找到event_bet这个event

   ```json
   {
   	"anonymous": false,
   	"inputs": [{
   		"indexed": false,
   		"name": "user",
   		"type": "address"
   	}, {
   		"indexed": false,
   		"name": "orderId",
   		"type": "string"
   	}],
   	"name": "event_bet",
   	"type": "event"
   }
   ```
   
   根据event定义和合约地址,我们可以新建一个事件监听的过滤器
   
2. 订阅事件

   根据websocket地址和过滤条件,就可以监听指定的事件

   ```java
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
   ```

3. 数据回调处理

   ​	订阅消息的回调处理需要实现 LogsListener这个接口的三个方法,分别是
   void onError(Exception e),
   
   void onLogs(Log log)和
   void onClose(int i, String s, boolean b)，
   在onLogs中可以处理接受到的事件log
   
   ```java
   package org.lkchain.sample;
   
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
   
       private static final Logger logger = LoggerFactory.getLogger(MyListener.class);
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
   
   ```
   
   ​	订阅收到的事件数据截图如下
   
   ![data](https://github.com/jr-go/demo/raw/master/img/data.png)
   
   #### 四 合约调用
   
    合约的调用需要使用rpc服务,调用合约时,需要根据要调用的function和参数生成合约调用时需要的payload。也可以使用其它的
   
    - 写合约
      
        写合约会改变合约状态,所以需要手续费,有的还需要带上链克
        
        我们以享云链的剪刀石头布_V1.0这个合约为例
        
        合约abi
        
        ```json
        [{"constant":false,"inputs":[{"name":"amount","type":"uint256"}],"name":"withdraw","outputs":[],"payable":true,"stateMutability":"payable","type":"function"},{"constant":true,"inputs":[],"name":"owner","outputs":[{"name":"","type":"address"}],"payable":false,"stateMutability":"view","type":"function"},{"constant":false,"inputs":[{"name":"_t","type":"uint256"},{"name":"orderId","type":"string"}],"name":"bet","outputs":[],"payable":true,"stateMutability":"payable","type":"function"},{"constant":true,"inputs":[],"name":"t","outputs":[{"name":"","type":"uint256"}],"payable":false,"stateMutability":"view","type":"function"},{"constant":false,"inputs":[{"name":"other","type":"address"},{"name":"amount","type":"uint256"}],"name":"transferToOther","outputs":[],"payable":true,"stateMutability":"payable","type":"function"},{"inputs":[],"payable":false,"stateMutability":"nonpayable","type":"constructor"},{"anonymous":false,"inputs":[{"indexed":false,"name":"user","type":"address"},{"indexed":false,"name":"orderId","type":"string"}],"name":"event_bet","type":"event"}]
        ```
        
        以bet为例
        
        ```json
        {
        	"constant": false,
        	"inputs": [{
        		"name": "_t",
        		"type": "uint256"
        	}, {
        		"name": "orderId",
        		"type": "string"
        	}],
        	"name": "bet",
        	"outputs": [],
        	"payable": true,
        	"stateMutability": "payable",
        	"type": "function"
        }
        ```
        
        根据方法定义,我们可以构造调用合约的data,然后调用合约
        
        ```java
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
        
                    String contractAddr = "0xfe8f4e74254cb69e2656f82b0925759b44eec0ba";
                    //执行合约时带两个链克
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
        ```
        
        执行结果截图
        
        ![receipt](https://github.com/jr-go/demo/raw/master/img/receipt.png)
        
        
   
   - 读合约
   

以链克打卡合约https://explorer.lkchain.org/#/address/0x5b9a5641db0dff5dd750e02979294498012c55fa

为例，我们找一个stateMutability值为view的函数getOrder,view表示这个方法是查看合约数据的。

```json
   {
	"constant": true,
   	"inputs": [{
		"name": "_gameID",
   		"type": "uint256"
	}, {
   		"name": "_orderID",
		"type": "bytes16"
   	}],
	"name": "getOrder",
   	"outputs": [{
		"name": "",
   		"type": "address"
   	}, {
   		"name": "",
   		"type": "uint256"
   	}, {
   		"name": "",
   		"type": "uint256"
   	}, {
   		"name": "",
   		"type": "uint256[]"
   	}],
   	"payable": false,
   	"stateMutability": "view",
   	"type": "function"
   }
```

   根据abi定义我们知道这个方法需要_gameID和_orderID两个入参和4个返回参数。接下来,我们可以在享云链浏览器中找到两个可用的gameid和orderid。

   打开浏览器,找到打卡合约,找一笔带有链克的交易

   ![order](https://github.com/jr-go/demo/raw/master/img/hash.png)

   点击交易hash进入详情页

   ![dataparse](https://github.com/jr-go/demo/raw/master/img/dataparse.png)

   找到数据输入的地方,点击数据解析,就可以看到这笔合约交易的orderid和gameid

   ![origindata](https://github.com/jr-go/demo/raw/master/img/origindata.png)

   拿到orderid和gameid后,我们就可以生成调用的getOrder函数的payload

   合约查询demo

   ```java
   private void queryContractDemo() throws Exception{
           Transfer transfer = new Transfer(rpcURL);
           //定义入参	
           List<Type> inputParameters = Arrays.asList(
                   new Uint256(1),
                   new Bytes16("35650b131d7b45c99138278ba2a238b1".getBytes()));
           //定义返回参数
           List<TypeReference<?>> outputParameters = Arrays.asList(
                   new TypeReference<Address>(){},
                   new TypeReference<Uint256>(){},
                   new TypeReference<Uint256>(){},
                   new TypeReference<DynamicArray<Uint256>>(){}
                   );
   
           Function func = new Function("getOrder",inputParameters,outputParameters);
           //生成合约调用的payload
           String data = FunctionEncoder.encode(func);
   
           String contractAddr = "0x5b9a5641db0dff5dd750e02979294498012c55fa";
           ResponseModel model = transfer.getRpc().EthCall(contractAddr,data);
           //根据func定义解析查询到的数据
       	String rawInput = model.getResult().toString();
           List returnData = FunctionReturnDecoder.decode(rawInput, func.getOutputParameters());
           ListIterator<Type> iterator = returnData.listIterator();
           System.out.println("查询结果数据解析");
           while (iterator.hasNext()) {
               Type a = iterator.next();
               System.out.println(a.getTypeAsString() + ":" + a.getValue().toString());
           }
       }
   ```

查询结果截图
       
![queryresult](https://github.com/jr-go/demo/raw/master/img/queryresult.png)

   示例中查询的这一笔交易订单是在1794099块打包进区块的,所以要等到节点同步到这个区块之后,我们才可以在链上去执行eth_call查询对应的数据
     

   #### 五 普通转账

   普通链克转账,调用transfer函数

   ```java
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
               //从account给0x7b8b0cf6f0f3a83cff0291d3482d2179d8b1588c转1链克
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
   ```

   #### 六 其它rpc请求

   除了常用的合约调用和转账之外,也可以查看github上的其它rpc接口定义,来构造对应请求

   rpc接口定义

   [https://github.com/lianxiangcloud/linkchain/blob/release/v0.1.3/doc/RPC%E6%9C%8D%E5%8A%A1.md](https://github.com/lianxiangcloud/linkchain/blob/release/v0.1.3/doc/RPC服务.md)

   以eth_getBlockByNumber为例

   ```java
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
   ```
 

   **依赖的sdk jar文件在项目根目录下的/lib/sdk-1.0.0-SNAPSHOT-jar-with-dependencies.jar**
   
   **sdk源码在https://github.com/jr-go/sdk**