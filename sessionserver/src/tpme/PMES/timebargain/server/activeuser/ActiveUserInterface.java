package tpme.PMES.timebargain.server.activeuser;

public interface ActiveUserInterface {

	static final int typeLogoff = 1; // 正常退出
	static final int typeKicked = 2; // 本实例被踢下线
	static final int typeExpired = 3; // 过期被踢下线

	/**
	 * 当用户登录时回调该方法
	 * 
	 * @param sessionID
	 * @param traderID
	 */
	void put(String sessionID, String traderID);

	/**
	 * 当用户登出、超时或者被踢下线时回调该方法
	 * 
	 * @param sessionID
	 * @param traderID
	 * @param type
	 * @param onlineNum 剩余在线实例数
	 */
	void remove(String sessionID, String traderID, int type, int onlineNum);
}
