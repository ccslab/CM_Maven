package kr.ac.konkuk.ccslab.cm.info;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

public class CMThreadInfo {
	private ExecutorService m_executorService;
	private ScheduledExecutorService m_schedExecutorService;

	public CMThreadInfo()
	{
		m_executorService = null;
		m_schedExecutorService = null;
	}

	///// set/get methods
	
	public synchronized void setExecutorService(ExecutorService es)
	{
		m_executorService = es;
	}
	
	public synchronized ExecutorService getExecutorService()
	{
		return m_executorService;
	}
	
	public synchronized void setScheduledExecutorService(ScheduledExecutorService ses)
	{
		m_schedExecutorService = ses;
	}
	
	public synchronized ScheduledExecutorService getScheduledExecutorService()
	{
		return m_schedExecutorService;
	}


}
