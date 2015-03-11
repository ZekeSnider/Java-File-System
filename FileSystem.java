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
	// [INCOMPLETE]
	boolean deallocAllBlocks(FileTableEntry fd)
	{
		//Empty out the inode, delete any blocks it frees in the process
	    /*Vector<Short> blocks_freed = fd.inode.deallocAllBlocks(fd.iNumber);
	    
	    for (int i = 0; i < blocks_freed.size(); i++) 
	    {
	      Short block = (Short)blocks_freed.elementAt(i);
	      superblock.returnBlock((short)block);
	    }
	    */
	    
		return true;
	}

	// 3. reads up to buffer.length bytes from the file indicated by fd
	// [WORKING ON]
	int read(FileTableEntry fd, byte[] buffer)
	{
		// check for invalid case: write or append
		if ((fd.mode.equals("w"))
				|| (fd.mode.equals("a"))) 
		{
			return -1;
		}

		// starting at the position currently pointed to by the seek pointer
		int seekPointerPosition = fd.seekPtr;
		int bytesRead = 0;
		
		// If bytes remaining between the current seek pointer and the end of file are 
		// less than buffer.length, SysLib.read reads as many bytes as possible
		while(seekPointerPosition < )
		{
			// check if block was not found
			int block = fd.inode.findTargetBlock(fd.seekPtr);
			if (block == -1) 
			{
				break;
			}

			// read from buffer
			byte[] readBuffer = new byte[512];
			SysLib.rawread(block, readBuffer);

		}
		
		return bytesRead;
	}

	// 4. writes the contents of buffer to the file indicated by fd, 
	// starting at the position indicated by the seek pointer. 
	// The operation may overwrite existing data in the file and/or 
	// append to the end of the file. SysLib.write increments the 
	// seek pointer by the number of bytes to have been written. 
	// The return value is the number of bytes that have been written,
	// or a negative value upon an error.
	int write(FileTableEntry fd, byte[] buffer)
	{
		return -1;
	}

	// 5.
	int seek(FileTableEntry fd, int offset, int whence)
	{
		//Updates the seek pointer corresponding to fd as follows:
		//If whence is SEEK_SET (= 0), the file's seek pointer is set to offset bytes from the beginning of the file
		//If whence is SEEK_CUR (= 1), the file's seek pointer is set to its current value plus the offset. The offset can be positive or negative.
		//If whence is SEEK_END (= 2), the file's seek pointer is set to the size of the file plus the offset. The offset can be positive or negative.
		//If the user attempts to set the seek pointer to a negative number you must clamp it to zero. If the user attempts to set the pointer to beyond the file size, you must set the seek pointer to the end of the file. In both cases, you should return success.

		return -1;
	}

	// 6. 
	int close(FileTableEntry fd)
	{
		//closes the file corresponding to fd, commits all file transactions on this file, and 
		//unregisters fd from the user file descriptor table of the calling thread's TCB. The 
		//return value is 0 in success, otherwise -1.
		return -1;
	}

	// 7.
	int delete(String fileName)
	{
		//destroys the file specified by fileName. If the file is currently open, it is not 
		//destroyed until the last open on it is closed, but new attempts to open it will fail.
		return -1;
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
}
