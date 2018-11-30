package zeusbank.she0020;

import com.ibm.cics.server.*;
import zeusbank.util.BTS;
import java.io.UnsupportedEncodingException;

public class TellerForm {
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
			
			BTS bts = new BTS("LARGS");
			
			try {
				bts.acquire_process(processName, "AFCHECK");
				
				String btsToken = bts.get_acqprocess_container("TOKEN");
				
				String msg = "<body><style>body {font-family: sans-serif; font-size: 1.5em;"
						+ "text-align: center; margin: 0 auto} table{margin: 0 auto;}</style>";

				msg += "<h1>Zeusbank Anti-Fraud Check - Teller Form (by SHE00" + userId + ")</h1>";
				msg += "<p>When investigation has completed, please tick checkbox and submit.</p>";
				msg += "<form method='post'><table><tr>";
				msg += "<td>AFCheck process ID: </td>";
				msg += "<td><input type='text' name='id' value='" + processName + "'></td></tr>";
				msg += "<tr><td>Investigation completed: </td>";
				msg += "<td><input type='checkbox' name='complete' value='yes'></td></tr></table>";
				msg += "<br/><input type='submit' value='Submit'></form></body>";
				
				String complete = req.getFormField("complete");
				
				if (complete != null && complete.equals("yes")) {
					bts.put_acqprocess_container("RESPONSE", "done");
					bts.run_acqprocess_asynchronous("TELLER_RESP");
				}
					
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
			t.out.println("she0020 TellerForm error:");
			e.printStackTrace(t.out);
		}
	}
}
