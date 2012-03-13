package fr.nom.tam.fits;

/*
 * Copyright: Thomas McGlynn 1997-1999.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 */
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.URL;
import java.util.NoSuchElementException;
import java.util.Vector;

import java.util.zip.GZIPInputStream;
import fr.nom.tam.util.ArrayDataInput;
import fr.nom.tam.util.ArrayDataOutput;
import fr.nom.tam.util.BufferedDataInputStream;
import fr.nom.tam.util.BufferedDataOutputStream;
import fr.nom.tam.util.BufferedFile;

/** This class provides access to routines to allow users
 * to read and write FITS files.
 * <p>
 *
 * <p>
 * <b> Description of the Package </b>
 * <p>
 * This FITS package attempts to make using FITS files easy,
 * but does not do exhaustive error checking.  Users should
 * not assume that just because a FITS file can be read
 * and written that it is necessarily legal FITS.
 *
 *
 * <ul>
 * <li> The Fits class provides capabilities to
 *      read and write data at the HDU level, and to
 *      add and delete HDU's from the current Fits object.
 *      A large number of constructors are provided which
 *      allow users to associate the Fits object with
 *      some form of external data.  This external
 *      data may be in a compressed format.
 * <li> The HDU class is a factory class which is used to
 *      create HDUs.  HDU's can be of a number of types
 *      derived from the abstract class BasicHDU.
 *      The hierarchy of HDUs is:
 *      <ul>
 *      <li>BasicHDU
 *           <ul>
 *           <li> ImageHDU
 *           <li> RandomGroupsHDU
 *           <li> TableHDU
 *                <ul>
 *                <li> BinaryTableHDU
 *                <li> AsciiTableHDU
 *                </ul>
 *           </ul>
 *       </ul>
 *
 * <li> The Header class provides many functions to
 *      add, delete and read header keywords in a variety
 *      of formats.
 * <li> The HeaderCard class provides access to the structure
 *      of a FITS header card.
 * <li> The Data class is an abstract class which provides
 *      the basic methods for reading and writing FITS data.
 *      Users will likely only be interested in the getData
 *      method which returns that actual FITS data.
 * <li> The TableHDU class provides a large number of
 *      methods to access and modify information in
 *      tables.
 * <li> The Column class
 *      combines the Header information and Data corresponding to
 *      a given column.
 * </ul>
 *
 * Thomas McGlynn 1997-2009.
 * This code may be used for any purpose, non-commercial
 * or commercial.
 *
 * @version 1.4.0  December 24, 2009
 */
public final class Fits {

  /** The input stream associated with this Fits object.
   */
  private ArrayDataInput dataStr;
  /** A vector of HDUs that have been added to this
   * Fits object.
   */
  private Vector hduList = new Vector();
  /** Has the input stream reached the EOF?
   */
  private boolean atEOF;
  /** The last offset we reached.
   *  A -1 is used to indicate that we
   *  cannot use the offset.
   */
  private long lastFileOffset = -1;
  /** What might URLs being with? */
  private static String[] urlProtocols = {"http:", "ftp:", "https:", "file:"};
  /** Is a compressed stream GZIP or Compress compressed */
  private boolean gzipCompress = false;

  /** Indicate the version of these classes */
  public static String version() {

    // Version 0.1: Original test FITS classes -- 9/96
    // Version 0.2: Pre-alpha release 10/97
    //              Complete rewrite using BufferedData*** and
    //              ArrayFuncs utilities.
    // Version 0.3: Pre-alpha release  1/98
    //              Incorporation of HDU hierarchy developed
    //              by Dave Glowacki and various bug fixes.
    // Version 0.4: Alpha-release 2/98
    //              BinaryTable classes revised to use
    //              ColumnTable classes.
    // Version 0.5: Random Groups Data 3/98
    // Version 0.6: Handling of bad/skipped FITS, FitsDate (D. Glowacki) 3/98
    // Version 0.9: ASCII tables, Tiled images, Faux, Bad and SkippedHDU class
    //              deleted. 12/99
    // Version 0.91: Changed visibility of some methods.
    //               Minor fixes.
    // Version 0.92: Fix bug in BinaryTable when reading from stream.
    // Version 0.93: Supports HIERARCH header cards.  Added FitsElement interface.
    //               Several bug fixes especially for null HDUs.
    // Version 0.96: Address issues with mandatory keywords.
    //               Fix problem where some keywords were not properly keyed.
    // Version 0.96a: Update version in FITS
    // Version 0.99: Added support for Checksums (thanks to RJ Mathar).
    //               Fixed bug with COMMENT and HISTORY keywords (Rose Early)
    //               Changed checking for compression and fixed bug with TFORM
    //               handling in binary tables (Laurent Michel)
    //               Distinguished arrays of length 1 from scalars in
    //               binary tables (Jorgo Bakker)
    //               Fixed bug in handling of length 0 values in headers (Fred Romerfanger, Jorgo Bakker)
    //               Truncated BufferedFiles when finishing write (but only
    //               for FITS file as a whole.)
    //               Fixed bug writing binary tables with deferred reads.
    //               Made addLine methods in Header public.
    //               Changed ArrayFuncs.newInstance to handle inputs with dimensionality of 0.
    // Version 0.99.1
    //               Added deleteRows and deleteColumns functionality to all tables.
    //               This includes changes
    //               to TableData, TableHDU, AsciiTable, BinaryTable and util/ColumnTable.
    //               Row deletions were suggested by code of R. Mathar but this works
    //               on all types of tables and implements the deletions at a lower level.
    //		  Completely revised util.HashedList to use more standard features from
    //               Collections.  The HashedList now melds a HashMap and ArrayList
    //               Added sort to HashedList function to enable sorting of the list.
    //               The logic now uses a simple index for the iterators rather than
    //               traversing a linked list.
    //               Added sort before write in Header to ensure keywords are in correct order.
    //               This uses a new HeaderOrder class which implements java.util.Comparator to
    //               indicate the required order for FITS keywords.  Users should now
    //               be able to write required keywords anywhere without getting errors
    //               later when they try to write out the FITS file.
    //               Fixed bug in setColumn in util.Column table where the new column
    //               was not being pointed to.  Any new column resets the table.
    //               Several fixes to BinaryTable to address errors in variable length
    //               array handling.
    //               Several fixes to the handling of variable length array in binary tables.
    //               (noted by Guillame Belanger).
    //               Several fixes and changes suggested by Richard Mathar mostly
    //               in BinaryTable.
    //  Version 0.99.2
    //               Revised test routines to use Junit.  Note that Junit tests
    //               use annotations and require Java 1.5.
    //               Added ArrayFuncs.arrayEquals() methods to compare
    //               arbitrary arrays.
    //               Fixed bugs in handling of 0 length columns and table update.
    //  Version 0.99.3
    //               Additional fixes for 0 length strings.
    //  Version 0.99.4
    //               Changed handling of constructor for File objects
    //          0.99.5
    //               Add ability to handle FILE, HTTPS and FTP URLs and to
    //               handle redirects amongst different protocols.
    //          0.99.5
    //               Fixes to String handling (A. Kovacs)
    //               Truncating long doubles to fit in
    //               standard header.
    //               Made some methods public in FitsFactory
    //               Added Set
    //          0.99.6
    //               Fix to BinaryTable (L. Michel)
    //   Version 1.00.0
    //               Support for .Z compressed data.
    //               Better detection of compressed data streams
    //               Bug fix for binary tables (A. Kovacs)
    //   Version 1.00.1 (2/09)
    //               Fix for exponential format in header keywords
    //   Version 1.00.2 (3/09)
    //               Fixed offsets when users read rows or elements
    //               within multiHDU files.
    //   Version 1.01.0
    //               Fixes bugs and adds some more graceful
    //               error handling for situations where arrays
    //               could exceed 2G.  More work is needed here though.
    //               Data.getTrueSize() now returns a long.
    //
    //               Fixed bug with initial blanks in HIERARCH
    //               values.
    //    Version 1.02.0 (7/09)
    //               Fixes bugs in ASCII tables where integer and real
    //               fields that are blank should be read as 0 per the FITS
    //               standard. (L. Michel)
    //
    //               Adds PaddingException to allow users to read
    //               improperly padded last HDU in FITS file. (suggested by L. Michel)
    //               This required changes to the Fits.java and all of the Data subclasses
    //               as well as the new exception classes.
    //     Version 1.03.0 (7/09)
    //               Many changes to support long (>2GB) arrays in
    //               reads and size computations:
    //                   ArrayDataInput.readArray deprecated in
    //                    favor of ArrayDataInput.readLArray
    //                   ArrayUtil.computeSize -> ArrayUtil.computeLSize
    //                   ArrayUtil.nElements   -> ArrayUtil.nLElements
    //               The skipBytes method in ArrayDataInput is overloaded
    //               to take a long argument and return a long value (in
    //               addition to the method inherited from DataInput
    //               the takes and returns an int).
    //
    //               Corresponding changes in FITS classes.
    //               [Note that there are still many restrictions
    //               due to the array size limits in Java.]
    //
    //               A number of obsolete comments regarding BITPIX=64 being non-standard
    //               were removed.
    //               If errors are found in reading the Header of an HDU
    //               an IOException is now returned in some situations
    //               where an Error was begin returned.
    //
    //               A bug in the new PaddingException was fixed that
    //               lets truncated ImageHDUs have Tilers.
    //
    //      Version 1.03.1 (7/09)
    //               Changed FitsUtil.byteArrayToStrings to make
    //               sure that deleted white space is eligible for
    //               garbage collection. (J.C. Segovia)
    //
    //      Version 1.04.0 (12/09)
    //
    //               Added support for the long string convention
    //               (see JavaDocs for Header).
    //               Fixed errors in handling of strings with embedded
    //               quotes.
    //               Other minor bugs.
    //
    return "1.040";
  }

  /** Create an empty Fits object which is not
   * associated with an input stream.
   */
  public Fits() {
  }

  /** Create a Fits object associated with
   * the given uncompressed data stream.
   * @param str The data stream.
   */
  public Fits(InputStream str) throws FitsException {
    streamInit(str, false);
  }

  /** Create a Fits object associated with a possibly
   * compressed data stream.
   * @param str The data stream.
   * @param compressed Is the stream compressed?
   */
  public Fits(InputStream str, boolean compressed)
          throws FitsException {
    streamInit(str, false);
  }

  /** See if a stream is compressed before moving on.
   */
  protected void streamInit(InputStream str, boolean seekable)
          throws FitsException {

    // We ignore the user's specification that the
    // stream is compressed except that it means we don't
    // try to use a random access input.

    if (str == null) {
      throw new FitsException("Null stream in constructor");
    }

    PushbackInputStream pb = new PushbackInputStream(str, 2);

    int mag1 = -1;
    int mag2 = -1;

    try {
      mag1 = pb.read();
      mag2 = pb.read();

      // Push the data back into the stream
      pb.unread(mag2);
      pb.unread(mag1);

    } catch (IOException e) {
      throw new FitsException("Unable to peek on input stream:" + e);
    }
    if (mag1 == 'S' && mag2 == 'I') {
      streamInit(pb, false, seekable);
      return;

    } else if (mag1 == 0x1f) {
      if (mag2 == 0x8b) {
        gzipCompress = true;
        streamInit(pb, true, false);
        return;
      } else if (mag2 == 0x9d) {
        gzipCompress = false;
        streamInit(pb, true, false);
        return;
      }
    }
    throw new FitsException("Invalid Magic number for FITS file");
  }
  
  /**
   * Return true if the file is gzip compressed
   * @return true if the file is gzip compressed 
   */
  public boolean isGzipCompressed() {
      return gzipCompress;
  }

  /** Do the stream initialization.
   *
   * @param str The input stream.
   * @param compressed Is this data compressed?  If so,
   *            then the GZIPInputStream class will be
   *            used to inflate it.
   */
  protected void streamInit(InputStream str, boolean compressed,
                            boolean seekable)
          throws FitsException {

    if (compressed) {
      if (gzipCompress) {
        try {
          str = new GZIPInputStream(str);
        } catch (IOException e) {
          throw new FitsException("Cannot inflate input stream" + e);
        }

      } else { // Compress
        try {
          final Process proc = new ProcessBuilder("uncompress", "-c").start();
          final InputStream src = str;

          // This is the input to the process -- but
          // an output from here.
          final OutputStream input = proc.getOutputStream();

          // Now copy everything in a separate thread.
          Thread copier = new Thread(
                  new Runnable() {

                    public void run() {
                      try {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = src.read(buffer, 0, buffer.length)) > 0) {
                          input.write(buffer, 0, len);
                        }
                        src.close();
                        input.close();
                      } catch (IOException e) {
                        return;
                      }
                    }
                  });
          copier.start();
          str = proc.getInputStream();
        } catch (Exception e) {
          throw new FitsException("Unable to read .Z compressed stream.\nIs `uncompress' in the path?\n:" + e);
        }
      }
    }

    if (str instanceof ArrayDataInput) {
      dataStr = (ArrayDataInput) str;
    } else {
      // Use efficient blocking for input.
      dataStr = new BufferedDataInputStream(str);
    }
  }

  /** Initialize using buffered random access */
  protected void randomInit(File f) throws FitsException {

    String permissions = "r";
    if (!f.exists() || !f.canRead()) {
      throw new FitsException("Non-existent or unreadable file");
    }
    if (f.canWrite()) {
      permissions += "w";
    }
    try {
      dataStr = new BufferedFile(f, permissions);

      ((BufferedFile) dataStr).seek(0);
    } catch (IOException e) {
      throw new FitsException("Unable to open file " + f.getPath());
    }
  }

  /** Associate FITS object with an uncompressed File
   * @param myFile The File object.
   */
  public Fits(File myFile) throws FitsException {
    this(myFile, FitsUtil.isCompressed(myFile));
  }

  /** Associate the Fits object with a File
   * @param myFile The File object.
   * @param compressed Is the data compressed?
   */
  public Fits(File myFile, boolean compressed) throws FitsException {
    fileInit(myFile, compressed);
  }

  /** Get a stream from the file and then use the stream initialization.
   * @param myFile  The File to be associated.
   * @param compressed Is the data compressed?
   */
  protected void fileInit(File myFile, boolean compressed) throws FitsException {

    try {
      if (compressed) {
        FileInputStream str = new FileInputStream(myFile);
        streamInit(str, true);
      } else {
        randomInit(myFile);
      }
    } catch (IOException e) {
      throw new FitsException("Unable to create Input Stream from File: " + myFile);
    }
  }

  /** Associate the FITS object with a file or URL.
   *
   * The string is assumed to be a URL if it begins one of the
   * protocol strings.
   * If the string ends in .gz it is assumed that
   * the data is in a compressed format.
   * All string comparisons are case insensitive.
   *
   * @param filename  The name of the file or URL to be processed.
   * @exception FitsException Thrown if unable to find or open
   *                          a file or URL from the string given.
   **/
  public Fits(String filename) throws FitsException {
    this(filename, FitsUtil.isCompressed(filename));
  }

  /** Associate the FITS object with a file or URL.
   *
   * The string is assumed to be a URL if it begins one of the
   * protocol strings.
   * If the string ends in .gz it is assumed that
   * the data is in a compressed format.
   * All string comparisons are case insensitive.
   *
   * @param filename  The name of the file or URL to be processed.
   * @exception FitsException Thrown if unable to find or open
   *                          a file or URL from the string given.
   **/
  public Fits(String filename, boolean compressed) throws FitsException {

    InputStream inp;

    if (filename == null) {
      throw new FitsException("Null FITS Identifier String");
    }

    try {
      InputStream is = FitsUtil.getURLStream(new URL(filename), 0);
      streamInit(is, false);
      return;
    } catch (Exception e) {
      // Just try it as a file
    }

    File fil = new File(filename);
    if (fil.exists()) {
      fileInit(fil, compressed);
      return;
    }


    try {
      InputStream str = ClassLoader.getSystemClassLoader().getResourceAsStream(filename);
      streamInit(str, false);
    } catch (Exception e) {
      //
    }

  }

  /** Associate the FITS object with a given uncompressed URL
   * @param myURL  The URL to be associated with the FITS file.
   * @exception FitsException Thrown if unable to use the specified URL.
   */
  public Fits(URL myURL) throws FitsException {
    this(myURL, FitsUtil.isCompressed(myURL.getFile()));
  }

  /** Associate the FITS object with a given URL
   * @param myURL  The URL to be associated with the FITS file.
   * @param compressed Is the data compressed?
   * @exception FitsException Thrown if unable to find or open
   *                          a file or URL from the string given.
   */
  public Fits(URL myURL, boolean compressed) throws FitsException {
    try {
      streamInit(FitsUtil.getURLStream(myURL, 0), false);
    } catch (IOException e) {
      throw new FitsException("Unable to open input from URL:" + myURL);
    }
  }

  /** Return all HDUs for the Fits object.   If the
   * FITS file is associated with an external stream make
   * sure that we have exhausted the stream.
   * @return an array of all HDUs in the Fits object.  Returns
   * null if there are no HDUs associated with this object.
   */
  public BasicHDU[] read() throws FitsException {

    readToEnd();

    int size = getNumberOfHDUs();

    if (size == 0) {
      return null;
    }

    BasicHDU[] hdus = new BasicHDU[size];
    hduList.copyInto(hdus);
    return hdus;
  }

  /** Read the next HDU on the default input stream.
   * @return The HDU read, or null if an EOF was detected.
   * Note that null is only returned when the EOF is detected immediately
   * at the beginning of reading the HDU.
   */
  public BasicHDU readHDU() throws FitsException, IOException {

    if (dataStr == null || atEOF) {
      return null;
    }

    if (!gzipCompress && lastFileOffset > 0) {
      FitsUtil.reposition(dataStr, lastFileOffset);
    }

    Header hdr = Header.readHeader(dataStr);
    if (hdr == null) {
      atEOF = true;
      return null;
    }

    Data datum = hdr.makeData();
    try {
      datum.read(dataStr);
    } catch (PaddingException e) {
      e.updateHeader(hdr);
      throw e;
    }

    lastFileOffset = FitsUtil.findOffset(dataStr);
    BasicHDU nextHDU = FitsFactory.HDUFactory(hdr, datum);

    hduList.addElement(nextHDU);
    return nextHDU;
  }

  /** Skip HDUs on the associate input stream.
   * @param n The number of HDUs to be skipped.
   */
  public void skipHDU(int n) throws FitsException, IOException {
    for (int i = 0; i < n; i++) {
      skipHDU();
    }
  }

  /** Skip the next HDU on the default input stream.
   */
  public void skipHDU() throws FitsException, IOException {

    if (atEOF) {
      return;
    } else {
      Header hdr = new Header(dataStr);
      if (hdr == null) {
        atEOF = true;
        return;
      }
      int dataSize = (int) hdr.getDataSize();
      dataStr.skip(dataSize);
    }
  }

  /** Return the n'th HDU.
   * If the HDU is already read simply return a pointer to the
   * cached data.  Otherwise read the associated stream
   * until the n'th HDU is read.
   * @param n The index of the HDU to be read.  The primary HDU is index 0.
   * @return The n'th HDU or null if it could not be found.
   */
  public BasicHDU getHDU(int n) throws FitsException, IOException {

    int size = getNumberOfHDUs();

    for (int i = size; i <= n; i++) {
      BasicHDU hdu;
      hdu = readHDU();
      if (hdu == null) {
        return null;
      }
    }

    try {
      return (BasicHDU) hduList.elementAt(n);
    } catch (NoSuchElementException e) {
      throw new FitsException("Internal Error: hduList build failed");
    }
  }

  /** Read to the end of the associated input stream */
  private void readToEnd() throws FitsException {

    while (dataStr != null && !atEOF) {
      try {
        if (readHDU() == null) {
          break;
        }
      } catch (IOException e) {
        throw new FitsException("IO error: " + e);
      }
    }
  }

  /** Return the number of HDUs in the Fits object.   If the
   * FITS file is associated with an external stream make
   * sure that we have exhausted the stream.
   * @return number of HDUs.
   * @deprecated The meaning of size of ambiguous.  Use
   */
  public int size() throws FitsException {
    readToEnd();
    return getNumberOfHDUs();
  }

  /** Add an HDU to the Fits object.  Users may intermix
   * calls to functions which read HDUs from an associated
   * input stream with the addHDU and insertHDU calls,
   * but should be careful to understand the consequences.
   *
   * @param myHDU  The HDU to be added to the end of the FITS object.
   */
  public void addHDU(BasicHDU myHDU)
          throws FitsException {
    insertHDU(myHDU, getNumberOfHDUs());
  }

  /** Insert a FITS object into the list of HDUs.
   *
   * @param myHDU The HDU to be inserted into the list of HDUs.
   * @param n     The location at which the HDU is to be inserted.
   */
  public void insertHDU(BasicHDU myHDU, int n)
          throws FitsException {

    if (myHDU == null) {
      return;
    }

    if (n < 0 || n > getNumberOfHDUs()) {
      throw new FitsException("Attempt to insert HDU at invalid location: " + n);
    }

    try {

      if (n == 0) {

        // Note that the previous initial HDU is no longer the first.
        // If we were to insert tables backwards from last to first,
        // we could get a lot of extraneous DummyHDUs but we currently
        // do not worry about that.

        if (getNumberOfHDUs() > 0) {
          ((BasicHDU) hduList.elementAt(0)).setPrimaryHDU(false);
        }

        if (myHDU.canBePrimary()) {
          myHDU.setPrimaryHDU(true);
          hduList.insertElementAt(myHDU, 0);
        } else {
          insertHDU(BasicHDU.getDummyHDU(), 0);
          myHDU.setPrimaryHDU(false);
          hduList.insertElementAt(myHDU, 1);
        }
      } else {
        myHDU.setPrimaryHDU(false);
        hduList.insertElementAt(myHDU, n);
      }
    } catch (NoSuchElementException e) {
      throw new FitsException("hduList inconsistency in insertHDU");
    }

  }

  /** Delete an HDU from the HDU list.
   *
   * @param n  The index of the HDU to be deleted.
   *           If n is 0 and there is more than one HDU present, then
   *           the next HDU will be converted from an image to
   *           primary HDU if possible.  If not a dummy header HDU
   *           will then be inserted.
   */
  public void deleteHDU(int n) throws FitsException {
    int size = getNumberOfHDUs();
    if (n < 0 || n >= size) {
      throw new FitsException("Attempt to delete non-existent HDU:" + n);
    }
    try {
      hduList.removeElementAt(n);
      if (n == 0 && size > 1) {
        BasicHDU newFirst = (BasicHDU) hduList.elementAt(0);
        if (newFirst.canBePrimary()) {
          newFirst.setPrimaryHDU(true);
        } else {
          insertHDU(BasicHDU.getDummyHDU(), 0);
        }
      }
    } catch (NoSuchElementException e) {
      throw new FitsException("Internal Error: hduList Vector Inconsitency");
    }
  }

  /** Write a Fits Object to an external Stream.
   *
   * @param dos  A DataOutput stream.
   */
  public void write(DataOutput os) throws FitsException {

    ArrayDataOutput obs;
    boolean newOS = false;

    if (os instanceof ArrayDataOutput) {
      obs = (ArrayDataOutput) os;
    } else if (os instanceof DataOutputStream) {
      newOS = true;
      obs = new BufferedDataOutputStream((DataOutputStream) os);
    } else {
      throw new FitsException("Cannot create ArrayDataOutput from class "
              + os.getClass().getName());
    }

    BasicHDU hh;
    for (int i = 0; i < getNumberOfHDUs(); i++) {
      try {
        hh = (BasicHDU) hduList.elementAt(i);
        hh.write(obs);
      } catch (ArrayIndexOutOfBoundsException e) {
        throw new FitsException("Internal Error: Vector Inconsistency" + e);
      }
    }
    if (newOS) {
      try {
        obs.flush();
        obs.close();
      } catch (IOException e) {
        System.err.println("Warning: error closing FITS output stream");
      }
    }
    try {
      if (obs instanceof BufferedFile) {
        ((BufferedFile) obs).setLength(((BufferedFile) obs).getFilePointer());
      }
    } catch (IOException e) {
      // Ignore problems...
    }

  }

  /** Read a FITS file from an InputStream object.
   *
   * @param is The InputStream stream whence the FITS information
   *            is found.
   */
  public void read(InputStream is) throws FitsException, IOException {

    if (is instanceof ArrayDataInput) {
      dataStr = (ArrayDataInput) is;
    } else {
      dataStr = new BufferedDataInputStream(is);
    }

    read();
  }

  /** Get the current number of HDUs in the Fits object.
   * @return The number of HDU's in the object.
   * @deprecated See getNumberOfHDUs()
   */
  public int currentSize() {
    return hduList.size();
  }

  /** Get the current number of HDUs in the Fits object.
   * @return The number of HDU's in the object.
   */
  public int getNumberOfHDUs() {
    return hduList.size();
  }

  /** Get the data stream used for the Fits Data.
   * @return The associated data stream.  Users may wish to
   *         call this function after opening a Fits object when
   *         they wish detailed control for writing some part of the FITS file.
   */
  public ArrayDataInput getStream() {
    return dataStr;
  }

  /** Set the data stream to be used for future input.
   *
   * @param stream The data stream to be used.
   */
  public void setStream(ArrayDataInput stream) {
    dataStr = stream;
    atEOF = false;
    lastFileOffset = -1;
  }

  /** Create an HDU from the given header.
   *  @param h  The header which describes the FITS extension
   */
  public static BasicHDU makeHDU(Header h) throws FitsException {
    Data d = FitsFactory.dataFactory(h);
    return FitsFactory.HDUFactory(h, d);
  }

  /** Create an HDU from the given data kernel.
   *  @param o The data to be described in this HDU.
   */
  public static BasicHDU makeHDU(Object o) throws FitsException {
    return FitsFactory.HDUFactory(o);
  }

  /** Create an HDU from the given Data.
   *  @param datum The data to be described in this HDU.
   */
  public static BasicHDU makeHDU(Data datum) throws FitsException {
    Header hdr = new Header();
    datum.fillHeader(hdr);
    return FitsFactory.HDUFactory(hdr, datum);
  }

  /**
   * Add or update the CHECKSUM keyword.
   * @param hdr the primary or other header to get the current DATE
   * @return checksum value
   * @throws nom.tam.fits.HeaderCardException
   * @author R J Mathar
   * @since 2005-10-05
   */
  public static long setChecksum(BasicHDU hdu)
          throws fr.nom.tam.fits.HeaderCardException, fr.nom.tam.fits.FitsException, java.io.IOException {
    /* the next line with the delete is needed to avoid some unexpected
     *  problems with non.tam.fits.Header.checkCard() which otherwise says
     *  it expected PCOUNT and found DATE.
     */
    Header hdr = hdu.getHeader();
    hdr.deleteKey("CHECKSUM");
    /* This would need org.nevec.utils.DateUtils compiled before org.nevec.prima.fits ....
     * final String doneAt = DateUtils.dateToISOstring(0) ;
     * We need to save the value of the comment string because this is becoming part
     * of the checksum calculated and needs to be re-inserted again - with the same string -
     * when the second/final call to addVallue() is made below.
     */
    final String doneAt = "as of " + FitsDate.getFitsDateString();
    hdr.addValue("CHECKSUM", "0000000000000000", doneAt);

    /* Convert the entire sequence of 2880 byte header cards into a byte array.
     * The main benefit compared to the C implementations is that we do not need to worry
     * about the particular byte order on machines (Linux/VAX/MIPS vs Hp-UX, Sparc...) supposed that
     * the correct implementation is in the write() interface.
     */
    // LAURENT: prepapre buffer capacity
    final int capacity = (int) (hdr.headerSize() + hdu.getData().getSize() + 2880);
    
    final ByteArrayOutputStream hduByteImage = new ByteArrayOutputStream(capacity);
    hdu.write(new BufferedDataOutputStream(hduByteImage));
    final byte[] data = hduByteImage.toByteArray();
    final long csu = checksum(data);

    /* This time we do not use a deleteKey() to ensure that the keyword is replaced "in place".
     * Note that the value of the checksum is actually independent to a permutation of the
     * 80-byte records within the header.
     */
    hdr.addValue("CHECKSUM", checksumEnc(csu, true), doneAt);
    
    return csu;
  }

  /**
   * Add or Modify the CHECKSUM keyword in all headers.
   * @throws nom.tam.fits.HeaderCardException
   * @throws nom.tam.fits.FitsException
   * @author R J Mathar
   * @since 2005-10-05
   */
  public void setChecksum()
          throws fr.nom.tam.fits.HeaderCardException, fr.nom.tam.fits.FitsException, java.io.IOException {
    for (int i = 0; i < getNumberOfHDUs(); i++) {
      setChecksum(getHDU(i));
    }
  }

  /**
   * Calculate the Seaman-Pence 32-bit 1's complement checksum over the byte stream. The option
   * to start from an intermediate checksum accumulated over another previous
   * byte stream is not implemented.
   * The implementation accumulates in two 64-bit integer values the two low-order and the two
   * high-order bytes of adjacent 4-byte groups. A carry-over of bits is never done within the main
   * loop (only once at the end at reduction to a 32-bit positive integer) since an overflow
   * of a 64-bit value (signed, with maximum at 2^63-1) by summation of 16-bit values could only
   * occur after adding approximately 140G short values (=2^47) (280GBytes) or more. We assume
   * for now that this routine here is never called to swallow FITS files of that size or larger.
   * @param data the byte sequence
   * @return the 32bit checksum in the range from 0 to 2^32-1
   * @see http://heasarc.gsfc.nasa.gov/docs/heasarc/fits/checksum.html
   * @author R J Mathar
   * @since 2005-10-05
   */
  private static long checksum(final byte[] data) {
    long hi = 0;
    long lo = 0;
    final int len = 2 * (data.length / 4);
    // System.out.println(data.length + " bytes") ;
    final int remain = data.length % 4;
    /* a write(2) on Sparc/PA-RISC would write the MSB first, on Linux the LSB; by some kind
     * of coincidence, we can stay with the byte order known from the original C version of
     * the algorithm.
     */
    for (int i = 0; i < len; i += 2) {
      /* The four bytes in this block handled by a single 'i' are each signed (-128 to 127)
       * in Java and need to be masked indivdually to avoid sign extension /propagation.
       */
      hi += (data[2 * i] << 8) & 0xff00L | data[2 * i + 1] & 0xffL;
      lo += (data[2 * i + 2] << 8) & 0xff00L | data[2 * i + 3] & 0xffL;
    }

    /* The following three cases actually cannot happen since FITS records are multiples of 2880 bytes.
     */
    if (remain >= 1) {
      hi += (data[2 * len] << 8) & 0xff00L;
    }
    if (remain >= 2) {
      hi += data[2 * len + 1] & 0xffL;
    }
    if (remain >= 3) {
      lo += (data[2 * len + 2] << 8) & 0xff00L;
    }

    long hicarry = hi >>> 16;
    long locarry = lo >>> 16;
    while (hicarry != 0 || locarry != 0) {
      hi = (hi & 0xffffL) + locarry;
      lo = (lo & 0xffffL) + hicarry;
      hicarry = hi >>> 16;
      locarry = lo >>> 16;
    }
    return (hi << 16) + lo;
  }

  /**
   * Encode a 32bit integer according to the Seaman-Pence proposal.
   * @param c the checksum previously calculated
   * @return the encoded string of 16 bytes.
   * @see http://heasarc.gsfc.nasa.gov/docs/heasarc/ofwg/docs/general/checksum/node14.html#SECTION00035000000000000000
   * @author R J Mathar
   * @since 2005-10-05
   */
  private static String checksumEnc(final long c, final boolean compl) {
    byte[] asc = new byte[16];
    final int[] exclude = {0x3a, 0x3b, 0x3c, 0x3d, 0x3e, 0x3f, 0x40, 0x5b, 0x5c, 0x5d, 0x5e, 0x5f, 0x60};
    final long[] mask = {0xff000000L, 0xff0000L, 0xff00L, 0xffL};
    final int offset = 0x30;	/* ASCII 0 (zero */
    final long value = compl ? ~c : c;
    for (int i = 0; i < 4; i++) {
      final int byt = (int) ((value & mask[i]) >>> (24 - 8 * i));	// each byte becomes four
      final int quotient = byt / 4 + offset;
      final int remainder = byt % 4;
      int[] ch = new int[4];
      for (int j = 0; j < 4; j++) {
        ch[j] = quotient;
      }

      ch[0] += remainder;
      boolean check = true;
      for (; check;) // avoid ASCII punctuation
      {
        check = false;
        for (int k = 0; k < exclude.length; k++) {
          for (int j = 0; j < 4; j += 2) {
            if (ch[j] == exclude[k] || ch[j + 1] == exclude[k]) {
              ch[j]++;
              ch[j + 1]--;
              check = true;
            }
          }
        }
      }

      for (int j = 0; j < 4; j++) // assign the bytes
      {
        asc[4 * j + i] = (byte) (ch[j]);
      }
    }

    // shift the bytes 1 to the right circularly.

    // LAURENT : TODO : use new String(byte[], int offset, int length, String charsetName) with charsetName = 'US_ASCII'
    String resul = new String(asc, 15, 1);
    return resul.concat(new String(asc, 0, 15));
  }

    public boolean isAtEOF() {
        return atEOF;
    }

    public void setAtEOF(final boolean atEOF) {
        this.atEOF = atEOF;
    }

    public long getLastFileOffset() {
        return lastFileOffset;
    }

    public void setLastFileOffset(final long lastFileOffset) {
        this.lastFileOffset = lastFileOffset;
    }
  
  
}
