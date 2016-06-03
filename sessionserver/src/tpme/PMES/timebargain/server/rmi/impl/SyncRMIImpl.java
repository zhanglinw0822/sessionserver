package tpme.PMES.timebargain.server.rmi.impl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tpme.PMES.timebargain.AppMonitor.AppMonitor;
import tpme.PMES.member.ActiveUser.ActiveUserManager;
import tpme.PMES.member.ActiveUser.LogonManager;
import tpme.PMES.timebargain.server.Constants;
import tpme.PMES.timebargain.server.Server;
import tpme.PMES.timebargain.server.SyncServer;
import tpme.PMES.timebargain.server.model.AUValue;
import tpme.PMES.timebargain.server.model.Privilege;
import tpme.PMES.timebargain.server.model.SyncData;
import tpme.PMES.timebargain.server.model.ValidCode;
import tpme.PMES.timebargain.server.rmi.SyncRMI;
import tpme.PMES.timebargain.server.util.LogUtil;

public class SyncRMIImpl extends UnicastRemoteObject implements SyncRMI {
	private static final long serialVersionUID = 2690197650654049817L;

	private final Log log = LogFactory.getLog(getClass());

	public SyncRMIImpl() throws RemoteException {
		super();
	}

	/**
	 * �򱾵ص�AU����һ��Session
	 * @param sessionID
	 * @param aUValue
	 * @param privilege
	 * @throws RemoteException
	 */
	private void putSession(String sessionID, AUValue aUValue, Privilege privilege) 
			throws RemoteException {
		Date now1 = new Date();
		LogUtil.beginLog(this.getClass(), "SyncRMIImpl.putSession", sessionID, now1);
		if(aUValue == null || privilege == null){
			log.info(LogUtil.getRmiMessage("system", "IP", 
					"ͬ����½���׷�����", new Object[]{sessionID}, 
					"ʧ��,aUValue or privilegeΪ��", "NA"));
			LogUtil.endLog(this.getClass(), "SyncRMIImpl.putSession", sessionID, now1);
			return;
		}
		try {
			log.info(LogUtil.getRmiMessage(aUValue.getUserName(), aUValue.getIP(), 
					"ͬ����½���׷�����", new Object[]{sessionID, aUValue, privilege}, 
					"��", "NA"));
			// ��¼�Ự��Ϣͬ������
			AppMonitor.getInstance().add(Constants.MON_PUTSESSION_COUNT, 1);
			
			aUValue.setLastTime(System.currentTimeMillis());
			Date now =new Date();
			//�ȷ�session�ٷ�privilege
			LogonManager.getActiveUserManager().put(sessionID, aUValue);
			LogUtil.beginLog(this.getClass(), "PrivilegeQueue.put", sessionID, now);
			LogonManager.getActiveUserManager().putPrivilegeQueue(sessionID, privilege, "putSession");
			LogUtil.endLog(this.getClass(), "PrivilegeQueue.put", sessionID, now);
			
			log.info(LogUtil.getRmiMessage(aUValue.getUserName(), aUValue.getIP(), 
					"ͬ����½���׷�����", new Object[]{sessionID, aUValue, privilege}, 
					"�ɹ�", "NA"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(aUValue.getUserName(), aUValue.getIP(), 
					"ͬ����½���׷�����", new Object[]{sessionID, aUValue, privilege}, 
					"ʧ��" + e, "NA"));
			
			throw new RuntimeException(e);
		}finally{
			LogUtil.endLog(this.getClass(), "SyncRMIImpl.putSession", sessionID, now1);
		}
	}
	
	/**
	 * �򱾵ص�AU����һ��Privilege
	 * @param sessionID
	 * @param privilege
	 * @throws RemoteException
	 */
	private void updateSession(String sessionID, Privilege privilege) 
			throws RemoteException {
		if(privilege == null){
			log.info(LogUtil.getRmiMessage("system", "IP", 
					"ͬ�����¼��ػỰ", new Object[]{sessionID}, 
					"ʧ��,privilegeΪ��", "NA"));
			return;
		}
		try {
			log.info(LogUtil.getRmiMessage(privilege.getTraderID(), privilege.getLogonIP(), 
					"ͬ�����¼��ػỰ", new Object[]{sessionID, privilege}, 
					"��", "NA"));
			//  ͬ�����¼��ػỰ����
			AppMonitor.getInstance().add(Constants.MON_SYNCUPDATE_COUNT, 1);
			
			LogonManager.getActiveUserManager().putPrivilegeQueue(sessionID, privilege, "updateSession");
			log.info(LogUtil.getRmiMessage(privilege.getTraderID(), privilege.getLogonIP(), 
					"ͬ�����¼��ػỰ", new Object[]{sessionID, privilege}, 
					"�ɹ�", "NA"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(privilege.getTraderID(), privilege.getLogonIP(), 
					"ͬ�����¼��ػỰ", new Object[]{sessionID, privilege}, 
					"ʧ��" + e, "NA"));
			
			throw new RuntimeException(e);
		}
	}

	/**
	 * �ӱ��ص�AU���Ƴ�һ��Session
	 * @param sessionID
	 * @throws RemoteException
	 */
	private void removeSession(String sessionID) throws RemoteException {
		try {
			log.info(LogUtil.getRmiMessage("ͬ������Աע��", 
					new Object[]{sessionID}, "��", "NA"));
			// ע���Ự��Ϣͬ������
			AppMonitor.getInstance().add(Constants.MON_REMOVESESSION_COUNT, 1);
			
			Server.getPrivilegeQueue().remove(sessionID);
			LogonManager.getActiveUserManager().remove(sessionID);
			
			log.info(LogUtil.getRmiMessage("ͬ������Աע��", 
					new Object[]{sessionID}, "�ɹ�", "NA"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage("ͬ������Աע��", 
					new Object[]{sessionID}, "ʧ��" + e, "NA"));

			throw new RuntimeException(e);
		}
	}

	/**
	 * ����sessionIDȡ��ͬ�����������
	 * @param sessionID
	 * @return SyncData
	 * @throws RemoteException
	 */
	private SyncData getSession(String sessionID) throws RemoteException {
		SyncData syncData = null;
		
		try {
			log.info(LogUtil.getRmiMessage("��ѯ����Ա��Ϣ", 
					new Object[]{sessionID}, "��", "NA"));
			// ȡ�Ự��Ϣͬ������
			AppMonitor.getInstance().add(Constants.MON_GETSESSION_COUNT, 1);
			
			syncData = new SyncData();

			syncData.setSessionID(sessionID);
			syncData.setPrivilege(Server.getPrivilegeQueue().get(sessionID));
			AUValue aUValue = LogonManager.getActiveUserManager().getAUValue(sessionID);
			if (null != aUValue) {
				aUValue.setLastTime(System.currentTimeMillis());
			}
			syncData.setAUValue(aUValue);

			log.info(LogUtil.getRmiMessage("��ѯ����Ա��Ϣ", 
					new Object[]{sessionID}, "�ɹ�", syncData));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage("��ѯ����Ա��Ϣ", 
					new Object[]{sessionID}, "ʧ��" + e, syncData));
			
			throw new RuntimeException(e);
		}
		
		return syncData;
	}

	/**
	 * �����AU��ָ����Session
	 * @param sessionID
	 * @throws RemoteException
	 */
	private void activeSession(String sessionID) throws RemoteException {
		try {
			log.info(LogUtil.getRmiMessage("����Ա�������ʱ��", 
					new Object[]{sessionID}, "��", "NA"));

			// ��Ự��Ϣͬ������
			AppMonitor.getInstance().add(Constants.MON_ACTIVESESSION_COUNT, 1);

			LogonManager.getActiveUserManager().ActiveSession(sessionID);
			
			log.info(LogUtil.getRmiMessage("����Ա�������ʱ��", 
					new Object[]{sessionID}, 
					"�ɹ�", "NA"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage("����Ա�������ʱ��", 
					new Object[]{sessionID}, 
					"ʧ��" + e, "NA"));

			throw new RuntimeException(e);
		}
	}

	/**
	 * ȡ��֤�벢ɾ����¼
	 * @param key
	 * @return SyncData
	 */
	private SyncData getValidCode(String key) {
		SyncData syncData = null;

		try {
			log.info(LogUtil.getRmiMessage("ȡ��֤�벢ɾ��", new Object[]{key}, "��"));
			// �Զ�ȡ��֤�����
			AppMonitor.getInstance().add(Constants.MON_GETOTHERCODE_COUNT, 1);
			log.info(LogUtil.getRmiMessage("ȡ��֤��", new Object[]{key}, "��", syncData));

			ValidCode validCode = Server.getVaidCodeQueue().remove(key);
			if (null == validCode) {
				return null;
			}

			syncData = new SyncData();

			syncData.setSessionID(key);
			syncData.setTraderID(validCode.getValue());
			log.info(LogUtil.getRmiMessage("ȡ��֤�벢ɾ��", new Object[]{key}, "�ɹ�"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage("ȡ��֤�벢ɾ��", 
					new Object[]{key}, "ʧ��" + e, syncData));

			throw new RuntimeException(e);
		}
		
		return syncData;
	}
	
	/**
	 * ͬ������
	 * @param syncData
	 * @return ����������(�����ѯ�Ļ᷵���µ�����)
	 * @throws RemoteException
	 */
	public SyncData Sync(SyncData syncData) throws RemoteException {
		SyncData newSyncData = null;

		switch (syncData.getOperatorCode()) {
		case Constants.SESSION_SYNC_OPR_ADD:
			putSession(syncData.getSessionID(), syncData.getAUValue(), 
					syncData.getPrivilege());
			break;
		case Constants.SESSION_SYNC_OPR_REMOVE:
			removeSession(syncData.getSessionID());
			break;
		case Constants.SESSION_SYNC_OPR_UPDATE:
			updateSession(syncData.getSessionID(), syncData.getPrivilege());
			break;
		case Constants.SESSION_SYNC_OPR_GET:
			newSyncData = getSession(syncData.getSessionID());
			break;
		case Constants.SESSION_SYNC_OPR_ACTIVE:
			activeSession(syncData.getSessionID());
			break;
		case Constants.SESSION_SYNC_OPR_VALID_CODE_GET:
			newSyncData = getValidCode(syncData.getSessionID()); // ���õ�sessionID����key
			break;
		default:
				break;
		}

		return newSyncData;
	}
	
	/**
	 * ��ȡ�������������Ϣ
	 * @return List
	 * @throws RemoteException
	 */
	public List<Map<String, Object>> getAllSyncDatas() throws RemoteException {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();	

		try {
			log.info(LogUtil.getRmiMessage("��ȡ�������������Ϣ", 
					new Object[]{"NA"}, "��", "NA"));
			
			SimpleDateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
			Iterator<SyncData> syncQueues = SyncServer.getInstance().
					 getSyncQueue().iterator();
			while(syncQueues.hasNext()){
				Map<String, Object> map = new HashMap<String, Object>();
				SyncData syncData = syncQueues.next();
				map.put("index", syncData.getIndex());
				map.put("oprCode", syncData.getOperatorCode());
				map.put("generateTime", formatter.format(syncData.getGenerateTime()));
				map.put("sessionID", syncData.getSessionID());
				map.put("traderID", syncData.getTraderID());
				map.put("retryNum", syncData.getRetryNum());
				
				list.add(map);
			}
			log.info(LogUtil.getRmiMessage("��ȡ�������������Ϣ", 
					new Object[]{"NA"}, "�ɹ�", list));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage("��ȡ�������������Ϣ", 
					new Object[]{"NA"}, "ʧ��" + e, list));

			throw new RuntimeException(e);
		}
		
		return list;
	}
	
	/**
	 * ɾ��������ѡ�еļ�¼
	 * @param indexs
	 * @return int
	 * @throws RemoteException
	 */
	public int remove(int[] indexs) throws RemoteException {
		int count = 0;

		try {
			log.info(LogUtil.getRmiMessage("ɾ��������ѡ�еļ�¼", 
					new Object[]{indexs}, "��", "NA"));
			
			BlockingDeque<SyncData> syncQueues = SyncServer.getInstance().getSyncQueue();
			SyncData syncData = new SyncData(0);
			for (int index : indexs) {
				syncData.setIndex(index);
				if (syncQueues.remove(syncData)) {
					count++;
				}
			}
			log.info(LogUtil.getRmiMessage("ɾ��������ѡ�еļ�¼", 
					new Object[]{indexs}, "�ɹ�", count));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage("ɾ��������ѡ�еļ�¼", 
					new Object[]{indexs}, "ʧ��" + e, count));

			throw new RuntimeException(e);
		}
		
		return count;
	}

	/**
	 * ��ն���
	 * @throws RemoteException
	 */
	public void clear() throws RemoteException {

		try {
			log.info(LogUtil.getRmiMessage("��ն���", new Object[]{"NA"}, "��", "NA"));
			
			SyncServer.getInstance().getSyncQueue().clear();
			log.info(LogUtil.getRmiMessage("��ն���", new Object[]{"NA"}, "�ɹ�", "NA"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage("��ն���", 
					new Object[]{"NA"}, "ʧ��" + e, "NA"));

			throw new RuntimeException(e);
		}
	}

	/**
	 * ��ȡͬ����������������
	 * @return int
	 * @throws RemoteException
	 */
	public int getSyncDataNum() throws RemoteException {
		int num = 0;
		
		try {
			log.info(LogUtil.getRmiMessage("��ն���", new Object[]{"NA"}, "��", "NA"));
			
			num = SyncServer.getInstance().getSyncQueue().size();
			
			log.info(LogUtil.getRmiMessage("��ȡͬ����������������", 
					new Object[]{"NA"}, "�ɹ�", num));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage("��ȡͬ����������������", 
					new Object[]{"NA"}, "ʧ��" + e, num));
			
			throw new RuntimeException(e);
		}
		
		return num;
	}
}
