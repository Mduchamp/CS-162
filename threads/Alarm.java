package nachos.threads;

import nachos.machine.*;
import java.util.PriorityQueue;


/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	PriorityQueue<ThreadWait> threadWaitQueue = new PriorityQueue<ThreadWait>();
	
	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		long curTime = Machine.timer().getTime();
		
		while (!threadWaitQueue.isEmpty() && threadWaitQueue.peek().getWaitTime() <= curTime) { // do this atomically?
			ThreadWait threadWait = threadWaitQueue.poll();
			KThread t = threadWait.getThread();
			if (t != null) t.ready();
		}
		
		KThread.currentThread().yield();
	}
	
	

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x
	 *            the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) { //TODO: test if waituntil is called multiple times for same thread.
		if (x <= 0){
			return;
		}
		
		boolean intStatus = Machine.interrupt().disable();
		long wakeTime = Machine.timer().getTime() + x;
		threadWaitQueue.add(new ThreadWait(KThread.currentThread(), wakeTime));
		KThread.currentThread().sleep();
		Machine.interrupt().restore(intStatus);

	}
	
	
	private class ThreadWait implements Comparable<ThreadWait>{

		private KThread thread;
		private long waitTime;
		
		public ThreadWait(KThread thread, long waitTime) {
			this.thread = thread;
			this.waitTime = waitTime;
		}
		
		public KThread getThread(){
			return thread;
		}
		
		public long getWaitTime(){
			return waitTime;
		}
		
		public int compareTo(ThreadWait threadWait){
    		if (waitTime > threadWait.getWaitTime()){
    			return 1;
    		} else if (waitTime < threadWait.getWaitTime()){
    			return -1;
    		} else { 
    			return 0;
    		}
    	}
    }
		
		
		
		
	}
	
