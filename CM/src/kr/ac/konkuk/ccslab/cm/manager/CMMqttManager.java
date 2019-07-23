package kr.ac.konkuk.ccslab.cm.manager;

import kr.ac.konkuk.ccslab.cm.entity.CMList;
import kr.ac.konkuk.ccslab.cm.entity.CMMqttTopicQoS;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventCONNECT;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

/**
 * The CMMqttManager class represents a CM service object with which an application can 
 * request Mqtt service.
 * @author CCSLab, Konkuk University
 *
 */
public class CMMqttManager extends CMServiceManager {

	public CMMqttManager(CMInfo cmInfo)
	{
		super(cmInfo);
	}
	
	public boolean connect(String strClientID)
	{
		boolean bRet = false;
		bRet = connect(null, null, false, (byte)0, false, false, 0);
		return bRet;
	}
	
	public boolean connect(String strWillTopic, String strWillMessage, boolean bWillRetain,
			byte willQoS, boolean bWillFlag, boolean bCleanSession, int nKeepAlive)
	{
		// check if the client has logged in to the default server.
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		if(confInfo.getSystemType().equals("SERVER"))
		{
			System.err.println("CMMqttManager.connect(), the system type is SERVER!");
			return false;
		}
		
		CMUser myself = m_cmInfo.getInteractionInfo().getMyself();
		int nState = myself.getState();
		if(nState == CMInfo.CM_INIT || nState == CMInfo.CM_CONNECT)
		{
			System.err.println("CMMqttManager.connect(), you must log in to the default "
					+ "server!");
			return false;
		}
		
		// make CONNECT event
		CMMqttEventCONNECT conEvent = new CMMqttEventCONNECT();
		// set variable header
		conEvent.setUserNameFlag(true);	// to use CM user name
		conEvent.setPasswordFlag(true);	// to use CM user password
		conEvent.setWillRetainFlag(bWillRetain);
		conEvent.setWillQoS(willQoS);
		conEvent.setWillFlag(bWillFlag);
		conEvent.setCleanSessionFlag(bCleanSession);
		conEvent.setKeepAlive(nKeepAlive);
		// set payload
		conEvent.setClientID(myself.getName());	// = CM user name
		conEvent.setWillTopic(strWillTopic);
		conEvent.setWillMessage(strWillMessage);
		conEvent.setUserName(myself.getName());
		conEvent.setPassword(myself.getPasswd());
		
		// process the clean-session flag
		
		
		// from here
		return true;
	}
	
	public boolean publish(String strTopic, String strMsg)
	{
		return true;
	}
	
	public boolean publish(int nPacketID, String strTopic, String strMsg, byte qos)
	{
		return true;
	}
	
	public boolean publish(String strReceiver, int nPacketID, String strTopic,
			String strMsg, byte qos, boolean bDupFlag, boolean bRetainFlag)
	{
		return true;
	}
	
	public boolean subscribe(String strTopicFilter, byte qos)
	{
		return true;
	}
	
	public boolean subscribe(String nPacketID, String strTopicFilterString, byte qos)
	{
		return true;
	}
	
	public boolean subscribe(int nPacketID, CMList<CMMqttTopicQoS> topicQoSList)
	{
		return true;
	}
	
	public boolean unsubscribe(String strTopic)
	{
		return true;
	}
	
	public boolean unsubscribe(int nPacketID, String strTopic)
	{
		return true;
	}
	
	public boolean unsubscribe(int nPacketID, CMList<String> topicList)
	{
		return true;
	}
	
	public boolean requestPing()
	{
		return true;
	}
	
	public boolean disconnect()
	{
		return true;
	}
}
