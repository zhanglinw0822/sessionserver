package tpme.PMES.timebargain.server.dao.jdbc;

import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.StoredProcedure;
import tpme.PMES.timebargain.MD5.MD5;
import tpme.PMES.timebargain.server.dao.SupperDAO;
import tpme.PMES.timebargain.server.dao.UserDAO;
import tpme.PMES.timebargain.server.model.Privilege;
import tpme.PMES.timebargain.server.model.TraderInfo;


public class UserDAOImpl extends SupperDAO implements UserDAO {

	private final Log log = LogFactory.getLog(getClass());

	/**
	 * ���±������������߻ỰΪ����״̬
	 * @param groupId ����Id
	 */
	public void updateGroupSessionDownLine(int groupId, String moduleId) {
		Object[] params = new Object[]{groupId, moduleId};
		int[] types = new int[]{Types.INTEGER, Types.VARCHAR};

		String sql = "update T_Session set DemiseTime = sysdate, OnlineStatus = 0 " 
			+ "where OnlineStatus = 1 and GroupID = ? and LogonModule = ? ";
		log.debug("sql: " + sql);
		getJdbcTemplate().update(sql, params, types);

		sql = "delete from t_onlinesession where GroupID = ? and LogonModule = ? ";
		log.debug("sql: " + sql);
		getJdbcTemplate().update(sql, params, types);
	}

	/**
	 * ���ȫ����־
	 * 
	 * @param operator
	 *            ������
	 * @param operatorIP
	 *            ������IP
	 * @param operatorType
	 *            ��������
	 * @param operatorContent
	 *            ��������
	 * @param operatorResult
	 *            �������
	 */
	public void addGlobalLog(String operator, String operatorIP,
			int operatorType, String operatorContent, int operatorResult) {
		String sql = "insert into c_globallog_all(id,operator,operatetime,operatetype, " 
			+ "operateip,operatecontent,operateresult) "
			+ "values(SEQ_C_GLOBALLOG.Nextval,?, sysdate,?,?,?,?)";
		Object[] params = new Object[]{operator,operatorType, operatorIP,
				operatorContent, operatorResult};
		int[] types = new int[]{Types.VARCHAR, Types.INTEGER, Types.VARCHAR,
				Types.VARCHAR, Types.INTEGER};
		log.debug("sql: " + sql);
		for (int i = 0, len = params.length; i < len; i++) {
			log.debug("params[" + i + "]: " + params[i]);
		}
		getJdbcTemplate().update(sql, params, types);
	}

	/**
	 * �޸Ŀͻ��绰����
	 * 
	 * @param traderID
	 * @param passwordOld
	 * @param password
	 * @return 0���ɹ���-1��ԭ�����ȷ��-2���ͻ����벻����
	 */
	public int changePhonePassowrd(String traderID, String passwordOld,
			String password, String operateIP) {
		String sql = "select t1.FirmID, t2.PhonePWD from M_Trader t1, M_CustomerInfo t2" 
			+ " where t1.traderid = t2.customerno and t1.traderid=? ";
		Object[] params = new Object[]{traderID};
		List list = getJdbcTemplate().queryForList(sql, params);
		// �ͻ����벻����
		if (null == list || list.size() <= 0) {
			return -2;
		}

		Map map = (Map) list.get(0);
		String firmID = (String) map.get("FirmID");
		String phonePWD = (String) map.get("PhonePWD");

		if (null != phonePWD && !phonePWD.equals(MD5.getMD5(firmID, passwordOld))) {
			return -1;
		}
		sql = "update M_CustomerInfo set PhonePWD=? where CustomerNo=?";
		params = new Object[]{MD5.getMD5(firmID, password), firmID};

		log.debug("sql: " + sql);
		for (int i = 0, len = params.length; i < len; i++) {
			log.debug("params[" + i + "]: " + params[i]);
		}

		getJdbcTemplate().update(sql, params);
		sql = "insert into c_globallog_all(id,operator,operatetime," 
			+ "operatetype,operateip,operatortype,operatecontent,operateresult) "
			+ "values(SEQ_C_GLOBALLOG.Nextval,?,sysdate,3006,?,'E',?,1)";
		params = new Object[]{firmID, operateIP, 
				"�޸ĵ绰����,��" + phonePWD + "��Ϊ" + MD5.getMD5(firmID, password)};
		getJdbcTemplate().update(sql, params);		
		return 0;
	}

	/**
	 * �޸Ŀͻ�����
	 * 
	 * @param traderID
	 * @param passwordOld
	 * @param password
	 * @return 0���ɹ���-1��ԭ�����ȷ��
	 */
	public int changePassowrd(String traderID, String passwordOld,
			String password, String operateIP) {
		String sql = "select Password from M_Trader t where t.traderid=? ";
		Object[] params = new Object[]{traderID};
		String pwd = (String) getJdbcTemplate().queryForObject(sql, params,
				String.class);
		if (null != pwd && !pwd.equals(MD5.getMD5(traderID, passwordOld))) {
			return -1;
		}
		sql = "update M_trader set password=?,forceChangePwd=0 where traderId=?";
		params = new Object[]{MD5.getMD5(traderID, password), traderID};

		log.debug("sql: " + sql);
		for (int i = 0, len = params.length; i < len; i++) {
			log.debug("params[" + i + "]: " + params[i]);
		}

		getJdbcTemplate().update(sql, params);

		sql = "update c_m_user set password=? where id=?";
		params = new Object[]{MD5.getMD5(traderID, password), traderID};

		log.debug("sql: " + sql);
		for (int i = 0, len = params.length; i < len; i++) {
			log.debug("params[" + i + "]: " + params[i]);
		}

		getJdbcTemplate().update(sql, params);
		//д��־
		sql = "insert into c_globallog_all(id,operator,operatetime," 
			+ "operatetype,operateip,operatortype,operatecontent,operateresult) "
			+ "values(SEQ_C_GLOBALLOG.Nextval,?,sysdate,3005,?,'E',?,1)";
		params = new Object[]{traderID, operateIP, 
				"�޸Ľ������룬��" + pwd + "��Ϊ" + MD5.getMD5(traderID, password)};
		
		getJdbcTemplate().update(sql, params);
		return 0;
	}

	/**
	 * ��������Ϣ ����Ա���ͻ�������ʱ����Ȩ��
	 * 
	 * @param memberID
	 *            ��Ա����
	 * @param customerID
	 *            �ͻ�����
	 * @param phonePassword
	 *            �绰����
	 * @return 0���ɹ� -1���ͻ������ڴ˻�Ա -2���ͻ����벻���� -3���绰���벻��ȷ
	 * @throws RemoteException
	 */
	public long checkDelegateInfo(String memberID, String customerID,
			String phonePassword) {
		String sql = "select MemberNo,PhonePWD from M_CustomerInfo where CustomerNo=? ";
		Object[] params = new Object[]{customerID};
		List list = getJdbcTemplate().queryForList(sql, params);
		// �ͻ����벻����
		if (null == list || 0 == list.size()) {
			return -2;
		}

		Map map = (Map) list.get(0);
		String memberNO = (String) map.get("MemberNo");
		String phonePWD = (String) map.get("PhonePWD");
		// �ͻ������ڴ˻�Ա
		if (!memberID.equals(memberNO)) {
			return -1;
		}

		// �绰���벻��ȷ
		if (null != phonePWD 
				&& !phonePWD.equals(MD5.getMD5(customerID, phonePassword))) {
			return -3;
		}

		return 0;
	}

	/**
	 * ���ݵ�½�Ľ���ԱID��ȡ����ԱȨ�޺ͽ�����Ȩ�� ��ʼ��Privilege����
	 */
	public Privilege getTradePrivilege(TraderInfo info) {
		Privilege privilege = new Privilege();
		privilege.setTraderID(info.traderId);
		privilege.setFirmId(null == info.firmId ? "" : info.firmId);
		privilege.setTraderName(info.traderName);
		privilege.setFirmName(info.firmName);

		privilege.setTraderType(info.type);
		privilege.setSessionID(info.auSessionId);

		String m_FirmSql = "select FirmType from m_firm where FirmID=? ";
		Map m_FirmMap = getJdbcTemplate().queryForMap(m_FirmSql,
				new Object[]{privilege.getFirmId()});
		if (null != m_FirmMap) {
			String firmType = (String) m_FirmMap.get("FirmType");
			if ("S".equals(firmType)) {
				privilege.setFirmType("2");
			} else if ("M".equals(firmType)) {
				privilege.setFirmType("1");
			} else {// C
				privilege.setFirmType("0");
			}
		}

		// ��ȡ���׶���
/*		StringBuffer sb = new StringBuffer();
		if ("0".equals(privilege.getFirmType())) {
			sb.append("select b.MemberNo,b.Name").append(
					" from M_CustomerInfo a,M_MemberInfo b ").append(
					" where a.CustomerNo=? and a.MemberNo = b.MemberNo ");
		} else {
			sb.append("select b.MemberNo,b.Name").append(
					" from M_MemberRelation a,M_S_MemberInfo b ").append(
					" where a.MemberNo=? and b.MemberNo = a.S_MemberNo order by a.sortno");
		}

		Object[] params = null;
		params = new Object[]{privilege.getFirmId()};

		log.debug("sql: " + sb.toString());
		if (params != null) {
			for (int i = 0, len = params.length; i < len; i++) {
				log.debug("params[" + i + "]: " + params[i]);
			}
		}

		List list = getJdbcTemplate().queryForList(sb.toString(), params);

		if (null != list && list.size() > 0) {
			Map map = (Map) list.get(0);
			privilege.setM_FirmID((String) map.get("MemberNo"));
			privilege.setM_FirmName((String) map.get("Name"));
		} else {
			privilege.setM_FirmID("");
			privilege.setM_FirmName("");
		}*/

		return privilege;
	}

	/**
	 * ��¼�ɹ�
	*/
	class LogonSuccessStoredProcedure extends StoredProcedure {
		private static final String SFUNC_NAME = "SP_M_TraderLogon_Sucess";

		public LogonSuccessStoredProcedure(DataSource ds) {
			super(ds, SFUNC_NAME);

			declareParameter(new SqlParameter("p_SessionID", Types.VARCHAR));
			declareParameter(new SqlParameter("p_TraderId", Types.VARCHAR));
			declareParameter(new SqlParameter("p_LogonIP", Types.VARCHAR));
			declareParameter(new SqlParameter("p_ModuleID", Types.VARCHAR));
			declareParameter(new SqlParameter("p_GroupID", Types.INTEGER));
			declareParameter(new SqlParameter("p_ServerID", Types.INTEGER));
			declareParameter(new SqlParameter("p_ZID", Types.VARCHAR));

			compile();
		}

		public Map execute(Map inParams) {
			return super.execute(inParams);
		}
	}

 	/**
 	 * ��½�ɹ�
 	 * @param sessionID �ỰID
 	 * @param traderID ����ԱID
 	 * @param logonIP ��¼IP
 	 * @param logonModule ��¼ģ��
 	 * @param logonTime ��¼ʱ��
 	 * @param groupId ����ID
 	 * @param serverId ������ID
 	 * @param ZID �����˺�
 	 * @param firmID ������ID
 	 * @return ����Ա����
 	 */
	public void LogonSuccess(String sessionID, String traderID, String logonIP,
			String logonModule, int groupId, int serverId, String ZID) {
		LogonSuccessStoredProcedure sfunc = new 
				LogonSuccessStoredProcedure(getDataSource());

		Map inputs = new HashMap();
		inputs.put("p_SessionID", sessionID);
		inputs.put("p_TraderId", traderID);
		inputs.put("p_LogonIP", logonIP);
		inputs.put("p_ModuleID", logonModule);
		inputs.put("p_GroupID", groupId);
		inputs.put("p_ServerID", serverId);
		inputs.put("p_ZID", ZID);

		sfunc.execute(inputs);
	}

	/*
	 * ��¼ʧ��
	*/
	class LogonErrorStoredProcedure extends StoredProcedure {
    	private static final String SFUNC_NAME = "SP_M_TraderLogon_Error";

    	public LogonErrorStoredProcedure(DataSource ds) {
    		super(ds, SFUNC_NAME);

    		declareParameter(new SqlParameter("p_TraderId", Types.VARCHAR));
    		declareParameter(new SqlParameter("p_LogonIP", Types.VARCHAR));
    		declareParameter(new SqlParameter("p_ModuleID", Types.VARCHAR));
    		declareParameter(new SqlParameter("p_LastLogonTime", Types.TIMESTAMP));
    		declareParameter(new SqlParameter("p_AllowLogonError", Types.INTEGER));

    		compile();
    	}

    	public Map execute(Map inParams) {
    		return super.execute(inParams);
    	}
	}

 	/**
 	 * ��½ʧ��
 	 * @param traderID ����ԱID
 	 * @param logonIP ��¼IP
 	 * @param logonModule ��¼ģ��
 	 * @param lastLogonTime �ϴε�½ʱ��
 	 * @param allowLogonError �������ֵ
 	 * @return -2:������� -3:�����������ֵ
 	 */
	public void LogonError(String traderID, String logonIP, String logonModule, 
			Timestamp lastLogonTime, int allowLogonError) {
		LogonErrorStoredProcedure sfunc = new 
				LogonErrorStoredProcedure(getDataSource());

		Map inputs = new HashMap();
		inputs.put("p_TraderId", traderID);
		inputs.put("p_LogonIP", logonIP);
		inputs.put("p_ModuleID", logonModule);
		inputs.put("p_LastLogonTime", lastLogonTime);
		inputs.put("p_AllowLogonError", allowLogonError);

		sfunc.execute(inputs);
	}

	/*
	 * ��¼����������ô��ڲ���
	*/
	class LogoutStoredProcedure extends StoredProcedure {
    	private static final String SFUNC_NAME = "SP_M_TraderLogout";

    	public LogoutStoredProcedure(DataSource ds) {
    		super(ds, SFUNC_NAME);

    		declareParameter(new SqlParameter("p_SessionID", Types.VARCHAR));
    		declareParameter(new SqlParameter("p_operator", Types.VARCHAR));
    		declareParameter(new SqlParameter("p_operateip", Types.VARCHAR));
    		declareParameter(new SqlParameter("p_operatecontent", Types.VARCHAR));

    		compile();
    	}

    	public Map execute(Map inParams) {
    		return super.execute(inParams);
    	}
	}

	/**
	 * ע��session
	 * @param sessionID
	 */
	public void Logout(String sessionID, String operator, 
			String operatorIP, String operatorContent) {
		LogoutStoredProcedure sfunc = new LogoutStoredProcedure(getDataSource());

		Map inputs = new HashMap();
		inputs.put("p_SessionID", sessionID);
		inputs.put("p_operator", operator);
		inputs.put("p_operateip", operatorIP);
		inputs.put("p_operatecontent", operatorContent);

		sfunc.execute(inputs);
	}

	/**
	 * ��ȡ����Ա��Ϣ
	 * @param traderID ����ԱID
	 * @return map ����Ա��Ϣ
	 */
	public Map getOneTraderById(String traderID) {
		String sql = "select f.name firmName, f.firmType, m.* from m_firm f, m_trader m " 
			+ "where f.firmId = m.firmId and m.traderId = ?";

		List lst = getJdbcTemplate().queryForList(sql, new Object[]{traderID},
				new int[]{Types.VARCHAR});
		if (null == lst || lst.size() <= 0) {
			return null;
		}

		return (Map)lst.get(0);
	}
	
	/**
	 * ��ȡ��������Ϣ
	 * @param firmID ������ID
	 * @return map ��������Ϣ
	 */
	public Map getFirmInfoById(String traderID) {
		String sql = "select * from m_firm where firmId = ?";

		List lst = getJdbcTemplate().queryForList(sql, new Object[]{traderID},
				new int[]{Types.VARCHAR});
		if (null == lst || lst.size() <= 0) {
			return null;
		}

		return (Map)lst.get(0);
	}

	/**
	 * ��ȡM_Configuration valueֵ
	 */
	public String getConfigurationValue(String key) {
		String sql = "select value from M_Configuration where key= ?";

		List lst = this.getJdbcTemplate().queryForList(sql, new Object[]{key},
				new int[]{Types.VARCHAR});
		if (null == lst || lst.size() <= 0) {
			return null;
		}

		return (String)((Map) lst.get(0)).get("value");
	}

	/**
	 * ����serverName��ȡ��serverName������
	 * @param serverName
	 * @return serverName���� Map;
	 */
	public Map<String, Object> getLBServerName(String serverName) {
		String sql = "select * from T_LB_Server where Server_Name = ?";

		try {
			return getJdbcTemplate().queryForMap(sql, new Object[]{serverName},
					new int[]{Types.VARCHAR});
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * ����serverName��ȡ��serverName�ı���������(�����ؾ��⹦�ܵ�)
	 * @param serverName
	 * @return serverNameͬ���˻������� Map;
	 */
	public Map<String, Object> getSlaveLBServerName(String serverName) {
		String sql = "select * from T_LB_Server where Server_ModuleId = " 
			+ "(select Server_ModuleId from T_LB_Server where Server_Name = ?) " 
			+ "and Server_GroupId = (select Server_GroupId " 
			+ "from T_LB_Server where Server_Name = ?) " 
			+ "and Server_Name != ?";

		try {
			return getJdbcTemplate().queryForMap(sql,
					new Object[]{serverName, serverName, serverName},
					new int[]{Types.VARCHAR, Types.VARCHAR, Types.VARCHAR});
		} catch (Exception e) {
			return null;
		}
    }
}
