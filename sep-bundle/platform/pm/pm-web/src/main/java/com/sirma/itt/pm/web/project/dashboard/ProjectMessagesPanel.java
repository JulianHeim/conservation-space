package com.sirma.itt.pm.web.project.dashboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.userdashboard.DashboardPanelActionBase;
import com.sirma.itt.emf.forum.ForumService;
import com.sirma.itt.emf.forum.model.CommentInstance;
import com.sirma.itt.emf.forum.model.TopicInstance;
import com.sirma.itt.emf.instance.dao.InstanceType;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.search.SearchArguments;
import com.sirma.itt.emf.util.DateRangeUtil;
import com.sirma.itt.emf.web.dashboard.panel.DashboardPanelController;
import com.sirma.itt.pm.domain.model.ProjectInstance;

/**
 * This class manage comments dashlet in project dashboard. To apply comments we update or create
 * topic {@link TopicInstance} and store it in the context.
 * 
 * @author svelikov
 */
@Named
@InstanceType(type = "ProjectDashboard")
@ViewAccessScoped
public class ProjectMessagesPanel extends
		DashboardPanelActionBase<ProjectInstance, SearchArguments<ProjectInstance>> implements
		Serializable, DashboardPanelController {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -1693155060260907440L;

	/** Check for available topic instance. */
	private boolean isTopicAvailable = false;

	/** The forum service {@link ForumService}. */
	@Inject
	private ForumService forumService;

	/** The list with project comments instances. */
	private List<CommentInstance> projectComments;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initData() {
		executeDefaultFilter();
	}

	/**
	 * Method that will load the default filter for generating comments.
	 * 
	 * @param topic
	 *            current topic instance
	 */
	private void loadDefaultComments(TopicInstance topic) {
		forumService.loadComments(topic, DateRangeUtil.getLast7Days());
		projectComments = new ArrayList<CommentInstance>();
		projectComments = topic.getComments();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void executeDefaultFilter() {
		if (!isAjaxRequest() || !isTopicAvailable) {
			isTopicAvailable = true;
			ProjectInstance instance = getDocumentContext().getInstance(ProjectInstance.class);
			TopicInstance topicInstance = forumService.getOrCreateTopicAbout(instance);
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
	public void filter(SearchArguments<ProjectInstance> searchArguments) {
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
	public SearchArguments<ProjectInstance> getSearchArguments() {
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
	 * Getter for retrieving project comments.
	 * 
	 * @return list with project comments
	 */
	public List<CommentInstance> getProjectComments() {
		TopicInstance topic = getDocumentContext().getTopicInstance();
		if (topic != null) {
			loadDefaultComments(topic);
		}
		return projectComments;
	}

	/**
	 * Setter for porject comments.
	 * 
	 * @param projectComments
	 *            list with comments
	 */
	public void setProjectComments(List<CommentInstance> projectComments) {
		this.projectComments = projectComments;
	}

}
