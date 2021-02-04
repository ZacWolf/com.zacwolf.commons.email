/*com.zacwolf.commons.email.EmailDistribution.java
 *
 * Used to define the various combination of email address types for a given Email objects
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSONObjectException;
import com.zacwolf.commons.utils.TimeUtils;

/**
 * @author Zac Morris <zac@zacwolf.com>
 * @version 1.3
 * @since Java1.8
 */
public class EmailDistribution implements Serializable {

final	static private	long 								serialVersionUID=	-7774607978066285874L;

final	static	public	String								LISTTYPE_FROM	=	"FROM";
final	static	public	String								LISTTYPE_TO		=	"TO";
final	static	public	String								LISTTYPE_CC		=	"CC";
final	static	public	String								LISTTYPE_BCC	=	"BCC";

final	static	public	String								GROUP_STATIC	=	"staticaddr";

				private	InternetAddress						from;
final			private	Map<String,Set<InternetAddress>>	toMap;
final			private	Map<String,Set<InternetAddress>>	ccMap;
final			private	Map<String,Set<InternetAddress>>	bccMap;
						long								last_changed;

	/**
	 * Create an EmailDistribution object from the JSON string that is returned by EmailDistribution's toString calls.
	 *
	 * @param json
	 * @return
	 * @throws AddressException
	 * @throws JSONObjectException
	 * @throws IOException
	 */
	 @SuppressWarnings("unchecked")
	public static EmailDistribution fromJSON(final String json) throws AddressException, JSONObjectException, IOException{
		final Map<String,Object>		obj		=	(Map<String,Object>)JSON.std.anyFrom(json);
		return	new EmailDistribution(	JSON.std.beanFrom(InternetAddress.class, JSON.std.asString(obj.get(LISTTYPE_FROM))),
										convert2InternetAddressSet((Map<String,ArrayList<Object>>)obj.get(LISTTYPE_TO)),
										convert2InternetAddressSet((Map<String,ArrayList<Object>>)obj.get(LISTTYPE_CC)),
										convert2InternetAddressSet((Map<String,ArrayList<Object>>)obj.get(LISTTYPE_BCC))
									);
	}

	/**
	 * Protected object instantiation call, used by the static fromJSON method and the clone method.
	 * @param from
	 * @param to
	 * @param cc
	 * @param bcc
	 */
	protected EmailDistribution(final InternetAddress from,
							  final Map<String,Set<InternetAddress>> to,
							  final Map<String,Set<InternetAddress>> cc,
							  final Map<String,Set<InternetAddress>> bcc)
	{
		this.from			=	from;
		toMap			=	new HashMap<String,Set<InternetAddress>>(to);
		ccMap			=	new HashMap<String,Set<InternetAddress>>(cc);
		bccMap			=	new HashMap<String,Set<InternetAddress>>(bcc);
		last_changed	=	TimeUtils.getGMTtime();
	}

	/**
	 * Creates an EmailDistribution object with the specified "from" address and "to" address.
	 *
	 * This object is serializable meaning it can be saved along with the email object it belongs to.
	 *
	 * @param from
	 * @param to
	 * @throws AddressException	if the domain string is not a valid Internet domain type or from is not a valid email address
	 */
	public EmailDistribution(final String from, final String to) throws AddressException{
		this(new InternetAddress(from),to);
	}

	/**
	 * Creates an EmailDistribution object with the specified "from" address and "to" address.
	 *
	 * This object is serializable meaning it can be saved along with the email object it belongs to
	 * @param from
	 * @param to
	 * @throws AddressException	if the email addresses are invalid
	 */
	public EmailDistribution(final InternetAddress from,final String to) throws AddressException{
		this.from			=	from;
		toMap			=	new HashMap<String,Set<InternetAddress>>();
		ccMap			=	new HashMap<String,Set<InternetAddress>>();
		bccMap			=	new HashMap<String,Set<InternetAddress>>();
		add2DistByAddress(toMap,InternetAddress.parseHeader(to,true));
		last_changed	=	TimeUtils.getGMTtime();
	}

	/**
	 * Creates an EmailDistribution object with the specified "from" and "to" addresses.
	 *
	 * This object is serializable meaning it can be saved along with the email object it belongs to.
	 *
	 * @param from
	 * @param to
	 * @throws AddressException	if the email addresses are invalid
	 */
	public EmailDistribution(final InternetAddress from,final InternetAddress[] to) throws AddressException{
		this.from			=	from;
		toMap			=	new HashMap<String,Set<InternetAddress>>();
		ccMap			=	new HashMap<String,Set<InternetAddress>>();
		bccMap			=	new HashMap<String,Set<InternetAddress>>();
		add2DistByAddress(toMap,to);
		last_changed	=	TimeUtils.getGMTtime();
	}

	/**
	 * Clones all the values of this EmailDistribution into a new EmailDistribution object.
	 * This is used to transfer a distribution from a template into a new email object.
	 *
	 * @return EmailDistribution object
	 */
	@Override
	public EmailDistribution clone(){
		return new EmailDistribution(from,toMap,ccMap,bccMap);
	}

	/**
	 * @param listtype
	 * @param address
	 * @return
	 * @throws AddressException
	 * @throws NullPointerException
	 */
	public boolean remove(final String listtype, final String address) throws AddressException, NullPointerException{
		return remove (listtype, GROUP_STATIC,address);
	}


	/**
	 * @param listtype TO, CC, or BCC
	 * @param group
	 * @param address
	 * @return True if the address was found and removed
	 * @throws AddressException
	 * @throws NullPointerException
	 */
	public boolean remove(final String listtype, final String group, final String address) throws AddressException, NullPointerException{
final	Map<String,Set<InternetAddress>>	map;
		if (listtype.equals(LISTTYPE_TO)) {
			map	=	toMap;
		} else if (listtype.equals(LISTTYPE_CC)) {
			map	=	ccMap;
		} else if (listtype.equals(LISTTYPE_BCC)) {
			map	=	bccMap;
		} else {
			throw new NullPointerException("Not a valid listtype");
		}

final	InternetAddress	realaddress	=	new InternetAddress(address);
		for (final InternetAddress addr:map.get(group)) {
			if (addr.getAddress().equals(realaddress.getAddress())) {
				if (map.get(group).remove(addr)){
					last_changed	=	TimeUtils.getGMTtime();
					return true;
				} else {
					break;
				}
			}
		}

		return false;
	}

	/**
	 * @param from
	 * @throws AddressException
	 */
	public synchronized void setFROM(final InternetAddress from) throws AddressException{
		if (from.isGroup()) {
			throw new AddressException("From may contain only a single email address, not a group");
		}
		if (!from.equals(this.from)){
			this.from			=	from;
			last_changed	=	TimeUtils.getGMTtime();
		}
	}

	/**
	 * @param to add an email TO recipient
	 * @return true if the address was added to the TO distribution
	 * @throws AddressException if it was invalid email address string
	 */
	public boolean addTO(final String to) throws AddressException{
		if (add2DistByAddress(toMap,InternetAddress.parseHeader(to,true))){
			last_changed	=	TimeUtils.getGMTtime();
			return true;
		}
		return false;
	}


	/**
	 * @param cc add an email CC recipient
	 * @return true if the address was added to the CC distribution
	 * @throws AddressException if it was invalid email address string
	 */
	public boolean addCC(final String cc) throws AddressException{
		if (cc==null || cc.length()==0) {
			return false;
		}
		if (add2DistByAddress(ccMap,InternetAddress.parseHeader(cc,true))){
			last_changed	=	TimeUtils.getGMTtime();
			return true;
		}
		return false;
	}

	/**
	 * @param bcc add an email BCC recipient
	 * @return true if the address was added to the CCC distribution
	 * @throws AddressException if it was invalid email address string
	 */
	public boolean addBCC(final String bcc) throws AddressException{
		if (bcc==null || bcc.length()==0) {
			return false;
		}
		if (add2DistByAddress(bccMap,InternetAddress.parseHeader(bcc,true))){
			last_changed	=	TimeUtils.getGMTtime();
			return true;
		}
		return false;
	}

	/**
	 * @return the email's FROM address
	 */
	public InternetAddress getFROM(){
		return from;
	}

	/**
	 * @return TO addresses
	 */
	public InternetAddress[] getTO(){
		return	getFlatDist(toMap);
	}

	/**
	 * @return CC addresses
	 */
	public InternetAddress[] getCC(){
		return getFlatDist(ccMap);
	}

	/**
	 * @return BCC addresses
	 */
	public InternetAddress[] getBCC(){
		return getFlatDist(bccMap);
	}

	/**
	 * @param listtype TO, CC, or BCC
	 * @return Set of any groupTypes assigned to
	 */
	public Set<String> getGroupTypes(final String listtype){
		if (listtype.equals("LISTTYPE_TO")) {
			return toMap.keySet();
		} else if (listtype.equals("LISTTYPE_CC")) {
			return ccMap.keySet();
		} else if (listtype.equals("LISTTYPE_BCC")) {
			return bccMap.keySet();
		}

		throw new NullPointerException("Not a valid listtype");
	}

	/**
	 * @return
	 * @throws JSONObjectException
	 * @throws IOException
	 */
	public String toJSONflat() throws JSONObjectException, IOException{
		return JSON.std.asString(this);
	}

	/**
	 * Returns a JSON string the represents the distribution list
	 * This method returns a compact string with no pretty-print formatting
	 *
	 * @return
	 * @throws JSONObjectException
	 * @throws IOException
	 */
	public String toJSONWithGroups() throws JSONObjectException, IOException {
final	StringBuilder						string	=	new StringBuilder();
											string.append("{\""+LISTTYPE_FROM+"\":"+JSON.std.asString(from));
											string.append(",\""+LISTTYPE_TO+"\":"+JSON.std.asString(toMap));
		if(ccMap.size()>0) {
			string.append(",\""+LISTTYPE_CC+"\":"+JSON.std.asString(ccMap));
		}
		if (bccMap.size()>0) {
			string.append(",\""+LISTTYPE_BCC+"\":"+JSON.std.asString(bccMap));
		}
											string.append("}");
		return string.toString();

/* 		//Requires jackson-jr-objects-2.4.2+
final	Map<String,Object>					temp	=	new HashMap<String,Object>();
											temp.put("from", this.from);
											temp.put("TO", this.toMap);
		if(this.ccMap.size()>0)				temp.put("CC", this.ccMap);
		if (this.bccMap.size()>0)			temp.put("BCC", this.bccMap);
		return JSON.std.asString(temp);
 */
	}

	/**
	 * @param listtype
	 * @return
	 * @throws NullPointerException
	 * @throws IOException
	 */
	public String toString_LISTTYPE(final String listtype) throws NullPointerException, IOException{
		if (listtype.equals(LISTTYPE_TO)) {
			return JSON.std.asString(toMap);
		} else if (listtype.equals(LISTTYPE_CC)) {
			return JSON.std.asString(ccMap);
		} else if (listtype.equals(LISTTYPE_BCC)) {
			return JSON.std.asString(bccMap);
		}

		throw new NullPointerException("Not a valid listtype");
	}

	/**
	 * @param listtype
	 * @param group
	 * @return
	 * @throws NullPointerException
	 * @throws IOException
	 */
	public String toString_LISTTYPE_GROUPTYPE(final String listtype, final String group) throws NullPointerException, IOException{
		if (listtype.equals(LISTTYPE_TO)) {
			return JSON.std.asString(toMap.get(group));
		} else if (listtype.equals(LISTTYPE_CC)) {
			return JSON.std.asString(ccMap.get(group));
		} else if (listtype.equals(LISTTYPE_BCC)) {
			return JSON.std.asString(bccMap.get(group));
		}

		throw new NullPointerException("Not a valid listtype");
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		try {
			return	JSON.std
						.with(JSON.Feature.PRETTY_PRINT_OUTPUT)
						.asString(JSON.std.anyFrom(toJSONWithGroups()));
		} catch (final Exception e) {
			return super.toString();
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 *
	 * We only calculate the hash code based on the actual EmailAddress.address text
	 */
	@Override
	public int hashCode(){
int		hash	=	from.getAddress().hashCode();
		if (toMap.size()>0) {
			for (final Set<InternetAddress> set:toMap.values()) {
				for (final InternetAddress addr:set) {
					hash+=addr.getAddress().hashCode();
				}
			}
		}
		if (ccMap.size()>0) {
			for (final Set<InternetAddress> set:ccMap.values()) {
				for (final InternetAddress addr:set) {
					hash+=addr.getAddress().hashCode();
				}
			}
		}
		if (bccMap.size()>0) {
			for (final Set<InternetAddress> set:bccMap.values()) {
				for (final InternetAddress addr:set) {
					hash+=addr.getAddress().hashCode();
				}
			}
		}
		return hash;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj){
		if (!(obj instanceof EmailDistribution)) {
			return false;
		}
		return obj.hashCode()==this.hashCode();
	}

	protected static boolean add2DistByAddress(final Map<String,Set<InternetAddress>> map, final InternetAddress[] addresses) throws AddressException{
		boolean					changed		=	false;
		synchronized(map){
			for (final InternetAddress addr:addresses){
		String					group		=	GROUP_STATIC;
				if (addr.isGroup()){
								group		=	addr.toUnicodeString();
								group		=	group.substring(0,group.indexOf(":"));
				}
		Set<InternetAddress>	addrs		=	map.get(group);
				if (addrs==null){
								addrs		=	groupToSet(group,addr);
								changed		=	true;
				} else {
								changed		=	addrs.addAll(groupToSet(group,addr));
				}
				map.put(group,addrs);
			}
		}
		return changed;
	}

	/**
	 * @param group
	 * @param address
	 * @return
	 * @throws AddressException
	 */
	protected static Set<InternetAddress> groupToSet(final String group, final InternetAddress address) throws AddressException{
final	Set<InternetAddress>	set	=	new HashSet<InternetAddress>();
		if (address.isGroup()) {
			for (final InternetAddress addr:address.getGroup(true)) {
				set.addAll(groupToSet(group,addr));
			}
		} else {
			if (address!=null ) {
				set.add(address);
			}
		}
		return set;
	}

	/**
	 * @param map
	 * @return
	 */
	protected static InternetAddress[] getFlatDist(final Map<String,Set<InternetAddress>> map){
		if (map.size()==0) {
			return null;
		}

final	Set<InternetAddress>	addresses	=	new HashSet<InternetAddress>();
		for (final String group:map.keySet()){
			try{
					addresses.addAll(map.get(group));
			}catch (final NullPointerException npe){

			}
		}
		return addresses.toArray(new InternetAddress[0]);
	}

	protected static Map<String,Set<InternetAddress>> convert2InternetAddressSet(final Map<String,ArrayList<Object>> map) throws JSONObjectException, IOException{
final	Map<String,Set<InternetAddress>>	n	=	new HashMap<String,Set<InternetAddress>>();
		for (final String key:map.keySet()){
final	ArrayList<Object>	set	=	map.get(key);
			for (final Object o:set){
				if (!n.containsKey(key)) {
					n.put(key, new HashSet<InternetAddress>());
				}
				n.get(key).add(JSON.std.beanFrom(InternetAddress.class, JSON.std.asString(o)));
			}
		}
		return n;
	}


	public static void main(final String[] args){
		try {
final	EmailDistribution	test;
							test	=	new EmailDistribution(args[0],args[1]);
							test.addCC("test@cisco.com");
							test.addBCC("Test Account (email)<test2@cisco.com>");
							System.out.println("FLAT---------------------------------------------------");
							System.out.println(test.toJSONflat());
							System.out.println("W/GROUPS-----------------------------------------------");
							System.out.println(test.toString());

final	EmailDistribution	test2	=	EmailDistribution.fromJSON(test.toString());
							System.out.println("FLAT---------------------------------------------------");
							System.out.println(test2.toJSONflat());
							System.out.println("W/GROUPS-----------------------------------------------");
							System.out.println(test2.toString());
							System.out.println("=======================================================");
							System.out.println("Matches:"+test.equals(test2));
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}