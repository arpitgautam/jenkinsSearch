package hobby.arpitgautam.buildmachinesearch;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.apache.log4j.Logger;

public class JenkinsFinder {
	private long timeout;
	private ConcurrentLinkedQueue<String> jenkinsUrlQ;
	private ExecutorService executor;
	private List<Future<?>> futures = new LinkedList<>();
	private Logger logger = Logger.getLogger(JenkinsFinder.class);
	String[] addresses;

	public List<Future<?>> getFutures() {
		return futures;
	}

	public enum Octent {
		SECOND, THIRD, FOURTH
	}

	public JenkinsFinder(int n, int timeout, TimeUnit unit) {
		executor = Executors.newFixedThreadPool(n);
		this.timeout = TimeUnit.MILLISECONDS.convert(timeout, unit);
	}

	public void shutDown() {
		executor.shutdown();
	}

	public void findMachines(String subnetString) throws IOException {
		SubnetUtils util = new SubnetUtils(subnetString);
		SubnetInfo info = util.getInfo();
		addresses = info.getAllAddresses();
		findMachines(addresses);

	}

	private void findMachines(String[] addresses) throws IOException {
		for(String address:addresses){
			findMachine(address);
		}
	}

	private void findMachine(String ip) throws IOException {
		InetAddress address = InetAddress.getByName(ip);
		Future<?> f = executor.submit(() -> {
			try {
				findJenkinsByIp(address);
			} catch (IOException e) {
				// logger.trace(address + " does not exist");
			}
		});
		futures.add(f);

	}

	// making public as this is meat method of this class and we really need
	// to test this, also this is a handy method, can be used by anyone
	public void findJenkinsByIp(InetAddress address) throws IOException {
		// address =InetAddress.getByName("kvs-us-gold");
		byte[] buffer = new byte[128];
		String url = null;
		Arrays.fill(buffer, (byte) 1);
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		packet.setAddress(address);
		packet.setPort(33848);
		// THINK ABOUT THIS
		DatagramSocket datagramSocket = createSocket();
		try {
			datagramSocket.setSoTimeout((int) timeout);

			datagramSocket.send(packet);
			DatagramPacket recv = new DatagramPacket(new byte[2048], 2048);
			logger.trace("Sending discovery packet to:" + address.toString());
			datagramSocket.receive(recv);
			String responseXml = new String(recv.getData(), 0, recv.getLength());
			logger.trace(address.toString() + "  is jenkins machine");
			url = parseResponseForURL(responseXml);
			logger.trace(url + "  added to jenkins URL queue");
		} finally {
			datagramSocket.close();
		}
		jenkinsUrlQ.add(url);
	}

	public DatagramSocket createSocket() throws SocketException {
		return new DatagramSocket();
	}

	private String parseResponseForURL(String responseXml) {
		int i1 = responseXml.indexOf("<url>");
		int i2 = responseXml.indexOf("</url>");
		if (i1 >= i2) {
			throw new IllegalArgumentException("url not found");
		}
		i1 += "<url>".length();
		String url = responseXml.substring(i1, i2);
		return url;
	}

	public void setQ(ConcurrentLinkedQueue<String> jenkinsUrlQ) {
		this.jenkinsUrlQ = jenkinsUrlQ;

	}

}
