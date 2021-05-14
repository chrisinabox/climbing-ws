package com.buldreinfo.jersey.jaxb.model;

public class MediaSvgElement {
	public enum TYPE { PATH, RAPPEL };
	private final TYPE t;
	private final int id;
	private final String path;
	private final int rappelX;
	private final int rappelY;
	
	public MediaSvgElement(int id, String path) {
		this.t = TYPE.PATH;
		this.id = id;
		this.path = path;
		this.rappelX = 0;
		this.rappelY = 0;
	}
	
	public MediaSvgElement(int id, int rappelX, int rappelY) {
		this.t = TYPE.RAPPEL;
		this.id = id;
		this.path = null;
		this.rappelX = rappelX;
		this.rappelY = rappelY;
	}
	
	public TYPE getT() {
		return t;
	}

	public int getId() {
		return id;
	}

	public String getPath() {
		return path;
	}
	
	public int getRappelX() {
		return rappelX;
	}
	
	public int getRappelY() {
		return rappelY;
	}
}