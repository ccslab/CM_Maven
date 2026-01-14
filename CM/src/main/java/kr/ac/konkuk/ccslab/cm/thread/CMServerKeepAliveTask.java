package kr.ac.konkuk.ccslab.cm.thread;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import kr.ac.konkuk.ccslab.cm.entity.CMList;
import kr.ac.konkuk.ccslab.cm.entity.CMMember;
import kr.ac.konkuk.ccslab.cm.entity.CMServer;
import kr.ac.konkuk.ccslab.cm.entity.CMUnknownChannelInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPINGREQ;
import kr.ac.konkuk.ccslab.cm.info.CMCommInfo;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMConfigurator;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import kr.ac.konkuk.ccslab.cm.manager.CMInteractionManager;

public class CMServerKeepAliveTask implements Runnable {

	private static final Logger LOG = Logger.getLogger(CMServerKeepAliveTask.class.getName());
	
	public CMServerKeepAliveTask()
	{

	}
	
	@Override
	public void run()
	{
		CMInfo cmInfo = CMInfo.getInstance();
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		if(confInfo.getLogLevel() == 0)
			LOG.setLevel(Level.SEVERE);

		long lCurTime = System.currentTimeMillis();
		long lElapsedTime = 0;
		int nKeepAliveTime = 0;
		int i = 0;
		
		// for each login user
		CMMember loginMembers = CMInteractionInfo.getInstance().getLoginUsers();
		// [Modified] CMMember now manages users with Hashtable<String, List<CMUser>>.
		// To safely iterate and remove users (which modifies the underlying collection),
		// we first copy all users into a temporary Vector.
		Hashtable<String, List<CMUser>> loginUsersHashtable = loginMembers.getAllMembers();
		Vector<CMUser> loginUsersVector = new Vector<>();
		for(List<CMUser> list : loginUsersHashtable.values()) {
			loginUsersVector.addAll(list);
		}
		// Iterate in reverse order to safely handle removals (same rationale as original code)
		for(i = loginUsersVector.size()-1; i >= 0; i--)
		{
			CMUser user = loginUsersVector.elementAt(i);
			lElapsedTime = lCurTime - user.getLastEventTransTime();
			nKeepAliveTime = user.getKeepAliveTime();
			if(lElapsedTime/1000.0 > nKeepAliveTime*1.5)
			{
				LOG.info("for user("+user.getName()+"), elapsed time("
						+(lElapsedTime/1000.0)+"), keep-alive time*1.5("
						+(nKeepAliveTime*1.5)+").");
				// This method will remove the user from the original loginMembers
				CMInteractionManager.disconnectBadClientByServer(user);

				LOG.info("disconnect user("+user.getName()+"), # login users: "
						+loginUsersVector.size());
			}
		}
		
		// for each unknown channel
		CMList<CMUnknownChannelInfo> unchList = CMCommInfo.getInstance()
				.getUnknownChannelInfoList();
		Vector<CMUnknownChannelInfo> unchVector = unchList.getList();
		Iterator<CMUnknownChannelInfo> iter = unchVector.iterator();
		while(iter.hasNext())
		{
			CMUnknownChannelInfo unch = iter.next();
			lElapsedTime = lCurTime - unch.getLastEventTransTime();
			nKeepAliveTime = CMConfigurationInfo.getInstance().getKeepAliveTime();
			if(lElapsedTime/1000.0 > nKeepAliveTime*1.5)
			{
				LOG.info("for unknown-channel, elapsed time("+(lElapsedTime/1000.0)
						+"), keep-alive time*1.5("+(nKeepAliveTime*1.5)+")");
				
				try {
					unch.getUnknownChannel().close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				iter.remove();
				
				LOG.info("removed from unknown-channel list: "+unch.getUnknownChannel()+"\n"
						+"channel hash code: "+unch.getUnknownChannel().hashCode()+"\n"
						+"# unknown-channel list: "+unchVector.size());
			}
		}
		
		// process of the default server
		if(CMConfigurator.isDServer())
		{
			// for each additional server
			Vector<CMServer> addServerVector = CMInteractionInfo.getInstance()
					.getAddServerList();
			for(i = addServerVector.size()-1; i >= 0; i--)
			{
				CMServer addServer = addServerVector.elementAt(i);
				lElapsedTime = lCurTime - addServer.getLastEventTransTime();
				nKeepAliveTime = addServer.getKeepAliveTime();
				if(lElapsedTime/1000.0 > nKeepAliveTime*1.5)
				{
					LOG.info("for add-server("+addServer.getServerName()+"), cur time("
							+lCurTime+"), last event-trans time("
							+addServer.getLastEventTransTime()+"), \n"
							+"elapsed time("+(lElapsedTime/1000.0)+"), keep-alive time("
							+nKeepAliveTime+").");

					CMInteractionManager.disconnectBadAddServerByDefaultServer(addServer
					);
					
					LOG.info("disconnected add-server("+addServer.getServerName()+").\n"
							+"# add-servers: "+addServerVector.size());
				}
			}
		}
		else	// process of an additional server
		{
			CMUser myself = CMInteractionInfo.getInstance().getMyself();
			if(myself.getState() >= CMInfo.CM_LOGIN)
			{
				String strDefServer = CMInteractionInfo.getInstance().getDefaultServerInfo()
						.getServerName();
				long lMyLastEventTransTime = myself.getMyLastEventTransTimeHashtable()
						.get(strDefServer);
				lElapsedTime = lCurTime - lMyLastEventTransTime;
				nKeepAliveTime = CMConfigurationInfo.getInstance().getKeepAliveTime();
				
				if(lElapsedTime/1000.0 > nKeepAliveTime)
				{
					LOG.info("cur time("+lCurTime+"), my last event-trans time("
							+lMyLastEventTransTime+"), \n"
							+ "elapsed time("+(lElapsedTime/1000.0)
							+"), keep-alive time("+nKeepAliveTime+")");

					CMMqttEventPINGREQ reqPingEvent = new CMMqttEventPINGREQ();
					reqPingEvent.setSender(myself.getName());
					CMEventManager.unicastEvent(reqPingEvent, strDefServer);
				}
			}
		} // else
		
	}	// run()

}
