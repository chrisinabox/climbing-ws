package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class Media {
	private final int id;
	private final int pitch;
	private final int width;
	private final int height;
	private final int idType;
	private final String t;
	private final List<MediaSvgElement> mediaSvgs;
	private final int svgProblemId;
	private final List<Svg> svgs;
	private final MediaMetadata mediaMetadata;
	private final String embedUrl;
	private final boolean inherited;
	private final int enableMoveToIdSector;
	private final int enableMoveToIdProblem;
	
	public Media(int id, int pitch, int width, int height, int idType, String t, List<MediaSvgElement> mediaSvgs, int svgProblemId, List<Svg> svgs, MediaMetadata mediaMetadata, String embedUrl) {
		this.id = id;
		this.pitch = pitch;
		this.width = width;
		this.height = height;
		this.idType = idType;
		this.t = t;
		this.mediaSvgs = mediaSvgs;
		this.svgProblemId = svgProblemId;
		this.svgs = svgs;
		this.mediaMetadata = mediaMetadata;
		this.embedUrl = embedUrl;
		this.inherited = false;
		this.enableMoveToIdSector = 0;
		this.enableMoveToIdProblem = 0;
	}

	public Media(int id, int pitch, int width, int height, int idType, String t, List<MediaSvgElement> mediaSvgs, int svgProblemId, List<Svg> svgs, MediaMetadata mediaMetadata, String embedUrl, boolean inherited, int enableMoveToIdSector, int enableMoveToIdProblem) {
		this.id = id;
		this.pitch = pitch;
		this.width = width;
		this.height = height;
		this.idType = idType;
		this.t = t;
		this.mediaSvgs = mediaSvgs;
		this.svgProblemId = svgProblemId;
		this.svgs = svgs;
		this.mediaMetadata = mediaMetadata;
		this.embedUrl = embedUrl;
		this.inherited = inherited;
		this.enableMoveToIdSector = enableMoveToIdSector;
		this.enableMoveToIdProblem = enableMoveToIdProblem;
	}
	
	public String getEmbedUrl() {
		return embedUrl;
	}
	
	public int getHeight() {
		return height;
	}

	public int getId() {
		return id;
	}
	
	public int getIdType() {
		return idType;
	}

	public MediaMetadata getMediaMetadata() {
		return mediaMetadata;
	}
	
	public List<MediaSvgElement> getMediaSvgs() {
		return mediaSvgs;
	}

	public int getPitch() {
		return pitch;
	}

	public int getSvgProblemId() {
		return svgProblemId;
	}
	
	public List<Svg> getSvgs() {
		return svgs;
	}

	public String getT() {
		return t;
	}
	
	public int getWidth() {
		return width;
	}
	
	public boolean isInherited() {
		return inherited;
	}
	
	public int getEnableMoveToIdProblem() {
		return enableMoveToIdProblem;
	}
	
	public int getEnableMoveToIdSector() {
		return enableMoveToIdSector;
	}
}