package com.atunk.reactor;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @description:设计服务端的Server
 * @author: 张军
 * @email: 23166321@qq.com
 * @date: 2023/6/3 11:55
 */
public class Server {
	private int port;

	public int getPort() {
		return port;
	}

	/**
	 * 一个接收主线程接收客户端连接
	 */
	private Accepter accepter;

	/**
	 * 多个IO线程负责读写客户端连接
	 */
	private Set<PollerIo> pollerIos;

	/**
	 * 业务线程池，负责处理客户端的业务请求
	 */
	private ExecutorService businessExecutor;

	/**
	 * 服务端是否停止
	 */
	public volatile boolean stopped = false;

	public void init() {
		// TODO 按照常规来说应该读取配置文件，这里为了方便测试直接写死
		this.port = 9999;
		// io线程数，最少为4个，最多为CPU核心数的2倍
		int ioNumbers = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);

		pollerIos = new HashSet<>(ioNumbers);

		for (int i = 0; i < ioNumbers; i++) {
			// this代表Server，传递给PollerIo，PollerIo中可以通过this调用Server的方法
			PollerIo Io = new PollerIo("PollerIo-" + i, this);
			// 将PollerIo添加到pollerIos中
			this.pollerIos.add(Io);
		}

		try {
			this.accepter = new Accepter("accepter", this, this.pollerIos);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void start(){
		this.businessExecutor = new ThreadPoolExecutor(
				200,
				500,
				60,
				TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(10000)
		);
		// start io thread
		for (PollerIo pollerIO: this.pollerIos) {
			pollerIO.start();
		}
		// start acceptor thread
		accepter.start();
	}

	public void shutdown() {
		this.stopped = true;
		this.businessExecutor.shutdown();
	}

	public void addBuTask(BuTask task) {
		businessExecutor.execute(task);
	}

	public static void main(String[] args) {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Server server = new Server();
		server.init();
		server.start();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			server.shutdown();
			countDownLatch.countDown();
		}));

		try {
			countDownLatch.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
