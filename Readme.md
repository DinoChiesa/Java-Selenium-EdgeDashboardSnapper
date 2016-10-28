# Dashboard Snapper

This tool to
- drive a web browser to login to Edge,
- navigate to a particular organization,
- grab a snapshot of the dashboard
- and post it to a slack channel of your choice. 

It depends on:

- Java
- Selenium (the library that allows automation of browsers)
- Google Chrome, and [ChromeDriver](https://sites.google.com/a/chromium.org/chromedriver/)
- Apigee Edge cloud


## Configuration

You must set the properties in a Java properties file.
The program looks for "autodash.properties" in the current directory, or you can override it with the -P option.

Example:

```
# these are secrets and should not be disclosed
org=edge-org-name-here
username=dino@apigee.com
password=MySecretPassword123
slackToken=acquire-this-from-www.slack.com
# please url-escape @ and # in the slackchannel name
slackChannel=%23slackbot

# the following is Optional, if not specified, WebDriver will search your path for chromedriver.
webdriver.chrome.driver=/Users/dino/Applications/chromedriver

```

This tool works only with the ChromeDriver.  You must have Google Chrome installed.


## Build with:

```
mvn clean package

```

## Run with:

```
java -classpath "target/lib/*:target/autodash-1.0-SNAPSHOT.jar"  com.dinochiesa.autodash.DashboardSnapper
```


