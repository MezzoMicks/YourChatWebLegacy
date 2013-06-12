package de.yovi.chat.processing;

import java.util.LinkedList;
import java.util.List;

import de.yovi.chat.processing.api.MessageElement;
import de.yovi.chat.processing.api.ProcessorResult;

/**
 * An original MessageElement, which is, by it's names definition, unprocessed
 * @author Michi
 *
 */
public class UnprocessedMessageElement implements MessageElement {

	private final String content;
	
	public UnprocessedMessageElement(String content) {
		this.content = content;
	}
	
	@Override
	public boolean isHidden() {
		return false;
	}
	
	@Override
	public String getTextContent() {
		return content;
	}
	
	@Override
	public ProcessorResult getProcessorResult() {
		return null;
	}
	
	@Override
	public List<MessageElement> shredElement(int start, int end) {
		List<MessageElement> result = new LinkedList<MessageElement>();
		if (start <= 0) {
			if (end >= content.length()) {
				result.add(this);
			} else {
				result.add(new UnprocessedMessageElement(content.substring(0, end)));
				result.add(new UnprocessedMessageElement(content.substring(end)));
			}
		} else if (end >= content.length()) {
			result.add(new UnprocessedMessageElement(content.substring(0, start)));
			result.add(new UnprocessedMessageElement(content.substring(start)));
		} else {
			result.add(new UnprocessedMessageElement(content.substring(0, start)));
			result.add(new UnprocessedMessageElement(content.substring(start, end)));
			result.add(new UnprocessedMessageElement(content.substring(end)));
		}
		return result;
	}
	
	public boolean isProcessed() {
		return false;
	};
	
}
