package it.polimi.greenhouse.util;

import java.util.Date;

public class StringTimeUtil {

	public static String createString(int bSize) {
		char[] c = new char[bSize];		
		for(int i = 0; i < c.length; i++)
			c[i] = '0';
		
		return new String(c);
	}
	
	public static long roundTripTime(String departureTimestamp, String arrivalTimestamp) {
		long i1 = 0, i2 = 0;		
		i1 = Long.parseLong(arrivalTimestamp);		
		i2 = Long.parseLong(departureTimestamp);
		return i1 - i2;
	}
	
	public static String getTimestamp() {
		return new Date().getTime() + "";
	}
	
}
