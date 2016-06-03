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
 * ��¼����
 * @author
 * �޸ļ�¼:
 * 1.�޸���logon��¼����,�������Key��֤,�Խ���ڿͱ����ƽ��ѵ�¼����ռ�����½�����������¼������ 2011-3-21 by zhousp
 */
public class LogonManager {

	private transient final Log logger = LogFactory.getLog(LogonManager.class);

	private String moduleId;

	private volatile static LogonManager um;

	private int space = 30; // Ĭ�ϵĳ�ʱ�߳�ɨ��ʱ����30��

	private int expireTime = 30; // Ĭ�ϵĳ�ʱʱ����30����

	private int MULTI_MODE = 1; // Ĭ��ʹ�õ���ģʽ

	private int allowLogonError = 30;// �������������������

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
	 * ��ȡUserManager����
	 * 
	 * @return
	 */
	public static LogonManager getInstance() {
		return um;
	}

	/**
	 * ����һ��ʵ��
	 * 
	 * @param moduleId
	  * @param otherModuleId ������Ҫ���ʵ�ģ��Id
	 * @param ds
	 * @param expireTime
	 * @param multiMode
	 * @param serverName
	 * @return ��¼�������
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
	 * ����һ��ʵ��
	 * 
	 * @param moduleId
	 * @param otherModuleId ������Ҫ���ʵ�ģ��Id
	 * @param ds
	 * @param multiMode
	 * @return ��¼�������
	 */
	public static LogonManager createInstance(String moduleId, int multiMode) {
		return createInstance(moduleId, 30, multiMode);
	}

	/**
	 * ����һ��ʵ��
	 * 
	 * @param moduleId
	 * @param ds
	 * @return ��¼�������
	 */
	public static LogonManager createInstance(String moduleId) {
		return createInstance(moduleId, 30, 1);
	}

	/**
	 * @return ����ActiveUserManager����
	 */
	public static ActiveUserManager getActiveUserManager() {
		if (null == au) {
			au = new ActiveUserManager();
		}
		return au;
	}

	/**
	 * ��ʼ��һ��UserManager����
	 * 
	 * @param moduleId
	 * @param otherModuleId ���ʵ�����ģ��Id
	 * @param ds
	 */
	private void init(String moduleId, int multiMode) {
		this.moduleId = moduleId;
		this.MULTI_MODE = multiMode;

		String value = userDAO.getConfigurationValue("allowLogonError");
		if (null != value) {
			this.allowLogonError = Integer.parseInt(value);
		}

		// ȡ������˹�����Կ
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
	 * �ر�ActiveUserService RMI
	 */
	public void dispose() throws RemoteException, NotBoundException {
		// �ر�AU���ļ���߳�
		closeActiveUserManager();

		um = null;
	}

	/**
	 * �õ�ָ��sessionID����Ӧ���û�ID
	 * @param auSessionId
	 * @return ������Ӧ���û�userID���������Ϊnull,���ʾ��sessionIDΪ��Ч��
	 */
	public String getUserID(String auSessionId) {
		return au.getUserID(auSessionId);
	}

	/**
	 * ������е�¼�û�
	 * 
	 * @return ����һ���ַ�������,�����е�ÿһ��Ԫ�ش���һ���û���¼����,�������û�ID�͵�¼��ʱ����","���Էָ���
	 */
	public String[] getAllUsers() {
		return au.getAllUsers();
	}

	/**
	 * ������е�¼�û�����IP��Ϣ
	 * 
	 * @return ����һ���ַ�������,�����е�ÿһ��Ԫ�ش���һ���û���¼����,�������û�ID����¼��ʱ��͵�¼ip��","���Էָ���
	 */
	public String[] getAllUsersWithIP() {
		return au.getAllUsersWithIP();
	}

	/**
	 * @param userId
	 * @param pwd
	 * @param clientToken
	 * @param logonTime
	 * @return �ɹ�����true,ʧ�ܷ���false
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

			if (strPlainUserId.equals(userId) && logonTime == plainLogonTime) { // �û����������
				result = true;
			}
		} catch (Exception e) { // ����ʧ��
			logger.info(LogUtil.getRmiMessage(userId, "����", 
					new Object[]{userId, pwd, clientToken, logonTime}, "ʧ��", "NA"));
		}

		return result;
	}
	
	/**
	 * @param userId
	 * @param password
	 * @param key
	 * @param logonIP
	 *            �û���¼IP
	 * @return �ɹ�����sessionID��-1������Ա���벻���ڣ�-2�������ȷ��-3����ֹ��¼��-4��Key����֤����-5�������쳣
	 *         -6���װ�鱻��ֹ
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
				trader.retCode = -1; // ����Ա���벻����
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

				//�жϽ���Ա״̬
				if ("N".equals(status)) {
					if ("Y".equals(enableKey) && !key.equals(keyCode)) {
						trader.retCode = -4; // Keyֵ����ȷ
					} else {
						boolean result = checkLogon(userId, pwd, clientToken, logonTime);
						if (!result) {
							trader.retCode = -2; // �������
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
								// ��¼�ɹ�,����serverToken, clientToken
								byte[] sessionKey = EncryptUtil.generateKey(); // ���ɻỰ��Կ
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
									trader.retCode = 1; // ��½�ɹ�
								} catch (Exception e) {
									//����ڴ�ʧ�ܣ�������ݿ��¼
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
					"�û���½", userId, "ʧ��" + e, trader));
		}

		return trader;
	}

	//���ⷽ���ظ����˷�����logoff��ΪlogoffUser
	public void logoffUser(String userId) {
		au.logoffUser(userId);
	}

	/**
	 * ֻ�Ǵ������б���logoff���sessionID,��д������־
	 * @param sessionID
	 */
	public void logoffSession(String sessionID) {
		au.logoff(sessionID);
	}

	/**
	 * ע��ָ��sessionID��Ӧ�ĵ�¼״̬��<br>
	 * <b>�� ����</b><br>
	 * <ul>
	 * sessionID:Ҫע����sessionID��<br>
	 * </ul>
	 * 
	 */
	public void logoff(String sessionID) {
		String userId = getUserID(sessionID);
		// ���userIdΪ�� ˵��sessionID��Ч �����Ѿ�ʧЧ
		if (userId != null) {
			logoff(userId, sessionID);
		}
	}

	public void logoff(String userId, String sessionID) {
		logoffSession(sessionID);
	}

	/**
	 * ֻ�ڱ���AU��֤�Ƿ��¼
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
	 * ��ȡһ������Ա���е�����Session
	 * @param traderId
	 * @return
	 */
	public String[] getOnlineSession(String traderId) {
		return au.getAllUsersSys(traderId);
	}

	/**
	 * @param userId
	 * @return �ɹ�����sessionID��-1��auû�д��û�
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
					"getTraderInfo", userId, "ʧ��" + e, trader));

		}

		return trader;
	}

	/*
	 * ��ȡ������ʹ�õķ������Կ
	 */
	public byte[] getServerKey() {
		return byteServerKey;
	}

	public TraderInfo logonWithoutpwd(String traderID,String logonIP, Server server) {
		TraderInfo trader = new TraderInfo();
		try {
			Map map = userDAO.getOneTraderById(traderID);

			if (null == map) {
				trader.retCode = -1; // ����Ա���벻����
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

				//�жϽ���Ա״̬
				if ("N".equals(status)) {
							trader.auSessionId = au.getSessionID();
							if (null == trader.auSessionId) {
								trader.retCode = -5;
							} else {
								// ��¼�ɹ�,����serverToken, clientToken
								byte[] sessionKey = EncryptUtil.generateKey(); // ���ɻỰ��Կ
								trader.serverKey = EncryptUtil.generateTicket(sessionKey, 
										trader.auSessionId, byteServerKey);
								trader.clientKey = trader.auSessionId;// EncryptUtil.generateTicket(sessionKey, trader.auSessionId, pwd);

								userDAO.LogonSuccess(trader.auSessionId, traderID, logonIP, 
										moduleId, server.groupId, server.serverId, "");

								try {
									au.logon(traderID, logonIP, trader.auSessionId);
									trader.retCode = 1; // ��½�ɹ�
								} catch (Exception e) {
									//����ڴ�ʧ�ܣ�������ݿ��¼
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
