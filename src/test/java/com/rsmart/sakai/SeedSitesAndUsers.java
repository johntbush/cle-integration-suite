package com.rsmart.sakai;

import com.github.javafaker.Faker;
import org.apache.commons.lang.StringUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.content.api.ContentCollectionEdit;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.exception.*;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: jbush
 * Date: 2/5/13
 * Time: 9:46 AM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(Arquillian.class)
public class SeedSitesAndUsers {
    private int NUM_SITES = 100;
    private int NUM_USERS = 1000;
    private int NUM_ENROLLMENTS_PER_SITE = 50;
    private int REPOSITORY_SIZE = 1573741824; // 1 GB
    //private long REPOSITORY_SIZE = 10737418240L; // 10 GB
    //private long REPOSITORY_SIZE = 21474836480L; // 20 GB
    //private long REPOSITORY_SIZE = 42949672960L; // 40 GB
    //private long REPOSITORY_SIZE = 64424509440L; // 60 GB

    private UserDirectoryService uds;
    private SiteService siteService;
    private Faker faker = new Faker();
    private Map<String, User> users = new HashMap();
    private Map<String, Site> sites = new HashMap();
    private Random randomGenerator = new Random();
    private ContentHostingService chs;
    private SessionManager sessionManager;
    private SecurityService securityService;
    private SqlService sqlService;

    @Before
    public void setUp() throws Exception {


        siteService = (SiteService) ComponentManager.get(SiteService.class);
        uds = (UserDirectoryService) ComponentManager.get(UserDirectoryService.class);
        chs = (ContentHostingService) ComponentManager.get(ContentHostingService.class);
        sessionManager = (SessionManager) ComponentManager.get(SessionManager.class);
        securityService = (SecurityService) ComponentManager.get(SecurityService.class);
        sqlService = (SqlService) ComponentManager.get(SqlService.class);

        Session sakaiSession = sessionManager.getCurrentSession();
        sakaiSession.setUserId("admin");
        sakaiSession.setUserEid("admin");

        SuperUserSecurityAdvisor securityAdvisor = new SuperUserSecurityAdvisor();
        securityAdvisor.setSuperUser("admin");
        securityService.pushAdvisor(securityAdvisor);

        createSites();
        createUsers();
        createEnrollments();

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
               // .addAsLibraries(resolver.artifact("com.github.javafaker:javafaker").resolveAsFiles())
                .addAsLibraries(resolver.artifact("org.jyaml:jyaml").resolveAsFiles());
    }

    @Test
    public void seedData() {
        long totalBytes = getRepositorySize();
        while (totalBytes < REPOSITORY_SIZE) {
            System.out.println("Current repository size: " + totalBytes);
            Site site = getRandomSite();
            String collectionName = getCollectionName(site);
            try {
                chs.getCollection(collectionName);
            } catch (IdUnusedException e ){
                createCollection(collectionName);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }

            // 5k paragraphs works out to on average docs around 1/2 MB in size
            byte[] rawFile = StringUtils.join(faker.paragraphs(randomGenerator.nextInt(5000)), "\n\n").getBytes();

            try {
                String fileName = StringUtils.join(faker.words(4), " ") + " " + String.valueOf(randomGenerator.nextInt(5000));
                ContentResourceEdit resourceEdit = chs.addResource(collectionName + fileName + ".txt");
                ResourcePropertiesEdit props = resourceEdit.getPropertiesEdit();
                props.addProperty(ResourceProperties.PROP_DISPLAY_NAME, fileName);
                props.addProperty(ResourceProperties.PROP_DESCRIPTION, "created for testing");
                props.addProperty(ResourceProperties.PROP_PUBVIEW, "false");
                resourceEdit.setContent(rawFile);
                resourceEdit.setContentType("text/plain");
                chs.commitResource(resourceEdit);

            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }

            totalBytes += rawFile.length;
        }
    }

    private long getRepositorySize() {
        Connection con = null;
           PreparedStatement ps = null;
           ResultSet resultSet = null;
           try {
               con = sqlService.borrowConnection();
               ps = con.prepareStatement("select sum(file_size) from content_resource");
               resultSet = ps.executeQuery();
               if (resultSet.next()) {
                   return resultSet.getLong(1);
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
           return 0;

    }

    private void createCollection(String collectionName) {
        ContentCollectionEdit collectionEdit = null;
        try {
            collectionEdit = chs.addCollection(collectionName);
            collectionEdit.getPropertiesEdit().addProperty(ResourceProperties.PROP_DISPLAY_NAME, "searchdata");
            chs.commitCollection(collectionEdit);
        } catch (Exception e1) {
            throw new RuntimeException(e1.getMessage(), e1);
        }
    }

    private String getCollectionName(Site site) {
        return "/group/" + site.getId() + "/searchdata/";
    }


    public void createSites() {
        for (int i = 0; i < NUM_SITES; i++) {
            try {
                Site site = siteService.addSite(faker.numerify("#################"), "course");
                site.addPage().addTool("sakai.search");
                site.addPage().addTool("sakai.resources");
                site.addPage().addTool("sakai.siteinfo");
                site.setPublished(true);
                siteService.save(site);
                sites.put(site.getId(), site);
                System.out.print("created site: " + site.getId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void createUsers() {
        for (int i = 0; i < NUM_USERS; i++) {
            try {
                String lastName = faker.lastName();
                String eid = faker.numerify("########");
                User user = uds.addUser(null, eid, faker.firstName(), lastName, eid + "@nowhere.com", "registered", faker.letterify("???????"), null);
                users.put(eid, user);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void createEnrollments() {
        User[] userArray = users.values().toArray(new User[]{});
        for (String siteId : sites.keySet()) {
            Site site = sites.get(siteId);
            Set usersInSite = new HashSet();
            for (int i = 0; i < NUM_ENROLLMENTS_PER_SITE; i++) {
                User user = getUnAddedUser(usersInSite, userArray);
                site.addMember(user.getId(), "Student", true, false);
                try {
                    siteService.save(site);
                    usersInSite.add(user);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

     private Site getRandomSite() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;
        try {
            con = sqlService.borrowConnection();
            ps = con.prepareStatement("SELECT site_id FROM sakai_site ORDER BY RAND() LIMIT 1");
            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                String siteId = resultSet.getString(1);
                return siteService.getSite(siteId);
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

    private User getUnAddedUser(Set usersInSite, User[] userArray) {
        while (true) {
            User user = userArray[randomGenerator.nextInt(userArray.length - 1)];
            if (!usersInSite.contains(user)) {
                return user;
            }
        }
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
}
