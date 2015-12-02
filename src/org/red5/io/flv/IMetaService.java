package org.red5.io.flv;

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

import java.io.File;
import java.io.IOException;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * IMetaService Defines the MetaData Service API
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public interface IMetaService {

	// Get FLV from FLVService
	// grab a reader from FLV	
	// Set up CuePoints
	// Set up MetaData
	// Pass CuePoint array into MetaData
	// read in current MetaData if there is MetaData
	// if there isn't MetaData, write new MetaData
	// Call writeMetaData method on MetaService
	// that in turn will write the current metadata
	// and the cuepoint data
	// after that, call writeMetaCue()
	// this will loop through all the tags making
	// sure that the cuepoints are inserted

	/**
	 * Initiates writing of the MetaData
	 * 
	 * @param meta              Metadata
	 * @throws IOException      I/O exception
	 */
	public void write(IMetaData<?, ?> meta) throws IOException;

	/**
	 * Writes the MetaData
	 * 
	 * @param metaData          Metadata
	 */
	public void writeMetaData(IMetaData<?, ?> metaData);

	/**
	 * Writes the Meta Cue Points
	 */
	public void writeMetaCue();

	/**
	 * Read the MetaData
	 * 
	 * @return metaData         Metadata
     * @param buffer            IoBuffer source
	 */
	public MetaData<?, ?> readMetaData(IoBuffer buffer);

	/**
	 * Read the Meta Cue Points
	 * 
	 * @return  Meta cue points
	 */
	public IMetaCue[] readMetaCue();

	/**
	 * Media file to be accessed
	 * 
	 * @param file
	 */
	public void setFile(File file);

	/**
	 * Returns the file being accessed
	 * 
	 * @return
	 */
	public File getFile();

}
