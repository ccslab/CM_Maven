package kr.ac.konkuk.ccslab.cm.entity;

import java.util.Iterator;
import java.util.Vector;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

public class CMList<T> {
	private Vector<T> m_list;
	
	public CMList()
	{
		m_list = new Vector<T>();
	}
	
	public boolean addElement(T element)
	{
		if(m_list.contains(element))
		{
			System.err.println("CMList.addElement(): already exists !: "+element.toString());
			return false;
		}
		
		m_list.addElement(element);
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMList.addElement() done: "+element.toString());
			System.out.println("# current element: "+m_list.size());
		}
		
		return true;
	}
	
	public boolean removeElement(T element)
	{
		boolean bResult = false;
		
		bResult = m_list.removeElement(element);
		if(!bResult)
		{
			System.err.println("CMList.removeElement() failed! : "+element.toString());
		}

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMList.removeElement() done: "+element.toString());
			System.out.println("# current element: "+m_list.size());
		}

		return bResult;
	}
	
	public void removeAllElements()
	{
		m_list.removeAllElements();
	}
	
	public T findElement(T element)
	{
		T tempElement = null;
		boolean bFound = false;
		
		Iterator<T> iterList = m_list.iterator();
		while(iterList.hasNext() && !bFound)
		{
			tempElement = iterList.next();
			if(element.equals(tempElement))
				bFound = true;
		}
		
		if(bFound)
			return tempElement;
		return null;
	}
	
	public int getSize()
	{
		return m_list.size();
	}
	
	public boolean isEmpty()
	{
		return m_list.isEmpty();
	}
	
	public Vector<T> getList()
	{
		return m_list;
	}
	
}
