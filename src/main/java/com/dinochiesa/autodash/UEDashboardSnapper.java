package com.dinochiesa.autodash;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import net.oneandone.sushi.util.NetRc;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class UEDashboardSnapper {
    private final String optString = "-P:vn"; // getopt style
    private Hashtable<String, Object> options = new Hashtable<String, Object> ();
    private WebDriver driver;

    public UEDashboardSnapper(String[] args) throws Exception {
        GetOpts(args, optString);
    }

    public static String nowFormatted(){
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd-HHmmss");
        Calendar c = new GregorianCalendar();
        fmt.setCalendar(c);
        return fmt.format(c.getTime());
    }

    private void login(String username, String password) throws java.lang.InterruptedException {
        driver.get("https://login.apigee.com/login");
        (new WebDriverWait(driver, 10))
            .until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@name='username']")));

        WebElement usernameBox = driver.findElement(By.xpath("//input[@name='username']"));
        usernameBox.sendKeys(username);
        WebElement passwordBox = driver.findElement(By.xpath("//input[@name='password']"));
        passwordBox.sendKeys(password);
        WebElement submitBtn = driver.findElement(By.xpath("//input[@type='submit']"));
        submitBtn.submit();
        Thread.sleep(2150);
        driver.get("https://apigee.com/edge");
        (new WebDriverWait(driver, 10)) // Wait for the page to load, timeout after 10 seconds
            .until(ExpectedConditions.visibilityOfElementLocated(By.id("user-ini")));
    }

    private void selectEnvironment(String env, String chart) throws java.lang.InterruptedException {
        // find the environment selector
        (new WebDriverWait(driver, 10))
            .until(ExpectedConditions.visibilityOfElementLocated(By.id("environmentSelector")));
        driver.findElement(By.id("environmentSelector")).click();
        Thread.sleep(850); // wait for animation to complete

        // select the correct environment
        String envSelectorPath = String.format("//div[@id='environmentSelector']/ul/li/a[normalize-space(text())='%s']", env);
        (new WebDriverWait(driver, 10))
            .until(ExpectedConditions.elementToBeClickable(By.xpath(envSelectorPath)));
        driver.findElement(By.xpath(envSelectorPath)).click();

        // wait for the chart legend to appear after selecting the new environment.
        String elementXpath = "//div[contains(@class,'ax-dashboard-element') and contains(@class,'top-element')]/div[contains(@class,'summary')]/div/div[contains(@class,'summary-title')]";
        (new WebDriverWait(driver, 10))
            .until(ExpectedConditions.elementToBeClickable(By.xpath(elementXpath)));
        Thread.sleep(3750); // give any warnings a chance to disappear

        switch (chart) {
            case "proxy" :

        // wait for the summary lines in the chart legend to be unhidden
        elementXpath = "//div[contains(@class,'ax-dashboard-element') and contains(@class,'top-element')]/div[contains(@class,'summary')]/div/div[contains(@class,'summary-repeater') and contains(@class, 'ng-scope') and not(contains(@class,'ng-hide'))]";
        (new WebDriverWait(driver, 10)) // Wait up to 10 seconds
            .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(elementXpath)));
        break;
            case "latency":
                break;
        }
    }

    private void selectProxy(String proxy) throws java.lang.InterruptedException {
        // find the proxy selector
        String elementXpath = "//div[contains(@class,'ax-toolbar-extension')]/div[contains(@class,'extension-element') and contains(@entities,'proxies')]/div[contains(@class,'btn-group')]";
        (new WebDriverWait(driver, 10))
            .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(elementXpath)));

        driver.findElement(By.xpath(elementXpath)).click();
        Thread.sleep(850); // wait for animation to complete

        // select the correct proxy
        elementXpath += String.format("/ul/li/a[normalize-space(text())='%s']", proxy);
        (new WebDriverWait(driver, 10))
            .until(ExpectedConditions.elementToBeClickable(By.xpath(elementXpath)));
        driver.findElement(By.xpath(elementXpath)).click();

        // wait for the chart legend to appear after selecting the new environment.
        elementXpath = "//div[contains(@class,'ax-dashboard-element') and contains(@class,'top-element')]/div[contains(@class,'summary')]/div/div[contains(@class,'summary-categories')]";
        (new WebDriverWait(driver, 10))
            .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(elementXpath)));
        Thread.sleep(2750); // give any warnings a chance to disappear

        elementXpath = "//div[contains(@class,'ax-dashboard-element') and contains(@class,'top-element')]/div[contains(@class,'summary')]/div/div[contains(@class,'summary-categories') and contains(@class, 'ng-isolate-scope')]/div[contains(@class,'scrollPanel')]/div/div[contains(@class,'faderContent')]/div[contains(@class,'category-repeater')]";
        (new WebDriverWait(driver, 10)) // Wait up to 10 seconds
            .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(elementXpath)));

        Thread.sleep(3750); // just for fun
    }

    public void visitAnalyticsProxyPerformancePage(String org) throws java.lang.InterruptedException {
            // String selectedOrgPath = "//div.navbar-head/div.account-info/div.avatar-userInfo/span.selectedOrg";
            // WebElement selectedOrgElement = driver.findElement(By.xpath(userInfoPath));
            // String selectedOrg = selectedOrgElement.getText();
            //
            // if(selectedOrg == null || ! selectedOrg.equals(org)) {
            //     // select the org
            //     driver.findElement(By.xpath(orgSelectorPath)).click();
            //     String orgItemPath = String.format("//div#orgpicker-scrollable/span.org-item[@data-id='%s']", org);
            //     (new WebDriverWait(driver, 10))
            //         .until(ExpectedConditions.elementToBeClickable(By.xpath(orgItemPath)));
            //     driver.findElement(By.xpath(orgItemPath)).click();
            // }
            //
            // String analyzeItemPath = "//ul#menu-list/li#analyze";
            // (new WebDriverWait(driver, 10))
            //     .until(ExpectedConditions.elementToBeClickable(By.xpath(analyzeItemPath)));
            //
            // driver.findElement(By.xpath(analyzeItemPath)).click();

        String orgAnalyticsPath = String.format("https://apigee.com/platform/%s/proxy-performance", org);
        driver.get(orgAnalyticsPath);
        collapseNavbar();
    }

    public void visitAnalyticsLatencyPage(String org) throws java.lang.InterruptedException {
        String orgAnalyticsPath = String.format("https://apigee.com/platform/%s/latency", org);
        driver.get(orgAnalyticsPath);
        collapseNavbar();
    }

    private void collapseNavbar() throws java.lang.InterruptedException {
        // collapse the navbar. The pin-button appears on hover. Use "moveToElement" to accomplish this.
        String xpath1 = "//ul[@id='menu-list']";
        WebElement element1 = driver.findElement(By.xpath(xpath1));
        if (element1 != null) {
            //System.out.printf("Found menu-list\n");
            Actions builder = new Actions(driver);
            builder.moveToElement(element1).perform();
            Thread.sleep(1250); // allow the animation to complete
            xpath1 = "//div[@id='navbar-pin-button']";
            element1 = driver.findElement(By.xpath(xpath1));
            if (element1 != null) {
                //System.out.printf("Found pin-button\n");
                element1.click();
                Thread.sleep(1050); // allow the animation to complete
            }
            else {
                System.out.printf("Cannot find pin-button\n");
            }
        }
        else {
            System.out.printf("Cannot find menu-list\n");
        }
    }

    private void selectWeekChart() throws java.lang.InterruptedException {
        // select the "Week" chart.
        String weekSelectorPath = "//div[contains(@class,'ax-toolbar')]/div[contains(@class,'date-range-container')]/div/div[contains(@class,'btn-group')]/button[normalize-space(text())='Week']";
        driver.findElement(By.xpath(weekSelectorPath)).click();
        Thread.sleep(1850); // the page will blank
    }

    private void postToSlack(String slackChannel, String slackToken, String snapshotFile) throws IOException {
        String curlCommandTemplate = "curl -i -F file=@%s -F filename=%s -F token=%s  https://slack.com/api/files.upload?channels=%s";
        String commandString = String.format(curlCommandTemplate,
                                             snapshotFile, snapshotFile, slackToken, slackChannel);
        if (getVerbose()) {
            String cleanCommandString = String.format(curlCommandTemplate,
                                                      snapshotFile, snapshotFile, "<TOKEN>", slackChannel);
            System.out.printf("%s\n", cleanCommandString);
        }

        Process p = Runtime.getRuntime().exec(commandString);
        if (getVerbose()) {
            BufferedReader in = new BufferedReader( new InputStreamReader( p.getInputStream()) );
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
            in.close();
        }
    }

    private String takeScreenShot(String org, String chart, String qualifier, boolean wantZoom) throws IOException, InterruptedException {
        if (wantZoom)
            zoomOut();
        File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
        String snapshotFilename = String.format("/tmp/%s-%s-%s-%s.png", org, qualifier, chart, nowFormatted());
        FileUtils.copyFile(scrFile, new File(snapshotFilename));
        System.out.printf("\nscreenshot: %s\n", snapshotFilename);
        if (wantZoom)
            zoomIn();
        return snapshotFilename;
    }

    private void startChrome(String windowSize) {
        ChromeOptions options = new ChromeOptions();
        //options.addArguments("--start-maximized"); // no worky for me
        //options.addArguments("--window-size=1920,1080");

        if (windowSize == null) {
            windowSize = "2480,1400";
        }
        options.addArguments(String.format("--window-size=%s", windowSize));
        driver = new ChromeDriver( options );
    }

    private void zoomOut() throws InterruptedException {
        JavascriptExecutor executor = (JavascriptExecutor)driver;
        executor.executeScript("document.body.style.zoom = '0.8'");
        Thread.sleep(1850); // the page will blank

        // WebElement html = driver.findElement(By.tagName("html"));
        // for(int i=0; i< factor; i++) {
        //     html.sendKeys(Keys.chord(Keys.COMMAND, Keys.ADD));
        // }
    }
    private void zoomIn() throws InterruptedException {
        JavascriptExecutor executor = (JavascriptExecutor)driver;
        executor.executeScript("document.body.style.zoom = '1.0'");
        Thread.sleep(1850); // the page will blank

        // WebElement html = driver.findElement(By.tagName("html"));
        // for(int i=0; i< factor; i++) {
        //     html.sendKeys(Keys.chord(Keys.COMMAND, Keys.ADD));
        // }
    }

    // private void zoomIn(int factor) {
    //     WebElement html = driver.findElement(By.tagName("html"));
    //     for(int i=0; i< factor; i++) {
    //         html.sendKeys(Keys.chord(Keys.COMMAND, Keys.SUBTRACT));
    //     }
    // }

    public void snapAndMaybePostCharts(Properties props) throws Exception {

        List<String> snapshotFiles = new ArrayList<String>();
        if (getVerbose()) {
            System.out.printf("[%s] snapAndMaybePostCharts", nowFormatted());
        }

        String slackChannel = (String)props.get("slackChannel");
        String slackToken = (String)props.get("slackToken");
        boolean wantZoom = (props.get("wantZoom") != null) &&  (((String)props.get("wantZoom")).toLowerCase() == "true");

        try {
            startChrome((String)props.get("windowSize"));
            login((String)props.get("username"), (String)props.get("password"));
            //driver.manage().window().maximize();

            String org = (String)props.get("org");
            String chart = (String)props.get("chart");
            if (chart == null) {
                chart = "proxy";
            }

            switch (chart) {
                case "proxy" :
                    visitAnalyticsProxyPerformancePage(org);
                    selectWeekChart();

                    String envlist = (String)props.get("environments");
                    for(String env : envlist.split(",")) {
                        selectEnvironment(env,chart);
                        String snapshotFilename = takeScreenShot(org,chart,env, wantZoom);
                        snapshotFiles.add(snapshotFilename);
                        if (slackChannel != null && slackToken != null) {
                            postToSlack(slackChannel, slackToken, snapshotFilename);
                        }
                    }
                    break;
                case "latency" :
                    visitAnalyticsLatencyPage(org);
                    selectWeekChart();

                    String proxylist = (String)props.get("proxies");
                    for(String envProxyPair: proxylist.split(",")) {
                        String[] parts = envProxyPair.split(":");
                        selectEnvironment(parts[0],chart);
                        selectProxy(parts[1]);
                        String snapshotFilename = takeScreenShot(org,chart,envProxyPair.replaceAll(":","-"), wantZoom);
                        snapshotFiles.add(snapshotFilename);
                        if (slackChannel != null && slackToken != null) {
                            postToSlack(slackChannel, slackToken, snapshotFilename);
                        }
                    }
                    break;
                default :
                    throw new Exception("unknown chart");
            }
        }
        finally {
            driver.quit();
            // remove files only if they have been posted to slack
            if (slackChannel != null && slackToken != null) {
                for (String filename : snapshotFiles) {
                    File file = new File(filename);
                    if(file.exists())
                        file.delete();
                }
            }
        }
    }

    public static void Usage() {
        System.out.println("UEDashboardSnapper: Screenshot Edge Proxy Performance chart and optionally post it to Slack.\n");
        System.out.println("Usage:\n  java UEDashboardSnapper [-v] [-P <propsfile>] [-n]");
    }

    private static NetRc.Authenticator getAuthenticator(String host) throws Exception {
        NetRc netrc = new NetRc();
        String netrcPath = String.format("%s/.netrc", System.getProperty("user.home"));
        InputStream in = new FileInputStream(netrcPath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        netrc.parse(reader);
        return netrc.getAuthenticator(host);
    }

    private static Properties loadProps(String filename) throws IOException {
        Properties prop = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream(filename);
            prop.load(input);
            return prop;
        }
        finally {
            if (input != null) {
                input.close();
            }
        }
    }

    private void GetOpts(String[] args, String optString) throws Exception {
        // Parse command line args for args in the following format:
        //   -a value -b value2 ... ...

        // sanity checks
        if (args == null) return;
        if (args.length == 0) return;
        if (optString == null) return;
        final String argPrefix = "-";
        String patternString = "^" + argPrefix + "([" + optString.replaceAll(":","") + "])";

        java.util.regex.Pattern p = java.util.regex.Pattern.compile(patternString);

        int L = args.length;
        for(int i=0; i < L; i++) {
            String arg = args[i];
            java.util.regex.Matcher m = p.matcher(arg);
            if (!m.matches()) {
                throw new java.lang.Exception("The command line arguments are improperly formed. Use a form like '-a value' or just '-b' .");
            }

            char ch = arg.charAt(1);
            int pos = optString.indexOf(ch);

            if ((pos != optString.length() - 1) && (optString.charAt(pos+1) == ':')) {
                if (i+1 < L) {
                    i++;
                    Object current = this.options.get(m.group(1));
                    if (current == null) {
                        // not a previously-seen option
                        this.options.put(m.group(1), args[i]);
                    }
                    else if (current instanceof ArrayList<?>) {
                        // previously seen, and already a list
                        @SuppressWarnings("unchecked") ArrayList<String> oldList = (ArrayList<String>) current;
                        oldList.add(args[i]);
                    }
                    else {
                        // we have one value, need to make a list
                        ArrayList<String> newList = new ArrayList<String>();
                        newList.add((String)current);
                        newList.add(args[i]);
                        this.options.put(m.group(1), newList);
                    }
                }
                else {
                    throw new java.lang.Exception("Incorrect arguments.");
                }
            }
            else {
                // a "no-value" argument, like -v for verbose
                options.put(m.group(1), (Boolean) true);
            }
        }
    }

    private boolean getVerbose() {
        Boolean v = (Boolean) this.options.get("v");
        return (v != null && v);
    }

    public void Run() throws Exception {

        if (getVerbose()) {
            Path currentRelativePath = Paths.get("");
            String s = currentRelativePath.toAbsolutePath().toString();
            System.out.println("Current path is: " + s);
        }

        Properties props = null;
        String propsFile = (String) this.options.get("P");
        if (propsFile == null) {
            // default
            File f = new File("./autodash.properties");
            if(f.exists() && !f.isDirectory()) {
                props = loadProps("./autodash.properties");
            }
            else {
                throw new IllegalStateException("missing P argument and cannot find autodash.properties file");
            }
        }
        else {
            props = loadProps(propsFile);
        }

        Boolean useNetrc = (Boolean) this.options.get("n");
        useNetrc = (useNetrc != null && useNetrc);

        if (useNetrc) {
            NetRc.Authenticator auth = getAuthenticator("apigee.com");
            if (auth == null) {
                throw new IllegalStateException("cannot find apigee.com in .netrc");
            }
            props.setProperty("username", auth.getUser());
            props.setProperty("password", auth.getPass());
        }

        String chromeDriverPropName = "webdriver.chrome.driver";
        String chromeDriver = (String) props.get(chromeDriverPropName);

        if (chromeDriver != null)
            System.setProperty(chromeDriverPropName, chromeDriver);

        if (props.get("username")==null || props.get("username").equals("")) {
            throw new IllegalStateException("missing username");
        }
        if (props.get("password")==null || props.get("password").equals("")) {
            throw new IllegalStateException("missing password");
        }

        snapAndMaybePostCharts(props);
    }

    public static void main(String[] args) {
        try {
            UEDashboardSnapper me = new UEDashboardSnapper(args);
            me.Run();
        }
        catch (java.lang.Exception exc1) {
            System.out.println("Exception:" + exc1.toString());
            exc1.printStackTrace();
        }
    }

}
