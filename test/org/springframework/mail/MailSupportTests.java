/*
 * Copyright 2002-2004 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package org.springframework.mail;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.mail.cos.CosMailSenderImpl;

/**
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @since 10.09.2003
 */
public class MailSupportTests extends TestCase {

	public void testSimpleMessage() {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom("me@mail.org");
		message.setTo("you@mail.org");

		SimpleMailMessage messageCopy = new SimpleMailMessage(message);
		assertEquals("me@mail.org", messageCopy.getFrom());
		assertEquals("you@mail.org", messageCopy.getTo()[0]);

		message.setReplyTo("reply@mail.org");
		message.setCc(new String[] {"he@mail.org", "she@mail.org"});
		message.setBcc(new String[] {"us@mail.org", "them@mail.org"});
		Date sentDate = new Date();
		message.setSentDate(sentDate);
		message.setSubject("my subject");
		message.setText("my text");

		assertEquals("me@mail.org", message.getFrom());
		assertEquals("reply@mail.org", message.getReplyTo());
		assertEquals("you@mail.org", message.getTo()[0]);
		List ccs = Arrays.asList(message.getCc());
		assertTrue(ccs.contains("he@mail.org"));
		assertTrue(ccs.contains("she@mail.org"));
		List bccs = Arrays.asList(message.getBcc());
		assertTrue(bccs.contains("us@mail.org"));
		assertTrue(bccs.contains("them@mail.org"));
		assertEquals(sentDate, message.getSentDate());
		assertEquals("my subject", message.getSubject());
		assertEquals("my text", message.getText());

		messageCopy = new SimpleMailMessage(message);
		assertEquals("me@mail.org", messageCopy.getFrom());
		assertEquals("reply@mail.org", messageCopy.getReplyTo());
		assertEquals("you@mail.org", messageCopy.getTo()[0]);
		ccs = Arrays.asList(messageCopy.getCc());
		assertTrue(ccs.contains("he@mail.org"));
		assertTrue(ccs.contains("she@mail.org"));
		bccs = Arrays.asList(message.getBcc());
		assertTrue(bccs.contains("us@mail.org"));
		assertTrue(bccs.contains("them@mail.org"));
		assertEquals(sentDate, messageCopy.getSentDate());
		assertEquals("my subject", messageCopy.getSubject());
		assertEquals("my text", messageCopy.getText());
	}

	public void testCosMailSenderImplWithSimpleMessageAndBadHostName() throws MailException {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom("a@a.com");
		message.setTo("b@b.com");
		message.setSubject("test");
		message.setText("another test");

		CosMailSenderImpl sender = new CosMailSenderImpl();
		sender.setHost("hostxyzdoesnotexist");
		try {
			sender.send(message);
			fail("Should have thrown MailSendException");
		}
		catch (Exception ex) {
			assertTrue(ex instanceof MailSendException);
		}
	}

}
