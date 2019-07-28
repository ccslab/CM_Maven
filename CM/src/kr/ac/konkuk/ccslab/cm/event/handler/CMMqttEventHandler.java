package kr.ac.konkuk.ccslab.cm.event.handler;

import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

import kr.ac.konkuk.ccslab.cm.entity.CMList;
import kr.ac.konkuk.ccslab.cm.entity.CMMqttSession;
import kr.ac.konkuk.ccslab.cm.entity.CMMqttTopicQoS;
import kr.ac.konkuk.ccslab.cm.entity.CMMqttWill;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEvent;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventCONNACK;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventCONNECT;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBACK;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBLISH;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBREC;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBREL;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMMqttInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMDBManager;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import kr.ac.konkuk.ccslab.cm.manager.CMMqttManager;

/**
 * The CMMqttEventHandler class represents a CM event handler that processes 
 * incoming CM MQTT events.
 * @author CCSLab, Konkuk University
 *
 */
public class CMMqttEventHandler extends CMEventHandler {

	public CMMqttEventHandler(CMInfo cmInfo)
	{
		super(cmInfo);
		m_nType = CMInfo.CM_MQTT_EVENT_HANDLER;
	}
	
	@Override
	public boolean processEvent(CMEvent event) {
		
		boolean bRet = false;
		CMMqttEvent mqttEvent = (CMMqttEvent)event;
		
		switch(event.getID())
		{
		case CMMqttEvent.CONNECT:
			bRet = processCONNECT(mqttEvent);
			break;
		case CMMqttEvent.CONNACK:
			bRet = processCONNACK(mqttEvent);
			break;
		case CMMqttEvent.PUBLISH:
			bRet = processPUBLISH(mqttEvent);
			break;
		case CMMqttEvent.PUBACK:
			bRet = processPUBACK(mqttEvent);
			break;
		case CMMqttEvent.PUBREC:
			bRet = processPUBREC(mqttEvent);
			break;
		case CMMqttEvent.PUBREL:
			bRet = processPUBREL(mqttEvent);
			break;
		case CMMqttEvent.PUBCOMP:
			bRet = processPUBCOMP(mqttEvent);
			break;
		case CMMqttEvent.SUBSCRIBE:
			bRet = processSUBSCRIBE(mqttEvent);
			break;
		case CMMqttEvent.SUBACK:
			bRet = processSUBACK(mqttEvent);
			break;
		case CMMqttEvent.UNSUBSCRIBE:
			bRet = processUNSUBSCRIBE(mqttEvent);
			break;
		case CMMqttEvent.UNSUBACK:
			bRet = processUNSUBACK(mqttEvent);
			break;
		case CMMqttEvent.PINGREQ:
			bRet = processPINGREQ(mqttEvent);
			break;
		case CMMqttEvent.PINGRESP:
			bRet = processPINGRESP(mqttEvent);
			break;
		case CMMqttEvent.DISCONNECT:
			bRet = processDISCONNECT(mqttEvent);
			break;
		default:
			System.err.println("CMMqttEventHandler.processEvent(), invalid event id: ("
					+event.getID()+")!");
			return false;
		}
		return bRet;
	}

	private boolean processCONNECT(CMMqttEvent event)
	{
		// initialization
		CMMqttEventCONNECT conEvent = (CMMqttEventCONNECT)event;
		CMMqttInfo mqttInfo = m_cmInfo.getMqttInfo();
		CMMqttEventCONNACK ackEvent = new CMMqttEventCONNACK();
		CMUser myself = m_cmInfo.getInteractionInfo().getMyself();
		boolean bConnAckFlag = false;
		byte returnCode = 0;	// connection success
		boolean bRet = false;
		
		// print the received CONNECT event
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventHandler.processCONNECT(): "+conEvent.toString());
		}
		
		// validate CONNECT packet format and set return code
		// If the format is invalid, the server responds with the failure return code.
		// In MQTT v3.1.1, if the format is invalid, the server disconnects with the client.
		returnCode = validateCONNECT(conEvent);
		if( returnCode != 0 ) // if the validation failed,
		{
			ackEvent.setSender(myself.getName());
			ackEvent.setReturnCode((byte)6);
			bRet = CMEventManager.unicastEvent(ackEvent, conEvent.getSender(), m_cmInfo);
			return bRet;
		}
		
		// to determine ack flag
		CMMqttSession mqttSession = mqttInfo.getMqttSessionHashtable().get(conEvent.getSender());
		if(conEvent.isCleanSessionFlag())
			bConnAckFlag = false;
		else if( mqttSession != null )
			bConnAckFlag = true;
		else
			bConnAckFlag = false;
		
		// to process clean-session flag
		if(mqttSession != null && conEvent.isCleanSessionFlag())
		{
			mqttInfo.getMqttSessionHashtable().remove(conEvent.getSender());
			mqttSession = null;
		}
		if(mqttSession == null)
		{
			mqttSession = new CMMqttSession();
			mqttInfo.getMqttSessionHashtable().put(conEvent.getSender(), mqttSession);
		}
		
		// to process will flag
		if(conEvent.isWillFlag())
		{
			CMMqttWill will = new CMMqttWill();
			will.setWillMessage(conEvent.getWillMessage());
			will.setWillTopic(conEvent.getWillTopic());
			will.setWillQoS(conEvent.getWillQoS());
			will.setWillRetain(conEvent.isWillRetainFlag());
			mqttSession.setMqttWill(will);
		}
		
		// to process keep-alive value (not yet)
		if(conEvent.getKeepAlive() > 0)
		{
			// will be incorporated with the CM keep-alive strategy
		}
		
		// to send CONNACK event
		ackEvent.setSender(myself.getName());
		ackEvent.setConnAckFlag(bConnAckFlag);
		ackEvent.setReturnCode(returnCode);
		bRet = CMEventManager.unicastEvent(ackEvent, conEvent.getSender(), m_cmInfo);
		
		return bRet;
	}
	
	// return value 0 : success
	// return value 1 : wrong packet level
	// return value 2 : client ID not allowed
	// return value 3 : MQTT service unavailable
	// return value 4 : user name and password malformed
	// return value 5 : client not authorized to connect
	// return value 6 : other failure (not defined in MQTT v3.1.1)
	private byte validateCONNECT(CMMqttEventCONNECT conEvent)
	{
		////////////////// validate fixed header

		// validate packet type
		if( conEvent.getPacketType() != 1 )
		{
			System.err.println("CMMqttEventHandler.validateCONNECT(), packet type is not 1! : "
					+conEvent.getPacketType());
			return 6;
		}
		// validate flag
		if( conEvent.getFlag() != 0 )
		{
			System.err.println("CMMqttEventHandler.validateCONNECT(), fixed header flag is not 0 : "
					+conEvent.getFlag());
			return 6;
		}
		
		////////////////// validate variable header

		// validate protocol name
		if(!conEvent.getProtocolName().contentEquals("MQTT"))
		{
			System.err.println("CMMqttEventHandler.validateCONNECT(), protocol name is not MQTT! : "
					+conEvent.getProtocolName());
			return 6;
		}
		// validate protocol level
		if( conEvent.getProtocolLevel() != 4 )
		{
			System.err.println("CMMqttEventHandler.validateCONNECT(), protocol level is not 4 : "
					+conEvent.getProtocolLevel());
			return 1;
		}
		// validate will flag and will qos
		if( conEvent.isWillFlag() && (conEvent.getWillQoS() > 2 || conEvent.getWillQoS() < 0 ))
		{
			System.err.println("CMMqttEventHandler.validateCONNECT(), wrong will QoS : "
					+conEvent.getWillQoS());
			return 6;
		}
		if( !conEvent.isWillFlag() && (conEvent.getWillQoS() != 0) )
		{
			System.err.println("CMMqttEventHandler.validateCONNECT(), will flag is not set, "
					+"but will QoS is not 0!");
			return 6;
		}
		// validate will retain
		if( !conEvent.isWillFlag() && conEvent.isWillRetainFlag() )
		{
			System.err.println("CMMqttEventHandler.validateCONNECT(), will flag is not set, "
					+"but will retain is set!");
			return 6;
		}
		
		/////////////////////// validate payload
		
		// validate client ID. In CM, client ID is the same as user name
		if( !conEvent.getClientID().equals(conEvent.getUserName()))
		{
			System.err.println("CMMqttEventHandler.validateCONNECT(), client ID("+conEvent.getClientID()
				+") and user name("+conEvent.getUserName()+") are different!");
			return 2;
		}
		// validate user name and flag
		String strUserName = conEvent.getUserName();
		if( conEvent.isUserNameFlag() && (strUserName == null || strUserName.isEmpty()))
		{
			System.err.println("CMMqttEventHandler.validateCONNECT(), user name flag is set, "
					+"but there is no user name!");
			return 4;
		}
		if( !conEvent.isUserNameFlag() && strUserName != null && !strUserName.isEmpty())
		{
			System.err.println("CMMqttEventHandler.validateCONNECT(), user name flag is not set, "
					+"but user name is not null and not empty ("+conEvent.getUserName()+")!");
			return 4;
		}
		// validate password and flag
		String strPassword = conEvent.getPassword();
		if( !conEvent.isPasswordFlag() && strPassword != null && !strPassword.isEmpty())
		{
			System.err.println("CMMqttEventHandler.validateCONNECT(), password flag is not set, "
					+"but password is not null and not empty ("+conEvent.getPassword()+")!");
			return 4;
		}
		if( conEvent.isPasswordFlag() && (strPassword == null || strPassword.isEmpty()))
		{
			System.err.println("CMMqttEventHandler.validateCONNECT(), password flag is set, "
					+"but there is no password");
			return 4;
		}
		if( !conEvent.isUserNameFlag() && conEvent.isPasswordFlag() )
		{
			System.err.println("CMMqttEventHandler.validateCONNECT(), user name flag is not set, "
					+"but password flag is set ("+conEvent.getPassword()+")!");
			return 4;
		}
		// authenticate user name and password
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		if( confInfo.isLoginScheme() && !CMDBManager.authenticateUser(strUserName, strPassword, m_cmInfo) )
		{
			System.err.println("CMMqttEventHandler.validateCONNECT(), user authentication failed! "
					+"user("+strUserName+"), password("+strPassword+")");
			return 5;
		}

		return 0;	// success
	}
	
	private boolean processCONNACK(CMMqttEvent event)
	{
		CMMqttEventCONNACK connackEvent = (CMMqttEventCONNACK)event;
		if(CMInfo._CM_DEBUG)
			System.out.println("CMMqttEventHandler.processCONNACK(): "+connackEvent.toString());
		return true;
	}
	
	private boolean processPUBLISH(CMMqttEvent event)
	{
		CMMqttEventPUBLISH pubEvent = (CMMqttEventPUBLISH)event;
		boolean bRet = false;
		// print received event
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventHandler.processPUBLISH(): received "
					+pubEvent.toString());
		}
		
		// response
		switch(pubEvent.getQoS())
		{
		case 0:
			// do nothing
			break;
		case 1:
			// send PUBACK
			bRet = sendPUBACK(pubEvent);
			if(!bRet)
				return false; 
			break;
		case 2:
			// store received PUBLISH event
			bRet = storeRecvPUBLISH(pubEvent);
			if(!bRet)
				return false;
			// send PUBREC
			bRet = sendPUBREC(pubEvent);
			if(!bRet)
				return false;
			break;
		default:
			System.err.println("CMMqttEventHandler.processPUBLISH(), wrong QoS: "
					+pubEvent.getQoS());
			return false;
		}
		
		// if CM is server, it forwards the event to the subscribers
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		if(confInfo.getSystemType().equals("SERVER"))
		{
			String strTopicName = pubEvent.getTopicName();
			CMMqttManager mqttManager = (CMMqttManager)m_cmInfo.getServiceManagerHashtable()
					.get(CMInfo.CM_MQTT_MANAGER);
			// to search for clients with the matching topic filters
			CMMqttInfo mqttInfo = m_cmInfo.getMqttInfo();
			Hashtable<String, CMMqttSession> sessionHashtable = 
					mqttInfo.getMqttSessionHashtable();
			Set<String> keys = sessionHashtable.keySet();
			for(String key : keys)
			{
				CMMqttSession session = sessionHashtable.get(key);
				Vector<CMMqttTopicQoS> filterVector = session.getSubscriptionList().getList();
				boolean bFound = false;
				for(int i = 0; i < filterVector.size(); i++)
				{
					String strFilter = filterVector.elementAt(i).getTopic();
					bFound = isTopicMatch(strTopicName, strFilter);
					if(bFound)
					{
						mqttManager.publish(key, pubEvent.getPacketID(), pubEvent.getTopicName(),
								pubEvent.getAppMessage(), filterVector.elementAt(i).getQoS(),
								false, pubEvent.isRetainFlag());
					}
				}
			}
		}
		
		return bRet;
	}
	
	private boolean storeRecvPUBLISH(CMMqttEventPUBLISH pubEvent)
	{
		// to get session information
		CMMqttSession session = null;
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		String strSysType = confInfo.getSystemType();
		CMMqttInfo mqttInfo = m_cmInfo.getMqttInfo();
		if(strSysType.equals("CLIENT"))
		{
			session = mqttInfo.getMqttSession();
		}
		else if(strSysType.equals("SERVER"))
		{
			session = mqttInfo.getMqttSessionHashtable().get(pubEvent.getSender());
		}
		if(session == null)
		{
			System.err.println("CMMqttEventHandler.storeRecvPUBLISH(): session is null!");
			return false;
		}
		
		// to add event to recvUnackEventList
		CMList<CMMqttEvent> recvUnackEventList = session.getRecvUnAckEventList();
		boolean bRet = recvUnackEventList.addElement(pubEvent);
		if(bRet && CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventHandler.storeRecvPUBLISH(): Ok "+pubEvent.toString());
		}
		if(!bRet)
		{
			System.err.println("CMMqttEventHandler.storeRecvPUBLISH(): FAILED! "+pubEvent.toString());
			return false;
		}
		
		return bRet;
	}
	
	private boolean sendPUBACK(CMMqttEventPUBLISH pubEvent)
	{
		// initialize PUBACK event
		CMMqttEventPUBACK pubackEvent = new CMMqttEventPUBACK();
		// set sender (in CM event header)
		CMUser myself = m_cmInfo.getInteractionInfo().getMyself();
		pubackEvent.setSender(myself.getName());
		// set fixed header in the CMMqttEVentPUBACK constructor
		// set variable header
		pubackEvent.setPacketID(pubEvent.getPacketID());
		
		// send ack to the PUBLISH sender
		boolean bRet = false;
		String strPubSender = pubEvent.getSender();
		bRet = CMEventManager.unicastEvent(pubackEvent, strPubSender, m_cmInfo);
		if(bRet && CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventHandler.sendPUBACK(): Ok "+pubackEvent.toString());
		}
		if(!bRet)
		{
			System.err.println("CMMqttEventHandler.sendPUBACK(): FAILED! "+pubackEvent.toString());
			return false;
		}

		return bRet;
	}
	
	private boolean sendPUBREC(CMMqttEventPUBLISH pubEvent)
	{
		// initialize PUBREC event
		CMMqttEventPUBREC recEvent = new CMMqttEventPUBREC();
		// set sender (in CM event header)
		CMUser myself = m_cmInfo.getInteractionInfo().getMyself();
		recEvent.setSender(myself.getName());
		// set fixed header in the CMMqttEventPUBREC constructor
		// set variable header
		recEvent.setPacketID(pubEvent.getPacketID());
		
		// send to the PUBLISH sender
		boolean bRet = false;
		String strPubSender = pubEvent.getSender();
		bRet = CMEventManager.unicastEvent(recEvent, strPubSender, m_cmInfo);
		if(bRet && CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventHandler.sendPUBREC(): Ok "+recEvent.toString());
		}
		if(!bRet)
		{
			System.err.println("CMMqttEventHandler.sendPUBREC(): FAILED! "+recEvent.toString());
			return false;
		}
		
		return bRet;
	}
	
	public static boolean isTopicMatch(String strTopic, String strFilter)
	{
		strTopic = strTopic.trim();
		strFilter = strFilter.trim();
		
		// get tokens
		String[] topicTokens = strTopic.split("/");
		String[] filterTokens = strFilter.split("/");
		
		int i = 0;
		for(i = 0; i < filterTokens.length; i++)
		{
			String filterToken = filterTokens[i];
			if(filterToken.equals("#") && i == filterTokens.length - 1)
				return true;
			
			// # of topic level is less than # of filter level
			if( i == topicTokens.length )
				return false;
			
			String topicToken = topicTokens[i];
			if(filterToken.equals(topicToken) || filterToken.equals("+"))
				continue;
			else
				return false;
		}
		
		// # of topic level is greater than # of topic filter
		if( i < topicTokens.length )
			return false;
		
		return true;
	}
	
	private boolean processPUBACK(CMMqttEvent event)
	{
		// A receiver of PUBLISH event with QoS 1 sends the PUBACK event.
		CMMqttEventPUBACK pubackEvent = (CMMqttEventPUBACK)event;
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventHandler.processPUBACK(), received "
					+pubackEvent.toString());
		}
		
		// to get session information
		CMMqttSession session = null;
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		String strSysType = confInfo.getSystemType();
		CMMqttInfo mqttInfo = m_cmInfo.getMqttInfo();
		
		if(strSysType.equals("CLIENT"))
		{
			session = mqttInfo.getMqttSession();
		}
		else if(strSysType.equals("SERVER"))
		{
			session = mqttInfo.getMqttSessionHashtable().get(pubackEvent.getSender());
		}
		else
		{
			System.err.println("CMMqttEventHandler.processPUBACK(), wrong system type! ("
					+strSysType+")");
			return false;
		}
		
		if(session == null)
		{
			System.err.println("CMMqttEventHandler.processPUBACK(), session is null!");
			return false;
		}
		
		// to remove the corresponding PUBLISH event (with the same packet ID) 
		// from the sent-unack-event list
		int nPacketID = pubackEvent.getPacketID();
		boolean bRet = session.removeSentUnAckEvent(nPacketID);
		if(bRet && CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventHandler.processPUBACK(), deleted PUBLISH event "
					+"with packet ID ("+nPacketID+").");
		}
		if(!bRet)
		{
			System.err.println("CMMqttEventHandler.processPUBACK(), error to delete PUBLISH "
					+"event with packet ID ("+nPacketID+")!");
			return false;
		}
		
		return true;
	}
	
	private boolean processPUBREC(CMMqttEvent event)
	{
		// A receiver of PUBLISH event with QoS 2 sends the PUBACK event.
		CMMqttEventPUBREC recEvent = (CMMqttEventPUBREC)event;
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventHandler.processPUBREC(), received "
					+recEvent.toString());
		}

		// to get session information
		CMMqttSession session = null;
		CMConfigurationInfo confInfo = m_cmInfo.getConfigurationInfo();
		String strSysType = confInfo.getSystemType();
		CMMqttInfo mqttInfo = m_cmInfo.getMqttInfo();
		
		if(strSysType.equals("CLIENT"))
		{
			session = mqttInfo.getMqttSession();
		}
		else if(strSysType.equals("SERVER"))
		{
			session = mqttInfo.getMqttSessionHashtable().get(recEvent.getSender());
		}
		else
		{
			System.err.println("CMMqttEventHandler.processPUBREC(), wrong system type! ("
					+strSysType+")");
			return false;
		}
		
		if(session == null)
		{
			System.err.println("CMMqttEventHandler.processPUBREC(), session is null!");
			return false;
		}

		// to remove PUBLISH event in the session (with the same packet ID)
		int nPacketID = recEvent.getPacketID();
		boolean bRet = session.removeSentUnAckEvent(nPacketID);
		if(bRet && CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventHandler.processPUBREC(), deleted PUBLISH event "
					+"with packet ID ("+nPacketID+").");
		}
		if(!bRet)
		{
			System.err.println("CMMqttEventHandler.processPUBREC(), error to delete "
					+"PUBLISH event with packet ID ("+nPacketID+")!");
			return false;
		}
		
		// to add PUBREC event to the session
		bRet = session.addRecvUnAckEvent(recEvent);
		if(bRet && CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventHandler.processPUBREC(), added PUBREC event "
					+"with packet ID ("+nPacketID+").");
		}
		if(!bRet)
		{
			System.err.println("CMMqttEventHandler.processPUBREC(), error to delete "
					+"PUBREC event with packet ID ("+nPacketID+")!");
			return false;
		}
		
		// make and send PUBREL event
		CMMqttEventPUBREL relEvent =  new CMMqttEventPUBREL();
		CMUser myself = m_cmInfo.getInteractionInfo().getMyself();
		// set sender (CM event header)
		relEvent.setSender(myself.getName());
		// set fixed header in the CMMqttEventPUBREL constructor
		// set variable header
		relEvent.setPacketID(nPacketID);
		
		bRet = CMEventManager.unicastEvent(relEvent, recEvent.getSender(), m_cmInfo);
		if(bRet && CMInfo._CM_DEBUG)
		{
			System.out.println("CMMqttEventHandler.processPUBREC(), sent "
					+relEvent.toString());
		}
		if(!bRet)
		{
			System.err.println("CMMqttEventHandler.processPUBREC(), error to send "
					+relEvent.toString());
			return false;
		}

		return true;
	}
	
	private boolean processPUBREL(CMMqttEvent event)
	{
		
		// from here
		return false;
	}
	
	private boolean processPUBCOMP(CMMqttEvent event)
	{
		return false;
	}
	
	private boolean processSUBSCRIBE(CMMqttEvent event)
	{
		return false;
	}
	
	private boolean processSUBACK(CMMqttEvent event)
	{
		return false;
	}
	
	private boolean processUNSUBSCRIBE(CMMqttEvent event)
	{
		return false;
	}
	
	private boolean processUNSUBACK(CMMqttEvent event)
	{
		return false;
	}
	
	private boolean processPINGREQ(CMMqttEvent event)
	{
		return false;
	}
	
	private boolean processPINGRESP(CMMqttEvent event)
	{
		return false;
	}
	
	private boolean processDISCONNECT(CMMqttEvent event)
	{
		return false;
	}
	
}
