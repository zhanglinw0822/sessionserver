package tpme.PMES.timebargain.server.dao;

import java.sql.Timestamp;
import java.util.Map;
import tpme.PMES.timebargain.server.model.Privilege;
import tpme.PMES.timebargain.server.model.TraderInfo;

public interface UserDAO {
	/**
	 * 修改客户电话密码
	 * 
	 * @param traderID
	 * @param passwordOld
	 * @param password
	 * @return 0：成功；-1：原口令不正确；-2：客户代码不存在
	 */
	int changePhonePassowrd(String traderID, String passwordOld,
			String password, String operateIP);
	
	/**
	 * 修改客户密码
	 * 
	 * @param traderID
	 * @param passwordOld
	 * @param password
	 * @return 0：成功；-1：原口令不正确；
	 */
	int changePassowrd(String traderID, String passwordOld,
			String password, String operateIP);

	/**
	 * 检查代理信息 当会员代客户操作的时候检查权限
	 * 
	 * @param memberID
	 *            会员代码
	 * @param customerID
	 *            客户代码
	 * @param phonePassword
	 *            电话密码
	 * @return 0：成功 -1：客户不属于此会员 -2：客户代码不存在 -3：电话密码不正确
	 */
	long checkDelegateInfo(String memberID, String customerID,
			String phonePassword);

	/**
	 * 更新本分组所有在线会话为下线状态
	 * @param groupId 分组Id
	 */
	void updateGroupSessionDownLine(int groupId, String moduleId);

	/**
	 * 添加全局日志
	 * 
	 * @param operator
	 *            操作人
	 * @param operatorIP
	 *            操作人IP
	 * @param operatorType
	 *            操作类型
	 * @param operatorContent
	 *            操作内容
	 * @param operatorResult
	 *            操作结果
	 */
	void addGlobalLog(String operator, String operatorIP,
			int operatorType, String operatorContent, int operatorResult);

	/**
	 * 根据登陆的交易员ID获取交易员权限和交易商权限 初始化Privilege对象
	 */
	Privilege getTradePrivilege(TraderInfo info);

	/**
	 * 登陆成功
	 * @param sessionID 会话ID
	 * @param traderID 交易员ID
	 * @param logonIP 登录IP
	 * @param logonModule 登录模块
	 * @param logonTime 登录时间
	 * @param groupId 分组ID
	 * @param serverId 服务器ID
	 * @param ZID 正心账号
	 * @param firmID 交易商ID
	 * @return 交易员名称
	 */
	void LogonSuccess(String sessionID, String traderID, String logonIP,
			String logonModule, int groupId, int serverId, String ZID);

	/**
	 * 登陆失败
	 * @param traderID 交易员ID
	 * @param logonIP 登录IP
	 * @param logonModule 登录模块
	 * @param lastLogonTime 上次登陆时间
	 * @param allowLogonError 允许错误阀值
	 * @return -2:密码错误 -3:超过允许错误阀值
	 */
	void LogonError(String traderID, String logonIP, String logonModule, 
			Timestamp lastLogonTime, int allowLogonError);

	/**
	 * 注销session
	 * @param sessionID
	 */
	void Logout(String sessionID, String operator, 
			String operatorIP, String operatorContent);

	/**
	 * 获取交易员信息
	 * @param userId 交易员ID
	 * @return map 交易员信息
	 */
	Map getOneTraderById(String userId);

	/**
	 * 获取M_Configuration value值
	 */
	String getConfigurationValue(String key);

	/**
	 * 根据serverName获取本serverName的属性
	 * @param serverName
	 * @return serverName属性 Map;
	 */
	Map<String, Object> getLBServerName(String serverName);

	/**
	 * 根据serverName获取本serverName的备机的属性(带负载均衡功能的)
	 * @param serverName
	 * @return serverName同步端机器属性 Map;
	 */
	Map<String, Object> getSlaveLBServerName(String serverName);
	
	/**
	 * 获取交易商信息
	 * @param firmID 交易商ID
	 * @return map 交易商信息
	 */
	public Map getFirmInfoById(String traderID);
}
