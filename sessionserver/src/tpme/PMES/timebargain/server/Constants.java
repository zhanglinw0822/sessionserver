package tpme.PMES.timebargain.server;

/**
 * ���׷�����������.
 * 
 * <p>
 * <a href="Constants.java.html"><i>View Source</i></a>
 * </p>
 * 
 * @version 1.0.0.1
 * @author <a href="mailto:zhousp@tpme.com.cn">zhousp</a>
 */
public class Constants {

	/************************ ���׷�����״̬ **************************/
	public static final int SYSTEM_STATUS_INIT_SUCCESS = 0;// ��ʼ�����
	public static final int SYSTEM_STATUS_CLOSE = 1;// ����״̬
	public static final int SYSTEM_STATUS_CALCING = 2;// ������
	public static final int SYSTEM_STATUS_CALCOVER = 3;// �ʽ�������
	public static final int SYSTEM_STATUS_PAUSE = 4;// ��ͣ����
	public static final int SYSTEM_STATUS_SECTION_OPEN = 5;// ������
	public static final int SYSTEM_STATUS_SECTION_PAUSE = 6;// �ڼ���Ϣ
	public static final int SYSTEM_STATUS_SECTION_CLOSE = 7;// ���׽���
	public static final int SYSTEM_STATUS_BID_OPEN = 8;// ���Ͼ��۽�����
	public static final int SYSTEM_STATUS_BID_CLOSE = 9;// ���Ͼ��۽��׽���
	public static final int SYSTEM_STATUS_TRADECALCOVER = 10;// ���׽������

	public static final String[] SYSTEM_STATUS = { "��ʼ�����", "����״̬", "������",
			"�ʽ�������", "��ͣ����", "������", "�ڼ���Ϣ", "���׽���", "���Ͼ��۽�����", "���Ͼ��۽��׽���",
			"���׽������" };

	public static final String ENC_ALGORITHM = "MD5";

	/**
	 * ��¼���� ��������
	 */
	public static final int LOG_MANAGE_OPERATORTYPR = 1105;
	
	
	/**
	 * �����м�������־���� ��������
	 */
	public static final int LOG_SE_OPERATORTYPR = 1202;
	/**
	 * ������־����-ί����־ ��������
	 */
	public static final int LOG_ORDER_OPERATORTYPR = 1401;

	/************************ Sessionͬ������ **************************/
	public static final int SESSION_SYNC_OPR_ADD = 1;// ��
	public static final int SESSION_SYNC_OPR_REMOVE = 2;// ɾ��
	public static final int SESSION_SYNC_OPR_UPDATE = 3;// ��
	public static final int SESSION_SYNC_OPR_GET = 4;// ��
	public static final int SESSION_SYNC_OPR_ACTIVE = 5;// �
	public static final int SESSION_SYNC_OPR_VALID_CODE_GET = 6;// ��ȡ��֤��

	/************************ Sessionͬ��ģʽ **************************/
	public static final int SESSION_SYNC_MODE_SYNC = 'S';// ͬ��ģʽ
	public static final int SESSION_SYNC_MODE_ASYNC = 'A';// �첽ģʽ
	public static final int SESSION_SYNC_MODE_NORMAL = 'N';// ����ģʽ
	
	/***********************���ָ�곣��*******************************/
	
	/**
	 * ��¼�������
	 */
	public static final String MON_LOGON_COUNT = "ss_001";
	/**
	 * ��¼����ɹ�����
	 */
	public static final String MON_LOGON_SUCCESS = "ss_002";
	/**
	 * ����ʧ�ܵ��µ�¼ʧ�ܼ���
	 */
	public static final String MON_LOGON_FAIL = "ss_003";
	/**
	 * �����쳣����ʧ�ܼ���
	 */
	public static final String MON_LOGON_EXCEPTION = "ss_004";
	/**
	 * checkuser����
	 */
	public static final String MON_CHECK_COUNT = "ss_005";
	/**
	 * checkuser�ɹ�����
	 */
	public static final String MON_CHECK_SUCCESS = "ss_006";
	/**
	 * ��ʱ�˳�����
	 */
	public static final String MON_REMOVE_COUNT = "ss_007";
	/**
	 * �����˳�����
	 */
	public static final String MON_LOGOFF_COUNT = "ss_008";
	/**
	 * ��ǰ���߽���Ա��
	 */
	public static final String MON_TRADER_NUM = "ss_009";
	/**
	 * ��ǰ���߻Ự��
	 */
	public static final String MON_SESSION_NUM = "ss_010";
	/**
	 * �����µ�ȡ�Ự�����ļ���
	 */
	public static final String MON_GET_CONTEXT = "ss_011";
	/**
	 * �ͻ���ǰ�û�ȡ�Ự�����ļ���
	 */
	public static final String MON_CLIENT_CONTEXT = "ss_012";

	/**
	 * �޸Ľ����������
	 */
	public static final String MON_PASSWORD_COUNT = "ss_013";
	/**
	 * �޸Ľ�������ɹ�����
	 */
	public static final String MON_PASSWORD_SUCCESS = "ss_014";
	/**
	 * �޸Ľ�������ʧ�ܼ���
	 */
	public static final String MON_PASSWORD_FAIL = "ss_015";

	/**
	 * �޸ĵ绰�������
	 */
	public static final String MON_PHONEPWD_COUNT = "ss_016";
	/**
	 * �޸ĵ绰����ɹ�����
	 */
	public static final String MON_PHONEPWD_SUCCESS = "ss_017";
	/**
	 * �޸ĵ绰����ʧ�ܼ���
	 */
	public static final String MON_PHONEPWD_FAIL = "ss_018";

	/**
	 * ��������Ϣ����
	 */
	public static final String MON_DELEGATE_COUNT = "ss_019";
	/**
	 * ��������Ϣ�ɹ�����
	 */
	public static final String MON_DELEGATE_SUCCESS = "ss_020";
	/**
	 * ��������Ϣʧ�ܼ���
	 */
	public static final String MON_DELEGATE_FAIL = "ss_021";
	/**
	 * ȡ��֤�����
	 */
	public static final String MON_GETCODE_COUNT = "ss_022";
	/**
	 * ȡ��֤��ɹ�����
	 */
	public static final String MON_GETCODE_SUCCESS = "ss_023";
	/**
	 * ȡ��֤��ʧ�ܼ���
	 */
	public static final String MON_GETCODE_FAIL = "ss_024";
	/**
	 * ����֤�����
	 */
	public static final String MON_PUTCODE_COUNT = "ss_025";
	/**
	 * ��¼�Ự��Ϣͬ������
	 */
	public static final String MON_PUTSESSION_COUNT = "ss_029";
	/**
	 * ע���Ự��Ϣͬ������
	 */
	public static final String MON_REMOVESESSION_COUNT = "ss_030";
	/**
	 * ȡ�Ự��Ϣͬ������
	 */
	public static final String MON_GETSESSION_COUNT = "ss_031";
	/**
	 * ��Ự��Ϣͬ������
	 */
	public static final String MON_ACTIVESESSION_COUNT = "ss_032";
	/**
	 * �Զ�ȡ��֤�����
	 */
	public static final String MON_GETOTHERCODE_COUNT = "ss_033";
	/**
	 * ͬ�����д�С
	 */
	public static final String MON_SYNCQUEUE_SIZE = "ss_034";
	/**
	 * ��֤����д�С
	 */
	public static final String MON_CODE_QUEUE = "ss_035";
	/**
	 * ����ڱ�ɾ������֤�����
	 */
	public static final String MON_REMOVECODE_COUNT = "ss_036";
	
	/**
	 * �Ự��ʱ����߳����м���
	 */
	public static final String MON_OUTTIME_COUNT = "ss_037";
	
	/**
	 * ��֤�볬ʱ����߳����м���
	 */
	public static final String MON_VALIDCODE_COUNT = "ss_038";
	
	/**
	 * sessionͬ���߳����м���
	 */
	public static final String MON_SYNCSESSION_COUNT = "ss_039";
	
	/**
	 * ͬ�����¼��ػỰ����
	 */
	public static final String MON_SYNCUPDATE_COUNT = "ss_040";
}
