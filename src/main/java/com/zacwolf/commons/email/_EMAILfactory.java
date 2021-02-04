/* com.zacwolf.commons.email._EMAILfactory.java
 *
 * Factory for managing Email specific operations
 *
* Copyright (C) 2021 Zac Morris

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.zacwolf.commons.email;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


/**
 * @author Zac Morris <zac@zacwolf.com>
 * @version 1.3
 * @since Java1.8
 */
public final class _EMAILfactory {
final	static	public		int				BREAKDOWNDISTRIBUTION	=	200;
final	static	public		ExecutorService	THREADPOOL				=	_THREADfactory.getThreadPool("EMAILfactory", 10);
final			private		Session			mailSession;
	static {
	// add handlers for main MIME types
	final MailcapCommandMap	mcap = new MailcapCommandMap();
						mcap.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
						mcap.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
						mcap.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
						mcap.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed; x-java-fallback-entry=true");
						mcap.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
						CommandMap.setDefaultCommandMap(mcap);
	}

	public _EMAILfactory(final String smtphost, final String username, final String password){
final	Properties 		eprops				=	new java.util.Properties();
						eprops.put("mail.transport.protocol", "smtp");
						eprops.put("mail.smtp.host", smtphost);
final	Authenticator	auth;
		if(username==null || password==null) {
			auth				=	null;
		} else {
			auth				=	new Authenticator(){
												@Override
												public PasswordAuthentication getPasswordAuthentication() {
													return new PasswordAuthentication(username, password);
												}
											};
		}
						mailSession	=	Session.getDefaultInstance(eprops, auth);
						mailSession.setDebug(false);
	}

	void sendEmailMessage(final Email email) throws MessagingException {
		sendEmailMessage(email,
						 email.getDistribution().getFROM(),
						 email.getDistribution().getTO(),
						 email.getDistribution().getCC(),
						 email.getDistribution().getBCC()
						);
	}

	private void sendEmailMessage(final Email email,
								  final Address from,
								  final Address[] addressTO,
								  final Address[] addressCC,
								  final Address[] addressBCC
								 ) throws MessagingException {

		if (addressTO.length>BREAKDOWNDISTRIBUTION){
	int		index						=	0;
	int		num							=	BREAKDOWNDISTRIBUTION;
			while (num==BREAKDOWNDISTRIBUTION){
				if(addressTO.length-index<BREAKDOWNDISTRIBUTION) {
					num					=	addressTO.length-index;
				}
	final Address[]		newTO				=	new InternetAddress[num];
					System.arraycopy(addressTO,index,newTO,0,num);
					sendEmailMessage(email, from, newTO, addressCC, addressBCC);
					index				+=	BREAKDOWNDISTRIBUTION;
			}
		}
		if (addressCC!=null && addressCC.length>BREAKDOWNDISTRIBUTION){
	int				index				=	0;
	int				num					=	BREAKDOWNDISTRIBUTION;
			while (num==BREAKDOWNDISTRIBUTION){
				if(addressCC.length-index<BREAKDOWNDISTRIBUTION) {
					num					=	addressCC.length-index;
				}
	final Address[]		newCC				=	new InternetAddress[num];
					System.arraycopy(addressCC,index,newCC,0,num);
					sendEmailMessage(email,from, addressTO, newCC, addressBCC);
					index				+=	BREAKDOWNDISTRIBUTION;
			}
		}
		if (addressBCC!=null && addressBCC.length>BREAKDOWNDISTRIBUTION){
	int		index						=	0;
	int		num							=	BREAKDOWNDISTRIBUTION;
			while (num==BREAKDOWNDISTRIBUTION){
				if(addressBCC.length-index<BREAKDOWNDISTRIBUTION) {
					num					=	addressBCC.length-index;
				}
	final Address[]		newBCC				=	new InternetAddress[num];
					System.arraycopy(addressBCC,index,newBCC,0,num);
					sendEmailMessage(email,from, addressTO, addressCC, newBCC);
					index				+=	BREAKDOWNDISTRIBUTION;
			}
		}
		// Here we're only dealing with an object with less
		// than {BREAKDOWNDISTRIBUTION} of each type of recipient
		if (addressTO!=null && addressTO.length<=BREAKDOWNDISTRIBUTION
			&& (addressCC==null || addressCC.length<=BREAKDOWNDISTRIBUTION)
			&& (addressBCC==null || addressBCC.length<=BREAKDOWNDISTRIBUTION)){
			THREADPOOL.submit(new Runnable(){
				@Override
				public void run() {
					try{
final	MimeMessage			msg		=	new MimeMessage(mailSession);
							msg.setFrom(from);
							msg.setRecipients(Message.RecipientType.TO, addressTO);
							//Here we just double check that users don't receive duplicate emails
final	Set<Address>		temp	=	new HashSet<Address>();
						for(final Address addr:addressCC) {
							if (!email.getSendLog().alreadySentTo(addr.toString())) {
								temp.add(addr);
							}
						}
						if (temp.size()>0) {
							msg.setRecipients(Message.RecipientType.CC, temp.toArray(new InternetAddress[0]));
						}
							temp.clear();
						for(final Address addr:addressBCC) {
							if (!email.getSendLog().alreadySentTo(addr.toString())) {
								temp.add(addr);
							}
						}
						if (temp.size()>0) {
							msg.setRecipients(Message.RecipientType.BCC, temp.toArray(new InternetAddress[0]));
						}
							temp.clear();

							msg.setDescription(email.getName(), "utf-8");
							msg.setSubject(email.getSubject(), "utf-8");
							msg.setContent(email.getAsMultipart());

						send(msg,email.getSendLog());
					} catch (final Exception e){
						email.getSendLog().logError(e);
					}
				}

			});
		}
	}

	private void send(final MimeMessage mymsg, final EmailSendLog sendlog){
	String					error					=	"";
		try{
//			System.out.println(mymsg.getRecipients(Message.RecipientType.BCC));
			try {
				Transport.send(mymsg);
			} catch (final SendFailedException sfex) {
final Address[] 				invalid 					=	sfex.getInvalidAddresses();
final Address[]				validUnsent 				=	sfex.getValidUnsentAddresses();
final Address[]				validSent					=	sfex.getValidSentAddresses();
				if (validUnsent != null) {
ArrayList<Address>		toAddresses					=	new ArrayList<Address>();
ArrayList<Address>		ccAddresses					=	new ArrayList<Address>();
ArrayList<Address>		bccAddresses				=	new ArrayList<Address>();
					try {
						if (mymsg.getRecipients(Message.RecipientType.TO) != null) {
							toAddresses 			=	new ArrayList<Address>(Arrays.asList(mymsg.getRecipients(Message.RecipientType.TO)));
						}
						if (mymsg.getRecipients(Message.RecipientType.CC) != null) {
							ccAddresses 			=	new ArrayList<Address>(Arrays.asList(mymsg.getRecipients(Message.RecipientType.CC)));
						}
						if (mymsg.getRecipients(Message.RecipientType.BCC) != null) {
							bccAddresses 			=	new ArrayList<Address>(Arrays.asList(mymsg.getRecipients(Message.RecipientType.BCC)));
						}
					} catch (final Exception ex2) {
						throw new MessagingException("_EMAILfactory.sendEmailMessage() ERR: "+ex2+" MSG:"+ex2.getMessage());
					}
					// remove the invalid addresses
					if (invalid != null) {
						for (final Address element : invalid) {
							error					+=	"Invalid email address for address:" + element + "\n";
							ccAddresses.remove(element);
							bccAddresses.remove(element);
						}
					}
					// remove the addresses the EMAIL *WAS* sent to
					if (validSent != null) {
						for (final Address element : validSent) {
							ccAddresses.remove(element);
							bccAddresses.remove(element);
						}
					}
					try {
						// set the message recipients
						mymsg.setRecipients(Message.RecipientType.TO, toAddresses.toArray(new InternetAddress[0]));
						mymsg.setRecipients(Message.RecipientType.CC, ccAddresses.toArray(new InternetAddress[0]));
						mymsg.setRecipients(Message.RecipientType.BCC, bccAddresses.toArray(new InternetAddress[0]));
						Transport.send(mymsg);
					} catch (final MessagingException ex3) {
						throw new MessagingException("_EMAILfactory.send(subject="+mymsg.getSubject()+") ERR: "+ex3+" MSG:"+ex3.getMessage());
					}
				}
			}
		} catch (final Exception e){
				e.printStackTrace();
		} finally {
			try{
				if (sendlog!=null){
					if (error.length()>0) {
						sendlog.logError(error);
					}
					for (final Address addr:mymsg.getAllRecipients()) {
						sendlog.logSentTo(addr.toString());
					}
				}
			} catch (final Exception e){
				e.printStackTrace();
				//If there was a problem generating the log, then dump stack and move on
			}
		}
	}



}


/**
 * Dump the message to be sent to allow easy debugging. This method
 * must not throw an exception.
 *
 * @param mimeMessage the MimeMessage to dump
 * @param type the type of action we are currently executing

@Deprecated
public final static void dump(MimeMessage mimeMessage, String refid){
	if( mimeMessage == null )
		return;
	if (refid==null)
		refid					=	TimeUtils.getTimestampString();
String				filename	=	"EMAIL_DUMP_" + refid;
File				dumpFile;
FileOutputStream 	fos 		=	null;
	try{
		//If running via app server create the file as a context temp file otherwise just create it in the app root [when running from main()]
		if (factory.TEMP_DIR!=null)
			dumpFile			=	File.createTempFile(filename,".eml",new File(factory.TEMP_DIR));
		else
			dumpFile			=	new File(filename);
		fos						=	new FileOutputStream(dumpFile);
		mimeMessage.writeTo(fos);
		fos.flush();
	} catch (Exception t){
		LOGGER.error("Error in _EMAILfactory dump of "+filename,t);
	} finally {
		if( fos != null ) {
			try {fos.close();
			} catch (IOException ioe) {
				LOGGER.error("Closing the FileOutputStream failed in _EMAILfactory dump of "+refid,ioe);
			}
		}
	}
*/
