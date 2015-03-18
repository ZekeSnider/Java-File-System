import java.util.Vector;


public class FileSystem
{
	private SuperBlock superblock;
	private Directory directory;
	private FileTable filetable;

	// Constructor, Fukuda gave us this in notes
	// [FINISHED]
	public FileSystem(int diskBlocks)
	{
		superblock = new SuperBlock(diskBlocks);
		directory = new Directory(superblock.totalInodes);
		filetable = new FileTable(directory);

		// read the "/" file from disk
		FileTableEntry dirEnt = open("/", "r");
		int dirSize = fsize(dirEnt);
		if(dirSize > 0)
		{
			// the directory has some data
			byte[] dirData = new byte[dirSize];
			read(dirEnt, dirData);
			directory.bytes2directory(dirData);
		}
		close(dirEnt);
	}

	// Fukuda gave us this
	// 2. int fd = SysLib.open( String fileName, String mode );
	// [FINISHED]
	FileTableEntry open(String filename, String mode)
	{
		FileTableEntry ftEnt = filetable.falloc(filename,  mode);
		if(mode.equals("w"))
		{
			if(deallocAllBlocks(ftEnt) == false)
			{
				return null;
			}
		}
		return ftEnt;
	}

	// 2. helper method, need to implement
	// [FINISHED]
	boolean deallocAllBlocks(FileTableEntry fd)
	{
		// check if no table entry points to this inode
		if (fd.inode.count != 1)
		{
			return false;
		}

		byte[] arrayOfByte = new byte[512];
		SysLib.rawread(fd.inode.indirect, arrayOfByte);
		fd.inode.indirect = -1;

		if (arrayOfByte != null)
		{
			int i = 0;
			int j;
			while ((j = SysLib.bytes2short(arrayOfByte, i)) != -1)
			{
				this.superblock.returnBlock(j);
			}
		}

		for (int i = 0; i < 11; i++)
		{
			if (fd.inode.direct[i] != -1)
			{
				this.superblock
				.returnBlock(fd.inode.direct[i]);
				fd.inode.direct[i] = -1;
			}
		}

		fd.inode.toDisk(fd.iNumber);


		return true;
	}

	// 3. reads up to buffer.length bytes from the file indicated by fd
	// [FINISHED]
	int read(FileTableEntry fd, byte[] buffer)
	{
		int bytesRead = 0;
		int bufferLength = buffer.length;
		int offsetPosition, remainingBytes, diskBytes, toRead, indexPosition, seekPtr;
		Inode theInode;
		byte[] readBuffer;

		if(fd == null)
			return -1;

		// check for invalid case: write or append
		else if ((fd.mode.equals("w")) || (fd.mode.equals("a")))
			return -1;
		
		if ((theInode = fd.inode) == null)
			return -1;


		// If bytes remaining between the current seek pointer and the end of file are
		// less than buffer.length, SysLib.read reads as many bytes as possible
		synchronized(fd)
		{
			//allocating read buffer, setting start seek position and index values
			readBuffer = new byte[Disk.blockSize];
			seekPtr = fd.seekPtr;
			indexPosition = 0;

			//repeat until all data has been read to the buffer
			while(indexPosition < bufferLength)
			{
				//setting read offset, number of bytes left on the disk, and remaining bytes requested vartiables
				offsetPosition = seekPtr % Disk.blockSize;
				diskBytes = Disk.blockSize - offsetPosition;
				remainingBytes = bufferLength - indexPosition;

				//only read up to the buffer's size, or the end of the disk, whichever is smaller
				if (diskBytes < remainingBytes)
					toRead = diskBytes;
				else
					toRead = remainingBytes;

				// check if block was not found
				int block = theInode.findTargetBlock(offsetPosition);
				if (block == -1 )
					return -1;
				else if (block< 0 || block>= superblock.totalBlocks) //invalid block cases
					break;

				if (offsetPosition == 0)
					readBuffer = new byte[Disk.blockSize];

				// read into readBuffer
				if (SysLib.rawread(block, readBuffer) == -1)
					return -1;

				//copy the read data into the buffer
				System.arraycopy(readBuffer, offsetPosition, buffer, indexPosition, toRead);
				indexPosition += toRead; //increment read positions
				seekPtr += toRead;
			}
			//re-set the seekPointer's position 
			seek(fd, indexPosition, 1);
		}
		//return last read position
		return indexPosition;
	}

	// 4. writes the contents of buffer to the file indicated by fd,
	// starting at the position indicated by the seek pointer.
	// The operation may overwrite existing data in the file and/or
	// append to the end of the file. SysLib.write increments the
	// seek pointer by the number of bytes to have been written.
	// The return value is the number of bytes that have been written,
	// or a negative value upon an error.
	// [FINISHED]
	int write(FileTableEntry fd, byte[] buffer)
	{
		int offsetPosition, remainingBytes, diskBytes, toWrite, indexPosition, seekPtr;
		short block;
		Inode theNode;

		//Checking for invalid states
		if (fd == null || fd.mode == "r")
			return -1;
		else if ((theNode = fd.inode) == null)
			return -1;
		else if (theNode.flag == 2 || theNode.flag == 3 || theNode.flag == 4)
			return -1;

		int bufferLength = buffer.length;

		synchronized (fd)
		{
			//if the mode is append, set the seek pointer to the end of the block
			if (fd.mode.compareTo( "a" ) == 0)
			{
				seekPtr = seek(fd, 0, 2);
			}
			else
				seekPtr = fd.seekPtr; //otherwise, set the seekpointer to the last position

			//set flag to write
			theNode.flag = 3;
			indexPosition = 0;
			byte[] writeData = new byte[Disk.blockSize];
			// initialize local variables
			


			// run loop as long as buffer isn't empty
			while (indexPosition < bufferLength)
			{
				//setting offset, remaining bytes in buffer, and remaining bytes on disk variables
				offsetPosition = seekPtr % Disk.blockSize;
			
				remainingBytes = bufferLength - indexPosition;

				diskBytes = Disk.blockSize - offsetPosition;

				//if the space requested is greater than the space remaining on the disk,
				//write until the space is filled. otherwise write the whole thing
				if (diskBytes < remainingBytes)
					toWrite = diskBytes;
				else
					toWrite = remainingBytes;

				// gets targetBlock
				if ((block = theNode.findTargetBlock(offsetPosition)) == -1)
				{	
					//Try to allocate new block, check if out of memory
					if ((block = superblock.getFreeBlock()) == -1)
					{
						//error out of memory, set delete flag then break
						theNode.flag = 4;
						break;
					}

					//if the block couldn't be found
					if (theNode.setTargetBlock(seekPtr, block) == -1)
					{
						//attempt to set index block, set delete flag and break on any errors
						if (!theNode.setIndexBlock(block))
						{
							theNode.flag = 4;
							break;
						}
						//get a new free block
						if ((block = superblock.getFreeBlock()) == -1)
						{
							theNode.flag = 4;
							break;
						}

						//setup the new block
						if (theNode.setTargetBlock(seekPtr, block) == -1)
						{
							theNode.flag = 4;
							break;
						}

					}

				}

				//if the block is not in the total block range, set flag to delete and exit
				if (block >= superblock.totalBlocks)
				{
					theNode.flag = 4;
					break;
				}

				if (offsetPosition == 0)
					writeData = new byte[Disk.blockSize];

				//read the data to the data byte array
				SysLib.rawread(block, writeData);
				System.arraycopy(buffer, indexPosition, writeData, offsetPosition, toWrite); //copy the new data to the arrray
				SysLib.rawwrite(block, writeData); //write it back
				indexPosition += toWrite; //increment variables
				seekPtr += toWrite;

				// sets the length to correct amount
				
			}
			//if the file size increased, update the inode's length
			if (seekPtr > theNode.length) 
			{
				theNode.length = seekPtr;
			}
			//re-set the new seek position
			seek(fd, indexPosition, 1);

			//set flag
			if(theNode.flag != 4)
				theNode.flag = 1;

			// saves to disk at iNumber
			theNode.toDisk(fd.iNumber);
		}
		//return last seekposition
		return indexPosition;
	}

	// 5.
	// [FINISHED]
	int seek(FileTableEntry fd, int offset, int whence)
	{
		//Updates the seek pointer corresponding to fd as follows:
		//If whence is SEEK_SET (= 0), the file's seek pointer is set to offset bytes from the beginning of the file
		//If whence is SEEK_CUR (= 1), the file's seek pointer is set to its current value plus the offset. The offset can be positive or negative.
		//If whence is SEEK_END (= 2), the file's seek pointer is set to the size of the file plus the offset. The offset can be positive or negative.
		//If the user attempts to set the seek pointer to a negative number you must clamp it to zero. If the user attempts to set the pointer to beyond the file size, you must set the seek pointer to the end of the file. In both cases, you should return success.
		if (fd==null)
			return -1;

		int retVal;
		int eofPtr;

		synchronized(fd)
		{
			retVal = fd.seekPtr;
			eofPtr = fsize(fd);

			if(whence == 0)
			{
				retVal = offset;
			}
			else if(whence == 1)
			{
				retVal += offset;
			}
			else if(whence == 2)
			{
				retVal = eofPtr + offset;
			}

			if (retVal < 0)
				retVal = 0;
			else if (retVal > eofPtr)
				retVal = eofPtr;

			fd.seekPtr = retVal;
		}
		return retVal;
		

	}

	// 6. 
	// [FINISHED]
	int close(FileTableEntry fd)
	{
		//closes the file corresponding to fd, commits all file transactions on this file, and 
		//unregisters fd from the user file descriptor table of the calling thread's TCB. The 
		//return value is 0 in success, otherwise -1.
		int retVal = -1;
		if (fd != null)
		{
			synchronized (fd) 
			{
				fd.count--;
				if (fd.count > 0)
				{
					retVal = 0;
				}
			}

			if(filetable.ffree(fd))
			{
				retVal = 0;
			}
			else
			{
				retVal = -1;
			}

			return retVal;
		} else
			return -1;
	}

	// 7.
	// [FINISHED]
	boolean delete(String fileName)
	{	
		if (fileName== "" || fileName ==null)
			return false;

		// get inode number
		short iNodeNumber = directory.namei(fileName);

		if (iNodeNumber == -1)
			return false;

		// return 
		return directory.ifree(iNodeNumber);
	}

	// 8. returns the size in bytes of the file indicated by fd
	// [FINISHED]
	int fsize(FileTableEntry fd)
	{
		synchronized (fd) 
		{
			return fd.inode.length;
		}
	}

	// Format()
	boolean format(int files)
	{
		boolean isFileTableEmpty = filetable.fempty();
		while (!isFileTableEmpty)
		{
			isFileTableEmpty = filetable.fempty();
			return false;
		}

		// format other class fields
		superblock.format(files);
		directory = new Directory(superblock.totalInodes);
		filetable = new FileTable(directory);

		return true;
	}

	// Sync()
	void sync()
	{
		FileTableEntry fileTableEntry = open("/", "w");
	    byte[] buffer = directory.directory2bytes();
	    write(fileTableEntry, buffer);
	    close(fileTableEntry);

	    this.superblock.sync();
	}

}
