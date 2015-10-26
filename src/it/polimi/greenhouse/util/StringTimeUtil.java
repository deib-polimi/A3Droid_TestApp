package it.polimi.greenhouse.util;

import java.util.Date;

public class StringTimeUtil {

	public static String createString(int size) {
		char[] c;
		
		switch(4){
		case 1:	c = new char[62484]; break;
		case 2:	c = new char[32000]; break;
		case 3:	c = new char[5017]; break;
		case 4:	c = new char[1812]; break;
		default: c = null;
		}
		
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
