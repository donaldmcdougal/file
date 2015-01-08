/**
 * 
 */
package com.donaldmcdougal.utils.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

/**
 * Utilities for dealing with files.
 * @author Donald McDougal
 *
 */
public class FileUtils {

	/**
	 * Copies the source directory to the target directory.
	 * 
	 * @param source The directory to copy.
	 * @param target The directory in which to place the copy.
	 * @return <code>true</code> if the directory was copied; <code>false</code> otherwise.
	 */
	public boolean copyDirectory(final Path source, final Path target) {
		try {
			Files.walkFileTree(source,
			EnumSet.of(FileVisitOption.FOLLOW_LINKS),
			Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir,
						BasicFileAttributes attrs) throws IOException {
					Path targetdir = target.resolve(source
							.relativize(dir));
					try {
						Files.copy(dir, targetdir);
					} catch (FileAlreadyExistsException e) {
						if (!Files.isDirectory(targetdir))
							throw e;
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file,
						BasicFileAttributes attrs) throws IOException {
					Files.copy(file,
							target.resolve(source.relativize(file)));
					return FileVisitResult.CONTINUE;
				}
			});

			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Deletes a path recursively.
	 * @param path The path to delete.
	 * @throws IOException if an I/O error occurs.
	 */
	public static void deleteRecursively(Path path) throws IOException {
		
		Files.walkFileTree(path, new SimpleFileVisitor<Path>()
	    {
			/*
			 * (non-Javadoc)
			 * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
			 */
	        @Override
	        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
	        {
	            Files.delete(file);
	            return FileVisitResult.CONTINUE;
	        }

	        /*
	         * (non-Javadoc)
	         * @see java.nio.file.SimpleFileVisitor#visitFileFailed(java.lang.Object, java.io.IOException)
	         */
	        @Override
	        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException
	        {
	            // try to delete the file anyway, even if its attributes
	            // could not be read, since delete-only access is
	            // theoretically possible
	            Files.delete(file);
	            return FileVisitResult.CONTINUE;
	        }

	        /*
	         * (non-Javadoc)
	         * @see java.nio.file.SimpleFileVisitor#postVisitDirectory(java.lang.Object, java.io.IOException)
	         */
	        @Override
	        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
	        {
	            if (exc == null)
	            {
	                Files.delete(dir);
	                return FileVisitResult.CONTINUE;
	            }
	            else
	            {
	                // directory iteration failed; propagate exception
	                throw exc;
	            }
	        }
	    });
	}
	
	/**
	 * Gets the number of chunks that would be created from a file with the given size and the size of each chunk.
	 * @param fileSize The size of the file, in bytes.
	 * @param chunkSize The size of each piece of the file after splitting.
	 * @return The number of chunks that would be created, or -1 if it is impossible to calculate the number of chunks
	 * because the parameters are bad.
	 */
	public int getFileChunkCount(long fileSize, int chunkSize) {
		if (fileSize < 0) {
			return -1;
		}
		if (chunkSize < 1) {
			return -1;
		}
		
		int numChunks = (int) (fileSize / chunkSize);
		
		if (fileSize % chunkSize > 0) {
			++numChunks;
		}
		
		return numChunks;
	}
	
	/**
	 * Gets the file extension of the provided file.
	 * 
	 * @param file A file for which we want the extension.
	 * @return A file extension as a String, or <code>null</code> if the
	 *         provided File object was a directory.
	 */
	public String getFileExtension(Path file) {
		if (file.toFile().isFile()) {
			String fName = file.toFile().getName().toLowerCase();
			int dotPos = fName.lastIndexOf(".");
			if (dotPos == -1) { // a file with no extension
				return fName;
			} else if (dotPos == fName.length() - 1) { // a file with a dot at the end.
				return "";
			} else {
				return fName.substring(dotPos + 1);
			}
		} else {
			return null;
		}
	}

	/**
	 * Gets the text from the given file as a string.
	 * 
	 * @param file The file to read as a string.
	 * @return The text from the given file as a string.
	 */
	public String getStringFromFile(Path file) {
		try {
			List<String> lines = Files.readAllLines(file,
					Charset.defaultCharset());
			Iterator<String> iter = lines.iterator();
			StringBuilder bldr = new StringBuilder();
			while (iter.hasNext()) {
				bldr.append(iter.next());
				bldr.append(System.getProperty("line.separator"));
			}
			return bldr.toString();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Makes a file hidden.
	 * 
	 * @param file The file to hide.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void hideFile(Path file) {
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			try {
				Process p = Runtime.getRuntime().exec(
						"attrib +h " + file.toFile().getPath());
				p.waitFor();
			} catch (InterruptedException e) {
			} catch (IOException e) {
			}
		}
		// add a dot to the front if it does not already contain one.
		// This way, if the file is moved to Linux, it will still be hidden.
		String localFileName = file.toFile().getName();
		if (localFileName.charAt(0) != '.') {
			localFileName = "." + localFileName;
		}
		String newFileName = file.getParent()
				+ System.getProperty("file.separator") + localFileName;
		file.toFile().renameTo(new File(newFileName));
	}
	
	/**
	 * Joins a file together from its split pieces.
	 * @param toCreate The file to create from split pieces.
	 * @param splitFilesDir The directory containing the pieces of the file.
	 * @param splitFilePrefix The prefix of the pieces of the file.
	 * @param deletePieces Whether or not to delete the file pieces after joining them together.
	 * @throws IOException 
	 */
	public void joinFile(Path toCreate, Path splitFilesDir, String splitFilePrefix, boolean deletePieces) throws IOException {
		this.joinFile(toCreate.toFile(), splitFilesDir.toFile(), splitFilePrefix, deletePieces);
	}
	
	/**
	 * Lists all files in a directory, or the file itself if the provided path is a file.
	 * @param path The path of the file or directory.
	 * @return All files in a directory, or the file itself if the provided path is a file.
	 * @throws IOException
	 */
	public static List<Path> listFilesRecursively(Path path) throws IOException {
		List<Path> paths = new ArrayList<Path>();
		
		Files.walkFileTree(path, new SimpleFileVisitor<Path>()
	    {
			/*
			 * (non-Javadoc)
			 * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
			 */
	        @Override
	        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
	        {
	            paths.add(file);
	            return FileVisitResult.CONTINUE;
	        }
	    });
		
		return paths;
	}
	
	/**
	 * Splits a file into chunks.  Appends ".{chunkPiece}" to the end of each file to indicate how to 
	 * reassemble the files.
	 * @param toSplit The file to split.
	 * @param chunkSize The size of each piece after splitting.
	 * @param splitFilesDir The directory in which to place the pieces of the file.
	 * @param splitFilePrefix The prefix to give to the split file.  For example,
	 * if the prefix is "a", and the file is split into three pieces, then the pieces will
	 * be called a.1, a.2, and a.3.  splitFilePrefix should be unique per file.
	 * @return The number of chunks created.
	 * @throws IOException 
	 */
	public int splitFile(Path toSplit, int chunkSize, Path splitFilesDir, String splitFilePrefix) throws IOException {
		return this.splitFile(toSplit.toFile(), chunkSize, splitFilesDir.toFile(), splitFilePrefix);
	}
	
	/**
	 * Unhides a file, even if it's a Windows system file.
	 * @param file The file to make visible.
	 */
	public void unhideFile(Path file) {
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			try {
				Process p = Runtime.getRuntime().exec(
						"attrib -h -s " + file.toFile().getPath());
				p.waitFor();
			} catch (InterruptedException e) {
			} catch (IOException e) {
			}
		}
		//if there are any dots at the beginning, remove them.  This way, even if the
		//file is moved to Linux, it will still be unhidden.
		String localFileName = file.toFile().getName();
		while (localFileName.charAt(0) == '.') {
			localFileName = localFileName.substring(1);
		}
		String newFileName = file.getParent()
				+ System.getProperty("file.separator") + localFileName;
		file.toFile().renameTo(new File(newFileName));
	}

	/**
	 * Writes a string to the provided file.
	 * 
	 * @param str The String to write to the file.
	 * @param file The File which we are writing.
	 * @return <code>true</code> if the write was successful; <code>false</code>
	 *         otherwise.
	 */
	public boolean writeStringToFile(String str, Path file) {
		try {
			FileWriter fWriter = new FileWriter(file.toFile());
			BufferedWriter out = new BufferedWriter(fWriter);
			out.write(str);
			out.close();
			fWriter.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Joins a file together from its split pieces.
	 * @param toCreate The file to create from split pieces.
	 * @param splitFilesDir The directory containing the pieces of the file.
	 * @param splitFilePrefix The prefix of the pieces of the file.
	 * @param deletePieces Whether or not to delete the file pieces after joining them together.
	 * @throws IOException 
	 */
	private void joinFile(File toCreate, File splitFilesDir, String splitFilePrefix, boolean deletePieces) throws IOException {
		
		int i = 1;
		
		File splitFile = new File(splitFilesDir.getAbsolutePath() + 
				System.getProperty("file.separator") + 
				splitFilePrefix + "." + i);
		toCreate.createNewFile();
		FileOutputStream os = new FileOutputStream(toCreate);
		
		while (splitFile.exists()) {
			
			os.write(Files.readAllBytes(splitFile.toPath()));
			if (deletePieces) {
				splitFile.delete();
			}
			
			++i;
			splitFile = new File(splitFilesDir.getAbsolutePath() + 
					System.getProperty("file.separator") + 
					splitFilePrefix + "." + i);
		}
		
		try {
			os.close();
		}
		catch (IOException e) {};
	}

	/**
	 * Splits a file into chunks.  Appends ".{chunkPiece}" to the end of each file to indicate how to 
	 * reassemble the files.
	 * @param toSplit The file to split.
	 * @param chunkSize The size of each piece after splitting.
	 * @param splitFilesDir The directory in which to place the pieces of the file.
	 * @param splitFilePrefix The prefix to give to the split file.  For example,
	 * if the prefix is "a", and the file is split into three pieces, then the pieces will
	 * be called a.1, a.2, and a.3.  splitFilePrefix should be unique per file.
	 * @return The number of chunks created.
	 * @throws IOException 
	 */
	private int splitFile(File toSplit, int chunkSize, File splitFilesDir, String splitFilePrefix) throws IOException {
		
		int chunkCount = getFileChunkCount(toSplit.length(), chunkSize);
		InputStream ios = new FileInputStream(toSplit);
		byte[] buffer = new byte[chunkSize];
		
		try {
			
			for (int i = 1; i <= chunkCount; i++) {
				
				FileOutputStream ous = null;
				
				try {
					ous = new FileOutputStream(splitFilesDir.getAbsolutePath() + 
							System.getProperty("file.separator") + 
							splitFilePrefix + "." + 
							i);
					ous.write(buffer, 0, ios.read(buffer));
				}
				finally {
					try {
						if (ous != null) {
							ous.close();
						}
					}
					catch (IOException e) {}
				}
			}
		}
		finally {
			try {
				if (ios != null) {
					ios.close();
				}
			}
			catch (IOException e) {}
		}
		
		return chunkCount;
	}
}
