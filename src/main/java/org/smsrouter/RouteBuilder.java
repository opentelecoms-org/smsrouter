package org.smsrouter;

import org.apache.camel.Predicate;
import org.apache.camel.PropertyInject;
import org.apache.camel.component.smpp.SmppException;
import org.apache.camel.spring.SpringRouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteBuilder extends SpringRouteBuilder {
	
	private Logger logger = LoggerFactory.getLogger(RouteBuilder.class);
	
	@PropertyInject(value = "local.country", defaultValue = "CH")
	private String localCountry;
	
	@PropertyInject(value = "smsc.country", defaultValue = "UK")
	private String smscCountry;
	
	@PropertyInject(value = "blacklist.countries", defaultValue = "CI;GF;GP;MQ;NC;PF;RE")
	private String blacklistCountries;
	
	@PropertyInject(value = "source-overrides", defaultValue = "US,CA:+16461234567")
	private String overrides;
	
	@PropertyInject(value = "throttle.timePeriodMillis", defaultValue = "1000")
	long throttleTimePeriodMillis;
	
	@PropertyInject(value = "throttle.maximumRequestsPerPeriod", defaultValue = "1")
	int throttleRequestsPerPeriod;
		
	@Override
	public void configure() throws Exception {
		
		/**
		 * Log some information about the configuration.
		 */
		logger.info("Parsing locally supplied numbers "
			+ "using context country: {}", localCountry);
		logger.info("Parsing SMSC supplied numbers using "
			+ "context country: {}", smscCountry);
		logger.info("Throttling allows {} request(s) per {}ms",
			throttleRequestsPerPeriod, throttleTimePeriodMillis);
		
		/**
		 * Create some Processor instances that will be used in the routes.
		 */
		CountryClassifier origin =
				new CountryClassifier(localCountry, "SMSOrigin");
		CountryClassifier destination =
				new CountryClassifier(localCountry, "SMSDestination");
		Predicate blacklistedDestination = new Blacklist(blacklistCountries,
				"SMSDestinationCountryISO2");
		SourceOverride sourceOverride = new SourceOverride(overrides,
				destination.getCountryHeaderName(), origin.getParsedHeaderName());
		SmppAddressing smppAddressing = new SmppAddressing(smscCountry,
				origin.getParsedHeaderName(), destination.getParsedHeaderName());
		
		/**
		 * Create some strings that will be used in the Camel routes
		 */
		String log = "log:org.smsrouter?level=INFO";
		String logWarn = "log:org.smsrouter?level=WARN";
		
		String smppUriTemplate =
				"smpp://{{smpp.username}}@{{smpp.host}}:{{smpp.port}}"
				+ "?password={{smpp.password}}"
				+ "&systemType={{smpp.system-type}}"
				+ "&enquireLinkTimer={{smpp.link-timer}}"
				+ "&typeOfNumber=1"
				+ "&numberingPlanIndicator=1";

		String smppUriProducer = smppUriTemplate + "&registeredDelivery=1";
		String smppUriConsumer = smppUriTemplate;
		
		String dlq = "activemq:smsrouter.outbox.failed";
		
		/**
		 * This Camel routes handles messages going out to the SMS world
		 */
		from("activemq:smsrouter.outbox")
			.errorHandler(deadLetterChannel(dlq))
			.onException(SmppException.class)
				.maximumRedeliveries(0)
				.end()
			.removeHeaders("CamelSmpp*")//In case it started as SMS elsewhere
			.process(origin)
			.process(destination)
			.choice()
				.when(blacklistedDestination)
					.to(dlq)
				.otherwise()
					.process(sourceOverride)
					.process(smppAddressing)
					.throttle(throttleRequestsPerPeriod)
						.timePeriodMillis(throttleTimePeriodMillis)
					.to(smppUriProducer)
					//.to("mock:foo")
					.setBody(simple("The SMSC accepted the message"
							+ " for ${header.CamelSmppDestAddr}"
							+ " and assigned SMPP ID: ${header.CamelSmppId}"))
					.to(log);
		
		/**
		 * This Camel route handles messages coming to us from the SMS world
		 */
		from(smppUriConsumer)
			.threads(1, 1)
			.choice()
				.when(simple("${header.CamelSmppMessageType} == 'DeliveryReceipt'"))
					.setBody(simple("Message delivery receipt"
							+ " for SMPP ID ${header.CamelSmppId}"))
					.to(log)
				.when(simple("${header.CamelSmppMessageType} == 'DeliverSm'"))
					.process(smppAddressing)
					.setHeader("SMSOrigin", header("SMSOriginE164"))
					.removeHeaders("Camel*")
					.to("activemq:smsrouter.inbox")
					.setBody(simple("Message from ${header.SMSOriginE164}"
							+ " to ${header.SMSDestinationE164}: ${body}"))
					.to(log)
				.otherwise()
					.setBody(simple("Unhandled event type: ${header.CamelSmppMessageType}"))
					.to(logWarn);	
	}

}
