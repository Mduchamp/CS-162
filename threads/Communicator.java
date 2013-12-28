package nachos.threads;

import java.util.*;

import org.omg.CORBA.PRIVATE_MEMBER;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
	Queue<Integer> messages = new LinkedList<Integer>();
	boolean isActiveSpeaker;
	boolean isActiveListener;
	boolean isActivePartner;
	Lock lock;
	Condition speakers;
	Condition listeners;
	Condition partner;
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    	lock = new Lock();
    	speakers = new Condition(lock);
    	listeners = new Condition(lock);
    	partner = new Condition(lock);
    	isActiveSpeaker = false;
    	isActiveListener = false;
    	isActivePartner = false;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
    	lock.acquire();
    	while(isActiveSpeaker)
    		speakers.sleep();
    	isActiveSpeaker = true;
    	
    	messages.add(word);
    	
    	if(isActivePartner) {
    		partner.wake();
    		isActivePartner = false;
    	}
    	else {
    		isActivePartner = true;
    		partner.sleep();
    	}
    	speakers.wake();
    	isActiveSpeaker = false;
    	lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
    	lock.acquire();
    	int receivedMessage;
    	while(isActiveListener)
    		listeners.sleep();
    	isActiveListener = true;
    	
    	if(isActivePartner) {
    		partner.wake();
    		isActivePartner = false;
    	}
    	else {
    		isActivePartner = true;
    		partner.sleep();
    	}
    	
    	receivedMessage = (Integer) messages.remove();
    	
    	listeners.wake();
    	isActiveListener = false;
    	lock.release();
    	return receivedMessage; 
    }
    
    private static final char dbgCommunicatorTest = 'c';
    public static void selfTest() {
    	System.out.println();
    	Lib.enableDebugFlags(dbgCommunicatorTest + "");
    	
		Lib.debug(dbgCommunicatorTest, "TASK 4: 4 Threads, All Possible Orders");
		final Communicator comm = new Communicator();
		KThread speaker1;
		KThread speaker2;
		KThread listener1;
		KThread listener2;
		
		Lib.debug(dbgCommunicatorTest, "TASK 4: Order1");
		speaker1 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Speaker1 begin speaking: 2");
				comm.speak(2);
				System.out.println("Speaker1 finished speaking");
			}
		});
		speaker2 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Speaker2 begin speaking: 99");
				comm.speak(99);
				System.out.println("Speaker2 finished speaking");
			}
		});
		listener1 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Listener1 begin listening");
				int message = comm.listen();
				System.out.println("Listener1 finished listening: " + message);
			}
		});
		listener2 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Listener2 begin listening");
				int message = comm.listen();
				System.out.println("Listener2 finished listening: " + message);
			}
		});

		listener1.setName("Listener1");
		listener2.setName("Listener2");
		speaker1.setName("Speaker1");
		speaker2.setName("Speaker2");
		
		speaker1.fork();
		speaker2.fork();
		listener1.fork();
		listener2.fork();
		
		speaker1.join();
		speaker2.join();
		listener1.join();
		listener2.join();
		Lib.debug(dbgCommunicatorTest, "[PASS]" + ": You didn't get stuck.");
		
		Lib.debug(dbgCommunicatorTest, "TASK 4: Order2");
		speaker1 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Speaker1 begin speaking: 2");
				comm.speak(2);
				System.out.println("Speaker1 finished speaking");
			}
		});
		speaker2 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Speaker2 begin speaking: 99");
				comm.speak(99);
				System.out.println("Speaker2 finished speaking");
			}
		});
		listener1 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Listener1 begin listening");
				int message = comm.listen();
				System.out.println("Listener1 finished listening: " + message);
			}
		});
		listener2 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Listener2 begin listening");
				int message = comm.listen();
				System.out.println("Listener2 finished listening: " + message);
			}
		});

		listener1.setName("Listener1");
		listener2.setName("Listener2");
		speaker1.setName("Speaker1");
		speaker2.setName("Speaker2");
		
		speaker1.fork();
		listener1.fork();
		speaker2.fork();
		listener2.fork();
		
		speaker1.join();
		speaker2.join();
		listener1.join();
		listener2.join();
		Lib.debug(dbgCommunicatorTest, "[PASS]" + ": You didn't get stuck.");
		
		Lib.debug(dbgCommunicatorTest, "TASK 4: Order3");
		speaker1 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Speaker1 begin speaking: 2");
				comm.speak(2);
				System.out.println("Speaker1 finished speaking");
			}
		});
		speaker2 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Speaker2 begin speaking: 99");
				comm.speak(99);
				System.out.println("Speaker2 finished speaking");
			}
		});
		listener1 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Listener1 begin listening");
				int message = comm.listen();
				System.out.println("Listener1 finished listening: " + message);
			}
		});
		listener2 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Listener2 begin listening");
				int message = comm.listen();
				System.out.println("Listener2 finished listening: " + message);
			}
		});

		listener1.setName("Listener1");
		listener2.setName("Listener2");
		speaker1.setName("Speaker1");
		speaker2.setName("Speaker2");
		
		speaker1.fork();
		listener1.fork();
		listener2.fork();
		speaker2.fork();
		
		speaker1.join();
		speaker2.join();
		listener1.join();
		listener2.join();
		Lib.debug(dbgCommunicatorTest, "[PASS]" + ": You didn't get stuck.");
		
		Lib.debug(dbgCommunicatorTest, "TASK 4: Order4");
		speaker1 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Speaker1 begin speaking: 2");
				comm.speak(2);
				System.out.println("Speaker1 finished speaking");
			}
		});
		speaker2 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Speaker2 begin speaking: 99");
				comm.speak(99);
				System.out.println("Speaker2 finished speaking");
			}
		});
		listener1 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Listener1 begin listening");
				int message = comm.listen();
				System.out.println("Listener1 finished listening: " + message);
			}
		});
		listener2 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Listener2 begin listening");
				int message = comm.listen();
				System.out.println("Listener2 finished listening: " + message);
			}
		});

		listener1.setName("Listener1");
		listener2.setName("Listener2");
		speaker1.setName("Speaker1");
		speaker2.setName("Speaker2");
		
		listener1.fork();
		listener2.fork();
		speaker1.fork();
		speaker2.fork();
		
		speaker1.join();
		speaker2.join();
		listener1.join();
		listener2.join();
		Lib.debug(dbgCommunicatorTest, "[PASS]" + ": You didn't get stuck.");
		
		Lib.debug(dbgCommunicatorTest, "TASK 4: Order5");
		speaker1 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Speaker1 begin speaking: 2");
				comm.speak(2);
				System.out.println("Speaker1 finished speaking");
			}
		});
		speaker2 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Speaker2 begin speaking: 99");
				comm.speak(99);
				System.out.println("Speaker2 finished speaking");
			}
		});
		listener1 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Listener1 begin listening");
				int message = comm.listen();
				System.out.println("Listener1 finished listening: " + message);
			}
		});
		listener2 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Listener2 begin listening");
				int message = comm.listen();
				System.out.println("Listener2 finished listening: " + message);
			}
		});

		listener1.setName("Listener1");
		listener2.setName("Listener2");
		speaker1.setName("Speaker1");
		speaker2.setName("Speaker2");
		
		listener1.fork();
		speaker1.fork();
		listener2.fork();
		speaker2.fork();
		
		speaker1.join();
		speaker2.join();
		listener1.join();
		listener2.join();
		Lib.debug(dbgCommunicatorTest, "[PASS]" + ": You didn't get stuck.");
		
		Lib.debug(dbgCommunicatorTest, "TASK 4: Order6");
		speaker1 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Speaker1 begin speaking: 2");
				comm.speak(2);
				System.out.println("Speaker1 finished speaking");
			}
		});
		speaker2 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Speaker2 begin speaking: 99");
				comm.speak(99);
				System.out.println("Speaker2 finished speaking");
			}
		});
		listener1 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Listener1 begin listening");
				int message = comm.listen();
				System.out.println("Listener1 finished listening: " + message);
			}
		});
		listener2 = new KThread(new Runnable() {
			public void run() {
				System.out.println("Listener2 begin listening");
				int message = comm.listen();
				System.out.println("Listener2 finished listening: " + message);
			}
		});

		listener1.setName("Listener1");
		listener2.setName("Listener2");
		speaker1.setName("Speaker1");
		speaker2.setName("Speaker2");
		
		listener1.fork();
		speaker1.fork();
		speaker2.fork();
		listener2.fork();
		
		speaker1.join();
		speaker2.join();
		listener1.join();
		listener2.join();
		Lib.debug(dbgCommunicatorTest, "[PASS]" + ": You didn't get stuck.");
		
		Lib.debug(dbgCommunicatorTest, "TASK 4: Stress Test");
		for(int i = 0; i < 20; i++) {
			KThread speak = new KThread(new Runnable() {
				public void run() {
					comm.speak(5);
				}
			});
			speak.setName("Speaker"+i);
			speak.fork();
		}
		
		for(int i = 0; i < 20; i++) {
			KThread listen = new KThread(new Runnable() {
				public void run() {
					int message = comm.listen();
				}
			});
			listen.setName("Listen"+i);
			listen.fork();
			listen.join();
		}
		Lib.debug(dbgCommunicatorTest, "[PASS]" + ": You didn't get stuck.");
		
		System.out.println();
    }
}
