/* com.zacwolf.commons.email.Email.java
 *
 * Abstract class that defined common properties for EmailTemplate and EmailFromTemplate objects
 *
*  Copyright (C) 2021 Zac Morris

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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;

import com.zacwolf.commons.utils.TimeUtils;

/**
 * @author Zac Morris <zac@zacwolf.com>
 * @version 1.3
 * @since Java1.8
 */
public abstract class Email implements Comparable<Email>, Serializable {
final	static	private		long 						serialVersionUID 	=	7850486608888530706L;
final			protected	String						refid;
final			protected	String						name;
final			protected	EmailSendLog				sendlog;
				protected	long						last_changed;
				protected	String						subject				=	null;
				protected	String						body				=	null;
				protected	String						body_plaintext		=	null;
				protected	Map<String,EmailAttachment>	attachments			=	null;
				protected	EmailDistribution			distribution		=	null;


	public Email(final String refid, final String name) {
		this.refid			=	refid;
		this.name			=	name;
		sendlog		=	new EmailSendLog();
		last_changed	=	TimeUtils.getGMTtime();
	}

	public synchronized void setBody(final String body) throws NullPointerException{
		if (body==null) {
			throw new NullPointerException("NULL is not a valid value for body");
		}
		if (this.body==null || !this.body.equals(body)){
			this.body			=	body;
			last_changed	=	TimeUtils.getGMTtime();
		}
	}

	public synchronized void setBodyPlainText(final String body_plaintext)throws NullPointerException{
		if (body_plaintext==null) {
			throw new NullPointerException("NULL is not a valid value for body_plaintext");
		}
		if (this.body_plaintext==null || !this.body_plaintext.equals(body_plaintext)){
			this.body_plaintext	=	body_plaintext;
			last_changed	=	TimeUtils.getGMTtime();
		}
	}

	public synchronized void setSubject(final String subject) throws NullPointerException, MessagingException, IOException{
		if (subject==null) {
			throw new NullPointerException("NULL is not a valid value for subject");
		}
		if (this.subject==null || !this.subject.equals(subject)){
			this.subject		=	subject;
			last_changed	=	TimeUtils.getGMTtime();
		}
	}

	public synchronized void setDistribution(final EmailDistribution distrib){
		if (distribution==null || !distribution.equals(distrib)) {
			distribution	=	distrib;
		}
	}

	public synchronized void addAttachment(final EmailAttachment attachment) throws MessagingException{
		if (attachments==null) {
			attachments	=	new HashMap<String,EmailAttachment>();
		}
		attachments.put(attachment.contentid,attachment);
	}

	public synchronized void bodyReplaceAll(final String regex, final String replacement){
		synchronized(body){
			body			=	body.replaceAll(regex, replacement);
		}
	}

	public String	getREFID(){
		return refid;
	}

	public String getName(){
		return name;
	}

	public EmailDistribution getDistribution(){
		return distribution;
	}

	public String getSubject(){
		return subject;
	}

	public String getBody(){
		return body;
	}

	public String getBodyPlainText(){
		return body_plaintext;
	}

	public Map<String,EmailAttachment> getAttachments(){
		return attachments;
	}

	public long getLastChanged(){
		return last_changed;
	}

	public EmailSendLog getSendLog(){
		return sendlog;
	}

	public String getAsHTML(){
final	org.jsoup.nodes.Document	doc			=	Jsoup.parse(getBody(), "UTF-8");
		prepareImgs(doc,null);
		prepare(doc);
		return doc.toString();
	}

	/*
	 * MimeMessages are very confusing in that MimeMultiparts may only ever be direct children
	 * of BodyPart objects, and not other MimeMultipart Objects.  This adds the requirement
	 * of "wrapping" a MimeMultipart by/into a BodyPart that may then be added back as a
	 * child of another MimeMultipart.  I try to illustrate the object structure via
	 * comments below, but understanding this relationship is key to understanding how an email is structured.
	 */
	public Multipart getAsMultipart() throws MessagingException{
		/** First we create the "related" htmlmultipart for the html email content:
		 * ┌─────────────────────────────────────────────┐
		 * │ msg.setContent()                            │
		 * │┌───────────────────────────────────────────┐│
		 * ││ htmlmultipart [MimeMultipart("related")]  ││
		 * ││┌─────────────────────────────────────────┐││
		 * │││ htmlmessageBodyPart [MimeBodyPart]      │││
		 * ││├─────────────────────────────────────────┤││
		 * │││ EmailAttachment(IN─LINE) [MimeBodyPart] │││
		 * ││├─────────────────────────────────────────┤││
		 * │││ EmailAttachment(IN─LINE) [BodyPart]     │││
		 * ││└─────────────────────────────────────────┘││
		 * │└───────────────────────────────────────────┘│
		 * └─────────────────────────────────────────────┘
		 **/
final	Multipart					htmlmultipart 		=	new MimeMultipart("related");
final	BodyPart					htmlmessageBodyPart	=	new MimeBodyPart();
									htmlmultipart.addBodyPart(htmlmessageBodyPart);
final	org.jsoup.nodes.Document	doc					=	Jsoup.parse(getBody(), "UTF-8");
									prepareImgs(doc,htmlmultipart);
									prepare(doc);
									htmlmessageBodyPart.setContent(doc.toString(),"text/html; charset=utf-8");

		// populate the top multipart
		Multipart 					msgmultipart		=	htmlmultipart;
		if (getBodyPlainText()!=null){// Now create a plain-text body part
		/**
		 * If there is a plain text attachment (and their should always be one),
		 * then an "alternative" type MimeMultipart is added to the structure
		 * ┌─────────────────────────────────────────────────┐
		 * │ msg.setContent()                                │
		 * │┌───────────────────────────────────────────────┐│
		 * ││ msgmultipart [MimeMultipart("alternative")]   ││
		 * ││┌─────────────────────────────────────────────┐││
		 * │││ htmlcontent [MimeBodyPart]                  │││
		 * │││┌───────────────────────────────────────────┐│││
		 * ││││ htmlmultipart [MimeMultipart("related")]  ││││
		 * ││││┌─────────────────────────────────────────┐││││
		 * │││││ htmlmessageBodyPart [MimeBodyPart]      │││││
		 * ││││├─────────────────────────────────────────┤││││
		 * │││││ EmailAttachment(IN─LINE) [MimeBodyPart] │││││
		 * ││││├─────────────────────────────────────────┤││││
		 * │││││ EmailAttachment(IN─LINE) [MimeBodyPart] │││││
		 * ││││└─────────────────────────────────────────┘││││
		 * │││└───────────────────────────────────────────┘│││
		 * ││├─────────────────────────────────────────────┤││
		 * │││plaintxtBodypart [MimeBodyPart]              │││
		 * │││.setText(message_plaintxt)                   │││
		 * ││└─────────────────────────────────────────────┘││
		 * │└───────────────────────────────────────────────┘│
		 * └─────────────────────────────────────────────────┘
		 */							msgmultipart 			=	new MimeMultipart("alternative");
final	BodyPart					plaintxtBodyPart		=	new MimeBodyPart();
									plaintxtBodyPart.setText(getBodyPlainText());
final	BodyPart					htmlBodyPart			=	new MimeBodyPart();
									htmlBodyPart.setContent(htmlmultipart);
									msgmultipart.addBodyPart(plaintxtBodyPart);
									msgmultipart.addBodyPart(htmlBodyPart);
		}

		/**
		 * If there are non-inline attachments, then a "mixed" type
		 * MimeMultipart object has to be added to the structure
		 * ┌─────────────────────────────────────────────────────┐
		 * │ msg.setContent()                                    │
		 * │┌───────────────────────────────────────────────────┐│
		 * ││ msgmultipart [MimeMultipart("mixed")]             ││
		 * ││┌─────────────────────────────────────────────────┐││
		 * │││ wrap [MimeBodyPart]                             │││
		 * │││┌───────────────────────────────────────────────┐│││
		 * ││││ msgmultipart [MimeMultipart("alternative")]   ││││
		 * ││││┌─────────────────────────────────────────────┐││││
		 * │││││ htmlcontent [MimeBodyPart]                  │││││
		 * │││││┌───────────────────────────────────────────┐│││││
		 * ││││││ htmlmultipart [MimeMultipart("related")]  ││││││
		 * ││││││┌─────────────────────────────────────────┐││││││
		 * │││││││ htmlmessageBodyPart [MimeBodyPart]      │││││││
		 * ││││││├─────────────────────────────────────────┤││││││
		 * │││││││ EmailAttachment(IN─LINE) [MimeBodyPart] │││││││
		 * ││││││├─────────────────────────────────────────┤││││││
		 * │││││││ EmailAttachment(IN─LINE) [MimeBodyPart] │││││││
		 * ││││││└─────────────────────────────────────────┘││││││
		 * │││││└───────────────────────────────────────────┘│││││
		 * ││││├─────────────────────────────────────────────┤││││
		 * │││││plaintxtBodypart [MimeBodyPart]              │││││
		 * │││││.setText(message_plaintxt)                   │││││
		 * ││││└─────────────────────────────────────────────┘││││
		 * │││└───────────────────────────────────────────────┘│││
		 * ││├─────────────────────────────────────────────────┤││
		 * │││ EmailAttachment (non-inline) [MimeBodyPart]     │││
		 * ││├─────────────────────────────────────────────────┤││
		 * │││ EmailAttachment (non-inline) [MimeBodyPart]     │││
		 * ││└─────────────────────────────────────────────────┘││
		 * │└───────────────────────────────────────────────────┘│
		 * └─────────────────────────────────────────────────────┘
		 */
		Multipart					mixed					=	msgmultipart;
final	Set<EmailAttachment>		noninlineattachments	=	new HashSet<EmailAttachment>();
		for (final EmailAttachment attach:getAttachments().values()) {
			if (attach.disposition!=null && !attach.disposition.equals(MimeBodyPart.INLINE)) {
				noninlineattachments.add(attach);
			}
		}
		// If there are non-IN-LINE attachments, we'll have to create another layer "mixed" MultiPart object
		if (!noninlineattachments.isEmpty()){
									mixed					=	new MimeMultipart("mixed");
		//Multiparts are not themselves containers, so create a wrapper BodyPart container
final	BodyPart					wrap					=	new MimeBodyPart();
									wrap.setContent(msgmultipart);
									mixed.addBodyPart(wrap);
			for (final EmailAttachment attach:noninlineattachments) {
				mixed.addBodyPart(attach);
			}
		}
		return mixed;
	}

	private void prepareImgs(final org.jsoup.nodes.Document doc, final Multipart htmlmultipart){
final	Map<String,EmailAttachment>	attachments		=	getAttachments();
final	org.jsoup.select.Elements	imgs			=	doc.getElementsByTag("img");
		for (final org.jsoup.nodes.Element img : imgs) {
final	String						src				=	img.attr("src");
final	String						cid				=	!src.startsWith("cid:")?null:src.substring(4);
			try{
		EmailAttachment				attachment;
		ByteArrayOutputStream		baos;
				if (cid!=null){
									attachment			=	attachments.get(cid);
									img.attr("alt",attachment.getDescription());
					if (!img.attr("style").contains("display:")) {
						img.attr("style",img.attr("style")+(!img.attr("style").endsWith(";")?";":"")+"display:block;");
					}
					if (cid.toLowerCase().contains("_banner") && doc.select("#banner").attr("style").contains("-radius")){
		final BufferedImage				image			=	makeRoundedBanner(ImageIO.read(new ByteArrayInputStream(attachment.data)), 20);
									doc.select("#contenttable").attr("style", "width:"+image.getWidth()+"px;"+doc.select("#contenttable").attr("style"));
									baos			=	new ByteArrayOutputStream();
						try{		ImageIO.write(image, EmailAttachment.CONTENT_MIMETYPES.get(attachment.contenttype), baos);
						} finally {	baos.flush();
						}			attachment		=	new EmailAttachment(attachment.filename,attachment.contenttype,baos.toByteArray(), cid, "Rounded banner image");
						if (htmlmultipart==null) {
							dataurlEncode(img,attachment);
						}
						if (doc.select("#footer").size()==1 && doc.select("#footer").first().attr("style").contains("-radius")){
		Color						bgcolor			=	Color.WHITE;
		Color						border			=	null;
		String						newstyle		=	"";
		final String[]					styles			=	doc.select("#footer").first().attr("style").split(";");
							for (final String style:styles){
								if (style.startsWith("border")) {
									border			=	getColorFromStyle(style,null);
								} else if (style.startsWith("background-color:")) {
									bgcolor			=	getColorFromStyle(style,Color.WHITE);
								} else {
									newstyle		+=	style+";";
								}
							}		baos			=	new ByteArrayOutputStream();
							try{	ImageIO.write(makeRoundedFooter(image.getWidth(), 20, bgcolor, border), "png", baos);
							} finally {
									baos.flush();
							}		doc.select("#footer").first().parent().html("<td style=\"margin:0px;padding:0px;\" valign=\"top\" style=\""+newstyle+"\"><img id=\"footer\" alt=\"rounded footer image\" src=\"cid:"+getREFID()+"_rounded_footer\" style=\"display:block;\" /></td>");
						}
						if (htmlmultipart==null) {
							dataurlEncode(doc.select("#footer").first(),new EmailAttachment("footer.png", "image/png", baos.toByteArray(), getREFID()+"_rounded_footer", "Rounded footer image"));
						} else {
							htmlmultipart.addBodyPart(new EmailAttachment("footer.png", "image/png", baos.toByteArray(), getREFID()+"_rounded_footer", "Rounded footer image"));
						}
					} else if (htmlmultipart==null) {
						dataurlEncode(img,attachment);
					}
					if (htmlmultipart!=null) {
						htmlmultipart.addBodyPart(attachment);
					}
				}
			} catch (final Exception e){
				throw new NullPointerException("Problem with embedding images into content.\nContact the content owner.\n\nERROR:"+e);
			}
		}
	}

	private void prepare(final org.jsoup.nodes.Document	doc){
		removeComments(doc);//Remove any comments from the html of the message to reduce the size
		//Change the title to match the subject of the email
		if (doc.getElementsByTag("title").size()>0) {
			doc.getElementsByTag("title").first().html(getSubject());
		}
		//Replace the contents of any tags with class="date" with the current date
		if (doc.getElementsByClass("date").size()>0){
			for (final org.jsoup.nodes.Element datelem: doc.getElementsByClass("date")){
		SimpleDateFormat			df				=	new SimpleDateFormat("MMMMMMMMMM d, yyyy");
				if (datelem.hasAttr("format")){
					try{			df				=	new SimpleDateFormat(datelem.attr("format"));
					} catch (final Exception ee){}//throw it away and just go back to the default format;
								datelem.html(df.format(TimeUtils.getGMTtime()));
				}
			}
		}
		//tables need the border-spacing: style attribute; added for GMail compatiblity
		for (final org.jsoup.nodes.Element tbl : doc.getElementsByTag("table")) {
			if (!tbl.attr("style").contains("border-spacing:")) {
				tbl.attr("style",tbl.attr("style")+(!tbl.attr("style").endsWith(";")?";":"")+"border-spacing:0;");
			}
		}
	}

	@Override
	public int compareTo(final Email o) {
		return refid.compareToIgnoreCase(o.refid);
	}

	@Override
	public int hashCode(){
		return refid.hashCode();
	}

	@Override
	public boolean equals(final Object e){
		if (e instanceof EmailTemplate ||  e instanceof EmailFromTemplate) {
			return this.hashCode()==e.hashCode();
		}

		return false;
	}

	public static BufferedImage makeRoundedBanner(final BufferedImage image, final int cornerRadius) {
	final int 			w 		= image.getWidth();
	final int 			h 		= image.getHeight()+10;
	final BufferedImage 	output 	= new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	final Graphics2D 		g2 		= output.createGraphics();
					g2.setComposite(AlphaComposite.Src);
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(Color.WHITE);
					g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));
					g2.setComposite(AlphaComposite.SrcAtop);
					g2.drawImage(image, 0, 0, null);
					g2.setComposite(AlphaComposite.SrcOver);
//					g2.setColor(new Color(153,153,153));//slight grey border
//					g2.drawRoundRect(0, 0, w-1, h, cornerRadius, cornerRadius);
					g2.dispose();
		return output.getSubimage(0,0,image.getWidth(),image.getHeight());
	}

	public static BufferedImage makeRoundedFooter(final int width, final int cornerRadius, final Color bgcolor, final Color border) throws Exception {
	final int				height	=	cornerRadius*2+10;
	final BufferedImage	output 	= new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	final Graphics2D 		g2 		= output.createGraphics();
					g2.setComposite(AlphaComposite.Src);
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setColor(bgcolor);
					g2.fillRoundRect(0, 0, width, height-1, cornerRadius, cornerRadius);
					g2.setComposite(AlphaComposite.SrcOver);
		if (border!=null){
					g2.setColor(border);
					g2.drawRoundRect(0, 0, width-1, height-2, cornerRadius, cornerRadius);
		}			g2.dispose();
	final Rectangle		clip	=	createClip(output,new Dimension(width,cornerRadius),0,height-cornerRadius-1);
		return output.getSubimage(clip.x, clip.y, clip.width, clip.height);
	}

	/**
	* This method crops an original image to the crop parameters provided.
	*
	* If the crop rectangle lies outside the rectangle (even if partially),
	* adjusts the rectangle to be included within the image area.
	*
	* @param img = Original Image To Be Cropped
	* @param size = Crop area rectangle
	* @param clipX = Starting X-position of crop area rectangle
	* @param clipY = Strating Y-position of crop area rectangle
	* @throws Exception
	*/
	public static Rectangle createClip(final BufferedImage img, final Dimension size, final int clipX, final int clipY) throws Exception {
	Rectangle	clip;
		if (size.width + clipX <= img.getWidth() && size.height + clipY <= img.getHeight()) {
				clip 		= new Rectangle(size);
				clip.x 		= clipX;
				clip.y 		= clipY;
		} else {
			if (size.width + clipX > img.getWidth()) {
				size.width	= img.getWidth() - clipX;
			}
			if (size.height + clipY > img.getHeight()) {
				size.height	= img.getHeight() - clipY;
			}
				clip		= new Rectangle(size);
				clip.x		= clipX;
				clip.y		= clipY;
		}
		return clip;
	}

	public static void removeComments(final org.jsoup.nodes.Node node){
		for (int i = 0; i < node.childNodes().size();i++) {
	final org.jsoup.nodes.Node	child = node.childNode(i);
			if (child.nodeName().equals("#comment")) {
				child.remove();
			} else {
				removeComments(child);
			}
		}
	}

	public static String color2hex(final Color col){
		return Integer.toHexString(col.getRGB() & 0xffffff | 0x1000000).substring(1);
	}

	public static Color getColorFromStyle(final String style, final Color defaultcolor){
		if (!style.contains("#")) {
			return defaultcolor;
		}
	final int	hexcolorstarts			=	style.indexOf("#");
		if (hexcolorstarts==-1) {
			return defaultcolor;
		}
	int	hexcolorends			=	style.indexOf(" ", hexcolorstarts);
		if (hexcolorends==-1) {
			hexcolorends		=	style.indexOf(";", hexcolorstarts);
		}
			if (hexcolorends==-1) {
				hexcolorends	=	style.length();
			}
		try{
			return Color.decode(style.substring(hexcolorstarts, hexcolorends));
		} catch (final Exception e){
			//TODO
		}
		return defaultcolor;
	}

	private static void dataurlEncode(final org.jsoup.nodes.Element img, final EmailAttachment attachment) throws IOException, MessagingException{
	final String					mime_type	=	attachment.contenttype;
	final InputStream				is			=	attachment.getDataHandler().getInputStream();
	final ByteArrayOutputStream	baos		=	new ByteArrayOutputStream();
	final OutputStream			b64os		=	MimeUtility.encode(baos, "base64");
							IOUtils.copy(is, b64os);
							b64os.close();
							img.attr("src", "data:"+mime_type+";base64,"+ new String(baos.toByteArray()));
	}
}
