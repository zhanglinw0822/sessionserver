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
 * 此对象负责为各种交易模式的当前登录用户提供管理。
 * 用户登录模式分成SINGLE_MODE和MULTI_MODE两种模。,SINGLE_MODE同一时间只允许用一个用户登录一次,即同一个用户当他第二次登录时,上一个登录会
 * 自动失效。MULTI_MODE允许同一用户同时多次登录。默认是MULTI_MODE模式。
 * 
 * 版本：1.0.0
 * 
 */

public class ActiveUserManager {
	private transient static final Log logger = LogFactory.getLog(ActiveUserManager.class);
	
	private static final int SINGLE_MODE = 1;
	private static final int MULTI_MODE =  2;

	private static Hashtable<String, AUValue> activeStore = null;    //用来存储活动用户的对象
	private static Hashtable<String, List> onlineTrader = null;    //按traderId存储该用户所有在线的SessionId
	private static AUThread auThread = null;      //用来进行超时扫描监控的内置线程类    
	private static Random random = new Random(); //用来生成唯一sessionID的随机对象    

	private static boolean auThreadFlag = false;
	private static int mode = MULTI_MODE ; //默认的用户登录类型是MULTI_MODE

	private int space = 30;       //默认的超时线程扫描时间是30秒
	private int expireTime = 60;  //默认的超时时间是60分钟     

	private int groupId = 0; // 默认不分组
	private int serverId = 0; // 默认不分server
	private static short sequeceId = 0; // 用来生成sessionID

	private static ActiveUserInterface activeUserInterface = new 
			ActiveUserInterfaceImpl(Server.getInstance()); // 当对activeStore对上层的回调

    /**
    * 构造函数
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
    * 构造一个联接池
    * @param space 超时扫描的间隔时间
    * @param expireTime 超时时间
    * @param mode 用户登录的管理模式
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
	 * 释放au
	 */
	public void dispose() {
		auThreadFlag = false;
		auThread.close();
	}

    /**
     * 设置交易模式
     * @param m 模式值
     * @return void
     */
	public static void setMode(int m) {
		mode = m;
	}

    /**
     * 设置本AU所属的分组Id
     * @param groupId 分组Id
     * @return void
     */
	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

    /**
     * 设置本AU的服务器Id
     * @param serverId 服务器Id
     * @return void
     */
	public void setServerId(int serverId) {
		this.serverId = serverId;
	}

    /**
     * 将sessionID和au放入到在线Hashtable中
     * @param sessionID 用户会话ID
     * @param aUValue 会话信息
     * @param isCallBack 是否回调上层实现
     * @return String 返回sessionID
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
			if (SINGLE_MODE == mode) { // 如果是单例模式则将该traderId原来的在线实例踢下线(下面再从数据库移除)
				now = new Date();
				LogUtil.beginLog(ActiveUserManager.class, "ActiveUserManager.removeOnlineTrader", sessionID, now);
				onlineLst = removeOnlineTrader(traderId, 
						ActiveUserInterface.typeKicked, false);
				LogUtil.endLog(ActiveUserManager.class, "ActiveUserManager.removeOnlineTrader", sessionID, now);
			} else if (MULTI_MODE != mode) {
				return null; // 即不是单例又不是多例模式(说明模式错误)
			}

			activeStore.put(sessionID, aUValue); // 将session信息添加到activeStore中

			List lst = onlineTrader.get(traderId); // 将traderId增加到该traderId的在线session列表中
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
			if (null != onlineLst) { // 有在线的,先移除在线的
				int size = onlineLst.size();
				for (Object s : onlineLst) {
					activeUserInterface.remove((String)s, traderId, 
							ActiveUserInterface.typeKicked, --size);
				}
			}
			LogUtil.endLog(ActiveUserManager.class, "ActiveUserInterface.remove", sessionID, now);

			activeUserInterface.put(sessionID, aUValue.getUserName()); // 登录时调用上层的回调函数
		}
		LogUtil.endLog(ActiveUserManager.class, "ActiveUserManager.addOnlineSession", sessionID, now1);
		return sessionID;
	}

    /**
     * 删除一个在线交易员
     * @param sessionID 用户会话ID
     * @param type 删除类型
     * @param isCallBack 是否回调上层实现
     * @return 返回注销的sessionid的List。
     */
	public static List removeOnlineTrader(String traderId, int type, 
			boolean isCallBack) {
		List lstRes = new LinkedList();

		synchronized (ActiveUserManager.class) {
			List lst = onlineTrader.get(traderId); // 获取该traderId所有在线的auSessionId列表
			if (null == lst) { // 没有在线信息
				return lstRes;
			}

			Object[] sessionIDs = lst.toArray();
			for (Object s : sessionIDs) {
				if (1 == removeOnlineSession((String)s, type, false)) { // 不回调,下面统一回调
					lstRes.add(s); // 将成功移除的session保存起来
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
     * 删除一个在线会话
     * @param sessionID 用户会话ID
     * @param type 删除类型
     * @param isCallBack 是否回调上层实现
     * @return 返回注销的sessionID数量。
     */
	public static int removeOnlineSession(String sessionID, int type, 
			boolean isCallBack) {
		String traderId = null;
		List lst = null;

		synchronized (ActiveUserManager.class) {
			AUValue onlineAUValue = activeStore.remove(sessionID);
			if (null == onlineAUValue) { // 该会话不在线
				return 0;
			}

			traderId = onlineAUValue.getUserName();
			if (null == traderId) {
				return 0;
			}

			lst = onlineTrader.get(traderId); // 获取该交易员所有的在线实例
			if (null == lst) {
				return 0;
			}

			lst.remove(sessionID);
			if (0 == lst.size()) { // 没有此交易员的在线实例了,删除此键值
				onlineTrader.remove(traderId);
			}
		}

		if (isCallBack) {
			// 登录另一实例被踢下线时回调上层的实现
			activeUserInterface.remove(sessionID, traderId, type, lst.size());
		}

		return 1;
	}

	/**
	 * 获取sessionId
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
    * 将指定用户置为登录状态
    * @param userID 要登录的用户ID    
    * @return 
    *        返回当前用户的sessionID。如果失败返回-1。
    */
	public String logon(String userID) {            
		return logon(userID, null) ;
	}

    /**
     * 将指定用户置为登录状态,并记录用户登录的ip地址
     * @param userID 要登录的用户ID
     * @param ip 要登录的用户ip  
     * @param return
     *        返回当前用户的sessionID。如果失败返回-1
     */
	public String logon(String userID, String ip) {
		return logon(userID, ip, null);
	}

    /**
     * 用指定的AU session id 登录,并记录用户登录的ip地址
     * @param userID 要登录的用户ID
     * @param ip 要登录的用户ip   
     * @return
     *        返回当前用户的sessionID。如果失败返回-1
     */
	public String logon(String userID, String ip, String auSessionId) {    
		//生成一个此用户对应的AUValue 对象
		AUValue au = new AUValue();
		au.setUserName(userID);
		// ip 可能为空
		au.setIP(ip);

		String sessionID;

		if (null == auSessionId) {
			// 生成一个新的唯一的sessionID 
			sessionID = getSessionID();
		} else { // add by fanzh 20090227
			if (!activeStore.containsKey(auSessionId)) {
				sessionID = auSessionId;
			} else {
				//AUValue没有重写toString方法, 不会进入if分支
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
    * 注销指定sessionID对应的登录状态
    * @param sessionID:要注销的sessionID   
    */
	public void logoff(String sessionID) {
		removeOnlineSession(sessionID, ActiveUserInterface.typeLogoff, true);
	}

    /**
    * 注销指定userID对应的登录状态。如果是MULTI_MODE模式则注销此用户对应的所有登录会话
    * @param userID:要注销的userID  
    * @return
    *        返回注销的登录连接数量。
    */
	public int logoffUser(String userID) {
		return removeOnlineTrader(userID, ActiveUserInterface.typeLogoff, true).size();
	}

    /**
    * 得到指定sessionID所对应的用户ID
    * @param sessionID:要查找的sessionID
    * @return
    *        返回相应的用户userID。如果返回为null,则表示此sessionID为无效
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
     * 得到指定sessionID所对应的用户ip
     * @param  sessionID:要查找的sessionID  
     * @return
     *        返回相应的用户ip。如果返回为null,则表示此sessionID为无效
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
	 * 返回所有当前有效的登录用户，如果在MUTIL_MODE模式下，同一个用户有多个连接则返回多条记录。<br>
	 * 
	 * @return
	 *        返回一个字符串数组,数组中的每一个元素代表一个用户登录连接,内容是用户ID和登录的时间用","加以分隔。
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
	 * 返回所有当前有效的登录用户，如果在MUTIL_MODE模式下，同一个用户有多个连接则返回多条记录
	 * 
	 * @return
	 *        返回一个字符串数组,数组中的每一个元素代表一个用户登录连接,内容是用户ID，登录的时间和登录ip用","加以分隔
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
	 * 获取在线交易员数
	 * @return int
	*/
	public int getOnlineTraderNum() {
		return onlineTrader.size();
	}

	/**
	 * 获取在线会话数
	 * @return int
	*/
	public int getOnlineSessionNum() {
		return activeStore.size();
	}

	/**
	 * 获取在线交易员
	 * @return Map
	*/
	public Map getOnlineTrader() {
		return onlineTrader;
	}

	/**
	 * 获取在线会话
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

    	if (groupId < 0 || groupId > 127 // 1个字节的正数最大值是127
    			|| serverId < 0 || serverId > 255) { // 允许负数
    		return null;
    	}

    	long p1 = ((long)groupId << 56) & 0x7F00000000000000L; // 第1字节表示groupId
    	long p2 = ((long)serverId << 48) & 0x00FF000000000000L; // 第2字节表示serverId
    	long p3 = ((0xFFFFFFFF & (System.currentTimeMillis() / 1000)) << 16) 
    			& 0x0000FFFFFFFF0000L; // 时间占4字节(取秒)
    	long p4 = sequeceId & 0x000000000000FFFFL; // 序号占2字节

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
	 * 增加一个值到activeStore
	 * @param sessionID
	 * @param aUValue
	 */
	public void put(String sessionID, AUValue aUValue) {
		addOnlineSession(sessionID, aUValue, true);
	}

	/**
	 * 从activeStore删除一个值
	 * @param sessionID
	 */
	public void remove(String sessionID) {
		removeOnlineSession(sessionID, ActiveUserInterface.typeLogoff, false);
	}

	// 
	/**
	 * 更新activeStore中的一个值
	 * @param sessionID
	 * @param av
	 */
	public void update(String sessionID, AUValue av) {
		if (null != activeStore.get(sessionID)) {
			activeStore.put(sessionID, av);
		}
	}

	/**
	 * 从activeStore中查询一个值
	 * @param sessionID
	 * @return AUValue
	 */
	public AUValue getAUValue(String sessionID) {
		return activeStore.get(sessionID);
	}

	/**
	 * 活动一个SessionID
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
	 * 活动一个SessionID
	 * @param sessionID
	 */
	public void ActiveSession(String sessionID) {
		ActiveSession(sessionID, System.currentTimeMillis());
	}
	
	public void putPrivilegeQueue(String sessionId, Privilege privilege,String methodNameForLog)
	{
		try {
			//判断该session是否还存活
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

	//监视线程，用来进行过期检查
	static class AUThread extends Thread {
		private Hashtable activeStore = null;
		private Hashtable onlineTrader = null;
		private int space; //毫秒为单位的间隔时间
		private long expireTime; //毫秒为单位的超时时间

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

					//会话超时监控线程运行计数
					AppMonitor.getInstance().add(Constants.MON_OUTTIME_COUNT, 1);
				} catch(InterruptedException e) {
					logger.error(LogUtil.getSysMessage("AU 监视线程", "异常" + e));
				} catch(NullPointerException e) {
					logger.error(LogUtil.getSysMessage("AU 监视线程", "异常" + e));
				} catch (Exception e) {
					logger.error(LogUtil.getSysMessage("AU 监视线程", "异常" + e));
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
					// 超时
					ActiveUserManager.removeOnlineSession(sessionID, 
							ActiveUserInterface.typeExpired, true);
				}
			}
		}

		public synchronized void wakeup() {
			this.notify();
		}

		/**
		 * 终止AU检查线程
		 */
		public void close() {
			try {
				// auThreadFlag 在外层被赋值为false,故此处不再赋值
				this.interrupt();
			} catch (Exception e) {
			}
		}
	}
}