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
	 * 向本地的AU放入一个Session
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
					"同步登陆交易服务器", new Object[]{sessionID}, 
					"失败,aUValue or privilege为空", "NA"));
			LogUtil.endLog(this.getClass(), "SyncRMIImpl.putSession", sessionID, now1);
			return;
		}
		try {
			log.info(LogUtil.getRmiMessage(aUValue.getUserName(), aUValue.getIP(), 
					"同步登陆交易服务器", new Object[]{sessionID, aUValue, privilege}, 
					"中", "NA"));
			// 登录会话信息同步计数
			AppMonitor.getInstance().add(Constants.MON_PUTSESSION_COUNT, 1);
			
			aUValue.setLastTime(System.currentTimeMillis());
			Date now =new Date();
			//先放session再放privilege
			LogonManager.getActiveUserManager().put(sessionID, aUValue);
			LogUtil.beginLog(this.getClass(), "PrivilegeQueue.put", sessionID, now);
			LogonManager.getActiveUserManager().putPrivilegeQueue(sessionID, privilege, "putSession");
			LogUtil.endLog(this.getClass(), "PrivilegeQueue.put", sessionID, now);
			
			log.info(LogUtil.getRmiMessage(aUValue.getUserName(), aUValue.getIP(), 
					"同步登陆交易服务器", new Object[]{sessionID, aUValue, privilege}, 
					"成功", "NA"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(aUValue.getUserName(), aUValue.getIP(), 
					"同步登陆交易服务器", new Object[]{sessionID, aUValue, privilege}, 
					"失败" + e, "NA"));
			
			throw new RuntimeException(e);
		}finally{
			LogUtil.endLog(this.getClass(), "SyncRMIImpl.putSession", sessionID, now1);
		}
	}
	
	/**
	 * 向本地的AU放入一个Privilege
	 * @param sessionID
	 * @param privilege
	 * @throws RemoteException
	 */
	private void updateSession(String sessionID, Privilege privilege) 
			throws RemoteException {
		if(privilege == null){
			log.info(LogUtil.getRmiMessage("system", "IP", 
					"同步重新加载会话", new Object[]{sessionID}, 
					"失败,privilege为空", "NA"));
			return;
		}
		try {
			log.info(LogUtil.getRmiMessage(privilege.getTraderID(), privilege.getLogonIP(), 
					"同步重新加载会话", new Object[]{sessionID, privilege}, 
					"中", "NA"));
			//  同步重新加载会话计数
			AppMonitor.getInstance().add(Constants.MON_SYNCUPDATE_COUNT, 1);
			
			LogonManager.getActiveUserManager().putPrivilegeQueue(sessionID, privilege, "updateSession");
			log.info(LogUtil.getRmiMessage(privilege.getTraderID(), privilege.getLogonIP(), 
					"同步重新加载会话", new Object[]{sessionID, privilege}, 
					"成功", "NA"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage(privilege.getTraderID(), privilege.getLogonIP(), 
					"同步重新加载会话", new Object[]{sessionID, privilege}, 
					"失败" + e, "NA"));
			
			throw new RuntimeException(e);
		}
	}

	/**
	 * 从本地的AU中移除一个Session
	 * @param sessionID
	 * @throws RemoteException
	 */
	private void removeSession(String sessionID) throws RemoteException {
		try {
			log.info(LogUtil.getRmiMessage("同步交易员注销", 
					new Object[]{sessionID}, "中", "NA"));
			// 注销会话信息同步计数
			AppMonitor.getInstance().add(Constants.MON_REMOVESESSION_COUNT, 1);
			
			Server.getPrivilegeQueue().remove(sessionID);
			LogonManager.getActiveUserManager().remove(sessionID);
			
			log.info(LogUtil.getRmiMessage("同步交易员注销", 
					new Object[]{sessionID}, "成功", "NA"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage("同步交易员注销", 
					new Object[]{sessionID}, "失败" + e, "NA"));

			throw new RuntimeException(e);
		}
	}

	/**
	 * 根据sessionID取出同步所需的数据
	 * @param sessionID
	 * @return SyncData
	 * @throws RemoteException
	 */
	private SyncData getSession(String sessionID) throws RemoteException {
		SyncData syncData = null;
		
		try {
			log.info(LogUtil.getRmiMessage("查询交易员信息", 
					new Object[]{sessionID}, "中", "NA"));
			// 取会话信息同步计数
			AppMonitor.getInstance().add(Constants.MON_GETSESSION_COUNT, 1);
			
			syncData = new SyncData();

			syncData.setSessionID(sessionID);
			syncData.setPrivilege(Server.getPrivilegeQueue().get(sessionID));
			AUValue aUValue = LogonManager.getActiveUserManager().getAUValue(sessionID);
			if (null != aUValue) {
				aUValue.setLastTime(System.currentTimeMillis());
			}
			syncData.setAUValue(aUValue);

			log.info(LogUtil.getRmiMessage("查询交易员信息", 
					new Object[]{sessionID}, "成功", syncData));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage("查询交易员信息", 
					new Object[]{sessionID}, "失败" + e, syncData));
			
			throw new RuntimeException(e);
		}
		
		return syncData;
	}

	/**
	 * 活动本地AU中指定的Session
	 * @param sessionID
	 * @throws RemoteException
	 */
	private void activeSession(String sessionID) throws RemoteException {
		try {
			log.info(LogUtil.getRmiMessage("交易员更新最后活动时间", 
					new Object[]{sessionID}, "中", "NA"));

			// 活动会话信息同步计数
			AppMonitor.getInstance().add(Constants.MON_ACTIVESESSION_COUNT, 1);

			LogonManager.getActiveUserManager().ActiveSession(sessionID);
			
			log.info(LogUtil.getRmiMessage("交易员更新最后活动时间", 
					new Object[]{sessionID}, 
					"成功", "NA"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage("交易员更新最后活动时间", 
					new Object[]{sessionID}, 
					"失败" + e, "NA"));

			throw new RuntimeException(e);
		}
	}

	/**
	 * 取验证码并删除记录
	 * @param key
	 * @return SyncData
	 */
	private SyncData getValidCode(String key) {
		SyncData syncData = null;

		try {
			log.info(LogUtil.getRmiMessage("取验证码并删除", new Object[]{key}, "中"));
			// 对端取验证码计数
			AppMonitor.getInstance().add(Constants.MON_GETOTHERCODE_COUNT, 1);
			log.info(LogUtil.getRmiMessage("取验证码", new Object[]{key}, "中", syncData));

			ValidCode validCode = Server.getVaidCodeQueue().remove(key);
			if (null == validCode) {
				return null;
			}

			syncData = new SyncData();

			syncData.setSessionID(key);
			syncData.setTraderID(validCode.getValue());
			log.info(LogUtil.getRmiMessage("取验证码并删除", new Object[]{key}, "成功"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage("取验证码并删除", 
					new Object[]{key}, "失败" + e, syncData));

			throw new RuntimeException(e);
		}
		
		return syncData;
	}
	
	/**
	 * 同步数据
	 * @param syncData
	 * @return 返回新数据(比如查询的会返回新的数据)
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
			newSyncData = getValidCode(syncData.getSessionID()); // 复用的sessionID代表key
			break;
		default:
				break;
		}

		return newSyncData;
	}
	
	/**
	 * 获取队列里的所需信息
	 * @return List
	 * @throws RemoteException
	 */
	public List<Map<String, Object>> getAllSyncDatas() throws RemoteException {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();	

		try {
			log.info(LogUtil.getRmiMessage("获取队列里的所需信息", 
					new Object[]{"NA"}, "中", "NA"));
			
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
			log.info(LogUtil.getRmiMessage("获取队列里的所需信息", 
					new Object[]{"NA"}, "成功", list));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage("获取队列里的所需信息", 
					new Object[]{"NA"}, "失败" + e, list));

			throw new RuntimeException(e);
		}
		
		return list;
	}
	
	/**
	 * 删除队列中选中的记录
	 * @param indexs
	 * @return int
	 * @throws RemoteException
	 */
	public int remove(int[] indexs) throws RemoteException {
		int count = 0;

		try {
			log.info(LogUtil.getRmiMessage("删除队列中选中的记录", 
					new Object[]{indexs}, "中", "NA"));
			
			BlockingDeque<SyncData> syncQueues = SyncServer.getInstance().getSyncQueue();
			SyncData syncData = new SyncData(0);
			for (int index : indexs) {
				syncData.setIndex(index);
				if (syncQueues.remove(syncData)) {
					count++;
				}
			}
			log.info(LogUtil.getRmiMessage("删除队列中选中的记录", 
					new Object[]{indexs}, "成功", count));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage("删除队列中选中的记录", 
					new Object[]{indexs}, "失败" + e, count));

			throw new RuntimeException(e);
		}
		
		return count;
	}

	/**
	 * 清空队列
	 * @throws RemoteException
	 */
	public void clear() throws RemoteException {

		try {
			log.info(LogUtil.getRmiMessage("清空队列", new Object[]{"NA"}, "中", "NA"));
			
			SyncServer.getInstance().getSyncQueue().clear();
			log.info(LogUtil.getRmiMessage("清空队列", new Object[]{"NA"}, "成功", "NA"));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage("清空队列", 
					new Object[]{"NA"}, "失败" + e, "NA"));

			throw new RuntimeException(e);
		}
	}

	/**
	 * 获取同步队列里的请求个数
	 * @return int
	 * @throws RemoteException
	 */
	public int getSyncDataNum() throws RemoteException {
		int num = 0;
		
		try {
			log.info(LogUtil.getRmiMessage("清空队列", new Object[]{"NA"}, "中", "NA"));
			
			num = SyncServer.getInstance().getSyncQueue().size();
			
			log.info(LogUtil.getRmiMessage("获取同步队列里的请求个数", 
					new Object[]{"NA"}, "成功", num));
		} catch (Exception e) {
			log.error(LogUtil.getRmiMessage("获取同步队列里的请求个数", 
					new Object[]{"NA"}, "失败" + e, num));
			
			throw new RuntimeException(e);
		}
		
		return num;
	}
}
