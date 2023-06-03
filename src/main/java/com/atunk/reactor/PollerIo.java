package com.atunk.reactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @description:
 * @author: 张军
 * @email: 23166321@qq.com
 * @date: 2023/6/3 12:06
 */
public class PollerIo extends ServerThread {
	private final Server server;

	private final Queue<SocketChannel> acceptedQueue;

	public PollerIo(String name, Server server) {
		super(name);
		this.server = server;
		this.acceptedQueue = new LinkedBlockingQueue<>();
	}

	public void addAcceptedChannel(SocketChannel socketChannel) {
		this.acceptedQueue.offer(socketChannel);
		wakeupSelector();
	}

	@Override
	public void run() {
		while (!server.stopped) {
			doSelect();
			doAcceptedConnections();
		}

		closeSelector();

	}

	/**
	 *
	 */
	private void doAcceptedConnections() {
		SocketChannel socketChannel;
		while (!server.stopped && (socketChannel = acceptedQueue.poll()) != null) {
			try {
				socketChannel.register(selector, SelectionKey.OP_READ);
			} catch (ClosedChannelException e) {
				throw new RuntimeException(e);
			}
		}

	}

	private void doSelect() {
		try {
			selector.select();
			Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
			while (iterator.hasNext()) {
				SelectionKey key = iterator.next();
				iterator.remove();

				if (!key.isValid()) {
					continue;
				}

				if (key.isReadable() || key.isWritable()) {
					handleIo(key);
				}
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void handleIo(SelectionKey key) {
		if (key.isReadable()) {
			SocketChannel socketChannel = (SocketChannel) key.channel();
			ByteBuffer readBuffer = ByteBuffer.allocate(1024);
			try {
				socketChannel.read(readBuffer);

				// 读取到的数据提交到业务线程池
				BuTask task = new BuTask(readBuffer, socketChannel);
				server.addBuTask(task);

			} catch (IOException e) {
				try {
					socketChannel.close();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
		} else if (key.isWritable()) {

		}
	}
}
