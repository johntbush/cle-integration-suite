package com.rsmart.sakai;


import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkOptionsSystemProperties;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.carrotsearch.junitbenchmarks.annotation.AxisRange;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkHistoryChart;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkMethodChart;
import com.carrotsearch.junitbenchmarks.annotation.LabelType;
import com.github.javafaker.Faker;
import org.apache.commons.lang.StringUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

import org.junit.*;

import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.search.api.*;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: jbush
 * Date: 2/5/13
 * Time: 3:43 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(Arquillian.class)
public class SearchTest extends AbstractBenchmark {
    protected UserDirectoryService uds;
    protected SiteService siteService;
    protected Faker faker = new Faker();
    protected Random randomGenerator = new Random();
    protected ContentHostingService chs;
    protected SessionManager sessionManager;
    protected SecurityService securityService;
    protected SearchService searchService;
    protected SqlService sqlService;
    protected SearchIndexBuilder searchIndexBuilder;
    AuthzGroupService authzGroupService;
    static final String NUM_NODES = "4";
    static final String REPO_SIZE = "10";
    static final String SEARCH_IMPL = "ElasticSearch";
    static StringBuffer buffer = new StringBuffer();

    @Before
    public void setUp() throws Exception {

        siteService = (SiteService) ComponentManager.get(SiteService.class);
        uds = (UserDirectoryService) ComponentManager.get(UserDirectoryService.class);
        chs = (ContentHostingService) ComponentManager.get(ContentHostingService.class);
        sessionManager = (SessionManager) ComponentManager.get(SessionManager.class);
        securityService = (SecurityService) ComponentManager.get(SecurityService.class);
        searchService = (SearchService) ComponentManager.get(SearchService.class);
        sqlService = (SqlService) ComponentManager.get(SqlService.class);
        searchIndexBuilder = (SearchIndexBuilder) ComponentManager.get(SearchIndexBuilder.class);
        authzGroupService = org.sakaiproject.authz.cover.AuthzGroupService.getInstance();

        //Session sakaiSession = sessionManager.getCurrentSession();
        //sakaiSession.setUserId("admin");
        //sakaiSession.setUserEid("admin");

        //SuperUserSecurityAdvisor securityAdvisor = new SuperUserSecurityAdvisor();
        //securityAdvisor.setSuperUser("admin");
        //securityService.pushAdvisor(securityAdvisor);
    }

    public class SuperUserSecurityAdvisor implements SecurityAdvisor {
        private String superUser;

        public SecurityAdvice isAllowed(String userId, String function, String reference) {
            if (userId != null && userId.equals(superUser)) {
                return SecurityAdvice.ALLOWED;
            }
            return SecurityAdvice.PASS;
        }

        public String getSuperUser() {
            return superUser;
        }

        public void setSuperUser(String superUser) {
            this.superUser = superUser;
        }
    }

    @BenchmarkOptions(benchmarkRounds = 10, warmupRounds = 1)
    @Test
    public void testSearchOneSite() {
        testSearchSites(false);
    }

    @BenchmarkOptions(benchmarkRounds = 10, warmupRounds = 1)
    @Test
    public void testSearchAllSites() {
       testSearchSites(true);
    }

    protected void log(String[] data) {
        log(StringUtils.join(data, ","));
    }

    @After
    public void writeLog() {
        System.out.println(buffer.toString());
    }

    protected void log(String data) {
        buffer.append(data + "\n");
       /* PrintWriter out = null;
        try {

            File file = new File("/files/search_results.csv");

            boolean newFile = false;

            //if file doesn't exists, then create it
            if (!file.exists()) {
                file.createNewFile();
                newFile = true;
            }

            out = new PrintWriter(new BufferedWriter(new FileWriter(file.getName(), true)));
            if (newFile) {
                out.println("# number of sites, search term, hits, time (ms), num nodes, repo size, search impl");
            }
            out.println(data);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (Exception e) {
            }
        }*/
    }

    protected void testSearchSites(boolean all) {
        try {
            List<Site> allSites = new ArrayList();
            while (allSites.size() < 1) {
                User user = getRandomUser();
                Session sakaiSession = sessionManager.getCurrentSession();
                sakaiSession.setUserId(user.getId());
                sakaiSession.setUserEid(user.getEid());
                authzGroupService.refreshUser(user.getId());
                allSites = siteService.getSites(SiteService.SelectionType.ACCESS, null, null,
                        null, SiteService.SortType.TITLE_ASC, null);
            }
     //       sakaiSession.setUserId("admin");
     //       sakaiSession.setUserEid("admin");
            //searchIndexBuilder.refreshIndex();
            String searchTerm = getSearchTerm();
            long startTime = System.currentTimeMillis();
            SearchList results = null;
            List<String> sites = new ArrayList();
            if (all) {
                sites = getSiteIdsForSites(allSites);
            } else {
                sites.add(getSiteIdsForSites(allSites).get(0));
            }
            results = searchService.search(searchTerm, sites, 0, 10);

            log(new String[]{String.valueOf(sites.size()), searchTerm, String.valueOf(results.getFullSize()), String.valueOf(System.currentTimeMillis() - startTime), NUM_NODES, REPO_SIZE, SEARCH_IMPL});
            System.out.println("search for: " + searchTerm + " took " + (System.currentTimeMillis() - startTime) + " ms");


        } catch (Exception e) {
            e.printStackTrace();
            //throw new RuntimeException(e.getMessage(), e);
        }
    }



    private List<String> getSiteIdsForSites(List<Site> allSites) {
        List<String> siteIds = new ArrayList<String>();
        for (Site site : allSites) {
            siteIds.add(site.getId());
        }
        return siteIds;
    }

    private User getRandomUser() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        try {
            con = sqlService.borrowConnection();
            ps = con.prepareStatement("SELECT user_id FROM sakai_user_id_map ORDER BY RAND() LIMIT 1");
            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                String userId = resultSet.getString(1);
                return uds.getUser(userId);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                }
            }
        }
        return null;
    }

    @Deployment
    public static Archive createDeployment() {
        //   return ShrinkWrap.create(JavaArchive.class).addClass(Faker.class);
        //MavenDependencyResolver resolver = DependencyResolvers.use(MavenDependencyResolver.class).includeDependenciesFromPom("pom.xml");
        MavenDependencyResolver resolver = DependencyResolvers.use(MavenDependencyResolver.class).loadMetadataFromPom("pom.xml");
        return ShrinkWrap.create(WebArchive.class, "test.war")
                .addClass(Faker.class)
                .addAsResource("en.yml")
                .addAsResource("words.txt")
                .addAsLibraries(resolver.artifact("com.carrotsearch:junit-benchmarks").resolveAsFiles())
                .addAsLibraries(resolver.artifact("com.h2database:h2").resolveAsFiles())
                .addAsLibraries(resolver.artifact("org.jyaml:jyaml").resolveAsFiles());
    }

    private List<String> getRandomSitesForUser(User user) {
        List sites = new ArrayList();
        sites.add("32003504886394898");
        return sites;
    }

    private String getSearchTerm() {
       return faker.words(1).get(0);
    /*    int length = randomGenerator.nextInt(word.length());
        if (length < 2) {
            length = 2;
        }
        if (length >= word.length()) {
            length--;
        }
        System.out.println("searching for: " + word.substring(0, length));
        return word.substring(0, length);*/
    }


}
