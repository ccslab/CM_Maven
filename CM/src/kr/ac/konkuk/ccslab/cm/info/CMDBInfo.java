package kr.ac.konkuk.ccslab.cm.info;
import java.sql.*;

public class CMDBInfo {
	private Connection m_connection;
	private Statement m_statement;
	private ResultSet m_resultSet;
	
	public CMDBInfo()
	{
		m_connection = null;
		m_statement = null;
		m_resultSet = null;
	}
	
	public void setConnection(Connection conn)
	{
		m_connection = conn;
	}
	
	public Connection getConnection()
	{
		return m_connection;
	}
	
	public void setStatement(Statement stat)
	{
		m_statement = stat;
	}
	
	public Statement getStatement()
	{
		return m_statement;
	}
	
	public void setResultSet(ResultSet result)
	{
		m_resultSet = result;
	}
	
	public ResultSet getResultSet()
	{
		return m_resultSet;
	}
}
