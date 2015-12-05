Copado Plugin for Jenkins
-------------
With this plugin you can add a build step to your Jenkins project that executes a Copado Webhook API task. You would add this step after your Copado setup is finished. After the Copado Webhook is triggered, the plugin will wait for the job run to finish (or timeout). If the Job step is successful, your pipeline will continue to the next step in your pipeline; however, if it fails (or times out), the build will be marked as failed.

## Installing
1. Download and unpack the source
2. From terminal ```mvn clean install```
3. Copy ```target/copado.hpi``` to ```$JENKINS_HOME/plugins```
4. Restart Jenkins 

## Prerequisites
* **Webhook URL** - review Webhook API  ```http://docs.copado.apiary.io```
* **API Key** - in order for the plugin to start the Job and check on the status (via the Copado Webhook API), it requires an API key. To create an api key, login to your Copado account and navigate to the Account Summary tab, and finally on the API Key sub-tab.
 
## Usage
1. Open your project configuration from the Jenkins dashboard. 
2. In the build section, click ```Add build step``` and select ```Copado Webhook```. 
3. In the ```Webhook URL``` field, paste in the  URL and make sure that Salesforce Ids and API Key parameter are set. For the ```API Key``` field, paste in the API Key from above. 
4. Save your Jenkins project.

## Change Log
2015-Nov-16: First release
