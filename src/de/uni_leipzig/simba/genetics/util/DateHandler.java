package de.uni_leipzig.simba.genetics.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

/**
 * Class to Handle Dates. From parsing from Strings to comparison.
 * @author Klaus Lyko
 *
 */
public class DateHandler {
	
	/**
	 * Computes the similarity of the two given Dates as 1 divided by their difference in seconds.
	 * @param d1
	 * @param d2
	 * @return
	 */
	public static double getDateSim(Date d1, Date d2) {
		
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal1.setTime(d1); cal2.setTime(d2);
		long diff = Math.abs(d1.getTime()-d2.getTime());// in seconds
		long days = Math.round( (double)diff / (24. * 60.*60.*1000.) );
		System.out.println("Diff in days ("+d1+" : "+d2+")= "+days);
		long months = Math.round( (double)diff / (31.*24. * 60.*60.*1000.) );
		System.out.println("\t..diff in months  "+months);
		if(diff>0)
			return 1d/diff;
		else
			return 1d;
	}
	
	/**
	 * Method to parse Dates from Strings. Accepts multiple formats and trys to guess the best one.
	 * @param toParse
	 * @return A Parsed Date instance.
	 */
	public static Date getDate(String toParse) {
		DateFormat format;
		Date date = null;
		// date is only a year
		String regex = "^(\\d?\\d?\\d\\d)$";
		if(toParse.matches(regex)) {
			format = new SimpleDateFormat("yyyy");
			try {
				date = (Date) format.parse(toParse);
				return date;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}		
		// date in format yyyy-mm-dd
		regex = "^(\\d?\\d\\d\\d)-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])$";
		if(toParse.matches(regex)) {
			format = new SimpleDateFormat("yyyy-MM-dd");
			try {
				date = (Date) format.parse(toParse);
				return date;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		//date in format yyyy/mm/dd
		regex = "^(\\d?\\d\\d\\d)/(0?[1-9]|1[012])/(0?[1-9]|[12][0-9]|3[01])$";
		if(toParse.matches(regex)) {
			format = new SimpleDateFormat("yyyy/MM/dd");
			try {
				date = (Date) format.parse(toParse);
				return date;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		//date in format dd/mm/yyyy
		regex = "^(0?[1-9]|[12][0-9]|3[01])/(0?[1-9]|1[012])/(\\d?\\d?\\d\\d)$";
		if(toParse.matches(regex)) {
			format = new SimpleDateFormat("dd/MM/yyyy");
			try {
				date = (Date) format.parse(toParse);
				return date;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		// date in format dd.mm.yyyy
		regex = "^(0?[1-9]|[12][0-9]|3[01])\\.(0?[1-9]|1[012])\\.\\d?\\d?\\d\\d$";
		if(toParse.matches(regex)) {
			format = new SimpleDateFormat("dd.MM.yyyy");
			try {
				date = (Date) format.parse(toParse);
				return date;
			} catch (ParseException e) {
				e.printStackTrace();
			}			
		}
		// date in format something like dd/Jan/yyyy
		regex = "^(0?[1-9]|[12][0-9]|3[01])[/.-][a-zA-Z]{3}[/.-](\\d?\\d\\d\\d)$";
		if(toParse.matches(regex)) {
			format = new SimpleDateFormat("dd/MMM/yyyy");
			try {
				date = (Date) format.parse(toParse);
				return date;
			} catch (ParseException e) {}
			format = new SimpleDateFormat("dd-MMM-yyyy");
			try {
				date = (Date) format.parse(toParse);
				return date;
			} catch (ParseException e) {}
			format = new SimpleDateFormat("dd.MMM.yyyy");
			try {
				date = (Date) format.parse(toParse);
				return date;
			} catch (ParseException e) {}	
		}
		// date in format something like dd/March/yyyy
			regex = "^(0?[1-9]|[12][0-9]|3[01])[/.-][a-zA-Z]{4,10}[/.-](\\d?\\d\\d\\d)$";
			if(toParse.matches(regex)) {
					format = new SimpleDateFormat("dd/MMMM/yyyy");
					try {
						date = (Date) format.parse(toParse);
						return date;
					} catch (ParseException e) {}
					format = new SimpleDateFormat("dd-MMMM-yyyy");
					try {
						date = (Date) format.parse(toParse);
						return date;
					} catch (ParseException e) {}
					format = new SimpleDateFormat("dd.MMMM.yyyy");
					try {
						date = (Date) format.parse(toParse);
						return date;
					} catch (ParseException e) {}	
				}
		return date;
	}
	
	public static void main(String args[]) {
		
//		Date d = getDate("1.1.2012"); 
//		String[] dates = {"1.1.2012", "2.1.2012", "3.1.2012","15.6.2012", "1.1.2011","1.1.2013"};
//		for(String s : dates) {
//			Date d2 = getDate(s);
//			System.out.println("\tSim('1.1.2012' : '"+s+"') = "+getDateSim(d, d2));
//		}
		System.out.println();
		System.out.println("\nSim('2011' : '1.1.2011') = "+getDateSim(getDate("01.01.2011"), getDate("1911")));
	}
	

	@Test
	public void testParsing() {
		String[] dates = {"01.01.1950", "2011/1/1", "31/Dec/2009", "1.1.1980", "1/1/1980", "15.1.75", "1999", "2011", "11"};
		for(String aDate : dates) {
			Date d = getDate(aDate);
			System.out.println("Parsed '"+aDate+"' into "+d);
			Assert.assertTrue(d !=  null); 
		}
	}
	
	
	@Test
	public void RegexTest() {
		String regex = "^(\\d?\\d\\d\\d)/(0?[1-9]|1[012])/(0?[1-9]|[12][0-9]|3[01])$";
		Assert.assertTrue("Date: matched. ", Pattern.matches(regex, "1790/11/25"));
		Assert.assertTrue("Date: matched.", Pattern.matches(regex, "2011/1/1"));
		Assert.assertFalse("Date : not matched.", Pattern.matches(regex, "2011/13/1"));
	}
	
	
	@Test
	public void RegexTest2() {
		String regex = "^(0?[1-9]|[12][0-9]|3[01])\\.(0?[1-9]|1[012])\\.\\d?\\d?\\d\\d$";
		Assert.assertTrue("Date: matched. ", Pattern.matches(regex, "01.01.1950"));
		Assert.assertTrue("Date: matched.", Pattern.matches(regex, "31.12.2009"));
		Assert.assertFalse("Date : not matched.", Pattern.matches(regex, "15.13.1855"));
	}
	
	@Test
	public void RegexTest3() {
		String regex = "^(0?[1-9]|[12][0-9]|3[01])[/.-][a-zA-Z]{3}[/.-](\\d?\\d\\d\\d)$";
		Assert.assertTrue("Date: matched. ", Pattern.matches(regex, "01-Jan-1950"));
		Assert.assertTrue("Date: matched.", Pattern.matches(regex, "31/Dec/2009"));
		Assert.assertFalse("Date : not matched.", Pattern.matches(regex, "15/13/1855"));
	}
	
	@Test
	public void RegexTest4() {
		String regex = "^(0?[1-9]|[12][0-9]|3[01])[/.-][a-zA-Z]{4,10}[/.-](\\d?\\d\\d\\d)$";
		Assert.assertTrue("Date: matched. ", Pattern.matches(regex, "01-January-1950"));
		Assert.assertTrue("Date: matched.", Pattern.matches(regex, "31/December/2009"));
		Assert.assertFalse("Date : not matched.", Pattern.matches(regex, "15/13/1855"));
	}
	
	
}

