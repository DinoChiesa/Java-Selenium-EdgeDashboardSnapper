# Dashboard Snapper

This is a tool that posts a snapshot of an Apigee dashboard to a slack channel. 

This tool 
- drives a web browser to login to Edge,
- navigates to a particular organization,
- grabs a snapshot of the dashboard
- and posts it to a slack channel of your choice.

The tool depends on:

- Java
- Selenium (the library that allows automation of browsers)
- Google Chrome, and [ChromeDriver](https://sites.google.com/a/chromium.org/chromedriver/)
- Apigee Edge cloud


## Configuration

You must set the properties in a Java properties file.
The program looks for "autodash.properties" in the current directory, or you can override it with the -P option.

Example contents of autodash.properties:

```
# these are secrets and should not be disclosed
org=edge-org-name-here
username=dino@apigee.com
password=MySecretPassword123
slackToken=acquire-this-from-www.slack.com
# please url-escape @ and # in the slackchannel name
slackChannel=%23slackbot

# specifying webdriver.chrome.driver is optional.
# if not specified, WebDriver will search your path for the chromedriver executable.
webdriver.chrome.driver=/Users/dino/Applications/chromedriver

```

You can also view [the example](autodash-EXAMPLE.properties).

This tool works only with the ChromeDriver.  You must have Google Chrome installed.


## Build with:

```
mvn clean package

```

## Run with:

```
mvn exec:exec
```

or:

```
mvn exec:exec -Dautodash.properties=./autodash-ORGNAME.properties
```
or:

```
java -classpath "target/lib/*:target/autodash-1.0-SNAPSHOT.jar"  com.dinochiesa.autodash.DashboardSnapper
```



## Running via Cron

One possible pattern is to run this program with cron so that you get a new snapshot every day, posted to slack.

The script [cron-autodash.sh](cron-autodash.sh) can help with that.

Format your crontab entry like this:

```
30 07 * * * /path/to/autodash/cron-autodash.sh ORGNAME
```

This will run the cron-autodash.sh at 7:30 am local time.  There is one argument required: the name of the Edge organization . The script looks for a file named  autodash-ORGNAME.properties, in the same directory as it resides.  Eg, in /path/to/autodash/autodash-ORGNAME.properties .



## License

This material is [copyright 2016 Google Inc.](NOTICE)
and is licensed under the [Apache 2.0 License](LICENSE). This includes the Java code as well as the API Proxy configuration. 

