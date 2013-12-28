package nachos.threads;

import java.util.HashSet;
import java.util.Random;

import nachos.machine.*;

/**
 * A scheduler that chooses threads using a lottery.
 * 
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 * 
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 * 
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking the
 * maximum).
 */

//extending scheduler rather than priority scheduler because our priority scheduler was still broken, fixed methods I needed and reimplemented in this class iteself
public class LotteryScheduler extends Scheduler {
        /**
         * Allocate a new lottery scheduler.
         */
        public LotteryScheduler() {
        }

        public int getPriority(KThread thread) {
        	return getLotteryThreadState(thread).getTickets();
        }

        public int getEffectivePriority(KThread thread) {
        	return getLotteryThreadState(thread).getEffectiveTickets();
        }

        public void setPriority(KThread thread, int priority) {
                LotteryThreadState lts = getLotteryThreadState(thread);
                if (priority != lts.getTickets()) {
                	lts.setTickets(priority);
                }
        }

        public boolean increasePriority() {
        	boolean intStatus = Machine.interrupt().disable(), returnBool = true;
        	KThread thread = KThread.currentThread();
        	int priority = getPriority(thread);
        	if (priority == priorityMaximum) {
        		returnBool = false;	
        	} else {
        		setPriority(thread, priority + 1);
        	}
            Machine.interrupt().restore(intStatus);
            return returnBool;
        }

        public boolean decreasePriority() {
        	boolean intStatus = Machine.interrupt().disable(), returnBool = true;
        	KThread thread = KThread.currentThread();
        	int priority = getPriority(thread);
        	if (priority == priorityMinimum) {
        		returnBool = false;
        	} else {
        		setPriority(thread, priority - 1);
        	}
        	Machine.interrupt().restore(intStatus);
        	return returnBool;
        }

        static final int priorityDefault = 1;

        static final int priorityMinimum = 1;

        static final int priorityMaximum = Integer.MAX_VALUE;

        protected LotteryThreadState getLotteryThreadState(KThread thread) {
        	if (thread.schedulingState == null) {
        		thread.schedulingState = new LotteryThreadState(thread);
        	}
        	return (LotteryThreadState) thread.schedulingState;
        }

        @Override
        public ThreadQueue newThreadQueue(boolean transferPriority) {
        	return new LotteryQueue(transferPriority);
        }

        protected class LotteryQueue extends ThreadQueue {
        	LotteryQueue(boolean transferSecondT) {
        		transferT = transferSecondT;
        	}
                
        	@Override public void print() {
        		//does not need to do anything, just here for compiler fix.
        	}

        	@Override
        	public void waitForAccess(KThread thread) {
        		Lib.assertTrue(Machine.interrupt().disabled());
        		getLotteryThreadState(thread).waitForAccess(this);
        	}

        	@Override
        	public KThread nextThread() {
        		Lib.assertTrue(Machine.interrupt().disabled());
        		if (waiting.isEmpty()) {
        			return null;
        		} else {
        			int ticketI = randomGenerator.nextInt(totalT)+1;
        			KThread returnThread = null;
        			for (LotteryThreadState lts : waiting) {
        				ticketI -= lts.getEffectiveTickets();
        				if (ticketI <= 0) {
        					returnThread = lts.thread;
        					lts.acquire(this);
        					break;
        				}
        			}
        			return returnThread;
        		}
        	}

        	@Override
        	public void acquire(KThread thread) {
        		getLotteryThreadState(thread).acquire(this);
        	}

        	Random randomGenerator = new Random();

        	void updateEffT() {
        		int temp = 0;
        		for (LotteryThreadState lts : waiting) {
        			temp += lts.getEffectiveTickets();
        		}
        		totalT = temp;
        	}

        	void removeFromWaiting(LotteryThreadState lotteryThreadState) {
        		if (waiting.remove(lotteryThreadState)) {
        			updateEffT();
        			if (lockingThread != null) {
        				lockingThread.completeUpdateEffT();
        			}
        		}
        	}
                
        	private LotteryThreadState lockingThread; 
        	
        	private HashSet<LotteryThreadState> waiting = new HashSet<LotteryThreadState>();

        	private boolean transferT;

        	private int totalT;
        }

        protected static class LotteryThreadState {

        	LotteryThreadState(KThread thread2) {
        		thread = thread2;
        	}

        	void acquire(LotteryQueue lotteryQueue) {
        		if (lotteryQueue.lockingThread != this) {
        			if (lotteryQueue.lockingThread != null) {
        				lotteryQueue.lockingThread.release(lotteryQueue);
        			}
        			lotteryQueue.removeFromWaiting(this);
        			lotteryQueue.lockingThread = this;
        			acquired.add(lotteryQueue);
        			waiting.remove(lotteryQueue);
        			completeUpdateEffT();
        		}
        	}

        	private void release(LotteryQueue lotteryQueue) {
        		if (lotteryQueue.lockingThread == this) {
        			lotteryQueue.lockingThread = null;
        			completeUpdateEffT();
        		}
        	}

        	void waitForAccess(LotteryQueue lotteryQueue) {
        		release(lotteryQueue);
        		if (!lotteryQueue.waiting.contains(this)) {
        			waiting.add(lotteryQueue);
        			lotteryQueue.waiting.add(this);
        			if (lotteryQueue.transferT && lotteryQueue.lockingThread != null) {
        				lotteryQueue.lockingThread.completeUpdateEffT();
        			} else {
        				lotteryQueue.updateEffT();
                    }
        		}
        	}

        	int getEffectiveTickets() {
        		return effectiveTickets;
        	}

        	int getTickets() {
        		return tickets;
        	}

        	void setTickets(int tickets2) {
        		tickets = tickets2;
        		completeUpdateEffT();
        	}

        	private void completeUpdateEffT() {
        		int temp = tickets;
        		for (LotteryQueue lq : acquired) {
        			if (lq.transferT) {
        				for (LotteryThreadState lts : lq.waiting) {
        					temp += lts.effectiveTickets;
        				}
        			}
        		}
        		effectiveTickets = temp;
        		for (LotteryQueue lq : waiting) {
        			lq.updateEffT();
        			if (lq.transferT && lq.lockingThread != null) {
        				lq.lockingThread.completeUpdateEffT();
        			}
        		}
        	}

        	private int tickets = priorityDefault;

        	private int effectiveTickets = priorityDefault;
        	
        	private KThread thread;
                
        	private HashSet<LotteryQueue> acquired = new HashSet<LotteryQueue>();

        	private HashSet<LotteryQueue> waiting = new HashSet<LotteryQueue>();
        }
        
        public static void selfTest() {
            if (ThreadedKernel.scheduler != null && ThreadedKernel.scheduler instanceof LotteryScheduler) {
            	boolean disabled = Machine.interrupt().disable();
                    
            	KThread k5 = new KThread(); 
            	KThread	k6 = new KThread(); 
            	KThread	k7 = new KThread(); 
            	KThread	k8 = new KThread();
            	LotteryQueue lq4 = (LotteryQueue)ThreadedKernel.scheduler.newThreadQueue(false); 
            	LotteryQueue lq5 = (LotteryQueue)ThreadedKernel.scheduler.newThreadQueue(false); 
            	LotteryQueue lq6 = (LotteryQueue)ThreadedKernel.scheduler.newThreadQueue(false);
            	
            	lq4.acquire(k5);
            	lq5.acquire(k6);
            	lq6.acquire(k7);
            	
            	lq4.waitForAccess(k6);
            	lq5.waitForAccess(k7);
            	lq6.waitForAccess(k8);
            	
            	((LotteryThreadState)k5.schedulingState).setTickets(100);
            	((LotteryThreadState)k6.schedulingState).setTickets(100);
            	((LotteryThreadState)k7.schedulingState).setTickets(100);
            	((LotteryThreadState)k8.schedulingState).setTickets(100);
            	
            	Lib.assertTrue(((LotteryThreadState)k5.schedulingState).getEffectiveTickets() == 100);
            	Lib.assertTrue(((LotteryThreadState)k6.schedulingState).getEffectiveTickets() == 100);
            	Lib.assertTrue(((LotteryThreadState)k7.schedulingState).getEffectiveTickets() == 100);
            	Lib.assertTrue(((LotteryThreadState)k8.schedulingState).getEffectiveTickets() == 100);
            	
            	
            	KThread k1 = new KThread();
            	KThread k2 = new KThread();
            	KThread k3 = new KThread();
            	KThread k4 = new KThread();
            	LotteryQueue lq1 = (LotteryQueue)ThreadedKernel.scheduler.newThreadQueue(true); 
            	LotteryQueue lq2 = (LotteryQueue)ThreadedKernel.scheduler.newThreadQueue(true); 
            	LotteryQueue lq3 = (LotteryQueue)ThreadedKernel.scheduler.newThreadQueue(true);
            	
            	lq1.acquire(k1);
            	lq2.acquire(k2);
            	lq3.acquire(k3);
            	
            	lq1.waitForAccess(k2);
            	lq2.waitForAccess(k3);
            	lq3.waitForAccess(k4);
            	
            	((LotteryThreadState)k1.schedulingState).setTickets(10);
            	((LotteryThreadState)k2.schedulingState).setTickets(10);
            	((LotteryThreadState)k3.schedulingState).setTickets(10);
            	((LotteryThreadState)k4.schedulingState).setTickets(10);
            	
            	Lib.assertTrue(((LotteryThreadState)k1.schedulingState).getEffectiveTickets() == 40);
            	Lib.assertTrue(((LotteryThreadState)k2.schedulingState).getEffectiveTickets() == 30);
            	Lib.assertTrue(((LotteryThreadState)k3.schedulingState).getEffectiveTickets() == 20);
            	Lib.assertTrue(((LotteryThreadState)k4.schedulingState).getEffectiveTickets() == 10);
            	
            	KThread k9 = new KThread(); 
            	KThread	k10 = new KThread(); 
            	KThread	k11 = new KThread(); 
            	KThread	k12 = new KThread();
            	
            	((LotteryThreadState)k9.schedulingState).setTickets(1);
            	((LotteryThreadState)k10.schedulingState).setTickets(2);
            	((LotteryThreadState)k11.schedulingState).setTickets(3);
            	((LotteryThreadState)k12.schedulingState).setTickets(4);
            	
            	Lib.assertTrue(((LotteryThreadState)k9.schedulingState).getEffectiveTickets() == 1);
            	Lib.assertTrue(((LotteryThreadState)k10.schedulingState).getEffectiveTickets() == 2);
            	Lib.assertTrue(((LotteryThreadState)k11.schedulingState).getEffectiveTickets() == 3);
            	Lib.assertTrue(((LotteryThreadState)k12.schedulingState).getEffectiveTickets() == 4);
                    
            	Machine.interrupt().restore(disabled);
            }
    }
}
