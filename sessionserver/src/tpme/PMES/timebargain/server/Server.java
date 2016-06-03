package tpme.PMES.timebargain.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import tpme.PMES.timebargain.AppMonitor.AppMonitor;
import tpme.PMES.member.ActiveUser.LogonManager;
import tpme.PMES.timebargain.server.dao.DAOBeanFactory;
import tpme.PMES.timebargain.server.dao.TradeQueryDAO;
import tpme.PMES.timebargain.server.dao.UserDAO;
import tpme.PMES.timebargain.server.model.Privilege;
import tpme.PMES.timebargain.server.model.ValidCode;
import tpme.PMES.timebargain.server.util.LogUtil;

public class Server {
	private final Log logger = LogFactory.getLog(getClass());

	public String serverName = null;
	public String moduleId = null;
	public int groupId; // 分组ID
	public int serverId; // 服务器ID
	
	private String localAddress = null;//本机IP
	
	private LogonManager logonManager = null;

	private static Server server = null;

	private static UserDAO userDAO = null;

	private static Hashtable<String, Privilege> privilegeQueue = new Hashtable<String, Privilege>(2000); // 用于存按auSessionId为键值的权限信息

	private static Hashtable<String, ValidCode> validCodeQueue = new Hashtable<String, ValidCode>(2000); // 用于存按key为键值的验证码信息

	private static long validCodeExpireTime = 120000; // 120秒超时

	private CheckValidCodeThread checkValidCodeThread = null;

	class CheckValidCodeThread extends Thread {
		private boolean threadEnd = false; // 线程结束标志

		public void run() {
			String validCodeKey = null;
            ValidCode validCode = null;
            Enumeration validCodeKeys = null;
            long curTime = System.currentTimeMillis();

			while (!threadEnd) {
	            validCodeKeys = validCodeQueue.keys();
	            curTime = System.currentTimeMillis();
	            
	            try {
		            while (validCodeKeys.hasMoreElements()) {
		            	validCodeKey = (String) validCodeKeys.nextElement();
		            	validCode = validCodeQueue.get(validCodeKey);
		
		                if (curTime > (validCode.getCreateTime() + validCodeExpireTime)) { // 超时
		                	validCodeQueue.remove(validCodeKey);
		                	// 因过期被删除的验证码计数
							AppMonitor.getInstance().add(Constants.MON_REMOVECODE_COUNT, 1);
		                }
		            }
		            // 验证码队列大小
					AppMonitor.getInstance().set(Constants.MON_CODE_QUEUE, validCodeQueue.size());

	            
					Thread.sleep(5000); // 每5秒种轮询一次

					//验证码超时监控线程运行计数
					AppMonitor.getInstance().add(Constants.MON_VALIDCODE_COUNT, 1);
				} catch (InterruptedException e) {
					logger.error(LogUtil.getSysMessage("验证码超时监控线程", "异常" + e));
				} catch (Exception e) {
					logger.error(LogUtil.getSysMessage("验证码超时监控线程", "异常" + e));
				}
			}
		}

		public void close() {
			threadEnd = true;
			try{
				this.interrupt();
			}catch(Exception e) {
				logger.error(LogUtil.getSysMessage("验证码超时监控线程", "异常" + e));
			}
		}
	}

	public static Hashtable<String, Privilege> getPrivilegeQueue() {
		return privilegeQueue;
	}

	public static Hashtable<String, ValidCode> getVaidCodeQueue() {
		return validCodeQueue;
	}

	public static UserDAO getUserDAO() {
		return userDAO;
	}

	private static TradeQueryDAO tradeQueryDAO = null;

	public static TradeQueryDAO getTradeQueryDAO() {
		return tradeQueryDAO;
	}

	public void init(String serverName, String moduleId, 
			int groupId, int serverId) {
		this.serverName = serverName;
		this.moduleId = moduleId;
		this.groupId = groupId;
		this.serverId = serverId;

		userDAO = (UserDAO) DAOBeanFactory.getBean("userDAO");
		tradeQueryDAO = (TradeQueryDAO) DAOBeanFactory.getBean("tradeQueryDAO");

		// 获取对端RMI失败,说明对端没有启动,本机也才启动,此时要将本分组所有会话下线
		if (null == SyncServer.getInstance().getSyncRMI()) { 
			// 将本分组所有会话设置为下线状态
			userDAO.updateGroupSessionDownLine(this.groupId, this.moduleId);
		}

		String strValidCodeExpireTime = DAOBeanFactory.getConfig("ValidCodeExpireTime");
		if (null != strValidCodeExpireTime) {
			validCodeExpireTime = Integer.parseInt(strValidCodeExpireTime);
		}

		checkValidCodeThread = new CheckValidCodeThread();
		checkValidCodeThread.start();
	}

	private Server(){
		try {
			this.localAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			this.localAddress = "127.0.0.1";
		}
	}

	public static Server getInstance(){
		if(null == server){
			synchronized (Server.class) {
				if (null == server) {
					server = new Server();
				}
			}
		}

		return server;
	}

	public void initLogonManager() {
		int expireTime = 5;
		String strExpireTime = DAOBeanFactory.getConfig("ExpireTime");
		if (null != strExpireTime) {
			expireTime = Integer.parseInt(strExpireTime);
		}

		int multiMode = 1;
		String strMultiMode = DAOBeanFactory.getConfig("MultiMode");
		if (null != strMultiMode) {
			multiMode = Integer.parseInt(strMultiMode);
		}

		this.logonManager = LogonManager.createInstance(moduleId,
				expireTime, multiMode);

		LogonManager.getActiveUserManager().setGroupId(groupId);
		LogonManager.getActiveUserManager().setServerId(serverId);
	}

	public LogonManager getLogonManager() {
		return logonManager;
	}

	public void stop() {
		try {
			checkValidCodeThread.close();
		} catch (Exception e) {
			logger.error(LogUtil.getMessage("system",
					"关闭logonManager", "NA", "失败" + e, "NA"));
		}
		logonManager = null;
	}

	public String getLocalAddress() {
		return localAddress;
	}

	public void setLocalAddress(String localAddress) {
		this.localAddress = localAddress;
	}
}
