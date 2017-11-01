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
import java.util.Properties;
import org.apache.commons.io.FileUtils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.OutputType;

import net.oneandone.sushi.util.NetRc;


public class UEDashboardSnapper {
    private final String optString = "-P:vn"; // getopt style
    private Hashtable<String, Object> options = new Hashtable<String, Object> ();

    public UEDashboardSnapper(String[] args) throws Exception {
        GetOpts(args, optString);
    }

    public static String nowFormatted(){
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd-HHmmss");
        Calendar c = new GregorianCalendar();
        fmt.setCalendar(c);
        return fmt.format(c.getTime());
    }

    public void snapAndPostDashboard(String org, String env, String username, String password, String slackChannel, String slackToken) throws InterruptedException, IOException {

        WebDriver driver = new ChromeDriver();
        String snapshotFile = "";
        try {
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

            // (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            //     public Boolean apply(WebDriver d) {
            //         WebElement apiMgmtTile = driver.findElement(By.id("apiManagementLink"));
            //     }
            // });

            // Wait for the page to load, timeout after 10 seconds
            //String orgSelectorPath = "//#navbar/div/div/div";
            driver.get("https://apigee.com/edge");
            //String orgSelectorPath = "div#user-ini";
            (new WebDriverWait(driver, 10))
                .until(ExpectedConditions.visibilityOfElementLocated(By.id("user-ini")));

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

            // env selector
            (new WebDriverWait(driver, 10))
                .until(ExpectedConditions.visibilityOfElementLocated(By.id("environmentSelector")));
            // String envSelectorDropPath = "//div[id='environmentSelector']/a[contains(@class,'dropdown-toggle')]";
            // (new WebDriverWait(driver, 10))
            //     .until(ExpectedConditions.elementToBeClickable(By.xpath(envSelectorDropPath)));
            // driver.findElement(By.xpath(envSelectorDropPath)).click();
            driver.findElement(By.id("environmentSelector")).click();
            Thread.sleep(850);
            
            String envSelectorPath = String.format("//div[@id='environmentSelector']/ul/li/a[normalize-space(text())='%s']", env);
            // WebElement element1 = driver.findElement(By.xpath(envSelectorPath));
            // if (element1 != null) {
            //     System.out.printf("element1: '%s'\n", element1.getText());
            // }
            
            //System.out.printf("waiting: %s\n", envSelectorPath);
            (new WebDriverWait(driver, 10))
                .until(ExpectedConditions.elementToBeClickable(By.xpath(envSelectorPath)));
            driver.findElement(By.xpath(envSelectorPath)).click();

            String weekSelectorPath = "//div[contains(@class,'ax-toolbar')]/div[contains(@class,'date-range-container')]/div/div[contains(@class,'btn-group')]/button[normalize-space(text())='Week']";
            driver.findElement(By.xpath(weekSelectorPath)).click();
            Thread.sleep(2750); // the legend is already showing. give it a chance to reset

            String summaryTitlePath = "//div[contains(@class,'ax-dashboard-element') and contains(@class,'top-element')]/div[contains(@class,'summary')]/div/div[contains(@class,'summary-title')]";
            (new WebDriverWait(driver, 10))
                .until(ExpectedConditions.elementToBeClickable(By.xpath(summaryTitlePath)));
            Thread.sleep(3750); // give any warnings a chance to disappear
            
            // take screenshot
            File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            snapshotFile = String.format("/tmp/dashboard-%s.png", nowFormatted());
            FileUtils.copyFile(scrFile, new File(snapshotFile));
            System.out.printf("\nscreenshot: %s\n", snapshotFile);

            if (slackChannel != null && slackToken != null) {
                String commandTemplate = "curl -i -F file=@%s -F filename=%s -F token=%s  https://slack.com/api/files.upload?channels=%s";

                String commandString = String.format(commandTemplate,
                                                     snapshotFile, snapshotFile, slackToken, slackChannel);
                if (getVerbose()) {
                    String cleanCommandString = String.format(commandTemplate,
                                                              snapshotFile, snapshotFile, "<TOKEN>", slackChannel);
                    System.out.printf("curl: %s\n", cleanCommandString);
                }

                Process p = Runtime.getRuntime().exec(commandString);
                if (getVerbose()) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()) );
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println(line);
                    }
                    in.close();
                }
            }
        }
        finally {
            driver.quit();
            if (slackChannel != null && slackToken != null) {
                File file = new File(snapshotFile);
                if(file.exists())
                    file.delete();
            }
        }
    }

    public static void Usage() {
        System.out.println("UEDashboardSnapper: Screenshot Edge dashboard and post it to Slack.\n");
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
                    ArrayList<String> newList;
                    if (current == null) {
                        // not a previously-seen option
                        this.options.put(m.group(1), args[i]);
                    }
                    else if (current instanceof ArrayList<?>) {
                        // previously seen, and already a lsit
                        newList = (ArrayList<String>) current;
                        newList.add(args[i]);
                    }
                    else {
                        // we have one value, need to make a list
                        newList = new ArrayList<String>();
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

        if (props.get("username")==null) {
            throw new IllegalStateException("missing username");
        }
        if (props.get("password")==null) {
            throw new IllegalStateException("missing password");
        }
        snapAndPostDashboard((String)props.get("org"),
                             (String)props.get("env"),
                             (String)props.get("username"),
                             (String)props.get("password"),
                             (String)props.get("slackChannel"),
                             (String)props.get("slackToken"));
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
