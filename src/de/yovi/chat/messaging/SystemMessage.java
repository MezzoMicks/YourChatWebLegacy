package de.yovi.chat.messaging;

import de.yovi.chat.api.Message;
import de.yovi.chat.api.Segment;
import de.yovi.chat.api.User;

public class SystemMessage extends AbstractMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5892914947569606185L;
	private final User origin;
	private final long id;
	
	public SystemMessage(User origin, long id, Segment... segments) {
		super(segments);
		this.origin = origin;
		this.id = id;
		if (origin == null) {
			setCode(-1);
		}
	}
	
	public SystemMessage(User origin, long id, Preset template, Object... args) {
		this(origin, id, template.getContent() != null ? new StringSegment(String.format(template.getContent(), args)) : null);
		setCode(template.getCode());
	}
	

	public SystemMessage(long id, Message original) {
		this(original.getOrigin(), id, original.getSegments());
		setCode(original.getCode());
	}


	@Override
	public long getID() {
		return id;
	}

	@Override
	public User getOrigin() {
		return origin;
	}

	
}
