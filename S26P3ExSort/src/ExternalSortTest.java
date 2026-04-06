import student.TestCase;

/**
 * This class was designed to test the External Sort class.
 * Each tests generates random ascii and binary files of
 * the specified size, then sorts both and then checking
 * each one with the file checker.
 *
 * @author CS3114/5040 Staff
 * @version Spring 2026
 */
public class ExternalSortTest extends TestCase
{
    /**
     * The file checker used to verify sorted output
     */
    private CheckFile fileChecker;

    /**
     * This method sets up the tests that follow.
     */
    public void setUp()
    {
        fileChecker = new CheckFile();
    }


    // ----------------------------------------------------------
    /**
     * Helper method for the tests: Run a test suite for a
     * given size. Creates two files (one ascii and one binary)
     * of the specified size, then for each one, runs the sort
     * and runs the checker.
     *
     * @param fileSize Number of (4096 byte) blocks to test
     * @throws Exception if sort or file check fails
     */
    public void sortHelper(int fileSize)
        throws Exception
    {

        FileGenerator it = new FileGenerator();
        String namea = "input" + fileSize + "asave.bin";
        String nameb = "input" + fileSize + "bsave.bin";
        it.generateFile(namea, fileSize, "a");
        it.generateFile(nameb, fileSize, "b");
        String[] args = new String[1];

        String testFilea = "testa" + fileSize + ".bin";
        args[0] = testFilea;
        SortUtils.copyFile(namea, testFilea);
        System.out.println("Sorting " + testFilea);
        ExternalSortProj.main(args);
        assertTrue(fileChecker.checkFileA(
            testFilea, fileSize));

        String testFileb = "testb" + fileSize + ".bin";
        args[0] = testFileb;
        SortUtils.copyFile(nameb, testFileb);
        System.out.println("Sorting " + testFileb);
        ExternalSortProj.main(args);
        assertTrue(fileChecker.checkFile(
            testFileb, fileSize));
    }


    // ----------------------------------------------------------
    /**
     * Test a file with 1 block (single run, tests fileCopy)
     * @throws Exception if sort or check fails
     */
    public void test1()
        throws Exception
    {
        sortHelper(1);
    }


    // ----------------------------------------------------------
    /**
     * Test a file with 15 blocks (multiple runs, single pass)
     * @throws Exception if sort or check fails
     */
    public void test15()
        throws Exception
    {
        sortHelper(15);
    }


    // ----------------------------------------------------------
    /**
     * Test a file with 200 blocks (multi-pass merge)
     * @throws Exception if sort or check fails
     */
    public void test200()
        throws Exception
    {
        sortHelper(200);
    }
}
