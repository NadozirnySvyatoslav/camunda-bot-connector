# camunda-bot-connector

## Install to camunda

1. Copy bot-connector.war to tomcat/webapps
2. Wait until application is deployed
3. Edit tomcat/webapps/bot-connector/WEB-INF/classes/configuration.xml

```
<?xml version="1.0" encoding="UTF-8" ?>
<configuration>
<bots>
    <bot>
        <token>918999146:AAEmqweqweasdsdqw0PlfRW1eeO-GB1c50Ek</token>
        <process_key>test-bot2</process_key>
    </bot>
    <bot>
        <token>918999146:AAEmmWvKvkeqweqweqqwefaaeeO-GB1c50Ek</token>
        <process_key>test-bot</process_key>
    </bot>

</bots>
<camunda_engine>http://localhost:8080/engine-rest</camunda_engine>
</configuration>

```

with your Telegram-bot tokens

4. Deploy your BPMN-process (demo/test-bot.bpmn can be used as an example)

![BPMN process](demo/demo.png)


5. Enjoy your bot

![Start bot](demo/step1.png)

![Click YES](demo/step2.png)
