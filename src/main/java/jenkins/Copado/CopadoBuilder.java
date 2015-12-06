package jenkins.Copado;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import hudson.Launcher;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * CopadoBuilder {@link Builder}.
 *
 */
public class CopadoBuilder extends Builder {

    private static final String DISPLAY_NAME = "Copado Webhook";
    private static final String JOB_SUCCEEDED = "success";
 	
    private final String stepName;
    private final String webhookUrl;
    private final String api_key;
    private int timeout = 60;
    
    public String resp;

    @DataBoundConstructor
    public CopadoBuilder(String stepName, String webhookUrl, String api_key, int timeout) {
    	this.stepName = stepName;
		this.api_key = api_key;
		if(timeout >= 0 )
		    this.timeout = timeout;
		this.webhookUrl = webhookUrl;
	}

	/**
	 * @return the stepName
	 */
	public String getStepName() {
		return stepName;
	}
	
	/**
	 * @return the webhookUrl
	 */
	public String getWebhookUrl() {
		return webhookUrl;
	}
	
	/**
	 * @return the api_key
	 */
	public String getApi_key() {
		return api_key;
	}
	
	/**
	 * @return the timeout
	 */
	public Integer getTimeout() {
		return timeout;
	}

    /* 
     * @see hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild, hudson.Launcher, hudson.model.BuildListener)
     */
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    	
    	PrintStream logger = listener.getLogger();
    	logger.println("\n");
    	logger.println("*** Copado Webhook ***");
    	logger.println("Step Name: " + stepName);
    	logger.println("Webhook URL: " + webhookUrl);
    	logger.println("API key: " + api_key);
    	logger.println("Timeout: " + timeout);
    	
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<String> future = executorService.submit(new CopadoTrigger(logger, webhookUrl, api_key, stepName));

        try {
            String result = future.get(timeout, TimeUnit.SECONDS);
            if (!JOB_SUCCEEDED.equalsIgnoreCase(result)) {
        	build.setResult(Result.FAILURE);
            }
        } catch (TimeoutException e) {
            logger.println(stepName+" Timeout Exception: " + e.toString());
            build.setResult(Result.FAILURE);
            e.printStackTrace();
        } catch (Exception e) {
            logger.println(stepName+" Exception: " + e.toString());
            build.setResult(Result.FAILURE);
            e.printStackTrace();
        }
        executorService.shutdownNow();
        
        return true;
    }
    
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link CopadoBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension 
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        /**
         * This name is used in the configuration screen.
         */
        public String getDisplayName() {
            return DISPLAY_NAME;
        }

    }
}

