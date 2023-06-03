package com.atunk.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * @description:
 * @author: 张军
 * @email: 23166321@qq.com
 * @date: 2023/6/3 12:05
 */
public class Accepter extends ServerThread {
	private final Server server;
	private final Collection<PollerIo> pollerIos;
	private final ServerSocketChannel serverSocketChannel;

	/**
	 * pollerIoIterator是为了能够简单的获取一个pollerIo。
	 */
	private Iterator<PollerIo> pollerIoIterator;

	public Accepter(String name, Server server, Set<PollerIo> pollerIos) throws IOException {
		super(name);
		this.server = server;
		// 将pollerIos转换为不可变集合
		this.pollerIos = Collections.unmodifiableList(new ArrayList<>(pollerIos));
		this.pollerIoIterator = pollerIos.iterator();
		this.serverSocketChannel = ServerSocketChannel.open();
		// 设置为非阻塞
		this.serverSocketChannel.configureBlocking(false);
		this.serverSocketChannel.bind(new InetSocketAddress(server.getPort()));
		SelectionKey selectionKey = this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
	}

	@Override
	public void run() {
		while (!server.stopped && !serverSocketChannel.socket().isClosed()) {
			try {
				selector.select();
				Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
				while (iterator.hasNext()) {
					SelectionKey key = iterator.next();
					iterator.remove();

					if (!key.isValid()) {
						continue;
					}

					if (key.isAcceptable()) {
						doAccept(key);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		closeSelector();
	}

	private void doAccept(SelectionKey key) {
		try {
			SocketChannel socketChannel = serverSocketChannel.accept();
			socketChannel.configureBlocking(false);

			if (!pollerIoIterator.hasNext()) {
				pollerIoIterator = pollerIos.iterator();
			}

			PollerIo pollerIo = pollerIoIterator.next();
			pollerIo.addAcceptedChannel(socketChannel);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
