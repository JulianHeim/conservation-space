package com.sirma.itt.objects.web.actionsmanager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sirma.cmf.web.actionsmanager.NonDefaultActionButtonExtensionPoint;
import com.sirma.itt.emf.plugin.Extension;
import com.sirma.itt.objects.security.ObjectActionTypeConstants;

/**
 * Extension for {@link NonDefaultActionButtonExtensionPoint} that provides non default action types
 * for objects module.
 * 
 * @author svelikov
 */
@Extension(target = NonDefaultActionButtonExtensionPoint.EXTENSION_POINT, enabled = true, order = 200, priority = 1)
public class ObjectsNonDefaultActionButtons implements NonDefaultActionButtonExtensionPoint {

	@SuppressWarnings("serial")
	private Map<String, List<String>> nondefault = new HashMap<String, List<String>>() {
		{
			put(ObjectActionTypeConstants.OBJECT_MOVE_SAME_CASE, null);
			put(ObjectActionTypeConstants.ATTACH_OBJECT, null);
			put(ObjectActionTypeConstants.ADD_THUMBNAIL, null);
			put(ObjectActionTypeConstants.CLONE, null);
		}

	};

	@Override
	public Map<String, List<String>> getActions() {
		return nondefault;
	}

}
