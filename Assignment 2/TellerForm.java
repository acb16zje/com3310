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

      // HTTP GET or POST
      String reqType = req.getHttpMethod();

      // Use query process name or form field process name
      String processName =
          reqType.equals("POST") ? req.getFormField("proc") : req.getQueryParm("proc");

      // HTML template
      String body = "<body><style>body {font-family: sans-serif; font-size: 1.5em;"
          + "text-align: center; margin: 0 auto} table{margin: 0 auto;}</style>"
          + "<h1>Zeusbank Anti-Fraud Check - Teller Form (by SHE0020)</h1>";
      String msg = body;
      msg += "<p>When investigation has completed, please tick checkbox and submit</p>";
      msg += "<form method='post'><table><tr>";
      msg += "<td>AFCheck process ID: </td>";
      msg +=
          "<td><input type='text' name='proc' value='" + req.getQueryParm("proc") + "'></td></tr>";
      msg += "<tr><td>Investigation completed: </td>";
      msg += "<td><input type='checkbox' name='complete' value='yes'></td></tr></table>";
      msg += "<br/><input type='submit' value='Submit'></form></body>";

      try {
        bts.acquire_process(processName, "AFCHECK");

        String btsToken = bts.get_acqprocess_container("TOKEN");

        if (btsToken.equals("")) {
          String complete = req.getFormField("complete");

          if (reqType.equals("POST")) {
            if (complete == null) {
              msg += "<p>Please tick checkbox to submit after investigation has completed</p>";
            } else {
              // Teller responded "yes", run TELLER_RESP event
              if (complete.equals("yes")) {
                bts.put_acqprocess_container("RESPONSE", "done");
                bts.run_acqprocess_asynchronous("TELLER_RESP");
              }
            }
          }
        } else {
          // Teller trying to visit when customer has not responded or timeout has not triggered yet
          msg = body + "<p>Customer has not yet responded or timeout has not triggered yet</p>";
        }

        // Teller finished investigation, don't show the template anymore
        try {
          String btsResponse = bts.get_acqprocess_container("RESPONSE");

          if (btsResponse.equals("done")) {
            msg = body + "<p>Investigation completed for " + processName + "</p>";
          }
        } catch (Exception e) {}

      } catch (Exception e) {
        // Teller submitting invalid or wrong AFCheck process ID
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
