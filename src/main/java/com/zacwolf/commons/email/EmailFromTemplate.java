/* com.zacwolf.commons.email.EmailFromTemplate.java
 *
 * Implementation of Email object based on an email Template
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

import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;

/**
 * @author Zac Morris <zac@zacwolf.com>
 * @version 1.3
 * @since Java1.8
 */
public class EmailFromTemplate extends Email {

	private	static		final 	long 			serialVersionUID	=	5143409985159356855L;

	public	transient	final	EmailTemplate	emailTemplate;

	public EmailFromTemplate(final int sid, final String name, final EmailTemplate template){
		super("sid_"+sid,name);
		emailTemplate	=	template;
		super.body			=	Jsoup.parse(template.body, "UTF-8").select("#content").html();
		setDistribution(template.getDistribution().clone());
	}

	@Override
	public final String getSubject(){
		if (super.subject==null && emailTemplate.subject!=null) {
			return emailTemplate.subject;
		}
		return subject;
	}

	@Override
	public final String getBody(){
	final org.jsoup.nodes.Document	doc=	Jsoup.parse(emailTemplate.body, "UTF-8");
		if (doc.select("#content").size()>0) {
			doc.select("#content").html(super.body);
		}
		return doc.toString();
	}

	public final String getBody(final boolean selfOnly){
		return body;
	}

	@Override
	public final String getBodyPlainText(){
		if (super.body_plaintext==null || super.body_plaintext.length()==0) {
			return emailTemplate.getBodyPlainText();
		}
		return super.getBodyPlainText();
	}

	@Override
	public final Map<String,EmailAttachment> getAttachments(){
	final Map<String,EmailAttachment>	combined	=	new HashMap<String,EmailAttachment>();
		if (attachments!=null && attachments.size()>0) {
			combined.putAll(attachments);
		}
		if (emailTemplate!=null && emailTemplate.attachments!=null && emailTemplate.attachments.size()>0) {
			combined.putAll(emailTemplate.attachments);
		}
		return combined;
	}

}
