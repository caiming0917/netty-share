# 三、Reactor 线程模型
> Netty 采用 Reactor 模型实现网络 I/O 事件处理。不知道有没有小伙伴在准备面试的时候，对 Reactor 总感觉一知半解。如果你有同样的问题，这篇文章也许能解答一些你的疑惑。

小伙伴们应该都听过大名鼎鼎的 Reactor 线程模型，Reactor 在 Redis、Nginx、Netty 等许多知名开源软件都有应用和落地。本篇文章主讲 Reactor，请思考一下问题：
- 什么是 Reactor 呢？
- Reactor 和 I/O 多路复用有什么关系？
- Reactor 有些实现方式呢？

## Reactor 介绍

### 什么是 Reactor
Reactor 对应的叫法有：反应器模式、分发器模式(Dispather)、通知者模式，是对**事件处理流程的一种模式抽象**。

### Reactor 与 I/O 多路复用
Reactor 是对 I/O 多路复用模式的一种封装，在网络编程的场景下指的是：对各种网络 I/O 事件的反应处理。 I/O 多路复用模式结合线程池，就是 Reactor 模式的基本设计思想。Reactor 模型有2个重要的组件：
- `Reactor`：Reactor 在一个单独或固定多个线程运行，负责监听和响应各种IO事件，例如连接事件、读写事件等，当监听到有一个新的事件发生时，就会交给相应的Handler去处理。Reactor 好比是一家电话公司的电话咨询前台，接听来自客户的电话然后将线路转接给对应的业务员。
- `Handler`：处理特定的 I/O 事件的执行者。好比是实际为客户服务的业务员。

Reactor 通过调度事件对应的 handler 处理和响应 I/O 事件，handler 执行非阻塞的业务处理操作。


## Reactor 线程模型

Reactor 线程模型的实现有三种：

- 单 Reactor 单线程模型
- 单 Reactor 多线程模型
- 主从 Reactor 模型

### 单 Reactor 单线程模型
单 Reactor 单线程模型，正如其名：在单 Reactor 单线程模型中，所有 I/O 操作（包括连接建立、数据读写、事件分发等），都是由一个线程完成的。具体流程是：

Reactor 通过调用网络编程系统调用 select 监听网络 I/O 事件，收到事件后通过 dispatch 进行分发。如果是连接事件通过 acceptor 调用 accept 处理连接请求，然后创建一个 handler 对象完成连接的后续业务处理。如果不是连接请求，Reactor 会分发对应的 handler 对象处理。handler 会完成 read → 业务处理 → send 的完成流程。

打个比方：
>一家饭店，只有一个服务员张三。顾客从进店迎接、点餐、记录点餐菜品、上菜、客户用餐期间的需求、结账送客，都由张三一个人做。弊端是：张三只要服务完当前顾客的需要，才能去服务下一个顾客。

- 《Scalable I/O in Java》 单 Reactor 单线程模型图示：
  ![](https://files.mdnice.com/user/22260/90fbb53f-6e40-4025-a700-3fe11b09f5ef.jpg)

- 单 Reactor 单线程模型图示：
  ![](https://files.mdnice.com/user/22260/5c42d697-c05c-4b0b-9ced-2fd93e36c8a3.png)


单线程模型的优点是：
- 逻辑简单，只有一个线程，不用考虑多线程带来的并发问题；

缺陷也十分明显：
- 线程支持处理的连接数非常有限，无法充分发挥多核 CPU 的性能，性能方面有明显瓶颈；
- 多个事件被同时触发时，只要有一个事件没有处理完，其他后面的事件就无法执行，这就会造成消息积压及请求超时；
- 在处理 I/O 事件时，Select 无法同时处理连接建立、事件分发等操作；
- 可靠性问题：线程意外终止或进入死循环，就会导致网络通信不可用。

使用场景：

- 客户端连接有限，业务处理非常快速，比如： Redis 在业务处理的时间复杂度 O(1) 的情况。

Netty 对 Reactor 模式提供了方便的自定义实现。下面的代码实例中，`EventLoopGroup` 是一个线程池，构造方法中指定线程池只创建一个线程。group 对象可以简单认为是一个线程对象，负责处理所有网络 I/O 事件。
~~~java
EventLoopGroup group = new NioEventLoopGroup(1);
ServerBootstrap b = new ServerBootstrap();
b.group(group)
~~~

### 单 Reactor 多线程模型

由于单线程模型有性能方面的瓶颈，多线程模型作为解决方案就应运而生了。多线程的例子好比：
> 饭店发现张三一个人忙不过来，于是又招了几个服务员。张三只负责迎客、点餐和结账送客。客户用餐期间的所有需求都由其他服务员服务。
- 《Scalable I/O in Java》 单 Reactor 多线程模型图示：
  ![](https://files.mdnice.com/user/22260/09e96478-2caf-4042-ae98-734723ac64e9.jpg)
- 单 Reactor 多线程模型图示：

相比于单 Reactor 单线程模型，单 Reactor 多线程模型将业务逻辑交给多个线程进行处理。除此之外，多线程模型其他的操作与单线程模型是类似的，例如读取数据依然保留了串行化的设计。当客户端有数据发送至服务端时，Select 会监听到可读事件，数据读取完毕后提交到业务线程池中并发处理。

单 Reactor 多线程模型的优点：

- 充分利用多核 CPU 的处理能力

缺点：

- 多线程数据共享比较复杂，要考虑并发问题；
- 只有一个 Reactor 线程处理所有网络 I/O 事件，高并发场景下存在性能瓶颈。

Netty 实现一个多 Reactor 线程模型，代码和单 Reactor 几乎一样，但在创建 EventLoopGroup 对象时没有指定参数。不指定参数 Netty 会默认创建一个线程数是两倍 cpu 核数的线程池，用来处理网络 I/O 事件。

~~~java
EventLoopGroup group = new NioEventLoopGroup();
ServerBootstrap b = new ServerBootstrap();
b.group(group);
~~~

### 主从 Reactor 多线程模型

主从多线程 Reactor 模型由多个 Reactor 线程组成，每个 Reactor 线程都有独立的 Selector 对象。MainReactor 仅负责处理客户端连接的 Accept 事件，连接建立成功后将新创建的连接对象注册至 SubReactor。再由 SubReactor 分配线程池中的 I/O 线程与其连接绑定，它将负责连接生命周期内所有的 I/O 事件。主从 Reactor 多线程模型好比：
> 饭店发现用餐高峰期，迎客、点餐、结账送客，张三一个人还是忙不过来。于是饭店又招了几个前台，张三只负责迎客，然后就把顾客交接给前台，前台负责顾客下单点餐、记录点餐菜品、和结账送客，上菜和就餐期间的需求，由前台前台交给其他服务员。这样饭店用餐高峰期能招待很多顾客了。
- 《Scalable I/O in Java》 主从 Reactor 多线程模型图示：
  ![](https://files.mdnice.com/user/22260/3482c94a-d735-4f3a-b905-70a794819bf4.jpg)
- 主从 Reactor 多线程模型图示：
  ![](https://files.mdnice.com/user/22260/0aff29f8-9093-4631-b9c5-6e44d97954c0.png)

优点：
- 支持并发网络连接数量大

缺点：
- 编程复杂度较高


Netty 推荐使用主从多线程 Reactor 模型，这样就可以轻松达到成千上万规模的客户端连接。在海量客户端并发请求的场景下，主从多线程模式甚至可以适当增加 SubReactor 线程的数量，从而利用多核能力提升系统的吞吐量。下面是 Netty 实现主从多线程模型 Reactor 的代码，bossGroup 是主 Reactor 线程，workerGroup 是从 Reactor 线程。
~~~java
EventLoopGroup bossGroup = new NioEventLoopGroup();
EventLoopGroup workerGroup = new NioEventLoopGroup();
ServerBootstrap b = new ServerBootstrap();
b.group(bossGroup, workerGroup)
~~~

## 总结

上述三种 Reactor 线程模型，再结合它们各自的架构图，我们能大致总结出 Reactor 线程模型运行机制的四个步骤，分别为连接注册、事件轮询、事件分发、任务处理，如下图所示。

![](https://files.mdnice.com/user/22260/90cd8933-e054-49ea-919e-a78cc555dcc1.jpg)

- 连接注册：网络连接(socket)建立后，注册至 Reactor 线程中的 Selector 选择器。
- 事件轮询：轮询 Selector 选择器中已注册的所有连接(socket)的 I/O 事件。
- 事件分发：为准备就绪的 I/O 分配相应的处理线程。
- 任务处理：Reactor 线程还负责任务队列中的非 I/O 任务，每个业务线程从各自维护的任务队列中取出任务异步执行。

## 参考
[IO多路复用和Reactor模型](https://blog.csdn.net/qq_42290561/article/details/125859442)

[尚硅谷Netty视频教程](https://www.bilibili.com/video/BV1DJ411m7NR/)

[Netty 核心原理剖析与 RPC 实践](https://kaiwu.lagou.com/course/courseInfo.htm?courseId=516#/detail/pc?id=4917)

Doug Lea: Scalable I/O in Java