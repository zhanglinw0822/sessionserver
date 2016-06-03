package tpme.PMES.member.ActiveUser;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tpme.PMES.timebargain.server.Server;
import tpme.PMES.timebargain.server.dao.UserDAO;
import tpme.PMES.timebargain.server.model.TraderInfo;
import tpme.PMES.timebargain.server.util.EncryptUtil;
import tpme.PMES.timebargain.server.util.LogUtil;
import tpme.PMES.timebargain.service.RMIClientAgency;

/**
 * 登录管理
 * @author
 * 修改记录:
 * 1.修改了logon登录方法,增加随机Key验证,以解决黑客暴力破解后把登录次数占满导致交易商正常登录受限制 2011-3-21 by zhousp
 */
public class LogonManager {

	private transient final Log logger = LogFactory.getLog(LogonManager.class);

	private String moduleId;

	private volatile static LogonManager um;

	private int space = 30; // 默认的超时线程扫描时间是30秒

	private int expireTime = 30; // 默认的超时时间是30分钟

	private int MULTI_MODE = 1; // 默认使用单例模式

	private int allowLogonError = 30;// 允许密码错误连续次数

	private static ActiveUserManager au;

	private String strServerKey = "0123456789abcdef";
	private byte[] byteServerKey = EncryptUtil.str2Bytes(strServerKey);
	
	private UserDAO userDAO = Server.getUserDAO();

	private LogonManager(int expireTime) {
		this.expireTime = expireTime;
	}

	private LogonManager() {
	}
	
	private void initActiveUserManager() {
		au = new ActiveUserManager(space, this.expireTime, MULTI_MODE);
	}
	
	private void closeActiveUserManager() {
		if (null != au) {
			au.dispose();
			au = null;
		}
	}

	/**
	 * 获取UserManager对象
	 * 
	 * @return
	 */
	public static LogonManager getInstance() {
		return um;
	}

	/**
	 * 创建一个实例
	 * 
	 * @param moduleId
	  * @param otherModuleId 其它需要访问的模块Id
	 * @param ds
	 * @param expireTime
	 * @param multiMode
	 * @param serverName
	 * @return 登录管理对象
	 */
	public static LogonManager createInstance(String moduleId,
			int expireTime, int multiMode) {
		if (null == um) {
			synchronized (LogonManager.class) {
				if (null == um) {
					um = new LogonManager(expireTime);
					um.init(moduleId, multiMode);
					um.initActiveUserManager();
				}
			}
		}

		return um;
	}

	/**
	 * 创建一个实例
	 * 
	 * @param moduleId
	 * @param otherModuleId 其它需要访问的模块Id
	 * @param ds
	 * @param multiMode
	 * @return 登录管理对象
	 */
	public static LogonManager createInstance(String moduleId, int multiMode) {
		return createInstance(moduleId, 30, multiMode);
	}

	/**
	 * 创建一个实例
	 * 
	 * @param moduleId
	 * @param ds
	 * @return 登录管理对象
	 */
	public static LogonManager createInstance(String moduleId) {
		return createInstance(moduleId, 30, 1);
	}

	/**
	 * @return 返回ActiveUserManager对象
	 */
	public static ActiveUserManager getActiveUserManager() {
		if (null == au) {
			au = new ActiveUserManager();
		}
		return au;
	}

	/**
	 * 初始化一个UserManager对象
	 * 
	 * @param moduleId
	 * @param otherModuleId 访问的其它模块Id
	 * @param ds
	 */
	private void init(String moduleId, int multiMode) {
		this.moduleId = moduleId;
		this.MULTI_MODE = multiMode;

		String value = userDAO.getConfigurationValue("allowLogonError");
		if (null != value) {
			this.allowLogonError = Integer.parseInt(value);
		}

		// 取出服务端共享密钥
		String confKey = null;
		if (RMIClientAgency.PC_SESSION_MODULE_ID.equals(moduleId)) {
			confKey = "PServerKey";
		} else if (RMIClientAgency.MOBILE_SESSION_MODULE_ID.equals(moduleId)) {
			confKey = "MServerKey";
		} else {
			return;
		}

		value = userDAO.getConfigurationValue(confKey);
		if (null != value) {
			strServerKey = value;
			byteServerKey = EncryptUtil.str2Bytes(strServerKey);
		}
	}

	/**
	 * 关闭ActiveUserService RMI
	 */
	public void dispose() throws RemoteException, NotBoundException {
		// 关闭AU检查的检查线程
		closeActiveUserManager();

		um = null;
	}

	/**
	 * 得到指定sessionID所对应的用户ID
	 * @param auSessionId
	 * @return 返回相应的用户userID。如果返回为null,则表示此sessionID为无效。
	 */
	public String getUserID(String auSessionId) {
		return au.getUserID(auSessionId);
	}

	/**
	 * 获得所有登录用户
	 * 
	 * @return 返回一个字符串数组,数组中的每一个元素代表一个用户登录连接,内容是用户ID和登录的时间用","加以分隔。
	 */
	public String[] getAllUsers() {
		return au.getAllUsers();
	}

	/**
	 * 获得所有登录用户，带IP信息
	 * 
	 * @return 返回一个字符串数组,数组中的每一个元素代表一个用户登录连接,内容是用户ID，登录的时间和登录ip用","加以分隔。
	 */
	public String[] getAllUsersWithIP() {
		return au.getAllUsersWithIP();
	}

	/**
	 * @param userId
	 * @param pwd
	 * @param clientToken
	 * @param logonTime
	 * @return 成功返回true,失败返回false
	 */
	private boolean checkLogon(String userId, String pwd, 
			String clientToken, long logonTime) {
		boolean result = false;

		try {
			byte[] plain = EncryptUtil.decryptStr(clientToken, pwd);

			ByteBuffer buffer = ByteBuffer.wrap(plain);
			buffer.order(ByteOrder.nativeOrder());
			byte[] plainUserId = EncryptUtil.getUTF(buffer);
			long plainLogonTime = buffer.getLong();

			String strPlainUserId = new String(plainUserId);

			if (strPlainUserId.equals(userId) && logonTime == plainLogonTime) { // 用户名密码相等
				result = true;
			}
		} catch (Exception e) { // 解密失败
			logger.info(LogUtil.getRmiMessage(userId, "解密", 
					new Object[]{userId, pwd, clientToken, logonTime}, "失败", "NA"));
		}

		return result;
	}
	
	/**
	 * @param userId
	 * @param password
	 * @param key
	 * @param logonIP
	 *            用户登录IP
	 * @return 成功返回sessionID；-1：交易员代码不存在；-2：口令不正确；-3：禁止登录；-4：Key盘验证错误；-5：其他异常
	 *         -6交易板块被禁止
	 */
	public TraderInfo logon(String userId, String clientToken, String key,
			String logonIP, long logonTime, String ZID, Server server,String moduleId) {
		Date now = new Date();
		TraderInfo trader = new TraderInfo();

		try {
			now = new Date();
			LogUtil.beginLog(this.getClass(), "UserDAO.getOneTraderById", userId, now);
			Map map = userDAO.getOneTraderById(userId);
			LogUtil.endLog(this.getClass(), "UserDAO.getOneTraderById", userId, now);

			if (null == map) {
				trader.retCode = -1; // 交易员代码不存在
			} else {
				String status = (String) map.get("status");
				String keyCode = (String) map.get("KeyCode");
				String enableKey = (String) map.get("EnableKey");
				String pwd = (String) map.get("password");

				trader.traderId = userId;
				trader.traderName = (String) map.get("name");
				trader.firmId = (String) map.get("firmID");
				trader.firmName = (String) map.get("firmName");
				trader.type = (String) map.get("type");
				trader.forceChangePwd = ((BigDecimal)map.get("forceChangePwd")).intValue();
				Timestamp lastTime = (Timestamp) map.get("onlinetime");
				trader.lastTime = (null == lastTime) ? null : lastTime.toString();
				trader.lastIP = (String) map.get("TraderIP");
				trader.lastModule = (String) map.get("lastModuleid");

				//判断交易员状态
				if ("N".equals(status)) {
					if ("Y".equals(enableKey) && !key.equals(keyCode)) {
						trader.retCode = -4; // Key值不正确
					} else {
						boolean result = checkLogon(userId, pwd, clientToken, logonTime);
						if (!result) {
							trader.retCode = -2; // 密码错误
							now = new Date();
							LogUtil.beginLog(this.getClass(), "UserDAO.LogonError", userId, now);
							userDAO.LogonError(userId, logonIP, moduleId, lastTime, allowLogonError);
							LogUtil.endLog(this.getClass(), "UserDAO.LogonError", userId, now);
						} else {
							now = new Date();
							LogUtil.beginLog(this.getClass(), "ActiveUserManager.getSessionID", userId, now);
							trader.auSessionId = au.getSessionID();
							LogUtil.endLog(this.getClass(), "ActiveUserManager.getSessionID", userId, now);
							if (null == trader.auSessionId) {
								trader.retCode = -5;
							} else {
								// 登录成功,生成serverToken, clientToken
								byte[] sessionKey = EncryptUtil.generateKey(); // 生成会话密钥
								trader.serverKey = EncryptUtil.generateTicket(sessionKey, 
										trader.auSessionId, byteServerKey);
								trader.clientKey = EncryptUtil.generateTicket(sessionKey, 
										trader.auSessionId, pwd);
								now = new Date();
								LogUtil.beginLog(this.getClass(), "UserDAO.LogonSuccess", userId, now);
								userDAO.LogonSuccess(trader.auSessionId, userId, logonIP, 
										moduleId, server.groupId, server.serverId, ZID);
								LogUtil.endLog(this.getClass(), "UserDAO.LogonSuccess", userId, now);

								try {
									now = new Date();
									LogUtil.beginLog(this.getClass(), "ActiveUserManager.logon", userId, now);
									au.logon(userId, logonIP, trader.auSessionId);
									LogUtil.endLog(this.getClass(), "ActiveUserManager.logon", userId, now);
									trader.retCode = 1; // 登陆成功
								} catch (Exception e) {
									//添加内存失败，清除数据库记录
									trader.retCode = -5;
									now = new Date();
									LogUtil.beginLog(this.getClass(), "logon logoffSession", userId, now);
									logoffSession(trader.auSessionId);
									LogUtil.endLog(this.getClass(), "logon logoffSession", userId, now);
								}
							}
						}
					}
				} else /* if ("D".equals(status) || "L".equals(status) || true) */ {
					trader.retCode = -3;
				}
			}
		} catch (Exception e) {
			trader.retCode = -5;
			logger.error(LogUtil.getMessage("system", 
					"用户登陆", userId, "失败" + e, trader));
		}

		return trader;
	}

	//避免方法重复，此方法由logoff改为logoffUser
	public void logoffUser(String userId) {
		au.logoffUser(userId);
	}

	/**
	 * 只是从在线列表中logoff这个sessionID,不写交易日志
	 * @param sessionID
	 */
	public void logoffSession(String sessionID) {
		au.logoff(sessionID);
	}

	/**
	 * 注销指定sessionID对应的登录状态。<br>
	 * <b>参 数：</b><br>
	 * <ul>
	 * sessionID:要注销的sessionID。<br>
	 * </ul>
	 * 
	 */
	public void logoff(String sessionID) {
		String userId = getUserID(sessionID);
		// 如果userId为空 说明sessionID无效 或者已经失效
		if (userId != null) {
			logoff(userId, sessionID);
		}
	}

	public void logoff(String userId, String sessionID) {
		logoffSession(sessionID);
	}

	/**
	 * 只在本地AU验证是否登录
	 * 
	 * @param userId
	 * @param auSessionId
	 * @return
	 */
	public boolean isLogon(String userId, String auSessionId) {
		String uid = getUserID(auSessionId);
		if (null == uid || !uid.equals(userId)) {
			return false;
		}

		return true;
	}

	/**
	 * 获取一个交易员所有的在线Session
	 * @param traderId
	 * @return
	 */
	public String[] getOnlineSession(String traderId) {
		return au.getAllUsersSys(traderId);
	}

	/**
	 * @param userId
	 * @return 成功返回sessionID；-1：au没有此用户
	 */
	public TraderInfo getTraderInfo(String userId) {
		TraderInfo trader = new TraderInfo();

		try {
			Map map = userDAO.getOneTraderById(userId);
	
			if (null != map) {
				trader.firmId = (String) map.get("firmID");
				trader.firmName = (String) map.get("firmName");
				trader.traderId = userId;
				trader.traderName = (String) map.get("name");
				trader.type = (String) map.get("type");
				trader.forceChangePwd = ((BigDecimal)map.get("forceChangePwd")).intValue();
				trader.lastIP = (String) map.get("TraderIP");
				Timestamp lastTime = (Timestamp) map.get("onlinetime");
				trader.lastTime = (null == lastTime) ? null : lastTime.toString();
				trader.lastModule = (String) map.get("lastModuleid");
			}
		} catch (Exception e) {
			logger.error(LogUtil.getMessage("system",
					"getTraderInfo", userId, "失败" + e, trader));

		}

		return trader;
	}

	/*
	 * 获取加密所使用的服务端密钥
	 */
	public byte[] getServerKey() {
		return byteServerKey;
	}

	public TraderInfo logonWithoutpwd(String traderID,String logonIP, Server server) {
		TraderInfo trader = new TraderInfo();
		try {
			Map map = userDAO.getOneTraderById(traderID);

			if (null == map) {
				trader.retCode = -1; // 交易员代码不存在
			} else {
				String status = (String) map.get("status");
				String keyCode = (String) map.get("KeyCode");
				String enableKey = (String) map.get("EnableKey");
				String pwd = (String) map.get("password");

				trader.traderId = traderID;
				trader.traderName = (String) map.get("name");
				trader.firmId = (String) map.get("firmID");
				trader.firmName = (String) map.get("firmName");
				trader.type = (String) map.get("type");
				trader.forceChangePwd = ((BigDecimal)map.get("forceChangePwd")).intValue();
				Timestamp lastTime = (Timestamp) map.get("onlinetime");
				trader.lastTime = (null == lastTime) ? null : lastTime.toString();
				trader.lastIP = (String) map.get("TraderIP");
				trader.lastModule = (String) map.get("lastModuleid");

				//判断交易员状态
				if ("N".equals(status)) {
							trader.auSessionId = au.getSessionID();
							if (null == trader.auSessionId) {
								trader.retCode = -5;
							} else {
								// 登录成功,生成serverToken, clientToken
								byte[] sessionKey = EncryptUtil.generateKey(); // 生成会话密钥
								trader.serverKey = EncryptUtil.generateTicket(sessionKey, 
										trader.auSessionId, byteServerKey);
								trader.clientKey = trader.auSessionId;// EncryptUtil.generateTicket(sessionKey, trader.auSessionId, pwd);

								userDAO.LogonSuccess(trader.auSessionId, traderID, logonIP, 
										moduleId, server.groupId, server.serverId, "");

								try {
									au.logon(traderID, logonIP, trader.auSessionId);
									trader.retCode = 1; // 登陆成功
								} catch (Exception e) {
									//添加内存失败，清除数据库记录
									trader.retCode = -5;
									logoffSession(trader.auSessionId);
								}
							}
				} else /* if ("D".equals(status) || "L".equals(status) || true) */ {
					trader.retCode = -3;
				}
			}
		} catch (Exception e) {
			trader.retCode = -5;
			logger.error(e);
		}

		return trader;
	}
}
