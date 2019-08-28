package kr.ac.konkuk.ccslab.cm.info;

import java.util.concurrent.ExecutorService;

public class CMThreadInfo {
	private ExecutorService m_executorService;

	public CMThreadInfo()
	{
		m_executorService = null;
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


}
