package hobby.arpitgautam.buildmachinesearch;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;




import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class JenkinsJobsProcessor extends QueueProcessor {

	private boolean posionPill = false;
	static private String INDEX = "machine";
	static private String TYPE = "build";
	private ConcurrentLinkedQueue<JobBean> inputQ;
	private Logger logger = Logger.getLogger(JenkinsJobsProcessor.class);
	private CloseableHttpClient httpclient;
	private int indexCounter;
	private String elasticURL;

	public JenkinsJobsProcessor(ConcurrentLinkedQueue<JobBean> inQ) {
		this.inputQ = inQ;
		httpclient = HttpClients.createDefault();
	}

	public void setServer(String elasticURL) {
		this.elasticURL = elasticURL;
		
	}
	
	@Override
	public void operation() {
		try {
			while (!posionPill) {
				JobBean bean = inputQ.peek();
				if (bean != null) {
					indexCounter = 0;
					String payload = bean.getData();
					String url = bean.getUrl();
					if (payload == null || payload.isEmpty()) {
						logger.warn("Empty URL found from jenkins machine");
						continue;
					}
					try {
						submitToElastic(url, payload);
					} catch (Exception e) {
						logger.error(e.getMessage());
					}
					inputQ.poll();

				} else {
					sleep(100);
				}
			}
		} finally {
			closeHttpClient();
		}
		logger.trace("posion pill recieved, exiting now!");

	}

	@Override
	public void killMe() {
		posionPill = true;

	}

	private void closeHttpClient() {
		try {
			httpclient.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	
	private void submitToElastic(String machineUrl, String payload) throws Exception{
		createPayloadForElastic(machineUrl,payload);
	}

	//TODO- put dynamic ids, generate one document per id.
	@SuppressWarnings("unchecked")
	private void createPayloadForElastic(String machineUrl,String payload) throws Exception {
		JSONParser parser = new JSONParser();
		Object obj = parser.parse(payload);
		JSONObject payloadObject = (JSONObject) obj;
		JSONArray jobsArray = (JSONArray)payloadObject.get("jobs");
		String url = this.elasticURL + INDEX + "/" + TYPE + "/" + machineUrl;
		
		for(int i =0;i<jobsArray.size();i++){
			JSONObject o = (JSONObject) jobsArray.get(i);
			o.put("machine", machineUrl);
			sendToElastic(o,url + indexCounter);
			indexCounter++;
		}
	}

	private void sendToElastic(JSONObject doc, String url) throws Exception {
		HttpPut httpPut = new HttpPut(url);
		StringEntity myEntity = new StringEntity(doc.toString(), ContentType.create(
				"application/json", "UTF-8"));
		httpPut.setEntity(myEntity);
		CloseableHttpResponse response1 = httpclient.execute(httpPut);
		try {
			int statusCode = response1.getStatusLine().getStatusCode();
			if(statusCode > 300){
				throw new Exception("Unable to create index for:" + url);
			}
			System.out.println(statusCode);
		} finally {
			response1.close();
		}
	
	}
	
}
