package com.sirma.cmf.web.navigation;

import javax.faces.application.NavigationHandler;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The listener interface for receiving emfPhase events.
 * The class that is interested in processing a emfPhase
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addEmfPhaseListener<code> method. When
 * the emfPhase event occurs, that object's appropriate
 * method is invoked.
 * 
 * @see EmfPhaseEvent
 */
public class EmfPhaseListener implements PhaseListener {

	private static final long serialVersionUID = -3455601862177474049L;

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Override
	public void afterPhase(PhaseEvent event) {
		FacesContext facesContext = event.getFacesContext();
		NavigationHandler navigationHandler = facesContext.getApplication().getNavigationHandler();
		HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext()
				.getRequest();
		String queryString = request.getQueryString();
		if (queryString != null) {
			boolean isHistory = queryString.contains(NavigationHandlerConstants.HISTORY_PAGE);
			if (isHistory) {
				log.debug("History page was requested!");
				navigationHandler.handleNavigation(facesContext, null,
						NavigationHandlerConstants.BACKWARD);
			}
			boolean isCurrent = queryString.contains(NavigationHandlerConstants.CURRENT_PAGE);
			if (isCurrent) {
				log.debug("Current page was requested/page-refresh");
				navigationHandler.handleNavigation(facesContext, null,
						NavigationHandlerConstants.REFRESH);
			}
		}
	}

	@Override
	public void beforePhase(PhaseEvent event) {
		//
	}

	@Override
	public PhaseId getPhaseId() {
		return PhaseId.RESTORE_VIEW;
	}

}