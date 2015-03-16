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
		// check for invalid case: write or append
		if ((fd.mode.equals("w"))
				|| (fd.mode.equals("a")))
		{
			return -1;
		}

		// starting at the position currently pointed to by the seek pointer
		int bytesRead = 0;
		int bufferLeftOver = buffer.length;

		// If bytes remaining between the current seek pointer and the end of file are
		// less than buffer.length, SysLib.read reads as many bytes as possible
		synchronized(fd)
		{
			while(fd.seekPtr < fsize(fd) && bufferLeftOver > 0)
			{
				// check if block was not found
				int block = fd.inode.findTargetBlock(fd.seekPtr);
				if (block == -1)
				{
					break;
				}

				// read into readBuffer
				byte[] readBuffer = new byte[512];
				SysLib.rawread(block, readBuffer);
				int increment = Math.min(512 - (fd.seekPtr % 512), fsize(fd));
				System.arraycopy(readBuffer, (fd.seekPtr % 512), buffer, bytesRead, increment);
				bufferLeftOver -= increment;
				fd.seekPtr += increment;
				bytesRead += increment;
			}
			return bytesRead;
		}
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
		// check if reading only
		if (fd.mode == "r") {
			return -1;
		}

		synchronized (fd)
		{
			// initialize local variables
			int retValue = 0;
			int bufferSize = buffer.length;

			// run loop as long as buffer isn't empty
			while (bufferSize > 0)
			{
				// gets targetBlock
				int block = fd.inode.findTargetBlock(fd.seekPtr);

				// validates block
				if (block == -1)
				{
					short freeBlock = (short) this.superblock.getFreeBlock();
					fd.inode.toDisk(freeBlock);
					block = freeBlock;
				}

				// Write correctly
				byte[] tempBuffer = new byte[512];
				SysLib.rawread(block, tempBuffer);
				int increment = Math.min(512 - (fd.seekPtr % 512), bufferSize);
				System.arraycopy(buffer, retValue, tempBuffer, (fd.seekPtr % 512), increment);
				SysLib.rawwrite(block, tempBuffer);
				fd.seekPtr += increment;
				retValue += increment;
				bufferSize -= increment;

				// sets the length to correct amount
				if (fd.seekPtr > fd.inode.length)
				{
					fd.inode.length = fd.seekPtr;
				}
			}
			// saves to disk at iNumber
			fd.inode.toDisk(fd.iNumber);
			return retValue;
		}
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

		int retVal = -1;

		synchronized(fd)
		{
			if(whence == 0
					&& (offset <= fsize(fd))
					&& (offset >= 0))
			{
				fd.seekPtr = offset;
				retVal = fd.seekPtr;
			}
			else if(whence == 1
					&& (fd.seekPtr + offset <= fsize(fd)) 
					&& (fd.seekPtr + offset >= 0))
			{
				fd.seekPtr += offset;
				retVal = fd.seekPtr;
			}
			else if(whence == 2 
					&& (fsize(fd) + offset <= fsize(fd)) 
					&& (fsize(fd) + offset >= 0))
			{
				fd.seekPtr = (fsize(fd) + offset);
				retVal = fd.seekPtr;
			}
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
	}

	// 7.
	// [FINISHED]
	boolean delete(String fileName)
	{
		//destroys the file specified by fileName. If the file is currently open, it is not 
		//destroyed until the last open on it is closed, but new attempts to open it will fail.
		boolean isDeleted = false;

		// opens Entry
		FileTableEntry fileTableEnt = open(fileName, "w");

		// get inode number
		short iNodeNumber = fileTableEnt.iNumber;

		if(close(fileTableEnt) == 0 && directory.ifree(iNodeNumber))
		{
			isDeleted = true;
		}

		// return 
		return isDeleted;
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
	int format(int files)
	{
		boolean isFileTableEmpty = filetable.fempty();
		while (!isFileTableEmpty)
		{
			isFileTableEmpty = filetable.fempty();
		}

		// format other class fields
		superblock.format(files);
		directory = new Directory(superblock.totalInodes);
		filetable = new FileTable(directory);

		return 0;
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
