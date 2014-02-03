package scfs.general;
/**
 * 
 */

/**
 * @author rmendes
 *
 */
public class SCFSConstants {

	public static final String LOCK_PREFIX = "Lock_";

	public static final int FILE_OR_DIR_INDEX = 0;
	public static final int PARENT_FOLDER_INDEX = 1; 
	public static final int NAME_INDEX = 2;
	public static final int STATS_INDEX = 3;
	public static final int ID_PATH_INDEX = 4;
	public static final int ACCESS_KEY_INDEX = 5;
	
	public static final int BLOCK_SIZE = 512;
	
	// Open flags mask 
	public static final int O_ACCMODE = 0x400003;
	// ++++++++
	
	// File type Modes
	public static final int S_IFMT = 0x0170000; /* type of file */
	public static final int S_IFLNK = 0x0120000; /* symbolic link */
	public static final int S_IFREG = 0x0100000; /* regular */
	public static final int S_IFBLK = 0x0060000; /* block special */
	public static final int S_IFDIR = 0x0040000; /* directory */
	public static final int S_IFCHR = 0x0020000; /* character special */
	public static final int S_IFIFO = 0x0010000; /* this is a FIFO */
	public static final int S_ISUID = 0x0004000; /* set user id on execution */
	// ++++++++

	// Access permitions mask
	public static final int S_IRWXU   = 00700;         /* owner:  rwx------ */
	public static final int S_IRUSR   = 00400;         /* owner:  r-------- */
	public static final int S_IWUSR   = 00200;         /* owner:  -w------- */
	public static final int S_IXUSR   = 00100;         /* owner:  --x------ */

	public static final int S_IRWXG   = 00070;         /* group:  ---rwx--- */
	public static final int S_IRGRP   = 00040;         /* group:  ---r----- */
	public static final int S_IWGRP   = 00020;         /* group:  ----w---- */
	public static final int S_IXGRP   = 00010;         /* group:  -----x--- */

	public static final int S_IRWXO   = 00007;         /* others: ------rwx */
	public static final int S_IROTH   = 00004;         /* others: ------r-- */ 
	public static final int S_IWOTH   = 00002;         /* others: -------w- */
	public static final int S_IXOTH   = 00001;         /* others: --------x */
	// ++++++++


}
