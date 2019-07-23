package kr.ac.konkuk.ccslab.cm.event.handler;

import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEvent;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventCONNECT;
import kr.ac.konkuk.ccslab.cm.event.mqttevent.CMMqttEventPUBLISH;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

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
		return false;
	}
	
	private boolean validateCONNECT(CMMqttEventCONNECT conEvent)
	{
		return false;
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
