package kr.ac.konkuk.ccslab.cm.thread;

import java.util.Hashtable;
import java.util.Vector;

import kr.ac.konkuk.ccslab.cm.entity.CMServer;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPINGREQ;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;

public class CMClientKeepAliveTask implements Runnable {

	private CMInfo m_cmInfo;
	
	public CMClientKeepAliveTask(CMInfo cmInfo)
	{
		m_cmInfo = cmInfo;
	}
	
	@Override
	public void run()
	{
		CMUser myself = m_cmInfo.getInteractionInfo().getMyself();
		Hashtable<String, Long> myLastEventTransTimeHashtable = myself.getMyLastEventTransTimeHashtable();
		CMServer defServer = m_cmInfo.getInteractionInfo().getDefaultServerInfo();
		Long lMyLastEventTransTimeToDefServer = myLastEventTransTimeHashtable
				.get(defServer.getServerName());
		if(lMyLastEventTransTimeToDefServer == null)
		{
			System.err.println("CMClientKeepAliveTask.run(), my last event transmission "
					+"time to the default server is null!");
			lMyLastEventTransTimeToDefServer = 0L;
		}
		int nKeepAliveTime = myself.getKeepAliveTime();
		
		long lCurTime = System.currentTimeMillis();
		
		if( (myself.getState() >= CMInfo.CM_LOGIN) && 
				((lCurTime - lMyLastEventTransTimeToDefServer) > nKeepAliveTime) )
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMClientKeepAliveTask.run(): "
						+ "(current time - last event-transmission time to def server = "
						+lMyLastEventTransTimeToDefServer+"), (my keep-alive time = "
						+nKeepAliveTime+")");
			}
			CMMqttEventPINGREQ reqPingEvent = new CMMqttEventPINGREQ();
			reqPingEvent.setSender(myself.getName());
			CMEventManager.unicastEvent(reqPingEvent, defServer.getServerName(), m_cmInfo);
		}
		
		Vector<CMServer> addServerList = m_cmInfo.getInteractionInfo().getAddServerList();
		for(CMServer addServer : addServerList)
		{
			if(addServer.getClientState() >= CMInfo.CM_LOGIN) 
			{
				// a client needs to manage the last event-trans time per server!!
				Long lMyLastEventTransTimeToAddServer = myLastEventTransTimeHashtable
						.get(addServer.getServerName());
				if(lMyLastEventTransTimeToAddServer == null)
				{
					System.err.println("CMClientKeepAliveTask.run(), my last event "
							+"transmission time to server("+addServer.getServerName()
							+") is null!");
					lMyLastEventTransTimeToAddServer = 0L;
				}
				
				if(lCurTime - lMyLastEventTransTimeToAddServer > nKeepAliveTime)
				{
					CMMqttEventPINGREQ reqPingEvent = new CMMqttEventPINGREQ();
					reqPingEvent.setSender(myself.getName());
					CMEventManager.unicastEvent(reqPingEvent, addServer.getServerName(), m_cmInfo);
				}
			}
		}
	}

}
