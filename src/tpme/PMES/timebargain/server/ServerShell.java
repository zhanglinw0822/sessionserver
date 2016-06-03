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
 * ��½������������.
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

	private static String logonRmiUrl = null; // �󶨵�¼��RMI��ַ
	private static String syncRmiUrl = null; // ��ͬ����RMI��ַ

	public static Registry regServer = null;

	public static LogonRMI logonRMI = null; // �Լ��󶨵ĻỰʹ�õ�RMI
	public static SyncRMI selfSyncRMI = null; // �Լ��󶨵�ͬ��RMI(���Լ�����)

	/**
	 * �����Ự������
	 * @param serverName ������
	 */
	public static void start(String serverName) {
		
		log.info(LogUtil.getMessage("system", 
				"�����Ự������", serverName, "��", "NA"));

		log.info(LogUtil.getMessage("system",  
				"��ȡRMI������Ϣ", serverName, "��", "NA"));

		Map rmiMap = null;
		Map slaveRmiMap = null;
		UserDAO userDAO = (UserDAO) DAOBeanFactory.getBean("userDAO");

		try {
			rmiMap = userDAO.getLBServerName(serverName);

			slaveRmiMap = userDAO.getSlaveLBServerName(serverName);

			log.info(LogUtil.getMessage("system", 
					"��ȡRMI������Ϣ", serverName, "�ɹ�", "NA"));
		} catch (Exception e) {
			log.error(LogUtil.getMessage("system",  
					"��ȡRMI������Ϣ", serverName, "ʧ��" + e, "NA"));
			System.exit(1);
		}

		// �ӱ���������Ϣ�л�ȡ����Session��ͬ����ʽ
		int syncMode = ((String) rmiMap.get("Server_SyncMode")).charAt(0);
		int syncRetryNum = ((BigDecimal) rmiMap.get("Server_SyncNum")).intValue();
		int groupId = ((BigDecimal) rmiMap.get("Server_GroupId")).intValue(); // ��ID
		int serverId = ((BigDecimal) rmiMap.get("Server_Id")).intValue(); // ������ID
		String rmiIp = (String) rmiMap.get("Server_Url");
		int rmiPort = ((BigDecimal) rmiMap.get("Server_Port")).intValue();
		String moduleId = (String) rmiMap.get("Server_ModuleId");

		String slaveRmiIp = (String) slaveRmiMap.get("Server_Url");
		int slaveRmiPort = ((BigDecimal) slaveRmiMap.get("Server_Port")).intValue();

		logonRmiUrl = "rmi://" + rmiIp + ":" + rmiPort + "/LogonRMI";
		
		log.info(LogUtil.getMessage("system",  
				"��ȡRMI����", logonRmiUrl, "�ɹ�", "NA"));
		
		syncRmiUrl = "rmi://" + rmiIp + ":" + rmiPort + "/SyncRMI";
		
		String slaveSyncRmiUrl = "rmi://" + slaveRmiIp + ":" 
			+ slaveRmiPort + "/SyncRMI";

		log.info(LogUtil.getMessage("system",  
				"ͬ��RMI����", slaveSyncRmiUrl, "�ɹ�", "NA"));

		Server server = Server.getInstance();
		try {
			SyncServer.getInstance().init(slaveSyncRmiUrl, syncMode, syncRetryNum);
			server.init(serverName, moduleId, groupId, serverId);
		} catch (Exception e) {
			log.error(LogUtil.getMessage("system", 
					"��ʼ��server", serverName, "ʧ��" + e, "NA"));

			System.exit(1);
		}

		//��ʼ�� logonRMI
		Server.getInstance().initLogonManager();

		// ����RMI��ͨ�Ŷ˿�Ϊ���ֶ˿ںż�1,�̶�ͨ�Ŷ˿�,Ϊ��������ǽ���򷽱�
		try {
			RMISocketFactory.setSocketFactory(new SMRMISocket(rmiPort + 1));
		} catch (IOException e) {
			log.error(LogUtil.getMessage("system", 
					"���ù̶�ͨ�Ŷ˿�", serverName, "ʧ��" + e, "NA"));
			System.exit(1);
		}

		try {
			log.info(LogUtil.getMessage("system", 
					"�����Ự������", serverName, "��", "NA"));
			logonRMI = new LogonRMIImpl(server); // ����Զ�̶���ʵ�ֵ�һ��ʵ����������Server����
			selfSyncRMI = new SyncRMIImpl(); // ����Զ��ʥ��ʵ�ֵ�һ��ʵ��

			// ��������װ��ȫ������
			/*
			 * if(System.getSecurityManager()==null) {
			 * System.setSecurityManager(new RMISecurityManager()); }
			 */

			// ע��˿�
			try {
				regServer = LocateRegistry.createRegistry(rmiPort);
			} catch (RemoteException re) {
				// ��ΪActiveUser�ȵ�����һ��,�����ٴε���createRegistryʱ,���ܻ�ʧ��һ��
				log.error(LogUtil.getMessage("system", 
						"ע��˿�", rmiPort, "ʧ��" + re, "NA"));
			}

			// ������ͬ����RMI(��������������ʱ��Զ˻��Ƚ�������Ϣȫ��ͬ������)
			Naming.rebind(syncRmiUrl, selfSyncRMI);

			Thread.sleep(10000); // �ȴ�10��,�������Ự��RMI

			Naming.rebind(logonRmiUrl, logonRMI);
			log.info(LogUtil.getMessage("system", 
					"RMI��������", serverName, "�ɹ�", "NA"));
		} catch (RemoteException re) {
			log.error(LogUtil.getMessage("system", 
					"RMI��������", serverName, "ʧ��" + re, "NA"));
			System.exit(1);
		} catch (MalformedURLException mfe) {
			log.error(LogUtil.getMessage("system", 
					"RMI��������", serverName, "ʧ��" + mfe, "NA"));
			System.exit(1);
		} catch (Exception e) {
			log.error(LogUtil.getMessage("system", 
					"RMI��������", serverName, "ʧ��" + e, "NA"));
			System.exit(1);
		}
	}

	/**
	 * ֹͣ�Ự������
	 */
	public static void stop() {
		
		// ֹͣRMI
		try {
			Naming.unbind(logonRmiUrl);
			Naming.unbind(syncRmiUrl);
		} catch (RemoteException re) {
			log.error(LogUtil.getMessage("system", 
					"RMI�����������", "NA", "ʧ��" + re, "NA"));
		} catch (MalformedURLException mfe) {
			log.error(LogUtil.getMessage("system", 
					"RMI�����������", "NA", "ʧ��" + mfe, "NA"));
		} catch (NotBoundException nbe) {
			log.error(LogUtil.getMessage("system", 
					"RMI�����������", "NA", "ʧ��" + nbe, "NA"));
		}

		// ����Ѿ�ռ�ö˿�,�˳�ʱע���˿�ռ��
		if (null != regServer) {
			try {
				UnicastRemoteObject.unexportObject(regServer, true); // ǿ��ע��
			} catch (Exception e) {
				log.error(LogUtil.getMessage("system", 
						"ǿ��ע��", "NA", "ʧ��" + e, "NA"));
			}
		}

		//�ͷ���Դ
		Server.getInstance().stop();
	}

	public static String getHostName() {
		try {
			InetAddress addr = InetAddress.getLocalHost();
			// String ip = addr.getHostAddress().toString(); // ��ñ���IP
			return addr.getHostName().toString(); // ��ñ�������
		} catch (UnknownHostException e) {
			return null;
		}
	}

	/**
	 * ��½�������������߳�
	 */
	public static void main(String[] args) {
		// ȡ������
		String serverName = DAOBeanFactory.getConfig("ServerName");

		if (null == serverName || 0 == serverName.length()) { // û��������ȡ������
			serverName = getHostName();

			if (null == serverName || 0 == serverName.length()) {
				log.warn(LogUtil.getMessage("system", 
						"��������", serverName, "ʧ��: û������severName", "NA"));
				return;
			}
		}

		start(serverName);
	}
}
