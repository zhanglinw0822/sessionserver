package tpme.PMES.timebargain.server.util;

import java.util.*;
import java.text.*;
import java.sql.Timestamp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * 日期实用类
 * 
 * @author zhousp
 * 
 */
public class DateUtil {
	private static Log log = LogFactory.getLog(DateUtil.class);
	private static String defaultDatePattern = null;
	private static String timePattern = "HH:mm:ss";

	/**
	 * Return default datePattern (MM/dd/yyyy)
	 * 
	 * @return a string representing the date pattern on the UI
	 */
	public static synchronized String getDatePattern() {
		Locale locale = LocaleContextHolder.getLocale();
		try {
			defaultDatePattern = ResourceBundle.getBundle("RESOURCE", locale)
					.getString("date.format");
		} catch (MissingResourceException mse) {
			defaultDatePattern = "yyyy-MM-dd";
		}

		return defaultDatePattern;
	}

	/**
	 * This method attempts to convert an Oracle-formatted date in the form
	 * dd-MMM-yyyy to mm/dd/yyyy.
	 * 
	 * @param aDate
	 *            date from database as a string
	 * @return formatted string for the ui
	 */
	public static final String getDate(Date aDate) {
		SimpleDateFormat df = null;
		String returnValue = "";

		if (aDate != null) {
			df = new SimpleDateFormat(getDatePattern());
			returnValue = df.format(aDate);
		}

		return (returnValue);
	}

	/**
	 * This method generates a string representation of a date/time in the
	 * format you specify on input
	 * 
	 * @param aMask
	 *            the date pattern the string is in
	 * @param strDate
	 *            a string representation of a date
	 * @return a converted Date object
	 * @see java.text.SimpleDateFormat
	 * @throws ParseException
	 */
	public static final Date convertStringToDate(String aMask, String strDate)
			throws ParseException {
		SimpleDateFormat df = null;
		Date date = null;
		df = new SimpleDateFormat(aMask);

		if (log.isDebugEnabled()) {
			log.debug("converting '" + strDate + "' to date with mask '"
					+ aMask + "'");
		}

		try {
			date = df.parse(strDate);
		} catch (ParseException pe) {
			// log.error("ParseException: " + pe);
			throw new ParseException(pe.getMessage(), pe.getErrorOffset());
		}

		return (date);
	}

	/**
	 * This method returns the current date time in the format: MM/dd/yyyy HH:MM
	 * a
	 * 
	 * @param theTime
	 *            the current time
	 * @return the current date/time
	 */
	public static String getTimeNow(Date theTime) {
		return getDateTime(timePattern, theTime);
	}

	/**
	 * This method returns the current date in the format: MM/dd/yyyy
	 * 
	 * @return the current date
	 * @throws ParseException
	 */
	public static Calendar getToday() throws ParseException {
		Date today = new Date();
		SimpleDateFormat df = new SimpleDateFormat(getDatePattern());

		// This seems like quite a hack (date -> string -> date),
		// but it works ;-)
		String todayAsString = df.format(today);
		Calendar cal = new GregorianCalendar();
		cal.setTime(convertStringToDate(todayAsString));

		return cal;
	}

	/**
	 * This method generates a string representation of a date's date/time in
	 * the format you specify on input
	 * 
	 * @param aMask
	 *            the date pattern the string is in
	 * @param aDate
	 *            a date object
	 * @return a formatted string representation of the date
	 * 
	 * @see java.text.SimpleDateFormat
	 */
	public static final String getDateTime(String aMask, Date aDate) {
		SimpleDateFormat df = null;
		String returnValue = "";

		if (aDate == null) {
			log.error("aDate is null!");
		} else {
			df = new SimpleDateFormat(aMask);
			returnValue = df.format(aDate);
		}

		return (returnValue);
	}

	/**
	 * This method generates a string representation of a date based on the
	 * System Property 'dateFormat' in the format you specify on input
	 * 
	 * @param aDate
	 *            A date to convert
	 * @return a string representation of the date
	 */
	public static final String convertDateToString(Date aDate) {
		return getDateTime(getDatePattern(), aDate);
	}

	/**
	 * This method converts a String to a date using the datePattern
	 * 
	 * @param strDate
	 *            the date to convert (in format MM/dd/yyyy)
	 * @return a date object
	 * 
	 * @throws ParseException
	 */
	public static Date convertStringToDate(String strDate)
			throws ParseException {
		Date aDate = null;

		try {
			if (log.isDebugEnabled()) {
				log.debug("converting date with pattern: " + getDatePattern());
			}

			aDate = convertStringToDate(getDatePattern(), strDate);
		} catch (ParseException pe) {
			log.error("Could not convert '" + strDate
					+ "' to a date, throwing exception");
			pe.printStackTrace();
			throw new ParseException(pe.getMessage(), pe.getErrorOffset());

		}

		return aDate;
	}

	// --------------------------------------------------------------------------

	/**
	 * 转换为日期
	 * 
	 * @param year
	 * @param mon
	 * @param day
	 * @return
	 */
	public static Date Str2Date(String year, String mon, String day) {
		Calendar cal = Calendar.getInstance();
		cal.set(Integer.parseInt(year), Integer.parseInt(mon) - 1, Integer
				.parseInt(day), 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
		// return new Date(Integer.parseInt(year) - 1900, Integer.parseInt(mon)
		// - 1, Integer.parseInt(day));
	}

	public static Date Str2Date(String str) {
		if (str != null && !str.trim().equals("")) {
			if (str.indexOf("-") != -1) {
				String[] arrDate = str.split("-");
				return Str2Date(arrDate[0], arrDate[1], arrDate[2]);
			} else {
				return Str2Date(str.substring(0, 4), str.substring(4, 6), str
						.substring(6));
			}
		} else {
			return new Date();
		}
	}

	/**
	 * 对于给定的日期表达式，返回指定数目的月数以前或以后的日期。
	 * 
	 * @param date
	 * @param mon
	 * @return
	 */
	public static Date GoMonth(Date date, int mon) {
		// Timestamp st = new Timestamp(date.getTime());
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.MONTH, mon);
		return cal.getTime();
	}

	/**
	 * 对于给定的日期表达式，返回指定数目的天数以前或以后的日期。
	 * 
	 * @param date
	 * @param day
	 */
	public static Date GoDate(Date date, int day) {
		long time = date.getTime();
		time += (long) day * 24 * 60 * 60 * 1000;
		return new Date(time);
	}

	/**
	 * 获取日期
	 * 
	 * @return 系统日期,输出格式如：2002-12-16
	 */
	public static String getCurDate() {
		Calendar c = Calendar.getInstance();
		Timestamp ts = new Timestamp(c.getTime().getTime());
		String curDate = String.valueOf(ts);
		curDate = curDate.substring(0, curDate.indexOf(" "));

		return curDate;
	}

	/**
	 * 获取时间
	 * 
	 * @return 系统时间,输出格式如：10:33:20
	 */
	public static String getCurTime() {
		Calendar c = Calendar.getInstance();

		Timestamp ts = new Timestamp(c.getTime().getTime());
		String curTime = String.valueOf(ts);
		curTime = curTime.substring(11, curTime.indexOf("."));

		return curTime;
	}

	/**
	 * 获取日期与时间
	 * 
	 * @return 系统日期与时间,输出格式如：2002-12-16 10:33:20
	 */
	public static String getCurDateTime() {
		Calendar c = Calendar.getInstance();
		Timestamp ts = new Timestamp(c.getTime().getTime());
		String curDateTime = String.valueOf(ts);
		curDateTime = curDateTime.substring(0, curDateTime.indexOf("."));
		return curDateTime;
	}

	/**
	 * 格式化输出系统日期
	 * 
	 * @param date
	 *            系统日期,格式如：2002-12-16
	 * @return 格式化的系统日期,输出格式如：20021216[yyyyMMdd]
	 */
	public static String formatDate(Date date, String pattern) {
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		return sdf.format(date);
	}

	public static String formatDate(String date, String pattern)
			throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		Date dt = sdf.parse(date);
		return sdf.format(date);
	}

	/*
	 * public static String formatDate(String date) { String
	 * cdate=date.substring(0,4)+date.substring(5,7)+date.substring(8,10);
	 * return cdate; } public static String formatDate(Date date) { Timestamp ts
	 * = new Timestamp(date.getTime()); String DateTime = String.valueOf(ts);
	 * DateTime = DateTime.substring(0,DateTime.indexOf("."));
	 * 
	 * return formatDate(DateTime); }
	 */

	/**
	 * 格式化输出系统时间
	 * 
	 * @param time
	 *            系统时间,格式如：10:33:20
	 * @return 格式化的系统时间,输出格式如：103320
	 */
	public static String formatTime(String time) {
		String ctime = time.substring(0, 2) + time.substring(3, 5)
				+ time.substring(6, 8);
		return ctime;
	}

	/**
	 * 获取系统年份
	 * 
	 * @return 系统年份,输出格式如：2003
	 */
	public static int getYear() {
		String curDate = getCurDate();
		int year = Integer.parseInt(curDate.substring(0, 4));
		return year;
	}

	/**
	 * 获取系统月份
	 * 
	 * @return 系统月份,输出格式如：10
	 */
	public static int getMon() {
		String curDate = getCurDate();
		int mon = Integer.parseInt(curDate.substring(5, 7));

		return mon;
	}

	/**
	 * 获取系统日期
	 * 
	 * @return 系统日期,输出格式如：20
	 */
	public static int getDd() {
		String curDate = getCurDate();
		int dd = Integer.parseInt(curDate.substring(8, 10));

		return dd;
	}

	/**
	 * 判断某日是星期几
	 * 
	 * @param date
	 * @return 星期日：1；星期一：2；。。。；星期六：7；
	 */
	public static int getWeekDay(Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		return c.get(Calendar.DAY_OF_WEEK);
	}

	/**
	 * 对于给定的日期表达式，返回指定数目的秒数以前或以后的日期。
	 * 
	 * @param date
	 * @param second
	 * @return Timestamp
	 */
	public static Timestamp GoSecond(Date date, int second) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.SECOND, second);
		return new Timestamp(c.getTime().getTime());
	}

	public static void main(String[] args) {
		try {
			Date date = new Date();
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			System.out.println(cal.get(Calendar.YEAR));
			System.out.println(cal.get(Calendar.MONTH));
			System.out.println(cal.get(Calendar.DATE));
			System.out.println(cal.get(Calendar.HOUR));
			System.out.println(cal.get(Calendar.MINUTE));
			System.out.println(cal.get(Calendar.SECOND));
			System.out.println(cal.get(Calendar.DAY_OF_WEEK));
			String s = "2010-03-11 14:26:20.999999999";
			java.sql.Timestamp ts = java.sql.Timestamp.valueOf(s);
			System.out.println("ts:" + ts);
			Date d = convertStringToDate("yyyy-MM-dd HH:mm:ss",
					"2010-03-11 14:26:59");
			System.out.println("ts:" + GoSecond(d, -60));
		} catch (Exception e) {
			System.out.println(e);
		}
	}

}
