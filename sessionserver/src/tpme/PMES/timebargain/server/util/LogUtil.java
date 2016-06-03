package tpme.PMES.timebargain.server.util;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tpme.PMES.timebargain.server.Server;

/**
 * ��־������
 * 
 * @author wangyang
 *
 */

public class LogUtil {
	/**
	 * Ӧ����־ �����û�###����ԴIP###��������(��:���������ȣ�###��������###����������磺�ɹ���ʧ�ܣ�###��������
	 * 
	 * @param system
	 * @param type
	 * @param content
	 * @param result
	 * @param reBack
	 * @return String
	 */
	public static String getMessage(String system, String type,
			Object content, String result, String reBack) {
		String mySystem = system;
		
		if (null == system) {
			mySystem = "system";
		}
		
		StringBuilder sb = new StringBuilder(mySystem);
		
		sb.append("###" + Server.getInstance().getLocalAddress()).
				append("###" + type).append("###" + content).
				append("###" + result).append("###" + reBack);
		
		return sb.toString();
	}
	
	/**
	 * Ӧ����־ �����û�###����ԴIP###��������(��:���������ȣ�###��������###����������磺�ɹ���ʧ�ܣ�###��������
	 * 
	 * @param system
	 * @param type
	 * @param content
	 * @param result
	 * @param reBack
	 * @return String
	 */
	public static String getMessage(String system, String type,
			Object content, String result, Object reBack) {
		String mySystem = system;
		
		if (null == system) {
			mySystem = "system";
		}
		
		StringBuilder sb = new StringBuilder(mySystem);
		
		sb.append("###" + Server.getInstance().getLocalAddress()).
				append("###" + type).append("###" + content).
				append("###" + result).append("###" + reBack);
		
		return sb.toString();
	}

	/**
	 * ϵͳ��־
	 * 
	 * @param type
	 * @param result
	 * @return String
	 */
	public static String getSysMessage(String type, String result) {
		StringBuilder sb = new StringBuilder("system");
		
		sb.append("###" + Server.getInstance().getLocalAddress()).
				append("###" + type).append("###NA").append("###" + result).
				append("###NA");
		
		return sb.toString();
	}
	
	/**
	 * ϵͳ��־
	 * 
	 * @param type
	 * @param content
	 * @param result
	 * @return String
	 */
	public static String getRmiMessage(String type, Object[] content, String result) {
		String cont = "";
		StringBuilder sb = new StringBuilder("system");
		
		for (int i = 0; i < content.length; i++) {
			cont += content[i] + "|";
		}
		
		cont = cont.substring(0, cont.lastIndexOf("|"));
		
		sb.append("###" + Server.getInstance().getLocalAddress()).append("###" + type).
				append("###" + cont).append("###" + result).
				append("###NA");
		
		return sb.toString();
	}
	
	/**
	 * ϵͳ��־
	 * 
	 * @param type
	 * @param content
	 * @param result
	 * @param reBack
	 * @return
	 */
	public static String getRmiMessage(String type, Object[] content, 
			String result, Object reBack) {
		String cont = "";
		StringBuilder sb = new StringBuilder("system");
		
		for (int i = 0; i < content.length; i++) {
			cont += content[i] + "|";
		}
		
		cont = cont.substring(0, cont.lastIndexOf("|"));
		
		sb.append("###" + Server.getInstance().getLocalAddress()).
				append("###" + type).append("###" + cont).append("###" + result).
				append("###" + reBack);
		
		return sb.toString();
	}
	
	/**
	 * Ӧ����־ �����û�###����ԴIP###��������(��:���������ȣ�###��������###����������磺�ɹ���ʧ�ܣ�###��������
	 * 
	 * @param system
	 * @param ip
	 * @param type
	 * @param content
	 * @param result
	 * @param reBack
	 * @return
	 */
	public static String getRmiMessage(String ip, String type,
			Object[] content, String result) {
		String cont = "";
		StringBuilder sb = new StringBuilder("system");
		
		for (int i = 0; i < content.length; i++) {
			cont += content[i] + "|";
		}
		
		cont = cont.substring(0, cont.lastIndexOf("|"));
		
		
		sb.append("###" + ip).append("###" + type).append("###" + cont)
				.append("###" + result).append("###" + "NA");
		
		return sb.toString();
	}
	
	/**
	 * Ӧ����־ �����û�###����ԴIP###��������(��:���������ȣ�###��������###����������磺�ɹ���ʧ�ܣ�###��������
	 * 
	 * @param ip
	 * @param type
	 * @param content
	 * @param result
	 * @param reBack
	 * @return String
	 */
	public static String getRmiMessage(String ip, String type,
			Object[] content, String result, Object reBack) {
		String cont = "";
		StringBuilder sb = new StringBuilder("system");
		
		for (int i = 0; i < content.length; i++) {
			cont += content[i] + "|";
		}
		
		cont = cont.substring(0, cont.lastIndexOf("|"));
		
		
		sb.append("###" + ip).append("###" + type).append("###" + cont)
				.append("###" + result).append("###" + reBack);
		
		return sb.toString();
	}
	
	/**
	 * Ӧ����־ �����û�###����ԴIP###��������(��:���������ȣ�###��������###����������磺�ɹ���ʧ�ܣ�###��������
	 * 
	 * @param system
	 * @param ip
	 * @param type
	 * @param content
	 * @param result
	 * @param reBack
	 * @return String
	 */
	public static String getRmiMessage(String system, String ip, String type,
			Object[] content, String result, Object reBack) {
		String cont = "";
		String mySystem = system;
		
		if (null == system) {
			mySystem = "system";
		}
		
		StringBuffer sb = new StringBuffer(mySystem);
		
		for (int i = 0; i < content.length; i++) {
			cont += content[i] + "|";
		}
		
		cont = cont.substring(0, cont.lastIndexOf("|"));
		
		sb.append("###" + ip).append("###" + type).append("###" + cont).
				append("###" + result).append("###" + reBack);
		
		return sb.toString();
	}
	
	private static final Log log = LogFactory.getLog("audit");
	
	public static void beginLog(Class clazz,String type,Object content,Date now){
		if(log.isDebugEnabled()){
			try{
				LogFactory.getLog("audit."+clazz.getName()).info(LogUtil.getMessage("Thread:"+Thread.currentThread().getId(),type+"###begin", content, "begin", "NA"));
				now.setTime(new Date().getTime());
			}catch(Exception e){
				LogFactory.getLog("audit."+clazz.getName()).error("Thread:"+Thread.currentThread().getId()+"###audit��־�쳣",e);
			}
		}
	}
	
	public static void endLog(Class clazz,String type,Object content,Date now){
		if(log.isDebugEnabled()){
			try{
				LogFactory.getLog("audit."+clazz.getName()).info(LogUtil.getMessage("Thread:"+Thread.currentThread().getId(),type+"###end", content, "end" , new Date().getTime() -now.getTime()+"ms"));
			}catch(Exception e){
				LogFactory.getLog("audit."+clazz.getName()).error("Thread:"+Thread.currentThread().getId()+"###audit��־�쳣",e);
			}
		}
	}
	
}