package tpme.PMES.timebargain.server;

/**
 * 交易服务器常量类.
 * 
 * <p>
 * <a href="Constants.java.html"><i>View Source</i></a>
 * </p>
 * 
 * @version 1.0.0.1
 * @author <a href="mailto:zhousp@tpme.com.cn">zhousp</a>
 */
public class Constants {

	/************************ 交易服务器状态 **************************/
	public static final int SYSTEM_STATUS_INIT_SUCCESS = 0;// 初始化完成
	public static final int SYSTEM_STATUS_CLOSE = 1;// 闭市状态
	public static final int SYSTEM_STATUS_CALCING = 2;// 结算中
	public static final int SYSTEM_STATUS_CALCOVER = 3;// 资金结算完成
	public static final int SYSTEM_STATUS_PAUSE = 4;// 暂停交易
	public static final int SYSTEM_STATUS_SECTION_OPEN = 5;// 交易中
	public static final int SYSTEM_STATUS_SECTION_PAUSE = 6;// 节间休息
	public static final int SYSTEM_STATUS_SECTION_CLOSE = 7;// 交易结束
	public static final int SYSTEM_STATUS_BID_OPEN = 8;// 集合竞价交易中
	public static final int SYSTEM_STATUS_BID_CLOSE = 9;// 集合竞价交易结束
	public static final int SYSTEM_STATUS_TRADECALCOVER = 10;// 交易结算完成

	public static final String[] SYSTEM_STATUS = { "初始化完成", "闭市状态", "结算中",
			"资金结算完成", "暂停交易", "交易中", "节间休息", "交易结束", "集合竞价交易中", "集合竞价交易结束",
			"交易结算完成" };

	public static final String ENC_ALGORITHM = "MD5";

	/**
	 * 登录管理 操作类型
	 */
	public static final int LOG_MANAGE_OPERATORTYPR = 1105;
	
	
	/**
	 * 开休市及结算日志管理 操作类型
	 */
	public static final int LOG_SE_OPERATORTYPR = 1202;
	/**
	 * 交易日志管理-委托日志 操作类型
	 */
	public static final int LOG_ORDER_OPERATORTYPR = 1401;

	/************************ Session同步操作 **************************/
	public static final int SESSION_SYNC_OPR_ADD = 1;// 增
	public static final int SESSION_SYNC_OPR_REMOVE = 2;// 删除
	public static final int SESSION_SYNC_OPR_UPDATE = 3;// 改
	public static final int SESSION_SYNC_OPR_GET = 4;// 查
	public static final int SESSION_SYNC_OPR_ACTIVE = 5;// 活动
	public static final int SESSION_SYNC_OPR_VALID_CODE_GET = 6;// 获取验证码

	/************************ Session同步模式 **************************/
	public static final int SESSION_SYNC_MODE_SYNC = 'S';// 同步模式
	public static final int SESSION_SYNC_MODE_ASYNC = 'A';// 异步模式
	public static final int SESSION_SYNC_MODE_NORMAL = 'N';// 正常模式
	
	/***********************监控指标常量*******************************/
	
	/**
	 * 登录请求计数
	 */
	public static final String MON_LOGON_COUNT = "ss_001";
	/**
	 * 登录请求成功计数
	 */
	public static final String MON_LOGON_SUCCESS = "ss_002";
	/**
	 * 密码失败导致登录失败计数
	 */
	public static final String MON_LOGON_FAIL = "ss_003";
	/**
	 * 其他异常导致失败计数
	 */
	public static final String MON_LOGON_EXCEPTION = "ss_004";
	/**
	 * checkuser计数
	 */
	public static final String MON_CHECK_COUNT = "ss_005";
	/**
	 * checkuser成功计数
	 */
	public static final String MON_CHECK_SUCCESS = "ss_006";
	/**
	 * 超时退出计数
	 */
	public static final String MON_REMOVE_COUNT = "ss_007";
	/**
	 * 主动退出计数
	 */
	public static final String MON_LOGOFF_COUNT = "ss_008";
	/**
	 * 当前在线交易员数
	 */
	public static final String MON_TRADER_NUM = "ss_009";
	/**
	 * 当前在线会话数
	 */
	public static final String MON_SESSION_NUM = "ss_010";
	/**
	 * 代客下单取会话上下文计数
	 */
	public static final String MON_GET_CONTEXT = "ss_011";
	/**
	 * 客户端前置机取会话上下文计数
	 */
	public static final String MON_CLIENT_CONTEXT = "ss_012";

	/**
	 * 修改交易密码计数
	 */
	public static final String MON_PASSWORD_COUNT = "ss_013";
	/**
	 * 修改交易密码成功计数
	 */
	public static final String MON_PASSWORD_SUCCESS = "ss_014";
	/**
	 * 修改交易密码失败计数
	 */
	public static final String MON_PASSWORD_FAIL = "ss_015";

	/**
	 * 修改电话密码计数
	 */
	public static final String MON_PHONEPWD_COUNT = "ss_016";
	/**
	 * 修改电话密码成功计数
	 */
	public static final String MON_PHONEPWD_SUCCESS = "ss_017";
	/**
	 * 修改电话密码失败计数
	 */
	public static final String MON_PHONEPWD_FAIL = "ss_018";

	/**
	 * 检查代理信息计数
	 */
	public static final String MON_DELEGATE_COUNT = "ss_019";
	/**
	 * 检查代理信息成功计数
	 */
	public static final String MON_DELEGATE_SUCCESS = "ss_020";
	/**
	 * 检查代理信息失败计数
	 */
	public static final String MON_DELEGATE_FAIL = "ss_021";
	/**
	 * 取验证码计数
	 */
	public static final String MON_GETCODE_COUNT = "ss_022";
	/**
	 * 取验证码成功计数
	 */
	public static final String MON_GETCODE_SUCCESS = "ss_023";
	/**
	 * 取验证码失败计数
	 */
	public static final String MON_GETCODE_FAIL = "ss_024";
	/**
	 * 存验证码计数
	 */
	public static final String MON_PUTCODE_COUNT = "ss_025";
	/**
	 * 登录会话信息同步计数
	 */
	public static final String MON_PUTSESSION_COUNT = "ss_029";
	/**
	 * 注销会话信息同步计数
	 */
	public static final String MON_REMOVESESSION_COUNT = "ss_030";
	/**
	 * 取会话信息同步计数
	 */
	public static final String MON_GETSESSION_COUNT = "ss_031";
	/**
	 * 活动会话信息同步计数
	 */
	public static final String MON_ACTIVESESSION_COUNT = "ss_032";
	/**
	 * 对端取验证码计数
	 */
	public static final String MON_GETOTHERCODE_COUNT = "ss_033";
	/**
	 * 同步队列大小
	 */
	public static final String MON_SYNCQUEUE_SIZE = "ss_034";
	/**
	 * 验证码队列大小
	 */
	public static final String MON_CODE_QUEUE = "ss_035";
	/**
	 * 因过期被删除的验证码计数
	 */
	public static final String MON_REMOVECODE_COUNT = "ss_036";
	
	/**
	 * 会话超时监控线程运行计数
	 */
	public static final String MON_OUTTIME_COUNT = "ss_037";
	
	/**
	 * 验证码超时监控线程运行计数
	 */
	public static final String MON_VALIDCODE_COUNT = "ss_038";
	
	/**
	 * session同步线程运行计数
	 */
	public static final String MON_SYNCSESSION_COUNT = "ss_039";
	
	/**
	 * 同步重新加载会话计数
	 */
	public static final String MON_SYNCUPDATE_COUNT = "ss_040";
}
