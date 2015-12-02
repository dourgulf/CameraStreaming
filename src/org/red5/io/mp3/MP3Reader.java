package org.red5.io.mp3;

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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
//import org.jaudiotagger.audio.AudioFileIO;
//import org.jaudiotagger.audio.mp3.MP3AudioHeader;
//import org.jaudiotagger.audio.mp3.MP3File;
//import org.jaudiotagger.tag.TagException;
//import org.jaudiotagger.tag.TagField;
//import org.jaudiotagger.tag.FieldKey;
//import org.jaudiotagger.tag.datatype.DataTypes;
//import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
//import org.jaudiotagger.tag.id3.ID3v24Tag;
//import org.jaudiotagger.tag.id3.framebody.FrameBodyAPIC;
import org.red5.io.IKeyFrameMetaCache;
import org.red5.io.IStreamableFile;
import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.IoConstants;
import org.red5.io.amf.Output;
import org.red5.io.flv.IKeyFrameDataAnalyzer;
import org.red5.io.flv.Tag;
import org.red5.io.object.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read MP3 files
 */
public class MP3Reader implements ITagReader, IKeyFrameDataAnalyzer {
	/**
	 * Logger
	 */
	protected static Logger log = LoggerFactory.getLogger(MP3Reader.class);

	/**
	 * File
	 */
	private File file;

	/**
	 * File input stream
	 */
	private FileInputStream fis;

	/**
	 * File channel
	 */
	private FileChannel channel;

	/**
	 * Memory-mapped buffer for file content
	 */
	private MappedByteBuffer mappedFile;

	/**
	 * Source byte buffer
	 */
	private IoBuffer in;

	/**
	 * Last read tag object
	 */
	private ITag tag;

	/**
	 * Previous tag size
	 */
	private int prevSize;

	/**
	 * Current time
	 */
	private double currentTime;

	/**
	 * Frame metadata
	 */
	private KeyFrameMeta frameMeta;

	/**
	 * Positions and time map
	 */
	private HashMap<Integer, Double> posTimeMap;

	private int dataRate;

	/**
	 * File duration
	 */
	private long duration;

	/**
	 * Frame cache
	 */
	static private IKeyFrameMetaCache frameCache;

	/**
	 * Holder for ID3 meta data
	 */
	private MetaData metaData;

	/**
	 * Container for metadata and any other tags that should
	 * be sent prior to media data.
	 */
	private LinkedList<ITag> firstTags = new LinkedList<ITag>();
	
	MP3Reader() {
		// Only used by the bean startup code to initialize the frame cache
	}

	/**
	 * Creates reader from file input stream
	 * @param file file input
	 * 
	 * @throws FileNotFoundException if not found 
	 */
	public MP3Reader(File file) throws FileNotFoundException {
		this.file = file;

		// parse the id3 info
		/*
		try {
			MP3File mp3file = (MP3File) AudioFileIO.read(file);
			MP3AudioHeader audioHeader = (MP3AudioHeader) mp3file.getAudioHeader();
			if (audioHeader != null) {				
				log.debug("Track length: {}", audioHeader.getTrackLength());
				log.debug("Sample rate: {}", audioHeader.getSampleRateAsNumber());
				log.debug("Channels: {}", audioHeader.getChannels());
				log.debug("Variable bit rate: {}", audioHeader.isVariableBitRate());
				log.debug("Track length (2): {}", audioHeader.getTrackLengthAsString());
				log.debug("Mpeg version: {}", audioHeader.getMpegVersion());
				log.debug("Mpeg layer: {}", audioHeader.getMpegLayer());
				log.debug("Original: {}", audioHeader.isOriginal());
				log.debug("Copyrighted: {}", audioHeader.isCopyrighted());
				log.debug("Private: {}", audioHeader.isPrivate());
				log.debug("Protected: {}", audioHeader.isProtected());
				log.debug("Bitrate: {}", audioHeader.getBitRate());
				log.debug("Encoding type: {}", audioHeader.getEncodingType());
				log.debug("Encoder: {}", audioHeader.getEncoder());
			}
			ID3v24Tag idTag = mp3file.getID3v2TagAsv24();
			if (idTag != null) {
				// create meta data holder
				metaData = new MetaData();
//				metaData.setAlbum(idTag.getFirstAlbum());
//				metaData.setArtist(idTag.getFirstArtist());
//				metaData.setComment(idTag.getFirstComment());
//				metaData.setGenre(idTag.getFirstGenre());
//				metaData.setSongName(idTag.getFirstTitle());
//				metaData.setTrack(idTag.getFirstTrack());
//				metaData.setYear(idTag.getFirstYear());
				//send album image if included
				List<TagField> tagFieldList = mp3file.getTag().getFields(FieldKey.COVER_ART);
				//fix for APPSERVER-310
				if (tagFieldList == null || tagFieldList.isEmpty()) {
					log.debug("No cover art was found");
				} else {
    				TagField imageField = tagFieldList.get(0);
    				if (imageField instanceof AbstractID3v2Frame) {
    				    FrameBodyAPIC imageFrameBody = (FrameBodyAPIC)((AbstractID3v2Frame)imageField).getBody();
    				    if (!imageFrameBody.isImageUrl()) {
    				        byte[] imageBuffer = (byte[]) imageFrameBody.getObjectValue(DataTypes.OBJ_PICTURE_DATA);
    						//set the cover image on the metadata
    						metaData.setCovr(imageBuffer);
    						// Create tag for onImageData event
    						IoBuffer buf = IoBuffer.allocate(imageBuffer.length);
    						buf.setAutoExpand(true);
    						Output out = new Output(buf);
    						out.writeString("onImageData");
    						Map<Object, Object> props = new HashMap<Object, Object>();
    						props.put("trackid", 1);
    						props.put("data", imageBuffer);
    						out.writeMap(props, new Serializer());
    						buf.flip();
    						//Ugh i hate flash sometimes!!
    						//Error #2095: flash.net.NetStream was unable to invoke callback onImageData.
    						ITag result = new Tag(IoConstants.TYPE_METADATA, 0, buf.limit(), null, 0);
    						result.setBody(buf);								
    						//add to first frames
    						firstTags.add(result);
    				    }
    				}
				}
			} else {
				log.info("File did not contain ID3v2 data: {}", file.getName());
			}
			mp3file = null;
		} catch (TagException e) {
			log.error("MP3Reader (tag error) {}", e);
		} catch (Exception e) {
			log.error("MP3Reader {}", e);
		}
	*/
		fis = new FileInputStream(file);
		// Grab file channel and map it to memory-mapped byte buffer in
		// read-only mode
		channel = fis.getChannel();
		try {
			mappedFile = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel
					.size());
		} catch (IOException e) {
			log.error("MP3Reader {}", e);
		}

		// Use Big Endian bytes order
		mappedFile.order(ByteOrder.BIG_ENDIAN);
		// Wrap mapped byte buffer to MINA buffer
		in = IoBuffer.wrap(mappedFile);
		// Analyze keyframes data
		analyzeKeyFrames();

		// Create file metadata object
		firstTags.addFirst(createFileMeta());

		// MP3 header is length of 32 bits, that is, 4 bytes
		// Read further if there's still data
		if (in.remaining() > 4) {
			// Look to next frame
			searchNextFrame();
			// Set position
			int pos = in.position();
			// Read header...
			// Data in MP3 file goes header-data-header-data...header-data
			MP3Header header = readHeader();
			// Set position
			in.position(pos);
			// Check header
			if (header != null) {
				checkValidHeader(header);
			} else {
				throw new RuntimeException("No initial header found.");
			}
		}
	}

	/**
	 * A MP3 stream never has video.
	 * 
	 * @return always returns <code>false</code>
	 */
	public boolean hasVideo() {
		return false;
	}

	public void setFrameCache(IKeyFrameMetaCache frameCache) {
		MP3Reader.frameCache = frameCache;
	}

	/**
	 * Check if the file can be played back with Flash. Supported sample rates
	 * are 44KHz, 22KHz, 11KHz and 5.5KHz
	 * 
	 * @param header
	 *            Header to check
	 */
	private void checkValidHeader(MP3Header header) {
		switch (header.getSampleRate()) {
			case 48000:
			case 44100:
			case 22050:
			case 11025:
			case 5513:
				// Supported sample rate
				break;

			default:
				throw new RuntimeException("Unsupported sample rate: "
						+ header.getSampleRate());
		}
	}

	/**
	 * Creates file metadata object
	 * 
	 * @return Tag
	 */
	private ITag createFileMeta() {
		// Create tag for onMetaData event
		IoBuffer buf = IoBuffer.allocate(1024);
		buf.setAutoExpand(true);
		Output out = new Output(buf);
		out.writeString("onMetaData");
		Map<Object, Object> props = new HashMap<Object, Object>();
		props.put("duration",
				frameMeta.timestamps[frameMeta.timestamps.length - 1] / 1000.0);
		props.put("audiocodecid", IoConstants.FLAG_FORMAT_MP3);
		if (dataRate > 0) {
			props.put("audiodatarate", dataRate);
		}
		props.put("canSeekToEnd", true);
		//set id3 meta data if it exists
		if (metaData != null) {
			props.put("artist", metaData.getArtist());
			props.put("album", metaData.getAlbum());
			props.put("songName", metaData.getSongName());
			props.put("genre", metaData.getGenre());
			props.put("year", metaData.getYear());
			props.put("track", metaData.getTrack());
			props.put("comment", metaData.getComment());
			if (metaData.hasCoverImage()) {
				Map<Object, Object> covr = new HashMap<Object, Object>(1);
				covr.put("covr", new Object[]{metaData.getCovr()});
				props.put("tags", covr);
			}
			//clear meta for gc
			metaData = null;
		}
		out.writeMap(props, new Serializer());
		buf.flip();

		ITag result = new Tag(IoConstants.TYPE_METADATA, 0, buf.limit(), null,
				prevSize);
		result.setBody(buf);
		return result;
	}

	/** Search for next frame sync word. Sync word identifies valid frame. */
	public void searchNextFrame() {
		while (in.remaining() > 1) {
			int ch = in.get() & 0xff;
			if (ch != 0xff) {
				continue;
			}

			if ((in.get() & 0xe0) == 0xe0) {
				// Found it
				in.position(in.position() - 2);
				return;
			}
		}
	}

	/** {@inheritDoc} */
	public IStreamableFile getFile() {
		return null;
	}

	/** {@inheritDoc} */
	public int getOffset() {
		return 0;
	}

	/** {@inheritDoc} */
	public long getBytesRead() {
		return in.position();
	}

	/** {@inheritDoc} */
	public long getDuration() {
		return duration;
	}

	/**
	 * Get the total readable bytes in a file or ByteBuffer.
	 * 
	 * @return Total readable bytes
	 */
	public long getTotalBytes() {
		return in.capacity();
	}

	/** {@inheritDoc} */
	public boolean hasMoreTags() {
		MP3Header header = null;
		while (header == null && in.remaining() > 4) {
			try {
				header = new MP3Header(in.getInt());
			} catch (IOException e) {
				log.error("MP3Reader :: hasMoreTags ::>\n", e);
				break;
			} catch (Exception e) {
				searchNextFrame();
			}
		}

		if (header == null) {
			return false;
		}

		if (header.frameSize() == 0) {
			// TODO find better solution how to deal with broken files...
			// See APPSERVER-62 for details
			return false;
		}

		if (in.position() + header.frameSize() - 4 > in.limit()) {
			// Last frame is incomplete
			in.position(in.limit());
			return false;
		}

		in.position(in.position() - 4);
		return true;
	}

	private MP3Header readHeader() {
		MP3Header header = null;
		while (header == null && in.remaining() > 4) {
			try {
				header = new MP3Header(in.getInt());
			} catch (IOException e) {
				log.error("MP3Reader :: readTag ::>\n", e);
				break;
			} catch (Exception e) {
				searchNextFrame();
			}
		}
		return header;
	}

	/** {@inheritDoc} */
	public synchronized ITag readTag() {
		if (!firstTags.isEmpty()) {
			// Return first tags before media data
			return firstTags.removeFirst();
		}

		MP3Header header = readHeader();
		if (header == null) {
			return null;
		}

		int frameSize = header.frameSize();
		if (frameSize == 0) {
			// TODO find better solution how to deal with broken files...
			// See APPSERVER-62 for details
			return null;
		}

		if (in.position() + frameSize - 4 > in.limit()) {
			// Last frame is incomplete
			in.position(in.limit());
			return null;
		}

		tag = new Tag(IoConstants.TYPE_AUDIO, (int) currentTime, frameSize + 1,
				null, prevSize);
		prevSize = frameSize + 1;
		currentTime += header.frameDuration();
		IoBuffer body = IoBuffer.allocate(tag.getBodySize());
		body.setAutoExpand(true);
		byte tagType = (IoConstants.FLAG_FORMAT_MP3 << 4)
				| (IoConstants.FLAG_SIZE_16_BIT << 1);
		switch (header.getSampleRate()) {
			case 44100:
				tagType |= IoConstants.FLAG_RATE_44_KHZ << 2;
				break;
			case 22050:
				tagType |= IoConstants.FLAG_RATE_22_KHZ << 2;
				break;
			case 11025:
				tagType |= IoConstants.FLAG_RATE_11_KHZ << 2;
				break;
			default:
				tagType |= IoConstants.FLAG_RATE_5_5_KHZ << 2;
		}
		tagType |= (header.isStereo() ? IoConstants.FLAG_TYPE_STEREO
				: IoConstants.FLAG_TYPE_MONO);
		body.put(tagType);
		final int limit = in.limit();
		body.putInt(header.getData());
		in.limit(in.position() + frameSize - 4);
		body.put(in);
		body.flip();
		in.limit(limit);

		tag.setBody(body);

		return tag;
	}

	/** {@inheritDoc} */
	public void close() {
		if (posTimeMap != null) {
			posTimeMap.clear();
		}
		mappedFile.clear();
		if (in != null) {
			in.free();
			in = null;
		}
		try {
			fis.close();
			channel.close();
		} catch (IOException e) {
			log.error("MP3Reader :: close ::>\n", e);
		}
	}

	/** {@inheritDoc} */
	public void decodeHeader() {
	}

	/** {@inheritDoc} */
	public void position(long pos) {
		if (pos == Long.MAX_VALUE) {
			// Seek at EOF
			in.position(in.limit());
			currentTime = duration;
			return;
		}
		in.position((int) pos);
		// Advance to next frame
		searchNextFrame();
		// Make sure we can resolve file positions to timestamps
		analyzeKeyFrames();
		Double time = posTimeMap.get(in.position());
		if (time != null) {
			currentTime = time;
		} else {
			// Unknown frame position - this should never happen
			currentTime = 0;
		}
	}

	/** {@inheritDoc} */
	public synchronized KeyFrameMeta analyzeKeyFrames() {
		if (frameMeta != null) {
			return frameMeta;
		}

		// check for cached frame informations
		if (frameCache != null) {
			frameMeta = frameCache.loadKeyFrameMeta(file);
			if (frameMeta != null && frameMeta.duration > 0) {
				// Frame data loaded, create other mappings
				duration = frameMeta.duration;
				frameMeta.audioOnly = true;
				posTimeMap = new HashMap<Integer, Double>();
				for (int i = 0; i < frameMeta.positions.length; i++) {
					posTimeMap.put((int) frameMeta.positions[i],
							(double) frameMeta.timestamps[i]);
				}
				return frameMeta;
			}
		}

		List<Integer> positionList = new ArrayList<Integer>();
		List<Double> timestampList = new ArrayList<Double>();
		dataRate = 0;
		long rate = 0;
		int count = 0;
		int origPos = in.position();
		double time = 0;
		in.position(0);
		// processID3v2Header();
		searchNextFrame();
		while (this.hasMoreTags()) {
			MP3Header header = readHeader();
			if (header == null) {
				// No more tags
				break;
			}

			if (header.frameSize() == 0) {
				// TODO find better solution how to deal with broken files...
				// See APPSERVER-62 for details
				break;
			}

			int pos = in.position() - 4;
			if (pos + header.frameSize() > in.limit()) {
				// Last frame is incomplete
				break;
			}

			positionList.add(pos);
			timestampList.add(time);
			rate += header.getBitRate() / 1000;
			time += header.frameDuration();
			in.position(pos + header.frameSize());
			count++;
		}
		// restore the pos
		in.position(origPos);

		duration = (long) time;
		dataRate = (int) (rate / count);
		posTimeMap = new HashMap<Integer, Double>();
		frameMeta = new KeyFrameMeta();
		frameMeta.duration = duration;
		frameMeta.positions = new long[positionList.size()];
		frameMeta.timestamps = new int[timestampList.size()];
		frameMeta.audioOnly = true;
		for (int i = 0; i < frameMeta.positions.length; i++) {
			frameMeta.positions[i] = positionList.get(i);
			frameMeta.timestamps[i] = timestampList.get(i).intValue();
			posTimeMap.put(positionList.get(i), timestampList.get(i));
		}
		if (frameCache != null) {
			frameCache.saveKeyFrameMeta(file, frameMeta);
		}
		return frameMeta;
	}

	/**
	 * Simple holder for id3 meta data
	 */
	static class MetaData {
		String album = "";

		String artist = "";

		String genre = "";

		String songName = "";

		String track = "";

		String year = "";

		String comment = "";
		
		byte[] covr = null;

		public String getAlbum() {
			return album;
		}

		public void setAlbum(String album) {
			this.album = album;
		}

		public String getArtist() {
			return artist;
		}

		public void setArtist(String artist) {
			this.artist = artist;
		}

		public String getGenre() {
			return genre;
		}

		public void setGenre(String genre) {
			this.genre = genre;
		}

		public String getSongName() {
			return songName;
		}

		public void setSongName(String songName) {
			this.songName = songName;
		}

		public String getTrack() {
			return track;
		}

		public void setTrack(String track) {
			this.track = track;
		}

		public String getYear() {
			return year;
		}

		public void setYear(String year) {
			this.year = year;
		}

		public String getComment() {
			return comment;
		}

		public void setComment(String comment) {
			this.comment = comment;
		}

		public byte[] getCovr() {
			return covr;
		}

		public void setCovr(byte[] covr) {
			this.covr = covr;
			log.debug("Cover image array size: {}", covr.length);
		}

		public boolean hasCoverImage() {
			return covr != null;
		}
		
	}

}
