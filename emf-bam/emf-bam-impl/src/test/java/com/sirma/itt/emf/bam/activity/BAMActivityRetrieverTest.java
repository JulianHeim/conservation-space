/**
 *
 */
package com.sirma.itt.emf.bam.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.sirma.itt.emf.time.DateRange;

/**
 * Test class for the implementation of {@link BAMActivityRetriever}.
 * 
 * @author Mihail Radkov
 */
public class BAMActivityRetrieverTest {

	// private JdbcDataSource dataSource;

	/** Connection to a H2 database used in the tests. */
	private Connection connection;

	/** The tested implementation. */
	private BAMActivityRetrieverImpl activityRetriever;

	/**
	 * Executed before every test. Sets up the connection to H2.
	 * 
	 * @throws SQLException
	 */
	@Before
	public void before() throws SQLException {
		activityRetriever = new BAMActivityRetrieverImpl();
		connection = DriverManager.getConnection("jdbc:h2:mem:mytest");
	}

	/**
	 * Executed after every test. Closes the connection to H2.
	 * 
	 * @throws SQLException
	 */
	@After
	public void after() throws SQLException {
		connection.close();
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.emf.bam.activity.BAMActivityRetrieverImpl#obtainConnection(javax.sql.DataSource)}
	 * .
	 * 
	 * @throws SQLException
	 */
	@Test
	public void testObtainConnection() throws SQLException {
		// Null test.
		assertNull(activityRetriever.obtainConnection(null));
		// Existing data source test.
		assertNotNull(activityRetriever.obtainConnection(prepareDataSource()));
	}

	/**
	 * Creates a JDBC data source pointing to a H2 DB.
	 * 
	 * @return a new data source
	 */
	public DataSource prepareDataSource() {
		JdbcDataSource ds = new JdbcDataSource();
		ds.setURL("jdbc:h2:mem:mytest2");
		return ds;
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.emf.bam.activity.BAMActivityRetrieverImpl#getActivities(java.sql.ResultSet)}
	 * .
	 */
	@Test
	@Ignore
	public void testGetActivities() {
		// Null test.
		assertTrue(activityRetriever.buildActivities(null, null).isEmpty());

		try {
			prepareDB(connection);
			prepareWrongDB(connection);
		} catch (SQLException e) {
			e.printStackTrace();
			fail();
		}
		// Correct test.
		String query = "select objectid, objecttitle, username, action,projectid,caseid,objectsubtype from emf_events";
		List<BAMActivity> results = activityRetriever.buildActivities(connection, query);

		assertEquals(1, results.size());
		BAMActivity activity = results.get(0);

		assertEquals("test2", activity.getAction());
		assertEquals("test3", activity.getId());
		assertEquals("test4", activity.getTitle());
		assertEquals("test5", activity.getProjectid());
		assertEquals("test6", activity.getCaseid());
		assertEquals("test7", activity.getUsername());
		assertEquals("test8", activity.getObjectsubtype());

		// Incorrect test.
		query = "select * from emf_events2";
		results = activityRetriever.buildActivities(connection, query);
		assertEquals(0, results.size());
	}

	/**
	 * Creates a correct table with one correct record.
	 * 
	 * @param conn
	 *            a connection used for executing queries
	 * @throws SQLException
	 *             if problem occurs while working with the connection
	 */
	public void prepareDB(Connection conn) throws SQLException {
		String table = "create table emf_events (eventdate varchar(255) primary key, action varchar(255), objectid varchar(255), objecttitle varchar(255), projectid varchar(255), caseid varchar(255), username varchar(255),objectsubtype varchar(255)) ";
		conn.prepareStatement(table).execute();
		String insert = "insert into emf_events values ('test1','test2','test3','test4','test5','test6','test7','test8')";
		conn.prepareStatement(insert).execute();
	}

	/**
	 * Creates a incorrect table with one incorrect record.
	 * 
	 * @param conn
	 *            a connection used for executing queries
	 * @throws SQLException
	 *             if problem occurs while working with the connection
	 */
	public void prepareWrongDB(Connection conn) throws SQLException {
		String table = "create table emf_events2 (eventdate varchar(255) primary key, action varchar(255))";
		conn.prepareStatement(table).execute();
		String insert = "insert into emf_events2 values ('test1','test2')";
		conn.prepareStatement(insert).execute();
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.emf.bam.activity.BAMActivityRetrieverImpl#constructCriteriaQuery(BAMActivityCriteria)}
	 * .
	 */
	@Test
	public void testConstructCriteriaQuery() {
		BAMActivityCriteria criteria = new BAMActivityCriteria();
		criteria.setDateRange(new DateRange(new Date(), new Date()));
		criteria.setIncludedUsername("testname");
		criteria.setCriteriaType(CriteriaType.PROJECT);
		criteria.addId("12");
		String query = activityRetriever.constructCriteriaQuery(criteria);

		assertTrue(query.contains("( projectid='\"12\"' )"));
		assertTrue(query.startsWith("Select "));
		assertTrue(query.endsWith(";"));
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.emf.bam.activity.BAMActivityRetrieverImpl#constructDateRangeCriteria(StringBuilder, DateRange)}
	 * .
	 */
	@Test
	public void testConstructDateRange() {
		StringBuilder builder = new StringBuilder();
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		Date d1 = new Date(123456789);
		Date d2 = new Date(987654321);
		String sd1 = dateFormat.format(d1);
		String sd2 = dateFormat.format(d2);
		DateRange range = new DateRange(d1, d2);

		// Null checks
		activityRetriever.constructDateRangeCriteria(null, null);
		activityRetriever.constructDateRangeCriteria(null, range);
		activityRetriever.constructDateRangeCriteria(builder, null);
		assertTrue(builder.toString().isEmpty());
		// Normal test
		activityRetriever.constructDateRangeCriteria(builder, range);
		assertTrue(builder.toString().contains("replace(eventdate,'\"','')::bigint>=" + sd1));
		assertTrue(builder.toString().contains("replace(eventdate,'\"','')::bigint<=" + sd2));
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.emf.bam.activity.BAMActivityRetrieverImpl#constructIds(StringBuilder, List, CriteriaType)}
	 * .
	 */
	@Test
	public void testConstructIds() {
		StringBuilder builder = new StringBuilder();
		List<String> ids = new ArrayList<String>();
		CriteriaType critType = CriteriaType.CASE;
		ids.add("12");
		ids.add("14");

		// Null checks.
		activityRetriever.constructIds(null, null, null);
		activityRetriever.constructIds(builder, null, null);
		activityRetriever.constructIds(null, ids, null);
		activityRetriever.constructIds(builder, ids, null);
		assertTrue(builder.toString().isEmpty());
		// Normal test.
		activityRetriever.constructIds(builder, ids, critType);
		assertTrue(builder.toString().contains("( caseid='\"12\"' or caseid='\"14\"' )"));
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.emf.bam.activity.BAMActivityRetrieverImpl#constructUser(StringBuilder, String, boolean)}
	 * .
	 */
	@Test
	public void testConstructUser() {
		StringBuilder builder = new StringBuilder();
		String user = "testuser";

		// Null checks
		activityRetriever.constructUser(null, null, true);
		activityRetriever.constructUser(builder, null, true);
		activityRetriever.constructUser(null, user, true);
		assertTrue(builder.toString().isEmpty());
		// Normal test
		activityRetriever.constructUser(builder, user, true);
		assertTrue(builder.toString().contains("and username='\"testuser\"'"));
		builder.delete(0, builder.length() - 1);
		activityRetriever.constructUser(builder, user, false);
		assertTrue(builder.toString().contains("and username!='\"testuser\"'"));
	}

	/**
	 * Test method for
	 * {@link com.sirma.itt.emf.bam.activity.BAMActivityRetrieverImpl#removeQuotes(String)} .
	 */
	@Test
	public void testRemoveQuotes() {
		String result = activityRetriever.removeQuotes(null);
		assertNull("", result);
		result = activityRetriever.removeQuotes("");
		assertEquals("", result);
		result = activityRetriever.removeQuotes("1");
		assertEquals("1", result);
		result = activityRetriever.removeQuotes("\"1");
		assertEquals("1", result);
		result = activityRetriever.removeQuotes("\"1\"");
		assertEquals("1", result);
		result = activityRetriever.removeQuotes("\"22\"");
		assertEquals("22", result);
		result = activityRetriever.removeQuotes("\"test\"");
		assertEquals("test", result);
		result = activityRetriever.removeQuotes("\"very long test string with \" inside of it\"\"");
		assertEquals("very long test string with \" inside of it\"", result);
	}
}
