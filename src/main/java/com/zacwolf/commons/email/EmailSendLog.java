/* com.zacwolf.commons.email.EmailSendLog.java
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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Zac Morris <zac@zacwolf.com>
 * @version 1.3
 * @since Java1.8
 */
public class EmailSendLog implements Serializable {
final	static	private		long 			serialVersionUID	=	-4795866199500575188L;
final			private		Set<String>		emailSentTo			=	new HashSet<String>();
final			private		StringBuilder	errors				=	new StringBuilder();

	synchronized void logSentTo(final String emailaddress){
		emailSentTo.add(emailaddress);
	}

	boolean alreadySentTo(final String emailaddress){
		synchronized(emailSentTo){
			return emailSentTo.contains(emailaddress);
		}
	}

	void logError(final String error){
		synchronized(errors){
			errors.append(error);
		}
	}

	void logError(final Exception e){
		synchronized(errors){
			errors.append("[ERROR]:"+e+" [MSG]:"+e.getMessage());
		}
	}

	public String getErrors(){
		synchronized(errors){
			return errors.toString();
		}
	}
}
