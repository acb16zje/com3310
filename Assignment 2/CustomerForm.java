package zeusbank.she0020;

import com.ibm.cics.server.*;
import zeusbank.util.*;
import java.io.UnsupportedEncodingException;

public class CustomerForm {
	private final static String EBCDIC = "037";
	private final static String ASCII = "iso-8859-1";

	public static void main(String args[]) {
		Task t = Task.getTask();
		if (t == null) {
			System.out.println("she0020: failed to get task");
			return;
		}
	
		try {
			HttpRequest req = HttpRequest.getHttpRequestInstance();
			HttpResponse resp = new HttpResponse();
			Document doc = new Document();
			
			String processName = req.getQueryParm("proc");
			String token = req.getQueryParm("token");
			String userId = req.getPath().substring(req.getPath().length() - 2);
			
			try {
				BTS bts = new BTS("LARGS");
				
				bts.acquire_process(processName, "AFCHECK");
				
				String msg = "<body><style>body {font-family: sans-serif; font-size: 1.5em;"
						+ "text-align: center; margin: 0 auto} table{margin: 0 auto;}</style>";
				
				String btsToken = bts.get_acqprocess_container("TOKEN");
				String btsAmount = bts.get_acqprocess_container("AMOUNT");
				
				if (token.equals(btsToken)) {
					msg += "<h1>Zeusbank Anti-Fraud Check - Customer Form (by SHE00" + userId + ")</h1>";
					msg += "<p>A transaction of \u00a3" + btsAmount + " is detected. Please verify: </p>";
					msg += "<form method='post'><table><tr>";
					msg += "<td><input type='radio' name='investigation' id='yes' value='yes' checked></td>";
					msg += "<td><label for='yes'>Investigation needed</label></td></tr>";
					msg += "<tr><td><input type='radio' name='investigation' id='no' value='no'></td>";
					msg += "<td><label for='no'>No investigation needed</label></td></tr></table>";
					msg += "<br/><input type='submit' value='Submit'></form></body>";
					
					String response = req.getFormField("investigation");
					
					if (response != null) {
						bts.put_acqprocess_container("RESPONSE", response);
						bts.run_acqprocess_asynchronous("CUSTOMER_RESP");
					}
				} else {
					msg += "<p>Invalid token</p>";
				}
				
				try {
					bts.get_acqprocess_container("RESPONSE");
					msg = "Response submitted";
				} catch (Exception e) {}
				
				doc.createText(msg);
				resp.setMediaType("text/html");
				resp.sendDocument(doc, (short) 200, "OK", ASCII);
			} catch (Exception e) {
				String errMsg = "Unable to find this AFCheck process";
				doc.createText(errMsg);
				resp.setMediaType("text/html");
				resp.sendDocument(doc, (short) 200, "OK", ASCII);
			}
		} catch (Exception e) {
			t.out.println("she0020 CustomerForm error:");
			e.printStackTrace(t.out);
		}
	}
}

