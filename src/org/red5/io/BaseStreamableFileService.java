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

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for streamable file services.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public abstract class BaseStreamableFileService implements
		IStreamableFileService {

    private static final Logger log = LoggerFactory.getLogger(BaseStreamableFileService.class);
	
	/** {@inheritDoc} */
    public void setPrefix(String prefix) {
    }

	/** {@inheritDoc} */
    public abstract String getPrefix();

	/** {@inheritDoc} */
    public void setExtension(String extension) {
    }
    
    /** {@inheritDoc} */
    public abstract String getExtension();

	/** {@inheritDoc} */
    public String prepareFilename(String name) {
    	String prefix = getPrefix() + ':';
		if (name.startsWith(prefix)) {
			name = name.substring(prefix.length());
			// if there is no extension on the file add the first one
			log.debug("prepareFilename - lastIndexOf: {} length: {}", name.lastIndexOf('.'), name.length());
			if (name.lastIndexOf('.') != name.length() - 4) {
				name = name + getExtension().split(",")[0];
			}
		}

		return name;
	}

	/** {@inheritDoc} */
    public boolean canHandle(File file) {
    	boolean valid = false;
    	if (file.exists()) {
        	String absPath = file.getAbsolutePath().toLowerCase();
        	String fileExt = absPath.substring(absPath.lastIndexOf('.'));
        	log.debug("canHandle - Path: {} Ext: {}", absPath, fileExt);
        	String[] exts = getExtension().split(",");
        	for (String ext : exts) {
        		if (ext.equals(fileExt)) {
        			valid = true;
        			break;
        		}
        	}
    	}
		return valid;
	}

	/** {@inheritDoc} */
    public abstract IStreamableFile getStreamableFile(File file)
			throws IOException;

}
