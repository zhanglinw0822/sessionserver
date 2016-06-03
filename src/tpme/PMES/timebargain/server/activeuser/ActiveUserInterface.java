package tpme.PMES.timebargain.server.activeuser;

public interface ActiveUserInterface {

	static final int typeLogoff = 1; // �����˳�
	static final int typeKicked = 2; // ��ʵ����������
	static final int typeExpired = 3; // ���ڱ�������

	/**
	 * ���û���¼ʱ�ص��÷���
	 * 
	 * @param sessionID
	 * @param traderID
	 */
	void put(String sessionID, String traderID);

	/**
	 * ���û��ǳ�����ʱ���߱�������ʱ�ص��÷���
	 * 
	 * @param sessionID
	 * @param traderID
	 * @param type
	 * @param onlineNum ʣ������ʵ����
	 */
	void remove(String sessionID, String traderID, int type, int onlineNum);
}
