/** Helios Development Group LLC, 2013 */
package com.heliosapm.shorthand.util.unsafe.collections;

import java.nio.LongBuffer;

import com.heliosapm.shorthand.util.unsafe.locks.NativeSpinLock;
import com.heliosapm.shorthand.util.unsafe.locks.NativeSpinLock.SpinLock;

/**
 * <p>Title: SpinLockLongSlidingWindow</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.collections.SpinLockLongSlidingWindow</code></p>
 */

public class SpinLockLongSlidingWindow extends LongSlidingWindow {
	/** The reentrant read/write lock */
	private final NativeSpinLock readWriteLock;
	/** The concurrent read lock */
	private final SpinLock readLock;
	/** The exclusive write lock */
	private final SpinLock writeLock;
	

	/**
	 * Creates a new SpinLockLongSlidingWindow
	 * @param size The size of the sliding window
	 * @param readerYield  Indicates if the reader lock spin will yield
	 * @param writerYield  Indicates if the writer lock spin will yield
	 */
	public SpinLockLongSlidingWindow(int size, boolean readerYield, boolean writerYield) {
		super(size);
		readWriteLock = new NativeSpinLock(readerYield, writerYield);
		readLock = readWriteLock.getReadLock();
		writeLock = readWriteLock.getWriteLock();
	}
	
	/**
	 * Creates a new SpinLockLongSlidingWindow
	 * @param size The size of the sliding window
	 * @param yield Indicates if the lock spins will yield
	 * @param values The initial values to populate the window with. 
	 * If the length of the values is longer than the size of the window, the values
	 * at the begining of the value array will slide out of the window.
	 */
	public SpinLockLongSlidingWindow(int size, boolean yield, long...values) {
		this(size, yield, yield);
		if(values!=null) {
			super.insert(values);
		}

	}
	

	/**
	 * Creates a new SpinLockLongSlidingWindow
	 * @param size The size of the sliding window
	 * @param readerYield  Indicates if the reader lock spin will yield
	 * @param writerYield  Indicates if the writer lock spin will yield
	 * @param values The initial values to populate the window with. 
	 * If the length of the values is longer than the size of the window, the values
	 * at the begining of the value array will slide out of the window.
	 */
	public SpinLockLongSlidingWindow(int size, boolean readerYield, boolean writerYield, long...values) {
		this(size, readerYield, writerYield);
		if(values!=null) {
			super.insert(values);
		}
	}
	
	


	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#insert(long)
	 */
	@Override
	public void insert(long...values) {
		writeLock.lock();
		try {
			super.insert(values);
		} finally {
			writeLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#insert(java.nio.LongBuffer)
	 */
	@Override
	public void insert(LongBuffer longBuff) {
		writeLock.lock();
		try {
			super.insert(longBuff);
		} finally {
			writeLock.unlock();
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#insert(long)
	 */
	@Override
	public Long insert(long value) {
		writeLock.lock();
		try {
			return super.insert(value);
		} finally {
			writeLock.unlock();
		}
	}	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#inc(int, long)
	 */
	@Override
	public long inc(int index, long value) {
		writeLock.lock();
		try {
			if(size()<index+1) throw new ArrayOverflowException("Attempted to increment at index [" + index + "] but size is [" + size() + "]", new Throwable());
			return array.set(index, array.get(index)+value).get(index);
		} finally {
			writeLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#inc(int)
	 */
	@Override
	public long inc(int index) {
		return inc(index, 1L);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#inc(long)
	 */
	@Override
	public long inc(long value) {
		return inc(0, value);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#inc()
	 */
	@Override
	public long inc() {
		return inc(0, 1L);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#find(long)
	 */
	@Override
	public int find(long value) {
		readLock.lock();
		try {
			return array.binarySearch(value);
		} finally {
			readLock.unlock();
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#set(long)
	 */
	@Override
	public void set(long value) {
		writeLock.lock();
		try {
			array.set(0, value);
		} finally {
			writeLock.unlock();
		}
	}
	
    /**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#asDoubleArray()
	 */
	@Override
	public double[] asDoubleArray() {
		readLock.lock();
		try {
			return array.asDoubleArray();
		} finally {
			readLock.unlock();
		}
	}	
	
	/**
	 * Returns this sliding window as a long array
	 * @return a long array
	 */
	@Override
	public long[] asLongArray() {
		readLock.lock();
		try {
			return array.getArray();
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#load(byte[])
	 */
	@Override
	public void load(byte[] arr) {
		writeLock.lock();
		try {
			array.load(arr);
		} finally {
			writeLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#reinitAndLoad(byte[])
	 */
	@Override
	public void reinitAndLoad(byte[] arr) {
		writeLock.lock();
		try {
			array.initAndLoad(arr);
		} finally {
			writeLock.unlock();
		}
		
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#clear()
	 */
	@Override
	public void clear() {
		writeLock.lock();
		try {
			super.clear();
		} finally {
			writeLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		readLock.lock();
		try {
			return super.isEmpty();
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#toString()
	 */
	@Override
	public String toString() {
		readLock.lock();
		try {
			return super.toString();
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#clone()
	 */
	@Override
	public LongSlidingWindow clone() {
		readLock.lock();
		try {
			return new LongSlidingWindow(array.clone());
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * Returns the most recent value in the array or -1L if the size is 0.
	 * @return the most recent in the array or -1L if the size is 0.
	 */
	public long getNewest() {
		return getFirst();
	}
	
	/**
	 * Returns the oldest value in the array or -1L if the size is 0.
	 * @return the oldest in the array or -1L if the size is 0.
	 */
	public long getOldest() {
		return getLast();
	}
	

	
	/**
	 * Returns the first (chronologically the most recent) value in the array or -1L if the size is 0.
	 * @return the first value in the array or -1L if the size is 0.
	 */
	@Override
	public long getFirst() {
		readLock.lock();
		try {
			if(size()<1) return -1L;
			return array.get(0);
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * Returns the last value (chronologically the oldest) in the array or -1L if the size is 0.
	 * @return the last value in the array or -1L if the size is 0.
	 */
	@Override
	public long getLast() {
		readLock.lock();
		try {
			if(size()<1) return -1L;
			return array.get(size()-1);
		} finally {
			readLock.unlock();
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#get(int)
	 */
	@Override
	public long get(int index) {
		readLock.lock();
		try {
			return array.get(index);
		} finally {
			readLock.unlock();
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#size()
	 */
	@Override
	public int size() {
		readLock.lock();
		try {
			return array.size();
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#sum(int)
	 */
	@Override
	public long sum(int within) {
		readLock.lock();
		try {
			return super.sum(within);
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#sum()
	 */
	@Override
	public long sum() {
		readLock.lock();
		try {
			return super.sum();
		} finally {
			readLock.unlock();
		}

	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#avg(int)
	 */
	@Override
	public long avg(int within) {
		readLock.lock();
		try {
			return super.avg(within);
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.collections.ILongSlidingWindow#avg()
	 */
	@Override
	public long avg() {
		readLock.lock();
		try {
			if(size()<1) return -1L;
			return super.avg();
		} finally {
			readLock.unlock();
		}

	}
	
	

}
