package nachos.threads;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 * 
 * <p>
 * You must implement this.
 * 
 * @see nachos.threads.Condition
 */
public class Condition2 {
	/**
	 * Allocate a new condition variable.
	 * 
	 * @param conditionLock
	 *            the lock associated with this condition variable. The current
	 *            thread must hold this lock whenever it uses <tt>sleep()</tt>,
	 *            <tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;
		waitQueue = ThreadedKernel.scheduler.newThreadQueue(false);
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The current
	 * thread must hold the associated lock. The thread will automatically
	 * reacquire the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		boolean intStatus = Machine.interrupt().disable();
		waitQueue.waitForAccess(KThread.currentThread());
		conditionLock.release();
		KThread.sleep();
		conditionLock.acquire();
		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	public void wake() {
		boolean intStatus = Machine.interrupt().disable();
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		KThread thread = waitQueue.nextThread();
		if (thread != null) {
		    thread.ready();
		}
		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		boolean intStatus = Machine.interrupt().disable();
		KThread kt;
		while ((kt = waitQueue.nextThread()) != null) {
			kt.ready();
		}
		Machine.interrupt().restore(intStatus);
	}
	
	
	/**
	 * Tests whether this module is working.
	 */
	public static void selfTest() {
		/*
		 * The only way we can test this without crashing the program is with a normal sort of test with KThread sleeping on this (just to test basic functionality)
		 */
		Lib.enableDebugFlags(dbgConditionTest + "");
		Lib.debug(dbgConditionTest, "TASK 2: Condition2 Self Test");
		KThread thread;

		// Verify that only the thread that holds the condition lock can call methods
		thread = new KThread(new Runnable() {
			// Nobody holds this condition lock
			Condition2 cond = new Condition2(new Lock());

			public void run() {
				try {
					cond.sleep();
					Lib.debug(dbgConditionTest, "Task2 Test1 [FAIL]: Successfully called Condition2.sleep() without holding condition lock");
				}
				catch (Error e) {
					Lib.debug(dbgConditionTest, "Task2 Test1 [PASS]: Condition2.sleep() threw assertion when called without holding condition lock");
				}
				try {
					cond.wake();
					Lib.debug(dbgConditionTest, "Task2 Test2 [FAIL]: Successfully called Condition2.wake() without holding condition lock");
				}
				catch (Error e) {
					Lib.debug(dbgConditionTest, "Task2 Test2 [PASS]: Condition2.wake() threw assertion when called without holding condition lock");
				}
				try {
					cond.wakeAll();
					Lib.debug(dbgConditionTest, "Task2 Test3 [FAIL]: Successfully called Condition2.wakeAll() without holding condition lock");
				}
				catch (Error e) {
					Lib.debug(dbgConditionTest, "Task2 Test3 [PASS]: Condition2.wakeAll() threw assertion when called without holding condition lock");
				}
			}
		});
		thread.fork();
		thread.join();

		// Verify that thread reacquires lock when woken
		final Lock lock = new Lock();
		final Condition2 cond = new Condition2(lock);

		KThread thread1 = new KThread(new Runnable() {
			public void run() {
				lock.acquire();
				cond.sleep();

				// When I wake up, I should hold the lock
				Lib.debug(dbgConditionTest, (lock.isHeldByCurrentThread() ? "Task2 Test4 [PASS]" : "Task2 Test4 [FAIL]") + ": Thread reacquires lock when woken.");
				lock.release();
			}
		});

		KThread thread2 = new KThread(new Runnable() {
			public void run() {
				lock.acquire();
				cond.wake();
				lock.release();
			}
		});

		//thread1.fork();
		//thread2.fork();
		thread1.join();

		// Verify that wake() wakes up 1 thread
		WakeCounter.wakeups = 0;
		WakeCounter.lock = lock;
		WakeCounter.cond = cond;

		new KThread(new WakeCounter()).fork();
		new KThread(new WakeCounter()).fork();
		thread = new KThread(new Runnable() {
			public void run() {
				lock.acquire();
				cond.wake();
				lock.release();
			}
		});
		thread.fork();
		thread.join();

		Lib.debug(dbgConditionTest, (WakeCounter.wakeups == 1 ? "Task2 Test5 [PASS]" : "Task2 Test5 [FAIL]") + ": Only 1 sleeping thread woken by Condition2.wake(). (" + WakeCounter.wakeups + ")");

		// Verify that wakeAll() wakes up all threads
		WakeCounter.wakeups = 0;

		new KThread(new WakeCounter()).fork();
		new KThread(new WakeCounter()).fork();
		thread = new KThread(new Runnable() {
			public void run() {
				lock.acquire();
				cond.wakeAll();
				lock.release();
			}
		});
		thread.fork();
		thread.join();

		// Notice: this should wake up the thread that's still hanging around from the last test, in addition to the new ones.
		Lib.debug(dbgConditionTest, (WakeCounter.wakeups == 3 ? "Task2 Test6 [PASS]" : "[FAIL]") + ": All sleeping threads woken by Condition2.wakeAll(). (" + WakeCounter.wakeups + ")");
	}

	/**
	 * Test class which increments a static counter when woken
	 */
	static class WakeCounter implements Runnable {
		public static int wakeups = 0;
		public static Lock lock = null;
		public static Condition2 cond = null;

		public void run() {
			lock.acquire();
			cond.sleep();
			wakeups++;
			lock.release();
		}
	}

	private static final char dbgConditionTest = 'r';
	private Lock conditionLock;
	private ThreadQueue waitQueue;
}
