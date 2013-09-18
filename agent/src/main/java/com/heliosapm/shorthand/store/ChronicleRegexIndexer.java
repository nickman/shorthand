/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package com.heliosapm.shorthand.store;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.heliosapm.shorthand.util.jmx.JMXHelper;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;
import com.heliosapm.shorthand.util.unsafe.collections.ConcurrentLongSlidingWindow;
import com.heliosapm.shorthand.util.unsafe.collections.UnsafeArrayBuilder;
import com.heliosapm.shorthand.util.unsafe.collections.UnsafeLongArray;
import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;

/**
 * <p>Title: ChronicleRegexIndexer</p>
 * <p>Description: Inexing and search service for chronicle resident metrics</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.store.ChronicleRegexIndexer</code></p>
 * TODO: Remove stale metric ids from regex-indexes.
 * TODO: Add Shorthand property to pre-define indexes to create
 * TODO: Persist shared index definition file
 */

public class ChronicleRegexIndexer implements ChronicleRegexIndexerMBean, NotificationBroadcaster  {
	/** The pattern caches */
	protected final Cache<String, Pattern> patternCache = CacheBuilder.newBuilder().build();	
	/** The chronicle caches */
	protected final Cache<String, Chronicle> regexChronicles = CacheBuilder.newBuilder().build();
	/** The name index chronicle */
	protected final Chronicle nameIndex;
	/** The current chronicle directory name */
	protected final File chronicleDir;
	
	

	/** Serial number for the regex chronicle files */
	protected final AtomicLong chronicleSerial = new AtomicLong(0L);
	/** A counter of metric names submitted */
	protected final AtomicLong submittedNames = new AtomicLong(0L);
	/** A counter of metric names indexed */
	protected final AtomicLong indexedNames = new AtomicLong(0L);
	
	/** The elapsed times of the last 50 searches */
	protected final ConcurrentLongSlidingWindow searchTimes = new ConcurrentLongSlidingWindow(50); 
	
	 
	
	
	/** A queue of index pending metric names */
	protected final ArrayBlockingQueue<NameIndex> pending = new ArrayBlockingQueue<NameIndex>(1024, false); 
	/** A thread pool to process indexing and search tasks asynchronously */
	protected final ExecutorService threadPool = Executors.newFixedThreadPool(ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors(), new ThreadFactory(){
		protected final AtomicLong serial = new AtomicLong();
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "ChronicleRegexIndexerReaderThread#" + serial.incrementAndGet());
			t.setDaemon(true);
			return t;
		}
	});
	/** The notification serial number */
	protected final AtomicLong notificationSerial = new AtomicLong();
	
	/** The delegate notification broadcaster */
	protected final NotificationBroadcasterSupport notificationBroadcaster = new NotificationBroadcasterSupport(threadPool, new MBeanNotificationInfo[] {
			new MBeanNotificationInfo(new String[]{NOTIF_NEW_METRIC}, Notification.class.getName(), "Notification emitted on a new metric name"),
			new MBeanNotificationInfo(new String[]{NOTIF_STALE_METRIC}, Notification.class.getName(), "Notification emitted on a stale metric name")
	}); 

	
	/** The index writer thread */
	protected final Thread indexUpdater = new Thread("ChronicleRegexIndexerWriterThread") {
		public void run() {
			while(true) {
				try {
					NameIndex newName = pending.take();					
					for(Map.Entry<String, Pattern> entry: patternCache.asMap().entrySet()) {
						if(entry.getValue().matcher(newName.name).matches()) {
							Chronicle c = regexChronicles.getIfPresent(entry.getKey());
							Excerpt ex = c.createExcerpt();
							ex.startExcerpt(UnsafeAdapter.LONG_SIZE);
							ex.writeLong(newName.index);
							ex.finish();
							ex.close();
							indexedNames.incrementAndGet();
						}
					}
				} catch (InterruptedException iex) {
					/* No Op */
				}
			}
		}
	};
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleRegexIndexerMBean#getIndexCount()
	 */
	@Override
	public long getIndexCount() {
		return regexChronicles.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleRegexIndexerMBean#count(java.lang.String)
	 */
	public int count(String namePattern) {
		return search(namePattern).length;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleRegexIndexerMBean#search(java.lang.String)
	 */
	@Override
	public long[] search(String namePattern) {
		long start = System.nanoTime();
		Pattern p = Pattern.compile(namePattern);
		Excerpt ex = null;
		try {
			Chronicle c = getChronicleIndex(namePattern);
			UnsafeLongArray lss = UnsafeArrayBuilder.newBuilder().buildLongArray();
			ex = c.createExcerpt();
			if(ex.index(1)) {
				while(ex.nextIndex()) {
					lss.append(ex.readLong());
				}
			}
			ex.close();
			ex = null;
			searchTimes.insert(System.nanoTime()-start);
			return lss.getArray();
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new RuntimeException("Failed to execute search for [" + p + "]", e);
		} finally {
			if(ex!=null) try { ex.close(); } catch(Exception e) {/* No Op */}
		}
	}
	
	/**
	 * Submits a new name for indexing
	 * @param name The metric name
	 * @param index The metric index
	 */
	public void submitNewName(String name, long index) {
		if(pending.offer(new NameIndex(name, index))) {
			submittedNames.incrementAndGet();
		}		
	}
	
	
	/**
	 * Creates a new ChronicleRegexIndexer
	 * @param nameIndex The name index
	 * @param chronicleDir The chronicle directory
	 */
	ChronicleRegexIndexer(Chronicle nameIndex, File chronicleDir) {		
		this.nameIndex = nameIndex;
		this.chronicleDir = chronicleDir;
		indexUpdater.setDaemon(true);
		indexUpdater.start();
		JMXHelper.registerMBean(this, OBJECT_NAME);
	}
	
	/**
	 * Returns the regex index chronicle for the passed pattern
	 * @param regex The regex pattern to get the chronicle for
	 * @return the chronicle
	 */
	protected Chronicle getChronicleIndex(final String regex)  {
		try {
			return regexChronicles.get(regex, new Callable<Chronicle>(){
				@Override
				public Chronicle call() throws Exception {
					Chronicle c = getChronicle("regexIndex-" + chronicleSerial.incrementAndGet());
					Excerpt nameEx = nameIndex.createExcerpt();
					nameEx.index(0);
					Excerpt ex = c.createExcerpt();
					Pattern p = Pattern.compile(regex);
					patternCache.put(regex, p);
					ex.startExcerpt(regex.getBytes().length + 8);
					ex.writeInt(regex.getBytes().length);
					ex.write(regex.getBytes());					
					ex.finish();
					log("Creating Regex Chronicle for [%s]", regex);
					while(nameEx.nextIndex()) {
						long newIndex = nameEx.index();
						if(ChronicleOffset.isDeleted(newIndex, nameEx)) continue;
						if(p.matcher(ChronicleOffset.getName(newIndex)).matches()) {
							ex.startExcerpt(UnsafeAdapter.LONG_SIZE);
							ex.writeLong(newIndex);
							ex.finish();
							indexedNames.incrementAndGet();
						}
					}
					nameEx.close();
					ex.close();
					return c;
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			throw new RuntimeException(ex);
		}
	}


	/**
	 * Acquires the named chronicle
	 * @param name The name of the chronicle
	 * @return the named chronicle
	 * @throws IOException An exception occured creating or opening the chronicle
	 */
	protected IndexedChronicle getChronicle(String name) throws IOException {
		IndexedChronicle ic =  new IndexedChronicle(chronicleDir.getAbsolutePath() + File.separator + name, 1, ByteOrder.nativeOrder(), true, false);
		ic.useUnsafe(UnsafeAdapter.FIVE_COPY);
		return ic;
	}


	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleRegexIndexerMBean#getSubmittedNameCount()
	 */
	@Override
	public long getSubmittedNameCount() {
		return submittedNames.get();
	}


	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleRegexIndexerMBean#getIndexedNames()
	 */
	@Override
	public long getIndexedNames() {
		return indexedNames.get();
	}
	
	/**
	 * Simple out formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleRegexIndexerMBean#getAverageSearchTimeNanos()
	 */
	public long getAverageSearchTimeNanos() {
		return searchTimes.avg();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleRegexIndexerMBean#getAverageSearchTimeMillis()
	 */
	public long getAverageSearchTimeMillis() {
		return TimeUnit.NANOSECONDS.toMillis(searchTimes.avg());
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleRegexIndexerMBean#getLastSearchTimeNanos()
	 */
	public long getLastSearchTimeNanos() {
		return searchTimes.getFirst();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleRegexIndexerMBean#getLastSearchTimeMillis()
	 */
	public long getLastSearchTimeMillis() {
		return TimeUnit.NANOSECONDS.toMillis(searchTimes.getFirst());
	}
	
	/**
	 * Returns a map of index sizes keyed by the index regex pattern
	 * @return a map of index sizes keyed by the index regex pattern
	 */
	public Map<String, Long> getIndexSizes() {
		Map<String, Long> map = new HashMap<String, Long>((int)regexChronicles.size());
		for(Map.Entry<String, Chronicle> entry: regexChronicles.asMap().entrySet()) {
			map.put(entry.getKey(), entry.getValue().size()-1);
		}
		return map;
	}
	
	/** A cache of sets of subscribed notification listeners keyed by the pattern they're subscribed to */
	protected final Cache<Pattern, Set<NotificationListener>> subscriptions = CacheBuilder.newBuilder().build();
	
	/** The runtime name */
	public static final String RUNTIME_ID = ManagementFactory.getRuntimeMXBean().getName();
	/** The source of notiifcations */
	public static final String NOTIF_SOURCE = RUNTIME_ID + "/" + OBJECT_NAME;
	
	/**
	 * Emmits a metric name notification to subscribers that supplied a matching pattern
	 * @param notifType The notification type to send
	 * @param name The new metric name
	 * @param index The metric index
	 */
	public void notify(final String notifType, final String name, final long index) {
		Set<NotificationListener> matches = new HashSet<NotificationListener>();
		Map<NotificationListener, Pattern> matchers = new HashMap<NotificationListener, Pattern>();
		for(Pattern p: subscriptions.asMap().keySet()) {
			try {
				if(p.matcher(name).matches()) {
					for(NotificationListener listener: subscriptions.getIfPresent(p)) {
						if(!matchers.containsKey(listener)) {
							matchers.put(listener, p);
						}
					}
					matches.addAll(subscriptions.getIfPresent(p));
				}
			} catch (Exception x) {/* No Op */}
		}
		final String messagePrefix = notifType.equals(NOTIF_NEW_METRIC) ? "New" : "Stale";
 		final Notification notif = new Notification(notifType, NOTIF_SOURCE, notificationSerial.incrementAndGet(), System.currentTimeMillis(), String.format("%s Metric [%s:%s]", messagePrefix, name, index));
		for(final Map.Entry<NotificationListener, Pattern> entry: matchers.entrySet()) {
			threadPool.execute(new Runnable() {
				@Override
				public void run() {					
					entry.getKey().handleNotification(notif, entry.getValue().toString());
				}
			});
		}
		sendNotification(notif);
	}
	
	/**
	 * Emmits a metric name notification to subscribers that supplied a matching pattern
	 * @param name The new metric name
	 * @param index The metric index
	 */
	public void notifyNewMetric(final String name, final long index) {
		notify(NOTIF_NEW_METRIC, name, index);
	}	
	/**
	 * Emmits a stale metric name notification to subscribers that supplied a matching pattern
	 * @param name The metric name that became stale
	 * @param index The metric index
	 */
	public void notifyStaleMetric(final String name, final long index) {
		notify(NOTIF_STALE_METRIC, name, index);
	}
	
	/**
	 * <p>If the passed handback is a {@link Pattern} or a {@link CharSequence} which is compilable into a {@link Pattern}, the passed listener will
	 * be subscribed to a notifications regarding new or stale metric names that match the resulting pattern. Otherwise the listener will be notified of all events.</p> 
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#addNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
		Pattern p = getPattern(handback);
		if(p!=null) {
			try {
				subscriptions.get(p, new Callable<Set<NotificationListener>>() {
					@Override
					public Set<NotificationListener> call() throws Exception {						
						return new CopyOnWriteArraySet<NotificationListener>();
					}
				}).add(listener);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to register listener", ex);
			}
			return;
		}
		notificationBroadcaster.addNotificationListener(listener, filter, handback);
	}
	
	/**
	 * Tests the passed object to see if it is  pattern
	 * @param handback The object to test
	 * @return the pattern or null if one could not be determined
	 */
	protected Pattern getPattern(Object handback) {
		if(handback==null) return null;
		if(handback instanceof Pattern) return (Pattern)handback;
		if(handback instanceof CharSequence) {
			try {
				return Pattern.compile(handback.toString().trim());
			} catch (Exception ex) {
				return null;
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#getNotificationInfo()
	 */
	public MBeanNotificationInfo[] getNotificationInfo() {
		return notificationBroadcaster.getNotificationInfo();
	}

	/**
	 * Removes a notification listener
	 * @param listener The listener to remove
	 * @param filter The listener's filter
	 * @param handback The listener's handback
	 * @throws ListenerNotFoundException thrown if the specified listener cannot be found
	 */
	public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
		Pattern p = getPattern(handback);
		if(p!=null) {
			Set<NotificationListener> listeners = subscriptions.getIfPresent(p);
			if(listeners!=null) {
				if(listeners.remove(listener)) {
					if(listeners.isEmpty()) {
						subscriptions.invalidate(p);
					}
					return;  // unless a listener is found, the built-in removeNotificationListener is called
				}
			}
		}
		notificationBroadcaster.removeNotificationListener(listener, filter, handback);
		
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#removeNotificationListener(javax.management.NotificationListener)
	 */
	public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
		boolean listenerFound = false;
		Set<Pattern> removes = new HashSet<Pattern>();
		for(Map.Entry<Pattern, Set<NotificationListener>> entry: subscriptions.asMap().entrySet()) {
			if(entry.getValue().remove(listener)) {
				listenerFound = true;
				if(entry.getValue().isEmpty()) {
					removes.add(entry.getKey());
				}
			}
		}
		if(!removes.isEmpty()) {
			subscriptions.invalidateAll(removes);
		}
		if(!listenerFound) {
			notificationBroadcaster.removeNotificationListener(listener);
		}
	}

	/**
	 * Broadcasts the passed notification
	 * @param notification the notification to broadcast
	 */
	public void sendNotification(Notification notification) {
		notificationBroadcaster.sendNotification(notification);
	}
	
	
}
