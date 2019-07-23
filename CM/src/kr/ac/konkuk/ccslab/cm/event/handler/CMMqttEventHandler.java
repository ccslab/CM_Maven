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
		// TODO Auto-generated method stub
		return false;
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
