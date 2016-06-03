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
	 * ���û���¼ʱ�ص��÷���
	 * 
	 * @param sessionID
	 * @param traderID
	 */
	public void put(String sessionID, String traderID) {
		// ��¼ʱ�ص�,����Ҫ��ʵ��
		log.info(LogUtil.getRmiMessage(traderID, 
				"�ص���½���׷�����", new Object[]{sessionID, traderID}, "�ɹ�", "NA"));
	}

	/**
	 * ���û��ǳ�����ʱ���߱�������ʱ�ص��÷���
	 * 
	 * @param sessionID
	 * @param traderID
	 * @param type
	 * @param onlineNum ʣ������ʵ����
	 */
	public void remove(String sessionID, String traderID, int type, int onlineNum) {
		SyncData syncData = null;

		switch (type) {
			/*�����˳�*/
			case ActiveUserInterface.typeLogoff:
				server.getPrivilegeQueue().remove(sessionID);
				break;

			/*�����˳�*/
			case ActiveUserInterface.typeKicked:
				server.getPrivilegeQueue().remove(sessionID);
				Date now = new Date();
				LogUtil.beginLog(this.getClass(), "UserDAO.Logout", sessionID, now);
				Server.getUserDAO().Logout(sessionID, "System", 
						null, "����Ա��" + traderID + "���ĻỰ��" + sessionID + "������һ���Ự������");
				LogUtil.endLog(this.getClass(), "UserDAO.Logout", sessionID, now);
				break;

			/*��ʱ�˳�*/
			case ActiveUserInterface.typeExpired:
				server.getPrivilegeQueue().remove(sessionID);

				boolean sessionOffline = true; // �˻Ự�Ƿ������������϶�������

				if (Constants.SESSION_SYNC_MODE_NORMAL 
						== SyncServer.getInstance().getSyncMode()) { // ����ģʽҪ�鿴�Զ�
					// ���Զ��Ƿ��д��˵�������Ϣ,���û������»ỰΪ����״̬
					SyncData newSyncData = SyncServer.getInstance().
							SyncGet(sessionID, traderID);
					if (null != newSyncData && null != newSyncData.getAUValue() && 
							null != newSyncData.getPrivilege()) {
						sessionOffline = false;
					}
				} else {
					SyncServer.getInstance().SyncRemove(sessionID, traderID); // �ߵ��Զ�
				}
				// ��ʱ�˳�����
				AppMonitor.getInstance().add(Constants.MON_REMOVE_COUNT, 1);
				// ��ǰ���߽���Ա��
				int onlineTraderNum = LogonManager.getActiveUserManager().getOnlineTraderNum();
				AppMonitor.getInstance().set(Constants.MON_TRADER_NUM, onlineTraderNum);
				// ��ǰ���߻Ự��
				int onlineSessionNum = LogonManager.getActiveUserManager().
						getOnlineSessionNum();
				AppMonitor.getInstance().set(Constants.MON_SESSION_NUM, onlineSessionNum);

				if (sessionOffline) { // �Ự������
					Server.getUserDAO().Logout(sessionID, "System", 
							null, "����Ա��" + traderID + "���ĻỰ��" + sessionID + "����ʱ�˳�");
				}
				break;

			default:
				break;
		}
		
		log.info(LogUtil.getRmiMessage(traderID, 
				"�ص��˳����׷�����", new Object[]{sessionID, traderID, type, onlineNum},
				"�ɹ�", "NA"));
	}
}
