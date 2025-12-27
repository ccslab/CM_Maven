package kr.ac.konkuk.ccslab.cm.manager;

import kr.ac.konkuk.ccslab.cm.entity.CMObject;

/**
 * The CMServiceManager class represents a CM service object.
 * 
 * An application or other CM objects can use a CM service with this class. 
 * 
 * @author CCSLab, Konkuk University
 *
 */
public abstract class CMServiceManager extends CMObject {

	public CMServiceManager()
	{
		m_nType = -1;
	}
	
}
