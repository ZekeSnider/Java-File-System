import java.util.*;

public class FileTable {

      private Vector table;         // the actual entity of this file table
      private Directory dir;        // the root directory 


      public static short APPEND = 0;
      public static short READONLY = 1;
      public static short WRITEONLY = 2;
      public static short READWRITE = 3;

      public FileTable( Directory directory ) { // constructor
         table = new Vector( );     // instantiate a file (structure) table
         dir = directory;           // receive a reference to the Director
      }                             // from the file system

      // major public methods
      public synchronized FileTableEntry falloc( String filename, String mode ) {
         // allocate a new file (structure) table entry for this file name
         // allocate/retrieve and register the corresponding inode using dir
         // increment this inode's count
         // immediately write back this inode to the disk
         // return a reference to this file (structure) table entry

         short iNodeNum = dir.namei(filename);
         short inputMode = getAccessMode(mode);
         Inode newInode = null;

         while(true)
         {
            if ( ((int) iNodeNum) == -1)  //not found, must allocate
            {
               if(inputMode == READONLY)
               {
                  return null; //no need to allocate if there's no data to read
               }

               iNodeNum = dir.ialloc(filename);
               //could not allocate, must exit with error
               if ( ((int) iNodeNum) == -1) 
                  return null;

               break;
            }
            else 
            {
               newInode = new Inode(iNodeNum);

               
               if (newInode.flag == 0 || newInode.flag == 1) //used/unused
                  break;
               else if (newInode.flag == 4) //delete
                  return null;
               else if (inputMode == READONLY && newInode.flag == 2) //only reading from the node
                  break;
               else
               {
                  try //wait on write
                  {
                     wait();
                  } catch (InterruptedException e){}
               }

            }
         }

         newInode.count++;
         newInode.toDisk(iNodeNum);

         FileTableEntry newEntry = new FileTableEntry(newInode, iNodeNum, mode);
         table.add(newEntry);

         return newEntry;


      }

      public synchronized boolean ffree( FileTableEntry e ) {
         // receive a file table entry reference
         // save the corresponding inode to the disk
         // free this file table entry.
         // return true if this file table entry found in my table

         //if the entry exists in the table, process the inode, otherwise just return false
         if (table.remove(e))
         {
            //getting Inode and Inode number
            Inode deleteNode = e.inode;
            short deleteNodeNum = e.iNumber;

            //decrementing the Inode's count
            deleteNode.count--;

            //If the flag is read/write, notify other threads waiting on this node
            if (deleteNode.flag == 2 || deleteNode.flag == 3)
               notify();

            //writing the changed node to disk
            deleteNode.toDisk(deleteNodeNum);

            return true;
         }
         else 
         {
            return false;
         }


      }

       public short getAccessMode(String inputString)
       {
           if ( inputString.compareTo( "a" ) == 0 )
               return APPEND;
           else if ( inputString.compareTo( "w" ) == 0 )
               return WRITEONLY;
           else if ( inputString.compareTo( "r" ) == 0 )
               return READONLY;
           else if ( inputString.compareTo( "w+" ) == 0 )
               return READWRITE;
           else
               return -1;
       }

      public synchronized boolean fempty( ) {
         return table.isEmpty( );  // return if table is empty 
      }                            // should be called before starting a format
   }