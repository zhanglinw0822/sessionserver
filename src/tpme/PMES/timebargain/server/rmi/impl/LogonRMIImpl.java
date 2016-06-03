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
 * 交易RMI实现类.
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

	/*********** 构造函数赋值的 ****************/
	private Server server; // 登陆服务器主控制类

	/**
	 * 构造函数
	 * 
	 * @param Server
	 * @throws RemoteException
	 */
	public LogonRMIImpl(Server server) throws RemoteException {
		this.server = server; 
	}

	/**
	 * 取调用RMI的客户端的主机名称
	 * @return String
	 */
	private String getRMIClientHostName() {
		String clientHostName = null;
		
		try {
			clientHostName = RemoteServer.getClientHost();
		} catch (ServerNotActiveException e) {
			log.error(LogUtil.getSysMessage("取调用RMI的客户端的主机名", "失败" + e));
		} catch (Exception e) {
			log.error(LogUtil.getSysMessage("取调用RMI的客户端的主机名", "失败" + e));
		}

		return clientHostName;
	}

	/**
	 * 取调用RMI的客户端的主机IP
	 * @return String
	 */
	private String getRMIClientHostIP() {
		String clientHostIP = null;
		String clientHostName = getRMIClientHostName();
		if (null != clientHostName) {
			try {
				clientHostIP = InetAddress.getByName(clientHostName).getHostAddress();
			} catch (UnknownHostException e) {
				log.error(LogUtil.getSysMessage("取调用RMI的客户端的主机IP", "失败" + e));
			} catch (Exception e) {
				log.error(LogUtil.getSysMessage("取调用RMI的客户端的主机IP", "失败" + e));
			}
		}

		return clientHostIP;
	}
	
	/**
	 * 交易员登陆
	 * 
	 * @param Trader
	 *            trader 包括ID，密码,key和IP
	 * @return 
	 *         TraderInfo,其中TraderInfo.retCode>0表示成功；-1：交易员代码不存在；-2：口令不正确；-3：
	 *         禁止登陆；-4：Key盘验证错误；-5：其它异常;-6:交易板块被禁止;
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
					"登陆交易服务器", new Object[]{trader}, "中", "NA"));
			// 登录请求计数
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
						"登陆交易服务器", new Object[]{trader}, "成功", traderInfo.auSessionId));

				Privilege privilege = getPrivilege(traderInfo);

				LogonManager.getActiveUserManager().putPrivilegeQueue(traderInfo.auSessionId, privilege, "logon");
				sessionContext.setPrivilege(privilege);

					// 同步本次登录信息到备机
				now = new Date();
				LogUtil.beginLog(this.getClass(), "SyncServer.SyncAdd", traderInfo.auSessionId, now);
				SyncServer.getInstance().SyncAdd(traderInfo.auSessionId, 
						LogonManager.getActiveUserManager().getAUValue(traderInfo.auSessionId), 
						privilege);
				LogUtil.endLog(this.getClass(), "SyncServer.SyncAdd", traderInfo.auSessionId, now);

				//  登录请求成功计数
				AppMonitor.getInstance().add(Constants.MON_LOGON_SUCCESS, 1);
				// 当前在线交易员数
				int onlineTraderNum = LogonManager.getActiveUserManager().
						getOnlineTraderNum();
				AppMonitor.getInstance().set(Constants.MON_TRADER_NUM, onlineTraderNum);
				// 当前在线会话数
				int onlineSessionNum = LogonManager.getActiveUserManager().
						getOnlineSessionNum();
				AppMonitor.getInstance().set(Constants.MON_SESSION_NUM, onlineSessionNum);
			} else {
				log.info(LogUtil.getRmiMessage(trader.getTraderID(), rmiClientHostIP, 
						"登陆交易服务器", new Object[]{trader}, "失败", 
						"retCode: " + sessionContext.getTraderInfo().retCode));

				if (-2 == traderInfo.retCode) {
					// 密码失败导致登录失败计数
					AppMonitor.getInstance().add(Constants.MON_LOGON_FAIL, 1);
				} else {
					//  其他异常导致失败计数
					AppMonitor.getInstance().add(Constants.MON_LOGON_EXCEPTION, 1);
				}
			}
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(trader.getTraderID(), rmiClientHostIP, 
					"登陆交易服务器", new Object[]{trader}, "失败" + e
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
					"登陆交易服务器", new Object[]{traderID}, "中", "NA"));
			// 登录请求计数
			AppMonitor.getInstance().add(Constants.MON_LOGON_COUNT, 1);

			LogonManager logonManager = server.getLogonManager();

			TraderInfo traderInfo = logonManager.logonWithoutpwd(traderID,logonIP,server);

		    sessionContext = new SessionContext();
			sessionContext.setTraderInfo(traderInfo);

			if (traderInfo.retCode > 0) {
				log.info(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
						"登陆交易服务器", new Object[]{traderID}, "成功", traderInfo.auSessionId));

				Privilege privilege = getPrivilege(traderInfo);

				
				LogonManager.getActiveUserManager().putPrivilegeQueue(traderInfo.auSessionId, privilege, "logonWithoutpwd");
				sessionContext.setPrivilege(privilege);

					// 同步本次登录信息到备机
				SyncServer.getInstance().SyncAdd(traderInfo.auSessionId, 
						LogonManager.getActiveUserManager().getAUValue(traderInfo.auSessionId), 
						privilege);

				//  登录请求成功计数
				AppMonitor.getInstance().add(Constants.MON_LOGON_SUCCESS, 1);
				// 当前在线交易员数
				int onlineTraderNum = LogonManager.getActiveUserManager().
						getOnlineTraderNum();
				AppMonitor.getInstance().set(Constants.MON_TRADER_NUM, onlineTraderNum);
				// 当前在线会话数
				int onlineSessionNum = LogonManager.getActiveUserManager().
						getOnlineSessionNum();
				AppMonitor.getInstance().set(Constants.MON_SESSION_NUM, onlineSessionNum);
			} else {
				log.info(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
						"登陆交易服务器", new Object[]{traderID}, "失败", 
						"retCode: " + sessionContext.getTraderInfo().retCode));

				if (-2 == traderInfo.retCode) {
					// 密码失败导致登录失败计数
					AppMonitor.getInstance().add(Constants.MON_LOGON_FAIL, 1);
				} else {
					//  其他异常导致失败计数
					AppMonitor.getInstance().add(Constants.MON_LOGON_EXCEPTION, 1);
				}
			}
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"登陆交易服务器", new Object[]{traderID}, "失败" + e
					, "retCode: " + sessionContext.getTraderInfo().retCode),e);
			throw new RuntimeException(e);
		}

		return sessionContext;
	}
	
	/**
	 * 验证交易员是否登陆
	 * 
	 * @param traderID
	 *            ， sessionID
	 * @return true:登陆；false:未登陆；
	 * @throws RemoteException
	 */
	public boolean isLogon(String traderID, String sessionID)
			throws RemoteException {
		boolean isOnline = false;
		
		try {
			LogonManager logonManager = server.getLogonManager();

			isOnline = logonManager.isLogon(traderID, sessionID);
			// checkuser计数
			AppMonitor.getInstance().add(Constants.MON_CHECK_COUNT, 1);
			if (isOnline) {
				// checkuser成功计数
				AppMonitor.getInstance().add(Constants.MON_CHECK_SUCCESS, 1);
			}

			if (Constants.SESSION_SYNC_MODE_NORMAL 
					== SyncServer.getInstance().getSyncMode()) {
				if (!isOnline) { // 正常模式且用户不在线,将去对端获取一次在线信息,并保存起来
					SyncData newSyncData = SyncServer.getInstance().SyncGet(sessionID, traderID);
		
					if (null != newSyncData 
							&& null != newSyncData.getAUValue() 
							&& null != newSyncData.getPrivilege()) { // 都不为null,则表示在线
						isOnline = true;
		
						// 将在线信息保存在本地
						newSyncData.getAUValue().setLastTime(System.currentTimeMillis());
						LogonManager.getActiveUserManager().put(sessionID, newSyncData.getAUValue());
						
						LogonManager.getActiveUserManager().putPrivilegeQueue(sessionID, newSyncData.getPrivilege(), "isLogon");
						
					}
				}
			} else {
				if (isOnline) { // 同步或异步模式且用户在线,同步一次活动状态
					SyncServer.getInstance().SyncActive(sessionID, traderID);
				}
			}

		} catch (Exception e) {
			String rmiClientHostIP = getRMIClientHostIP();
			log.error(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"验证交易员是否登陆", new Object[]{traderID, sessionID}, 
					"失败" + e, isOnline));
			throw new RuntimeException(e);
		}
		
		return isOnline;
	}

	/**
	 * 验证交易员登录状态
	 * 
	 * @param traderID
	 *            ， sessionID
	 * @return 0:登陆；1:未登陆或者session失效 2:用户在其它地方登录 当前登录被踢；
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

				// 如果userId为空 说明sessionID无效 或者已经失效
				if (userId == null) {
					String[] array = LogonManager.getActiveUserManager()
							.getAllUsersSys(traderID);
					// 如果au中仍然存在一个用户ID 说明此session已经失效但是 又有新的地方登陆
					if (array != null && array.length > 0) {
						return 2;
					}
				}
			}
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"验证交易员登录状态", new Object[]{traderID, sessionID}, 
					"失败" + e, "1"));
			throw new RuntimeException(e);
		}
		
		return 1;
	}

	/**
	 * 根据traderID查找TraderInfo对象属性
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
					"根据traderID查找TraderInfo对象属性", new Object[]{traderID}, "中", "NA"));
			
			LogonManager logonManager = server.getLogonManager();

			traderInfo = logonManager.getTraderInfo(traderID);
			log.info(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"根据traderID查找TraderInfo对象属性", new Object[]{traderID}, 
					"成功", traderInfo));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"根据traderID查找TraderInfo对象属性", new Object[]{traderID}, 
					"失败" + e, traderInfo));
			throw new RuntimeException(e);
		}

		return traderInfo;
	}

	/**
	 * 根据traderInfo查找Privilege对象属性
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
					"根据traderInfo查找Privilege对象属性", 
					new Object[]{traderInfo}, "中", "NA"));
			
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
					"根据traderInfo查找Privilege对象属性", 
					new Object[]{traderInfo}, "成功", privilege));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(traderInfo.traderId, rmiClientHostIP, 
					"根据traderInfo查找Privilege对象属性", 
					new Object[]{traderInfo}, "失败" + e, privilege));
			throw new RuntimeException(e);
		}finally{
			LogUtil.endLog(this.getClass(), "LogonRMIImpl.getPrivilege(TraderInfo)", traderInfo.auSessionId, now1);
		}
		
		return privilege;
	}

	/**
	 * 根据auSessionId查找Privilege对象属性
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
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, "根据auSessionId查找Privilege对象属性", 
					new Object[]{auSessionId}, "中"));
			
			if (null != getUserID(auSessionId)) {
				privilege = server.getPrivilegeQueue().get(auSessionId);
			}
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"根据auSessionId查找Privilege对象属性", 
					new Object[]{auSessionId}, "成功", privilege));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"根据auSessionId查找Privilege对象属性", 
					new Object[]{auSessionId}, "失败" + e, privilege));
			
			throw new RuntimeException(e);
		}finally{
			LogUtil.endLog(this.getClass(), "LogonRMIImpl.getPrivilege(auSessionId)", auSessionId, now);
		}
		
		return  privilege;
	}

	/**
	 * 根据traderID查找SessionContext对象属性
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
					"根据traderID查找SessionContext对象属性", 
					new Object[]{traderID}, "中", "NA"));
			
			TraderInfo traderInfo = getTraderInfo(traderID);
			Privilege privilege = null;
			if (null != traderInfo) {
				privilege = getPrivilege(traderInfo);
			}

			// 代客下单取会话上下文计数
			AppMonitor.getInstance().add(Constants.MON_GET_CONTEXT, 1);
			
			sessionContext = new SessionContext(traderInfo, privilege);
			log.info(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"根据traderID查找SessionContext对象属性", 
					new Object[]{traderID}, "成功", sessionContext));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"根据traderID查找SessionContext对象属性", 
					new Object[]{traderID}, "失败" + e, sessionContext));
			
			throw new RuntimeException(e);
		}
		
		return sessionContext; 
	}

	/**
	 * 根据traderID, auSessionId查找SessionContext对象属性
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
					"根据traderID, auSessionId查找SessionContext对象属性", 
					new Object[]{traderID, auSessionId}, "中", "NA"));
			
			Privilege privilege = getPrivilege(auSessionId);

			// 客户端前置机取会话上下文计数
			AppMonitor.getInstance().add(Constants.MON_CLIENT_CONTEXT, 1);

			sessionContext = new SessionContext(null, privilege);
			log.info(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"根据traderID, auSessionId查找SessionContext对象属性", 
					new Object[]{traderID, auSessionId}, "成功", sessionContext));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"根据traderID, auSessionId查找SessionContext对象属性", 
					new Object[]{traderID, auSessionId}, "失败" + e, sessionContext));
			
			throw new RuntimeException(e);
		}finally{
			LogUtil.endLog(this.getClass(), "LogonRMIImpl.getSessionContext", auSessionId, now);
		}
		
		return sessionContext;
	}

	/**
	 * 交易员注销登陆
	 * 
	 * @param sessionID
	 * @throws RemoteException
	 */
	private void logoff(String sessionID) throws RemoteException {
		String rmiClientHostIP = getRMIClientHostIP();

		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"交易员注销登陆", new Object[]{sessionID}, "中"));
			
			LogonManager logonManager = server.getLogonManager();
			logonManager.logoff(sessionID);
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"交易员注销登陆", new Object[]{sessionID}, "成功"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"交易员注销登陆", new Object[]{sessionID}, "失败" + e));

			throw new RuntimeException(e);
		}
	}

	/**
	 * 交易员注销登陆
	 * 
	 * @param tradeID
	 * @param sessionID
	 * @param ip
	 * @param note
	 *            备注
	 * @throws RemoteException
	 */
	public void logoff(String traderID, String sessionID, String ip, String note)
			throws RemoteException {
		String myNote = note;
		String rmiClientHostIP = getRMIClientHostIP();
		
		try {
			log.info(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"退出交易服务器", new Object[]{sessionID, traderID, ip, note}, "中", "NA"));
			
			LogonManager logonManager = server.getLogonManager();

			String userId = logonManager.getUserID(sessionID);
			boolean sessionOffline = true; // 此会话是否已在主备机上都离线了

			if (null == userId) { // 本地没有此用户的在线信息
				if (Constants.SESSION_SYNC_MODE_NORMAL 
						== SyncServer.getInstance().getSyncMode()) { // 正常模式要查看对端
					// 看对端是否还有此人的在线信息
					SyncData newSyncData = SyncServer.getInstance().SyncGet(sessionID, traderID);
					if (null != newSyncData && null != newSyncData.getAUValue() 
							&& null != newSyncData.getPrivilege()) {
						sessionOffline = false; // 对端有此会话信息,表示之前还未离线
					}
				}
			} else {
				if (!userId.equals(traderID)) { // 从session信息中取得的交易员ID与传过来的不相同
					return;
				}

				logoff(sessionID); // 注销本机
				sessionOffline = false; // 本地有此会话信息,表示之前还未离线
			}

			if (!sessionOffline) { // 之前未离线,本次修改为离线
				if (note == null || 0 == note.length()) {
					myNote = "退出交易服务器";
				}
				// 修改在线状态
				Server.getUserDAO().Logout(sessionID, traderID, ip, myNote);
			}
			
			// 主动退出计数
			AppMonitor.getInstance().add(Constants.MON_LOGOFF_COUNT, 1);
			// 当前在线交易员数
			int onlineTraderNum = LogonManager.getActiveUserManager().
					getOnlineTraderNum();
			AppMonitor.getInstance().set(Constants.MON_TRADER_NUM, onlineTraderNum);
			// 当前在线会话数
			int onlineSessionNum = LogonManager.getActiveUserManager().
					getOnlineSessionNum();
			AppMonitor.getInstance().set(Constants.MON_SESSION_NUM, onlineSessionNum);
			
			// 调用备机的注销
			SyncServer.getInstance().SyncRemove(sessionID, traderID);
			
			log.info(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"退出交易服务器", new Object[]{sessionID, traderID, ip, myNote}, 
					"成功", "NA"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"退出交易服务器", new Object[]{sessionID, traderID, ip, myNote}, 
					"失败" + e, "NA"));

			throw new RuntimeException(e);
		}
	}

	/**
	 * 根据sessionID取得交易员ID
	 * 
	 * @param sessionID
	 * @return 返回交易员ID
	 * @throws RemoteException
	 */
	public String getUserID(String sessionID) throws RemoteException {
		Date now =new Date();
		LogUtil.beginLog(this.getClass(), "LogonRMIImpl.getUserID", sessionID, now);
		String traderId = null;
		String rmiClientHostIP = getRMIClientHostIP();
		
		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"根据sessionID取得交易员ID", new Object[]{sessionID}, "中", "NA"));

			LogonManager logonManager = server.getLogonManager();

			traderId = logonManager.getUserID(sessionID);

			if (Constants.SESSION_SYNC_MODE_NORMAL 
					== SyncServer.getInstance().getSyncMode()) {
				if (null == traderId) { // 正常模式且用户不在线,将去对端获取一次在线信息,并保存起来
					SyncData newSyncData = SyncServer.getInstance().SyncGet(sessionID, traderId);

					if (null != newSyncData && null != newSyncData.getAUValue() 
							&& null != newSyncData.getPrivilege()) { // 都不为null,则表示在线
						traderId = newSyncData.getAUValue().getUserName();
		
						// 将在线信息保存在本地
						newSyncData.getAUValue().setLastTime(System.currentTimeMillis());
						LogonManager.getActiveUserManager().put(sessionID, 
								newSyncData.getAUValue());
						
						LogonManager.getActiveUserManager().putPrivilegeQueue(sessionID, newSyncData.getPrivilege(), "getUserID");
					}
				}
			} else {
				if (null != traderId) { // 同步或异步模式且用户在线,同步一次活动状态
					SyncServer.getInstance().SyncActive(sessionID, traderId);
				}
			}
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"根据sessionID取得交易员ID", new Object[]{sessionID}, 
					"失败", traderId));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"根据sessionID取得交易员ID", new Object[]{sessionID}, 
					"失败" + e, traderId));

			throw new RuntimeException(e);
		}finally{
			LogUtil.endLog(this.getClass(), "LogonRMIImpl.getUserID", sessionID, now);
		}
		
		return traderId;
	}

	/**
	 * 返回在线交易员 返回所有当前有效的登录用户，如果在MUTIL_MODE模式下，同一个用户有多个连接则返回多条记录
	 * 返回一个字符串数组,数组中的每一个元素代表一个用户登录连接,内容是用户ID,登录的时间和登陆IP,用","加以分隔。
	 * @return list
	 * @throws RemoteException
	 */
	public List getTraders() throws RemoteException {
		List lst = null;
		String rmiClientHostIP = getRMIClientHostIP();

		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"返回在线交易员", new Object[]{"NA"}, "中", "NA"));
			
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
					"返回在线交易员", new Object[]{"NA"}, "成功", lst));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"返回在线交易员", new Object[]{"NA"}, "失败" + e, lst));
			
			throw new RuntimeException(e);
		}
		
		return lst;
	}

	/**
	 * 返回在线交易员数
	 * @return int
	 * @throws RemoteException
	 */
	public int getOnlineTraderNum() throws RemoteException {
		int num = 0;
		String rmiClientHostIP = getRMIClientHostIP();

		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"返回在线交易员数", new Object[]{"NA"}, "中", "NA"));
			
			num = server.getLogonManager().getActiveUserManager().
					getOnlineTraderNum();
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"返回在线交易员数", new Object[]{"NA"}, "成功", num));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"返回在线交易员数", new Object[]{"NA"}, "失败" + e, num));

			throw new RuntimeException(e);
		}
		
		return num; 
	}

	/**
	 * 返回在线会话数
	 * @return int
	 * @throws RemoteException
	 */
	public int getOnlineSessionNum() throws RemoteException {
		int num = 0;
		String rmiClientHostIP = getRMIClientHostIP();

		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"返回在线会话数", new Object[]{"NA"}, "中", "NA"));
			
			num = server.getLogonManager().getActiveUserManager().
					getOnlineSessionNum();
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"返回在线会话数", new Object[]{"NA"}, "成功", num));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"返回在线会话数", new Object[]{"NA"}, "失败" + e, num));
			throw new RuntimeException(e);
		}
		
		return num;
	}

	/**
	 * 返回在线交易员
	 * @return Map
	 * @throws RemoteException
	 */
	public Map getOnlineTrader() throws RemoteException {
		Map trader = null;
		String rmiClientHostIP = getRMIClientHostIP();

		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"返回在线交易员", new Object[]{"NA"}, "中", "NA"));
			
			trader = server.getLogonManager().getActiveUserManager().getOnlineTrader();
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"返回在线交易员", new Object[]{"NA"}, "成功", trader));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"返回在线交易员", new Object[]{"NA"}, "失败" + e, trader));

			throw new RuntimeException(e);
		}
		
		return trader;
	}

	/**
	 * 返回在线会话
	 * @return Map
	 * @throws RemoteException
	 */
	public Map getOnlineSession() throws RemoteException {
		Map session = null;
		String rmiClientHostIP = getRMIClientHostIP();
		
		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"返回在线会话", new Object[]{"NA"}, "中", "NA"));
			
			session = server.getLogonManager().getActiveUserManager().
					getOnlineSession();
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"返回在线会话", new Object[]{"NA"}, "成功", session));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"返回在线会话", new Object[]{"NA"}, "失败" + e, session));
			
			throw new RuntimeException(e);
		}
		
		return session;
	}

	/**
	 * 强制当前登陆的交易员LOGOFF，销毁SESSION 用于在线权限控制,强迫重新登陆
	 * @param traderID
	 * @throws RemoteException
	 */
	public void kickOnlineTrader(String traderID) throws RemoteException {
		String rmiClientHostIP = getRMIClientHostIP();
		
		try {
			log.info(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"强制当前登陆的交易员", new Object[]{traderID}, "中", "NA"));
			
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
					"强制当前登陆的交易员", new Object[]{traderID}, "成功", "NA"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(traderID, rmiClientHostIP, 
					"强制当前登陆的交易员", new Object[]{traderID}, "失败" + e, "NA"));
			
			throw new RuntimeException(e);
		}
	}

	/**
	 * 当修改交易商属性时应让所有交易员重新登录,并将交易商从交易商列表中删除
	 * 
	 * @param traerID
	 * @throws RemoteException
	 */
	public void kickAllTrader(String firmID) throws RemoteException {
		String rmiClientHostIP = getRMIClientHostIP();

		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"kickAllTrader", new Object[]{firmID}, "中", "NA"));
			
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
					"kickAllTrader", new Object[]{firmID}, "成功", "NA"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"kickAllTrader", new Object[]{firmID}, "失败" + e, "NA"));

			throw new RuntimeException(e);
		}
	}

	/**
	 * 修改交易员密码
	 * 
	 * @param userId
	 * @param password
	 * @return 成功返回0；-1：原口令不正确； -2操作异常
	 * @throws RemoteException
	 */
	public int changePassowrd(String userId, String passwordOld, String password,
			String operateIP) throws RemoteException {
		int rst = 0;
		
		try {
			log.info(LogUtil.getRmiMessage(userId, operateIP, 
					"修改交易员密码", new Object[]{userId, passwordOld,
					password, operateIP}, "中", "NA"));
			// 修改交易密码计数
			AppMonitor.getInstance().add(Constants.MON_PASSWORD_COUNT, 1);

			// return logonManager.changePassowrd(userId, passwordOld, password);
			// 2、修改客户的电话密码

		    rst = Server.getUserDAO().changePassowrd(userId, passwordOld,
					password, operateIP);
			if (0  != rst){
				log.info(LogUtil.getRmiMessage(userId, operateIP, 
						"修改交易员密码", new Object[]{userId, passwordOld,
						password, operateIP}, "失败", rst));
				// 修改交易密码失败计数
				AppMonitor.getInstance().add(Constants.MON_PASSWORD_FAIL, 1);
			} else {
				// 修改交易密码成功计数
				log.info(LogUtil.getRmiMessage(userId, operateIP, 
						"修改交易员密码", new Object[]{userId, passwordOld,
						password, operateIP}, "成功", rst));
				AppMonitor.getInstance().add(Constants.MON_PASSWORD_SUCCESS, 1);
			}
		} catch (Exception e) {
			String rmiClientHostIP = getRMIClientHostIP();
			log.error(LogUtil.getRmiMessage(userId, rmiClientHostIP, 
					"修改交易员密码", new Object[]{userId, passwordOld,
					password, operateIP}, "失败" + e, rst));

			throw new RuntimeException(e);
		}
		
		return rst;
	}
	
	/**
	 * 修改代为委托的电话密码
	 * 
	 * @param userId
	 *            交易员ID
	 * @param passwordOld
	 *            原密码
	 * @param password
	 *            新密码
	 * @return 成功返回0；-1：原口令不正确； -2操作异常
	 * @throws RemoteException
	 */
	public int changePhonePassowrd(String userId, String passwordOld,
			String password, String operateIP) throws RemoteException {
		int rst = 0;
		
		try {
			log.info(LogUtil.getRmiMessage(userId, operateIP, 
					"修改代为委托的电话密码", new Object[]{userId, passwordOld,
					password, operateIP}, "中", "NA"));
			// 修改电话密码计数
			AppMonitor.getInstance().add(Constants.MON_PHONEPWD_COUNT, 1);

		    rst = Server.getUserDAO().changePhonePassowrd(userId, passwordOld,
					password, operateIP);
			if (0 != rst){
				log.info(LogUtil.getRmiMessage(userId, operateIP, 
						"修改代为委托的电话密码", new Object[]{userId, passwordOld,
						password, operateIP}, "失败", rst));
				// 修改电话密码失败计数
				AppMonitor.getInstance().add(Constants.MON_PHONEPWD_FAIL, 1);
			} else {
				// 修改电话密码成功计数
				log.info(LogUtil.getRmiMessage(userId, operateIP, 
						"修改代为委托的电话密码", new Object[]{userId, passwordOld,
						password, operateIP}, "成功", rst));
				AppMonitor.getInstance().add(Constants.MON_PHONEPWD_SUCCESS, 1);
			}
			// 2、修改客户的电话密码
		} catch (Exception e) {
			String rmiClientHostIP = getRMIClientHostIP();
			log.error(LogUtil.getRmiMessage(userId, rmiClientHostIP, 
					"修改代为委托的电话密码", new Object[]{userId, passwordOld,
					password, operateIP}, "失败" + e, rst));
			
			throw new RuntimeException(e);
		}
		
		return rst;
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
			String phonePassword) throws RemoteException {
		long ret = 0;
		String rmiClientHostIP = getRMIClientHostIP();

		try {
			log.info(LogUtil.getRmiMessage(memberID, rmiClientHostIP, 
					"检查代理信息", new Object[]{memberID, customerID,
					phonePassword}, "中", "NA"));
			// 检查代理信息计数
			AppMonitor.getInstance().add(Constants.MON_DELEGATE_COUNT, 1);

		    ret = Server.getUserDAO().checkDelegateInfo(memberID, customerID,
					phonePassword);
			if (0 == ret) {
				// 检查代理信息成功计数
				log.info(LogUtil.getRmiMessage(memberID, rmiClientHostIP, 
						"检查代理信息", new Object[]{memberID, customerID,
						phonePassword}, "成功", ret));
				
				AppMonitor.getInstance().add(Constants.MON_DELEGATE_SUCCESS, 1);
			} else {
				// 检查代理信息失败计数
				log.info(LogUtil.getRmiMessage(memberID, rmiClientHostIP, 
						"检查代理信息", new Object[]{memberID, customerID,
						phonePassword}, "失败", ret));
				
				AppMonitor.getInstance().add(Constants.MON_DELEGATE_FAIL, 1);
			}
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"检查代理信息", new Object[]{memberID, customerID,
					phonePassword}, "失败" + e, ret));
			
			throw new RuntimeException(e);
		}
		
		return ret;
	}

	/**
	 * 强行终止一个会话
	 * 
	 * @param sessionID
	 * @throws RemoteException 
	 */
	public void kickOnlineSession(String sessionID) throws RemoteException {
		String rmiClientHostIP = getRMIClientHostIP();

		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"强行终止一个会话", new Object[]{sessionID}, "中", "NA"));
			
			LogonManager logonManager = server.getLogonManager();

			String traderId = logonManager.getUserID(sessionID);
			boolean sessionOffline = true; // 此会话是否已在主备机上都离线了

			if (null == traderId) { // 本地没有此用户的在线信息
				if (Constants.SESSION_SYNC_MODE_NORMAL 
						== SyncServer.getInstance().getSyncMode()) { // 正常模式要查看对端
					// 看对端是否还有此人的在线信息
					SyncData newSyncData = SyncServer.getInstance().
							SyncGet(sessionID, traderId);
					if (null != newSyncData && null != newSyncData.getAUValue() 
							&& null != newSyncData.getPrivilege()) {
						sessionOffline = false; // 对端有此会话信息,表示之前还未离线
					}
				}
			} else {
				logonManager.logoffSession(sessionID); // 注销本机
				sessionOffline = false; // 本地有此会话信息,表示之前还未离线
			}

			if (!sessionOffline) { // 之前未离线,本次修改为离线
				Server.getUserDAO().Logout(sessionID, "System", 
						null, "交易员【" + traderId + "】的会话【" + sessionID + "】已被强制终止");
				
				log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
						"强行终止一个会话", new Object[]{sessionID}, "成功", "NA"));
			}

			// 调用备机的注销
			SyncServer.getInstance().SyncRemove(sessionID, traderId);
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"强行终止一个会话", new Object[]{sessionID}, "失败" + e, "NA"));

			throw new RuntimeException(e);
		}
	}

	/**
	 * 强行终止本地的一个会话
	 * 
	 * @param sessionID
	 * @throws RemoteException
	 */
	public void kickLocalOnlineSession(String sessionID) throws RemoteException {
		String rmiClientHostIP = getRMIClientHostIP();
		
		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"强行终止本地的一个会话", new Object[]{sessionID}, "中", "NA"));

			LogonManager logonManager = server.getLogonManager();

			String traderId = logonManager.getUserID(sessionID);

			if (null == traderId) { // 本地没有此用户的在线信息
				return;
			} else {
				logonManager.logoffSession(sessionID); // 注销本机
			}

			boolean sessionOffline = true; // 此会话是否已在主备机上都离线了
			if (Constants.SESSION_SYNC_MODE_NORMAL 
					== SyncServer.getInstance().getSyncMode()) { // 正常模式要查看对端
				// 看对端是否还有此人的在线信息
				SyncData newSyncData = SyncServer.getInstance().
						SyncGet(sessionID, traderId);
				if (null != newSyncData && null != newSyncData.getAUValue() 
						&& null != newSyncData.getPrivilege()) {
					sessionOffline = false; // 对端有此会话信息,表示现在还在线
				}
			}

			if (sessionOffline) { // 如果主备机都已离线,则修改在线状态
				Server.getUserDAO().Logout(sessionID, "System", 
						null, "交易员【" + traderId + "】的会话【" + sessionID + "】已被强制终止");
				
				log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
						"强行终止本地的一个会话", new Object[]{sessionID}, "成功", "NA"));
			}
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"强行终止本地的一个会话", new Object[]{sessionID}, "失败" + e, "NA"));

			throw new RuntimeException(e);
		}
	}

	/**
	 * 获取服务端共享的加密密钥
	 * 
	 * @return 服务端共享的加密密钥
	 * @throws RemoteException
	 */
	public byte[] getServerKey() throws RemoteException {
		byte[] key = null;
		String rmiClientHostIP = getRMIClientHostIP();
		
		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"获取服务端共享的加密密钥", new Object[]{"NA"}, "中"));
			
			LogonManager logonManager = server.getLogonManager();
			key = logonManager.getServerKey();
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"获取服务端共享的加密密钥", new Object[]{"NA"}, "成功", key));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"获取服务端共享的加密密钥", new Object[]{"NA"}, "失败" + e, key));

			throw new RuntimeException(e);
		}

		return key;
	}

	/**
	 * 存放验证码
	 * @param key
	 * @param value
	 * @throws RemoteException
	 */
	public void putValidCode(String key, String value) throws RemoteException {
		String rmiClientHostIP = getRMIClientHostIP();
		
		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"存放验证码", new Object[]{key, value}, "中", "NA"));

			// 存验证码计数
			AppMonitor.getInstance().add(Constants.MON_PUTCODE_COUNT, 1);
			server.getVaidCodeQueue().put(key, new ValidCode(value)); // 只放在本地
			
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"存放验证码", new Object[]{key, value}, "成功", "NA"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"存放验证码", new Object[]{key, value}, "失败" + e, "NA"));

			throw new RuntimeException(e);
		}
	}

	/**
	 * 获取验证码
	 * @param key
	 * @return String
	 * @throws RemoteException
	 */
	public String getValidCode(String key) throws RemoteException {
	    ValidCode validCode = null;
		String rmiClientHostIP = getRMIClientHostIP();

		try {
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"获取验证码", new Object[]{key}, "中", "NA"));
			// 取验证码计数
			AppMonitor.getInstance().add(Constants.MON_GETCODE_COUNT, 1);

			validCode = server.getVaidCodeQueue().remove(key);
			if (null == validCode) { // 本地没有取到,从远程机器上再取一次
				// 取信息(复用sessionID作为key)
				SyncData newSyncData = SyncServer.getInstance().SyncGetValidCode(key);
				if (null == newSyncData) {
					return null;
				}

				validCode = new ValidCode(newSyncData.getTraderID());
			}

			if (null == validCode.getValue()) {
				// 取验证码失败计数
				AppMonitor.getInstance().add(Constants.MON_GETCODE_FAIL, 1);

				log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
						"获取验证码", new Object[]{key}, "失败", "NA"));
			} else {
				// 取验证码成功计数
				AppMonitor.getInstance().add(Constants.MON_GETCODE_SUCCESS, 1);
				
				log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
						"获取验证码", new Object[]{key}, "成功", validCode.getValue()));
			}
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"获取验证码", new Object[]{key}, "失败" + e, validCode.getValue()));

			throw new RuntimeException(e);
		}

		return validCode.getValue();
	}

	/**
	 * 重新加载会话上下文
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
					"重新加载会话上下文", new Object[]{sessionID}, "中"));
			
			Privilege privilege = server.getPrivilegeQueue().get(sessionID);
			if (null == privilege) { // 会话不在线
				return null;
			}

			TraderInfo traderInfo = getTraderInfo(privilege.getTraderID());
			if (null != traderInfo) {
				traderInfo.retCode = 1;
				traderInfo.auSessionId = sessionID;

				privilege = getPrivilege(traderInfo);

				
				LogonManager.getActiveUserManager().putPrivilegeQueue(sessionID, privilege, "reloadSessionContext");

				// 同步重新加载的会话信息到备机
				SyncServer.getInstance().SyncUpdate(traderInfo.auSessionId, privilege);
			}

			sessionContext = new SessionContext(traderInfo, privilege);
			log.info(LogUtil.getRmiMessage(rmiClientHostIP, 
					"重新加载会话上下文", new Object[]{sessionID}, 
					"成功", sessionContext));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(rmiClientHostIP, 
					"重新加载会话上下文", new Object[]{sessionID}, 
					"失败" + e, sessionContext));

			throw new RuntimeException(e);
		}
		
		return sessionContext;
	}
}
