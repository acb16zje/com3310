package zeusbank.she0020;

import java.text.*;
import java.util.Random;

import com.ibm.cics.server.*;
import zeusbank.util.*;
 
public class AFCheck {
    public static void main(CommAreaHolder CAH) {    	
        Task t = Task.getTask();
        
        if (t == null) {
        	System.err.println("she0020: Can't get Task");
			return;
        }
        
		try {
			BTS bts = new BTS("LARGS");
			String event = bts.retrieve_reattach_event();
			String processName = bts.get_process_name();
			String toAcctNum = bts.get_process_container("TO_ACCTNUM");
			String userId = toAcctNum.substring(toAcctNum.length() - 2);
						
			if (event.equals("DFHINITIAL")) {
				Random r = new Random();
				NumberFormat formatter = new DecimalFormat("00000000");
				String rtok = formatter.format(r.nextInt(100000000));
				
				bts.put_process_container("TOKEN", rtok);
				t.out.print("she0020: Notify customer (");
				t.out.print(bts.get_process_container("TO_SORTCODE") + ", ");
				t.out.print(toAcctNum + "): ");
				t.out.print("An unknown transaction has been detected.\n Please visit ");
				t.out.print("https://zeuszos.edu.ihost.com:8005/afcheck/customer/she00");
				t.out.println(userId + "?proc=" + processName + "&token=" + rtok);
				
				bts.define_input_event("CUSTOMER_RESP");
				bts.define_timer_after("CUSTOMER_TIMER", "TELLER", 5);
				
			} else if (event.equals("TELLER")) {
				bts.delete_event("CUSTOMER_RESP");
				bts.delete_timer("CUSTOMER_TIMER");
				bts.define_input_event("TELLER_RESP");
				
				t.out.print("she0020: Notify teller: Please investigate the transaction \n");
				t.out.print("https://zeuszos.edu.ihost.com:8005/afcheck/teller/she00");
				t.out.println(userId + "?proc=" + processName);
				
			} else if (event.equals("CUSTOMER_RESP")) {
				bts.delete_event("CUSTOMER_RESP");
				bts.delete_timer("CUSTOMER_TIMER");
				bts.define_input_event("TELLER_RESP");
				
				String response = bts.get_process_container("RESPONSE");
				
				if (response.equals("yes")) {
					t.out.print("she0020: Notify teller: Please investigate the transaction \n");
					t.out.print("https://zeuszos.edu.ihost.com:8005/afcheck/teller/she00");
					t.out.println(userId + "?proc=" + processName);
				} else {
					bts.delete_event("TELLER_RESP");
				}
			} else if (event.equals("TELLER_RESP")) {
				bts.delete_event("TELLER_RESP");
			}
			
		} catch(Exception e) {
	    	t.out.println("she0020: error:");
	    	e.printStackTrace(t.out);
		}
	}
}
