package hobby.arpitgautam.buildmachinesearch;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

public class JenkinsUrlQProcessor extends QueueProcessor {

	private ConcurrentLinkedQueue<String> inputQ;
	private boolean posionPill = false;
	private Logger logger = Logger.getLogger(JenkinsUrlQProcessor.class);
	private CloseableHttpClient httpclient;
	private ConcurrentLinkedQueue<JobBean> outputQ;

	public JenkinsUrlQProcessor(ConcurrentLinkedQueue<String> upIpsQ) {
		this.inputQ = upIpsQ;
		httpclient = HttpClients.createDefault();
	}

	//do not make it multi threaded as it processes and then removes
	// multiple threads will process same message then
	@Override
	public void operation() {
		try{
			while (!posionPill) {
				String url = inputQ.peek();
				if (url != null) {
					if (url.isEmpty()) {
						logger.warn("Empty URL found from jenkins machine");
						continue;
					}
					doWork(url);
					inputQ.poll();
	
				} else {
					sleep(500);
				}
			}
		}finally{
			closeHttpClient();
		}
		logger.trace("posion pill recieved, exiting now!");
	}

	private void doWork(String url) {
		try {
			logger.trace("sending api reuqest to:" + url);
			String response = getJenkinsJobs(url + "api/json");
			//retrieve host from url
			URI uri = new URI(url);
			JobBean bean = new JobBean(uri.getHost(),response);
			logger.trace("adding api response to queue for:" + url);
			outputQ.add(bean);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	public void setOutputQ(ConcurrentLinkedQueue<JobBean> jenkinsJobsQ) {
		this.outputQ = jenkinsJobsQ;

	}

	@Override
	public void killMe() {
		this.posionPill = true;

	}

	private void closeHttpClient() {
		try {
			httpclient.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	private String getJenkinsJobs(String url) throws Exception {
		HttpGet httpGet = new HttpGet(url);
		String jenkinsResponse = null;
		CloseableHttpResponse response1 = httpclient.execute(httpGet);

		try {
			int statusCode = response1.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				throw new Exception("status code for " + url + ":" + statusCode);
			}
			HttpEntity entity1 = response1.getEntity();
			InputStream stream = entity1.getContent();
			StringWriter writer = new StringWriter();
			IOUtils.copy(stream, writer, "UTF-8");
			jenkinsResponse = writer.toString();
			if (outputQ == null) {
				throw new IllegalStateException("Output queue not supplied");
			}
			EntityUtils.consume(entity1);
		} finally {
			response1.close();
		}
		return jenkinsResponse;
	}

}
