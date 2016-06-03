package tpme.PMES.timebargain.server.dao;

import java.io.Serializable;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * ȡ��DAO��BEAN.
 * ��ʼ��BeanFactory ͨ�������ļ���beanId���factory�е�bean
 * <p><a href="DAOBeanFactory.java.html"><i>View Source</i></a></p>
 *
 * @version 1.0.0.4
 * @author <a href="mailto:zhoushp@tpme.com.cn">zhoushp</a>
 */
public class DAOBeanFactory implements Serializable {
	private static final Log log = LogFactory.getLog(DAOBeanFactory.class);

	private static final long serialVersionUID = 1L;

	/**
	 * spring context file path
	 */
	private static final String beanConfig = "PMES_timebargain.xml";

	private static BeanFactory factory;

	private static Properties props = getProps();

	/**
	 * ��ʼ��context����ͨ��beanConfigָ����context file
	 */
	public static void init() {
		log.debug("��ʼ��context:" + beanConfig);
		factory = new ClassPathXmlApplicationContext(beanConfig);
	}

	/**
	 * ͨ��beanId�õ�factory�е�beanʵ��
	 * 
	 * @param beanId
	 * @return Object
	 */
	public static Object getBean(String beanId) {
		Object obj = null;

		if (factory == null) {
			synchronized (DAOBeanFactory.class) {
				if (factory == null) {
					init();
				}
			}
		}
		if (factory != null) {
			obj = factory.getBean(beanId);
		}
			
		return obj;
	}

	/**
	 * ���BeanFactoryʵ��
	 * 
	 * @return the BeanFactory
	 */
	public static BeanFactory getBeanFactory() {
		if (factory == null) {
			synchronized (DAOBeanFactory.class) {
				if (factory == null) {
					init();
				}
			}
		}
		return factory;
	}

	/**
	 * ���ϵͳ�������Զ���
	 * 
	 * @return ���Զ���
	 */
	public static Properties getProps() {
		Properties conf = null;
		try {
			conf = (Properties) getBean("config");
		} catch (NoSuchBeanDefinitionException e) {
			log.error("û���ҵ�config�����֣�");
		}
		return conf;
	}

	/**
	 * �õ�ϵͳ��������
	 * 
	 * @param name
	 * @return �����ַ�ֵ
	 */
	public static String getConfig(String name) {
		if (props != null) {
			return (String) props.getProperty(name);
		} else {
			return null;
		}
	}

	public static void main(String[] args) {

	}
}
