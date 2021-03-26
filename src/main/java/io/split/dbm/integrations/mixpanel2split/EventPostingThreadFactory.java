package io.split.dbm.integrations.mixpanel2split;

import java.util.concurrent.ThreadFactory;

public class EventPostingThreadFactory implements ThreadFactory {

	public Thread newThread(Runnable r) {
		Thread t = new Thread(r);
		t.setDaemon(true);
		return t;
	}

}
