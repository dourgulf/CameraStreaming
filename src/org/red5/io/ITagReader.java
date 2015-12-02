package org.red5.io;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2010 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

public interface ITagReader {

	/**
	 * Return the file that is loaded.
	 * 
	 * @return the file to be loaded
	 */
	public IStreamableFile getFile();

	/**
	 * Returns the offet length
	 * 
	 * @return int
	 */
	public int getOffset();

	/**
	 * Returns the amount of bytes read
	 * 
	 * @return long
	 */
	public long getBytesRead();

	/**
	 * Return length in seconds
     * @return length in seconds
     */
	public long getDuration();

	/**
	 * Get the total readable bytes in a file or ByteBuffer
	 *
	 * @return          Total readable bytes
	 */
	public long getTotalBytes();
	
	/**
	 * Decode the header of the stream;
	 *
	 */
	public void decodeHeader();

	/**
	 * Move the reader pointer to given position in file.
	 * 
	 * @param pos File position to move to
	 */
	public void position(long pos);

	/**
	 * Returns a boolean stating whether the FLV has more tags
	 * 
	 * @return boolean
	 */
	public boolean hasMoreTags();

	/**
	 * Returns a Tag object
	 * 
	 * @return Tag
	 */
	public ITag readTag();

	/**
	 * Closes the reader and free any allocated memory.
	 */
	public void close();
	
	/**
	 * Check if the reader also has video tags.
	 * 
	 * @return has video
	 */
	public boolean hasVideo();

}
