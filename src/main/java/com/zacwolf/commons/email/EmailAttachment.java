/* com.zacwolf.commons.email.EmailAttachment.java
 *
 * Extension of a MimeBodyPart that specifically defined an email attachment
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

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;

import com.zacwolf.commons.utils.TimeUtils;

/**
 * @author Zac Morris <zac@zacwolf.com>
 * @version 1.3
 * @since Java1.8
 */
public class EmailAttachment extends MimeBodyPart implements Serializable{
	private	static final 	long 					serialVersionUID 	=	-2600972241622783139L;
	public 	static final	Map<String,String>		CONTENT_MIMETYPES	=	new HashMap<String,String>();

							String					filename;
							String					contenttype;
							String					contentid;
							String					description;
							String					disposition;
							byte[]					data;

	static{
		CONTENT_MIMETYPES.put("image/png","png");
		CONTENT_MIMETYPES.put("image/jpeg","jpg");
		CONTENT_MIMETYPES.put("image/gif","gif");
	}

	public EmailAttachment(final String filename, final String contenttype, final byte[] data, final String contentid, final String description) throws MessagingException, NullPointerException, IOException{
		this(filename,contenttype,data,contentid,description,MimeMessage.INLINE);
	}

	public EmailAttachment(final String filename, final String contenttype, final byte[] data, final String contentid, final String description, final String disposition) throws MessagingException, NullPointerException, IOException {
		if (contentid==null) {
			throw new NullPointerException("Attachment's contentid may not be null.");
		}
		this.contentid		=	contentid;
		this.filename		=	filename==null?"inline_"+TimeUtils.getTimestampString()+"."+CONTENT_MIMETYPES.get(contenttype):filename;
		this.contenttype	=	contenttype;
		this.description	=	description;
		this.disposition	=	disposition;
		this.data			=	data;
		super.setDataHandler(new DataHandler(new ByteArrayDataSource(this.data,this.contenttype)));
		super.setHeader("Content-Type", contenttype);
		super.setDescription(description, "utf-8");
		super.setFileName(filename);
		if (this.contentid!=null && this.contentid.length()>0) {
			super.setContentID((this.contentid.startsWith("<")?"":"<")+this.contentid+(this.contentid.endsWith(">")?"":">"));
		}
		if (disposition!=null && disposition.equals(MimeBodyPart.INLINE)) {
			super.setDisposition(null);//correct issue with not showing inline attachments on iPad
		} else {
			super.setDisposition(disposition);
		}
	}

	public byte[] getData(){
		return data;
	}

	@Override
	public EmailAttachment clone(){
		try {
			return new EmailAttachment(filename,contenttype,data,contentid,description,disposition);
		} catch (final Exception e) {
			//throw it away as these errors would have been thrown on the original
		}
		return null;
	}

	private void writeObject(final java.io.ObjectOutputStream out) throws IOException{
		synchronized(this){
			out.defaultWriteObject();
		}
	}

	private void readObject(final java.io.ObjectInputStream in) throws IOException, ClassNotFoundException{
		synchronized(this){
			in.defaultReadObject();
			//manually deserialize superclass
			try{
				super.setDataHandler(new DataHandler(new ByteArrayDataSource(data,contenttype)));
				super.setHeader("Content-Type", contenttype);
				super.setDescription(description, "utf-8");
				super.setFileName(filename);
				if (contentid!=null && contentid.length()>0) {
					super.setContentID((contentid.startsWith("<")?"":"<")+contentid+(contentid.endsWith(">")?"":">"));
				}
				if (disposition!=null && disposition.equals(MimeBodyPart.INLINE)) {
					super.setDisposition(null);//correct issue with not showing inline attachments on iPad
				} else {
					super.setDisposition(disposition);
				}
			} catch (final MessagingException me){
				 me.printStackTrace();
			}
		}
	 }
}