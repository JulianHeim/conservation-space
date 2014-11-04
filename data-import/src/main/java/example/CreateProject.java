package example;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.sirma.itt.seip.rest.IdentityRestClient;
import com.sirma.itt.seip.rest.OperationExecutor;

public class CreateProject {

	public static void main(String[] args) {
		String applicationUrl = "https://cspace-test.sirmaplatform.com:8443";

		IdentityRestClient identityService = new IdentityRestClient();
		String sessionCookie = identityService.login(applicationUrl, "a.mitev", "mitev");

		Map<String, String> properties = new HashMap<>();

		// create Exhibition project
		JsonObject result = OperationExecutor.build(applicationUrl, sessionCookie)
				.createProject("PRJ10001", properties).execute();
	}
}
