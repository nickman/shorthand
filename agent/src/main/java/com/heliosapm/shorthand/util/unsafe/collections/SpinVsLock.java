/** Helios Development Group LLC, 2013 */
package com.heliosapm.shorthand.util.unsafe.collections;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>Title: SpinVsLock</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.collections.SpinVsLock</code></p>
 */

public class SpinVsLock {
	static final int WARMUPS = 10000;
	static final int RUNS = 10000000;
	static final int SIZE = 1000;
	static final int TCOUNT = 4;
	
	public static void main(String[] args) {
		log("SpinVsLock Test");
		final long[] elapsedTimes = new long[TCOUNT];
		final ConcurrentLongSlidingWindow clsw = new ConcurrentLongSlidingWindow(SIZE);
		final SpinLockLongSlidingWindow slsw = new SpinLockLongSlidingWindow(SIZE, true);
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch endLatch = new CountDownLatch(TCOUNT);		
		final AtomicReference<CountDownLatch> slatch = new AtomicReference<CountDownLatch>(startLatch); 
		final AtomicReference<CountDownLatch> elatch = new AtomicReference<CountDownLatch>(endLatch);
		for(int i = 0; i < TCOUNT; i++) {
			final int x = i;
			Thread t = new Thread("CLSW-" + i) {
				@Override
				public void run() {
					try { slatch.get().await(); } catch (Exception ex) {}
					for(int i = 0; i < WARMUPS; i++) {
						clsw.insert(i);
					}					
					clsw.clear();
					long start = System.currentTimeMillis();
					for(int i = 0; i < RUNS; i++) {
						clsw.insert(i);
					}				
					long elapsed = System.currentTimeMillis()-start;
					elapsedTimes[x] = elapsed;
//					log(elapsed + " ms.");	
					elatch.get().countDown();
				}
			};  t.setPriority(i%2==0 ? Thread.MAX_PRIORITY : Thread.MIN_PRIORITY); t.start();		
		}
		log("Starting CLSW");
		startLatch.countDown();
		try { endLatch.await(); } catch (Exception x) {}
		printResults("CLSW", elapsedTimes);
		log("=================================================");
		startLatch = new CountDownLatch(1);
		endLatch = new CountDownLatch(TCOUNT);		
		slatch.set(startLatch); 
		elatch.set(endLatch);
		for(int i = 0; i < TCOUNT; i++) {	
			final int x = i;
			Thread t = new Thread("SLSW-" + i) {
				@Override
				public void run() {
					try { slatch.get().await(); } catch (Exception ex) {}
					for(int i = 0; i < WARMUPS; i++) {
						slsw.insert(i);
					}
					slsw.clear();
					long start = System.currentTimeMillis();
					for(int i = 0; i < RUNS; i++) {
						slsw.insert(i);
					}				
					long elapsed = System.currentTimeMillis()-start;
					elapsedTimes[x] = elapsed;
//					log(elapsed + " ms.");	
					elatch.get().countDown();
				}
			}; t.setPriority(i%2==0 ? Thread.MAX_PRIORITY : Thread.MIN_PRIORITY); t.start();			
		}
		log("Starting SLSW");
		startLatch.countDown();
		try { endLatch.await(); } catch (Exception x) {}
		printResults("SLSW", elapsedTimes);
		log("============= END ===========");
		
		
		
		
	}
	
	protected static void printResults(String name, long[] etimes) {
		long total = 0;
		for(long v: etimes) {
			total += v;
		}
		long avg = total/etimes.length;
		log(String.format("Type:%s  Total:[%s] ms. Avg:[%s] ms.", name, total, avg));
	}
	
	public static void log(Object msg) {
		System.out.println("[" + Thread.currentThread().getName() + "]: " + msg);
	}


}
