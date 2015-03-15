public class Inode {
      private final static int iNodeSize = 32;       // fix to 32 bytes
      private final static int directSize = 11;      // # direct pointers

      public int length;                             // file size in bytes
      public short count;                            // # file-table entries pointing to this
      public short flag;                             // 0 = unused, 1 = used, 2 = read, 3 = write, 4 = delete
      public short direct[] = new short[directSize]; // direct pointers
      public short indirect;                         // a indirect pointer

      Inode( ) {                                     // a default constructor
         length = 0;
         count = 0;
         flag = 1;
         for ( int i = 0; i < directSize; i++ )
            direct[i] = -1;
         indirect = -1;
      }

      Inode( short iNumber ) {                       // retrieving inode from disk
         byte[] readData = new byte[Disk.blockSize];
         int offset = (iNumber % 16) * iNodeSize;
         int blockNum = 1 + (iNumber / 16);
         
         SysLib.rawread(blockNum, readData);

         length = SysLib.bytes2int(readData, offset + 0);
         count = SysLib.bytes2short(readData, offset + 4);
         flag = SysLib.bytes2short(readData, offset + 6);

         for (int i = 0; i<directSize; i++)
         {
            int directOffset = offset + (8+(2*i));
            direct[i] = SysLib.bytes2short(readData, directOffset);
         }

         indirect = SysLib.bytes2short(readData, offset + 30);

      }

      int toDisk( short iNumber ) {                  // save to disk as the i-th inode

         byte[] writeData = new byte[Disk.blockSize];

         int offset = (iNumber % 16) * iNodeSize;
         int blockNum = 1 + (iNumber / 16);

         SysLib.int2bytes(length, writeData, offset + 0);
         SysLib.short2bytes(count, writeData, offset + 4);
         SysLib.short2bytes(flag, writeData, offset + 6);

         for (int i = 0; i<directSize; i++)
         {
            int directOffset = offset + (8+(2*i));
            SysLib.short2bytes(direct[i], writeData, directOffset);
         }

         SysLib.short2bytes(indirect, writeData, offset + 30);

         SysLib.rawwrite(blockNum, writeData);

         return 0;


      }

      public short getIndexBlockNumber( ) 
      {
         return indirect;
      }

      public boolean setIndexBlock( short indexBlockNumber )
      {
         //if the indexBlockNumber is not valid or if the indirect block
         //is already allocated, return an error.
         if (indexBlockNumber < 0 || indirect != -1)
            return false;

         //don't allow allocation if any of the direct blocks are available
         for (int i=0; i< directSize; i++)
            if (direct[i] == -1)
               return false;

         byte[] writeData =  new byte[Disk.blockSize];
         for (int i=0; i<Disk.blockSize/2; i+=2)
         {
            SysLib.short2bytes((short)-1, writeData, i);
         }
         SysLib.rawwrite(indexBlockNumber, writeData);
         return true;
      }

      public short findTargetBlock( int offset ) 
      {
         int blockNum = offset/Disk.blockSize;

         //if the block is in direct memory, just return it
         if (blockNum < directSize)
            return direct[blockNum];

         //if the indirect pointer is not null, look for it in indirect
         if (indirect != -1)
         {
            byte[] indirectRead = new byte[Disk.blockSize];
            SysLib.rawread(indirect, indirectRead);
            int blockLocation = blockNum - directSize;

            return SysLib.bytes2short(indirectRead, blockLocation * 2);
         }
         else  //return an error if it's supposed to be in indirect but indirect is emtpy
            return -1;

      } 
   }