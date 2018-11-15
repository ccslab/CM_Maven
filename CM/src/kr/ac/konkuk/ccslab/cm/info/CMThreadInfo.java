package kr.ac.konkuk.ccslab.cm.info;

import java.util.concurrent.ExecutorService;

public class CMThreadInfo {
	private ExecutorService m_executorService;

	public CMThreadInfo()
	{
		m_executorService = null;
	}

	///// set/get methods
	
	public void setExecutorService(ExecutorService es)
	{
		m_executorService = es;
	}
	
	public ExecutorService getExecutorService()
	{
		return m_executorService;
	}


}
