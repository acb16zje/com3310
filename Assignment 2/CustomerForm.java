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
			
			try {
				BTS bts = new BTS("LARGS");
				
				bts.acquire_process(processName, "AFCHECK");
				
        String btsToken = bts.get_acqprocess_container("TOKEN");
        String btsAmount = bts.get_acqprocess_container("AMOUNT");
				
        // HTML template
				String body = "<body><style>body {font-family: sans-serif; font-size: 1.5em;"
						+ "text-align: center; margin: 0 auto} table{margin: 0 auto;}</style>" 
				    + "<h1>Zeusbank Anti-Fraud Check - Customer Form (by SHE0020)</h1>";
				String msg = body;
				
				// Timeout token, same token or invalid token
				if (btsToken.equals("")) {
					msg += "<p>Can no longer respond to this AFCheck process</p>";
				} else if (btsToken.equals(token)) {
					msg += "<p>A transaction of \u00a3" + btsAmount + " is detected. Please verify: </p>";
					msg += "<form method='post'><table><tr>";
					msg += "<td><input type='radio' name='investigation' id='yes' value='yes' checked></td>";
					msg += "<td><label for='yes'>Investigation needed</label></td></tr>";
					msg += "<tr><td><input type='radio' name='investigation' id='no' value='no'></td>";
					msg += "<td><label for='no'>No investigation needed</label></td></tr></table>";
					msg += "<br/><input type='submit' value='Submit'></form></body>";
					
					String response = req.getFormField("investigation");
					
					// Customer responded, run CUSTOMER_RESP event
					if (response != null) {
						bts.put_acqprocess_container("RESPONSE", response);
						bts.run_acqprocess_asynchronous("CUSTOMER_RESP");
					}
				} else {
					msg += "<p>Invalid token</p>";
				}
				
				// Check if a response has already submitted
				try {
					bts.get_acqprocess_container("RESPONSE");
					msg = body + "<p>Response submitted</p>";
				} catch (Exception e) {}
				
				doc.createText(msg);
			} catch (Exception e) {
				doc.createText("Unable to find this AFCheck process");
			}
			
      resp.setMediaType("text/html");
      resp.sendDocument(doc, (short) 200, "OK", ASCII);
		} catch (Exception e) {
			t.out.println("she0020 CustomerForm error:");
			e.printStackTrace(t.out);
		}
	}
}

