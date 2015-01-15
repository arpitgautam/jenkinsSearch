package hobby.arpitgautam.buildmachinesearch;

public class JobBean {

	private String data;
	private  String url;
	
	
	public String getData() {
		return data;
	}


	public void setData(String data) {
		this.data = data;
	}


	public String getUrl() {
		return url;
	}


	public void setUrl(String url) {
		this.url = url;
	}


	public JobBean(String url, String data) {
		this.url = url;
		this.data = data;
	}
	
	
}
