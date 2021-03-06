package com.sirma.cmf.web.form.builder;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.component.html.HtmlMessage;
import javax.faces.component.html.HtmlOutputLabel;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.sirma.cmf.web.form.BuilderCssConstants;
import com.sirma.cmf.web.form.BuilderType;
import com.sirma.cmf.web.form.ComponentType;
import com.sirma.cmf.web.form.FormViewMode;
import com.sirma.cmf.web.form.RegExGenerator;
import com.sirma.cmf.web.util.LabelConstants;
import com.sirma.itt.commons.utils.string.StringUtils;
import com.sirma.itt.emf.codelist.CodelistService;
import com.sirma.itt.emf.codelist.model.CodeValue;
import com.sirma.itt.emf.definition.model.ControlParam;
import com.sirma.itt.emf.definition.model.DataTypeDefinition;
import com.sirma.itt.emf.definition.model.PropertyDefinition;
import com.sirma.itt.emf.domain.DisplayType;
import com.sirma.itt.emf.label.LabelProvider;
import com.sirma.itt.emf.properties.model.PropertyModel;
import com.sirma.itt.emf.util.PathHelper;

/**
 * Base form builder.
 * 
 * @author svelikov
 */
public abstract class FormBuilder extends FormBuilderAdapter {

	/** The log. */
	protected static Logger log = Logger.getLogger(FormBuilder.class);

	/** If log level is trace mode. */
	protected static boolean trace = log.isTraceEnabled();

	/** The reg ex generator. */
	private final RegExGenerator regExGenerator;

	/** The Constant FIELD_PATH_REPLACEMENT_PATTERN. */
	private static final Pattern FIELD_PATH_REPLACEMENT_PATTERN = Pattern.compile("/|\\$|:");

	/** The Constant STYLE_CLASS. */
	protected static final String STYLE_CLASS = "styleClass";

	/** The Constant VALIDATION_PATTERN. */
	public static final String VALIDATION_PATTERN = "validationPattern";

	/** The builder type. */
	private BuilderType builderType;

	/** The form view mode. */
	protected FormViewMode formViewMode;

	/** The instance name. */
	private String instanceName;

	private PropertyModel instance;

	/** The base instance name. */
	private String baseInstanceName;

	/** The lbl provider. */
	protected LabelProvider labelProvider;

	/** The builder helper. */
	FormBuilderHelper builderHelper;

	/** The codelist service. */
	protected CodelistService codelistService;

	/** Current property definition. */
	protected PropertyDefinition propertyDefinition;

	/** The property value. */
	protected Object propertyValue;

	/** Container component where the generated properties will go. */
	protected UIComponent container;

	/**
	 * The required dynamically shows if properties are required dynamically as stated in
	 * definition.
	 */
	protected boolean requiredDynamically;

	/**
	 * Instantiates a new form builder.
	 * 
	 * @param labelProvider
	 *            the label provider
	 * @param codelistService
	 *            the codelist service
	 */
	public FormBuilder(com.sirma.itt.emf.label.LabelProvider labelProvider,
			CodelistService codelistService) {
		this.labelProvider = labelProvider;
		this.codelistService = codelistService;
		builderHelper = new FormBuilderHelper();
		regExGenerator = new RegExGenerator(labelProvider);
	}

	/**
	 * Builder implementation. Dispatches invocation if the function invocations for the component
	 * creation.
	 */
	public void build() {

		String displayStatusKey = getRenderedStatusKey(propertyDefinition, formViewMode);
		boolean displayStatus = renderStatusMap.get(displayStatusKey);

		if (trace) {
			String msg = MessageFormat.format(
					"CMFWeb: building field [{0}] with display status key [{1} = {2}]",
					propertyDefinition.getName(), displayStatusKey, displayStatus);
			log.trace(msg);
		}

		// if display status is true, then go ahead and build visible field
		if (displayStatus) {
			// build a wrapper for the label and field
			UIComponent wrapper = buildFieldWrapper();

			// build label and put it in the wrapper
			wrapper.getChildren().add(buildLabel());

			// build field and put it in the wrapper
			String previewStatusKey = getPreviewStatusKey(propertyDefinition, formViewMode);
			boolean previewStatus = previewStatusMap.get(previewStatusKey);

			if (trace) {
				String msg = MessageFormat.format("CMFWeb: previewStatusKey [{0} = {1}]",
						previewStatusKey, previewStatus);
				log.trace(msg);
			}

			// if preview status is true and the field doesn't have attached
			// codelist that have no values, then render editable field
			if (previewStatusMap.get(previewStatusKey) && !isMissingCodelist()) {

				UIComponent uiComponent = buildField();
				wrapper.getChildren().add(uiComponent);
				setHtmlMessage(wrapper, uiComponent);
				setRequired(wrapper);
				addAfterFieldContent(wrapper);
				addTooltip(wrapper, uiComponent);

			} else {

				if (this instanceof UsernameFieldBuilder) {
					UIComponent uiComponent = buildField();
					wrapper.getChildren().add(uiComponent);
				} else {
					wrapper.getChildren().add(buildOutputField());
				}
			}

			List<UIComponent> containerChildren = container.getChildren();

			containerChildren.add(wrapper);

		} else {
			// For all fields that the status doesn't say to be rendered we build a preview field
			// and set rendered=false.
			// This is necessary in order to allow rnc to be applied on hidden field
			UIComponent wrapper = buildFieldWrapper();
			addStyleClass(wrapper, BuilderCssConstants.HIDDEN_FIELD);
			UIComponent outputField = buildOutputField();
			wrapper.getChildren().add(outputField);
			container.getChildren().add(wrapper);
		}

	}

	/**
	 * Adds the tooltip content inside wrapper element.
	 * 
	 * @param wrapper
	 *            the wrapper
	 * @param uiComponent
	 *            the ui component
	 */
	protected void addTooltip(UIComponent wrapper, UIComponent uiComponent) {

		String tooltip = propertyDefinition.getTooltip();

		if (StringUtils.isNotNullOrEmpty(tooltip)) {

			uiComponent.getAttributes().put(STYLE_CLASS, "has-tooltip");

			HtmlPanelGroup tooltipWrapper = (HtmlPanelGroup) builderHelper
					.getComponent(ComponentType.OUTPUT_PANEL);
			tooltipWrapper.setLayout("block");
			tooltipWrapper.getAttributes().put(STYLE_CLASS, "tooltip");

			UIComponent tooltipContent = builderHelper.getComponent(ComponentType.OUTPUT_PANEL);
			tooltipContent.getAttributes().put(STYLE_CLASS, "cmf-tooltip-content");
			tooltipWrapper.getChildren().add(tooltipContent);

			HtmlOutputText tooltipText = (HtmlOutputText) builderHelper
					.getComponent(ComponentType.OUTPUT_TEXT);
			tooltipText.setValueExpression("value", createValueExpression(tooltip, String.class));
			// tooltipText.setEscape(false);
			tooltipContent.getChildren().add(tooltipText);

			wrapper.getChildren().add(tooltipWrapper);
		}
	}

	/**
	 * Checks if current field has attached codelist and whether that codelist has values in it. If
	 * there is no values in the codelist, then the field should be rendered as output field.
	 * 
	 * @return true, if is missing codelist
	 */
	private boolean isMissingCodelist() {

		boolean renderOutputField = false;

		Integer codelistNumber = propertyDefinition.getCodelist();

		if (codelistNumber != null) {
			Map<String, CodeValue> codeValues = codelistService.getCodeValues(codelistNumber);
			// when given codelist doesn't exists but is set to the
			// field, we just build simple empty output field
			if (codeValues.isEmpty()) {
				renderOutputField = true;
			}
		}

		return renderOutputField;
	}

	/**
	 * Calculates the preview status key.
	 * 
	 * @param propertyDefinition
	 *            the property definition
	 * @param formViewMode
	 *            The {@link FormViewMode}.
	 * @return Calculated form view mode. {@link PropertyDefinition}.
	 */
	public String getPreviewStatusKey(PropertyDefinition propertyDefinition,
			FormViewMode formViewMode) {
		DisplayType displayType = propertyDefinition.getDisplayType();
		StringBuilder previewStatusKey = new StringBuilder();
		previewStatusKey.append(formViewMode.name());

		if (formViewMode == FormViewMode.EDIT) {
			previewStatusKey.append("_" + displayType.name());
		}

		return previewStatusKey.toString();
	}

	/**
	 * Calculates the rendered status key.
	 * 
	 * @param propertyDefinition
	 *            the property definition
	 * @param formViewMode
	 *            The {@link FormViewMode}.
	 * @return Calculated key. {@link PropertyDefinition}.
	 */
	public String getRenderedStatusKey(PropertyDefinition propertyDefinition,
			FormViewMode formViewMode) {

		boolean isPreviewEnabled = propertyDefinition.isPreviewEnabled();

		boolean isEmpty = isEmptyValue(propertyValue);

		DisplayType displayType = propertyDefinition.getDisplayType();

		StringBuilder key = new StringBuilder();
		if (!isEmpty) {
			key.append("FULL_");
		} else {
			key.append("EMPTY_");
			if (isPreviewEnabled) {
				key.append("FORCEPREVIEW_");
			} else {
				key.append("NOPREVIEW_");
			}
		}
		key.append(displayType.name() + "_");
		key.append(formViewMode.name());

		return key.toString();
	}

	/**
	 * Checks if the value of property definition exists and is not an empty string.
	 * 
	 * @param value
	 *            The value to check.
	 * @return true if the value is null or is an empty string and false otherwise.
	 */
	protected boolean isEmptyValue(Object value) {
		boolean isEmpty = true;

		if (value != null) {
			if (value instanceof String) {
				isEmpty = "".equals(value);
			} else {
				isEmpty = false;
			}
		}

		return isEmpty;
	}

	/**
	 * Sets a HtmlMessage in the wrapper for the given field.
	 * 
	 * @param wrapper
	 *            The field wrapper.
	 * @param uiComponent
	 *            The field.
	 */
	protected void setHtmlMessage(UIComponent wrapper, UIComponent uiComponent) {
		HtmlMessage htmlMessage = (HtmlMessage) createComponentInstance("javax.faces.HtmlMessage",
				"javax.faces.Message");

		htmlMessage.setFor(uiComponent.getId());
		htmlMessage.setTitle(labelProvider.getValue(LabelConstants.MSG_ERROR_REQUIRED_FIELD));
		htmlMessage.setErrorClass(BuilderCssConstants.CMF_REQUIRED_MESSAGE_STYLE);
		wrapper.getChildren().add(htmlMessage);
	}

	/**
	 * If field is set to be required, then apply some styling to the field wrapper and add faces
	 * message component for the field in the wrapper.
	 * 
	 * @param wrapper
	 *            The field wrapper.
	 */
	protected void setRequired(UIComponent wrapper) {
		if (isRequiredDynamically()) {
			String styleClass = " " + BuilderCssConstants.CMF_REQUIRED_FIELD;
			addStyleClass(wrapper, styleClass);
		}
	}

	/**
	 * Renders preview only fields.
	 * 
	 * @return Output field.
	 */
	public UIComponent buildOutputField() {
		HtmlOutputText output = (HtmlOutputText) createComponentInstance(
				"javax.faces.HtmlOutputText", "javax.faces.Text");
		String propertyName = propertyDefinition.getName();

		if (StringUtils.isNullOrEmpty(propertyName)) {
			propertyName = getGeneratedFieldName();
			// if name is missing set the generated one as simple string
			output.getAttributes().put("value", propertyName);
		} else {
			String type = propertyDefinition.getDataType().getName();
			ValueExpression ve = null;
			// if the type is date or datetime, convert the value using date
			// converter
			if (DataTypeDefinition.DATE.equals(type)) {
				ve = createValueExpression("#{dateUtil.getFormattedDate(" + instanceName
						+ ".properties['" + propertyName + "'])}", String.class);
			} else if (DataTypeDefinition.DATETIME.equals(type)) {
				ve = createValueExpression("#{dateUtil.getFormattedDateTime(" + instanceName
						+ ".properties['" + propertyName + "'])}", String.class);
			} else {
				Integer codelist = propertyDefinition.getCodelist();
				String valueExpressionString = null;
				if (codelist != null) {
					valueExpressionString = getValueExpressionStringForCodelist(instanceName,
							propertyName, codelist);
				} else {
					valueExpressionString = getValueExpressionString(instanceName, propertyName);
				}
				ve = createValueExpression(valueExpressionString, getValueResultType());
			}
			output.setValueExpression("value", ve);
		}

		output.setId(getIdForField(propertyName));
		addStyleClass(output, BuilderCssConstants.CMF_PREVIEW_FIELD);

		return output;
	}

	/**
	 * Retrieve date format hint label for date-pickers.
	 * 
	 * @param configDatePattern
	 *            date/time format from configuration
	 * @return combine message for date/time format
	 */
	public String getDateFormatHintLabel(String configDatePattern) {
		String label = LabelConstants.DATEPICKER_DATEFORMAT_HINT;
		Date date = new Date();
		String formatedDate = new SimpleDateFormat(configDatePattern).format(date);
		label = labelProvider.getValue(label) + formatedDate;
		return label;
	}

	/**
	 * Gets the id for field using as target the passed to builder property definition object.
	 * 
	 * @param propertyName
	 *            the property name
	 * @return the id for field
	 */
	protected String getIdForField(String propertyName) {
		return getId(propertyDefinition, propertyName);
	}

	/**
	 * Gets the id for field using the property definition argument.
	 * 
	 * @param propertyDefinition
	 *            the property definition
	 * @param propertyName
	 *            the property name
	 * @return the id for field
	 */
	protected String getIdForField(PropertyDefinition propertyDefinition, String propertyName) {

		return getId(propertyDefinition, propertyName);
	}

	/**
	 * Gets the id.
	 * 
	 * @param propertyDefinition
	 *            the property definition
	 * @param propertyName
	 *            the property name
	 * @return the id
	 */
	protected String getId(PropertyDefinition propertyDefinition, String propertyName) {

		String path = PathHelper.getPath(propertyDefinition);
		path = FIELD_PATH_REPLACEMENT_PATTERN.matcher(path + "_" + propertyName).replaceAll("_");

		return path;
	}

	/**
	 * Creates a {@link UIComponent} field.
	 * 
	 * @return {@link UIComponent}.
	 */
	public UIComponent buildField() {

		UIComponent uiComponent = getComponentInstance();

		setFieldValidator(uiComponent, regExGenerator.getPattern(propertyDefinition.getType()));

		String propertyName = propertyDefinition.getName();
		if (StringUtils.isNullOrEmpty(propertyName)) {

			propertyName = getGeneratedFieldName();
			// if name is missing set the generated one as simple string
			uiComponent.getAttributes().put("value", propertyName);

		} else {

			String valueExpressionString = getValueExpressionString(instanceName, propertyName);
			ValueExpression ve = createValueExpression(valueExpressionString, getValueResultType());
			uiComponent.setValueExpression("value", ve);

		}

		uiComponent.setId(getIdForField(propertyName));

		// uiComponent.getAttributes().put(SIMPLE_ID_ATTRIBUTE, propertyName);

		// Set the field as required if necessary and add required message.
		if (isRequiredDynamically()) {
			((UIInput) uiComponent).setRequired(true);
			((UIInput) uiComponent).setRequiredMessage(labelProvider
					.getValue(LabelConstants.MSG_ERROR_REQUIRED_FIELD));
		}

		updateField(uiComponent);

		return uiComponent;
	}

	/**
	 * Getter for current timestamp.
	 * 
	 * @return Timestamp as string
	 */
	protected String getTimestamp() {
		return Calendar.getInstance().getTimeInMillis() + "";
	}

	/**
	 * Generates unique field name.
	 * 
	 * @return Generated field name.
	 */
	protected String getGeneratedFieldName() {
		return "generatedFieldName_" + getTimestamp();
	}

	/**
	 * Generates label.
	 * 
	 * @return generated label.
	 */
	protected String getGeneratedLabel() {
		return "Missing label! Please check the definition!";
	}

	/**
	 * Builds some wrapper ui component where the created input field and label will be placed.
	 * 
	 * @return Created wrapper component.
	 */
	public UIComponent buildFieldWrapper() {
		// build panel wrapper where the field and the label will be put.
		HtmlPanelGroup wrapper = (HtmlPanelGroup) builderHelper
				.getComponent(ComponentType.OUTPUT_PANEL);

		addStyleClass(wrapper, BuilderCssConstants.CMF_FIELD_WRAPPER);

		updateWrapper(wrapper);

		return wrapper;
	}

	/**
	 * Builds a {@link HtmlOutputLabel} component. This method initializes only base properties and
	 * delegates to the implementation to enhance the label component if necessary.
	 * 
	 * @return The created label component.
	 */
	public UIOutput buildLabel() {
		HtmlOutputLabel labelField = (HtmlOutputLabel) builderHelper
				.getComponent(ComponentType.OUTPUT_LABEL);

		String label = null;
		String name = null;
		if (propertyDefinition != null) {
			name = propertyDefinition.getName();
			label = propertyDefinition.getLabel();
		}

		if (StringUtils.isNullOrEmpty(label)) {
			log.warn("CMFWeb: missing label definition for field [" + name + "]");
			label = getGeneratedLabel();
		}

		if (StringUtils.isNullOrEmpty(name)) {
			name = getGeneratedFieldName();
		}

		labelField.setValue(label + BuilderCssConstants.CMF_AFTER_LABEL_CONTENT);
		labelField.setFor(name);

		labelField.getAttributes().put(STYLE_CLASS, BuilderCssConstants.CMF_DYNAMIC_FORM_LABEL);

		// call template method where the label component will be enhanced if
		// necessary
		updateLabel(labelField);

		return labelField;
	}

	/**
	 * Creates the component instance.
	 * 
	 * @param componentType
	 *            the component type
	 * @param componentRenderer
	 *            the component renderer
	 * @return the uI component
	 */
	protected UIComponent createComponentInstance(String componentType, String componentRenderer) {

		FacesContext facesContext = FacesContext.getCurrentInstance();

		UIComponent createdComponent = facesContext.getApplication().createComponent(facesContext,
				componentType, componentRenderer);

		return createdComponent;
	}

	/**
	 * Creates the component instance.
	 * 
	 * @param componentType
	 *            the component type
	 * @return the uI component
	 */
	protected UIComponent createComponentInstance(String componentType) {

		FacesContext facesContext = FacesContext.getCurrentInstance();

		UIComponent createdComponent = facesContext.getApplication().createComponent(componentType);

		return createdComponent;
	}

	/**
	 * Sets the rendered state of the provided component to false (component should not be visible).
	 * 
	 * @param uiComponent
	 *            the new hidden {@link UIComponent}.
	 */
	protected void setHidden(UIComponent uiComponent) {
		uiComponent.setRendered(false);
	}

	/**
	 * Sets disabled attribute and style class if necessary.
	 * 
	 * @param uiComponent
	 *            uiComponent.
	 */
	protected void setDisabled(UIComponent uiComponent) {
		Map<String, Object> attributes = uiComponent.getAttributes();
		attributes.put("disabled", true);
		addStyleClass(uiComponent, BuilderCssConstants.CMF_DISABLED_FIELD);
	}

	/**
	 * Add a style class to the styleClass attribute of the provided component.
	 * 
	 * @param component
	 *            component
	 * @param styleClass
	 *            The style class attribute to add.
	 */
	protected void addStyleClass(UIComponent component, String styleClass) {
		if ((component == null) || (styleClass == null)) {
			return;
		}

		String styleClassAttribute = (String) component.getAttributes().get(STYLE_CLASS);

		if (StringUtils.isNotNullOrEmpty(styleClassAttribute)) {
			styleClassAttribute += " " + styleClass;
		} else {
			styleClassAttribute = styleClass;
		}

		component.getAttributes().put(STYLE_CLASS, styleClassAttribute);
	}

	/**
	 * Creates an output filed with escape=false that is used to inject a script tag to load js
	 * module inside html.
	 * 
	 * @param filename
	 *            the js module filename
	 * @return the script output
	 */
	protected HtmlOutputText getScriptFileInject(String filename) {
		HtmlOutputText outputScript = getScriptOutputField();
		HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance()
				.getExternalContext().getRequest();
		String context = request.getContextPath();
		String script = "<script type='text/javascript' src='" + context + "/js/" + filename
				+ "'></script>";
		outputScript.setValue(script);
		return outputScript;
	}

	/**
	 * Creates an output field with escape=false that is used to add script tag with some js code
	 * inside in html.
	 * 
	 * @param script
	 *            the script value
	 * @return the script output
	 */
	protected HtmlOutputText getScriptOutput(String script) {
		HtmlOutputText outputScript = getScriptOutputField();
		String scriptValue = "<script type='text/javascript'>" + script + "</script>";
		outputScript.setValue(scriptValue);
		return outputScript;
	}

	/**
	 * Gets the script output field.
	 * 
	 * @return the script output field
	 */
	private HtmlOutputText getScriptOutputField() {
		HtmlOutputText outputScript = (HtmlOutputText) builderHelper
				.getComponent(ComponentType.OUTPUT_TEXT);
		outputScript.setEscape(false);
		return outputScript;
	}

	/**
	 * This map holds the statuses as described in Lot3_AIS_ObjectsMetadata.xls metadata properties
	 * sheet. <br>
	 * The keys are assembled as follows: <br>
	 * FULL|EMPTY - if the field has some value or is empty <br>
	 * FORCEPREVIEW|NOPREVIEW - if isPreviewEnabled is set to be true or false <br>
	 * Display status: <br>
	 * EDITABLE - <br>
	 * HIDDEN - <br>
	 * SYSTEM - <br>
	 * READ_ONLY - <br>
	 * Form view mode: <br>
	 * EDIT - if the form is rendered in edit mode <br>
	 * PREVIEW - if the form is rendered in preview mode <br>
	 */
	protected static Map<String, Boolean> renderStatusMap = new HashMap<String, Boolean>();
	static {
		renderStatusMap.put("FULL_EDITABLE_EDIT", true);
		renderStatusMap.put("FULL_HIDDEN_EDIT", false);
		renderStatusMap.put("FULL_SYSTEM_EDIT", false);
		renderStatusMap.put("FULL_READ_ONLY_EDIT", true);

		renderStatusMap.put("FULL_EDITABLE_PREVIEW", true);
		renderStatusMap.put("FULL_HIDDEN_PREVIEW", true);
		renderStatusMap.put("FULL_SYSTEM_PREVIEW", false);
		renderStatusMap.put("FULL_READ_ONLY_PREVIEW", true);

		renderStatusMap.put("EMPTY_FORCEPREVIEW_EDITABLE_EDIT", true);
		renderStatusMap.put("EMPTY_FORCEPREVIEW_HIDDEN_EDIT", false);
		renderStatusMap.put("EMPTY_FORCEPREVIEW_SYSTEM_EDIT", false);
		renderStatusMap.put("EMPTY_FORCEPREVIEW_READ_ONLY_EDIT", true);

		renderStatusMap.put("EMPTY_FORCEPREVIEW_EDITABLE_PREVIEW", true);
		renderStatusMap.put("EMPTY_FORCEPREVIEW_HIDDEN_PREVIEW", true);
		renderStatusMap.put("EMPTY_FORCEPREVIEW_SYSTEM_PREVIEW", false);
		renderStatusMap.put("EMPTY_FORCEPREVIEW_READ_ONLY_PREVIEW", true);

		renderStatusMap.put("EMPTY_NOPREVIEW_EDITABLE_EDIT", true);
		renderStatusMap.put("EMPTY_NOPREVIEW_HIDDEN_EDIT", false);
		renderStatusMap.put("EMPTY_NOPREVIEW_SYSTEM_EDIT", false);
		renderStatusMap.put("EMPTY_NOPREVIEW_READ_ONLY_EDIT", false);

		renderStatusMap.put("EMPTY_NOPREVIEW_EDITABLE_PREVIEW", false);
		renderStatusMap.put("EMPTY_NOPREVIEW_HIDDEN_PREVIEW", false);
		renderStatusMap.put("EMPTY_NOPREVIEW_SYSTEM_PREVIEW", false);
		renderStatusMap.put("EMPTY_NOPREVIEW_READ_ONLY_PREVIEW", false);
	}

	/** The preview status map. */
	protected static Map<String, Boolean> previewStatusMap = new HashMap<String, Boolean>();
	static {
		previewStatusMap.put("EDIT_EDITABLE", true);
		previewStatusMap.put("EDIT_READ_ONLY", false);
		previewStatusMap.put("PREVIEW", false);
		previewStatusMap.put("PRINT", false);
	}

	/**
	 * Getter method for propertyDefinition.
	 * 
	 * @return the propertyDefinition
	 */
	public PropertyDefinition getPropertyDefinition() {
		return propertyDefinition;
	}

	/**
	 * Setter method for propertyDefinition.
	 * 
	 * @param propertyDefinition
	 *            the propertyDefinition to set
	 */
	public void setPropertyDefinition(PropertyDefinition propertyDefinition) {
		this.propertyDefinition = propertyDefinition;
	}

	/**
	 * Getter method for container.
	 * 
	 * @return the container
	 */
	public UIComponent getContainer() {
		return container;
	}

	/**
	 * Setter method for container.
	 * 
	 * @param container
	 *            the container to set
	 */
	public void setContainer(UIComponent container) {
		this.container = container;
	}

	/**
	 * Getter method for builderType.
	 * 
	 * @return the builderType
	 */
	public BuilderType getBuilderType() {
		return builderType;
	}

	/**
	 * Setter method for builderType.
	 * 
	 * @param builderType
	 *            the builderType to set
	 */
	public void setBuilderType(BuilderType builderType) {
		this.builderType = builderType;
	}

	/**
	 * Getter method for viewMode.
	 * 
	 * @return the viewMode
	 */
	public FormViewMode getViewMode() {
		return formViewMode;
	}

	/**
	 * Setter method for viewMode.
	 * 
	 * @param viewMode
	 *            the viewMode to set
	 */
	public void setViewMode(FormViewMode viewMode) {
		formViewMode = viewMode;
	}

	/**
	 * Setter method for instanceName.
	 * 
	 * @param instanceName
	 *            the instanceName to set
	 */
	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	/**
	 * Getter method for propertyValue.
	 * 
	 * @return the propertyValue
	 */
	public Object getPropertyValue() {
		return propertyValue;
	}

	/**
	 * Setter method for propertyValue.
	 * 
	 * @param propertyValue
	 *            the propertyValue to set
	 */
	public void setPropertyValue(Object propertyValue) {
		this.propertyValue = propertyValue;
	}

	/**
	 * Gets the as map.
	 * 
	 * @param controlParameters
	 *            the control parameters
	 * @return the as map
	 */
	protected Map<String, ControlParam> getAsMap(List<ControlParam> controlParameters) {
		Map<String, ControlParam> map = new HashMap<String, ControlParam>(controlParameters.size());

		for (ControlParam controlParam : controlParameters) {
			map.put(controlParam.getName(), controlParam);
		}

		return map;

	}

	/**
	 * Getter method for instanceName.
	 * 
	 * @return the instanceName
	 */
	public String getInstanceName() {
		return instanceName;
	}

	/**
	 * Getter method for lblProvider.
	 * 
	 * @return the lblProvider
	 */
	public com.sirma.itt.emf.label.LabelProvider getLblProvider() {
		return labelProvider;
	}

	/**
	 * Setter method for lblProvider.
	 * 
	 * @param lblProvider
	 *            the lblProvider to set
	 */
	public void setLblProvider(com.sirma.itt.emf.label.LabelProvider lblProvider) {
		labelProvider = lblProvider;
	}

	/**
	 * Getter method for codelistService.
	 * 
	 * @return the codelistService
	 */
	public CodelistService getCodelistService() {
		return codelistService;
	}

	/**
	 * Setter method for codelistService.
	 * 
	 * @param codelistService
	 *            the codelistService to set
	 */
	public void setCodelistService(CodelistService codelistService) {
		this.codelistService = codelistService;
	}

	/**
	 * Getter method for baseInstanceName.
	 * 
	 * @return the baseInstanceName
	 */
	public String getBaseInstanceName() {
		return baseInstanceName;
	}

	/**
	 * Setter method for baseInstanceName.
	 * 
	 * @param baseInstanceName
	 *            the baseInstanceName to set
	 */
	public void setBaseInstanceName(String baseInstanceName) {
		this.baseInstanceName = baseInstanceName;
	}

	/**
	 * Getter method for requiredDynamically.
	 * 
	 * @return the requiredDynamically
	 */
	public boolean isRequiredDynamically() {
		return requiredDynamically || propertyDefinition.isMandatory();
	}

	/**
	 * Setter method for requiredDynamically.
	 * 
	 * @param requiredDynamically
	 *            the requiredDynamically to set
	 */
	public void setRequiredDynamically(boolean requiredDynamically) {
		this.requiredDynamically = requiredDynamically;
	}

	/**
	 * Getter method for instance.
	 * 
	 * @return the instance
	 */
	public PropertyModel getInstance() {
		return instance;
	}

	/**
	 * Setter method for instance.
	 * 
	 * @param instance
	 *            the instance to set
	 */
	public void setInstance(PropertyModel instance) {
		this.instance = instance;
	}

}
