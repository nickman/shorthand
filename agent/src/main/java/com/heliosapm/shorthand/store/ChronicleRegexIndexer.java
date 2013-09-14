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
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

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
 */

public class ChronicleRegexIndexer implements ChronicleRegexIndexerMBean  {
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
					lss.insert(ex.readLong());
				}
			}
			ex.close();
			ex = null;
			searchTimes.insert(System.nanoTime()-start);
			return lss.getArray();
		} catch (Exception e) {
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
	
	
	
}
