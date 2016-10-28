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


public class DashboardSnapper {
    private final String optString = "-P:vn"; // getopt style
    private Hashtable<String, Object> options = new Hashtable<String, Object> ();

    public DashboardSnapper(String[] args) throws Exception {
        GetOpts(args, optString);
    }

    public static String nowFormatted(){
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd-HHmmss");
        Calendar c = new GregorianCalendar();
        fmt.setCalendar(c);
        return fmt.format(c.getTime());
    }

    public void snapAndPostDashboard(String org, String username, String password, String slackChannel, String slackToken) throws InterruptedException, IOException {

        WebDriver driver = new ChromeDriver();
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
            // Wait for the page to load, timeout after 10 seconds
            (new WebDriverWait(driver, 10))
                .until(ExpectedConditions.elementToBeClickable(By.id("apiManagementLink")));

            // (new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
            //     public Boolean apply(WebDriver d) {
            //         WebElement apiMgmtTile = driver.findElement(By.id("apiManagementLink"));
            //     }
            // });

            // navigate to edge
            driver.get("https://enterprise.apigee.com");
            String selectorPath = "//div[@id='organizationSelector']/a";
            (new WebDriverWait(driver, 10))
                .until(ExpectedConditions.elementToBeClickable(By.xpath(selectorPath)));

            driver.findElement(By.xpath(selectorPath)).click();

            //String orgAnchorPath = "//div[@id='organizationSelector']/ul/li/a[span[contains(text(),'ap-parityapi')]]";
            String orgAnchorPath = String.format("//div[@id='organizationSelector']/ul/li/a[span[normalize-space(text())='%s']]", org);
            driver.findElement(By.xpath(orgAnchorPath)).click();
            Thread.sleep(750);

            // wait again for the org selector
            (new WebDriverWait(driver, 10))
                .until(ExpectedConditions.elementToBeClickable(By.xpath(selectorPath)));

            // wait for the dashboard legend to appear
            // "/html/body//div[contains(@class,'summary-label') and normalize-space(text())='total traffic']";
            String legendPath = "/html/body//div[contains(@class,'summary-label')]";
            (new WebDriverWait(driver, 10))
                .until(ExpectedConditions.elementToBeClickable(By.xpath(legendPath)));

            WebElement legend = driver.findElement(By.xpath(legendPath));
            // legend.getText()

            // take screenshot
            File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            String filename = String.format("/tmp/dashboard-%s.png", nowFormatted());
            FileUtils.copyFile(scrFile, new File(filename));
            System.out.printf("screenshot: %s\n", filename);

            String commandTemplate = "curl -i -F file=@%s -F filename=%s -F token=%s  https://slack.com/api/files.upload?channels=%s";

            String commandString = String.format(commandTemplate,
                                                 filename, filename, slackToken, slackChannel);
            if (getVerbose()) {
                String cleanCommandString = String.format(commandTemplate,
                                                 filename, filename, "<TOKEN>", slackChannel);
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
        finally {
            driver.quit();
        }
    }

    public static void Usage() {
        System.out.println("DashboardSnapper: Screenshot Edge dashboard and post it to Slack.\n");
        System.out.println("Usage:\n  java DashboardSnapper [-v] [-P <propsfile>] [-n]");
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
                throw new IllegalStateException("missing P argument");
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

        snapAndPostDashboard((String)props.get("org"),
                             (String)props.get("username"),
                             (String)props.get("password"),
                             (String)props.get("slackChannel"),
                             (String)props.get("slackToken"));
    }

    public static void main(String[] args) {
        try {
            DashboardSnapper me = new DashboardSnapper(args);
            me.Run();
        }
        catch (java.lang.Exception exc1) {
            System.out.println("Exception:" + exc1.toString());
            exc1.printStackTrace();
        }
    }

}
