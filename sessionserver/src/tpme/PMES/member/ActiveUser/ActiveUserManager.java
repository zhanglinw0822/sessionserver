/*******************************************************************
 * ActiveUserManager.java   04/1/24
 * Copyright 2006 by TPME Company. All Rights Reserved.
 * Author:zhousp
 * 
 ******************************************************************/
package tpme.PMES.member.ActiveUser;

import java.util.*;
import java.text.SimpleDateFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tpme.PMES.timebargain.AppMonitor.AppMonitor;
import tpme.PMES.timebargain.server.Constants;
import tpme.PMES.timebargain.server.Server;
import tpme.PMES.timebargain.server.activeuser.ActiveUserInterface;
import tpme.PMES.timebargain.server.activeuser.impl.ActiveUserInterfaceImpl;
import tpme.PMES.timebargain.server.model.AUValue;
import tpme.PMES.timebargain.server.model.Privilege;
import tpme.PMES.timebargain.server.util.LogUtil;

/**
 * 
 * �˶�����Ϊ���ֽ���ģʽ�ĵ�ǰ��¼�û��ṩ����
 * �û���¼ģʽ�ֳ�SINGLE_MODE��MULTI_MODE����ģ��,SINGLE_MODEͬһʱ��ֻ������һ���û���¼һ��,��ͬһ���û������ڶ��ε�¼ʱ,��һ����¼��
 * �Զ�ʧЧ��MULTI_MODE����ͬһ�û�ͬʱ��ε�¼��Ĭ����MULTI_MODEģʽ��
 * 
 * �汾��1.0.0
 * 
 */

public class ActiveUserManager {
	private transient static final Log logger = LogFactory.getLog(ActiveUserManager.class);
	
	private static final int SINGLE_MODE = 1;
	private static final int MULTI_MODE =  2;

	private static Hashtable<String, AUValue> activeStore = null;    //�����洢��û��Ķ���
	private static Hashtable<String, List> onlineTrader = null;    //��traderId�洢���û��������ߵ�SessionId
	private static AUThread auThread = null;      //�������г�ʱɨ���ص������߳���    
	private static Random random = new Random(); //��������ΨһsessionID���������    

	private static boolean auThreadFlag = false;
	private static int mode = MULTI_MODE ; //Ĭ�ϵ��û���¼������MULTI_MODE

	private int space = 30;       //Ĭ�ϵĳ�ʱ�߳�ɨ��ʱ����30��
	private int expireTime = 60;  //Ĭ�ϵĳ�ʱʱ����60����     

	private int groupId = 0; // Ĭ�ϲ�����
	private int serverId = 0; // Ĭ�ϲ���server
	private static short sequeceId = 0; // ��������sessionID

	private static ActiveUserInterface activeUserInterface = new 
			ActiveUserInterfaceImpl(Server.getInstance()); // ����activeStore���ϲ�Ļص�

    /**
    * ���캯��
    */
	public ActiveUserManager() {
    	if (!auThreadFlag) {
    		activeStore = new Hashtable<String, AUValue>(600);
    		onlineTrader = new Hashtable<String, List>(600);

    		auThreadFlag = true;
    		auThread = new AUThread(activeStore, onlineTrader, space * 1000, 
    				expireTime * 60000);
    		auThread.start();     
        }
	}

    /**
    * ����һ�����ӳ�
    * @param space ��ʱɨ��ļ��ʱ��
    * @param expireTime ��ʱʱ��
    * @param mode �û���¼�Ĺ���ģʽ
    */
	public ActiveUserManager(int space , int expireTime , int mode) {
    	if (!auThreadFlag) {
    		this.space = space;
    		this.expireTime = expireTime;
    		setMode(mode);

    		activeStore = new Hashtable<String,AUValue>(600);
    		onlineTrader = new Hashtable<String, List>(600);

    		auThreadFlag = true;
    		auThread = new AUThread(activeStore, onlineTrader, space * 1000, 
    				expireTime * 60000);
    		auThread.start(); 
    	}
	}

	/**
	 * �ͷ�au
	 */
	public void dispose() {
		auThreadFlag = false;
		auThread.close();
	}

    /**
     * ���ý���ģʽ
     * @param m ģʽֵ
     * @return void
     */
	public static void setMode(int m) {
		mode = m;
	}

    /**
     * ���ñ�AU�����ķ���Id
     * @param groupId ����Id
     * @return void
     */
	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

    /**
     * ���ñ�AU�ķ�����Id
     * @param serverId ������Id
     * @return void
     */
	public void setServerId(int serverId) {
		this.serverId = serverId;
	}

    /**
     * ��sessionID��au���뵽����Hashtable��
     * @param sessionID �û��ỰID
     * @param aUValue �Ự��Ϣ
     * @param isCallBack �Ƿ�ص��ϲ�ʵ��
     * @return String ����sessionID
     */
	public static String addOnlineSession(String sessionID, AUValue aUValue, 
			boolean isCallBack) {
		Date now =new Date();
		Date now1 = new Date();
		LogUtil.beginLog(ActiveUserManager.class, "ActiveUserManager.addOnlineSession", sessionID, now1);
		if (null == aUValue) {
			return null;
		}

		String traderId = aUValue.getUserName();
		if (null == traderId) {
			return null;
		}

		List onlineLst = null;
		synchronized (ActiveUserManager.class) {
			if (SINGLE_MODE == mode) { // ����ǵ���ģʽ�򽫸�traderIdԭ��������ʵ��������(�����ٴ����ݿ��Ƴ�)
				now = new Date();
				LogUtil.beginLog(ActiveUserManager.class, "ActiveUserManager.removeOnlineTrader", sessionID, now);
				onlineLst = removeOnlineTrader(traderId, 
						ActiveUserInterface.typeKicked, false);
				LogUtil.endLog(ActiveUserManager.class, "ActiveUserManager.removeOnlineTrader", sessionID, now);
			} else if (MULTI_MODE != mode) {
				return null; // �����ǵ����ֲ��Ƕ���ģʽ(˵��ģʽ����)
			}

			activeStore.put(sessionID, aUValue); // ��session��Ϣ��ӵ�activeStore��

			List lst = onlineTrader.get(traderId); // ��traderId���ӵ���traderId������session�б���
			if (null == lst) {
				lst = new LinkedList();
				onlineTrader.put(traderId, lst);
			}

			if (!lst.contains(sessionID)) {
				lst.add(sessionID);
			}
		}

		if (isCallBack) {
			now = new Date();
			LogUtil.beginLog(ActiveUserManager.class, "ActiveUserInterface.remove", sessionID, now);
			if (null != onlineLst) { // �����ߵ�,���Ƴ����ߵ�
				int size = onlineLst.size();
				for (Object s : onlineLst) {
					activeUserInterface.remove((String)s, traderId, 
							ActiveUserInterface.typeKicked, --size);
				}
			}
			LogUtil.endLog(ActiveUserManager.class, "ActiveUserInterface.remove", sessionID, now);

			activeUserInterface.put(sessionID, aUValue.getUserName()); // ��¼ʱ�����ϲ�Ļص�����
		}
		LogUtil.endLog(ActiveUserManager.class, "ActiveUserManager.addOnlineSession", sessionID, now1);
		return sessionID;
	}

    /**
     * ɾ��һ�����߽���Ա
     * @param sessionID �û��ỰID
     * @param type ɾ������
     * @param isCallBack �Ƿ�ص��ϲ�ʵ��
     * @return ����ע����sessionid��List��
     */
	public static List removeOnlineTrader(String traderId, int type, 
			boolean isCallBack) {
		List lstRes = new LinkedList();

		synchronized (ActiveUserManager.class) {
			List lst = onlineTrader.get(traderId); // ��ȡ��traderId�������ߵ�auSessionId�б�
			if (null == lst) { // û��������Ϣ
				return lstRes;
			}

			Object[] sessionIDs = lst.toArray();
			for (Object s : sessionIDs) {
				if (1 == removeOnlineSession((String)s, type, false)) { // ���ص�,����ͳһ�ص�
					lstRes.add(s); // ���ɹ��Ƴ���session��������
				}
			}
		}

		if (isCallBack) {
			int size = lstRes.size();
			for (Object s : lstRes) {
				activeUserInterface.remove((String)s, traderId, type, --size);
			}
		}

		return lstRes;
	}

    /**
     * ɾ��һ�����߻Ự
     * @param sessionID �û��ỰID
     * @param type ɾ������
     * @param isCallBack �Ƿ�ص��ϲ�ʵ��
     * @return ����ע����sessionID������
     */
	public static int removeOnlineSession(String sessionID, int type, 
			boolean isCallBack) {
		String traderId = null;
		List lst = null;

		synchronized (ActiveUserManager.class) {
			AUValue onlineAUValue = activeStore.remove(sessionID);
			if (null == onlineAUValue) { // �ûỰ������
				return 0;
			}

			traderId = onlineAUValue.getUserName();
			if (null == traderId) {
				return 0;
			}

			lst = onlineTrader.get(traderId); // ��ȡ�ý���Ա���е�����ʵ��
			if (null == lst) {
				return 0;
			}

			lst.remove(sessionID);
			if (0 == lst.size()) { // û�д˽���Ա������ʵ����,ɾ���˼�ֵ
				onlineTrader.remove(traderId);
			}
		}

		if (isCallBack) {
			// ��¼��һʵ����������ʱ�ص��ϲ��ʵ��
			activeUserInterface.remove(sessionID, traderId, type, lst.size());
		}

		return 1;
	}

	/**
	 * ��ȡsessionId
	 * @return 
	 *  String
	*/
	public String getSessionID() {
		String sessionID = createSessionID();

		while (activeStore.containsKey(sessionID)) {
			sessionID = createSessionID();
		}

		return sessionID;
	}
	
    /**
    * ��ָ���û���Ϊ��¼״̬
    * @param userID Ҫ��¼���û�ID    
    * @return 
    *        ���ص�ǰ�û���sessionID�����ʧ�ܷ���-1��
    */
	public String logon(String userID) {            
		return logon(userID, null) ;
	}

    /**
     * ��ָ���û���Ϊ��¼״̬,����¼�û���¼��ip��ַ
     * @param userID Ҫ��¼���û�ID
     * @param ip Ҫ��¼���û�ip  
     * @param return
     *        ���ص�ǰ�û���sessionID�����ʧ�ܷ���-1
     */
	public String logon(String userID, String ip) {
		return logon(userID, ip, null);
	}

    /**
     * ��ָ����AU session id ��¼,����¼�û���¼��ip��ַ
     * @param userID Ҫ��¼���û�ID
     * @param ip Ҫ��¼���û�ip   
     * @return
     *        ���ص�ǰ�û���sessionID�����ʧ�ܷ���-1
     */
	public String logon(String userID, String ip, String auSessionId) {    
		//����һ�����û���Ӧ��AUValue ����
		AUValue au = new AUValue();
		au.setUserName(userID);
		// ip ����Ϊ��
		au.setIP(ip);

		String sessionID;

		if (null == auSessionId) {
			// ����һ���µ�Ψһ��sessionID 
			sessionID = getSessionID();
		} else { // add by fanzh 20090227
			if (!activeStore.containsKey(auSessionId)) {
				sessionID = auSessionId;
			} else {
				//AUValueû����дtoString����, �������if��֧
				if (userID.equals(activeStore.get(auSessionId).getUserName())) {
					return auSessionId;
				} else {
					return null;
				}
			}
		}

		au.setSessionId(sessionID);

		return addOnlineSession(sessionID, au, true);
	}

    /**
    * ע��ָ��sessionID��Ӧ�ĵ�¼״̬
    * @param sessionID:Ҫע����sessionID   
    */
	public void logoff(String sessionID) {
		removeOnlineSession(sessionID, ActiveUserInterface.typeLogoff, true);
	}

    /**
    * ע��ָ��userID��Ӧ�ĵ�¼״̬�������MULTI_MODEģʽ��ע�����û���Ӧ�����е�¼�Ự
    * @param userID:Ҫע����userID  
    * @return
    *        ����ע���ĵ�¼����������
    */
	public int logoffUser(String userID) {
		return removeOnlineTrader(userID, ActiveUserInterface.typeLogoff, true).size();
	}

    /**
    * �õ�ָ��sessionID����Ӧ���û�ID
    * @param sessionID:Ҫ���ҵ�sessionID
    * @return
    *        ������Ӧ���û�userID���������Ϊnull,���ʾ��sessionIDΪ��Ч
    */
	public String getUserID(String sessionID) {
		AUValue av = (AUValue) activeStore.get(sessionID);
        if (null == av) {
        	return null;
        } else {
        	av.setLastTime(System.currentTimeMillis());
        	return av.getUserName();
        }
    }

    /**
     * �õ�ָ��sessionID����Ӧ���û�ip
     * @param  sessionID:Ҫ���ҵ�sessionID  
     * @return
     *        ������Ӧ���û�ip���������Ϊnull,���ʾ��sessionIDΪ��Ч
     */
     public String getUserIP(String sessionID) {
    	 AUValue av = (AUValue) activeStore.get(sessionID);
    	 if (null == av) {
             return null;
    	 } else {
    		 return av.getIP();
    	 }
     }

	/**
	 * �������е�ǰ��Ч�ĵ�¼�û��������MUTIL_MODEģʽ�£�ͬһ���û��ж�������򷵻ض�����¼��<br>
	 * 
	 * @return
	 *        ����һ���ַ�������,�����е�ÿһ��Ԫ�ش���һ���û���¼����,�������û�ID�͵�¼��ʱ����","���Էָ���
	*/
	public String[] getAllUsers() {          
		SimpleDateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");

        Enumeration en = activeStore.elements();
        Vector<String> v1 = new Vector<String>();
        AUValue av = null;
        StringBuffer tempBuffer = null;
        while (en.hasMoreElements()) {
        	av = ((AUValue)en.nextElement());
        	tempBuffer = new StringBuffer(av.getUserName());
            tempBuffer.append(",");
            tempBuffer.append(formatter.format(av.getLogonTime()));
            tempBuffer.append(",");
            tempBuffer.append(formatter.format(av.getLastTime()));
            tempBuffer.append(",");
            tempBuffer.append(av.getIP());
            tempBuffer.append(",");
            tempBuffer.append(av.getSessionId());
            v1.addElement(tempBuffer.toString());
        }

        String[] tmp = new String[v1.size()];
        v1.toArray(tmp);
        return tmp;
	}

	public String[] getAllUsersSys(String name) {
    	SimpleDateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");

        Vector<String> v1 = new Vector<String>();
        AUValue av = null;
        StringBuffer tempBuffer = null;

        List lst = onlineTrader.get(name);
        if (null != lst) {
        	for (int i = 0; i < lst.size(); i++) {
        		av = activeStore.get(lst.get(i));
        		if (null == av) {
        			continue;
        		}

        		tempBuffer = new StringBuffer(av.getUserName());
        		tempBuffer.append(",");
                tempBuffer.append(formatter.format(av.getLogonTime()));
                tempBuffer.append(",");
                tempBuffer.append(formatter.format(av.getLastTime()));
                tempBuffer.append(",");
                tempBuffer.append(av.getIP());
                tempBuffer.append(",");
                tempBuffer.append(av.getSessionId());
                v1.addElement(tempBuffer.toString());
        	}
        }

        String[] tmp = new String[v1.size()];
        v1.toArray(tmp);

        return tmp;
    }

	/**
	 * �������е�ǰ��Ч�ĵ�¼�û��������MUTIL_MODEģʽ�£�ͬһ���û��ж�������򷵻ض�����¼
	 * 
	 * @return
	 *        ����һ���ַ�������,�����е�ÿһ��Ԫ�ش���һ���û���¼����,�������û�ID����¼��ʱ��͵�¼ip��","���Էָ�
	*/
	public String[] getAllUsersWithIP() {
    	 SimpleDateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");

         Enumeration en = activeStore.elements();

         Vector<String> v1 = new Vector<String>();
         AUValue av = null;
         StringBuffer tempBuffer = null;
         while (en.hasMoreElements()) {
        	 av = ((AUValue)en.nextElement());
             tempBuffer = new StringBuffer(av.getUserName());
             tempBuffer.append(",");
             tempBuffer.append(formatter.format(av.getLogonTime()));
             tempBuffer.append(",");
             tempBuffer.append(formatter.format(av.getLastTime()));
             tempBuffer.append(",");
             tempBuffer.append(av.getIP());
             tempBuffer.append(",");
             tempBuffer.append(av.getSessionId());
             v1.addElement(tempBuffer.toString());
         }

         String[] tmp = new String[v1.size()];
         v1.toArray(tmp);
         return tmp;
	}

	/**
	 * ��ȡ���߽���Ա��
	 * @return int
	*/
	public int getOnlineTraderNum() {
		return onlineTrader.size();
	}

	/**
	 * ��ȡ���߻Ự��
	 * @return int
	*/
	public int getOnlineSessionNum() {
		return activeStore.size();
	}

	/**
	 * ��ȡ���߽���Ա
	 * @return Map
	*/
	public Map getOnlineTrader() {
		return onlineTrader;
	}

	/**
	 * ��ȡ���߻Ự
	 * @return Map
	*/
	public Map getOnlineSession() {
		return activeStore;
	}

    public Object getPrivateDate(String sessionID , String key) {
    	return ((AUValue)activeStore.get(sessionID)).getPersonalData(key);
	}

    public void setPrivateDate(String sessionID , String key ,Object obj) {
    	((AUValue)activeStore.get(sessionID)).setPersonalData(key , obj);
	}

	private static String createSessionID(int groupId, int serverId, 
			short sequeceId) {
    	if (0 == groupId && 0 == serverId) {
    		long t1 = 0x000000007FFFFFFF & System.currentTimeMillis();

    		return new Long(((t1 << 32) | Math.abs(random.nextInt()))).toString(); 
    	}

    	if (groupId < 0 || groupId > 127 // 1���ֽڵ��������ֵ��127
    			|| serverId < 0 || serverId > 255) { // ������
    		return null;
    	}

    	long p1 = ((long)groupId << 56) & 0x7F00000000000000L; // ��1�ֽڱ�ʾgroupId
    	long p2 = ((long)serverId << 48) & 0x00FF000000000000L; // ��2�ֽڱ�ʾserverId
    	long p3 = ((0xFFFFFFFF & (System.currentTimeMillis() / 1000)) << 16) 
    			& 0x0000FFFFFFFF0000L; // ʱ��ռ4�ֽ�(ȡ��)
    	long p4 = sequeceId & 0x000000000000FFFFL; // ���ռ2�ֽ�

    	long sessionID = p1 | p2 | p3 | p4;

    	return new Long(sessionID).toString();
	}

	private String createSessionID() {
		short localSequenceId;
		synchronized (ActiveUserManager.class) {
			++this.sequeceId;
			localSequenceId = this.sequeceId;
		}

		return createSessionID(groupId, serverId, localSequenceId);
	}

	/**
	 * ����һ��ֵ��activeStore
	 * @param sessionID
	 * @param aUValue
	 */
	public void put(String sessionID, AUValue aUValue) {
		addOnlineSession(sessionID, aUValue, true);
	}

	/**
	 * ��activeStoreɾ��һ��ֵ
	 * @param sessionID
	 */
	public void remove(String sessionID) {
		removeOnlineSession(sessionID, ActiveUserInterface.typeLogoff, false);
	}

	// 
	/**
	 * ����activeStore�е�һ��ֵ
	 * @param sessionID
	 * @param av
	 */
	public void update(String sessionID, AUValue av) {
		if (null != activeStore.get(sessionID)) {
			activeStore.put(sessionID, av);
		}
	}

	/**
	 * ��activeStore�в�ѯһ��ֵ
	 * @param sessionID
	 * @return AUValue
	 */
	public AUValue getAUValue(String sessionID) {
		return activeStore.get(sessionID);
	}

	/**
	 * �һ��SessionID
	 * @param sessionID
	 * @param lastActiveTime
	 */
	public void ActiveSession(String sessionID, long lastActiveTime) {
		AUValue aUValue = getAUValue(sessionID);
		if (null != aUValue) {
			aUValue.setLastTime(lastActiveTime);
		}
	}

	/**
	 * �һ��SessionID
	 * @param sessionID
	 */
	public void ActiveSession(String sessionID) {
		ActiveSession(sessionID, System.currentTimeMillis());
	}
	
	public void putPrivilegeQueue(String sessionId, Privilege privilege,String methodNameForLog)
	{
		try {
			//�жϸ�session�Ƿ񻹴��
			if (activeStore.containsKey(sessionId)) {
				synchronized (ActiveUserManager.class) {
					if (activeStore.containsKey(sessionId)) {
						Server.getPrivilegeQueue().put(sessionId, privilege);
					} else {
						logger.info("privilegeerror " + methodNameForLog + ":"
								+ sessionId + "," + privilege.getTraderID()
								+ " , " + Server.getPrivilegeQueue().size());
					}
				}
			} else {
				logger.info("privilegeerror " + methodNameForLog + ":"
						+ sessionId + "," + privilege.getTraderID() + " , "
						+ Server.getPrivilegeQueue().size());
			}
		} catch (Exception e) {
			logger.error(
					"privilegeerror " + methodNameForLog + ":" + sessionId, e);
		}
	}

	//�����̣߳��������й��ڼ��
	static class AUThread extends Thread {
		private Hashtable activeStore = null;
		private Hashtable onlineTrader = null;
		private int space; //����Ϊ��λ�ļ��ʱ��
		private long expireTime; //����Ϊ��λ�ĳ�ʱʱ��

		public AUThread (Hashtable activeStore, Hashtable onlineTrader, 
				int space, long expireTime) {
			this.space = space;
			this.expireTime = expireTime;
			this.activeStore = activeStore;
			this.onlineTrader = onlineTrader;
		}

		public void run() {
			while (ActiveUserManager.auThreadFlag) {
				try {
					sleep(space);
					checkExpire();

					//�Ự��ʱ����߳����м���
					AppMonitor.getInstance().add(Constants.MON_OUTTIME_COUNT, 1);
				} catch(InterruptedException e) {
					logger.error(LogUtil.getSysMessage("AU �����߳�", "�쳣" + e));
				} catch(NullPointerException e) {
					logger.error(LogUtil.getSysMessage("AU �����߳�", "�쳣" + e));
				} catch (Exception e) {
					logger.error(LogUtil.getSysMessage("AU �����߳�", "�쳣" + e));
				}
			}
		}

		private void checkExpire() {
			String sessionID = null;
			AUValue av = null;
			Enumeration sessionKeys = activeStore.keys();
			long curTime = System.currentTimeMillis();
			while (sessionKeys.hasMoreElements()) {
				sessionID = (String)sessionKeys.nextElement();
				av = (AUValue)activeStore.get(sessionID);
				if ((curTime - av.getLastTime()) > expireTime) {
					// ��ʱ
					ActiveUserManager.removeOnlineSession(sessionID, 
							ActiveUserInterface.typeExpired, true);
				}
			}
		}

		public synchronized void wakeup() {
			this.notify();
		}

		/**
		 * ��ֹAU����߳�
		 */
		public void close() {
			try {
				// auThreadFlag ����㱻��ֵΪfalse,�ʴ˴����ٸ�ֵ
				this.interrupt();
			} catch (Exception e) {
			}
		}
	}
}