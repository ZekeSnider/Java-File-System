public class Directory {
      private static int maxChars = 30; // max characters of each file name

      // Directory entries
      private int fsize[];        // each element stores a different file size.
      private char fnames[][];    // each element stores a different file name.
      private int maxInumber;
      private int directorySize;

      public Directory( int inputNum ) { // directory constructor
         maxInumber = inputNum;
         fsize = new int[maxInumber];     // maxInumber = max files
         for ( int i = 0; i < maxInumber; i++ ) 
             fsize[i] = 0;                 // all file size initialized to 0
         fnames = new char[maxInumber][maxChars];
         String root = "/";                // entry(inode) 0 is "/"
         fsize[0] = root.length( );        // fsize[0] is the size of "/".
         root.getChars( 0, fsize[0], fnames[0], 0 ); // fnames[0] includes "/"

         //stores how many bytes are to be used for writing/reading
         //the directory to a byte array as a class variable for easy access
         directorySize = (4 * maxInumber) + (maxChars * maxInumber * 2);
      }

      public int bytes2directory( byte data[] ) {
         // assumes data[] received directory information from disk
         // initializes the Directory instance with this data[]
         
         int readOffset = 0;

         //reading the file sizes from the byte array
         for (int i=0; i<maxInumber; i++)
         {
            fsize[i] = SysLib.bytes2int(data, readOffset);
            readOffset += 4;
         }       

         //reading the file names from the byte array
         for (int i=0; i<maxInumber; i++)
         {
            for (int j=0; j<maxChars; j++)
            {
               //reading each short from the byte array
               //then converting it to a char and adding it to the name array
               short readShort = SysLib.bytes2short(data, readOffset);
               fnames[i][j] = (char)readShort;
               readOffset += 2;
            }
         }
         return 1;

      }

      public byte[] directory2bytes( ) {
         // converts and return Directory information into a plain byte array
         // this byte array will be written back to disk
         // note: only meaningfull directory information should be converted
         // into bytes.
         byte data[] = new byte[directorySize];

         int writeOffset = 0;

         //writing the file sizes to the byte array
         for (int i=0; i<maxInumber; i++)
         {
            SysLib.int2bytes(fsize[i], data, writeOffset);
            writeOffset += 4;
         }       

         //writing the file names to the byte array
         for (int i=0; i<maxInumber; i++)
         {
            for (int j=0; j<maxChars; j++)
            {
               //converting each char to a short then using
               //short2byte to write the names to the array
               short writeShort = (short)fnames[i][j];
               SysLib.short2bytes(writeShort, data, writeOffset);
               writeOffset += 2;
            }
         }
         return data;

      }

      public short ialloc( String filename ) {
         //searaching for an empty spot
         for (int i=0; i<maxInumber; i++)
         {
            //if the spot is empty
            if (fsize[i] == 0)
            {
               //If the filename is shorter than the max length, copy the whole filename.
               //If the filename exceeds the maxChar limit, only copy up to the limit
               int copyLimit;
               if (filename.length() < maxChars)
                  copyLimit = filename.length();
               else 
                  copyLimit = maxChars;

               //setting the size of the name
               fsize[i] = copyLimit;

               for (int j = 0; j < copyLimit; j++)
               {
                  //converting character from string to char, then placing it in array
                  char stringChar = filename.charAt(j);
                  fnames[i][j] = stringChar;
               }
               //returning inode number
               return (short) i;
            }
         }
         return (short) -1;
      }

      public boolean ifree( short iNumber ) {
         // deallocates this inumber (inode number)
         // the corresponding file will be deleted.

         //return false if the inumber is not valid
         if ((iNumber < 0) || (iNumber > maxInumber))
            return false;

         //reset size
         fsize[iNumber] = 0;

         return true;
      }

      public short namei( String filename ) {
         // returns the inumber corresponding to this filename
         
         //getting length of input name 
         int fileNameLength = filename.length();

         //if the string is longer than the max length, crop it
         if (fileNameLength > maxChars)
         {
            fileNameLength = maxChars;
            filename = filename.substring(0, maxChars);
         }

         //looping through elements 
         for (int i=0; i<maxInumber; i++)
         {
            //if the size matches, check this element
            if (fileNameLength == fsize[i])
            {
               //copying name from inode to string
               String INodeName = new String(fnames[i], 0, fsize[i]);

               //if the names are equal, return this inode number
               if (filename.compareTo(INodeName) == 0)
                  return (short) i;
            }

         }
         //no matches found if this point is reached
         return -1;
      }
   }