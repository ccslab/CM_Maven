package kr.ac.konkuk.ccslab.cm.event.handler;

import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEvent;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventCONNACK;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventCONNECT;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBLISH;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMMqttInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMDBManager;

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
		boolean bConnAckFlag = false;
		byte returnCode = 0;	// connection success
		
		// validate CONNECT packet format
		// If the format is invalid, the server responds with the failure return code.
		// In MQTT v3.1.1, if the format is invalid, the server disconnects with the client.
		
		// from here
		
		
		return false;
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
		return false;
	}
	
	private boolean processPUBLISH(CMMqttEvent event)
	{
		return false;
	}
	
	private boolean sendPUBACK(CMMqttEventPUBLISH pubEvent)
	{
		return false;
	}
	
	private boolean sendPUBREC(CMMqttEventPUBLISH pubEvent)
	{
		return false;
	}
	
	private boolean processPUBACK(CMMqttEvent event)
	{
		return false;
	}
	
	private boolean processPUBREC(CMMqttEvent event)
	{
		return false;
	}
	
	private boolean processPUBREL(CMMqttEvent event)
	{
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
