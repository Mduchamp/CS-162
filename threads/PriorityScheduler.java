package nachos.threads;

import nachos.machine.*;

import java.util.*;

/**
 * A scheduler that chooses threads based on their priorities.
 * 
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the thread
 * that has been waiting longest.
 * 
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fashion to
 * all the highest-priority threads, and ignores all other threads. This has the
 * potential to starve a thread if there's always a thread waiting with higher
 * priority.
 * 
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {

	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a nh0p+ew priority thread queue.
	 * 
	 * @param transferPriority
	 *            <tt>true</tt> if this queue should transfer priority from
	 *            waiting threads to the owning thread.
	 * @return a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum
				&& priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		setPriority(thread, priority - 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;

	/**
	 * Return the scheduling state of the specified thread.
	 * 
	 * @param thread
	 *            the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue {
		private ThreadState holdingThread;

		public void setHoldingThread(ThreadState hold) {
			holdingThread = hold;
		}

		public int getSize() {
			return pq.size();
		}

		public ThreadState getHoldingThread() {
			return holdingThread;
		}

		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
			this.pq = new java.util.PriorityQueue<ThreadState>(
					100, new threadsComparator<ThreadState>(this));
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			ThreadState nextThread = pq.poll();
			// Lib.debug(dbgPS, "Removed thread from queue = " +
			// nextThread.getName());
			ThreadState oldHoldingThread = this.holdingThread;
			ThreadState newHoldingThread = null;
			// Lib.debug(dbgPS, "New Holding Thread= " +
			// newHoldingThread.thread.getName() + " Old holding thread= " +
			// oldHoldingThread.thread.getName());

			/*
			 * 
			 * Needs to take in account waiting
			 */
			if (transferPriority) {
				if (oldHoldingThread != null) {
					oldHoldingThread.removeQueue(this);
				}
				if (nextThread != null) {
					newHoldingThread = nextThread;
					newHoldingThread.waiting = false;
					// newHoldingThread.addQueue(this);
					this.holdingThread = newHoldingThread;
					acquire(nextThread.thread);
				}
				if (newHoldingThread == null)
					this.holdingThread = null;
			}
			if (pq.isEmpty() && newHoldingThread == null) {
				return null;
			} else {
				return nextThread.thread;
			}
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 * 
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */
		protected ThreadState pickNextThread() {
			if (pq.isEmpty()) {
				return null;
			} else {
				return pq.peek();
			}
		}

		public void add(ThreadState thread) {
			pq.add(thread);
		}

		public void remove(ThreadState thread) {
			pq.remove(thread);
		}

		// need to implement this pq data structure
		java.util.PriorityQueue<ThreadState> pq;

		protected class threadsComparator<T extends ThreadState> implements
				Comparator<T> {
			private nachos.threads.PriorityScheduler.PriorityQueue pQueue;

			protected threadsComparator(
					nachos.threads.PriorityScheduler.PriorityQueue pq) {
				pQueue = pq;
			}

			@Override
			public int compare(T o1, T o2) {
				int effPrior1 = o1.getEffectivePriority();
				int effPrior2 = o2.getEffectivePriority();
				if (effPrior1 > effPrior2) {
					return -1;
				} else if (effPrior1 < effPrior2) {
					return 1;
				} else {
					if (o1.getWaitTime() < o2.getWaitTime()) {
						return 1;
					} else {
						return -1;
					}
				}
			}
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue it's
	 * waiting for, if any.
	 * 
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 * 
		 * @param thread
		 *            the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;
			this.waitTime = Machine.timer().getTime();
			this.queueOfQueues = new ArrayList<nachos.threads.PriorityScheduler.PriorityQueue>();
			this.waitingQueue = new nachos.threads.PriorityScheduler.PriorityQueue(
					true);
			setPriority(priorityDefault);
		}

		/**
		 * Return the priority of the associated thread.
		 * 
		 * @return the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 * 
		 * @return the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			return effectivePriority;
		}

		protected void updateEffectivePriority() {

			/**
			 * Goes through all of the queues that are waiting and finds if the
			 * current ThreadState is in the queues.
			 * 
			 * This should be only one according to us. Maybe this keeps a
			 * history or something interesting. Like how would you remove the
			 * previously used queue after acquired?
			 */

			if (waiting) {
				if (this != waitingQueue.getHoldingThread())
					waitingQueue.remove(this);
			}

			int maxPriorityOfDonors = -1;
			for (nachos.threads.PriorityScheduler.PriorityQueue currentQ : queueOfQueues) {
				if (currentQ.transferPriority) {
					ThreadState frontThread = currentQ.pickNextThread();
					if (frontThread != null) {
						int frontThreadPriority = frontThread
								.getEffectivePriority();
						if (frontThreadPriority > priority
								&& frontThreadPriority > maxPriorityOfDonors) {
							maxPriorityOfDonors = frontThreadPriority;
						}
					}
				}
			}

			if (priority > maxPriorityOfDonors) {
				maxPriorityOfDonors = priority;
			}

			effectivePriority = maxPriorityOfDonors;

			/*
			 * String temp; if (waitingQueue.getHoldingThread() != null) temp =
			 * waitingQueue.getHoldingThread().thread.getName(); else temp =
			 * null;
			 * 
			 * Lib.debug(dbgPS, "Holding Thread= " + temp + " Current Thread= "
			 * + this.thread.getName());
			 */

			if (waiting) {
				if (this != waitingQueue.getHoldingThread())
					waitingQueue.add(this);
				if (waitingQueue.getHoldingThread() != null) {
					if (waitingQueue.getHoldingThread().getEffectivePriority() < effectivePriority) {
						/*Lib.debug(
								dbgPS,
								"Calling holding Thread= "
										+ waitingQueue.getHoldingThread().thread
												.getName()
										+ " Holding thread Priority= "
										+ waitingQueue.getHoldingThread()
												.getEffectivePriority()
										+ " current thread effective priority= "
										+ effectivePriority);
						waitingQueue.getHoldingThread()
								.updateEffectivePriority();*/
					}
				}
			}
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the resource
		 * guarded by <tt>waitQueue</tt>. This method is only called if the
		 * associated thread cannot immediately obtain access.
		 * 
		 * @param waitQueue
		 *            the queue that the associated thread is now waiting on.
		 * 
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
			waiting = true;
			waitQueue.add(this);
			waitingQueue = waitQueue;
//			addQueue(waitQueue);
			this.waitTime = Machine.timer().getTime();
			updateEffectivePriority();
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 * 
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue acquiredQueue) {
			waiting = false;
			waitingQueue = null;
			acquiredQueue.setHoldingThread(this);
			addQueue(acquiredQueue);
			updateEffectivePriority();
		}

		public long getWaitTime() {
			return waitTime;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 * 
		 * @param priority
		 *            the new priority.
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;

			this.priority = priority;
			updateEffectivePriority();
		}

		public void removeQueue(
				nachos.threads.PriorityScheduler.PriorityQueue queue) {
			queueOfQueues.remove(queue);
			// updateEffectivePriority();
		}

		public void addQueue(
				nachos.threads.PriorityScheduler.PriorityQueue queue) {
			queueOfQueues.add(queue);
		}

		private boolean waiting = false;
		public long waitTime = Machine.timer().getTime();
		/** The thread with which this object is associated. */
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		/** The effective priority of the associated thread. */
		protected int effectivePriority;

		private nachos.threads.PriorityScheduler.PriorityQueue waitingQueue;
		private ArrayList<nachos.threads.PriorityScheduler.PriorityQueue> queueOfQueues;

	}

	public static final char dbgPS = 'u';

	public static void selfTest() {
		Lib.enableDebugFlags(dbgPS + "");
		Lib.debug(dbgPS, "TASK 5: Priority Scheduler Self Test");
		PriorityScheduler s = new PriorityScheduler();
		ThreadQueue queue = s.newThreadQueue(true);
		ThreadQueue queue2 = s.newThreadQueue(true);
		ThreadQueue queue3 = s.newThreadQueue(true);

		KThread thread1 = new KThread();
		KThread thread2 = new KThread();
		KThread thread3 = new KThread();
		KThread thread4 = new KThread();
		KThread thread5 = new KThread();
		thread1.setName("thread1");
		thread2.setName("thread2");
		thread3.setName("thread3");
		thread4.setName("thread4");
		thread5.setName("thread5");

		KThread kt_1 = new KThread();
		KThread kt_2 = new KThread();
		KThread kt_3 = new KThread();
		KThread kt_4 = new KThread();
		ThreadQueue tq1 = s.newThreadQueue(true);
		ThreadQueue tq2 = s.newThreadQueue(true);
		ThreadQueue tq3 = s.newThreadQueue(true);
		ThreadQueue tq4 = s.newThreadQueue(true);
		kt_1.setName("Thread 1");
		kt_2.setName("Thread 2");
		kt_3.setName("Thread 3");
		kt_4.setName("Thread 4");

		boolean intStatus = Machine.interrupt().disable();

		tq1.acquire(kt_2);
		tq2.acquire(kt_3);
		tq3.acquire(kt_4);

		tq1.waitForAccess(kt_1);
		tq2.waitForAccess(kt_2);
		tq3.waitForAccess(kt_3);

		queue3.acquire(thread1);
		queue.acquire(thread1);
		queue.waitForAccess(thread2);
		queue2.acquire(thread4);
		queue2.waitForAccess(thread1);

		if (s.getThreadState(thread1).getEffectivePriority() == 1
				&& s.getThreadState(thread2).getEffectivePriority() == 1
				&& s.getThreadState(thread4).getEffectivePriority() == 1)
			Lib.debug(dbgPS,
					"Task 5 Test 1 [PASS] Setting Default Effective Priority Correctly");
		else
			Lib.debug(dbgPS,
					"Task 5 Test 1 [FAIL] Setting Default Effective Prioirty Correctly");

		s.getThreadState(thread2).setPriority(3);

		if (s.getThreadState(thread2).getEffectivePriority() == 3
				&& s.getThreadState(thread4).getEffectivePriority() == 3)
			Lib.debug(dbgPS,
					"Task 5 Test 2 [PASS] Simple Effective Priority Donation");
		else
			Lib.debug(dbgPS,
					"Task 5 Test 2 [FAIL] Simple Effective Prioirty Donation");

		queue.waitForAccess(thread3);
		s.getThreadState(thread3).setPriority(5);

		if (s.getThreadState(thread2).getEffectivePriority() == 3
				&& s.getThreadState(thread3).getEffectivePriority() == 5
				&& s.getThreadState(thread1).getEffectivePriority() == 5
				&& s.getThreadState(thread4).getEffectivePriority() == 5)
			Lib.debug(dbgPS,
					"Task 5 Test 3 [PASS] Simple Transitive Effective Priority Donation");
		else
			Lib.debug(dbgPS,
					"Task 5 Test 3 [FAIL] Simple Transitive Effective Prioirty Donation");

		s.getThreadState(thread3).setPriority(2);

		if (s.getThreadState(thread2).getEffectivePriority() == 3
				&& s.getThreadState(thread3).getEffectivePriority() == 2
				&& s.getThreadState(thread1).getEffectivePriority() == 5
				&& s.getThreadState(thread4).getEffectivePriority() == 5)
			Lib.debug(dbgPS,
					"Task 5 Test 4 [PASS] Lowering Priority Does Not Effect Ordering");
		else
			Lib.debug(dbgPS,
					"Task 5 Test 4 [FAIL] Lowering Prioirty Does Not Effect Ordering");

		s.getThreadState(kt_1).setPriority(6);

		// Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(kt_4)==6);

		if (s.getThreadState(kt_1).getEffectivePriority() == 6
				&& s.getThreadState(kt_2).getEffectivePriority() == 6
				&& s.getThreadState(kt_3).getEffectivePriority() == 6
				&& s.getThreadState(kt_4).getEffectivePriority() == 6)
			Lib.debug(dbgPS, "Task 5 Test 5 [PASS] 3 layer Transitive Test");
		else
			Lib.debug(dbgPS,
					"Task 5 Test 5 [FAIL] 3 layer Transitive Test Thread1="
							+ s.getThreadState(kt_1).getEffectivePriority()
							+ " Thread2= "
							+ s.getThreadState(kt_2).getEffectivePriority()
							+ " Thread3= "
							+ s.getThreadState(kt_3).getEffectivePriority()
							+ " Thread4= "
							+ s.getThreadState(kt_4).getEffectivePriority());

		KThread kt_5 = new KThread();
		kt_5.setName("Thread 5");

		ThreadedKernel.scheduler.setPriority(kt_5, 7);

		tq1.waitForAccess(kt_5);

		// Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(kt_4)==7);

		KThread returnThread = tq1.nextThread();

		if (returnThread.getName() == "Thread 5")
			Lib.debug(dbgPS,
					"Task 5 Test 6 [PASS] Priority Scheduler Comparator");
		else
			Lib.debug(dbgPS,
					"Task 5 Test 6 [FAIL]  Priority Scheduler Comparator Thread Name="
							+ returnThread.getName());

		if (s.getThreadState(kt_1).getEffectivePriority() == 6
				&& s.getThreadState(kt_2).getEffectivePriority() == 7
				&& s.getThreadState(kt_3).getEffectivePriority() == 7
				&& s.getThreadState(kt_4).getEffectivePriority() == 7
				&& s.getThreadState(kt_5).getEffectivePriority() == 7)
			Lib.debug(dbgPS, "Task 5 Test 7 [PASS] 4 Layer Transitive Test");
		else
			Lib.debug(dbgPS,
					"Task 5 Test 7 [FAIL]  4 Layer Transitive Test Thread1="
							+ s.getThreadState(kt_1).getEffectivePriority()
							+ " Thread2= "
							+ s.getThreadState(kt_2).getEffectivePriority()
							+ " Thread3= "
							+ s.getThreadState(kt_3).getEffectivePriority()
							+ " Thread4= "
							+ s.getThreadState(kt_4).getEffectivePriority()
							+ " Thread5= "
							+ s.getThreadState(kt_5).getEffectivePriority());

		KThread secondReturnedThread = tq1.nextThread();

		if (secondReturnedThread.getName() == "Thread 1")
			Lib.debug(dbgPS, "Task 5 Test 8 [PASS] Second NextThread call");
		else
			Lib.debug(dbgPS,
					"Task 5 Test 8 [FAIL] Second NextThread call Thread Name="
							+ secondReturnedThread.getName());

		KThread thirdReturnedThread = tq1.nextThread();

		if (thirdReturnedThread == null)
			Lib.debug(dbgPS,
					"Task 5 Test 9 [PASS] Has holder Empty Queue NextThread call");
		else
			Lib.debug(dbgPS,
					"Task 5 Test 9 [FAIL] Has holder Empty Queue NextThread call Thread Name="
							+ secondReturnedThread.getName());

		KThread fourthReturnedThread = tq1.nextThread();

		if (fourthReturnedThread == null)
			Lib.debug(dbgPS,
					"Task 5 Test 10 [PASS] No holder Empty Queue NextThread call second");
		else
			Lib.debug(
					dbgPS,
					"Task 5 Test 10 [FAIL] No holder Empty Queue NextThread call second Thread Name="
							+ fourthReturnedThread.getName());

		tq1.waitForAccess(kt_5);

		try {
			returnThread = tq1.nextThread();
			if (returnThread.getName() == "Thread 5")
				Lib.debug(dbgPS,
						"Task 5 Test 11 [PASS] No holder Next Thread call on Non-empty Queue");
		}

		catch (Exception e) {
			Lib.debug(dbgPS,
					"Task 5 Test 11 [FAIL] No holder Next Thread call on Non-empty Queue");
		}

		Machine.interrupt().restore(intStatus);

		tieBreakingTests();
		infiniteLoopTest();

	}

	public static void tieBreakingTests() {

		boolean intStatus = Machine.interrupt().disable();
		Lib.enableDebugFlags(dbgPS + "");
		PriorityScheduler s = new PriorityScheduler();
		ThreadQueue queue = s.newThreadQueue(true);

		KThread k1 = new KThread();
		k1.setName("Thread 1");
		KThread k2 = new KThread();
		k2.setName("Thread 2");
		KThread k3 = new KThread();
		k3.setName("Thread 3");

		queue.acquire(k1);

		ThreadedKernel.scheduler.setPriority(k2, 6);
		queue.waitForAccess(k2);

		ThreadedKernel.scheduler.setPriority(k3, 7);
		queue.waitForAccess(k3);

		ThreadedKernel.scheduler.setPriority(k2, 7);

		KThread returnThread = queue.nextThread();
		KThread secondReturnedThread = queue.nextThread();

		if (returnThread.getName() == "Thread 2"
				&& secondReturnedThread.getName() == "Thread 3")
			Lib.debug(dbgPS, "Task 5 Test 12 [PASS] Tie Breaking Test ");
		else
			Lib.debug(dbgPS,
					"Task 5 Test 12 [FAIL] Tie Breaking Test Thread Name = "
							+ returnThread.getName() + " Second Thread Name = "
							+ secondReturnedThread);
	}

	public static void infiniteLoopTest() {

		boolean intStatus = Machine.interrupt().disable();
		Lib.enableDebugFlags(dbgPS + "");
		PriorityScheduler s = new PriorityScheduler();
		ThreadQueue queue = s.newThreadQueue(true);
		ThreadQueue queue2 = s.newThreadQueue(true);

		KThread k1 = new KThread();
		k1.setName("Thread 1");
		KThread k2 = new KThread();
		k2.setName("Thread 2");
		KThread k3 = new KThread();
		k3.setName("Thread 3");

		queue.waitForAccess(k2);
		queue2.acquire(k3);

		queue.waitForAccess(k3);

		ThreadedKernel.scheduler.setPriority(k3, 7);

		KThread returnThread = queue.nextThread();

		queue2.waitForAccess(k2);

		ThreadedKernel.scheduler.setPriority(k2, 6);

		// KThread secondReturnedThread = queue.nextThread();

		Lib.debug(dbgPS, "Task 5 Test 13 [PASS] Infinite Loop Test ");

	}
}
