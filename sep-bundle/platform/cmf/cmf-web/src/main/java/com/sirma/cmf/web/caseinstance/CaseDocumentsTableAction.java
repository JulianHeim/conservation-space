package com.sirma.cmf.web.caseinstance;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ViewAccessScoped;

import com.sirma.cmf.web.TableAction;
import com.sirma.cmf.web.constants.NavigationConstants;
import com.sirma.cmf.web.document.content.DocumentContentAreaProvider;
import com.sirma.cmf.web.document.editor.DocumentEditor;
import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.cmf.constants.DocumentProperties;
import com.sirma.itt.cmf.event.document.DocumentOpenEvent;
import com.sirma.itt.emf.plugin.ExtensionPoint;

/**
 * Backing bean for documents list table.
 * 
 * @author svelikov
 */
@Named
@ViewAccessScoped
public class CaseDocumentsTableAction extends TableAction implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 6042499806553671486L;

	/** The document open event. */
	@Inject
	private Event<DocumentOpenEvent> documentOpenEvent;

	/** The document editor that is selected for editing of current document instance. */
	private DocumentEditor documentEditor;

	/** All registered document editor plugins. */
	@Inject
	@ExtensionPoint(DocumentEditor.EXTENSION_POINT)
	private Iterable<DocumentEditor> documentEditors;

	/** All registered document content area providers. */
	@Inject
	@ExtensionPoint(DocumentContentAreaProvider.EXTENSION_POINT)
	private Iterable<DocumentContentAreaProvider> documentContentAreaProviders;

	/** The document content area provider. */
	private DocumentContentAreaProvider documentContentAreaProvider;

	/**
	 * Initializes the action bean.
	 */
	@PostConstruct
	public void initBean() {
		DocumentInstance documentInstance = getDocumentContext().getDocumentInstance();
		// find out the document editors
		if (documentInstance != null) {
			findDocumentEditor(documentInstance);
			findDocumentContentAreaProvider(documentInstance);
		}
	}

	/**
	 * Opens the selected document instance for preview.
	 * 
	 * @param selectedDocumentInstance
	 *            Selected {@link DocumentInstance}.
	 * @return Navigation string.
	 */
	public String open(final DocumentInstance selectedDocumentInstance) {
		return open(selectedDocumentInstance, true);
	}

	/**
	 * Opens the selected document instance for edit.
	 * 
	 * @param selectedDocumentInstance
	 *            Selected {@link DocumentInstance}.
	 * @return Navigation string.
	 */
	public String openForEdit(final DocumentInstance selectedDocumentInstance) {
		return open(selectedDocumentInstance, false);
	}

	/**
	 * Opens the selected document instance.
	 * 
	 * @param selectedDocumentInstance
	 *            the selected document instance
	 * @param preview
	 *            the preview
	 * @return the string
	 */
	private String open(final DocumentInstance selectedDocumentInstance, boolean preview) {
		log.debug("CMFWeb: Execute CaseDocumentsTableAction.open - document: "
				+ selectedDocumentInstance.getProperties().get(DocumentProperties.NAME));

		// fire an event that a document is to be opened
		documentOpenEvent.fire(new DocumentOpenEvent(selectedDocumentInstance));

		// default navigation is to reload the page
		String navigationString = NavigationConstants.RELOAD_PAGE;

		// try to find a suitable editor for the current document
		findDocumentEditor(selectedDocumentInstance);
		// if a suitable editor is found, let it handle the document
		navigationString = invokeEditorHandler(selectedDocumentInstance, preview, navigationString);

		findDocumentContentAreaProvider(selectedDocumentInstance);
		invokeContentAreaProviderHandler(selectedDocumentInstance);

		return navigationString;
	}

	/**
	 * Invoke content area provider handler.
	 * 
	 * @param documentInstance
	 *            the document instance
	 */
	private void invokeContentAreaProviderHandler(DocumentInstance documentInstance) {
		if (documentContentAreaProvider != null) {
			documentContentAreaProvider.handle(documentInstance);
		}
	}

	/**
	 * Invoke editor handler.
	 * 
	 * @param selectedDocumentInstance
	 *            the selected document instance
	 * @param preview
	 *            the preview
	 * @param navigationString
	 *            the navigation string
	 * @return the string
	 */
	protected String invokeEditorHandler(final DocumentInstance selectedDocumentInstance,
			boolean preview, String navigationString) {
		String navigation = navigationString;
		if (documentEditor != null) {
			documentEditor.handle(selectedDocumentInstance, preview);
			navigation = NavigationConstants.NAVIGATE_DOCUMENT_DETAILS;
			getDocumentContext().setDocumentInstance(selectedDocumentInstance);
		}
		return navigation;
	}

	/**
	 * Find document content area provider.
	 * 
	 * @param selectedDocumentInstance
	 *            the selected document instance
	 */
	private void findDocumentContentAreaProvider(DocumentInstance selectedDocumentInstance) {
		if (documentContentAreaProvider == null) {
			for (DocumentContentAreaProvider current : documentContentAreaProviders) {
				if (current.canHandle(selectedDocumentInstance)) {
					documentContentAreaProvider = current;
					break;
				}
			}
		}
	}

	/**
	 * Find document editor.
	 * 
	 * @param selectedDocumentInstance
	 *            the selected document instance
	 */
	protected void findDocumentEditor(final DocumentInstance selectedDocumentInstance) {
		if (documentEditor == null) {
			for (DocumentEditor current : documentEditors) {
				if (current.canHandle(selectedDocumentInstance)) {
					documentEditor = current;
					break;
				}
			}
		}
	}

	/**
	 * Getter method for documentEditor.
	 * 
	 * @return the documentEditor
	 */
	public DocumentEditor getDocumentEditor() {
		return documentEditor;
	}

	/**
	 * Setter method for documentEditor.
	 * 
	 * @param documentEditor
	 *            the documentEditor to set
	 */
	public void setDocumentEditor(DocumentEditor documentEditor) {
		this.documentEditor = documentEditor;
	}

	/**
	 * Getter method for documentContentAreaProvider.
	 * 
	 * @return the documentContentAreaProvider
	 */
	public DocumentContentAreaProvider getDocumentContentAreaProvider() {
		return documentContentAreaProvider;
	}

	/**
	 * Setter method for documentContentAreaProvider.
	 * 
	 * @param documentContentAreaProvider
	 *            the documentContentAreaProvider to set
	 */
	public void setDocumentContentAreaProvider(
			DocumentContentAreaProvider documentContentAreaProvider) {
		this.documentContentAreaProvider = documentContentAreaProvider;
	}

}
