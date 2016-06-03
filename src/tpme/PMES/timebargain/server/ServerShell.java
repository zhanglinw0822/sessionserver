package tpme.PMES.timebargain.server;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import tpme.PMES.timebargain.server.dao.DAOBeanFactory;
import tpme.PMES.timebargain.server.dao.UserDAO;
import tpme.PMES.timebargain.server.rmi.LogonRMI;
import tpme.PMES.timebargain.server.rmi.SyncRMI;
import tpme.PMES.timebargain.server.rmi.impl.LogonRMIImpl;
import tpme.PMES.timebargain.server.rmi.impl.SyncRMIImpl;
import tpme.PMES.timebargain.server.util.LogUtil;
import tpme.PMES.timebargain.service.SMRMISocket;

/**
 * 登陆服务器启动类.
 * 
 * <p>
 * <a href="ServerShell.java.html"><i>View Source</i></a>
 * </p>
 * 
 * @version 1.0.0.1
 * @author <a href="mailto:wangy@tpme.com.cn">wangy</a>
 */
public class ServerShell {
	private static final Log log = LogFactory.getLog(ServerShell.class);

	private static String logonRmiUrl = null; // 绑定登录的RMI地址
	private static String syncRmiUrl = null; // 绑定同步的RMI地址

	public static Registry regServer = null;

	public static LogonRMI logonRMI = null; // 自己绑定的会话使用的RMI
	public static SyncRMI selfSyncRMI = null; // 自己绑定的同步RMI(给自己调用)

	/**
	 * 启动会话服务器
	 * @param serverName 机器名
	 */
	public static void start(String serverName) {
		
		log.info(LogUtil.getMessage("system", 
				"启动会话服务器", serverName, "中", "NA"));

		log.info(LogUtil.getMessage("system",  
				"读取RMI配置信息", serverName, "中", "NA"));

		Map rmiMap = null;
		Map slaveRmiMap = null;
		UserDAO userDAO = (UserDAO) DAOBeanFactory.getBean("userDAO");

		try {
			rmiMap = userDAO.getLBServerName(serverName);

			slaveRmiMap = userDAO.getSlaveLBServerName(serverName);

			log.info(LogUtil.getMessage("system", 
					"读取RMI配置信息", serverName, "成功", "NA"));
		} catch (Exception e) {
			log.error(LogUtil.getMessage("system",  
					"读取RMI配置信息", serverName, "失败" + e, "NA"));
			System.exit(1);
		}

		// 从本机配置信息中获取本机Session的同步方式
		int syncMode = ((String) rmiMap.get("Server_SyncMode")).charAt(0);
		int syncRetryNum = ((BigDecimal) rmiMap.get("Server_SyncNum")).intValue();
		int groupId = ((BigDecimal) rmiMap.get("Server_GroupId")).intValue(); // 分ID
		int serverId = ((BigDecimal) rmiMap.get("Server_Id")).intValue(); // 服务器ID
		String rmiIp = (String) rmiMap.get("Server_Url");
		int rmiPort = ((BigDecimal) rmiMap.get("Server_Port")).intValue();
		String moduleId = (String) rmiMap.get("Server_ModuleId");

		String slaveRmiIp = (String) slaveRmiMap.get("Server_Url");
		int slaveRmiPort = ((BigDecimal) slaveRmiMap.get("Server_Port")).intValue();

		logonRmiUrl = "rmi://" + rmiIp + ":" + rmiPort + "/LogonRMI";
		
		log.info(LogUtil.getMessage("system",  
				"获取RMI配置", logonRmiUrl, "成功", "NA"));
		
		syncRmiUrl = "rmi://" + rmiIp + ":" + rmiPort + "/SyncRMI";
		
		String slaveSyncRmiUrl = "rmi://" + slaveRmiIp + ":" 
			+ slaveRmiPort + "/SyncRMI";

		log.info(LogUtil.getMessage("system",  
				"同步RMI配置", slaveSyncRmiUrl, "成功", "NA"));

		Server server = Server.getInstance();
		try {
			SyncServer.getInstance().init(slaveSyncRmiUrl, syncMode, syncRetryNum);
			server.init(serverName, moduleId, groupId, serverId);
		} catch (Exception e) {
			log.error(LogUtil.getMessage("system", 
					"初始化server", serverName, "失败" + e, "NA"));

			System.exit(1);
		}

		//初始化 logonRMI
		Server.getInstance().initLogonManager();

		// 设置RMI的通信端口为握手端口号加1,固定通信端口,为建立防火墙规则方便
		try {
			RMISocketFactory.setSocketFactory(new SMRMISocket(rmiPort + 1));
		} catch (IOException e) {
			log.error(LogUtil.getMessage("system", 
					"设置固定通信端口", serverName, "失败" + e, "NA"));
			System.exit(1);
		}

		try {
			log.info(LogUtil.getMessage("system", 
					"启动会话服务器", serverName, "中", "NA"));
			logonRMI = new LogonRMIImpl(server); // 生成远程对象实现的一个实例，并传递Server对象
			selfSyncRMI = new SyncRMIImpl(); // 生成远程圣像实现的一个实例

			// 创建并安装安全管理器
			/*
			 * if(System.getSecurityManager()==null) {
			 * System.setSecurityManager(new RMISecurityManager()); }
			 */

			// 注册端口
			try {
				regServer = LocateRegistry.createRegistry(rmiPort);
			} catch (RemoteException re) {
				// 因为ActiveUser先调用了一次,所以再次调用createRegistry时,可能会失败一次
				log.error(LogUtil.getMessage("system", 
						"注册端口", rmiPort, "失败" + re, "NA"));
			}

			// 先启动同步的RMI(这样自身启动的时候对端会先将在线信息全部同步过来)
			Naming.rebind(syncRmiUrl, selfSyncRMI);

			Thread.sleep(10000); // 等待10秒,再启动会话的RMI

			Naming.rebind(logonRmiUrl, logonRMI);
			log.info(LogUtil.getMessage("system", 
					"RMI启动服务", serverName, "成功", "NA"));
		} catch (RemoteException re) {
			log.error(LogUtil.getMessage("system", 
					"RMI启动服务", serverName, "失败" + re, "NA"));
			System.exit(1);
		} catch (MalformedURLException mfe) {
			log.error(LogUtil.getMessage("system", 
					"RMI启动服务", serverName, "失败" + mfe, "NA"));
			System.exit(1);
		} catch (Exception e) {
			log.error(LogUtil.getMessage("system", 
					"RMI启动服务", serverName, "失败" + e, "NA"));
			System.exit(1);
		}
	}

	/**
	 * 停止会话服务器
	 */
	public static void stop() {
		
		// 停止RMI
		try {
			Naming.unbind(logonRmiUrl);
			Naming.unbind(syncRmiUrl);
		} catch (RemoteException re) {
			log.error(LogUtil.getMessage("system", 
					"RMI服务器解除绑定", "NA", "失败" + re, "NA"));
		} catch (MalformedURLException mfe) {
			log.error(LogUtil.getMessage("system", 
					"RMI服务器解除绑定", "NA", "失败" + mfe, "NA"));
		} catch (NotBoundException nbe) {
			log.error(LogUtil.getMessage("system", 
					"RMI服务器解除绑定", "NA", "失败" + nbe, "NA"));
		}

		// 如果已经占用端口,退出时注销端口占用
		if (null != regServer) {
			try {
				UnicastRemoteObject.unexportObject(regServer, true); // 强制注销
			} catch (Exception e) {
				log.error(LogUtil.getMessage("system", 
						"强制注销", "NA", "失败" + e, "NA"));
			}
		}

		//释放资源
		Server.getInstance().stop();
	}

	public static String getHostName() {
		try {
			InetAddress addr = InetAddress.getLocalHost();
			// String ip = addr.getHostAddress().toString(); // 获得本机IP
			return addr.getHostName().toString(); // 获得本机名称
		} catch (UnknownHostException e) {
			return null;
		}
	}

	/**
	 * 登陆服务器启动主线程
	 */
	public static void main(String[] args) {
		// 取本机名
		String serverName = DAOBeanFactory.getConfig("ServerName");

		if (null == serverName || 0 == serverName.length()) { // 没有配置则取机器名
			serverName = getHostName();

			if (null == serverName || 0 == serverName.length()) {
				log.warn(LogUtil.getMessage("system", 
						"启动服务", serverName, "失败: 没有配置severName", "NA"));
				return;
			}
		}

		start(serverName);
	}
}
