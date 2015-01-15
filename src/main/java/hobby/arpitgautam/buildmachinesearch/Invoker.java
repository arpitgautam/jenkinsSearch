package hobby.arpitgautam.buildmachinesearch;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

//currently one shot, need to make it a daemon
public class Invoker {

	private static Logger logger = Logger.getLogger(Invoker.class);
	private Properties properties;
	private int waitingTimeForProcessorShutDown;
	private int socketTimeout;
	private int threadCount;
	private String elasticURL;

	public void start(String ips) throws Exception {
		getProperties();
		JenkinsFinder finder = new JenkinsFinder(threadCount, socketTimeout,
				TimeUnit.MILLISECONDS);
		ConcurrentLinkedQueue<String> jenkinsUrlQ = new ConcurrentLinkedQueue<>();
		ConcurrentLinkedQueue<JobBean> jenkinsJobsQ = new ConcurrentLinkedQueue<>();
		JenkinsUrlQProcessor jenkinsUrlQProcessor = new JenkinsUrlQProcessor(
				jenkinsUrlQ);
		JenkinsJobsProcessor jobsQProcessor = new JenkinsJobsProcessor(
				jenkinsJobsQ);
		jobsQProcessor.setServer(elasticURL);
		finder.setQ(jenkinsUrlQ);
		
		startProcessors(jenkinsJobsQ, jenkinsUrlQProcessor, jobsQProcessor);
		logger.trace("Going for netowork scan for jenkins machines");
		findJenkins(finder, ips);
		logger.trace("Sleeping for now so that other threads needs to start their work");
		Thread.sleep(1000);
		logger.trace("stopping Jenkins URL processor");
		stopProcessors(jenkinsUrlQ, jenkinsJobsQ, jenkinsUrlQProcessor,
				jobsQProcessor);
		
		System.out.println("dolne");
	}

	private void getProperties() {
		threadCount = Integer.parseInt(properties.getProperty(
				SharedConstants.FINDERTHREADCOUNT, "10"));
		socketTimeout = Integer.parseInt(properties.getProperty(
				SharedConstants.FINDERSOCKETTIMEOUT, "300"));
		waitingTimeForProcessorShutDown = Integer.parseInt(properties.getProperty(
				SharedConstants.WAITFORPROCESSORSHUTDOWN, "1000"));
		elasticURL = properties.getProperty(
				SharedConstants.INDEXERURL, "http://localhost:9200/");
	}

	private void stopProcessors(ConcurrentLinkedQueue<String> jenkinsUrlQ,
			ConcurrentLinkedQueue<JobBean> jenkinsJobsQ,
			JenkinsUrlQProcessor jenkinsUrlQProcessor,
			JenkinsJobsProcessor jobsQProcessor) throws InterruptedException {
		stopQProcessor(jenkinsUrlQProcessor, jenkinsUrlQ);
		logger.trace("stopping Jenkins jobs processor");
		stopQProcessor(jobsQProcessor, jenkinsJobsQ);
	}

	private void startProcessors(ConcurrentLinkedQueue<JobBean> jenkinsJobsQ,
			JenkinsUrlQProcessor jenkinsUrlQProcessor,
			JenkinsJobsProcessor jobsQProcessor) {
		logger.trace("Starting Jenkins URL processor");
		jenkinsUrlQProcessor.start();
		logger.trace("Starting Jenkins jobs processor");
		jobsQProcessor.start();
		// dummyPoster(jenkinsUrlQ);
		// dummypayloadPoster(jenkinsJobsQ);
		jenkinsUrlQProcessor.setOutputQ(jenkinsJobsQ);
	}

	private void findJenkins(JenkinsFinder finder, String ips)
			throws IOException {
		finder.findMachines(ips);
		waitForFinderCompletion(finder);
		finder.shutDown();
	}

	private void waitForFinderCompletion(JenkinsFinder finder) {
		finder.getFutures().forEach((f) -> {
			try {
				f.get();
			} catch (Exception e) {
			}
		});
	}

	private void stopQProcessor(QueueProcessor jenkinsQProcessor,
			ConcurrentLinkedQueue<? extends Object> queue)
			throws InterruptedException {

		while (queue.peek() != null) {
			Thread.sleep(waitingTimeForProcessorShutDown);
		}

		jenkinsQProcessor.killMe();

		// wait for it
		while (queue.peek() != null) {
			Thread.sleep(waitingTimeForProcessorShutDown);
		}
	}

	public void setProperties(Properties prop) {
		this.properties = prop;

	}

}
