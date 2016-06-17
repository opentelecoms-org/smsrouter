
# Introduction

[Project web site](http://smsrouter.org)

There are a range of companies offering Internet-based services
for sending and receiving SMS messages using SMPP.

SMPP is currently the only vendor-neutral standard on the Internet
that gives application developers a close approximation of the
wire format of real SMS messages.

In most organizations there is often more than one team or application
requiring connectivity to the SMS / SMPP network.

The SMS Router project provides a convenient solution.  Multiple
users on the corporate network can send SMS over message queues,
using protocols like JMS (from Java) or STOMP (from Python and Perl).

SMS Router aggregates all the SMS traffic and sends it to the provider.
Incoming SMS messages can be routed to individual applications by
customizing the Camel routes.

# DISCLAIMER

SMS, SMPP and the mobile/cellular telephone network are not designed
or warranted for the secure transmission of login credentials, token codes
or passwords.

Telecommunications industry experts and the engineers who have
developed the SMS Router, Camel SMPP and jSMPP discourage the use
of SMS for user login authentication or two-factor authentication purposes
and accept no liability for any problems that arise.

We wish to draw your attention to two specific risks with SMS
authentication:
- interception of messages: messages are not protected from interception
  by employees of the SMPP gateway, the mobile telephone network, third parties
  with access to the SS7 network, the vendor of the mobile handset and any
  of the other apps that a user has installed on their phone.  There are
  potentially hundreds of thousands of people who can access SMS messages
  through one of the weaknesses in this list.
- non-delivery: there are many situations where a user may not be able
  to receive messages and login to your service.  Consider the situations
  when users are roaming (SMS delivery can be delayed by hours), when a
  user replaces their SIM card during an extended period of travel,
  when network outages occur, when they attempt to port the number to
  a different network and it fails, when their mobile device has a flat battery
  or is being repaired.  In these cases users may experience inconvenience
  and potentially financial losses if unable to complete transactions.

It is suggested that public key solutions based on smartcards or
purpose-built security tokens, compliant with industry standards
are able to address both of these issues comprehensively.

The [Initiative for Open Authentication](https://openauthentication.org/)
provides leadership in this area, including the vendor-neutral and
cost-effective HOTP and TOTP tokens that are becoming increasingly common.

# Features

- Vendor-neutral: don't waste time developing for a vendor's
  proprietary REST API as every REST API is different and your code
  has to be changed every time the vendor makes a substantial update
  to their API.
- Works behind firewalls and NAT: many REST APIs expect to make callbacks
  to your web server, increasing the complexity for organizations
  with strict firewall management policies.  SMPP doesn't have this
  limitation.
- Converts locally formatted phone numbers to E.164 for international
  delivery.
- Throttles delivery of messages to the gateway to adhere to
  network policies and minimize the risk of excessive expenditure.
- Blacklist delivery to countries with high SMS charges.
- Override sending numbers on a per-country basis where required for
  corporate or network policies.
- Configured with a simple properties file.
- Places undeliverable messages on a dead letter queue.
- Incoming messages delivered to an inbox queue.
- Requests and logs delivery receipts.
- Automatically handles message splitting.
- Automatic selection of appropriate SMPP character encoding.
- Logging with log4j2 supports SysLog and many other logging mechanisms.
- Works with a range of JMS providers.
- Sample Perl script for submitting SMS messages over STOMP with ActiveMQ.

# Limitations

## Potential improvements to this code

- Some of the properties from the properties file are ignored and
  hardcoded into the RouteBuilder.java class due to Camel bug CAMEL-8125.
  The bug has been fixed in more recent versions of Camel and the
  use of property injection should be tested again.
- The logging code needs to be reviewed to distinguish system-wide
  errors (such as a failure to bind to the SMPP gateway) from
  message-specific errors (such as a missing destination).  System-wide
  errors are those that should be reported to the sysadmin, while
  message-specific errors should be reported to the owner of the
  application who is sending the messages.
- The SMPP component doesn't currently support transactions.  It would
  be desirable to investigate the interaction of JMS transactions with
  the successful transfer of a message to the SMPP gateway.
- Adapting the code to work with arbitrary camel-jms queue URIs,
  it is currently hard-coded for the "activemq:" URI.
- Passing the SMPP tracking ID and delivery receipt messages back
  to the sending application through the queue.
- Routing inbound messages to different queues based on the
  destination number in the message header.
- Exceptions are not currently classified or logged, messages are simply
  sent to the dead letter queue.
- In many cases, nested exceptions need to be examined.
- Messages and delivery receipts should be logged to a CSV file or table
  for audit purposes.
- SMS alphabet can't be specified on a per-message basis, the default
  alphabet-selection logic in the camel-smpp component is used.  It would
  be desirable to allow the alphabet to be specified with a header
  or provide a more advanced method for automatically selecting a
  compatible SMS alphabet for any given UTF-8 String.  For best results,
  it is recommended to only send message strings that contain symbols
  from the GSM 03.38 alphabet / character set.
- The type of number and numbering plan indicator values are hard-coded
  to use numbers in international (E.164) format.  Customization is
  required to support alphanumeric sender names, short codes and destination
  numbers in local / national format.  Such customization would need to be
  careful to ensure that alphabetic sender names are only used when
  sending to countries where they are permitted.
- Detect when the message is going to a country / network that doesn't
  support message splitting and throw an exception if the message
  would be split.
- Enforce maximum hourly and/or daily message limits for individual
  destination numbers to ensure users are not accidentally spammed.
  Accidentally spamming an individual destination number can result
  in immediate suspension or termination by the SMPP provider.

## SMS / SMPP limitations

- SMS was not developed as a secure communications system.  While messages
  are encrypted during the transmission from a mobile base station to a
  mobile handset, they are not encrypted or authenticated in any other
  part of the network.  SMS should not be relied upon for secure
  authentication.
- SMS can be intercepted and monitored by any app the user has on their
  smart phone.
- SMS is not real-time, when messages are sent internationally or to
  users who are roaming there are often delays of minutes and even hours.
- Some networks / countries never send delivery receipts.
- Some networks / countries send fake delivery receipts for all messages,
  even when the destination number is invalid or the phone is off.
- Some networks / countries silently drop all messages from automated
  systems unless the user has opted-in to receive automated messages
  from all possible senders.  Many users are either not aware of this
  or they are reluctant to opt-in due to the increasing burden of
  SMS marketing.  India is one example of a country with an opt-in system.
- Some countries have a policy of not accepting split messages, only the
  first part of such messages may be delivered or sometimes nothing is
  delivered at all.

# Getting started

- Review the documentation in the sms-router.properties file.
- Set up a message broker.  Apache ActiveMQ is suggested but
  any message broker with JMS support should work.
- Set up an SMPP account (see details below for provider-specific notes)
- Configure the properties file with the necessary values for your
  chose SMPP gateway.
- Run it from the command line or with the Java Service Wrapper or
  another script of your choosing.

# Support

Please ask questions on the [Apache Camel Users mailing list](http://camel.apache.org/mailing-lists.html)

Commercial support is available by contacting the developers
at support@smsrouter.org

# SMPP providers

## Nexmo

- After creating a free Nexmo account, you need to contact Nexmo
  support and ask them to enable SMPP access on your new account.
  You can then use the free trial credit for SMPP.
- Make sure you check the default character set in the Nexmo portal.
- Read about how Nexmo uses the SMPP System Type parameter in the
  properties file and configure it for inbound numbers in the SMPP
  portal.
- Review the number of concurrent transmitter and receiver binds
  supported on your Nexmo account.

# Copyright and license

Copyright (C) 2012-2016, Daniel Pocock http://danielpocock.com

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

