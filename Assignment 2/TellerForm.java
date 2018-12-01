package zeusbank.she0020;

import com.ibm.cics.server.*;
import zeusbank.util.BTS;

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
		  BTS bts = new BTS("LARGS");
			HttpRequest req = HttpRequest.getHttpRequestInstance();
			HttpResponse resp = new HttpResponse();
			Document doc = new Document();

			String reqType = req.getHttpMethod();
			String processName = "";
      String userId = req.getPath().substring(req.getPath().length() - 2);
      String m = "<body><style>body {font-family: sans-serif; font-size: 1.5em;"
          + "text-align: center; margin: 0 auto} table{margin: 0 auto;}</style>";
      String msg = m + "<h1>Zeusbank Anti-Fraud Check - Teller Form (by SHE00" + userId + ")</h1>";
      msg += "<p>When investigation has completed, please tick checkbox and submit</p>";
      msg += "<form method='post'><table><tr>";
      msg += "<td>AFCheck process ID: </td>";
      msg += "<td><input type='text' name='proc' value='" + req.getQueryParm("proc") + "'></td></tr>";
      msg += "<tr><td>Investigation completed: </td>";
      msg += "<td><input type='checkbox' name='complete' value='yes'></td></tr></table>";
      msg += "<br/><input type='submit' value='Submit'></form></body>";
			
			if (reqType.equals("POST")) {
			  processName = req.getFormField("proc");
			} else {
			  processName = req.getQueryParm("proc");
			}
			
			try {
				bts.acquire_process(processName, "AFCHECK");

				String btsToken = bts.get_acqprocess_container("TOKEN");

				if (btsToken.equals("")) {
	        String process = req.getFormField("proc");
	        String complete = req.getFormField("complete");
	         
	        if (reqType.equals("POST")) {
	          if (complete == null) {
	            msg += "<p>Please tick checkbox to submit after investigation has completed</p>";
	          } else {
	            if (complete.equals("yes")) {
	              bts.put_acqprocess_container("RESPONSE", "done");
	              bts.run_acqprocess_asynchronous("TELLER_RESP");
	            }
	          }
	        }
				} else {
					msg += m + "<p>Customer has not yet responded or timeout has not triggered yet</p>";
				}

				try {
					String btsResponse = bts.get_acqprocess_container("RESPONSE");

					if (btsResponse.equals("done")) {
						msg = m + "<p>Investigation complete for " + processName + "</p>";
					}
				} catch (Exception e) {}
			} catch (Exception e) {
			  if (reqType.equals("POST")) {
          if (processName.equals("")) {
            msg += "<p>Please enter a valid AFCheck process ID</p>";
          } else {
            msg += "<p>Process " + processName + " is not found</p>";
          }
			  } else {
			    msg = "Unable to find this AFCheck process";
			  }
			}
			
      doc.createText(msg);
      resp.setMediaType("text/html");
      resp.sendDocument(doc, (short) 200, "OK", ASCII);
		} catch (Exception e) {
			t.out.println("she0020 TellerForm error:");
			e.printStackTrace(t.out);
		}
	}
}
