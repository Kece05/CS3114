import java.io.IOException;
import java.io.RandomAccessFile;

// -------------------------------------------------------------------------
/**
 * This file uses a multiway merge algorithm to merge sorted runs from the
 * temp file back into the output file. Uses multi-pass merging to keep
 * buffer sizes large enough for efficient I/O.
 *
 *  @author kece05
 *  @version Mar 25, 2026
 */
public class MultiMerge {
    /**
     * The max number of bytes in the memory pool
     */
    private static final int MEMBYTES = 50000;

    /**
     * The number of bytes per I/O block
     */
    private static final int BLOCK_SIZE = 4096;

    /**
     * Bytes per record (4-byte key + 4-byte data)
     */
    private static final int RECORD_SIZE = 8;

    /**
     * The heap size in bytes, must match Generator
     */
    private static final int RUN_SIZE_BYTES = 41808;

    /**
     * Maximum runs to merge at once, each gets a BLOCK_SIZE input buffer
     */
    private static final int MAX_WAYS =
        (MEMBYTES - BLOCK_SIZE) / BLOCK_SIZE;

    // ----------------------------------------------------------
    /**
     * Merges sorted runs from inputFile into outputFile using
     * multi-pass multiway merge. Each pass merges up to MAX_WAYS
     * runs at a time with properly sized I/O buffers.
     *
     * @param inputFile the file containing sorted runs
     * @param outputFile the file to write the final sorted result
     * @param memPool the 50,000 byte working memory
     * @throws IOException if cannot access one of the files
     */
    public static void mergeRuns(String inputFile, String outputFile,
        byte[] memPool) throws IOException {

        // Getting the file length
        RandomAccessFile tempRaf = new RandomAccessFile(inputFile, "r");
        long fileLength = tempRaf.length();
        tempRaf.close();

        // If there is nothing to merge
        if (fileLength == 0) {
            return;
        }

        // Counting total initial runs
        int numRuns = (int)Math.ceil((double)fileLength / RUN_SIZE_BYTES);

        // If only one run, just copy to output
        if (numRuns <= 1) {
            fileCopy(inputFile, outputFile, memPool);
            return;
        }

        // Track which file is current source and destination
        String srcFile = inputFile;
        String dstFile = outputFile;
        long currentRunSize = RUN_SIZE_BYTES;
        int currentNumRuns = numRuns;

        // Each pass merges groups of 'ways' runs until one run remains
        while (currentNumRuns > 1) {
            int ways = Math.min(MAX_WAYS, currentNumRuns);

            doOnePass(srcFile, dstFile, memPool, fileLength,
                currentNumRuns, currentRunSize, ways);

            // Update run count and size for next pass
            currentNumRuns =
                (int)Math.ceil((double)currentNumRuns / ways);
            currentRunSize *= ways;

            // Swap source and destination for next pass
            String temp = srcFile;
            srcFile = dstFile;
            dstFile = temp;
        }

        // If final sorted data ended up in the wrong file, copy it
        if (!srcFile.equals(outputFile)) {
            fileCopy(srcFile, outputFile, memPool);
        }
    }


    /**
     * Performs one pass of multiway merge, reading from srcFile and
     * writing merged groups to dstFile.
     *
     * @param srcFile the source file to read runs from
     * @param dstFile the destination file to write merged runs to
     * @param memPool the 50,000 byte working memory
     * @param fileLength total length of the data in bytes
     * @param numRuns number of runs in the source file
     * @param runSize size of each run in bytes (last may be smaller)
     * @param ways number of runs to merge per group
     * @throws IOException if file access fails
     */
    private static void doOnePass(String srcFile, String dstFile,
        byte[] memPool, long fileLength, int numRuns, long runSize,
        int ways) throws IOException {

        RandomAccessFile in = new RandomAccessFile(srcFile, "r");
        RandomAccessFile out = new RandomAccessFile(dstFile, "rw");

        // Each input buffer gets BLOCK_SIZE bytes
        int inputBufSize = BLOCK_SIZE;

        // Output buffer gets the remaining memory
        int outputOffset = ways * inputBufSize;
        int outputBufSize =
            ((MEMBYTES - outputOffset) / RECORD_SIZE) * RECORD_SIZE;

        // Creating the output buffer at the end of the memory pool
        BufferSort outputBuffer =
            new BufferSort(out, memPool, outputOffset, outputBufSize);
        long writePos = 0;
        int runsProcessed = 0;

        // Process each group of runs
        while (runsProcessed < numRuns) {
            int mergeWays = Math.min(ways, numRuns - runsProcessed);

            // Set up input buffers and tracking arrays
            BufferSort[] inputs = new BufferSort[mergeWays];
            long[] runFilePos = new long[mergeWays];
            long[] runBytesLeft = new long[mergeWays];

            // Initialize each input buffer with first chunk of its run
            for (int i = 0; i < mergeWays; i++) {
                int offset = i * inputBufSize;
                inputs[i] = new BufferSort(in, memPool, offset,
                    inputBufSize);

                // Computing the start and length of this run
                long runStart =
                    ((long)(runsProcessed + i)) * runSize;
                long runEnd =
                    Math.min(runStart + runSize, fileLength);
                long runLen = runEnd - runStart;

                // Reading the first block of this run
                int toRead =
                    (int)Math.min(inputBufSize, runLen);
                inputs[i].readBlock(runStart, toRead);
                runFilePos[i] = runStart + toRead;
                runBytesLeft[i] = runLen - toRead;
            }

            // Merge loop: find smallest key across all runs
            while (true) {
                int bestIdx = -1;
                int bestKey = Integer.MAX_VALUE;

                for (int i = 0; i < mergeWays; i++) {
                    // Refill buffer from disk if empty and run
                    // still has data
                    if (inputs[i].isEmpty()
                        && runBytesLeft[i] > 0) {
                        int toRead = (int)Math.min(
                            inputBufSize, runBytesLeft[i]);
                        inputs[i].readBlock(
                            runFilePos[i], toRead);
                        runFilePos[i] += toRead;
                        runBytesLeft[i] -= toRead;
                    }

                    // Check if this run has a record to compare
                    if (!inputs[i].isEmpty()) {
                        int key = inputs[i].peekKey();
                        if (bestIdx == -1 || key < bestKey) {
                            bestKey = key;
                            bestIdx = i;
                        }
                    }
                }

                // All runs exhausted for this group
                if (bestIdx == -1) {
                    break;
                }

                // Write the smallest record to output buffer
                outputBuffer.writeInt(inputs[bestIdx].readInt());
                outputBuffer.writeInt(inputs[bestIdx].readInt());

                // Flush output buffer to disk when full
                if (outputBuffer.isFull()) {
                    outputBuffer.writeBlock(writePos);
                    writePos += outputBufSize;
                }
            }

            runsProcessed += mergeWays;
        }

        // Flush any remaining data in the output buffer
        if (outputBuffer.bufferedBytes() > 0) {
            outputBuffer.writeBlock(writePos);
        }

        in.close();
        out.close();
    }


    /**
     * Copies a file using the memory pool as a buffer
     *
     * @param src source file path
     * @param dst destination file path
     * @param memPool the byte array to use as copy buffer
     * @throws IOException if file access fails
     */
    private static void fileCopy(String src, String dst,
        byte[] memPool) throws IOException {
        RandomAccessFile in = new RandomAccessFile(src, "r");
        RandomAccessFile out = new RandomAccessFile(dst, "rw");
        long remaining = in.length();
        while (remaining > 0) {
            int toRead = (int)Math.min(memPool.length, remaining);
            in.readFully(memPool, 0, toRead);
            out.write(memPool, 0, toRead);
            remaining -= toRead;
        }
        in.close();
        out.close();
    }
}
