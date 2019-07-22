package kr.ac.konkuk.ccslab.cm.event.handler;

import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMServiceManager;

/**
 * The CMEventHandler class represents a CM event handler that processes incoming events.
 * @author CCSLab, Konkuk University
 *
 */
public abstract class CMEventHandler extends CMServiceManager {
	
	public CMEventHandler(CMInfo cmInfo)
	{
		super(cmInfo);
	}
	
	public abstract boolean processEvent(CMEvent event);
}
