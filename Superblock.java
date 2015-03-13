class SuperBlock 
{
      public int totalBlocks; // the number of disk blocks
      public int totalInodes; // the number of inodes
      public int freeList;    // the block number of the free list's head

      public SuperBlock( int diskSize ) 
      {
      	byte[] superBlock = new byte[Disk.blockSize];
      	SysLib.rawread( 0, superBlock );
      	totalBlocks = SysLib.bytes2int( superBlock, 0 );
      	totalInodes = SysLib.bytes2int( superBlock, 4 );
      	freeList = SysLib.bytes2int( superBlock, 8 );

      	if (totalBlocks == diskSize && totalInodes > 0 && freeList >=2)
      		return;
      	else {
      		totalBlocks = diskSize;
      		format(defaultInodeBlocks);
      	}
      }

      public sync() 
      {
      		//initliazing byte stream
      		byte backUpBlock = new byte[Disk.blockSize];

      		//converting the values to bytes and writing to the backUpBlock
      		SysLib.int2bytes(totalBlocks, backUpBlock, 0 );
      		SysLib.int2bytes(totalInodes, backUpBlock, 4 );
      		SysLib.int2bytes(freeList, backUpBlock, 8 );

      		//writing the bytes back to disk
      		SysLib.rawwrite(0, backUpBlock);

      		//outputting confirmation to console
      		Kernel.report("The Superblock has been synced back to the disk.\n")

      }

      //dequeue from the freeblock list
      public short getFreeBlock()
      {
      	//if the freeList is invalid, return an error
      	if (freeList <= || freeList > totalBlocks)
      		return -1;

      	int freeBlock = freeList;

      	byte[] newBlock = new byte[Disk.blockSize];

      	SysLib.rawread(freeList, newBlock);
      	SysLib.int2bytes(0, newBlock, 0);
      	SysLib.rawwrite(freeList, newBlock)

      	freeList = SysLib.bytes2int(newBlock, 0);

      	return freeBlock


      }

      //enqueue a new entry to the freeblock list.
      public boolean returnBlock(int blockNumber)
      {

      	//if the freeList is invalid, return an error
      	if (blockNumber <= || blockNumber > totalBlocks)
      		return false;

      	byte[] newBlock = new byte[Disk.blockSize];

      	SysLib.int2bytes(freeList, newBlock, 0);
      	SysLib.rawwrite(blockNumber, newBlock)

      	freeList = blockNumber;

      	return freeBlock

      }

      public void format(int newInodeCount)
      {	
      		//resetting the total inode count
      		totalInodes = newInodeCount;

      		//creating a new node for each inode, then writing it to disk
      		for (int i=0; i< newInodeCount; i++)
      		{
      			Inode newInode = newInode();
      			newInode.toDisk((short) i);
      		}

      		//setting the new location of the freelist
      		if (iNodes % 16 == 0)
      			freeLsit = newInodeCount / 16 + 1
      		else
      			freeList = newInodeCount / 16 + 2

      		//loop from the second to last block to the end of the freeList
      		for (int i=totalBlocks - 2; i>=freeList; i--)
      		{
      			//creating a new empty block
      			byte[] newBlock = new byte[Disk.blockSize];
      			for (int j=0; j<Disk.BlockSize; j++)
      				newblock[j] = (byte)0;
      			//writing the block number of the next block to the begining of the block
      			SysLib.int2bytes(i+1, newBlock, 0);
      			//writing the block to disk
      			SysLib.rawwrite(i,newBlock)
      		}

      		//the last block will be a null pointer, creating it and 
      		//writing it to disk.
      		byte[] lastBlock = new byte[Disk.blockSize];
      		SysLib.in2bytes(-1, lastBlock, 0);
      		SysLib.rawwrite(totalBlocks - 1, lastBlock);

      		//syncing formatted blocks back to the disk
      		sync();
      	
      }
}