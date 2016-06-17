package org.smsrouter;

import java.util.HashSet;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Blacklist implements Predicate {
	
	private static final String SEP = ";";
	
	private Logger logger = LoggerFactory.getLogger(Blacklist.class);
	
	private Set<String> tokens;
	private String headerName;

	public Blacklist(String blacklistCountries, String headerName) {
		tokens = new HashSet<String>();
		for(String s : blacklistCountries.split(SEP)) {
			tokens.add(s.toLowerCase());
		}
		this.headerName = headerName;
	}

	@Override
	public boolean matches(Exchange exchange) {
		Message message = exchange.getIn();
		if(!message.getHeaders().containsKey(headerName)) {
			return false;
		}
		String value = message.getHeader(headerName, String.class).toLowerCase();
		if(tokens.contains(value)) {
			logger.info("header '{}' value '{}' is blacklisted", headerName, value);
			return true;
		}
		return false;
	}

}
