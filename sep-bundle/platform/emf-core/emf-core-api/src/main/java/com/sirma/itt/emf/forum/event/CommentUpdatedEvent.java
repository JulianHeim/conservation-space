package com.sirma.itt.emf.forum.event;

import com.sirma.itt.emf.event.EmfEvent;
import com.sirma.itt.emf.forum.model.CommentInstance;
import com.sirma.itt.emf.util.Documentation;

/**
 * Event fired to notify that topic/comment is added/edited.
 * 
 * @author Stella
 */
@Documentation("Event fired to notify that topic/comment is added/edited.")
public class CommentUpdatedEvent implements EmfEvent {

	/** The old comment instance. */
	private final CommentInstance oldComment;

	/** The new comment instance. */
	private final CommentInstance commentInstance;

	/**
	 * Instantiates a comment updated event.
	 * 
	 * @param oldComment
	 *            the old comment instance
	 * @param commentInstance
	 *            the new comment instance
	 */
	public CommentUpdatedEvent(CommentInstance oldComment, CommentInstance commentInstance) {
		this.oldComment = oldComment;
		this.commentInstance = commentInstance;
	}

	/**
	 * Getter method for old comment instance.
	 * 
	 * @return the oldComment
	 */
	public CommentInstance getOldCommentInstance() {
		return oldComment;
	}

	/**
	 * Getter method for old comment instance.
	 * 
	 * @return the commentInstance
	 */
	public CommentInstance getCommentInstance() {
		return commentInstance;
	}
}
