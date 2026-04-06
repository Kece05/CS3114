import java.io.*;

// -------------------------------------------------------------------------
/**
 * Performance test for ExternalSort on larger file sizes.
 * Generates binary files of various sizes and measures sort time.
 *
 * @author kece05
 * @version Mar 25, 2026
 */
public class TestPerf {

    /**
     * Main method to run the performance tests.
     *
     * @param args command line arguments (unused)
     * @throws Exception if file operations fail
     */
    public static void main(String[] args) throws Exception {
        CheckFile fileChecker = new CheckFile();
        FileGenerator gen = new FileGenerator();

        int[] sizes = {100, 200, 500, 1000};
        for (int size : sizes) {
            String na = "tp_" + size + "a.bin";
            gen.generateFile(na, size, "b");

            long t1 = System.currentTimeMillis();
            ExternalSort.sort(na);
            long t2 = System.currentTimeMillis();

            boolean ok = fileChecker.checkFile(na, size);
            int kb = size * 4096 / 1024;
            System.out.println("Size " + size
                + " blocks (" + kb + "KB): "
                + (t2 - t1) + "ms, correct=" + ok);
            new File(na).delete();
        }
    }
}
