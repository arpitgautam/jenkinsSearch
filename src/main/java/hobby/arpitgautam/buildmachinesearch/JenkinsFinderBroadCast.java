package hobby.arpitgautam.buildmachinesearch;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JenkinsFinderBroadCast {

	public String autoDiscoveryAddress = "255.255.255.255";

	public void findByName(String machineName) throws IOException {
		byte[] buffer = new byte[128];
		Arrays.fill(buffer, (byte) 1);
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		packet.setAddress(InetAddress.getByName(machineName));
		packet.setPort(33848);
		DatagramSocket datagramSocket = new DatagramSocket();
		datagramSocket.setSoTimeout(1);
		datagramSocket.send(packet);
		DatagramPacket recv = new DatagramPacket(new byte[2048], 2048);
		datagramSocket.receive(recv);
		String responseXml = new String(recv.getData(), 0, recv.getLength());
		System.out.println(responseXml);
		datagramSocket.close();
	}

	public void discoverFromBroadcast() throws Exception {

		DatagramSocket socket = new DatagramSocket();
		socket.setBroadcast(true);

		sendBroadcast(socket);
		List<DatagramPacket> responses = collectBroadcastResponses(socket);
		socket.close();
		responses.forEach((packet)->{
			String responseXml = new String(packet.getData(), 0, packet.getLength());
			System.out.println(responseXml);
		});
		return;
	}

	protected void sendBroadcast(DatagramSocket socket) throws IOException {
		byte[] buffer = new byte[128];
		Arrays.fill(buffer, (byte) 1);
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		packet.setAddress(InetAddress.getByName(autoDiscoveryAddress));
		packet.setPort(33848);
		socket.send(packet);
	}

	protected List<DatagramPacket> collectBroadcastResponses(
			DatagramSocket socket) throws Exception {
		List<DatagramPacket> responses = new ArrayList<DatagramPacket>();

		
		long limit = System.currentTimeMillis() + 5 * 1000;
		while (true) {
			try {
				socket.setSoTimeout(Math.max(1,
						(int) (limit - System.currentTimeMillis())));

				DatagramPacket recv = new DatagramPacket(new byte[2048], 2048);
				socket.receive(recv);
				responses.add(recv);
			} catch (SocketTimeoutException e) {
				// timed out
				if (responses.isEmpty()) {
					throw new Exception(
							"Failed to receive a reply to broadcast.");
				}
				return responses;
			}
		}
	}
}
