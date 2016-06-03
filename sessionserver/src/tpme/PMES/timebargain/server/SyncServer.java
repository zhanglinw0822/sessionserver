package tpme.PMES.timebargain.server;

import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tpme.PMES.timebargain.AppMonitor.AppMonitor;
import tpme.PMES.timebargain.server.model.AUValue;
import tpme.PMES.timebargain.server.model.Privilege;
import tpme.PMES.timebargain.server.model.SyncData;
import tpme.PMES.timebargain.server.rmi.SyncRMI;
import tpme.PMES.timebargain.server.util.LogUtil;

public class SyncServer {
	private Log log = LogFactory.getLog(getClass());

	private static SyncServer syncServer = null;

	private volatile boolean runFlag = true;

	// ͬ����ʹ�õĶԷ���url
	private String syncRmiUrl = null;

	// Sessionͬ��ģʽ
	private int syncMode = Constants.SESSION_SYNC_MODE_SYNC; // Ĭ��ʹ��ͬ��ģʽ

	// �첽ģʽ����ʹ�õ�ͬ������
	private BlockingDeque<SyncData> syncQueue = new LinkedBlockingDeque<SyncData>();

	private SyncRMI slaveSyncRMI = null; // �õ�����ͬ����RMI(���Է�����)

	int syncRetryNum = 10; // ͬ�����Դ���


	private SyncServer() {
	}
	
	public static SyncServer getInstance() {
		if (null == syncServer) {
			synchronized (SyncServer.class) {
				if (null == syncServer) {
					syncServer = new SyncServer();
				}
			}
		}

		return syncServer;
	}

	public BlockingDeque<SyncData> getSyncQueue() {
		return syncQueue;
	}

	public SyncRMI getSyncRMI() {
		return this.slaveSyncRMI;
	}
	
	public void init(String syncRmiUrl, int syncMode, int syncRetryNum) {
		this.syncRmiUrl = syncRmiUrl;
		this.syncMode = syncMode;
		this.syncRetryNum = syncRetryNum;

		try {
			slaveSyncRMI = (SyncRMI) Naming.lookup(syncRmiUrl);
		} catch (Exception e) {
			log.error(LogUtil.getMessage("system", 
					"���ҶԶ�ͬ����RMI", "NA", "ʧ��" + e, "NA"));
		}

		// ����Sessionͬ���߳�
		SessionSyncThread sessionSyncThread = new SessionSyncThread();
		sessionSyncThread.start();

		return;
	}

	public int getSyncMode() {
		return this.syncMode;
	}

	/**
	 * ͬ������
	 * @param syncData
	 */
	public SyncData Sync(SyncData syncData) {
		SyncData newSyncData = null;
		
		switch (syncMode) {
		case Constants.SESSION_SYNC_MODE_SYNC: // ͬ��ģʽ��ֱ�ӵ��öԶ˵�RMI
		case Constants.SESSION_SYNC_MODE_NORMAL: // ����ģʽ��ͬ��ģʽһ��

			if (null != slaveSyncRMI) { // ��ͬ��RMI��Ϊ�յ������,ֱ�ӵ��öԶ˵�RMIͬ��
				try {
					Date now =new Date();
					LogUtil.beginLog(this.getClass(), "SyncRMI.Sync", syncData, now);
					newSyncData = slaveSyncRMI.Sync(syncData);
					LogUtil.endLog(this.getClass(), "SyncRMI.Sync", syncData, now);
					break;
				} catch (ConnectException e) {
					slaveSyncRMI = null;
					log.error(LogUtil.getMessage("system", 
							"ͬ������", syncData.getAUValue(), "ʧ��" + e, newSyncData));

				} catch (RemoteException e) {
					slaveSyncRMI = null;
					log.error(LogUtil.getMessage("system", 
							"ͬ������", syncData.getAUValue(), "ʧ��" + e, newSyncData));

				} catch (Exception e) {
					slaveSyncRMI = null;
					log.error(LogUtil.getMessage("system", 
							"ͬ������", syncData.getAUValue(), "ʧ��" + e, newSyncData));
				}
			} else {
				if (Constants.SESSION_SYNC_OPR_GET != syncData.getOperatorCode()) { // ��ʵʱ�����������ͬ��
					try {
						syncQueue.put(syncData);
						// ͬ�����д�С
						AppMonitor.getInstance().set(Constants.MON_SYNCQUEUE_SIZE, syncQueue.size());
					} catch (InterruptedException e) {
					}
				}
				break;
			}
		case Constants.SESSION_SYNC_MODE_ASYNC: // �첽ģʽ������������
			try {
				syncQueue.put(syncData);
				// ͬ�����д�С
				AppMonitor.getInstance().set(Constants.MON_SYNCQUEUE_SIZE, syncQueue.size());
			} catch (InterruptedException e) {
			}
			break;
		default:
			break;
		}

		return newSyncData;
	}

	/**
	 * ͬ������һ���Ự
	 * @param sessionID
	 * @param aUVale
	 * @param privilege
	 */
	public void SyncAdd(String sessionID, AUValue aUVale, Privilege privilege) {
		SyncData syncData = new SyncData();
		syncData.setOperatorCode(Constants.SESSION_SYNC_OPR_ADD);
		syncData.setSessionID(sessionID);
		syncData.setAUValue(aUVale);
		syncData.setPrivilege(privilege);
		syncData.setTraderID(privilege.getTraderID());

		Sync(syncData);
	}
	
	/**
	 * ͬ�����¼���һ���Ự
	 * @param sessionID
	 * @param privilege
	 */
	public void SyncUpdate(String sessionID, Privilege privilege) {
		SyncData syncData = new SyncData();
		syncData.setOperatorCode(Constants.SESSION_SYNC_OPR_UPDATE);
		syncData.setSessionID(sessionID);
		syncData.setPrivilege(privilege);
		syncData.setTraderID(privilege.getTraderID());

		Sync(syncData);
	}

	/**
	 * ͬ���Ƴ�һ���Ự
	 * @param sessionID
	 * @param traderID
	 */
	public void SyncRemove(String sessionID, String traderID) {
		SyncData syncData = new SyncData();
		syncData.setOperatorCode(Constants.SESSION_SYNC_OPR_REMOVE);
		syncData.setSessionID(sessionID);
		syncData.setTraderID(traderID);

		Sync(syncData);
	}

	/**
	 * ͬ���һ���Ự
	 * @param traderID
	 * @param sessionID
	 */
	public void SyncActive(String sessionID, String traderID) {
		SyncData syncData = new SyncData();
		syncData.setOperatorCode(Constants.SESSION_SYNC_OPR_ACTIVE);
		syncData.setSessionID(sessionID);
		syncData.setTraderID(traderID);

		Sync(syncData);
	}

	/**
	 * ͬ����ȡһ���Ự
	 * @param sessionID
	 * @param traderID
	 * @return
	 */
	public SyncData SyncGet(String sessionID, String traderID) {
		SyncData syncData = new SyncData();
		syncData.setOperatorCode(Constants.SESSION_SYNC_OPR_GET);
		syncData.setSessionID(sessionID);
		syncData.setTraderID(traderID);

		return Sync(syncData);
	}

	/**
	 * ͬ����ȡһ����֤��
	 * @param sessionID
	 * @return
	 */
	public SyncData SyncGetValidCode(String sessionID) {
		SyncData syncData = new SyncData();
		syncData.setOperatorCode(Constants.SESSION_SYNC_OPR_VALID_CODE_GET);
		syncData.setSessionID(sessionID);

		return Sync(syncData);
	}

	class SessionSyncThread extends Thread {
		public void run() {
			while (runFlag) {
				//sessionͬ���߳����м���
				try {
					AppMonitor.getInstance().add(Constants.MON_SYNCSESSION_COUNT, 1);
				} catch (Exception e) {
				}

				invoke();
			}
		}

		public void invoke() {
			SyncData syncData = null;
			SyncData newSyncData = null;

			try {
				if (null == slaveSyncRMI) { // ͬ����RMIʧ����(�����ǶԷ��ѹر�)
					try {
						slaveSyncRMI = (SyncRMI) Naming.lookup(syncRmiUrl);
						if (null == slaveSyncRMI) {
							Thread.sleep(100);
							return;
						}
					} catch (Exception e) {
						Thread.sleep(100); // RMIû�����ӳɹ�˯��һ��ʱ��֮�����lookup
						return;
					}
				}

				syncData = syncQueue.poll(1000, TimeUnit.MILLISECONDS);
				
				if (null == syncData) {
					return;
				}
			} catch (InterruptedException e) {
				return;
			}

			try {
				syncData.increaseRetryNum();
				newSyncData = slaveSyncRMI.Sync(syncData); // get����ʹ���첽
			} catch (ConnectException e) {
				slaveSyncRMI = null;
				if (syncData.getRetryNum() < syncRetryNum) {
					try {
						syncQueue.putFirst(syncData);
					} catch (InterruptedException e1) {
						log.error(LogUtil.getSysMessage("ͬ�������ж�", "�쳣" + e));
					}
				}
			} catch (RemoteException e) {
				slaveSyncRMI = null;
				if (syncData.getRetryNum() < syncRetryNum) {
					try {
						syncQueue.putFirst(syncData);
					} catch (InterruptedException e1) {
						log.error(LogUtil.getSysMessage("ͬ�������ж�", "�쳣" + e));
					}
				}
			} catch (Exception e) {
				slaveSyncRMI = null;
				if (syncData.getRetryNum() < syncRetryNum) {
					try {
						syncQueue.putFirst(syncData);
					} catch (InterruptedException e1) {
						log.error(LogUtil.getSysMessage("ͬ�������ж�", "�쳣" + e));
					}
				}
			}
			// ͬ�����д�С
			AppMonitor.getInstance().set(Constants.MON_SYNCQUEUE_SIZE, syncQueue.size());
		}
		
		public void close() {
			runFlag = false;
			this.interrupt();
		}
	}
}
