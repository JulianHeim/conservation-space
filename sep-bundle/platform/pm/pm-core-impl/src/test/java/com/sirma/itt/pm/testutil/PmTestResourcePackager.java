package com.sirma.itt.pm.testutil;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.formatter.Formatters;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;

import com.sirma.itt.cmf.services.mock.InstanceLinkExpressionEvaluator;
import com.sirma.itt.cmf.services.mock.UserLinkExpressionEvaluator;
import com.sirma.itt.cmf.testutil.CmfTestResourcePackager;
import com.sirma.itt.cmf.testutil.TestPackageBuilder;
import com.sirma.itt.cmf.testutil.TestableJarModules;
import com.sirma.itt.pm.config.ProjectDefinitionCompilerCallback;
import com.sirma.itt.pm.constants.PMConfigProperties;
import com.sirma.itt.pm.domain.definitions.impl.ProjectDefinitionImpl;
import com.sirma.itt.pm.domain.entity.ProjectEntity;
import com.sirma.itt.pm.domain.jaxb.ObjectFactory;
import com.sirma.itt.pm.domain.jaxb.ProjectDefinition;
import com.sirma.itt.pm.dozer.PmDozerMappingProvider;
import com.sirma.itt.pm.instance.PmEntityTypeProviderExtension;
import com.sirma.itt.pm.instance.ProjectInstanceToServiceRegisterExtension;
import com.sirma.itt.pm.patch.PmDbPatch;
import com.sirma.itt.pm.security.evaluator.PmGlobalRoleEvaluator;
import com.sirma.itt.pm.security.evaluator.ProjectRoleEvaluator;
import com.sirma.itt.pm.security.provider.PmActionProvider;
import com.sirma.itt.pm.security.provider.PmCollectableRoleProviderExtension;
import com.sirma.itt.pm.security.provider.PmRoleProviderExtension;
import com.sirma.itt.pm.service.adapter.PMDefinitionServiceExtensionMock;
import com.sirma.itt.pm.service.adapter.PMProjectInstanceAdapterServiceMock;
import com.sirma.itt.pm.services.DbQueryTemplatesPm;
import com.sirma.itt.pm.services.impl.PmRoleServiceImpl;
import com.sirma.itt.pm.services.impl.ProjectDefinitionManagementServiceImpl;
import com.sirma.itt.pm.services.impl.ProjectServiceImpl;
import com.sirma.itt.pm.services.impl.dao.BasePmInstanceDaoImpl;
import com.sirma.itt.pm.services.impl.dao.ProjectDefinitionAccessor;
import com.sirma.itt.pm.services.impl.dao.ProjectInstanceDao;
import com.sirma.itt.pm.services.impl.dao.ProjectInstancePropertyModelCallback;
import com.sirma.itt.pm.services.impl.dao.ProjectStateServiceExtension;
import com.sirma.itt.pm.util.datatype.PmInstanceToLinkSourceConverterProvider;
import com.sirma.itt.pm.util.serialization.PmKryoInitializer;
import com.sirma.itt.pm.xml.schema.PmSchemaBuilder;

/**
 * Helper class for building test archives using the ShrinkWrap API.
 *
 * @author bbanchev
 */
public class PmTestResourcePackager extends CmfTestResourcePackager implements TestPackageBuilder {

	/**
	 * Instantiates a new pm test resource builder.
	 */
	public PmTestResourcePackager() {
		super();
	}

	@Override
	public TestPackageBuilder buildJar(TestableJarModules... jarModules) {
		addOptional(jarModules);
		testableJar.writeTo(System.out, Formatters.VERBOSE);
		return this;
	}

	/**
	 * Custom pom to use for optimization. Highly recomended for usage
	 *
	 * @param pathToPomFile
	 *            is pom name at root of project
	 */
	public PmTestResourcePackager(String pathToPomFile) {
		super(pathToPomFile);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected JavaArchive createBaseArchive() {
		return super.createBaseArchive();
	}

	/**
	 * Builds the adapters.
	 *
	 * @return the class[]
	 */
	public Class<?>[] buildMockAdapters() {
		Class<?>[] mockAdapters = super.buildMockAdapters();

		List<Class<?>> asList = Arrays.asList(mockAdapters);
		LinkedList<Class<?>> linkedList = new LinkedList<Class<?>>(asList);
		linkedList.add(PMProjectInstanceAdapterServiceMock.class);
		return linkedList.toArray(new Class[linkedList.size()]);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<?>[] buildMockAdaptersExtensions() {
		Class<?>[] buildMockAdaptersExtensions = super.buildMockAdaptersExtensions();
		return concatenate(buildMockAdaptersExtensions,
				new Class<?>[] { PMDefinitionServiceExtensionMock.class });
	}

	/**
	 * Builds the dao classes.
	 *
	 * @return the class[]
	 */
	@Override
	public Class<?>[] buildDaoClasses() {
		return new Class<?>[] { PmDbPatch.class, ProjectInstancePropertyModelCallback.class,
				ProjectDefinitionAccessor.class, ProjectDefinitionCompilerCallback.class,
				ProjectInstanceDao.class, BasePmInstanceDaoImpl.class };
	}

	/**
	 * Builds the definitions.
	 *
	 * @return the class[]
	 */
	@Override
	public Class<?>[] buildConverters() {
		return EMTPTY_ARRAY;
	}

	/**
	 * Builds the type instances.
	 *
	 * @return the class[]
	 */
	@Override
	public Class<?>[] buildTypeInstances() {
		return EMTPTY_ARRAY;
	}

	/**
	 * Builds the entity classes.
	 *
	 * @return the class[]
	 */
	@Override
	public Class<?>[] buildEntityClasses() {
		return new Class<?>[] { ProjectEntity.class };
	}

	/**
	 * Builds the impl services.
	 *
	 * @return the class[]
	 */
	@Override
	public Class<?>[] buildImplServices() {
		return new Class<?>[] { InstanceLinkExpressionEvaluator.class,
				UserLinkExpressionEvaluator.class, ProjectDefinitionManagementServiceImpl.class,
				ProjectServiceImpl.class, DbQueryTemplatesPm.class, PmKryoInitializer.class,
				PmInstanceToLinkSourceConverterProvider.class };
	}

	/**
	 * Builds the state extensions services.
	 *
	 * @return the class[]
	 */
	@Override
	public Class<?>[] buildStateExtensionsServices() {
		return new Class<?>[] { ProjectStateServiceExtension.class };
	}

	/**
	 * Builds the permission services.
	 *
	 * @return the class[]
	 */
	@Override
	public Class<?>[] buildPermissionServices() {
		return new Class<?>[] { PmRoleServiceImpl.class, PmActionProvider.class,
				PmCollectableRoleProviderExtension.class, PmRoleProviderExtension.class,
				PmGlobalRoleEvaluator.class, ProjectRoleEvaluator.class };
	}

	/**
	 * Builds the jax b definitions.
	 *
	 * @return the class[]
	 */
	@Override
	public Class<?>[] buildJaxBDefinitions() {
		return new Class<?>[] { ObjectFactory.class, ProjectDefinition.class };
	}

	/**
	 * Builds the dozer extensions.
	 *
	 * @return the class[]
	 */
	@Override
	public Class<?>[] buildDozerExtensions() {
		return new Class<?>[] { PmDozerMappingProvider.class };
	}

	/**
	 * Builds the model casses.
	 *
	 * @return the class[]
	 */
	@Override
	public Class<?>[] buildModelCasses() {
		return EMTPTY_ARRAY;
	}

	/**
	 * Builds the utility classes.
	 *
	 * @return the class[]
	 */
	@Override
	public Class<?>[] buildUtilityClasses() {
		return new Class<?>[] { PmEntityTypeProviderExtension.class,
				ProjectInstanceToServiceRegisterExtension.class };
	}

	/**
	 * Builds the xml classes.
	 *
	 * @return the class[]
	 */
	@Override
	public Class<?>[] buildXmlClasses() {
		return new Class<?>[] { PmSchemaBuilder.class };
	}

	/**
	 * Gets the deployment war name.
	 *
	 * @return the deployment war name
	 */
	@Override
	protected String getDeploymentWarName() {
		return "pm-test-deployment.war";
	}

	/**
	 * Gets the base jar name. Should be in format groupId-artifactId to match peristance.xml
	 *
	 * @return the base jar name
	 */
	@Override
	protected String getBaseJarName() {
		return "com.sirma.itt.sep.pm-tests.jar";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<?>[] buildCacheClasses() {
		return EMTPTY_ARRAY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<?>[] buildConfigClasses() {
		return new Class<?>[] { PMConfigProperties.class };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<?>[] buildDefinitionCallbacks() {
		return new Class<?>[] { ProjectDefinitionCompilerCallback.class };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<?>[] buildDefinitions() {
		return new Class<?>[] { ProjectDefinitionImpl.class };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WebArchive createDeploymentContainer() {
		WebArchive webArchive = ShrinkWrap.create(WebArchive.class, getDeploymentWarName());
		if (testableJar == null) {
			testableJar = createBaseArchive();
		}
		webArchive.addAsLibrary(testableJar);
		importLibraries(webArchive, dependencies);
		webArchive = webArchive
				.addAsResource(this.getClass().getResource("hibernate.cfg.xml"),
						"hibernate.cfg.xml")
				.addAsResource(this.getClass().getResource("persistence.xml"),
						"META-INF/persistence.xml")
				.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
		return webArchive;
	}

	/**
	 * {@inheritDoc} <br>
	 * Extracts the dependencies from the provided as initialization argument pom. On exit libraries
	 * are filtered out
	 */
	@Override
	protected Map<File, MavenResolvedArtifact> getLibrariesFromDeployment() {
		try {
			File createTempFile = File.createTempFile("" + System.currentTimeMillis(), ".xml");
			try (FileOutputStream stream = new FileOutputStream(createTempFile)) {
				IOUtils.copy(PmTestResourcePackager.class.getResourceAsStream(mavenModule), stream);
			} catch (Exception e) {
				logger.error(e);
			}
			PomEquippedResolveStage loadPomFromFile = resolver.offline().loadPomFromFile(
					createTempFile);
			MavenResolvedArtifact[] asResolvedArtifact = loadPomFromFile
					.importRuntimeAndTestDependencies().resolve().withTransitivity()
					.asResolvedArtifact();

			Map<File, MavenResolvedArtifact> libs = loadFromArtifacts(asResolvedArtifact,
					loadPomFromFile, null);

			return filterOutputLibraries(libs);
		} catch (Exception e) {
			logger.error(e);
			throw new RuntimeException(e);
		}
	}
}
