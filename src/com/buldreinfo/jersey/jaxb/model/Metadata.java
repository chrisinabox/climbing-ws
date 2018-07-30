package com.buldreinfo.jersey.jaxb.model;

import com.buldreinfo.jersey.jaxb.metadata.jsonld.JsonLd;

public class Metadata {
	private final String title;
	private String description;
	private JsonLd jsonLd;
	private int defaultZoom;
	private LatLng defaultCenter;
	private boolean isBouldering;
	
	public Metadata(String title) {
		this.title = title;
	}
	
	public LatLng getDefaultCenter() {
		return defaultCenter;
	}
	
	public int getDefaultZoom() {
		return defaultZoom;
	}
	
	public String getDescription() {
		return description;
	}

	public JsonLd getJsonLd() {
		return jsonLd;
	}

	public String getTitle() {
		return title;
	}

	public boolean isBouldering() {
		return isBouldering;
	}

	public Metadata setDefaultCenter(LatLng defaultCenter) {
		this.defaultCenter = defaultCenter;
		return this;
	}
	
	public Metadata setDefaultZoom(int defaultZoom) {
		this.defaultZoom = defaultZoom;
		return this;
	}

	public Metadata setDescription(String description) {
		this.description = description;
		return this;
	}
	
	public Metadata setIsBouldering(boolean isBouldering) {
		this.isBouldering = isBouldering;
		return this;
	}

	public Metadata setJsonLd(JsonLd jsonLd) {
		this.jsonLd = jsonLd;
		return this;
	}
}