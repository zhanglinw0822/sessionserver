import tpme.PMES.timebargain.server.util.EncryptUtil;


public class test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		EncryptUtil e = new EncryptUtil();
		try {
			byte[] result = e.decryptStr("+QUb0JMy0Ha/5CtjcgkvGCHGFyYG+cN3vo72lsFFIeo=","18c1609d0b404bac9848f6e7f7382750");
			System.out.println(result.length);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
