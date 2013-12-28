package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.util.*;
import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
 public static final int ROOT = 1;
 private static int ProcessCounter = 0;
 public int ProcessID;
 public UserProcess parent = null;
 public LinkedList<UserProcess> children;
 public int status = 0;
 public byte[] joinStatus = new byte[4];
 public UThread thread;
 public boolean exception = true;
 
 //Open files, max 16. [0] and [1] must be for stdin stdout
 OpenFile[] activeTable = new OpenFile[16];
 //Do something to reserve 0 and 1
 final int MAX_LENGTH_ARG = 256; //Max length of argument to system call

    /**
     * Allocate a new process.
     */
    public UserProcess() {
     //Initializing 0 and 1 for stdin stdout
     activeTable[0] = UserKernel.console.openForReading();
     activeTable[1] = UserKernel.console.openForWriting();
        ProcessCounter++;
    ProcessID = ProcessCounter;
    System.out.println(" IN UserProcess ProcessID= " + ProcessID);
    children = new LinkedList<UserProcess>();
    int numPhysPages = Machine.processor().getNumPhysPages();
    pageTable = new TranslationEntry[numPhysPages];
    for (int i=0; i<numPhysPages; i++)
    {
      pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
    }
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
 return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param name the name of the file containing the executable.
     * @param args the arguments to pass to the executable.
     * @return <tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
        System.out.println(" IN Execute name= " + name);
 if (!load(name, args))
     return false;
 
 thread = new UThread(this);
 thread.setName(name).fork();

 return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
 Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param vaddr the starting virtual address of the null-terminated
     *   string.
     * @param maxLength the maximum number of characters in the string,
     *    not including the null terminator.
     * @return the string read, or <tt>null</tt> if no null terminator was
     *  found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
 Lib.assertTrue(maxLength >= 0);

 byte[] bytes = new byte[maxLength+1];

 int bytesRead = readVirtualMemory(vaddr, bytes);

 for (int length=0; length<bytesRead; length++) {
     if (bytes[length] == 0)
  return new String(bytes, 0, length);
 }

 return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param vaddr the first byte of virtual memory to read.
     * @param data the array where the data will be stored.
     * @return the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
 return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param vaddr the first byte of virtual memory to read.
     * @param data the array where the data will be stored.
     * @param offset the first byte to write in the array.
     * @param length the number of bytes to transfer from virtual memory to
     *   the array.
     * @return the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
     int length) {
 Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
     Lib.debug(dbgProcess, "Read Virtual Memory start vaddr= "
     + vaddr + " offset= " + offset + " length " + length);
     System.out.println("Read Virtual Memory start vaddr= "
     + vaddr + " offset= " + offset + " length " + length);

 byte[] memory = Machine.processor().getMemory();

    if (vaddr < 0 || vaddr > Machine.processor().makeAddress(numPages -1, pageSize -1))
        return 0;
 if (length > Machine.processor().makeAddress(numPages-1, pageSize-1) - vaddr)
        length = Machine.processor().makeAddress(numPages-1, pageSize-1) - vaddr;

    int transferred = 0;
    int currentVPN = Machine.processor().pageFromAddress(vaddr);
    int currentOffset = Machine.processor().offsetFromAddress(vaddr);
    System.out.println("currentVPN= " + currentVPN + " currentOffset= " + currentOffset);
    while (transferred < data.length && length > 0){
        if (!pageTable[currentVPN].valid)
            break;
        int innerOffset = currentOffset % Machine.processor().pageSize;
        int paddr = Machine.processor().makeAddress(pageTable[currentVPN].ppn, innerOffset);
        int amount = Math.min(data.length - transferred, pageSize - innerOffset);
        int temp = data.length -transferred;
        int temp2 = pageSize - innerOffset;
        System.out.println("DL - Trans= " + temp + "page size - innerOffset= " + temp2);
        System.out.println("InnerOffset= " + innerOffset + " Paddr = " + paddr + " amount= " + amount);
        System.out.println("InnerOffset= " + innerOffset + " Paddr = " + paddr + " amount= " + amount);
        System.arraycopy(memory, paddr, data, offset, amount);
        if (amount > 0)
            pageTable[currentVPN].used = true;
        vaddr += amount;
        currentVPN = Machine.processor().pageFromAddress(vaddr);
        System.out.println("New currentVPN= " + currentVPN);
        currentOffset += amount;
        offset += amount;
        length -= amount;
        System.out.println("End length = " + length);
        transferred += amount;
    }
    return transferred;


    }
    

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param vaddr the first byte of virtual memory to write.
     * @param data the array containing the data to transfer.
     * @return the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
 return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param vaddr the first byte of virtual memory to write.
     * @param data the array containing the data to transfer.
     * @param offset the first byte to transfer from the array.
     * @param length the number of bytes to transfer from the array to
     *   virtual memory.
     * @return the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
      int length) {
 Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);
    Lib.debug(dbgProcess, "Write Virtual Memory start vaddr= "
     + vaddr + " offset= " + offset + " length " + length);
    System.out.println( "Write Virtual Memory start vaddr= "
     + vaddr + " offset= " + offset + " length " + length);

    byte[] memory = Machine.processor().getMemory();

    if (vaddr < 0 || vaddr > Machine.processor().makeAddress(numPages -1, pageSize -1))
        return 0;
    if (length > Machine.processor().makeAddress(numPages-1, pageSize-1) - vaddr)
        length = Machine.processor().makeAddress(numPages-1, pageSize-1) - vaddr;

    int transferred = 0;
    int currentVPN = Machine.processor().pageFromAddress(vaddr);
    int currentOffset = Machine.processor().offsetFromAddress(vaddr);
    System.out.println("currentVPN= " + currentVPN + " currentOffset= " + currentOffset);
    while (transferred < data.length && length > 0){
        if (!pageTable[currentVPN].valid)
            break;
        int innerOffset = currentOffset % Machine.processor().pageSize;
        int paddr = Machine.processor().makeAddress(pageTable[currentVPN].ppn, innerOffset);
        int amount = Math.min(data.length - transferred, pageSize - innerOffset);
        int temp = data.length -transferred;
        int temp2 = pageSize - innerOffset;
        System.out.println("DL - Trans= " + temp + "page size - innerOffset= " + temp2);
        System.out.println("InnerOffset= " + innerOffset + " Paddr = " + paddr + " amount= " + amount);
        System.arraycopy(data, offset, memory, paddr, amount);
        if (amount > 0){
            pageTable[currentVPN].used = true;
            pageTable[currentVPN].dirty = true;
        }
        vaddr += amount;
        currentVPN = Machine.processor().pageFromAddress(vaddr);
        currentOffset += amount;
        offset += amount;
        length -= amount;
        transferred += amount;
         Lib.debug(dbgProcess, "Write Virtual Memory Loop vaddr= "
     + vaddr + " offset= " + offset + " length= " + length + " transferred= " + transferred);
    }
    return transferred;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param name the name of the file containing the executable.
     * @param args the arguments to pass to the executable.
     * @return <tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
 Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
 
 OpenFile executable = ThreadedKernel.fileSystem.open(name, false);

    if (executable == null) {
     Lib.debug(dbgProcess, "\topen failed");
     return false;
 }

 try {
     coff = new Coff(executable);
 }
 catch (EOFException e) {
     executable.close();
     Lib.debug(dbgProcess, "\tcoff load failed");
     return false;
 }

 // make sure the sections are contiguous and start at page 0
 numPages = 0;
 for (int s=0; s<coff.getNumSections(); s++) {
     CoffSection section = coff.getSection(s);
     if (section.getFirstVPN() != numPages) {
  coff.close();
  Lib.debug(dbgProcess, "\tfragmented executable");
  return false;
     }
     numPages += section.getLength();
 }

 // make sure the argv array will fit in one page
 byte[][] argv = new byte[args.length][];
 int argsSize = 0;
 for (int i=0; i<args.length; i++) {
     argv[i] = args[i].getBytes();
     // 4 bytes for argv[] pointer; then string plus one for null byte
     argsSize += 4 + argv[i].length + 1;
 }
 if (argsSize > pageSize) {
     coff.close();
     Lib.debug(dbgProcess, "\targuments too long");
     return false;
 }

 // program counter initially points at the program entry point
 initialPC = coff.getEntryPoint(); 

 // next comes the stack; stack pointer initially points to top of it
 numPages += stackPages;
 initialSP = numPages*pageSize;

 // and finally reserve 1 page for arguments
 numPages++;

 if (!loadSections())
     return false;
 
 // store arguments in last page
 int entryOffset = (numPages-1)*pageSize;
 int stringOffset = entryOffset + args.length*4;

 this.argc = args.length;
 this.argv = entryOffset;
 

 for (int i=0; i<argv.length; i++) {
     byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
    
     Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
     
     entryOffset += 4;
     Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
         argv[i].length);
     
     stringOffset += argv[i].length;
     Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
      
     stringOffset += 1;
 }



 return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return <tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
       

        UserKernel.lock.acquire();
       
        if(UserKernel.freePages.size() < numPages){
                Lib.debug(dbgProcess, "\tinsufficient physical memory freePages= " + UserKernel.freePages.size());
                coff.close(); 
                UserKernel.lock.release();
                return false;   
        }
       
        pageTable = new TranslationEntry[numPages];
        
        for (int i = 0; i < numPages; i++){
            int ppn = UserKernel.usePage();
            pageTable[i] = new TranslationEntry(i, ppn, true, false, false, false);   
        }

        for (int s = 0; s < coff.getNumSections(); s++){
            CoffSection section = coff.getSection(s);
          //  Lib.debug(dbgProcess, "\tinitializing " + section.getName()
            //        + " section (" + section.getLength() + " pages) Pages Left= " + UserKernel.freePages.size());
            for (int i = 0; i<section.getLength(); i++){
                int vpn = section.getFirstVPN() + i;   
                pageTable[vpn].readOnly = section.isReadOnly(); //changed this line to vpn + i from vpn
                section.loadPage(i, pageTable[vpn].ppn);
               // System.out.println("PT read-only= " + pageTable[vpn].readOnly  + " Section read-only " + section.isReadOnly());
            }
        }

        System.out.println("Finished load sections");
        UserKernel.lock.release();
     
     return true;
 }


    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        UserKernel.lock.acquire();
        if (pageTable != null)
            for (int i = 0; i < pageTable.length; i++){
                UserKernel.freeUpPage(pageTable[i].ppn);
            }
        UserKernel.lock.release();
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
 Processor processor = Machine.processor();

 // by default, everything's 0
 for (int i=0; i<processor.numUserRegisters; i++)
     processor.writeRegister(i, 0);

 // initialize PC and SP according
 processor.writeRegister(Processor.regPC, initialPC);
 processor.writeRegister(Processor.regSP, initialSP);

 // initialize the first two argument registers to argc and argv
 processor.writeRegister(Processor.regA0, argc);
 processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {

 Machine.halt();
 
 Lib.assertNotReached("Machine.halt() did not halt machine!");
 return 0;
    }
    
    /*
     * Task 1: Create, Open, Read, Write, Close, Unlink Handles + Helper functions
     */
    int handleCreate(int filenameAddr) {
     return accessFile(filenameAddr, true);
    }

    int handleOpen(int filenameAddr) {
     return accessFile(filenameAddr, false);
    }
    
    int getEmptyTablePosition() {
     for (int i = 0; i < activeTable.length; i++) {
      if (activeTable[i] == null) {
       return i;
      }
     }
     return -1;
    }
    
    int accessFile(int filenameAddr, boolean isAutoCreate) {
         System.out.println("In AccessFile fileNameAddr= " + filenameAddr + " isAutoCreate= " + isAutoCreate);

         int fileDescriptor = getEmptyTablePosition();
         if (fileDescriptor == -1)
          return -1;
         System.out.println("FileDescriptor: "+fileDescriptor);
         String filename = readVirtualMemoryString(filenameAddr, MAX_LENGTH_ARG);
         if (filename == null)
          return -1;
     
     OpenFile openFile = UserKernel.fileSystem.open(filename, isAutoCreate);
     activeTable[fileDescriptor] = openFile;
     return fileDescriptor;
    }
    
    int handleRead(int fileDescriptor, int bufferAddr, int length) {
        System.out.println("In handleRead fileDescriptor= " + fileDescriptor + " bufferAddr= " + bufferAddr + " Length= " + length);
        if (fileDescriptor < 0 || fileDescriptor > 15 || length < 0)
         return -1;
        OpenFile openFile = activeTable[fileDescriptor];
        if (openFile == null)
         return -1;
        
        int numReadBytes;
        int numWriteBytes;
        int totalNumWriteBytes = 0;
        if (length <= 1024) {
   byte[] buffer = new byte[length];
   numReadBytes = openFile.read(buffer, 0, length);
   if (numReadBytes == -1)
    return -1;
   numWriteBytes = writeVirtualMemory(bufferAddr, buffer, 0, numReadBytes);
   if (numWriteBytes != numReadBytes)
    return -1;
   return numWriteBytes;
  } else {
   for(int countAddr = bufferAddr; countAddr-bufferAddr < length; countAddr += 1024) {
    byte[] buffer = new byte[1024];
    numReadBytes = openFile.read(buffer, 0, 1024);
    if (numReadBytes == -1)
     return -1;
    numWriteBytes = writeVirtualMemory(countAddr, buffer, 0, 1024);
    totalNumWriteBytes += numWriteBytes;
    if (numWriteBytes != numReadBytes)
     return -1;
    if (numReadBytes < 1024)
     return totalNumWriteBytes;
   }
   return totalNumWriteBytes;
  }
    }
    
    int handleWrite(int fileDescriptor, int bufferAddr, int size) {
         System.out.println("In handleWrite fileDescriptor= " + fileDescriptor + " bufferAddr= " + bufferAddr + " size= " + size);
	     if (fileDescriptor > 15 || fileDescriptor < 0 || size < 0) {
	    	 return -1;
	     }

    	OpenFile file = activeTable[fileDescriptor];
    	if (file == null) {
    		return -1;
    	}
    	
    	int numWriteBytes; 
    	int totalNumWriteBytes = 0;
    	int readBytes;
    	
    	if (size <= 1024) {
	    	byte[] buffer = new byte[size];
	    	readBytes = readVirtualMemory(bufferAddr, buffer);
	    	if (readBytes != size) {
				return -1;
			}
	
	    	numWriteBytes = file.write(buffer, 0, readBytes);
	    	if (numWriteBytes == -1) {
	    		return -1;
	    	}
	    	return numWriteBytes;
    	} else {
    		for(int countAddr = bufferAddr; countAddr-bufferAddr < size; countAddr += 1024) {
    			byte[] buffer = new byte[1024];
    			readBytes = readVirtualMemory(countAddr, buffer);
    			if (readBytes != size) {
    				return -1;
    			}
    			
    			numWriteBytes = file.write(buffer, 0, 1024);
    			if (numWriteBytes == -1) {
    	    		return -1;
    	    	}
    			totalNumWriteBytes += numWriteBytes;
    		}
    		return totalNumWriteBytes;
    	}
    }
    
    int handleClose(int fileDescriptor) {
        System.out.println("In handleClose fileDescriptor= " + fileDescriptor);
     if (fileDescriptor > 15 || fileDescriptor < 0) { 
      return -1;
     }
     OpenFile file = activeTable[fileDescriptor];
     if (file != null) { 
      file.close();
      activeTable[fileDescriptor] = null;
      return 0;
     } else { 
      return -1;
     }
    }
    
    int handleUnlink(int fileNameAddr) {
        System.out.println("In handleUnlink fileNameAddr= " + fileNameAddr);
     String fileName = readVirtualMemoryString(fileNameAddr, MAX_LENGTH_ARG);
     if (fileName == null) { 
      return -1;
     }
     if (ThreadedKernel.fileSystem.remove(fileName)) {
      return 0;
     } else { 
      return -1;
     }
    }
    /*
     * End Task 1
     */

    private static final int
 syscallHalt = 0,
 syscallExit = 1,
 syscallExec = 2,
 syscallJoin = 3,
 syscallCreate = 4,
 syscallOpen = 5,
 syscallRead = 6,
 syscallWrite = 7,
 syscallClose = 8,
 syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     *         </tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *        </tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *        </tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param syscall the syscall number.
     * @param a0 the first syscall argument.
     * @param a1 the second syscall argument.
     * @param a2 the third syscall argument.
     * @param a3 the fourth syscall argument.
     * @return the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
        System.out.println("Syscall= " + syscall);
  switch (syscall) {
   case syscallHalt:
       return handleHalt();
   
   /*Task 1 Handles*/
   case syscallExit:
    return handleExit(a0);
   case syscallExec: 
    return handleExec(a0, a1, a2);
   case syscallJoin:
    return handleJoin(a0, a1);
   case syscallCreate:
    return handleCreate(a0);
   case syscallOpen:
    return handleOpen(a0);
   case syscallRead:
    return handleRead(a0, a1, a2);
   case syscallWrite:
    return handleWrite(a0, a1, a2);
   case syscallClose:
    return handleClose(a0);
   case syscallUnlink:
    return handleUnlink(a0);
 
   default:
       Lib.debug(dbgProcess, "Unknown syscall " + syscall);
       Lib.assertNotReached("Unknown system call!");
  }
  return 0;
    }
    
    public int handleExec(int file, int argc, int argv)
    {
    System.out.println("In handleExec file= " + file + " argc= " + argc + " argv= " + argv);
     if(file < 0 || argc < 0 || argv < 0)
     {
      return -1;
     }
     String name = readVirtualMemoryString(file,256);
     if(name == null)
     {
      return -1;
     }
     String args[] = new String[argc];
     int byteSize;
     int address;
     byte recieved[] = new byte[4];
     int counter = 0;
     while(counter < argc)
     {
      byteSize = readVirtualMemory(argv+counter*4, recieved);
      if(byteSize != 4)
      {
       return -1;
      }
      address = Lib.bytesToInt(recieved,0);
      args[counter] = readVirtualMemoryString(address,256);
      if(args[counter] == null)
      {
       return -1;
      }
      counter++;
     }
     UserProcess child = new UserProcess();
     child.parent = this;
     this.children.add(child);
     if(child.execute(name, args))
     {
      return child.ProcessID;
     }
     return -1;
    }
    
    public int handleExit(int status)
    {
         System.out.println("In handleExit status= " + status);
     this.exception = false;
     this.status = status;
     while(this.children.size() != 0)
      {
        this.children.poll().parent = null;
      }
     int counter = 0;
     while(counter < 16)
     {
      handleClose(counter);
      counter++;
     }

     this.unloadSections();
     System.out.println("After unload sections ProcessID= " + this.ProcessID + "Root= " + ROOT);
     if(this.ProcessID == ROOT)
     {
      Kernel.kernel.terminate();
     }
     else
     {
      thread.finish();
     }
     System.out.println("in handle exit before return");
     return 0;
    }
    
    public int handleJoin(int ID, int status)
    {
         System.out.println("In handleJoin ID= " + ID + " status= " + status);
     if(ID <= 0 || status < 0)
     {
      return -1;
     }
     UserProcess child = null;
     int counter = 0;
     int size = this.children.size();
     UserProcess temp;
      System.out.println("In handleJoin 1 children size= " + size);
     while(counter < size)
     {
      temp = this.children.get(counter);
      if(temp.ProcessID == ID)
      {
        System.out.println("In handJoin child found with process same ID");
       child = temp;
       break;
      }
      counter++;
     }
     if(child == null)
     {
      return -1;
     }
     child.thread.join();
     this.children.remove(child);
     if(child.exception)
     {
       return 0;
     }
     Lib.bytesFromInt(this.joinStatus, 0, 4, child.status);
     System.out.println("Join status= " + joinStatus + " child status= " + child.status);
     int byteSize = writeVirtualMemory(status,this.joinStatus);
     return 1;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param cause the user exception that occurred.
     */
    public void handleException(int cause) {
 Processor processor = Machine.processor();

 switch (cause) {
 case Processor.exceptionSyscall:
     int result = handleSyscall(processor.readRegister(Processor.regV0),
           processor.readRegister(Processor.regA0),
           processor.readRegister(Processor.regA1),
           processor.readRegister(Processor.regA2),
           processor.readRegister(Processor.regA3)
           );
     processor.writeRegister(Processor.regV0, result);
     processor.advancePC();
     break;           
           
 default:
     Lib.debug(dbgProcess, "Unexpected exception: " +
        Processor.exceptionNames[cause]);
     Lib.assertNotReached("Unexpected exception");
 }
    }

    public static final char dbgUP = 'u';
    
    public static void selfTest(){
        Lib.enableDebugFlags(dbgUP + "");
        Lib.enableDebugFlags(dbgProcess + "");
        Lib.debug(dbgUP, "TASK 2: MultiProgramming Self Test");

       

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



       /* thread1.setTarget(new Runnable() {
            public void run() {
                String testStatus = "[PASS]";
                UserProcess up = null;
                String[] string = null;
                try {
                   up = new UserProcess();
                   string = new String[1];
                   string[0] = "asshole";
                   up.load("cat.coff", string);
                } catch (Error e) {
                    testStatus = "[FAIL]";
                }
                Lib.debug(dbgUP, "TEST 1 Task2: " + testStatus
                        + ": LoadSection is able to load");
                
                task2Test4();
                task2Test2(up, string);
                task2Test3(up, string);
               
            }
        });

        thread1.fork();
        thread1.join();
        */

       // task5Test2(thread4);
    }

    public static void task5Test2(KThread thread){
        thread.setTarget(new Runnable() {
            public void run() {
                String testStatus = "[PASS]";
                UserProcess up = null;
                String[] string = null;
                try {
                   up = new UserProcess();
                   string = new String[1];
                   string[0] = "asshole";
                   //up.load("cat.coff", string);
                   //up.load("cp.coff", string);
                   //up.load("matmult.coff", string);
                   //up.load("mv.coff", string);
                   //up.load("rm.coff", string);
                   up.load("sh.coff", string);
                   up.load("sort.coff", string);
                } catch (Error e) {
                    testStatus = "[FAIL]";
                }
                Lib.debug(dbgUP, "TEST 5 Task2: Testing all possible coffs");
            }
        });

        thread.fork();
        thread.join();
    }

    public static void task2Test2(UserProcess up, String[] string){
        
        int i = 0;
        int max = 0;       
        String testStatus = "[PASS]";
        i = 0; 
        max = UserKernel.freePages.size();
        do {
                if (up.load("echo.coff", string))
                    i += 15;
                else 
                    break;
            } 
            while (i < max + 20);
        if (i > max)
             testStatus = "[FAIL]";

        Lib.debug(dbgUP, "TEST 2 Task2: " + testStatus
                + ": Max=" + max + " #Allocated= " + i + " LoadSection Recognizes when resources have been exceeded");
    }


    public static void task2Test4 (){
        String testStatus = "[PASS]"; 
       

        KThread thread2 = new KThread();
        KThread thread3 = new KThread();

        int max = UserKernel.freePages.size();
        int i = 0, g = 0;

         thread2.setTarget(new Runnable() {
            public void run() {
                String testStatus = "[PASS]";
                UserProcess up1 = new UserProcess();
                int max = UserKernel.freePages.size();
                int i = 0;
                while(true){
                    if (up1.load("echo.coff", new String[]{"dkf"}))
                        i += up1.numPages;
                    else  
                        break;
               }
                if (up1.numPages < UserKernel.freePages.size()){
                    testStatus = "[FAIL]";
                }
                 Lib.debug(dbgUP, "TEST 4 Task2 Process 1: " + testStatus
            + ": Max=" + max + " #Allocated= " + i + " UnloadSections properly unloads resources with multiple processes");
               up1.unloadSections();
            }
        });

        thread3.setTarget(new Runnable() {
            public void run() {
                String testStatus = "[PASS]";
                UserProcess up2 = new UserProcess();
                int max = UserKernel.freePages.size();
                int i = 0;
                while(true){
                   if (up2.load("echo.coff", new String[]{"dkf"}))
                        i += up2.numPages;
                    else  
                        break;
                }
                if (up2.numPages < UserKernel.freePages.size()){
                    testStatus = "[FAIL]";
                }
                 Lib.debug(dbgUP, "TEST 4 Task2 Process 2: " + testStatus
            + ": Max=" + max + " #Allocated= " + i + " UnloadSections properly unloads resources with multiple processes");
               up2.unloadSections();
            }
        });

        thread2.fork();
        thread3.fork();
        thread3.join();
        thread2.join();
      
        if (i + g> max)
             testStatus = "[FAIL]";
        i += g;
       
   
    }
    public static void task2Test3(UserProcess up, String[] string){
             
        String testStatus = "[PASS]"; 
        int old = UserKernel.freePages.size();
        up.unloadSections();
        int new_ = UserKernel.freePages.size();
        if (old == new_)
             testStatus = "[FAIL]";

        Lib.debug(dbgUP, "TEST 3 Task2: " + testStatus
                + ": Before Size=" + old + " After Size= " + new_ + " UnloadSections properly unloads resources");
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
 
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
}
