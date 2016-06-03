package tpme.PMES.timebargain.server.rmi.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.*;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tpme.PMES.timebargain.AppMonitor.AppMonitor;
import tpme.PMES.member.ActiveUser.LogonManager;
import tpme.PMES.timebargain.server.Constants;
import tpme.PMES.timebargain.server.Server;
import tpme.PMES.timebargain.server.SyncServer;
import tpme.PMES.timebargain.server.dao.TradeQueryDAO;
import tpme.PMES.timebargain.server.dao.UserDAO;
import tpme.PMES.timebargain.server.model.Privilege;
import tpme.PMES.timebargain.server.model.SessionContext;
import tpme.PMES.timebargain.server.model.SyncData;
import tpme.PMES.timebargain.server.model.Trader;
import tpme.PMES.timebargain.server.model.TraderInfo;
import tpme.PMES.timebargain.server.model.ValidCode;
import tpme.PMES.timebargain.server.rmi.LogonRMI;
import tpme.PMES.timebargain.server.util.LogUtil;
import tpme.PMES.timebargain.server.util.StringUtil;

/**
 * ����RMIʵ����.
 * 
 * <p>
 * <a href="TradeRMIImpl.java.html"><i>View Source</i></a>
 * </p>
 * 
 * @version 1.0.0.1
 * @author <a href="mailto:zhousp@tpme.com.cn">zhousp</a>
 */
public class LogonRMIImpl extends UnicastRemoteObject implements LogonRMI {
	private static final long serialVersionUID = 2690197650654049816L;
	private final Log log = LogFactory.getLog(getClass());

	/*********** ���캯����ֵ�� ****************/
	private Server server; // ��½��������������

	/**
	 * ���캯��
	 * 
	 * @param Server
	 * @throws RemoteException
	 */
	public LogonRMIImpl(Server server) throws RemoteException {
		this.server = server; 
	}

	/**
	 * ȡ����RMI�Ŀͻ��˵���������
	 * @return String
	 */
	private String getRMIClientHostName() {
		String clientHostName = null;
		
		try {
			clientHostName = RemoteServer.getClientHost();
		} catch (ServerNotActiveException e) {
			log.error(LogUtil.getSysMessage("ȡ����RMI�Ŀͻ��˵�������", "ʧ��" + e));
		} catch (Exception e) {
			log.error(LogUtil.getSysMessage("ȡ����RMI�Ŀͻ��˵�������", "ʧ��" + e));
		}

		return clientHostName;
	}

	/**
	 * ȡ����RMI�Ŀͻ��˵�����IP
	 * @return String
	 */
	private String getRMIClientHostIP() {
		String clientHostIP = null;
		String clientHostName = getRMIClientHostName();
		if (null != clientHostName) {
			try {
				clientHostIP = InetAddress.getByName(clientHostName).getHostAddress();
			} catch (UnknownHostException e) {
				log.error(LogUtil.getSysMessage("ȡ����RMI�Ŀͻ��˵�����IP", "ʧ��" + e));
			} catch (Exception e) {
				log.error(LogUtil.getSysMessage("ȡ����RMI�Ŀͻ��˵�����IP", "ʧ��" + e));
			}
		}

		return clientHostIP;
	}
	
	/**
	 * ����Ա��½
	 * 
	 * @param Trader
	 *            trader ����ID������,key��IP
	 * @return 
	 *         TraderInfo,����TraderInfo.retCode>0��ʾ�ɹ���-1������Ա���벻���ڣ�-2�������ȷ��-3��
	 *         ��ֹ��½��-4��Key����֤����-5�������쳣;-6:���װ�鱻��ֹ;
	 * @throws RemoteException
	 */
	public SessionContext logon(Trader trader,String moduleId) throws RemoteException {
		Date now1 = new Date();
		Date now = new Date();
		LogUtil.beginLog(this.getClass(), "LogonRMIImpl.logon", trader.getTraderID(), now1);
		SessionContext sessionContext = null;
		String rmiClientHostIP = getRMIClientHostIP();
		
		try {
			log.info(LogUtil.getRmiMessage(trader.getTraderID(), rmiClientHostIP, 
					"��½���׷�����", new Object[]{trader}, "��", "NA"));
			// ��¼�������
			AppMonitor.getInstance().add(Constants.MON_LOGON_COUNT, 1);

			LogonManager logonManager = server.getLogonManager();
			
			now = new Date();
			LogUtil.beginLog(this.getClass(), "LogonManager.logon", trader.getTraderID(), now);
			TraderInfo traderInfo = logonManager.logon(trader.getTraderID(), 
					trader.getClientToken(), trader.getKeyCode(), trader.getLogonIP(), 
					trader.getLogonTime(), trader.getZID(), server,moduleId);
			LogUtil.endLog(this.getClass(), "LogonManager.logon", trader.getTraderID(), now);

		    sessionContext = new SessionContext();
			sessionContext.setTraderInfo(traderInfo);

			if (traderInfo.retCode > 0) {
				log.info(LogUtil.getRmiMessage(trader.getTraderID(), rmiClientHostIP, 
						"��½���׷�����", new Object[]{trader}, "�ɹ�", traderInfo.auSessionId));

				Privilege privilege = getPrivilege(traderInfo);

				LogonManager.getActiveUserManager().putPrivilegeQueue(traderInfo.auSessionId, privilege, "logon");
				sessionContext.setPrivilege(privilege);

					// ͬ�����ε�¼��Ϣ������
				now = new Date();
				LogUtil.beginLog(this.getClass(), "SyncServer.SyncAdd", traderInfo.auSessionId, now);
				SyncServer.getInstance().SyncAdd(traderInfo.auSessionId, 
						LogonManager.getActiveUserManager().getAUValue(traderInfo.auSessionId), 
						privilege);
				LogUtil.endLog(this.getClass(), "SyncServer.SyncAdd", traderInfo.auSessionId, now);

				//  ��¼����ɹ�����
				AppMonitor.getInstance().add(Constants.MON_LOGON_SUCCESS, 1);
				// ��ǰ���߽���Ա��
				int onlineTraderNum = LogonManager.getActiveUserManager().
						getOnlineTraderNum();
				AppMonitor.getInstance().set(Constants.MON_TRADER_NUM, onlineTraderNum);
				// ��ǰ���߻Ự��
				int onlineSessionNum = LogonManager.getActiveUserManager().
						getOnlineSessionNum();
				AppMonitor.getInstance().set(Constants.MON_SESSION_NUM, onlineSessionNum);
			} else {
				log.info(LogUtil.getRmiMessage(trader.getTraderID(), rmiClientHostIP, 
						"��½���׷�����", new Object[]{trader}, "ʧ��", 
						"retCode: " + sessionContext.getTraderInfo().retCode));

				if (-2 == traderInfo.retCode) {
					// ����ʧ�ܵ��µ�¼ʧ�ܼ���
					AppMonitor.getInstance().add(Constants.MON_LOGON_FAIL, 1);
				} else {
					//  �����쳣����ʧ�ܼ���
					AppMonitor.getInstance().add(Constants.MON_LOGON_EXCEPTION, 1);
				}
			}
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(trader.getTraderID(), rmiClientHostIP, 
					"��½���׷�����", new Object[]{trader}, "ʧ��" + e
					, "retCode: " + sessionContext.getTraderInfo().retCode),e);
			throw new RuntimeException(e);
		}finally{
			LogUtil.endLog(this.getClass(), "LogonRMIImpl.logon", trader.getTraderID(), now1);
		}
		
		return sessionContext;
	}

	public SessionContext logonWithoutpwd(String traderID,String logonIP) throws RemoteException {
		SessionContext sessionContext = null;
		String rmiClientHostIP = getRMIClientHostIP();
		
		try {
			log.info(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"��½���׷�����", new Object[]{traderID}, "��", "NA"));
			// ��¼�������
			AppMonitor.getInstance().add(Constants.MON_LOGON_COUNT, 1);

			LogonManager logonManager = server.getLogonManager();

			TraderInfo traderInfo = logonManager.logonWithoutpwd(traderID,logonIP,server);

		    sessionContext = new SessionContext();
			sessionContext.setTraderInfo(traderInfo);

			if (traderInfo.retCode > 0) {
				log.info(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
						"��½���׷�����", new Object[]{traderID}, "�ɹ�", traderInfo.auSessionId));

				Privilege privilege = getPrivilege(traderInfo);

				
				LogonManager.getActiveUserManager().putPrivilegeQueue(traderInfo.auSessionId, privilege, "logonWithoutpwd");
				sessionContext.setPrivilege(privilege);

					// ͬ�����ε�¼��Ϣ������
				SyncServer.getInstance().SyncAdd(traderInfo.auSessionId, 
						LogonManager.getActiveUserManager().getAUValue(traderInfo.auSessionId), 
						privilege);

				//  ��¼����ɹ�����
				AppMonitor.getInstance().add(Constants.MON_LOGON_SUCCESS, 1);
				// ��ǰ���߽���Ա��
				int onlineTraderNum = LogonManager.getActiveUserManager().
						getOnlineTraderNum();
				AppMonitor.getInstance().set(Constants.MON_TRADER_NUM, onlineTraderNum);
				// ��ǰ���߻Ự��
				int onlineSessionNum = LogonManager.getActiveUserManager().
						getOnlineSessionNum();
				AppMonitor.getInstance().set(Constants.MON_SESSION_NUM, onlineSessionNum);
			} else {
				log.info(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
						"��½���׷�����", new Object[]{traderID}, "ʧ��", 
						"retCode: " + sessionContext.getTraderInfo().retCode));

				if (-2 == traderInfo.retCode) {
					// ����ʧ�ܵ��µ�¼ʧ�ܼ���
					AppMonitor.getInstance().add(Constants.MON_LOGON_FAIL, 1);
				} else {
					//  �����쳣����ʧ�ܼ���
					AppMonitor.getInstance().add(Constants.MON_LOGON_EXCEPTION, 1);
				}
			}
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"��½���׷�����", new Object[]{traderID}, "ʧ��" + e
					, "retCode: " + sessionContext.getTraderInfo().retCode),e);
			throw new RuntimeException(e);
		}

		return sessionContext;
	}
	
	/**
	 * ��֤����Ա�Ƿ��½
	 * 
	 * @param traderID
	 *            �� sessionID
	 * @return true:��½��false:δ��½��
	 * @throws RemoteException
	 */
	public boolean isLogon(String traderID, String sessionID)
			throws RemoteException {
		boolean isOnline = false;
		
		try {
			LogonManager logonManager = server.getLogonManager();

			isOnline = logonManager.isLogon(traderID, sessionID);
			// checkuser����
			AppMonitor.getInstance().add(Constants.MON_CHECK_COUNT, 1);
			if (isOnline) {
				// checkuser�ɹ�����
				AppMonitor.getInstance().add(Constants.MON_CHECK_SUCCESS, 1);
			}

			if (Constants.SESSION_SYNC_MODE_NORMAL 
					== SyncServer.getInstance().getSyncMode()) {
				if (!isOnline) { // ����ģʽ���û�������,��ȥ�Զ˻�ȡһ��������Ϣ,����������
					SyncData newSyncData = SyncServer.getInstance().SyncGet(sessionID, traderID);
		
					if (null != newSyncData 
							&& null != newSyncData.getAUValue() 
							&& null != newSyncData.getPrivilege()) { // ����Ϊnull,���ʾ����
						isOnline = true;
		
						// ��������Ϣ�����ڱ���
						newSyncData.getAUValue().setLastTime(System.currentTimeMillis());
						LogonManager.getActiveUserManager().put(sessionID, newSyncData.getAUValue());
						
						LogonManager.getActiveUserManager().putPrivilegeQueue(sessionID, newSyncData.getPrivilege(), "isLogon");
						
					}
				}
			} else {
				if (isOnline) { // ͬ�����첽ģʽ���û�����,ͬ��һ�λ״̬
					SyncServer.getInstance().SyncActive(sessionID, traderID);
				}
			}

		} catch (Exception e) {
			String rmiClientHostIP = getRMIClientHostIP();
			log.error(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"��֤����Ա�Ƿ��½", new Object[]{traderID, sessionID}, 
					"ʧ��" + e, isOnline));
			throw new RuntimeException(e);
		}
		
		return isOnline;
	}

	/**
	 * ��֤����Ա��¼״̬
	 * 
	 * @param traderID
	 *            �� sessionID
	 * @return 0:��½��1:δ��½����sessionʧЧ 2:�û��������ط���¼ ��ǰ��¼���ߣ�
	 * @throws RemoteException
	 */
	public int getLogonStatus(String traderID, String sessionID)
			throws RemoteException {
		String rmiClientHostIP = getRMIClientHostIP();
		
		try {
			LogonManager logonManager = server.getLogonManager();
			
			boolean isLogon = isLogon(traderID, sessionID);
			if (isLogon) {
				return 0;
			} else {
				String userId = logonManager.getUserID(sessionID);

				// ���userIdΪ�� ˵��sessionID��Ч �����Ѿ�ʧЧ
				if (userId == null) {
					String[] array = LogonManager.getActiveUserManager()
							.getAllUsersSys(traderID);
					// ���au����Ȼ����һ���û�ID ˵����session�Ѿ�ʧЧ���� �����µĵط���½
					if (array != null && array.length > 0) {
						return 2;
					}
				}
			}
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"��֤����Ա��¼״̬", new Object[]{traderID, sessionID}, 
					"ʧ��" + e, "1"));
			throw new RuntimeException(e);
		}
		
		return 1;
	}

	/**
	 * ����traderID����TraderInfo��������
	 * 
	 * @param traderID
	 * @return TraderInfo
	 * @throws RemoteException
	 *             *
	 */
	private TraderInfo getTraderInfo(String traderID) throws RemoteException {
		TraderInfo traderInfo = null;
		String rmiClientHostIP = getRMIClientHostIP();
		
		try {
			log.info(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"����traderID����TraderInfo��������", new Object[]{traderID}, "��", "NA"));
			
			LogonManager logonManager = server.getLogonManager();

			traderInfo = logonManager.getTraderInfo(traderID);
			log.info(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"����traderID����TraderInfo��������", new Object[]{traderID}, 
					"�ɹ�", traderInfo));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"����traderID����TraderInfo��������", new Object[]{traderID}, 
					"ʧ��" + e, traderInfo));
			throw new RuntimeException(e);
		}

		return traderInfo;
	}

	/**
	 * ����traderInfo����Privilege��������
	 * 
	 * @param traderInfo
	 * @return Privilege
	 * @throws RemoteException
	 */
	public Privilege getPrivilege(TraderInfo traderInfo) throws RemoteException {
		Date now = new Date();
		Date now1 = new Date();
		LogUtil.beginLog(this.getClass(), "LogonRMIImpl.getPrivilege(TraderInfo)", traderInfo.auSessionId, now1);
		
		Privilege privilege = null;
		String rmiClientHostIP = getRMIClientHostIP();

		try {
			log.info(LogUtil.getRmiMessage(traderInfo.traderId, rmiClientHostIP, 
					"����traderInfo����Privilege��������", 
					new Object[]{traderInfo}, "��", "NA"));
			
			UserDAO userDAO = server.getUserDAO();
			TradeQueryDAO tradeQueryDAO = server.getTradeQueryDAO();
			now = new Date();
			LogUtil.beginLog(this.getClass(), "UserDAO.getTradePrivilege", traderInfo.auSessionId, now);
			privilege = userDAO.getTradePrivilege(traderInfo);
			LogUtil.endLog(this.getClass(), "UserDAO.getTradePrivilege", traderInfo.auSessionId, now);
			
			now = new Date();
			LogUtil.beginLog(this.getClass(), "TradeQueryDAO.getTradePrivilege", traderInfo.auSessionId, now);
			privilege = tradeQueryDAO.getTradePrivilege(privilege);
			LogUtil.endLog(this.getClass(), "TradeQueryDAO.getTradePrivilege", traderInfo.auSessionId, now);
			
			now = new Date();
			LogUtil.beginLog(this.getClass(), "UserDAO.getFirmInfoById", traderInfo.auSessionId, now);
			Map firmInfo = userDAO.getFirmInfoById(privilege.getFirmId());
			LogUtil.endLog(this.getClass(), "UserDAO.getFirmInfoById", traderInfo.auSessionId, now);
			if(firmInfo != null && firmInfo.get("name") != null){				
				privilege.setM_FirmName(firmInfo.get("name").toString());
			}
			log.info(LogUtil.getRmiMessage(traderInfo.traderId, rmiClientHostIP, 
					"����traderInfo����Privilege��������", 
					new Object[]{traderInfo}, "�ɹ�", privilege));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(traderInfo.traderId, rmiClientHostIP, 
					"����traderInfo����Privilege��������", 
					new Object[]{traderInfo}, "ʧ��" + e, privilege));
			throw new RuntimeException(e);
		}finally{
			LogUtil.endLog(this.getClass(), "LogonRMIImpl.getPrivilege(TraderInfo)", traderInfo.auSessionId, now1);
		}
		
		return privilege;
	}

	/**
	 * ����auSessionId����Privilege��������
	 * 
	 * @param auSessionId
	 * @return Privilege
	 * @throws RemoteException
	 */
	public Privilege getPrivilege(String auSessionId) throws RemoteException {
		Date now =new Date();
		LogUtil.beginLog(this.getClass(), "LogonRMIImpl.getPrivilege(auSessionId)", auSessionId, now);
		Privilege privilege = null;
		String rmiClientHostIP = getRMIClientHostIP();
		
		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, "����auSessionId����Privilege��������", 
					new Object[]{auSessionId}, "��"));
			
			if (null != getUserID(auSessionId)) {
				privilege = server.getPrivilegeQueue().get(auSessionId);
			}
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"����auSessionId����Privilege��������", 
					new Object[]{auSessionId}, "�ɹ�", privilege));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"����auSessionId����Privilege��������", 
					new Object[]{auSessionId}, "ʧ��" + e, privilege));
			
			throw new RuntimeException(e);
		}finally{
			LogUtil.endLog(this.getClass(), "LogonRMIImpl.getPrivilege(auSessionId)", auSessionId, now);
		}
		
		return  privilege;
	}

	/**
	 * ����traderID����SessionContext��������
	 * 
	 * @param traderID
	 * @return SessionContext
	 * @throws RemoteException
	 */
	public SessionContext getSessionContext(String traderID) throws RemoteException {
		SessionContext sessionContext = null;
		String rmiClientHostIP = getRMIClientHostIP();
		
		try {
			log.info(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"����traderID����SessionContext��������", 
					new Object[]{traderID}, "��", "NA"));
			
			TraderInfo traderInfo = getTraderInfo(traderID);
			Privilege privilege = null;
			if (null != traderInfo) {
				privilege = getPrivilege(traderInfo);
			}

			// �����µ�ȡ�Ự�����ļ���
			AppMonitor.getInstance().add(Constants.MON_GET_CONTEXT, 1);
			
			sessionContext = new SessionContext(traderInfo, privilege);
			log.info(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"����traderID����SessionContext��������", 
					new Object[]{traderID}, "�ɹ�", sessionContext));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"����traderID����SessionContext��������", 
					new Object[]{traderID}, "ʧ��" + e, sessionContext));
			
			throw new RuntimeException(e);
		}
		
		return sessionContext; 
	}

	/**
	 * ����traderID, auSessionId����SessionContext��������
	 * 
	 * @param traderID
	 * @param auSessionId
	 * @return SessionContext
	 * @throws RemoteException
	 */
	public SessionContext getSessionContext(String traderID, 
			String auSessionId) throws RemoteException {
		Date now =new Date();
		LogUtil.beginLog(this.getClass(), "LogonRMIImpl.getSessionContext", auSessionId, now);
		SessionContext sessionContext = null;
		String rmiClientHostIP = getRMIClientHostIP();

		try {
			log.info(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"����traderID, auSessionId����SessionContext��������", 
					new Object[]{traderID, auSessionId}, "��", "NA"));
			
			Privilege privilege = getPrivilege(auSessionId);

			// �ͻ���ǰ�û�ȡ�Ự�����ļ���
			AppMonitor.getInstance().add(Constants.MON_CLIENT_CONTEXT, 1);

			sessionContext = new SessionContext(null, privilege);
			log.info(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"����traderID, auSessionId����SessionContext��������", 
					new Object[]{traderID, auSessionId}, "�ɹ�", sessionContext));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"����traderID, auSessionId����SessionContext��������", 
					new Object[]{traderID, auSessionId}, "ʧ��" + e, sessionContext));
			
			throw new RuntimeException(e);
		}finally{
			LogUtil.endLog(this.getClass(), "LogonRMIImpl.getSessionContext", auSessionId, now);
		}
		
		return sessionContext;
	}

	/**
	 * ����Աע����½
	 * 
	 * @param sessionID
	 * @throws RemoteException
	 */
	private void logoff(String sessionID) throws RemoteException {
		String rmiClientHostIP = getRMIClientHostIP();

		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"����Աע����½", new Object[]{sessionID}, "��"));
			
			LogonManager logonManager = server.getLogonManager();
			logonManager.logoff(sessionID);
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"����Աע����½", new Object[]{sessionID}, "�ɹ�"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"����Աע����½", new Object[]{sessionID}, "ʧ��" + e));

			throw new RuntimeException(e);
		}
	}

	/**
	 * ����Աע����½
	 * 
	 * @param tradeID
	 * @param sessionID
	 * @param ip
	 * @param note
	 *            ��ע
	 * @throws RemoteException
	 */
	public void logoff(String traderID, String sessionID, String ip, String note)
			throws RemoteException {
		String myNote = note;
		String rmiClientHostIP = getRMIClientHostIP();
		
		try {
			log.info(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"�˳����׷�����", new Object[]{sessionID, traderID, ip, note}, "��", "NA"));
			
			LogonManager logonManager = server.getLogonManager();

			String userId = logonManager.getUserID(sessionID);
			boolean sessionOffline = true; // �˻Ự�Ƿ������������϶�������

			if (null == userId) { // ����û�д��û���������Ϣ
				if (Constants.SESSION_SYNC_MODE_NORMAL 
						== SyncServer.getInstance().getSyncMode()) { // ����ģʽҪ�鿴�Զ�
					// ���Զ��Ƿ��д��˵�������Ϣ
					SyncData newSyncData = SyncServer.getInstance().SyncGet(sessionID, traderID);
					if (null != newSyncData && null != newSyncData.getAUValue() 
							&& null != newSyncData.getPrivilege()) {
						sessionOffline = false; // �Զ��д˻Ự��Ϣ,��ʾ֮ǰ��δ����
					}
				}
			} else {
				if (!userId.equals(traderID)) { // ��session��Ϣ��ȡ�õĽ���ԱID�봫�����Ĳ���ͬ
					return;
				}

				logoff(sessionID); // ע������
				sessionOffline = false; // �����д˻Ự��Ϣ,��ʾ֮ǰ��δ����
			}

			if (!sessionOffline) { // ֮ǰδ����,�����޸�Ϊ����
				if (note == null || 0 == note.length()) {
					myNote = "�˳����׷�����";
				}
				// �޸�����״̬
				Server.getUserDAO().Logout(sessionID, traderID, ip, myNote);
			}
			
			// �����˳�����
			AppMonitor.getInstance().add(Constants.MON_LOGOFF_COUNT, 1);
			// ��ǰ���߽���Ա��
			int onlineTraderNum = LogonManager.getActiveUserManager().
					getOnlineTraderNum();
			AppMonitor.getInstance().set(Constants.MON_TRADER_NUM, onlineTraderNum);
			// ��ǰ���߻Ự��
			int onlineSessionNum = LogonManager.getActiveUserManager().
					getOnlineSessionNum();
			AppMonitor.getInstance().set(Constants.MON_SESSION_NUM, onlineSessionNum);
			
			// ���ñ�����ע��
			SyncServer.getInstance().SyncRemove(sessionID, traderID);
			
			log.info(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"�˳����׷�����", new Object[]{sessionID, traderID, ip, myNote}, 
					"�ɹ�", "NA"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"�˳����׷�����", new Object[]{sessionID, traderID, ip, myNote}, 
					"ʧ��" + e, "NA"));

			throw new RuntimeException(e);
		}
	}

	/**
	 * ����sessionIDȡ�ý���ԱID
	 * 
	 * @param sessionID
	 * @return ���ؽ���ԱID
	 * @throws RemoteException
	 */
	public String getUserID(String sessionID) throws RemoteException {
		Date now =new Date();
		LogUtil.beginLog(this.getClass(), "LogonRMIImpl.getUserID", sessionID, now);
		String traderId = null;
		String rmiClientHostIP = getRMIClientHostIP();
		
		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"����sessionIDȡ�ý���ԱID", new Object[]{sessionID}, "��", "NA"));

			LogonManager logonManager = server.getLogonManager();

			traderId = logonManager.getUserID(sessionID);

			if (Constants.SESSION_SYNC_MODE_NORMAL 
					== SyncServer.getInstance().getSyncMode()) {
				if (null == traderId) { // ����ģʽ���û�������,��ȥ�Զ˻�ȡһ��������Ϣ,����������
					SyncData newSyncData = SyncServer.getInstance().SyncGet(sessionID, traderId);

					if (null != newSyncData && null != newSyncData.getAUValue() 
							&& null != newSyncData.getPrivilege()) { // ����Ϊnull,���ʾ����
						traderId = newSyncData.getAUValue().getUserName();
		
						// ��������Ϣ�����ڱ���
						newSyncData.getAUValue().setLastTime(System.currentTimeMillis());
						LogonManager.getActiveUserManager().put(sessionID, 
								newSyncData.getAUValue());
						
						LogonManager.getActiveUserManager().putPrivilegeQueue(sessionID, newSyncData.getPrivilege(), "getUserID");
					}
				}
			} else {
				if (null != traderId) { // ͬ�����첽ģʽ���û�����,ͬ��һ�λ״̬
					SyncServer.getInstance().SyncActive(sessionID, traderId);
				}
			}
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"����sessionIDȡ�ý���ԱID", new Object[]{sessionID}, 
					"ʧ��", traderId));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"����sessionIDȡ�ý���ԱID", new Object[]{sessionID}, 
					"ʧ��" + e, traderId));

			throw new RuntimeException(e);
		}finally{
			LogUtil.endLog(this.getClass(), "LogonRMIImpl.getUserID", sessionID, now);
		}
		
		return traderId;
	}

	/**
	 * �������߽���Ա �������е�ǰ��Ч�ĵ�¼�û��������MUTIL_MODEģʽ�£�ͬһ���û��ж�������򷵻ض�����¼
	 * ����һ���ַ�������,�����е�ÿһ��Ԫ�ش���һ���û���¼����,�������û�ID,��¼��ʱ��͵�½IP,��","���Էָ���
	 * @return list
	 * @throws RemoteException
	 */
	public List getTraders() throws RemoteException {
		List lst = null;
		String rmiClientHostIP = getRMIClientHostIP();

		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"�������߽���Ա", new Object[]{"NA"}, "��", "NA"));
			
			LogonManager logonManager = server.getLogonManager();
			
			lst = new ArrayList();
			String[] sArr = logonManager.getAllUsers();
			String[] userArr;
			for (int i = 0, len = sArr.length; i < len; i++) {
				// userArr = sArr[i].split(",");
				userArr = StringUtil.split(sArr[i], ",");
				Map map = new HashMap();
				map.put("traderID", userArr[0]);
				map.put("loginTime", userArr[1]);
				map.put("lastTime", userArr[2]);
				map.put("loginIP", userArr[3]);
				map.put("sessionID", userArr[4]);
				lst.add(map);
			}
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"�������߽���Ա", new Object[]{"NA"}, "�ɹ�", lst));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"�������߽���Ա", new Object[]{"NA"}, "ʧ��" + e, lst));
			
			throw new RuntimeException(e);
		}
		
		return lst;
	}

	/**
	 * �������߽���Ա��
	 * @return int
	 * @throws RemoteException
	 */
	public int getOnlineTraderNum() throws RemoteException {
		int num = 0;
		String rmiClientHostIP = getRMIClientHostIP();

		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"�������߽���Ա��", new Object[]{"NA"}, "��", "NA"));
			
			num = server.getLogonManager().getActiveUserManager().
					getOnlineTraderNum();
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"�������߽���Ա��", new Object[]{"NA"}, "�ɹ�", num));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"�������߽���Ա��", new Object[]{"NA"}, "ʧ��" + e, num));

			throw new RuntimeException(e);
		}
		
		return num; 
	}

	/**
	 * �������߻Ự��
	 * @return int
	 * @throws RemoteException
	 */
	public int getOnlineSessionNum() throws RemoteException {
		int num = 0;
		String rmiClientHostIP = getRMIClientHostIP();

		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"�������߻Ự��", new Object[]{"NA"}, "��", "NA"));
			
			num = server.getLogonManager().getActiveUserManager().
					getOnlineSessionNum();
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"�������߻Ự��", new Object[]{"NA"}, "�ɹ�", num));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"�������߻Ự��", new Object[]{"NA"}, "ʧ��" + e, num));
			throw new RuntimeException(e);
		}
		
		return num;
	}

	/**
	 * �������߽���Ա
	 * @return Map
	 * @throws RemoteException
	 */
	public Map getOnlineTrader() throws RemoteException {
		Map trader = null;
		String rmiClientHostIP = getRMIClientHostIP();

		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"�������߽���Ա", new Object[]{"NA"}, "��", "NA"));
			
			trader = server.getLogonManager().getActiveUserManager().getOnlineTrader();
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"�������߽���Ա", new Object[]{"NA"}, "�ɹ�", trader));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"�������߽���Ա", new Object[]{"NA"}, "ʧ��" + e, trader));

			throw new RuntimeException(e);
		}
		
		return trader;
	}

	/**
	 * �������߻Ự
	 * @return Map
	 * @throws RemoteException
	 */
	public Map getOnlineSession() throws RemoteException {
		Map session = null;
		String rmiClientHostIP = getRMIClientHostIP();
		
		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"�������߻Ự", new Object[]{"NA"}, "��", "NA"));
			
			session = server.getLogonManager().getActiveUserManager().
					getOnlineSession();
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"�������߻Ự", new Object[]{"NA"}, "�ɹ�", session));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"�������߻Ự", new Object[]{"NA"}, "ʧ��" + e, session));
			
			throw new RuntimeException(e);
		}
		
		return session;
	}

	/**
	 * ǿ�Ƶ�ǰ��½�Ľ���ԱLOGOFF������SESSION ��������Ȩ�޿���,ǿ�����µ�½
	 * @param traderID
	 * @throws RemoteException
	 */
	public void kickOnlineTrader(String traderID) throws RemoteException {
		String rmiClientHostIP = getRMIClientHostIP();
		
		try {
			log.info(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"ǿ�Ƶ�ǰ��½�Ľ���Ա", new Object[]{traderID}, "��", "NA"));
			
			LogonManager logonManager = server.getLogonManager();

			String[] array = LogonManager.getActiveUserManager().
					getAllUsersSys(traderID);
			if (array != null && array.length > 0) {
				String[] userArr;
				for (int i = 0, len = array.length; i < len; i++) {
					userArr = StringUtil.split(array[i], ",");
					kickOnlineSession(userArr[4]);
				}
			}
			log.info(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"ǿ�Ƶ�ǰ��½�Ľ���Ա", new Object[]{traderID}, "�ɹ�", "NA"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"ǿ�Ƶ�ǰ��½�Ľ���Ա", new Object[]{traderID}, "ʧ��" + e, "NA"));
			
			throw new RuntimeException(e);
		}
	}

	/**
	 * ���޸Ľ���������ʱӦ�����н���Ա���µ�¼,���������̴ӽ������б���ɾ��
	 * 
	 * @param traerID
	 * @throws RemoteException
	 */
	public void kickAllTrader(String firmID) throws RemoteException {
		String rmiClientHostIP = getRMIClientHostIP();

		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"kickAllTrader", new Object[]{firmID}, "��", "NA"));
			
			LogonManager logonManager = server.getLogonManager();
			
			if (null != server.getPrivilegeQueue()) {
				Enumeration<String> keys = server.getPrivilegeQueue().keys();
				
				while (keys.hasMoreElements()) {
					String key = (String) keys.nextElement();
					Privilege privilege = server.getPrivilegeQueue().get(key);
					
					if (null == privilege) {
						continue;
					}
					
					String id = privilege.getTraderID();
					
					if (firmID.equals(privilege.getFirmId())) {
						kickOnlineTrader(id);
					}
				}
			}

			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"kickAllTrader", new Object[]{firmID}, "�ɹ�", "NA"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"kickAllTrader", new Object[]{firmID}, "ʧ��" + e, "NA"));

			throw new RuntimeException(e);
		}
	}

	/**
	 * �޸Ľ���Ա����
	 * 
	 * @param userId
	 * @param password
	 * @return �ɹ�����0��-1��ԭ�����ȷ�� -2�����쳣
	 * @throws RemoteException
	 */
	public int changePassowrd(String userId, String passwordOld, String password,
			String operateIP) throws RemoteException {
		int rst = 0;
		
		try {
			log.info(LogUtil.getRmiMessage(userId, operateIP, 
					"�޸Ľ���Ա����", new Object[]{userId, passwordOld,
					password, operateIP}, "��", "NA"));
			// �޸Ľ����������
			AppMonitor.getInstance().add(Constants.MON_PASSWORD_COUNT, 1);

			// return logonManager.changePassowrd(userId, passwordOld, password);
			// 2���޸Ŀͻ��ĵ绰����

		    rst = Server.getUserDAO().changePassowrd(userId, passwordOld,
					password, operateIP);
			if (0  != rst){
				log.info(LogUtil.getRmiMessage(userId, operateIP, 
						"�޸Ľ���Ա����", new Object[]{userId, passwordOld,
						password, operateIP}, "ʧ��", rst));
				// �޸Ľ�������ʧ�ܼ���
				AppMonitor.getInstance().add(Constants.MON_PASSWORD_FAIL, 1);
			} else {
				// �޸Ľ�������ɹ�����
				log.info(LogUtil.getRmiMessage(userId, operateIP, 
						"�޸Ľ���Ա����", new Object[]{userId, passwordOld,
						password, operateIP}, "�ɹ�", rst));
				AppMonitor.getInstance().add(Constants.MON_PASSWORD_SUCCESS, 1);
			}
		} catch (Exception e) {
			String rmiClientHostIP = getRMIClientHostIP();
			log.error(LogUtil.getRmiMessage(userId, rmiClientHostIP, 
					"�޸Ľ���Ա����", new Object[]{userId, passwordOld,
					password, operateIP}, "ʧ��" + e, rst));

			throw new RuntimeException(e);
		}
		
		return rst;
	}
	
	/**
	 * �޸Ĵ�Ϊί�еĵ绰����
	 * 
	 * @param userId
	 *            ����ԱID
	 * @param passwordOld
	 *            ԭ����
	 * @param password
	 *            ������
	 * @return �ɹ�����0��-1��ԭ�����ȷ�� -2�����쳣
	 * @throws RemoteException
	 */
	public int changePhonePassowrd(String userId, String passwordOld,
			String password, String operateIP) throws RemoteException {
		int rst = 0;
		
		try {
			log.info(LogUtil.getRmiMessage(userId, operateIP, 
					"�޸Ĵ�Ϊί�еĵ绰����", new Object[]{userId, passwordOld,
					password, operateIP}, "��", "NA"));
			// �޸ĵ绰�������
			AppMonitor.getInstance().add(Constants.MON_PHONEPWD_COUNT, 1);

		    rst = Server.getUserDAO().changePhonePassowrd(userId, passwordOld,
					password, operateIP);
			if (0 != rst){
				log.info(LogUtil.getRmiMessage(userId, operateIP, 
						"�޸Ĵ�Ϊί�еĵ绰����", new Object[]{userId, passwordOld,
						password, operateIP}, "ʧ��", rst));
				// �޸ĵ绰����ʧ�ܼ���
				AppMonitor.getInstance().add(Constants.MON_PHONEPWD_FAIL, 1);
			} else {
				// �޸ĵ绰����ɹ�����
				log.info(LogUtil.getRmiMessage(userId, operateIP, 
						"�޸Ĵ�Ϊί�еĵ绰����", new Object[]{userId, passwordOld,
						password, operateIP}, "�ɹ�", rst));
				AppMonitor.getInstance().add(Constants.MON_PHONEPWD_SUCCESS, 1);
			}
			// 2���޸Ŀͻ��ĵ绰����
		} catch (Exception e) {
			String rmiClientHostIP = getRMIClientHostIP();
			log.error(LogUtil.getRmiMessage(userId, rmiClientHostIP, 
					"�޸Ĵ�Ϊί�еĵ绰����", new Object[]{userId, passwordOld,
					password, operateIP}, "ʧ��" + e, rst));
			
			throw new RuntimeException(e);
		}
		
		return rst;
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
			String phonePassword) throws RemoteException {
		long ret = 0;
		String rmiClientHostIP = getRMIClientHostIP();

		try {
			log.info(LogUtil.getRmiMessage(memberID, rmiClientHostIP, 
					"��������Ϣ", new Object[]{memberID, customerID,
					phonePassword}, "��", "NA"));
			// ��������Ϣ����
			AppMonitor.getInstance().add(Constants.MON_DELEGATE_COUNT, 1);

		    ret = Server.getUserDAO().checkDelegateInfo(memberID, customerID,
					phonePassword);
			if (0 == ret) {
				// ��������Ϣ�ɹ�����
				log.info(LogUtil.getRmiMessage(memberID, rmiClientHostIP, 
						"��������Ϣ", new Object[]{memberID, customerID,
						phonePassword}, "�ɹ�", ret));
				
				AppMonitor.getInstance().add(Constants.MON_DELEGATE_SUCCESS, 1);
			} else {
				// ��������Ϣʧ�ܼ���
				log.info(LogUtil.getRmiMessage(memberID, rmiClientHostIP, 
						"��������Ϣ", new Object[]{memberID, customerID,
						phonePassword}, "ʧ��", ret));
				
				AppMonitor.getInstance().add(Constants.MON_DELEGATE_FAIL, 1);
			}
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"��������Ϣ", new Object[]{memberID, customerID,
					phonePassword}, "ʧ��" + e, ret));
			
			throw new RuntimeException(e);
		}
		
		return ret;
	}

	/**
	 * ǿ����ֹһ���Ự
	 * 
	 * @param sessionID
	 * @throws RemoteException 
	 */
	public void kickOnlineSession(String sessionID) throws RemoteException {
		String rmiClientHostIP = getRMIClientHostIP();

		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"ǿ����ֹһ���Ự", new Object[]{sessionID}, "��", "NA"));
			
			LogonManager logonManager = server.getLogonManager();

			String traderId = logonManager.getUserID(sessionID);
			boolean sessionOffline = true; // �˻Ự�Ƿ������������϶�������

			if (null == traderId) { // ����û�д��û���������Ϣ
				if (Constants.SESSION_SYNC_MODE_NORMAL 
						== SyncServer.getInstance().getSyncMode()) { // ����ģʽҪ�鿴�Զ�
					// ���Զ��Ƿ��д��˵�������Ϣ
					SyncData newSyncData = SyncServer.getInstance().
							SyncGet(sessionID, traderId);
					if (null != newSyncData && null != newSyncData.getAUValue() 
							&& null != newSyncData.getPrivilege()) {
						sessionOffline = false; // �Զ��д˻Ự��Ϣ,��ʾ֮ǰ��δ����
					}
				}
			} else {
				logonManager.logoffSession(sessionID); // ע������
				sessionOffline = false; // �����д˻Ự��Ϣ,��ʾ֮ǰ��δ����
			}

			if (!sessionOffline) { // ֮ǰδ����,�����޸�Ϊ����
				Server.getUserDAO().Logout(sessionID, "System", 
						null, "����Ա��" + traderId + "���ĻỰ��" + sessionID + "���ѱ�ǿ����ֹ");
				
				log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
						"ǿ����ֹһ���Ự", new Object[]{sessionID}, "�ɹ�", "NA"));
			}

			// ���ñ�����ע��
			SyncServer.getInstance().SyncRemove(sessionID, traderId);
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"ǿ����ֹһ���Ự", new Object[]{sessionID}, "ʧ��" + e, "NA"));

			throw new RuntimeException(e);
		}
	}

	/**
	 * ǿ����ֹ���ص�һ���Ự
	 * 
	 * @param sessionID
	 * @throws RemoteException
	 */
	public void kickLocalOnlineSession(String sessionID) throws RemoteException {
		String rmiClientHostIP = getRMIClientHostIP();
		
		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"ǿ����ֹ���ص�һ���Ự", new Object[]{sessionID}, "��", "NA"));

			LogonManager logonManager = server.getLogonManager();

			String traderId = logonManager.getUserID(sessionID);

			if (null == traderId) { // ����û�д��û���������Ϣ
				return;
			} else {
				logonManager.logoffSession(sessionID); // ע������
			}

			boolean sessionOffline = true; // �˻Ự�Ƿ������������϶�������
			if (Constants.SESSION_SYNC_MODE_NORMAL 
					== SyncServer.getInstance().getSyncMode()) { // ����ģʽҪ�鿴�Զ�
				// ���Զ��Ƿ��д��˵�������Ϣ
				SyncData newSyncData = SyncServer.getInstance().
						SyncGet(sessionID, traderId);
				if (null != newSyncData && null != newSyncData.getAUValue() 
						&& null != newSyncData.getPrivilege()) {
					sessionOffline = false; // �Զ��д˻Ự��Ϣ,��ʾ���ڻ�����
				}
			}

			if (sessionOffline) { // �����������������,���޸�����״̬
				Server.getUserDAO().Logout(sessionID, "System", 
						null, "����Ա��" + traderId + "���ĻỰ��" + sessionID + "���ѱ�ǿ����ֹ");
				
				log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
						"ǿ����ֹ���ص�һ���Ự", new Object[]{sessionID}, "�ɹ�", "NA"));
			}
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"ǿ����ֹ���ص�һ���Ự", new Object[]{sessionID}, "ʧ��" + e, "NA"));

			throw new RuntimeException(e);
		}
	}

	/**
	 * ��ȡ����˹���ļ�����Կ
	 * 
	 * @return ����˹���ļ�����Կ
	 * @throws RemoteException
	 */
	public byte[] getServerKey() throws RemoteException {
		byte[] key = null;
		String rmiClientHostIP = getRMIClientHostIP();
		
		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"��ȡ����˹���ļ�����Կ", new Object[]{"NA"}, "��"));
			
			LogonManager logonManager = server.getLogonManager();
			key = logonManager.getServerKey();
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"��ȡ����˹���ļ�����Կ", new Object[]{"NA"}, "�ɹ�", key));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"��ȡ����˹���ļ�����Կ", new Object[]{"NA"}, "ʧ��" + e, key));

			throw new RuntimeException(e);
		}

		return key;
	}

	/**
	 * �����֤��
	 * @param key
	 * @param value
	 * @throws RemoteException
	 */
	public void putValidCode(String key, String value) throws RemoteException {
		String rmiClientHostIP = getRMIClientHostIP();
		
		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"�����֤��", new Object[]{key, value}, "��", "NA"));

			// ����֤�����
			AppMonitor.getInstance().add(Constants.MON_PUTCODE_COUNT, 1);
			server.getVaidCodeQueue().put(key, new ValidCode(value)); // ֻ���ڱ���
			
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"�����֤��", new Object[]{key, value}, "�ɹ�", "NA"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"�����֤��", new Object[]{key, value}, "ʧ��" + e, "NA"));

			throw new RuntimeException(e);
		}
	}

	/**
	 * ��ȡ��֤��
	 * @param key
	 * @return String
	 * @throws RemoteException
	 */
	public String getValidCode(String key) throws RemoteException {
	    ValidCode validCode = null;
		String rmiClientHostIP = getRMIClientHostIP();

		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"��ȡ��֤��", new Object[]{key}, "��", "NA"));
			// ȡ��֤�����
			AppMonitor.getInstance().add(Constants.MON_GETCODE_COUNT, 1);

			validCode = server.getVaidCodeQueue().remove(key);
			if (null == validCode) { // ����û��ȡ��,��Զ�̻�������ȡһ��
				// ȡ��Ϣ(����sessionID��Ϊkey)
				SyncData newSyncData = SyncServer.getInstance().SyncGetValidCode(key);
				if (null == newSyncData) {
					return null;
				}

				validCode = new ValidCode(newSyncData.getTraderID());
			}

			if (null == validCode.getValue()) {
				// ȡ��֤��ʧ�ܼ���
				AppMonitor.getInstance().add(Constants.MON_GETCODE_FAIL, 1);

				log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
						"��ȡ��֤��", new Object[]{key}, "ʧ��", "NA"));
			} else {
				// ȡ��֤��ɹ�����
				AppMonitor.getInstance().add(Constants.MON_GETCODE_SUCCESS, 1);
				
				log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
						"��ȡ��֤��", new Object[]{key}, "�ɹ�", validCode.getValue()));
			}
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"��ȡ��֤��", new Object[]{key}, "ʧ��" + e, validCode.getValue()));

			throw new RuntimeException(e);
		}

		return validCode.getValue();
	}

	/**
	 * ���¼��ػỰ������
	 * @param sessionID
	 * @return SessionContext
	 * @throws RemoteException
	 */
	public SessionContext reloadSessionContext(String sessionID) 
			throws RemoteException {
		SessionContext sessionContext = null;
		String rmiClientHostIP = getRMIClientHostIP();
		
		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"���¼��ػỰ������", new Object[]{sessionID}, "��"));
			
			Privilege privilege = server.getPrivilegeQueue().get(sessionID);
			if (null == privilege) { // �Ự������
				return null;
			}

			TraderInfo traderInfo = getTraderInfo(privilege.getTraderID());
			if (null != traderInfo) {
				traderInfo.retCode = 1;
				traderInfo.auSessionId = sessionID;

				privilege = getPrivilege(traderInfo);

				
				LogonManager.getActiveUserManager().putPrivilegeQueue(sessionID, privilege, "reloadSessionContext");

				// ͬ�����¼��صĻỰ��Ϣ������
				SyncServer.getInstance().SyncUpdate(traderInfo.auSessionId, privilege);
			}

			sessionContext = new SessionContext(traderInfo, privilege);
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"���¼��ػỰ������", new Object[]{sessionID}, 
					"�ɹ�", sessionContext));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"���¼��ػỰ������", new Object[]{sessionID}, 
					"ʧ��" + e, sessionContext));

			throw new RuntimeException(e);
		}
		
		return sessionContext;
	}
}
