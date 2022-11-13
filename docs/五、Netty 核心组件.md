# 四、Netty 核心组件

> 在讲完网络编程、I/O 多路复用、Reactor 线程模型这些底层的知识之后，这一篇终于开始讲 Netty。本篇主讲 Netty 的整体架构，并对 Netty 的核心组件做了一些简单的介绍。

## 前言
在学习的一项技术或者做技术调研的时候，通常首先会关注：它能做什么？解决了什么问题？有哪些应用场景？然后是技术的大体框架，先对技术构建一个全局观，在开始上手的时候，不宜陷入琐碎的技术细节，避免从入门到弃坑。这篇文章使从 Netty 网络应用开发的逻辑架构视角切入，介绍 Netty 的核心组件，以及它们在 Netty 框架中的作用。

## Netty 的逻辑框架
Netty 官网给出了一张 Netty 的整体框架，划分为 Core 核心层、Protocol Support 协议支持层和 Transport Service 传输服务层。

![](https://files.mdnice.com/user/22260/18213ee2-c980-4e12-8bf3-f91eed71818a.png)

这里并不会按上面的整体框架介绍，而是按照组件之间的功能逻辑框架进行介绍。 Netty 分为网络通信层、事件调度层、服务编排层，各层职责划分明确、功能界限清晰。下面对各层的功能和组件进行一个简单的介绍：

![Netty 逻辑框架](https://files.mdnice.com/user/22260/17437f7d-7b79-4760-86fc-51a0fceddd83.jpg)

### 网络通信层
网络通信层的职责是执行网络 I/O 的操作，主要的两个组件是 Bootstrap 和 Channel。
#### Bootstrap
Bootstrap 是引导的意思，Bootstrap 组件负责 Netty 程序的启动、初始化、服务器连接，按照角色又分为 BootStrap 和 ServerBootStrap，BootStrap 和 ServerBootStrap 分别负责客户端和服务端的启动。
- BootStrap：

BootStrap 是客户端持有的程序启动引导类，持有一个 EventLoopGroup（EventLoopGroup 先简单理解为线程池，下文会介绍），用来配置客户端启动信息，启动客户端、以及连接服务器。

- ServerBootStrap：

ServerBootStrap 是服务端持有的程序启动引导类，持有两个 EventLoopGroup —— boss 和 work（对应主从 Reactor 线程池），用来配置服务端启动信息，比如：线程池、编解码器、TCP连接设置，启动服务端并阻塞监听 I/O 事件。

#### Channel
Channel 的作用类似 Java BIO 的 Socket，用于网络 I/O 操作，如 register、bind、connect、read、write、flush 等。Channel 是网络通信的载体，提供了与底层 Socket 交互的能力。

Netty 自己实现的 Channel 是以 JDK NIO Channel 为基础的，相比较于 JDK NIO，Netty 的 Channel 提供了更高层次的抽象，同时屏蔽了底层 Socket 的复杂性，赋予了 Channel 更加强大的功能，使用 Netty 时基本不需要再与 Java Socket 类直接打交道。

### 事件调度层
Netty 通过 Reactor 线程模型对各类事件进行聚合处理，通过 Selector 主循环线程集成多种事件（I/O 事件、信号事件、定时事件等），实际的业务处理逻辑是交由服务编排层中相关的 Handler 完成。

事件调度层的核心组件包括 EventLoopGroup、EventLoop。
- EventLoopGroup & EventLoop

EventLoopGroup 本质是一个线程池，主要负责接收 I/O 请求，并分配线程执行处理请求。


![](https://files.mdnice.com/user/22260/324bc690-7755-4148-bd76-0d917917c751.jpg)

从上图中，我们可以总结出 EventLoopGroup、EventLoop、Channel 的几点关系。

1. 一个 EventLoopGroup 往往包含一个或者多个 EventLoop。EventLoop 用于处理 Channe 生命周期内的所有 I/O 事件，如 accept、connect、read、write 等 I/O 事件。
2. EventLoop 同一时间会与一个线程绑定，每个 EventLoop 负责处理多个 Channel。
3. 每新建一个 Channel，EventLoopGroup 会选择一个 EventLoop 与其绑定。该 Channel 在生命周期内都可以对 EventLoop 进行多次绑定和解绑。

EventLoopGroup 是 Netty 的核心处理引擎，那么 EventLoopGroup 和上一篇提到的 Reactor 线程模型到底是什么关系呢？其实 EventLoopGroup 是 Netty Reactor 线程模型的具体实现方式，Netty 通过创建不同的 EventLoopGroup 参数配置，就可以支持 Reactor 的三种线程模型：

- 单线程模型：EventLoopGroup 只包含一个 EventLoop，Boss 和 Worker 使用同一个EventLoopGroup；

- 多线程模型：EventLoopGroup 包含多个 EventLoop，Boss 和 Worker 使用同一个EventLoopGroup；

- 主从多线程模型：EventLoopGroup 包含多个 EventLoop，Boss 是主 Reactor，Worker 是从 Reactor，它们分别使用不同的 EventLoopGroup，主 Reactor 负责新的网络连接 Channel 创建，然后把 Channel 注册到从 Reactor。

在介绍完事件调度层之后，可以说 Netty 的发动机已经转起来了，事件调度层负责监听网络连接和读写操作，然后触发各种类型的网络事件，需要一种机制管理这些错综复杂的事件，并有序地执行，接下来就该介绍 Netty 服务编排层中的核心组件的职责。

### 服务编排层

服务编排层的职责是负责组装各类服务，它是 Netty 的核心处理链，用以实现网络事件的动态编排和有序传播。

服务编排层的核心组件包括 ChannelPipeline、ChannelHandler、ChannelHandlerContext。
#### ChannelHandler

ChannelHandler 是进行开发业务代码的组件，数据的编解码工作以及其他转换工作实际都是通过 ChannelHandler 处理的。站在开发者的角度，最需要关注的就是 ChannelHandler，我们很少会直接操作 Channel，都是通过 ChannelHandler 间接完成。如果把一次完成的网络请求-响应看做一条流水线生产产品，那么 ChannelHandler 就是对产品进行加工的一个一个车间。

#### ChannelPipeline
ChannelPipeline 是 Netty 的核心编排组件，负责组装各种 ChannelHandler，实际数据的编解码以及加工处理操作都是由 ChannelHandler 完成的。ChannelPipeline 可以理解为ChannelHandler 的实例列表——内部通过双向链表将不同的 ChannelHandler 链接在一起。当 I/O 读写事件触发时，ChannelPipeline 会依次调用 ChannelHandler 列表对 Channel 的数据进行拦截和处理。还是类比工厂，ChannelPipeline 就像工厂车间的流水线，将一个个车间串联起来，使产品在各个车间流转。

![](https://files.mdnice.com/user/22260/43e36eb4-56dc-4ecc-ad66-ee4b35b0c296.png)

![](https://files.mdnice.com/user/22260/f02019b9-c21b-43fb-8d5a-aa7462003fdd.png)

#### ChannelHanlerContext
下图描述了 Channel 与 ChannelPipeline 的关系，从图中可以看出，每创建一个 Channel 都会绑定一个新的 ChannelPipeline，ChannelPipeline 中每加入一个 ChannelHandler 都会绑定一个 ChannelHandlerContext。由此可见，ChannelPipeline、ChannelHandlerContext、ChannelHandler 三个组件的关系是密切相关的，那么每个 ChannelHandler 绑定ChannelHandlerContext 的作用是什么呢？

![](https://files.mdnice.com/user/22260/4b031e4f-3569-480a-ae04-5fd62f39f498.jpg)

ChannelHandlerContext 用于保存 ChannelHandler 上下文，通过 ChannelHandlerContext 我们可以知道 ChannelPipeline 和 ChannelHandler 的关联关系。ChannelHandlerContext 可以实现 ChannelHandler 之间的交互，ChannelHandlerContext 包含了 ChannelHandler 生命周期的所有事件，如 connect、bind、read、flush、write、close 等。此外，你可以试想这样一个场景，如果每个 ChannelHandler 都有一些通用的逻辑需要实现，没有 ChannelHandlerContext 这层模型抽象，你是不是需要写很多相同的代码呢？

## Netty 组件关系梳理

当你了解每个 Netty 核心组件的概念后。你会好奇这些组件之间如何协作？结合客户端和服务端的交互流程，通过下图，完整地梳理一遍 Netty 内部逻辑的流转。

![](https://files.mdnice.com/user/22260/22098b64-35f4-43b3-848f-0488499e0d94.jpg)

服务端启动初始化时有 Boss EventLoopGroup 和 Worker EventLoopGroup 两个组件，其中 Boss 负责监听网络连接事件。当有新的网络连接事件到达时，则将 Channel 注册到 Worker EventLoopGroup。

Worker EventLoopGroup 会被分配一个 EventLoop 负责处理该 Channel 的读写事件。每个 EventLoop 都是单线程的，通过 Selector 进行事件循环。

当客户端发起 I/O 读写事件时，服务端 EventLoop 会进行数据的读取，然后通过 Pipeline 触发各种监听器进行数据的加工处理。

客户端数据会被传递到 ChannelPipeline 的第一个 ChannelInboundHandler 中，数据处理完成后，将加工完成的数据传递给下一个 ChannelInboundHandler。

当数据写回客户端时，会将处理结果在 ChannelPipeline 的 ChannelOutboundHandler 中传播，最后到达客户端。

以上便是 Netty 各个组件的整体交互流程，你只需要对每个组件的工作职责有所了解，在心中可以串成一条流水线。


![](https://files.mdnice.com/user/22260/f43dde8b-1461-4a3f-8db0-8d5105bc6abe.png)
