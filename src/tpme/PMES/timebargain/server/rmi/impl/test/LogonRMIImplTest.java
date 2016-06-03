package tpme.PMES.timebargain.server.rmi.impl.test;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Random;

import tpme.PMES.timebargain.server.model.Privilege;
import tpme.PMES.timebargain.server.model.SessionContext;
import tpme.PMES.timebargain.server.model.Trader;


/**
 * ��longRMI�ķ������в���
 * @author wangyang
 *
 */
public class LogonRMIImplTest {
	public Object call(String rmiUrl, String name, Object... parameters) throws ConnectException, RemoteException, Exception {
		  Class[] parameterTypes = new Class[parameters.length];
		  for (int i = 0; i < parameters.length; i++) {
			  parameterTypes[i] = parameters[i].getClass();
		  }

		  Remote rmi = null;

		  try {
			  rmi = Naming.lookup(rmiUrl);
		  } catch (MalformedURLException e4) {
			  e4.printStackTrace();
		  } catch (RemoteException e4) {
			  e4.printStackTrace();
		  } catch (NotBoundException e4) {
			  e4.printStackTrace();
		  }

		  if (null == rmi) {
			  return null;
		  }

		  Class rmiClass = rmi.getClass();
		  Method method = null;
		  try {
			  method = rmiClass.getMethod(name, parameterTypes);
		  } catch (SecurityException e2) {
			  e2.printStackTrace();
		  } catch (NoSuchMethodException e2) {
			  e2.printStackTrace();
		  }

		  if (null == method) {
			  return null;
		  }

		  Object ret = null;
		  try {
			  ret = method.invoke(rmi, parameters);
		  } catch (IllegalArgumentException e) {
			  e.printStackTrace();
		  } catch (IllegalAccessException e) {
			  e.printStackTrace();
		  } catch (InvocationTargetException e) {
			  if (!(e.getCause() instanceof ConnectException)) { // �������Ҫ���Ի��߲��������쳣��ֱ�������׳��쳣
					if (e.getCause() instanceof ConnectException) {
						throw new ConnectException(e.getCause().getMessage());
					} else if (e.getCause() instanceof RemoteException) {
						throw new RemoteException(e.getCause().getMessage());
					} else {
						throw new Exception(e.getCause());
					}
				} else { // �����Ҫ�������������쳣,���������һ��
					System.out.println("�����һ�ε����쳣,�������� : " + e);
				}
		  }

		  return ret;
	  }
	  
	  public void myTest() throws UnknownHostException{
		  String[] rmiUrls = new String[]{"rmi://10.10.11.1:26060/LogonRMI"
				  , "rmi://10.10.11.1:26061/LogonRMI"};
		  String rmiUrl = rmiUrls[new Random().nextInt(2)];
		  
		  String traderId = "101000000024001";
		  String password = "123456";
		  long logonTime = new Date().getTime();
		  String logonIP = InetAddress.getLocalHost().getHostAddress().toString();

		  Trader trader = new Trader();
		  trader.setTraderID(traderId);
		  trader.setPassword(password);
		  trader.setLogonTime(logonTime);
		  trader.setLogonIP(logonIP);
		  
		  String methodName = null;
		  int retCode = -100;
		  boolean flag = false;
		  String sessionID = null;
		  try {
			  //������֤�û�ID������
			  methodName = "checkUser";
			  retCode = Integer.parseInt(call(rmiUrl, methodName, traderId, password).toString());
			  System.out.println("checkUser the retCode is " + retCode);
			  
			  Thread.sleep(3000);
			  //����logon����
			  methodName = "logon";
			  SessionContext sc =  (SessionContext) call(rmiUrl, methodName, trader);
			  Privilege privilege = sc.getPrivilege();
			  sessionID = privilege.getSessionID();
			  System.out.println("logon sessionID is " + sessionID);
			   
			 Thread.sleep(3000);
			 //���Խ���Ա��¼״̬
			 methodName = "getLogonStatus";
			 retCode = Integer.parseInt(call(rmiUrl, methodName, traderId, sessionID).toString());
			 System.out.println("the LogonStatus is " + retCode);
			 
			 Thread.sleep(3000);
			 //����isLogon����
			 methodName = "isLogon";
			 flag = Boolean.parseBoolean(call(rmiUrl, methodName, traderId, sessionID).toString());
			 System.out.println("isLogon is " + flag);
			 
			 Thread.sleep(3000);
			 //�����޸Ľ���Ա����
			 methodName ="changePassowrd";
			// retCode = Integer.parseInt(call(rmiUrl, methodName, traderId, "123qwe", "123456", logonIP).toString());
			 System.out.println("changePassowrd the retCode is " + retCode);
			 
			 Thread.sleep(3000);
			//����ǿ����ֹһ���Ự
			 methodName = "kickOnlineSession";
			 call(rmiUrl, methodName, sessionID);
			 System.out.println("kickOnlineSession is invoked");
		  } catch (Exception e) {
			  System.out.println(e);
		  }
	  }
	  
	  public static void main(String[] args) throws UnknownHostException {
		new LogonRMIImplTest().myTest();
	}
}
