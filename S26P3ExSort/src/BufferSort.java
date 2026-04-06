import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

// -------------------------------------------------------------------------
/**
 * This class is used to as a utilization class used for grabbing 
 * the 50000 bytes that will be used for other classes.
 * 
 *  @author kece05
 *  @version Mar 23, 2026
 */
public class BufferSort {
    
    /**
     * The file being accessed
     */
    private RandomAccessFile file;
    
    /**
     * The buffer pool being looked at
     */
    private ByteBuffer buffer;
    
    /**
     * The current position in the file
     */
    private long currentFilePos;
    
    /**
     * This initializing buffer class
     * 
     * @param file the file that is being read or wrote too
     * @param memPool the single 50,000 byte array
     * @param offset the starting index in the memory pool for this buffer
     * @param len the size of the buffer
     */
    public BufferSort(RandomAccessFile file, byte[] memPool, int offset, 
        int len) {
        this.file = file;
        
        // Wrapping a specific slice of the given memory pool
        this.buffer = ByteBuffer.wrap(memPool, offset, len).slice();
        
        this.currentFilePos = 0;
    }
    
    /**
     * This method reads a block of data into the memory pool used for Generator
     * 
     * 
     * @param curr The position in the file
     * @throws IOException An error if failed to access position in the buffer 
     */
    public void readBlock(long curr) throws IOException {
        // Accessing locale position in file
        this.currentFilePos = curr;
        file.seek(currentFilePos);
        
        // Clearing the buffering pool
        buffer.clear();
        
        // Used for edge cases
        long bytesRemaining = file.length() - curr;
        int bytesToRead = (int) Math.min(buffer.capacity(), bytesRemaining);
        
        if (bytesToRead > 0) {
            // buffer.arrayOffset() now correctly points 
            // to the exact chunk of memPool
            file.readFully(buffer.array(), buffer.arrayOffset(), bytesToRead);
        }
        // Reading the specific byte array
        buffer.limit(bytesToRead);
    }

    /**
     * Reads a block limited to a maximum number of bytes, used for
     * run-bounded reads in multiway merge
     *
     * @param pos The position in the file
     * @param maxBytes The maximum number of bytes to read
     * @throws IOException An error if failed to access position in the buffer
     */
    public void readBlock(long pos, int maxBytes) throws IOException {
        // Accessing locale position in file
        this.currentFilePos = pos;
        file.seek(pos);

        // Clearing the buffering pool
        buffer.clear();

        // Limit by both buffer capacity and maxBytes
        int bytesToRead = Math.min(buffer.capacity(), maxBytes);

        if (bytesToRead > 0) {
            file.readFully(buffer.array(), buffer.arrayOffset(), bytesToRead);
        }
        // Reading the specific byte array
        buffer.limit(bytesToRead);
    }

    /**
     * Returns the number of bytes currently buffered for writing
     *
     * @return the number of bytes written to the buffer
     */
    public int bufferedBytes() {
        return buffer.position();
    }


    /**
     * This method writes the content into the give file and position
     * 
     * @param curr The position in the file
     * @throws IOException An error if failed to access position in the buffer 
     */
    public void writeBlock(long curr) throws IOException {
        file.seek(curr);
        
        // Getting length of bytes to write in the file 
        int bytesToWrite = buffer.position(); 
        if (bytesToWrite > 0) {
            file.write(buffer.array(), buffer.arrayOffset(), bytesToWrite);
        }
        
        this.currentFilePos = curr + bytesToWrite;
        
        buffer.clear();
    }

    /**
     * This method reads the next four byte integer from the buffer
     * 
     * @return the next four bytes
     */
    public int readInt() {
        return buffer.getInt();
    }
    
    /**
     * This method writes a four byte integer into the buffer
     * 
     * @param value the value to be written
     */
    public void writeInt(int value) {
        buffer.putInt(value);
    }
    
    /**
     * The method for checking if the buffer has been fully read
     * 
     * @return true if empty and false if not
     */
    public boolean isEmpty() {
        return !buffer.hasRemaining();
    }

    /**
     * The method for checking if the buffer is completely full
     * @return true if full and false if not
     */
    public boolean isFull() {
        return buffer.position() == buffer.capacity();
    }
    
    /**
     * This method looks at the next bytes which is used in the multiway merge
     * 
     * @return the next buffer bytes 
     */
    public int peekKey() {
        int currentPos = buffer.position();
        int key = buffer.getInt();
        
        //Reseting the position
        buffer.position(currentPos);
        
        return key;
    }
}
