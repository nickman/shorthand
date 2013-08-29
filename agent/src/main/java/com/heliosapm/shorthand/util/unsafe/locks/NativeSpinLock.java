/** Helios Development Group LLC, 2013 */
package com.heliosapm.shorthand.util.unsafe.locks;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import com.heliosapm.shorthand.util.ref.RunnableReferenceQueue;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: NativeSpinLock</p>
 * <p>Description: A spin lock based on a pointer to a native address.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.collections.NativeSpinLock</code></p>
 */

public class NativeSpinLock {
	/** Indicates if spin lock stats should be collected on all instances */
	protected static final AtomicBoolean threadStatsEnabled = new AtomicBoolean(false);
	/** The value the address points to when unlocked */
	public static final long UNLOCKED = 0;
	/** The address of the spin lock */
	protected final long address;
	/** The read lock */
	protected final SpinLock readLock;
	/** The write lock */
	protected final SpinLock writeLock;
	
	
	
	
	public static void main(String[] args) {
		log("FastReader Test");
		final NativeSpinLock spinlock = new NativeSpinLock(false, true);
		final long allotedTime = 1000 * 60 * 10;
		final CountDownLatch startLatch = new CountDownLatch(1);		
		final long endTime = System.currentTimeMillis() + allotedTime;
		new Thread() {
			public void run() {
				SpinLock readLock = spinlock.getReadLock();
				try { startLatch.await(); } catch (Exception ex) {}
				log("Reader Started");
				long timesAcquired = 0;
				while(System.currentTimeMillis()<endTime) {
					readLock.lock();
					readLock.unlock();
					timesAcquired++;					
				}
				log("ReadLock:" + timesAcquired);				
			}
		}.start();
		new Thread() {
			public void run() {
				SpinLock writeLock = spinlock.getWriteLock();
				try { startLatch.await(); } catch (Exception ex) {}
				log("Writer Started");
				long timesAcquired = 0;
				while(System.currentTimeMillis()<endTime) {
					writeLock.lock();
					writeLock.unlock();
					timesAcquired++;
					
				}
				log("WriteLock:" + timesAcquired);				
			}
		}.start();
		startLatch.countDown();
		log("=================================================");
		
		
		
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

	/**
	 * Creates a new NativeSpinLock
	 * @param readYield Indicates if the read lock should yield while spinning
	 * @param writeYield Indicates if the write lock should yield while spinning 
	 */
	public NativeSpinLock(boolean readYield, boolean writeYield) {
		address = UnsafeAdapter.allocateMemory(UnsafeAdapter.LONG_SIZE);
		RunnableReferenceQueue.getInstance().buildPhantomReference(this, address);
		readLock = readYield ? new NativeYieldingSpinLock(address, this) : new NativeNonYieldingSpinLock(address, this); 
		writeLock = writeYield ? new NativeYieldingSpinLock(address, this) : new NativeNonYieldingSpinLock(address, this);
		UnsafeAdapter.putLong(address, UNLOCKED);
	}
	


	/**
	 * Returns the read lock
	 * @return the readLock
	 */
	public SpinLock getReadLock() {
		return readLock;
	}
	/**
	 * Returns the write lock 
	 * @return the writeLock
	 */
	public SpinLock getWriteLock() {
		return writeLock;
	}	
	
	/**
	 * <p>Title: SpinLock</p>
	 * <p>Description: Defines the basic operations of a spin lock</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.shorthand.util.collections.NativeSpinLock.SpinLock</code></p>
	 */
	public abstract class SpinLock {
		/** The address of the spin lock */
		protected final long address;
		
		/** The spin lock owner */
		protected final NativeSpinLock owner;

		/**
		 * Creates a new SpinLock
		 * @param address The address of the spin lock
		 * @param owner The spin lock owner
		 */
		public SpinLock(long address, NativeSpinLock owner) {
			this.address = address;
			this.owner = owner;
		}
		/**
		 * Acquires the lock, assigning it to the current thread.
		 */
		public abstract void lock();
		
		/**
		 * Indicates if the lock is held by the current thread
		 * @return true if the lock is held by the current thread, false otherwise
		 */
		public boolean isLockHeldByCurrentThread() {
			return UnsafeAdapter.getLong(address)==Thread.currentThread().getId();
		}
		
		/**
		 * Releases the lock
		 * @return true if the lock was held by the current thread and was released, 
		 * false if the lock was not held by the current thread
		 */
		public boolean unlock() {
			return UnsafeAdapter.compareAndSwapLong(null, address, Thread.currentThread().getId(), UNLOCKED);
		}
	}
	
	
	/**
	 * <p>Title: NativeYieldingSpinLock</p>
	 * <p>Description: A native spin lock that yields when spinning on acquiring the lock</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.shorthand.util.collections.NativeSpinLock.NativeYieldingSpinLock</code></p>
	 */
	private class NativeYieldingSpinLock extends SpinLock {
		/**
		 * Creates a new NativeYieldingSpinLock
		 * @param address the address of the lock
		 * @param owner The spin lock owner
		 */
		public NativeYieldingSpinLock(long address, NativeSpinLock owner) {
			super(address, owner);
		}

		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.util.unsafe.locks.collections.NativeSpinLock.SpinLock#lock()
		 */
		@Override
		public void lock() {
			final long id = Thread.currentThread().getId();
			while(!UnsafeAdapter.compareAndSwapLong(null, address, UNLOCKED, id)) {
				Thread.yield();
			}
		}
	
	}
	/**
	 * <p>Title: NativeNonYieldingSpinLock</p>
	 * <p>Description: A native spin lock that does not yield when spinning on acquiring the lock</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.shorthand.util.collections.NativeSpinLock.NativeNonYieldingSpinLock</code></p>
	 */
	private class NativeNonYieldingSpinLock extends SpinLock {
		
		/**
		 * Creates a new NativeNonYieldingSpinLock
		 * @param address The address of the spin lock
		 * @param owner The spin lock owner
		 */
		public NativeNonYieldingSpinLock(long address, NativeSpinLock owner) {
			super(address, owner);
		}

		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.util.unsafe.locks.collections.NativeSpinLock.SpinLock#lock()
		 */
		@Override
		public void lock() {
			final long id = Thread.currentThread().getId();
			while(!UnsafeAdapter.compareAndSwapLong(null, address, UNLOCKED, id)) {
			}			
		}
		
	}


}
