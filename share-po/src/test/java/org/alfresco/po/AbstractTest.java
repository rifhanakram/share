/*
 * #%L
 * share-po
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.po;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import javax.imageio.ImageIO;

import org.alfresco.dataprep.UserService;
import org.alfresco.po.exception.PageRenderTimeException;
import org.alfresco.po.share.DashBoardPage;
import org.alfresco.po.share.FactoryPage;
import org.alfresco.po.share.SharePage;
import org.alfresco.po.share.ShareUtil;
import org.alfresco.po.share.cmm.steps.CmmActions;
import org.alfresco.po.share.dashlet.FactoryShareDashlet;
import org.alfresco.po.share.enums.UserRole;
import org.alfresco.po.share.site.AddUsersToSitePage;
import org.alfresco.po.share.site.SiteDashboardPage;
import org.alfresco.po.share.site.SiteFinderPage;
import org.alfresco.po.share.site.UploadFilePage;
import org.alfresco.po.share.site.document.DocumentLibraryPage;
import org.alfresco.po.share.site.document.UserProfile;
import org.alfresco.po.share.steps.AdminActions;
import org.alfresco.po.share.steps.SiteActions;
import org.alfresco.po.share.steps.UserProfileActions;
import org.alfresco.po.share.util.SiteUtil;
import org.alfresco.po.share.workflow.MyWorkFlowsPage;
import org.alfresco.selenium.FetchUtil;
import org.alfresco.test.AlfrescoTests;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.Augmenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
@ContextConfiguration("classpath*:share-po-test-context.xml")
@PropertySources({
    @PropertySource("classpath:test.properties"),
    @PropertySource("classpath:module.properties"),
    @PropertySource("classpath:cmm.properties")
})
/**
 * Abstract test holds all common methods and information required
 * to run share page object tests.
 *
 * @author Michael Suzuki
 */
public abstract class AbstractTest extends AbstractTestNGSpringContextTests implements AlfrescoTests
{
    private static Log logger = LogFactory.getLog(AbstractTest.class);
    @Autowired private ApplicationContext ctx;
    @Value("${share.url}")protected String shareUrl;
    @Value("${share.license}")protected String licenseShare;
    @Value("${download.directory}")protected String downloadDirectory;
    @Value("${test.password}") protected String password;
    @Value("${test.username}") protected String username;
    @Value("${blog.url}") protected String blogUrl;
    @Value("${blog.username}") protected String blogUsername;
    @Value("${blog.password}") protected String blogPassword;
    @Value("${render.error.popup.time}") protected long popupRendertime;
    @Value("${share.version}") protected String alfrescoVersion;
    @Value("${render.page.wait.time}") protected long maxPageWaitTime;
    @Autowired protected UserProfile anotherUser;
    @Autowired protected FactoryPage factoryPage;
    @Autowired protected FactoryShareDashlet dashletFactory;
    @Autowired protected ShareUtil shareUtil;
    @Autowired protected SiteUtil siteUtil;
    @Autowired protected UserService userService;
    @Autowired protected CmmActions cmmActions;
    @Autowired protected SiteActions siteActions;
    @Autowired protected AdminActions adminActions;
    @Autowired protected UserProfileActions userActions;
    
    public static Integer retrySearchCount = 3;
    protected WebDriver driver;
    protected static final String UNAME_PASSWORD = "password";
    

    @BeforeClass(alwaysRun = true)
    public void getWebDriver() throws Exception
    {
        driver = (WebDriver) ctx.getBean("webDriver");
        driver.manage().window().maximize();
    }

    @AfterClass(alwaysRun = true)
    public void closeWebDriver()
    {
        if (logger.isTraceEnabled())
        {
            logger.trace("Closing web driver");
        }
        // Close the browser
        if (driver != null)
        {
            driver.quit();
            driver = null;
        }
    }

    /**
     * Helper to log admin user into dashboard.
     *
     * @return DashBoardPage page object.
     * @throws Exception if error
     */
    public DashBoardPage loginAs(final String... userInfo) throws Exception
    {
        return shareUtil.loginAs(driver, shareUrl, userInfo).render();
    }

    /**
     * Helper to log admin user into dashboard.
     *
     * @return DashBoardPage page object.
     * @throws Exception if error
     */
    public DashBoardPage loginAs(WebDriver driver, String shareUrl, final String... userInfo) throws Exception
    {
        return shareUtil.loginAs(driver, shareUrl, userInfo).render();
    }

    public void saveOsScreenShot(String methodName) throws IOException, AWTException
    {
        Robot robot = new Robot();
        BufferedImage screenShot = robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
        ImageIO.write(screenShot, "png", new File("target/webdriver-" + methodName+ "_OS" +".png"));
    }
    /**
     * Grabs a screen shot of what the {@link WebDriver} is currently viewing. This is only possible on WebDriver that are UI based browser.
     * 
     * @return {@link File} screen image of the page
     */
    public final File getScreenShot()
    {
        if(driver instanceof TakesScreenshot) 
        {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        }
        WebDriver augmentedDriver = new Augmenter().augment(driver);
        return ((TakesScreenshot) augmentedDriver).getScreenshotAs(OutputType.FILE);
    }
    public void saveScreenShot(String methodName) throws IOException
    {
        if(StringUtils.isEmpty(methodName))
        {
            throw new IllegalArgumentException("Method Name can't be empty or null.");
        }
        File file = getScreenShot();
        File tmp = new File("target/webdriver-" + methodName + ".png");
        FileUtils.copyFile(file, tmp);
    }

    public void savePageSource(String methodName) throws IOException
    {
        FetchUtil.save(driver, methodName + ".html");
    }

    @BeforeMethod(alwaysRun = true)
    protected void startSession(Method method) throws Exception
    {
        String testName = method.getName();
        if(logger.isTraceEnabled())
        {
            logger.trace(String.format("Test run:%s.%s",
                                        method.getDeclaringClass().getCanonicalName(),
                                        testName));
        }
    }

    /**
     * User Log out using logout URL Assumes User is logged in.
     *
     * @param driver WebDriver Instance
     */
    public void logout(WebDriver driver)
    {
        if(driver != null)
        {
            try
            {
                if (driver.getCurrentUrl().contains(shareUrl.trim()))
                {
                    shareUtil.logout(driver);
                    if(logger.isTraceEnabled())
                    {
                        logger.trace("Logout");
                    }
                }
            }
            catch (Exception e)
            {
                // Already logged out.
            }
        }
    }

    /**
     * Function to create user on Enterprise using UI
     *
     * @param uname - This should always be unique. So the user of this method needs to verify it is unique.
     *                eg. - "testUser" + System.currentTimeMillis();
     * @return
     * @throws Exception
     */
    public void createEnterpriseUser(String uname) throws Exception
    {
        userService.create(username, password, uname, "password", uname + "@test.com", uname, uname);
    }


    /**
     * Utility method to open site document library from search
     * @param driver
     * @param siteName
     * @return
     */
    protected DocumentLibraryPage openSiteDocumentLibraryFromSearch(WebDriver driver, String siteName)
    {
        SharePage sharePage = factoryPage.getPage(driver).render();
        SiteFinderPage siteFinderPage = sharePage.getNav().selectSearchForSites().render();
        siteFinderPage.searchForSite(siteName).render();
        siteFinderPage = siteUtil.siteSearchRetry(driver, siteFinderPage, siteName);
        SiteDashboardPage siteDashboardPage = siteFinderPage.selectSite(siteName).render();
        DocumentLibraryPage documentLibPage = siteDashboardPage.getSiteNav().selectDocumentLibrary().render();
        return documentLibPage;
    }

    /**
     * Method to Cancel a WorkFlow or Delete a WorkFlow (To use in TearDown method)
     * @param workFlow
     */
    protected void cancelWorkFlow(String workFlow)
    {
        SharePage sharePage = factoryPage.getPage(driver).render();
        MyWorkFlowsPage myWorkFlowsPage = sharePage.getNav().selectWorkFlowsIHaveStarted().render();
        myWorkFlowsPage.render();
        if(myWorkFlowsPage.isWorkFlowPresent(workFlow))
        {
            myWorkFlowsPage.cancelWorkFlow(workFlow);
        }
        myWorkFlowsPage = myWorkFlowsPage.selectCompletedWorkFlows().render();
        if(myWorkFlowsPage.isWorkFlowPresent(workFlow))
        {
            myWorkFlowsPage.deleteWorkFlow(workFlow);
        }
    }

    /**
     * Method to upload a file from given path. Method assumes that user is already in Document Library Page
     * @param driver
     * @param filePath
     * @return
     */
    public DocumentLibraryPage uploadContent(WebDriver driver, String filePath)
    {
        DocumentLibraryPage documentLibraryPage = factoryPage.getPage(driver).render();
        UploadFilePage uploadForm = documentLibraryPage.getNavigation().selectFileUpload().render();
        return uploadForm.uploadFile(filePath).render();
    }

    protected HtmlPage resolvePage(WebDriver driver)
    {
        return factoryPage.getPage(driver);
    }
    
    /**
     * Method to add user to the site
     * 
     * @param addUsersToSitePage
     * @param userName
     * @param role
     * @throws Exception
     */
    protected void addUsersToSite(AddUsersToSitePage addUsersToSitePage, String userName, UserRole role) throws Exception
    {
        int counter = 0;
        int waitInMilliSeconds = 2000;
        List<String> searchUsers = null;
        while (counter < retrySearchCount + 8)
        {
            searchUsers = addUsersToSitePage.searchUser(userName);
            if (searchUsers != null && searchUsers.size() > 0 && hasUser(searchUsers, userName))
            {
                addUsersToSitePage.clickSelectUser(userName);
                addUsersToSitePage.setUserRoles(userName, role);
                addUsersToSitePage.clickAddUsersButton();
                break;
            }
            else
            {
                counter++;
                factoryPage.getPage(driver).render();
            }
            // double wait time to not over do solr search
            waitInMilliSeconds = (waitInMilliSeconds * 2);
            synchronized (this)
            {
                try
                {
                    this.wait(waitInMilliSeconds);
                }
                catch (InterruptedException e)
                {
                }
            }
        }
        try
        {
            addUsersToSitePage.renderWithUserSearchResults(maxPageWaitTime);
        }
        catch (PageRenderTimeException exception)
        {
            saveScreenShot("SiteTest.instantiateMembers-error");
            throw new Exception("Waiting for object to load", exception);

        }        
    }
    
    
    /**
     * Returns true if the search user list contains created user
     * 
     * @param searchUsers
     * @param userName
     * @return
     */
    protected boolean hasUser(List<String> searchUsers, String userName)
    {
        boolean hasUser = false;
        for(String searchUser : searchUsers)
        {
            if(searchUser.indexOf(userName) != -1)
            {
                hasUser = true;
            }
        }
        return hasUser;
    }
    
}
