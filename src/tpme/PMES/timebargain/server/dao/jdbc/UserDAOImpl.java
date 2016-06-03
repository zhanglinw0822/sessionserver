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
	 * 更新本分组所有在线会话为下线状态
	 * @param groupId 分组Id
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
	 * 添加全局日志
	 * 
	 * @param operator
	 *            操作人
	 * @param operatorIP
	 *            操作人IP
	 * @param operatorType
	 *            操作类型
	 * @param operatorContent
	 *            操作内容
	 * @param operatorResult
	 *            操作结果
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
	 * 修改客户电话密码
	 * 
	 * @param traderID
	 * @param passwordOld
	 * @param password
	 * @return 0：成功；-1：原口令不正确；-2：客户代码不存在
	 */
	public int changePhonePassowrd(String traderID, String passwordOld,
			String password, String operateIP) {
		String sql = "select t1.FirmID, t2.PhonePWD from M_Trader t1, M_CustomerInfo t2" 
			+ " where t1.traderid = t2.customerno and t1.traderid=? ";
		Object[] params = new Object[]{traderID};
		List list = getJdbcTemplate().queryForList(sql, params);
		// 客户代码不存在
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
				"修改电话密码,由" + phonePWD + "改为" + MD5.getMD5(firmID, password)};
		getJdbcTemplate().update(sql, params);		
		return 0;
	}

	/**
	 * 修改客户密码
	 * 
	 * @param traderID
	 * @param passwordOld
	 * @param password
	 * @return 0：成功；-1：原口令不正确；
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
		//写日志
		sql = "insert into c_globallog_all(id,operator,operatetime," 
			+ "operatetype,operateip,operatortype,operatecontent,operateresult) "
			+ "values(SEQ_C_GLOBALLOG.Nextval,?,sysdate,3005,?,'E',?,1)";
		params = new Object[]{traderID, operateIP, 
				"修改交易密码，由" + pwd + "改为" + MD5.getMD5(traderID, password)};
		
		getJdbcTemplate().update(sql, params);
		return 0;
	}

	/**
	 * 检查代理信息 当会员代客户操作的时候检查权限
	 * 
	 * @param memberID
	 *            会员代码
	 * @param customerID
	 *            客户代码
	 * @param phonePassword
	 *            电话密码
	 * @return 0：成功 -1：客户不属于此会员 -2：客户代码不存在 -3：电话密码不正确
	 * @throws RemoteException
	 */
	public long checkDelegateInfo(String memberID, String customerID,
			String phonePassword) {
		String sql = "select MemberNo,PhonePWD from M_CustomerInfo where CustomerNo=? ";
		Object[] params = new Object[]{customerID};
		List list = getJdbcTemplate().queryForList(sql, params);
		// 客户代码不存在
		if (null == list || 0 == list.size()) {
			return -2;
		}

		Map map = (Map) list.get(0);
		String memberNO = (String) map.get("MemberNo");
		String phonePWD = (String) map.get("PhonePWD");
		// 客户不属于此会员
		if (!memberID.equals(memberNO)) {
			return -1;
		}

		// 电话密码不正确
		if (null != phonePWD 
				&& !phonePWD.equals(MD5.getMD5(customerID, phonePassword))) {
			return -3;
		}

		return 0;
	}

	/**
	 * 根据登陆的交易员ID获取交易员权限和交易商权限 初始化Privilege对象
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

		// 获取交易对手
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
	 * 登录成功
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
 	 * 登陆成功
 	 * @param sessionID 会话ID
 	 * @param traderID 交易员ID
 	 * @param logonIP 登录IP
 	 * @param logonModule 登录模块
 	 * @param logonTime 登录时间
 	 * @param groupId 分组ID
 	 * @param serverId 服务器ID
 	 * @param ZID 正心账号
 	 * @param firmID 交易商ID
 	 * @return 交易员名称
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
	 * 登录失败
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
 	 * 登陆失败
 	 * @param traderID 交易员ID
 	 * @param logonIP 登录IP
 	 * @param logonModule 登录模块
 	 * @param lastLogonTime 上次登陆时间
 	 * @param allowLogonError 允许错误阀值
 	 * @return -2:密码错误 -3:超过允许错误阀值
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
	 * 登录密码错错误调用此内部类
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
	 * 注销session
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
	 * 获取交易员信息
	 * @param traderID 交易员ID
	 * @return map 交易员信息
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
	 * 获取交易商信息
	 * @param firmID 交易商ID
	 * @return map 交易商信息
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
	 * 获取M_Configuration value值
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
	 * 根据serverName获取本serverName的属性
	 * @param serverName
	 * @return serverName属性 Map;
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
	 * 根据serverName获取本serverName的备机的属性(带负载均衡功能的)
	 * @param serverName
	 * @return serverName同步端机器属性 Map;
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
