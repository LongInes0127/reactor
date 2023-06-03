package com.atunk.reactor;

import java.io.IOException;
import java.nio.channels.Selector;

/**
 * @description:
 * @author: 张军
 * @email: 23166321@qq.com
 * @date: 2023/6/3 11:50
 */
public abstract class ServerThread extends Thread {

	protected Selector selector;

	public ServerThread(String name) {
		super(name);

		try {
			// 为了保证线程一启动就能获取到selector，这里进行初始化
			this.selector = Selector.open();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 唤醒selector
	 */
	protected void wakeupSelector() {
		this.selector.wakeup();
	}

	/**
	 * 检查selector是否打开，如果是则关闭
	 */
	protected void closeSelector() {
		if (this.selector.isOpen()) {
			try {
				this.selector.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
