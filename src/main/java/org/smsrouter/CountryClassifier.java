package org.smsrouter;

import org.apache.camel.CamelExchangeException;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public class CountryClassifier implements Processor {
	
	private Logger logger = LoggerFactory.getLogger(CountryClassifier.class);
	
	private PhoneNumberUtil pnu;
	private String defaultRegion;
	private String headerName;
	private String parsedHeaderName;
	private String countryHeaderName;
	
	public CountryClassifier(String defaultRegion, String headerName) {
		this.defaultRegion = defaultRegion;
		this.headerName = headerName;
		pnu = PhoneNumberUtil.getInstance();
		
		parsedHeaderName = headerName + "E164";
		countryHeaderName = headerName + "CountryISO2";
	}
	
	public String getParsedHeaderName() {
		return parsedHeaderName;
	}
	
	public String getCountryHeaderName() {
		return countryHeaderName;
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		Message message = exchange.getIn();
		String rawNumber = message.getHeader(headerName, String.class);
		
		if(rawNumber == null) {
			String error = "Missing header: " + headerName;
			logger.debug(error);
			throw new CamelExchangeException(error, exchange);
		}
		
		PhoneNumber phoneNumber = null;
		try {
			phoneNumber = pnu.parse(rawNumber, defaultRegion);
		} catch (NumberParseException ex) {
			String error = "Error while parsing: " + rawNumber;
			logger.debug(error);
			throw new CamelExchangeException(error, exchange, ex);
		}
		
		if (phoneNumber == null) {
			String error = "phoneNumber is null";
			logger.error(error);
			throw new CamelExchangeException(error, exchange);
		}
		
		message.setHeader(parsedHeaderName, pnu.format(phoneNumber, PhoneNumberFormat.E164));
		String countryIso2 = pnu.getRegionCodeForNumber(phoneNumber);
		if(countryIso2 != null) {
			message.setHeader(countryHeaderName, countryIso2);
		} else {
			logger.warn("Failed to find an ISO country code for number: {}", rawNumber);
		}
	}

}
