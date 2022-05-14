package kr.ac.konkuk.ccslab.cm.info;

import java.util.concurrent.*;

public class CMThreadInfo {
	private ExecutorService executorService;
	private ScheduledExecutorService scheduledExecutorService;
	private ScheduledFuture<?> scheduledFuture;
	private long eventReceiverId;

	public CMThreadInfo()
	{
		executorService = null;
		scheduledExecutorService = null;
		scheduledFuture = null;
		eventReceiverId = -1;
	}

	///// set/get methods
	
	public synchronized void setExecutorService(ExecutorService es)
	{
		executorService = es;
	}
	
	public synchronized ExecutorService getExecutorService()
	{
		return executorService;
	}
	
	public synchronized void setScheduledExecutorService(ScheduledExecutorService ses)
	{
		scheduledExecutorService = ses;
	}
	
	public synchronized ScheduledExecutorService getScheduledExecutorService()
	{
		return scheduledExecutorService;
	}

	public synchronized void setScheduledFuture(ScheduledFuture<?> future)
	{
		scheduledFuture = future;
	}
	
	public synchronized ScheduledFuture<?> getScheduledFuture()
	{
		return scheduledFuture;
	}

	public synchronized long getEventReceiverId() {
		return eventReceiverId;
	}

	public synchronized void setEventReceiverId(long eventReceiverId) {
		this.eventReceiverId = eventReceiverId;
	}

	// Override method

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		if(executorService != null && executorService instanceof ThreadPoolExecutor) {
			ThreadPoolExecutor pool = (ThreadPoolExecutor) executorService;
			sb.append("---------- Thread pool info\n");
			sb.append("maximum pool size = "+ pool.getMaximumPoolSize()+"\n");
			sb.append("current number of threads = "+ pool.getPoolSize()+"\n");
			sb.append("core number of threads = "+ pool.getCorePoolSize()+"\n");
			sb.append("number of actively executing threads = "+ pool.getActiveCount()+"\n");
		}
		if(scheduledExecutorService != null && scheduledExecutorService instanceof ScheduledThreadPoolExecutor) {
			ScheduledThreadPoolExecutor pool = (ScheduledThreadPoolExecutor) scheduledExecutorService;
			sb.append("---------- Scheduled thread pool info\n");
			sb.append("maximum pool size = "+ pool.getMaximumPoolSize()+"\n");
			sb.append("current number of threads = "+ pool.getPoolSize()+"\n");
			sb.append("core number of threads = "+ pool.getCorePoolSize()+"\n");
			sb.append("number of actively executing threads = "+ pool.getActiveCount()+"\n");
			sb.append("----------\n");
		}

		return sb.toString();
	}

}
