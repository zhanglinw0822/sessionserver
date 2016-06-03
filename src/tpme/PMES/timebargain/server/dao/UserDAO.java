package tpme.PMES.timebargain.server.dao;

import java.sql.Timestamp;
import java.util.Map;
import tpme.PMES.timebargain.server.model.Privilege;
import tpme.PMES.timebargain.server.model.TraderInfo;

public interface UserDAO {
	/**
	 * �޸Ŀͻ��绰����
	 * 
	 * @param traderID
	 * @param passwordOld
	 * @param password
	 * @return 0���ɹ���-1��ԭ�����ȷ��-2���ͻ����벻����
	 */
	int changePhonePassowrd(String traderID, String passwordOld,
			String password, String operateIP);
	
	/**
	 * �޸Ŀͻ�����
	 * 
	 * @param traderID
	 * @param passwordOld
	 * @param password
	 * @return 0���ɹ���-1��ԭ�����ȷ��
	 */
	int changePassowrd(String traderID, String passwordOld,
			String password, String operateIP);

	/**
	 * ��������Ϣ ����Ա���ͻ�������ʱ����Ȩ��
	 * 
	 * @param memberID
	 *            ��Ա����
	 * @param customerID
	 *            �ͻ�����
	 * @param phonePassword
	 *            �绰����
	 * @return 0���ɹ� -1���ͻ������ڴ˻�Ա -2���ͻ����벻���� -3���绰���벻��ȷ
	 */
	long checkDelegateInfo(String memberID, String customerID,
			String phonePassword);

	/**
	 * ���±������������߻ỰΪ����״̬
	 * @param groupId ����Id
	 */
	void updateGroupSessionDownLine(int groupId, String moduleId);

	/**
	 * ���ȫ����־
	 * 
	 * @param operator
	 *            ������
	 * @param operatorIP
	 *            ������IP
	 * @param operatorType
	 *            ��������
	 * @param operatorContent
	 *            ��������
	 * @param operatorResult
	 *            �������
	 */
	void addGlobalLog(String operator, String operatorIP,
			int operatorType, String operatorContent, int operatorResult);

	/**
	 * ���ݵ�½�Ľ���ԱID��ȡ����ԱȨ�޺ͽ�����Ȩ�� ��ʼ��Privilege����
	 */
	Privilege getTradePrivilege(TraderInfo info);

	/**
	 * ��½�ɹ�
	 * @param sessionID �ỰID
	 * @param traderID ����ԱID
	 * @param logonIP ��¼IP
	 * @param logonModule ��¼ģ��
	 * @param logonTime ��¼ʱ��
	 * @param groupId ����ID
	 * @param serverId ������ID
	 * @param ZID �����˺�
	 * @param firmID ������ID
	 * @return ����Ա����
	 */
	void LogonSuccess(String sessionID, String traderID, String logonIP,
			String logonModule, int groupId, int serverId, String ZID);

	/**
	 * ��½ʧ��
	 * @param traderID ����ԱID
	 * @param logonIP ��¼IP
	 * @param logonModule ��¼ģ��
	 * @param lastLogonTime �ϴε�½ʱ��
	 * @param allowLogonError �������ֵ
	 * @return -2:������� -3:�����������ֵ
	 */
	void LogonError(String traderID, String logonIP, String logonModule, 
			Timestamp lastLogonTime, int allowLogonError);

	/**
	 * ע��session
	 * @param sessionID
	 */
	void Logout(String sessionID, String operator, 
			String operatorIP, String operatorContent);

	/**
	 * ��ȡ����Ա��Ϣ
	 * @param userId ����ԱID
	 * @return map ����Ա��Ϣ
	 */
	Map getOneTraderById(String userId);

	/**
	 * ��ȡM_Configuration valueֵ
	 */
	String getConfigurationValue(String key);

	/**
	 * ����serverName��ȡ��serverName������
	 * @param serverName
	 * @return serverName���� Map;
	 */
	Map<String, Object> getLBServerName(String serverName);

	/**
	 * ����serverName��ȡ��serverName�ı���������(�����ؾ��⹦�ܵ�)
	 * @param serverName
	 * @return serverNameͬ���˻������� Map;
	 */
	Map<String, Object> getSlaveLBServerName(String serverName);
	
	/**
	 * ��ȡ��������Ϣ
	 * @param firmID ������ID
	 * @return map ��������Ϣ
	 */
	public Map getFirmInfoById(String traderID);
}
