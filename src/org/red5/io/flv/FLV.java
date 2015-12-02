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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

//import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.ITagWriter;
import org.red5.io.IoConstants;
//import org.red5.server.cache.ICacheStore;
//import org.red5.server.cache.ICacheable;
//import org.red5.server.cache.NoCacheImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A FLVImpl implements the FLV api
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class FLV implements IFLV {

	protected static Logger log = LoggerFactory.getLogger(FLV.class);

//	private static ICacheStore cache;

	private File file;

	private boolean generateMetadata;

	private IMetaService metaService;

	private IMetaData<?, ?> metaData;

	/**
	 * Default constructor, used by Spring so that parameters may be injected.
	 */
	public FLV() {
	}

	/**
	 * Create FLV from given file source
	 * 
	 * @param file File source
	 */
	public FLV(File file) {
		this(file, false);
	}

	/**
	 * Create FLV from given file source and with specified metadata generation
	 * option
	 * 
	 * @param file File source
	 * @param generateMetadata Metadata generation option
	 */
	public FLV(File file, boolean generateMetadata) {
		this.file = file;
		this.generateMetadata = generateMetadata;
		int count = 0;
		if (!generateMetadata) {
			try {
				FLVReader reader = new FLVReader(this.file);
				ITag tag = null;
				while (reader.hasMoreTags() && (++count < 5)) {
					tag = reader.readTag();
					if (tag.getDataType() == IoConstants.TYPE_METADATA) {
						if (metaService == null) {
							metaService = new MetaService(this.file);
						}
						metaData = metaService.readMetaData(tag.getBody());
					}
				}
				reader.close();
			} catch (Exception e) {
				log.error("An error occured looking for metadata {}", e);
			}
		}
	}

	/**
	 * Sets the cache implementation to be used.
	 * 
	 * @param cache Cache store
	 */
//	public void setCache(ICacheStore cache) {
//		FLV.cache = cache;
//	}

	/**
	 * {@inheritDoc}
	 */
	public boolean hasMetaData() {
		return metaData != null;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "rawtypes" })
	public IMetaData getMetaData() throws FileNotFoundException {
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
	@SuppressWarnings({ "rawtypes" })
	public void setKeyFrameData(Map keyframedata) {
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "rawtypes" })
	public Map getKeyFrameData() {
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
		FLVReader reader = null;
//		IoBuffer fileData;
		String fileName = file.getName();
		// if no cache is set an NPE will be thrown
//		if (cache == null) {
			log.info("FLV cache is null, forcing NoCacheImpl instance");
//			cache = NoCacheImpl.getInstance();
//		}
//		ICacheable ic = cache.get(fileName);
		// look in the cache before reading the file from the disk
//		if (null == ic || (null == ic.getByteBuffer())) {
			if (file.exists()) {
				log.debug("File size: {}", file.length());
				reader = new FLVReader(file, generateMetadata);
				// get a ref to the mapped byte buffer
//				fileData = reader.getFileData();
				// offer the uncached file to the cache
//				if (fileData != null && cache.offer(fileName, fileData)) {
//					log.debug("Item accepted by the cache: {}", fileName);
//				} else {
					
					log.debug("Item will not be cached: {}", fileName);
//				}
			} else {
				log.info("Creating new file: {}", file);
				file.createNewFile();
			}
//		} else {
//			fileData = IoBuffer.wrap(ic.getBytes());
//			reader = new FLVReader(fileData, generateMetadata);
//		}
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
		if (file.exists()) {
			file.delete();
		}
		file.createNewFile();
		ITagWriter writer = new FLVWriter(file, false);
		return writer;
	}

	/** {@inheritDoc} */
	public ITagWriter getAppendWriter() throws IOException {
		// If the file doesn't exist, we can't append to it, so return a writer
		if (!file.exists()) {
			log.info("File does not exist, calling writer. This will create a new file.");
			return getWriter();
		}
		//Fix by Mhodgson: FLVWriter constructor allows for passing of file object
		ITagWriter writer = new FLVWriter(file, true);
		return writer;
	}

	/**
	 * {@inheritDoc}
	 */
	public ITagWriter writerFromNearestKeyFrame(int seekPoint) {
		return null;
	}

	/** {@inheritDoc} */
	@SuppressWarnings({ "rawtypes" })
	public void setMetaData(IMetaData meta) throws IOException {
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
}
