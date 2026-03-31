import java.io.IOException;
import java.io.RandomAccessFile;

// -------------------------------------------------------------------------
/**
 *  This method is used as the first part of the stage to generate a sort
 *  buffer to the output file which uses a minheap to sort and then output
 *  the data to the hard drive
 * 
 *  @author kece05
 *  @version Mar 23, 2026
 */

public class Generator {
    /*
     *  File block size in bytes
     */
    private static final int BLOCK_SIZE = 4096;
    
    /*
     * Bytes per record
     */
    private static final int RECORD_SIZE = 8;

    
    /**
     * This method is use for generating the initial sorted runs
     * 
     * @param inputFile The original unsorted file
     * @param runFile The temporary file to store sorted runs
     * @param memPool The 50,000 byte working memory
     * @throws IOException an error if it cannot open file
     */
    public static void generateRuns(String inputFile, String runFile, 
        byte[] memPool)  throws IOException {
        
        try {
            RandomAccessFile in = new RandomAccessFile(inputFile, "r");
            RandomAccessFile out = new RandomAccessFile(runFile, "rw");
            
            // Creating an input/output buffer sort
            BufferSort i = new BufferSort(new RandomAccessFile(inputFile, "r"), memPool, 0, BLOCK_SIZE);
            BufferSort o = new BufferSort(out, memPool, BLOCK_SIZE, BLOCK_SIZE);
            
            // Setting up inital parameters
            int heapOffset = BLOCK_SIZE * 2;
            int heapCapacityBytes = memPool.length - heapOffset;
            int maxRecords = heapCapacityBytes / RECORD_SIZE;
            
            MinHeap heap = new MinHeap(memPool, heapOffset, maxRecords);
            
            // Initializing for the loop
            long readPosition = 0;
            long writePosition = 0;
            long totalFileLength = in.length();
            long bytesProcessed = 0;
            
            // Fetching the first block
            i.readBlock(readPosition);
            readPosition += BLOCK_SIZE;
            
            // Processes through the given file
            while (bytesProcessed < totalFileLength) {
                // Repeats until the heap is full or no more inputs from file
                while (!heap.isFull() && bytesProcessed < totalFileLength) {
                    
                    // If file is empty, get the next block from disk
                    if (i.isEmpty()) {
                        i.readBlock(readPosition);
                        // Advancing by 4096 bytes for the next part, buffering
                        readPosition += BLOCK_SIZE; 
                    }
                    //Creating key and data to insert into the heap
                    int key = i.readInt();
                    int data = i.readInt();
                    heap.insert(key, data);
                    
                    bytesProcessed += RECORD_SIZE;
                }
                
                // Emptying the heap it into the output buffer 
                // which creates a sorted run
                while (!heap.isEmpty()) {
                    // If it is full save it to the hard drive
                    if (o.isFull()) {
                        o.writeBlock(writePosition);
                        
                        //Adding a buffer
                        writePosition += BLOCK_SIZE;
                    }
                    
                    // removeMin writes the 8-byte record to the output Buffer
                    heap.removeMin(o);
                }
                
            }
            
            in.close();
            // Save the remaining data to the hard drive
            o.writeBlock(writePosition);
            
        } 
        catch (IOException e) {
            System.out.println("Error Occured In Generator: " + e.toString());
        }
    }
}
