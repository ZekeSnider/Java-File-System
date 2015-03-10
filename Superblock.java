class Superblock 
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

      public short getFreeBlock()
      {

      }

      public boolean returnBlock(int blockNumber)
      {

      }
}