package com.sirma.cmf.web.caseinstance.dashboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.userdashboard.DashboardPanelActionBase;
import com.sirma.itt.cmf.beans.model.CaseInstance;
import com.sirma.itt.emf.forum.ForumService;
import com.sirma.itt.emf.forum.model.CommentInstance;
import com.sirma.itt.emf.forum.model.TopicInstance;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.util.DateRangeUtil;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;

/**
 * This class manage comments dashlet in case dashboard. To apply comments we update or create topic
 * {@link TopicInstance} and store it in the context.
 * 
 * @author svelikov
 */
@Named
@InstanceType(type = "CaseDashboard")
@ViewAccessScoped
public class CaseMessagesPanel extends
		DashboardPanelActionBase<CaseInstance, SearchArguments<CaseInstance>> implements
		Serializable, DashboardPanelController {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 6119169275720417596L;

	/** The forum service {@link ForumService}. */
	@Inject
	private ForumService forumService;

	/** Check for available topic instance. */
	private boolean isTopicAvailable = false;

	/** List with comments that will be generated. */
	private List<CommentInstance> caseComments;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initData() {
		onOpen();
	}

	/**
	 * Method that will load the default filter for generating comments.
	 * 
	 * @param topic
	 *            current topic instance
	 */
	private void loadDefaultComments(TopicInstance topic) {
		forumService.loadComments(topic, DateRangeUtil.getLast7Days());
		caseComments = new ArrayList<CommentInstance>();
		caseComments = topic.getComments();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void executeDefaultFilter() {
		if (!isAjaxRequest() || !isTopicAvailable) {
			isTopicAvailable = true;
			CaseInstance caseInstance = getDocumentContext().getInstance(CaseInstance.class);
			TopicInstance topicInstance = forumService.getOrCreateTopicAbout(caseInstance);
			getDocumentContext().setTopicInstance(topicInstance);
			loadDefaultComments(topicInstance);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void loadFilters() {
		// TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean isAsynchronousLoadingSupported() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void filter(SearchArguments<CaseInstance> searchArguments) {
		// TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateSelectedFilterField(String selectedItemId) {
		// TODO Auto-generated method stub
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SearchArguments<CaseInstance> getSearchArguments() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> dashletActionIds() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String targetDashletName() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Instance dashletActionsTarget() {
		return getDocumentContext().getCurrentInstance();
	}

	/**
	 * Getter for retrieving current case comments.
	 * 
	 * @return list with comment instances
	 */
	public List<CommentInstance> getCaseComments() {
		TopicInstance topic = getDocumentContext().getTopicInstance();
		if (topic != null) {
			loadDefaultComments(topic);
		}
		return caseComments;
	}

	/**
	 * Setter for case comments.
	 * 
	 * @param caseComments
	 *            current list with comment instance
	 */
	public void setCaseComments(List<CommentInstance> caseComments) {
		this.caseComments = caseComments;
	}

}
