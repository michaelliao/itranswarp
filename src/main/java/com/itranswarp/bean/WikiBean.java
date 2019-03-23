package com.itranswarp.bean;

public class WikiBean {

	public String tag;
	public String name;
	public String description;

	/**
	 * Markdown content.
	 */
	public String content;

	/**
	 * Base64 encoded image data.
	 */
	public String image;

	/**
	 * Publish at.
	 */
	public Long publishAt;

}
