package com.atunk.reactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

/**
 * @description:
 * @author: 张军
 * @email: 23166321@qq.com
 * @date: 2023/6/3 17:14
 */
public class BuTask implements Runnable{
	private ByteBuffer buffer;
	private final SocketChannel socketChannel;

	public BuTask(ByteBuffer readBuffer, SocketChannel socketChannel) {
		this.buffer = readBuffer;
		this.socketChannel = socketChannel;
	}

	@Override
	public void run() {
		buffer.flip();

		byte[] bytes = new byte[buffer.remaining()];
		buffer.get(bytes);
		buffer = null;

		String message = new String(bytes, Charset.defaultCharset());

		// TODO 业务处理逻辑
		System.out.println("服务端收到了的数据:" + message);

		// 向客户端写数据
		byte[] outs = "hello I am reactor".getBytes(Charset.defaultCharset());
		ByteBuffer writeBuffer = ByteBuffer.allocate(outs.length);
		writeBuffer.put(outs);

		writeBuffer.flip();
		try {
			socketChannel.write(writeBuffer);
		} catch (IOException e) {
			try {
				socketChannel.close();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}

	}
}
