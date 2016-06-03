package tpme.PMES.timebargain.server.activeuser.impl;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tpme.PMES.timebargain.AppMonitor.AppMonitor;
import tpme.PMES.member.ActiveUser.LogonManager;
import tpme.PMES.timebargain.server.Constants;
import tpme.PMES.timebargain.server.Server;
import tpme.PMES.timebargain.server.SyncServer;
import tpme.PMES.timebargain.server.activeuser.ActiveUserInterface;
import tpme.PMES.timebargain.server.model.SyncData;
import tpme.PMES.timebargain.server.util.LogUtil;


public class ActiveUserInterfaceImpl implements ActiveUserInterface {
	private final Log log = LogFactory.getLog(getClass());

	private Server server;
	
	public ActiveUserInterfaceImpl(Server server) {
		this.server = server;
	}

	/**
	 * 当用户登录时回调该方法
	 * 
	 * @param sessionID
	 * @param traderID
	 */
	public void put(String sessionID, String traderID) {
		// 登录时回调,不需要再实现
		log.info(LogUtil.getRmiMessage(traderID, 
				"回调登陆交易服务器", new Object[]{sessionID, traderID}, "成功", "NA"));
	}

	/**
	 * 当用户登出、超时或者被踢下线时回调该方法
	 * 
	 * @param sessionID
	 * @param traderID
	 * @param type
	 * @param onlineNum 剩余在线实例数
	 */
	public void remove(String sessionID, String traderID, int type, int onlineNum) {
		SyncData syncData = null;

		switch (type) {
			/*正常退出*/
			case ActiveUserInterface.typeLogoff:
				server.getPrivilegeQueue().remove(sessionID);
				break;

			/*单例退出*/
			case ActiveUserInterface.typeKicked:
				server.getPrivilegeQueue().remove(sessionID);
				Date now = new Date();
				LogUtil.beginLog(this.getClass(), "UserDAO.Logout", sessionID, now);
				Server.getUserDAO().Logout(sessionID, "System", 
						null, "交易员【" + traderID + "】的会话【" + sessionID + "】被另一个会话踢下线");
				LogUtil.endLog(this.getClass(), "UserDAO.Logout", sessionID, now);
				break;

			/*超时退出*/
			case ActiveUserInterface.typeExpired:
				server.getPrivilegeQueue().remove(sessionID);

				boolean sessionOffline = true; // 此会话是否已在主备机上都离线了

				if (Constants.SESSION_SYNC_MODE_NORMAL 
						== SyncServer.getInstance().getSyncMode()) { // 正常模式要查看对端
					// 看对端是否还有此人的在线信息,如果没有则更新会话为下线状态
					SyncData newSyncData = SyncServer.getInstance().
							SyncGet(sessionID, traderID);
					if (null != newSyncData && null != newSyncData.getAUValue() && 
							null != newSyncData.getPrivilege()) {
						sessionOffline = false;
					}
				} else {
					SyncServer.getInstance().SyncRemove(sessionID, traderID); // 踢掉对端
				}
				// 超时退出计数
				AppMonitor.getInstance().add(Constants.MON_REMOVE_COUNT, 1);
				// 当前在线交易员数
				int onlineTraderNum = LogonManager.getActiveUserManager().getOnlineTraderNum();
				AppMonitor.getInstance().set(Constants.MON_TRADER_NUM, onlineTraderNum);
				// 当前在线会话数
				int onlineSessionNum = LogonManager.getActiveUserManager().
						getOnlineSessionNum();
				AppMonitor.getInstance().set(Constants.MON_SESSION_NUM, onlineSessionNum);

				if (sessionOffline) { // 会话已离线
					Server.getUserDAO().Logout(sessionID, "System", 
							null, "交易员【" + traderID + "】的会话【" + sessionID + "】超时退出");
				}
				break;

			default:
				break;
		}
		
		log.info(LogUtil.getRmiMessage(traderID, 
				"回调退出交易服务器", new Object[]{sessionID, traderID, type, onlineNum},
				"成功", "NA"));
	}
}
