package test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TestTPCMaster {

	
	
	@Test
	public void testRegex() {
		String newString = "-4123434436234736@faASDsd:243234";
		Pattern slaveInfoPattern = Pattern.compile("(-?\\d+)@(.+):(-?\\d+)"); // TODO: something off? fix regex for long. "can start with minus sign"
		Matcher m = slaveInfoPattern.matcher(newString);
		
		assertTrue(m.matches());
	}
}
