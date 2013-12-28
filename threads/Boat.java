package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    
    public static int AdultsOnOahu;
    public static int ChildrenOnOahu;
    public static int AdultsOnMolokai;
    public static int ChildrenOnMolokai;
    public static int boat_here;
    public static int boat_row;
    public static int boat_ride;
    public static int MemoryOfOahu;
    public static Lock ActionLock;
    public static Communicator BoatTalk;
    public static Condition Done;
      
    public static void selfTest()
    {
    	BoatGrader b = new BoatGrader();
 
    	//System.out.println("\n ***Testing Boats with only 2 children***");
    	//begin(0, 2, b);

    	//System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
    	//begin(1, 2, b);

    	//System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
    	//begin(3, 3, b);
   
    	//System.out.println("\n ***Testing Boats with 2 children, 3 adults***");
    	//begin(3, 2, b);
   
    	//System.out.println("\n ***Testing Boats with 8 children, 4 adults***");
    	//begin(4, 8, b);
   
    	//System.out.println("\n ***Testing Boats with only 6 children***");
    	//begin(0, 6, b);
   
    	//System.out.println("\n ***Testing Boats with 4 children, 2 adults***");
    	//begin(2, 4, b);
   
    	//System.out.println("\n ***Testing Boats with 10 children, 10 adults***");
    	//begin(10, 10, b);
   
    	//System.out.println("\n ***Testing Boats with 100 children, 100 adults!***");
    	//begin(100, 100, b);
    }

    public static void begin(int adults, int children, BoatGrader b)
    {
     // Store the externally generated autograder in a class
     // variable to be accessible by children.
     bg = b;

     // Instantiate global variables here
     assert children >= 2;
     AdultsOnOahu = 0;
     ChildrenOnOahu = 0;
     AdultsOnMolokai = 0;
     ChildrenOnMolokai = 0;
     boat_here = 1;
     boat_row = 0;
     boat_ride = 0;
     MemoryOfOahu = 1;
     ActionLock = new Lock();
     BoatTalk = new Communicator();
     Done = new Condition(ActionLock);
  
  // Create threads here. See section 3.4 of the Nachos for Java
  // Walkthrough linked from the projects page.

     Runnable AdultBlueprint = new AdultRunnable();
  	 Runnable ChildBlueprint = new ChildRunnable();
  	 ActionLock.acquire();
  	 int counter = 0;
  	 KThread nextThread;
  	 while(counter < children)
  	 {
  		 nextThread = new KThread(ChildBlueprint);
  		 nextThread.fork();
  		 counter++;
  	 }
  	 counter = 0;
  	 while(counter < adults)
  	 {
  		 nextThread = new KThread(AdultBlueprint);
  		 nextThread.fork();
  		 counter++;
  	 }
  	 while(true)
  	 {
  		 Done.sleep();
  		 if (AdultsOnMolokai == adults && ChildrenOnMolokai == children)
  		 {
  			 return;
  		 }
  	 }
    }

 /* This is where you should put your solutions. Make calls
    to the BoatGrader to show that it is synchronized. For
    example:
        bg.AdultRowToMolokai();
    indicates that an adult has rowed the boat across to Molokai
 */
    static void AdultItinerary()
    {
    	/*Initialization*/
    	ActionLock.acquire();
    	AdultsOnOahu++;
    	int island = 1;
    	int memory = 1;
    	ActionLock.release();
    	KThread.yield();
    	while(true)
    	{
    		while(island == 1) //Oahu Actions
    		{	
    			ActionLock.acquire();
    			if(ChildrenOnOahu <= 1)
    			{
    				if(boat_here == 1)
    				{
    					if(boat_row == 0)
    					{
    						AdultsOnOahu--;
    						memory = AdultsOnOahu + ChildrenOnOahu;
    						boat_row = 1;
    						bg.AdultRowToMolokai();
    						MemoryOfOahu = memory;
    						boat_here = 0;
    						island = 0;
    						boat_row = 0;
    						AdultsOnMolokai++;
    					}
    				}
    			}
    			if(ActionLock.isHeldByCurrentThread())
    			{
    				ActionLock.release();
    			}
    			KThread.yield();
    		}
    		while(island == 0) //Molokai Actions
    		{
    			ActionLock.acquire();
    			if(ChildrenOnMolokai >= 1)
    			{
    				ActionLock.release();
    				KThread.finish();
    			}
    			else
    			{
    				if(boat_here == 0)
    				{
    					if(boat_row == 0)
    					{
    						AdultsOnMolokai--;
    						boat_row = 1;
    						bg.AdultRowToOahu();
    						boat_here = 1;
    						island = 1;
    						boat_row = 0;
    						AdultsOnOahu++;
    					}
    				}    
    			}
    			if(ActionLock.isHeldByCurrentThread())
    			{
    				ActionLock.release();
    			}
    			KThread.yield();
    		}
    	}
    }

    static void ChildItinerary()
    {
    	/*Initialization*/
    	ActionLock.acquire();
    	ChildrenOnOahu++;
    	int island = 1;
    	int memory = 1;
    	ActionLock.release();
    	KThread.yield();
    	while(true)
    	{
    		while(island == 0) //Molokai Actions
    		{	
    			ActionLock.acquire();
    			if(MemoryOfOahu != 0)
    			{
    				if(boat_here == 0)
    				{
    					if(boat_row == 0)
    					{
    						ChildrenOnMolokai--;
    						boat_row = 1;
    						bg.ChildRowToOahu();
    						boat_here = 1;
    						island = 1;
    						boat_row = 0;
    						ChildrenOnOahu++;
    					}	
    				}
    			}
    			if(ActionLock.isHeldByCurrentThread())
    			{
    				ActionLock.release();
    			}
    			KThread.yield();
    		}
    		while(island == 1) //Oahu Actions
    		{
    			ActionLock.acquire();
    			if(ChildrenOnOahu > 1)
    			{
    				if(boat_here == 1)
    				{
    					if(boat_row == 0)
    					{
    						boat_row = 1;
    						ActionLock.release();
    						BoatTalk.listen();
    						ActionLock.acquire();
    						ChildrenOnOahu--;
    						bg.ChildRowToMolokai();
    						island = 0;
    						ChildrenOnMolokai++;
    						ActionLock.release();
    						BoatTalk.speak(1);
    					}
    					else if(boat_ride == 0)
    					{
    						boat_ride = 1;
    						ActionLock.release();
    						BoatTalk.speak(1);
    						BoatTalk.listen();
    						ActionLock.acquire();
    						ChildrenOnOahu--;
    						memory = AdultsOnOahu + ChildrenOnOahu;
    						bg.ChildRideToMolokai();
    						MemoryOfOahu = memory;
    						island = 0;
    						ChildrenOnMolokai++;
    						Done.wake();
    						boat_here = 0;
    						boat_row = 0;
    						boat_ride = 0;
    					}
    				}
    			}
    			if(ActionLock.isHeldByCurrentThread())
    			{
    				ActionLock.release();
    			}
    			KThread.yield();
    		}
    	}
    }

    static void SampleItinerary()
    {
 // Please note that this isn't a valid solution (you can't fit
 // all of them on the boat). Please also note that you may not
 // have a single thread calculate a solution and then just play
 // it back at the autograder -- you will be caught.
 System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
 bg.AdultRowToMolokai();
 bg.ChildRideToMolokai();
 bg.AdultRideToMolokai();
 bg.ChildRideToMolokai();
    }
    }
    
   class AdultRunnable implements Runnable
    {
     public void run()
     {
      Boat.AdultItinerary();
     }
    }
    
    class ChildRunnable implements Runnable
    {
     public void run()
     {
      Boat.ChildItinerary();
     }
    }
