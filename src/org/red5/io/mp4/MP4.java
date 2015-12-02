package org.red5.io.mp4;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 *
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.ITagReader;
import org.red5.io.ITagWriter;
import org.red5.io.flv.IMetaData;
import org.red5.io.flv.IMetaService;
import org.red5.io.flv.MetaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A MP4Impl implements the MP4 api
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire, (mondain@gmail.com)
 */
public class MP4 implements IMP4 {

	protected static Logger log = LoggerFactory.getLogger(MP4.class);

	private File file;

	private IMetaService metaService;

	private IMetaData<?, ?> metaData;

	/**
	 * Default constructor, used by Spring so that parameters may be injected.
	 */
	public MP4() {
	}

	/**
	 * Create MP4 from given file source.
	 * 
	 * @param file File source
	 */
	public MP4(File file) {
		this.file = file;
		/*
		try {
			MP4Reader reader = new MP4Reader(this.file);
			ITag tag = reader.createFileMeta();
			if (metaService == null) {
				metaService = new MetaService(this.file);
			}
			metaData = metaService.readMetaData(tag.getBody());
			reader.close();
		} catch (Exception e) {
			log.error("An error occurred looking for metadata:", e);
		}
		*/		
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean hasMetaData() {
		return metaData != null;
	}

	/**
	 * {@inheritDoc}
	 */
	public IMetaData<?, ?> getMetaData() throws FileNotFoundException {
		metaService.setFile(file);
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean hasKeyFrameData() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setKeyFrameData(Map<?, ?> keyframedata) {
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<?, ?> getKeyFrameData() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void refreshHeaders() throws IOException {
	}

	/**
	 * {@inheritDoc}
	 */
	public void flushHeaders() throws IOException {
	}

	/**
	 * {@inheritDoc}
	 */
	public ITagReader getReader() throws IOException {
		MP4Reader reader = null;
		IoBuffer fileData = null;
		String fileName = file.getName();
		if (file.exists()) {
			log.debug("File name: {} size: {}", fileName, file.length());
			reader = new MP4Reader(new FileInputStream(file));
			// get a ref to the mapped byte buffer
			fileData = reader.getFileData();
			log.trace("File data size: {}", fileData);
		} else {
			log.info("Creating new file: {}", file);
			file.createNewFile();
		}
		return reader;
	}

	/**
	 * {@inheritDoc}
	 */
	public ITagReader readerFromNearestKeyFrame(int seekPoint) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public ITagWriter getWriter() throws IOException {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public ITagWriter writerFromNearestKeyFrame(int seekPoint) {
		return null;
	}

	/** {@inheritDoc} */
	public void setMetaData(IMetaData<?, ?> meta) throws IOException {
		if (metaService == null) {
			metaService = new MetaService(file);
		}
		//if the file is not checked the write may produce an NPE
		if (metaService.getFile() == null) {
			metaService.setFile(file);
		}
		metaService.write(meta);
		metaData = meta;
	}

	/** {@inheritDoc} */
	public void setMetaService(IMetaService service) {
		metaService = service;
	}

	public ITagWriter getAppendWriter() throws IOException {
		return null;
	}
	
}
