package kr.ac.konkuk.ccslab.cm.event.handler;

import kr.ac.konkuk.ccslab.cm.event.CMEvent;

/**
 * The CMAppEventHandler interface represents an event handler of a CM application.
 * <p>The application should implements this interface so that it can receive incoming CM events.
 * 
 * @author CCSLab, Konkuk University
 *
 */
public interface CMAppEventHandler {
	void processEvent(CMEvent cme);
}
