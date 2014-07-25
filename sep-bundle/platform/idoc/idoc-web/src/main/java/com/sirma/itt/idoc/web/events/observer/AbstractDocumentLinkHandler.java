package com.sirma.itt.idoc.web.events.observer;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.sirma.itt.cmf.beans.model.DocumentInstance;
import com.sirma.itt.cmf.beans.model.VersionInfo;
import com.sirma.itt.cmf.services.DocumentService;
import com.sirma.itt.emf.converter.TypeConverter;
import com.sirma.itt.emf.forum.model.CommentInstance;
import com.sirma.itt.emf.instance.model.InitializedInstance;
import com.sirma.itt.emf.instance.model.Instance;
import com.sirma.itt.emf.instance.model.InstanceReference;
import com.sirma.itt.emf.link.LinkReference;
import com.sirma.itt.emf.link.LinkService;
import com.sirma.itt.emf.rest.model.ViewInstance;
import com.sirma.itt.emf.util.JsonUtil;
import com.sirma.itt.idoc.web.document.IntelligentDocumentEditor;
import com.sirma.itt.idoc.web.document.IntelligentDocumentService;
import com.sirma.itt.idoc.web.events.CreateRelationshipFromLinkEvent;
import com.sirma.itt.idoc.web.events.CreateRelationshipFromWidgetEvent;
import com.sirma.itt.idoc.web.events.WidgetBinding;

/**
 * Base class for parsing the document content and firing events for widgets.
 *
 * @author yasko
 */
public abstract class AbstractDocumentLinkHandler {

	@Any
	@Inject
	private IntelligentDocumentEditor editor;

	/** The document service. */
	@Inject
	private DocumentService documentService;

	/** The type converter. */
	@Inject
	private TypeConverter typeConverter;

	/** The idoc service. */
	@Inject
	private IntelligentDocumentService idocService;

	/** The link service. */
	@Inject
	private LinkService linkService;

	@Inject
	private Event<CreateRelationshipFromWidgetEvent> createRelationshipsEvent;

	@Inject
	private Event<CreateRelationshipFromLinkEvent> createLinkRelationshipsEvent;

	private static final Logger LOGGER = Logger.getLogger(AbstractDocumentLinkHandler.class);

	/**
	 * Parses the document and fires events for widgets.
	 *
	 * @param instance
	 *            {@link Instance} to parse.
	 * @param linkFrom
	 *            The 'real' instance to be used in for example in creating relationships, either
	 *            {@link DocumentInstance} or ObjectInstance.
	 */
	public void handle(Instance instance, Instance linkFrom) {

		DocumentInstance documentInstance = null;
		CommentInstance commentInstance = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		String content = null;
		Document doc = null;

		if (instance instanceof DocumentInstance) {
			removeOldDocumentLinks(instance, linkFrom.toReference());
			documentInstance = (DocumentInstance) instance;

			if (!editor.canHandle(documentInstance)) {
				return;
			}

			// FIXME: work with the stream
			documentService.getContent(documentInstance, baos);

			try {
				content = new String(baos.toByteArray(), "UTF-8");

				doc = Jsoup.parse(content);
				Elements widgets = doc.select(".widget");
				Iterator<Element> iterator = widgets.iterator();
				while (iterator.hasNext()) {
					Element widget = iterator.next();
					String widgetName = widget.attr("data-name");

					JSONObject configAsJson = null;
					JSONObject valueAsJson = null;
					String config = widget.attr("data-config");
					String value = widget.attr("data-value");
					if (StringUtils.isNotBlank(config) && StringUtils.isNotBlank(value)) {
						/*
						 * FIXME: When we save the widget config we html escape it, so it can be
						 * saved as an attribute, but when we save the document the sanitizer kicks
						 * in and it escapes it again. Maybe we should save the attribute as a
						 * base64 string.
						 */
						config = StringEscapeUtils.unescapeHtml(StringEscapeUtils
								.unescapeHtml(config));
						configAsJson = new JSONObject(config);
						value = StringEscapeUtils.unescapeHtml(StringEscapeUtils
								.unescapeHtml(value));
						valueAsJson = new JSONObject(value);
						WidgetBinding binding = new WidgetBinding(widgetName);
						createRelationshipsEvent.select(binding).fire(
								new CreateRelationshipFromWidgetEvent(widgetName, configAsJson,
										valueAsJson, linkFrom.toReference()));
					}
				}

				Elements links = doc.getElementsByClass("instance-link");
				iterator = links.iterator();
				while (iterator.hasNext()) {
					Element link = iterator.next();
					createLinkRelationshipsEvent.fire(new CreateRelationshipFromLinkEvent(link
							.attr("data-instance-id"), link.attr("data-instance-type"), linkFrom
							.toReference()));
				}

			} catch (UnsupportedEncodingException e) {
				LOGGER.error("Can not convert document content to ByteArrayOutputStream ", e);
			} catch (JSONException e) {
				LOGGER.error("Can not parse given strng to JSON", e);
			}

			// If event is fired by comment extract all links to documents/objects
			// and make relations between comment and corresponding document/object
		} else if (instance instanceof CommentInstance) {
			commentInstance = (CommentInstance) instance;
			InstanceReference reference = commentInstance.getTopic().getTopicAbout();

			if (linkFrom == null) {
				removeOldCommentLinks(commentInstance, reference);
				return;
			}
			if (linkFrom instanceof CommentInstance) {
				removeOldCommentLinks((CommentInstance) linkFrom, reference);
			}

			doc = Jsoup.parse(commentInstance.getComment());
			Elements links = doc.getElementsByClass("instance-link");

			Iterator<Element> iterator = links.iterator();
			while (iterator.hasNext()) {
				Element link = iterator.next();
				createLinkRelationshipsEvent.fire(new CreateRelationshipFromLinkEvent(link
						.attr("data-instance-id"), link.attr("data-instance-type"), reference));
			}
		}
	}

	/**
	 * Parses the document. Compare old and new version and deactivate all removed links.
	 *
	 * @param instance
	 *            {@link Instance} to parse.
	 * @param linkFrom
	 *            The 'real' instance to be used in for example in creating relationships, either
	 *            {@link DocumentInstance} or ObjectInstance.
	 */
	public void removeOldDocumentLinks(Instance instance, InstanceReference linkFrom) {

		// Get previous version of document
		ViewInstance viewInstance = typeConverter.convert(ViewInstance.class, instance);
		DocumentInstance view = (DocumentInstance) typeConverter.convert(InitializedInstance.class,
				viewInstance.getViewReference()).getInstance();
		List<VersionInfo> versions = idocService.getVersions(view);

		VersionInfo[] version = new VersionInfo[2];
		DocumentInstance[] docInstance = new DocumentInstance[2];
		ArrayList[] widgets = new ArrayList[2];
		ArrayList[] internalLinks = new ArrayList[2];

		// If there's no previous version can not exist deleted links so just return
		if (versions.size() < 2) {
			return;
		}

		// 0 -> current version, 1-> previous version
		for (int i = 1; i > -1; i--) {
			version[i] = versions.get(i);
			docInstance[i] = idocService
					.loadVersion(instance.getId(), version[i].getVersionLabel());

			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				documentService.getContent(docInstance[i], baos);
				String content;
				content = new String(baos.toByteArray(), "UTF-8");

				Document doc = Jsoup.parse(content);
				Elements docWidgets = doc.select(".widget");
				Iterator<Element> iterator = docWidgets.iterator();
				widgets[i] = new ArrayList<>();
				while (iterator.hasNext()) {
					Element widget = iterator.next();
					String name = widget.attr("data-name");

					if ("datatable".equals(name) || "objectData".equals(name)
							|| "imageWidget".equals(name)) {
						String value = widget.attr("data-value");
						if (StringUtils.isNotBlank(value)) {

							value = StringEscapeUtils.unescapeHtml(StringEscapeUtils
									.unescapeHtml(value));
							JSONObject valueAsJson = new JSONObject(value);

							try {
								if (valueAsJson.has("manuallySelectedObjects")) {

									JSONArray manuallySelected = valueAsJson
											.getJSONArray("manuallySelectedObjects");
									int length = manuallySelected.length();

									for (int a = 0; a < length; a++) {
										JSONObject item = manuallySelected.getJSONObject(a);
										String id = JsonUtil.getStringValue(item, "dbId");
										widgets[i].add(id);
									}
								}
							} catch (Exception e) {
								LOGGER.error("", e);
							}

							JSONObject selectedObject = JsonUtil.getJsonObject(valueAsJson,
									"selectedObject");
							if (selectedObject != null) {
								String id = JsonUtil.getStringValue(selectedObject, "dbId");
								widgets[i].add(id);
							}

							try {
								value = widget.attr("data-config");
								value = StringEscapeUtils.unescapeHtml(StringEscapeUtils
										.unescapeHtml(value));
								valueAsJson = new JSONObject(value);

								String id = JsonUtil.getStringValue(valueAsJson, "imageId");

								if (id != null) {
									widgets[i].add(id);
								}
							} catch (Exception e) {
								LOGGER.error("", e);
							}
						}
					}
				}

			} catch (UnsupportedEncodingException e) {
				LOGGER.error("Can not convert document content to ByteArrayOutputStream ", e);
			} catch (JSONException e) {
				LOGGER.error("Can not parse given strng to JSON", e);
			}

			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				documentService.getContent(docInstance[i], baos);

				String content = new String(baos.toByteArray(), "UTF-8");
				Document doc = Jsoup.parse(content);

				Elements links = doc.getElementsByClass("instance-link");
				Iterator<Element> iterator = links.iterator();
				internalLinks[i] = new ArrayList<>();
				while (iterator.hasNext()) {
					Element link = iterator.next();
					internalLinks[i].add(link.attr("data-instance-id"));
				}
			} catch (UnsupportedEncodingException e) {
				LOGGER.error("Can not convert document content to ByteArrayOutputStream ", e);
			}
		}

		// remove old widgets
		widgets[1].removeAll(widgets[0]);

		// remove old internal links
		internalLinks[1].removeAll(internalLinks[0]);

		List<LinkReference> oldLinks = linkService.getLinksTo(linkFrom);

		for (LinkReference link : oldLinks) {

			if (widgets[1].contains(link.getFrom().getIdentifier())) {
				linkService.removeLinkById(link.getId()); 
			}

			if (internalLinks[1].contains(link.getFrom().getIdentifier())) {
				linkService.removeLinkById(link.getId());
			}
		}
		
		oldLinks = linkService.getLinks(linkFrom);
	
		for (LinkReference link : oldLinks) {

			if (widgets[1].contains(link.getTo().getIdentifier())) {
				linkService.removeLinkById(link.getId()); 
			}

			if (internalLinks[1].contains(link.getFrom().getIdentifier())) {
				// deactivate relation
				linkService.removeLinkById(link.getId());
			}
		}
	}

	/**
	 * Parses the old comment and remove links.
	 *
	 * @param instance
	 *            {@link Instance} to parse.
	 * @param reference
	 *            The 'real' instance to be used in for example in creating relationships, either
	 *            {@link DocumentInstance} or ObjectInstance.
	 */
	public void removeOldCommentLinks(CommentInstance instance, InstanceReference reference) {

		if (instance.getComment() == null) {
			return;
		}

		Document doc = Jsoup.parse(instance.getComment());
		Elements links = doc.getElementsByClass("instance-link");

		Iterator<Element> iterator = links.iterator();
		ArrayList internalLinks = new ArrayList<>();
		while (iterator.hasNext()) {
			Element link = iterator.next();
			internalLinks.add(link.attr("data-instance-id"));
		}

		List<LinkReference> oldLinks = linkService.getLinksTo(reference);

		for (LinkReference link : oldLinks) {
			if (internalLinks.contains(link.getFrom().getIdentifier())) {
				// deactivate relation
				linkService.removeLinkById(link.getId());
			}
		}
	}

}
