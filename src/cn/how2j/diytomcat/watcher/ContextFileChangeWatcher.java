package cn.how2j.diytomcat.watcher;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

import cn.how2j.diytomcat.catalina.Context;
import cn.how2j.diytomcat.catalina.Host;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.LogFactory;

//检测Context是否发生了变化，用于热部署
public class ContextFileChangeWatcher {

	private WatchMonitor monitor;
	private boolean stop = false;

	public ContextFileChangeWatcher(Context context) {
		this.monitor = WatchUtil.createAll(context.getDocBase(), Integer.MAX_VALUE, new Watcher() {
			private void dealWith(WatchEvent<?> event) {
				synchronized (ContextFileChangeWatcher.class) {
					String fileName = event.context().toString();
					if (stop)
						return;
					if (fileName.endsWith(".jar") || fileName.endsWith(".class") || fileName.endsWith(".xml")) {
						stop = true;
						LogFactory.get().info(ContextFileChangeWatcher.this + " 检测到了Web应用下的重要文件变化 {} " , fileName);
						context.reload();
					}


				}
			}

			@Override
			public void onCreate(WatchEvent<?> event, Path currentPath) {
				dealWith(event);
			}

			@Override
			public void onModify(WatchEvent<?> event, Path currentPath) {
				dealWith(event);

			}

			@Override
			public void onDelete(WatchEvent<?> event, Path currentPath) {
				dealWith(event);

			}

			@Override
			public void onOverflow(WatchEvent<?> event, Path currentPath) {
				dealWith(event);
			}

		});

		this.monitor.setDaemon(true);
	}

	public void start() {
		monitor.start();
	}

	public void stop() {
		monitor.close();
	}
}
