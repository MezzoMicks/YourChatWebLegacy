package de.yovi.chat.messaging;

import de.yovi.chat.api.Message;
import de.yovi.chat.api.Segment;

public abstract class AbstractMessage implements Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -290010414219802813L;
	private Segment[] segments;
	private int code = 0;

	protected AbstractMessage(Segment[] segments) {
		if (segments != null && segments.length > 0 && segments[0] != null) {
			this.segments = segments;
		}
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(getOrigin() != null ? getOrigin().getUserName() : "SYSTEM");
		result.append("::");
		for (Segment seg : getSegments()) {
			result.append(seg.getContent());
		}
		return result.toString();
	}

	@Override
	public int getCode() {
		return code;
	}
	
	@Override
	public Segment[] getSegments() {
		return segments;
	}
	
	public void setCode(int code) {
		this.code = code;
	}
	
	public void append(Segment... segments) {
		if (segments != null) {
			Segment[] newSegments = new Segment[this.segments.length + segments.length];
			int i;
			for (i = 0; i < newSegments.length; i++) {
				if (i >= this.segments.length) {
					int offset = this.segments.length;
					newSegments[i] = segments[i - offset];
				} else {
					newSegments[i] = this.segments[i];
				}
			}
			this.segments = newSegments;
		}
	}
	
}
