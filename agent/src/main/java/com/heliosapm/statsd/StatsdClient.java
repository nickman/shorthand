/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
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
package com.heliosapm.statsd;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


/**
 * <p>Title: StatsdClient</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.statsd.StatsdClient</code></p>
 */

public class StatsdClient extends TimerTask {
    private ByteBuffer sendBuffer;
    private Timer flushTimer;
    private boolean multi_metrics = false;

private static final Random RNG = new Random();


private final InetSocketAddress _address;
private final Socket _socket;
private final OutputStream socketOutputStream;
private final MulticastSocket _msocket;

public StatsdClient(String host, int port) throws UnknownHostException, IOException {
	this(InetAddress.getByName(host), port);
}

public static void main(String[] args) {
	log("StatsD client");
	try {
		Random random = new Random(System.currentTimeMillis());
		StatsdClient client = new StatsdClient("239.192.74.66", 25826);
		log("Connected");
		for(int i = 0; i < 100; i++) {
			client.increment("foo.bar.baz");
			client.gauge("foo.bar.snafu", Math.abs(random.nextInt(400)));
			Thread.sleep(3000);
		}
	} catch (Exception e) {		
		e.printStackTrace();
	} 
}

public StatsdClient(InetAddress host, int port) throws IOException {
	_address = new InetSocketAddress(host, port);
	if(_address.getAddress().isMulticastAddress()) {
		_msocket = new MulticastSocket(_address);
		_socket = null;
		socketOutputStream = null;
		_msocket.setSendBufferSize(1500);
	} else {
		_msocket = null;
		_socket = new Socket(_address.getAddress(), _address.getPort());
		socketOutputStream =  _socket.getOutputStream();
		_socket.setSendBufferSize(1500);
	}
	this.setBufferSize((short)1500);
}
	

    protected void finalize() {
            flush();
    }

    public synchronized void setBufferSize(short packetBufferSize) {
            if(sendBuffer != null) {
                    flush();
            }
            sendBuffer = ByteBuffer.allocate(packetBufferSize);
    }

    public synchronized void enableMultiMetrics(boolean enable) {
            multi_metrics = enable;
    }

    public synchronized boolean startFlushTimer(long period) {
            if(flushTimer == null) {
                    // period is in msecs 
                    if(period <= 0) { period = 2000; }
                    flushTimer = new Timer();

                    // We pass this object in as the TimerTask (which calls run())
                    flushTimer.schedule((TimerTask)this, period, period);
                    return true;
            }
            return false;
    }

    public synchronized void stopFlushTimer() {
            if(flushTimer != null) {
                    flushTimer.cancel();
                    flushTimer = null;
            }
    }

    public void run() {     // used by Timer, we're a Runnable TimerTask
            flush();
    }


public boolean timing(String key, int value) {
	return timing(key, value, 1.0);
}

public boolean timing(String key, int value, double sampleRate) {
	return send(sampleRate, String.format(Locale.ENGLISH, "%s:%d|ms", key, value));
}

public boolean decrement(String key) {
	return increment(key, -1, 1.0);
}

public boolean decrement(String key, int magnitude) {
	return decrement(key, magnitude, 1.0);
}

public boolean decrement(String key, int magnitude, double sampleRate) {
	magnitude = magnitude < 0 ? magnitude : -magnitude;
	return increment(key, magnitude, sampleRate);
}

public boolean decrement(String... keys) {
	return increment(-1, 1.0, keys);
}

public boolean decrement(int magnitude, String... keys) {
	magnitude = magnitude < 0 ? magnitude : -magnitude;
	return increment(magnitude, 1.0, keys);
}

public boolean decrement(int magnitude, double sampleRate, String... keys) {
	magnitude = magnitude < 0 ? magnitude : -magnitude;
	return increment(magnitude, sampleRate, keys);
}

public boolean increment(String key) {
	return increment(key, 1, 1.0);
}

public boolean increment(String key, int magnitude) {
	return increment(key, magnitude, 1.0);
}

public boolean increment(String key, int magnitude, double sampleRate) {
	String stat = String.format(Locale.ENGLISH, "%s:%s|c", key, magnitude);
	return send(sampleRate, stat);
}

public boolean increment(int magnitude, double sampleRate, String... keys) {
	String[] stats = new String[keys.length];
	for (int i = 0; i < keys.length; i++) {
		stats[i] = String.format(Locale.ENGLISH, "%s:%s|c", keys[i], magnitude);
	}
	return send(sampleRate, stats);
}

public boolean gauge(String key, double magnitude){
	return gauge(key, magnitude, 1.0);
}

public boolean gauge(String key, double magnitude, double sampleRate){
	final String stat = String.format(Locale.ENGLISH, "%s:%s|g", key, magnitude);
	return send(sampleRate, stat);
}

private boolean send(double sampleRate, String... stats) {

	boolean retval = false; // didn't send anything
	if (sampleRate < 1.0) {
		for (String stat : stats) {
			if (RNG.nextDouble() <= sampleRate) {
				stat = String.format(Locale.ENGLISH, "%s|@%f", stat, sampleRate);
				if (doSend(stat)) {
					retval = true;
				}
			}
		}
	} else {
		for (String stat : stats) {
			if (doSend(stat)) {
				retval = true;
			}
		}
	}

	return retval;
}

private synchronized boolean doSend(String stat) {
            try {
                    final byte[] data = stat.getBytes("utf-8");

                    // If we're going to go past the threshold of the buffer then flush.
                    // the +1 is for the potential '\n' in multi_metrics below
                    if(sendBuffer.remaining() < (data.length + 1)) {  
                            flush();
                    }

                    if(sendBuffer.position() > 0) {         // multiple metrics are separated by '\n'
                            sendBuffer.put( (byte) '\n');
                    }

                    sendBuffer.put(data);   // append the data

                    if(! multi_metrics) {
                            flush();
                    }

                    return true;

	} catch (IOException e) {
		loge("Could not send stat %s to host %s:%d", e, sendBuffer.toString(), _address.getHostName(), _address.getPort());
		return false;
	}
    }

    public synchronized boolean flush() {
	try {
                    final int sizeOfBuffer = sendBuffer.position();
                
                    if(sizeOfBuffer <= 0) { return false; } // empty buffer

                    // send and reset the buffer 
                    sendBuffer.flip();
                    byte[] arr = sendBuffer.array();
                    if(_socket!=null) {
                    	socketOutputStream.write(arr);
                    	socketOutputStream.flush();
                    }
                    else {
                    	DatagramPacket dPacket = new DatagramPacket(arr, arr.length, _address);
                    	_msocket.send(dPacket);
                    }
                    sendBuffer.limit(sendBuffer.capacity());
                    sendBuffer.rewind();

			return true;

	} catch (IOException e) {
		loge(String.format("Could not send stat %s to host %s:%s", e, sendBuffer.toString(), _address.getHostName(), _address.getPort()));
		return false;
	}
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
	 * Simple err formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void loge(String fmt, Object...args) {
		System.err.println(String.format(fmt, args));
	}
	
	/**
	 * Simple err formatted logger
	 * @param fmt The format of the message
	 * @param t The throwable to print stack trace for
	 * @param args The message arguments
	 */
	public static void loge(String fmt, Throwable t, Object...args) {
		System.err.println(String.format(fmt, args));
		t.printStackTrace(System.err);
	}	
}

