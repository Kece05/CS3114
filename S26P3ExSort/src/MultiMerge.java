import java.io.IOException;
import java.io.RandomAccessFile;

// -------------------------------------------------------------------------
/**
 * This file uses a merge algorthm to put sorted chunks from the temp file
 * back into the output file
 * 
 *  @author kece05
 *  @version Mar 25, 2026
 */
public class MultiMerge {
    /**
     * The max number of bytes
     */
    private static final int MEMBYTES = 50000;
    
    /**
     * The number of bytes per chunk
     */
    private static final int BLOCK_SIZE = 4096;
    
    /**
     * Bytes per record
     */
    private static final int RECORD_SIZE = 8;
    
    /**
     * The heap size
     */
    private static final int RUN_SIZE_BYTES = 41808;
    
    // ----------------------------------------------------------
    /**
     * This file goes through each buffer and adds to the disk from 
     * min order(as in keys) which was already sorted by the BufferSort 
     * all the way up to max order and then puts out to the output file
     * 
     * 
     * @param inputFile the input file
     * @param outputFile the output file
     * @param memPool the pool of buffers
     * @throws IOException if cannot access the one of the files
     */
    public static void mergeRuns(String inputFile, String outputFile, 
        byte[] memPool)  throws IOException {
        try {
            RandomAccessFile in = new RandomAccessFile(inputFile, "r");
            RandomAccessFile out = new RandomAccessFile(outputFile, "rw");
            
            long length = in.length();
            
            // If there is nothing to pull
            if (length == 0) return;
            
            // Counting total runs for each buffer 
            int total_runs = (int) Math.ceil((double) length / RUN_SIZE_BYTES);
            
            // Setting up the offset the buffer at the end of the mem pool
            int outputOffset = MEMBYTES - BLOCK_SIZE;
            BufferSort outputBuffer = new BufferSort(out, memPool, outputOffset,
                BLOCK_SIZE);
            long writePosition = 0;
            
            // Creating the dynamic capacity buffer used for each run
            int inputBufferCap = outputOffset/total_runs;
            
            // Rounding down for the nearest multiple of 8
            inputBufferCap = (inputBufferCap / RECORD_SIZE) * RECORD_SIZE;
            
            // Initializing position and bytes
            BufferSort[] inputBuffers = new BufferSort[total_runs];
            long[] runPosition = new long[total_runs];
            long[] runBtyes = new long[total_runs];
            
            // Initializing each index of both for each position of byte for
            // each of the runs
            for (int i =0; i < total_runs; i++) {
                int offset = i * inputBufferCap;
                inputBuffers[i] = new BufferSort(in, memPool, 
                    offset, inputBufferCap);
                
                runPosition[i] = (long) i * RUN_SIZE_BYTES;
                
                // For each end of the chunks in case it is smaller 
                // than expected
                long remainingInRun = length - runPosition[i];
                runBtyes[i] = Math.min(RUN_SIZE_BYTES, remainingInRun);
                
                // Fetching the first chunk of data for this run
                inputBuffers[i].readBlock(runPosition[i]);
                runPosition[i] += inputBufferCap;
            }
            
            long totalMerged = 0;
            
            // The loop to merge 
            while (totalMerged < length) {
                int smallestIndex = -1;
                int smallestKey = Integer.MAX_VALUE;
                
                // First find the smallest key for each buffer
                for (int i = 0; i < total_runs; i++) {
                    if (runBtyes[i] > 0) {
                        
                        // Refilling the buffer from disk if it's empty
                        if (inputBuffers[i].isEmpty()) {
                            inputBuffers[i].readBlock(runPosition[i]);
                            runPosition[i] += inputBufferCap;
                        }

                        // Peeking at the key without extracting it
                        // to put that record and data on to disk in order
                        int currentKey = inputBuffers[i].peekKey();
                        if (smallestIndex == -1 || currentKey < smallestKey) {
                            smallestKey = currentKey;
                            smallestIndex = i;
                        }
                    }
                }
                
                // Now putting the smallest record to the output file 
                BufferSort smallestBuffer = inputBuffers[smallestIndex];
                int key = smallestBuffer.readInt();
                int data = smallestBuffer.readInt();
                
                outputBuffer.writeInt(key);
                outputBuffer.writeInt(data);
                
                // Updating the count variables
                runBtyes[smallestIndex] -= RECORD_SIZE;
                totalMerged += RECORD_SIZE;

                // Flush output buffer if full
                if (outputBuffer.isFull()) {
                    outputBuffer.writeBlock(writePosition);
                    writePosition += BLOCK_SIZE;
                }
            }
            
            // Flushing the final bytes onto the output
            outputBuffer.writeBlock(writePosition);
            
        } catch(Exception e) {
            System.out.println("Error Occured In MultiMerge: " + e.toString());
        }
    }
}
