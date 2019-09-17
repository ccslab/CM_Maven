package kr.ac.konkuk.ccslab.cm.thread;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import kr.ac.konkuk.ccslab.cm.entity.CMList;
import kr.ac.konkuk.ccslab.cm.entity.CMMember;
import kr.ac.konkuk.ccslab.cm.entity.CMServer;
import kr.ac.konkuk.ccslab.cm.entity.CMUnknownChannelInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPINGREQ;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMConfigurator;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import kr.ac.konkuk.ccslab.cm.manager.CMInteractionManager;

public class CMServerKeepAliveTask implements Runnable {

	private CMInfo m_cmInfo;
	
	public CMServerKeepAliveTask(CMInfo cmInfo)
	{
		m_cmInfo = cmInfo;
	}
	
	@Override
	public void run()
	{
		long lCurTime = System.currentTimeMillis();
		long lElapsedTime = 0;
		int nKeepAliveTime = 0;
		int i = 0;
		
		// for each login user
		CMMember loginMembers = m_cmInfo.getInteractionInfo().getLoginUsers();
		Vector<CMUser> loginUsersVector = loginMembers.getAllMembers();
		for(i = loginUsersVector.size()-1; i >= 0; i--)
		{
			CMUser user = loginUsersVector.elementAt(i);
			lElapsedTime = lCurTime - user.getLastEventTransTime();
			nKeepAliveTime = user.getKeepAliveTime();
			if(lElapsedTime/1000.0 > nKeepAliveTime*1.5)
			{
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMServerKeepAliveTime.run(), for user("
							+user.getName()+"), elapsed time("+(lElapsedTime/1000.0)
							+"), keep-alive time*1.5("+(nKeepAliveTime*1.5)+").");
				}
				CMInteractionManager.disconnectBadClientByServer(user, m_cmInfo);
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMServerKeepAliveTask.run(), disconnect user("
							+user.getName()+").");
					System.out.println("CMServerKeepAliveTask.run(), # login users: "
							+loginUsersVector.size());
				}
			}
		}
		
		// for each unknown channel
		CMList<CMUnknownChannelInfo> unchList = m_cmInfo.getCommInfo()
				.getUnknownChannelInfoList();
		Vector<CMUnknownChannelInfo> unchVector = unchList.getList();
		Iterator<CMUnknownChannelInfo> iter = unchVector.iterator();
		while(iter.hasNext())
		{
			CMUnknownChannelInfo unch = iter.next();
			lElapsedTime = lCurTime - unch.getLastEventTransTime();
			nKeepAliveTime = m_cmInfo.getConfigurationInfo().getKeepAliveTime();
			if(lElapsedTime/1000.0 > nKeepAliveTime*1.5)
			{
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMServerKeepAliveTask.run(), for unknown-channel, "
							+"elapsed time("+(lElapsedTime/1000.0)+"), keep-alive time*1.5("
							+(nKeepAliveTime*1.5)+").");
				}
				
				try {
					unch.getUnknownChannel().close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				iter.remove();
				
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMServerKeepAliveTask.run(), remove from "
							+ "unknown-channel list: "+unch);
					System.out.println("# unknown-channel list: "+unchVector.size());
				}
			}
		}
		
		// process of the default server
		if(CMConfigurator.isDServer(m_cmInfo))
		{
			// for each additional server
			Vector<CMServer> addServerVector = m_cmInfo.getInteractionInfo()
					.getAddServerList();
			for(i = addServerVector.size()-1; i >= 0; i--)
			{
				CMServer addServer = addServerVector.elementAt(i);
				lElapsedTime = lCurTime - addServer.getLastEventTransTime();
				nKeepAliveTime = addServer.getKeepAliveTime();
				if(lElapsedTime/1000.0 > nKeepAliveTime*1.5)
				{
					CMInteractionManager.disconnectBadAddServerByDefaultServer(addServer, 
							m_cmInfo);
				}
			}
		}
		else	// process of an additional server
		{
			CMUser myself = m_cmInfo.getInteractionInfo().getMyself();
			if(myself.getState() >= CMInfo.CM_LOGIN)
			{
				String strDefServer = m_cmInfo.getInteractionInfo().getDefaultServerInfo()
						.getServerName();
				long lMyLastEventTransTime = myself.getMyLastEventTransTimeHashtable()
						.get(strDefServer);
				lElapsedTime = lCurTime - lMyLastEventTransTime;
				nKeepAliveTime = m_cmInfo.getConfigurationInfo().getKeepAliveTime();
				
				if(lElapsedTime/1000.0 > nKeepAliveTime)
				{
					if(CMInfo._CM_DEBUG)
					{
						System.out.println("CMServerKeepAliveTime.run(): cur time("
								+lCurTime+"), my last event-trans time("
								+lMyLastEventTransTime+"), elapsed time("+lElapsedTime
								+"), keep-alive time("+nKeepAliveTime+")");
					}
					CMMqttEventPINGREQ reqPingEvent = new CMMqttEventPINGREQ();
					reqPingEvent.setSender(myself.getName());
					CMEventManager.unicastEvent(reqPingEvent, strDefServer, m_cmInfo);
				}
			}
		} // else
		
	}	// run()

}
