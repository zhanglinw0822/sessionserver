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

	// 同步所使用的对方的url
	private String syncRmiUrl = null;

	// Session同步模式
	private int syncMode = Constants.SESSION_SYNC_MODE_SYNC; // 默认使用同步模式

	// 异步模式下所使用的同步队列
	private BlockingDeque<SyncData> syncQueue = new LinkedBlockingDeque<SyncData>();

	private SyncRMI slaveSyncRMI = null; // 得到备机同步的RMI(给对方调用)

	int syncRetryNum = 10; // 同步重试次数


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
					"查找对端同步的RMI", "NA", "失败" + e, "NA"));
		}

		// 启用Session同步线程
		SessionSyncThread sessionSyncThread = new SessionSyncThread();
		sessionSyncThread.start();

		return;
	}

	public int getSyncMode() {
		return this.syncMode;
	}

	/**
	 * 同步数据
	 * @param syncData
	 */
	public SyncData Sync(SyncData syncData) {
		SyncData newSyncData = null;
		
		switch (syncMode) {
		case Constants.SESSION_SYNC_MODE_SYNC: // 同步模式下直接调用对端的RMI
		case Constants.SESSION_SYNC_MODE_NORMAL: // 正常模式与同步模式一致

			if (null != slaveSyncRMI) { // 在同步RMI不为空的情况下,直接调用对端的RMI同步
				try {
					Date now =new Date();
					LogUtil.beginLog(this.getClass(), "SyncRMI.Sync", syncData, now);
					newSyncData = slaveSyncRMI.Sync(syncData);
					LogUtil.endLog(this.getClass(), "SyncRMI.Sync", syncData, now);
					break;
				} catch (ConnectException e) {
					slaveSyncRMI = null;
					log.error(LogUtil.getMessage("system", 
							"同步数据", syncData.getAUValue(), "失败" + e, newSyncData));

				} catch (RemoteException e) {
					slaveSyncRMI = null;
					log.error(LogUtil.getMessage("system", 
							"同步数据", syncData.getAUValue(), "失败" + e, newSyncData));

				} catch (Exception e) {
					slaveSyncRMI = null;
					log.error(LogUtil.getMessage("system", 
							"同步数据", syncData.getAUValue(), "失败" + e, newSyncData));
				}
			} else {
				if (Constants.SESSION_SYNC_OPR_GET != syncData.getOperatorCode()) { // 非实时操作允许后续同步
					try {
						syncQueue.put(syncData);
						// 同步队列大小
						AppMonitor.getInstance().set(Constants.MON_SYNCQUEUE_SIZE, syncQueue.size());
					} catch (InterruptedException e) {
					}
				}
				break;
			}
		case Constants.SESSION_SYNC_MODE_ASYNC: // 异步模式则将请求放入队列
			try {
				syncQueue.put(syncData);
				// 同步队列大小
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
	 * 同步增加一个会话
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
	 * 同步重新加载一个会话
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
	 * 同步移除一个会话
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
	 * 同步活动一个会话
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
	 * 同步获取一个会话
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
	 * 同步获取一个验证码
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
				//session同步线程运行计数
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
				if (null == slaveSyncRMI) { // 同步的RMI失败了(可能是对方已关闭)
					try {
						slaveSyncRMI = (SyncRMI) Naming.lookup(syncRmiUrl);
						if (null == slaveSyncRMI) {
							Thread.sleep(100);
							return;
						}
					} catch (Exception e) {
						Thread.sleep(100); // RMI没有连接成功睡眠一段时间之后继续lookup
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
				newSyncData = slaveSyncRMI.Sync(syncData); // get不会使用异步
			} catch (ConnectException e) {
				slaveSyncRMI = null;
				if (syncData.getRetryNum() < syncRetryNum) {
					try {
						syncQueue.putFirst(syncData);
					} catch (InterruptedException e1) {
						log.error(LogUtil.getSysMessage("同步队列中断", "异常" + e));
					}
				}
			} catch (RemoteException e) {
				slaveSyncRMI = null;
				if (syncData.getRetryNum() < syncRetryNum) {
					try {
						syncQueue.putFirst(syncData);
					} catch (InterruptedException e1) {
						log.error(LogUtil.getSysMessage("同步队列中断", "异常" + e));
					}
				}
			} catch (Exception e) {
				slaveSyncRMI = null;
				if (syncData.getRetryNum() < syncRetryNum) {
					try {
						syncQueue.putFirst(syncData);
					} catch (InterruptedException e1) {
						log.error(LogUtil.getSysMessage("同步队列中断", "异常" + e));
					}
				}
			}
			// 同步队列大小
			AppMonitor.getInstance().set(Constants.MON_SYNCQUEUE_SIZE, syncQueue.size());
		}
		
		public void close() {
			runFlag = false;
			this.interrupt();
		}
	}
}
