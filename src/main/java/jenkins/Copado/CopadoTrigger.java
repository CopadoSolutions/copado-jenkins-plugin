package jenkins.Copado;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * CopadoTrigger
 *
 */
public class CopadoTrigger implements Callable<String> {

    private static String API_HOST = "copado.herokuapp.com";
    private static final String START_JOB = "start";
    private static final String JOB_STATUS = "results";
    private static final String JOB_SUCCEEDED = "success";
    private static final String JOB_IS_WORKING = "working";
    private static final String JOB_FAILED = "fail";

    private String url;
    private String result;
    private String api_key;
    private String stepName;

    private PrintStream log;
    String resp = null;

    public CopadoTrigger(PrintStream logger, String url, String api_key, String stepName) {
        this.log = logger;
        this.url = url;
        this.api_key = api_key;
        this.stepName = stepName;
        
        try {
			API_HOST = new URL(url).getHost();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        if(!url.contains("api_key")){
			try {
				url += (url.contains("?")?"&":"?") + "api_key="+URLEncoder.encode(api_key, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(url.contains("user_api_key"))url = url.replace("user_api_key", api_key);
		this.url = url;
    }

    @Override
    public String call() throws Exception {

        String statusUrl = process(url, START_JOB);
        TimeUnit.SECONDS.sleep(10);

        while (true) {
            resp = process(statusUrl, JOB_STATUS);
            log.println("Response received: " + resp);

            /* If test run is not complete, sleep 1s and try again. */
            if (JOB_IS_WORKING.equalsIgnoreCase(resp)) {
            	TimeUnit.SECONDS.sleep(10);
            } else {
                break;
            }
        }
        return resp;
    }

    /**
     * Method for making HTTP call
     *
     * @param url
     * @param apiEndPoint
     * @return the status url to be called after the job has been started, or the status of an on going job
     */
    public String process(String url, final String apiEndPoint) {
    	log.println("");
    	log.println("Process ("+apiEndPoint+") URL: "+ url);
        
        try {
        	URL u = new URL(url);
    		HttpURLConnection urlConnection = (HttpURLConnection) u.openConnection();
    		urlConnection.connect();
    		String status = String.format("Respnose Code - %d - %s", urlConnection.getResponseCode(), urlConnection.getResponseMessage());
    		
    		if (urlConnection.getResponseCode() <200 || urlConnection.getResponseCode()>=300){
    			log.println("\nERROR: "+status);
    			throw new Exception(status);
    		}
    		log.println(status);
    		InputStream is = urlConnection.getInputStream();
    		InputStreamReader isr = new InputStreamReader(is);

    		int numCharsRead;
    		char[] charArray = new char[1024];
    		StringBuffer sb = new StringBuffer();
    		while ((numCharsRead = isr.read(charArray)) > 0) {
    			sb.append(charArray, 0, numCharsRead);
    		}
            String responseBody = sb.toString();
            log.println("\n*** " + stepName + "\nData received: " + responseBody);
            result = parseJSON(responseBody, apiEndPoint);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception: ", e);
            e.printStackTrace();
        }
        return result;
    }


    /**
     * @param data
     * @return test result
     */
    private String parseJSON(String data, String apiendPoint) {
    	String res = null;
    	log.println("Parsing ("+apiendPoint+") data: "+ data);
        JSONObject jsonObject = JSONObject.fromObject(data);
        if(apiendPoint.equals(START_JOB)){
        	String copadoJobId = jsonObject.getString("copadoJobId");
        	res = "https://"+API_HOST+"/json/v1/webhook/jobStatus/"+copadoJobId+"?api_key="+api_key;
        }
        if(apiendPoint.equals(JOB_STATUS)){
        	boolean isFinished = jsonObject.containsKey("isFinished") && jsonObject.getBoolean("isFinished");
        	if(!isFinished) return JOB_IS_WORKING;
        	boolean isSuccess = jsonObject.containsKey("isSuccess") && jsonObject.getBoolean("isSuccess");
        	try{
        		JSONArray messages = jsonObject.getJSONArray("messages");
        		if(messages!=null && messages.size()>0){
        			for(Object o:messages){
        				String msg = (String)o;
        				log.println("\n" +(!isSuccess?"ERROR: ":"Message: ") + msg+"\n");
        			}
        		}
        	}
        	catch(Exception e){
        		LOGGER.log(Level.SEVERE, "Exception: ", e);
        		e.printStackTrace();
        	}
        	res = isSuccess?JOB_SUCCEEDED:JOB_FAILED;
        	log.println("Finished: " + isFinished + "\nSuccess: " + isSuccess);
        }
        return res;
    }

    private static final Logger LOGGER = Logger.getLogger(CopadoTrigger.class.getName());
}
