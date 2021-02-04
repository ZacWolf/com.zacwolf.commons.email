/* com.zacwolf.commons.concurrent._THREADfactory.java - zac@zacwolf.com
 *
 * Convenience class for creating a thread pool by name
 *
	Licensed under the MIT License (MIT)

	Copyright (c) 2021 Zac Morris <a href="mailto:zac@zacwolf.com">zac@zacwolf.com</a>

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.
 */
package com.zacwolf.commons.email;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Zac Morris <a href="mailto:zac@zacwolf.com">zac@zacwolf.com</a>
 * @version 2.0
 * @since Java1.8
 */
public class _THREADfactory {
final	private	static	ConcurrentMap<String, ExecutorService>	pools	=	new ConcurrentHashMap<String,ExecutorService>();

	static{
		//If pools are not shutdown gracefully, then shutdown hard
		Runtime.getRuntime().addShutdownHook(new Thread("_THREADfactory shutdown hook") {
			@Override
			public void run() {
				shutdownNow();
			}
		});
	}

	/**
	 * Looks for a ExecutorService thread pool specified by name.
	 * If no thread pool by that name exists, a new one is created
	 * with <code>size</code> number of threads.
	 *
	 * If a thread already exists, then <code>side</code> is ignored.
	 *
	 * @param name thread name
	 * @param size number of threads
	 * @return ExecutorService
	 */
	public static ExecutorService getThreadPool(final String name, final int size){
		if (!pools.containsKey(name.toLowerCase()) || pools.get(name).isShutdown()){
final	ExecutorService		pool		=	Executors.newFixedThreadPool(size, new ThreadFactory(name));
							pools.put(name, pool);
		}
		return pools.get(name);
	}

	@Override
	public void finalize(){
		shutdownNow();
	}

	/**
	 * Shuts down ALL the thread pools
	 */
	public static void shutdownNow(){
		for (final String poolname:pools.keySet()) {
			try{
				pools.get(poolname).shutdownNow();
			} catch (final Exception e){
				System.err.println("Error shutting down pool:"+poolname+" [ERROR]:"+e+" [MSG]:"+e.getMessage());
			}
		}
	}


	/**
	 * @param name
	 * @return list of tasks that never commenced execution
	 * @throws NullPointerException
	 */
	public static List<Runnable> shutdownNowAndRemove(final String name) throws NullPointerException {
final	ExecutorService		pool	=	pools.remove(name);
		return pool.shutdownNow();
	}

	/**
	 * Shutdown a single pool by name.
	 *
	 * @param pool
	 * @return
	 * @throws NullPointerException
	 */
	public static List<Runnable> shutdownNowAndRemove(final ExecutorService pool) throws NullPointerException {
		pools.values().remove(pool);
		return pool.shutdownNow();
	}

	final public static class ThreadFactory implements java.util.concurrent.ThreadFactory {

final	String		name;
final	ThreadGroup	group;

		ThreadFactory(final String name){
			this.name	=	name;
			group		=	new ThreadGroup(name);
		}

		/* (non-Javadoc)
		 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
		 */
		@Override
		public Thread newThread(final Runnable r) {
final	Thread	t = new Thread(group,r);
				t.setName(name + t.getName());
				t.setDaemon(false);
				return t;
		}


	}
}
