package a3.a3droid;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Status;

import android.os.Handler;
import android.os.Message;

public class DiscoveryManager extends Thread{
	
	private String name;
	private Service ui;
	private BusAttachment mBus;
	private CallbackThread callbackThread;
	private boolean isDuplicated;

	public DiscoveryManager(String nameToDiscover, Service service){
		name = nameToDiscover;
		ui = service;
		callbackThread = new CallbackThread();
		isDuplicated = false;
		start();
	}

	public void connect(){

		mBus = new BusAttachment(getClass().getPackage().getName(), BusAttachment.RemoteMessage.Receive);

		mBus.registerBusListener(new BusListener() {
			
			@Override
			public void foundAdvertisedName(String name, short transport, String namePrefix) {
				
				Message msg = callbackThread.obtainMessage();
				msg.obj = name;
				msg.arg1 = transport;
				//msg.arg2 = Constants.FOUND_NAME;
				callbackThread.sendMessage(msg);
				//showOnScreen("Trovato nome " + name);
			}
		});

		Status status = mBus.connect();
		if (Status.OK != status) {
			showOnScreen("STATUS = " + status + " DOPO CONNECT().");
			return;
		}

		/* Inizio il discovery, che continua finchè il canale è connesso.
		 * Attivo il timer, il quale, una volta scaduto, chiama il metodo timerFired().
		 * Allo scadere del tempo, se non sono ancora connesso a nessun gruppo creo il Service.
		 * A questo punto scopro il gruppo appena creato e mi ci connetto.
		 */
		status = mBus.findAdvertisedName(name);
		if (Status.OK != status) {
			showOnScreen("STATUS = " + status + " DOPO FINDADVERTISEDNAME().");
			return;
		}
	}

	public void disconnect(){
		mBus.cancelFindAdvertisedName(name);
		mBus.disconnect();
	}
	
	private void showOnScreen(String string) {
		// TODO Auto-generated method stub
		ui.showOnScreen("(DiscoveryManager): " + string);
	}
	
	
	private class CallbackThread extends Handler{
		
		public CallbackThread() {
			super();
		}
	
		public void handleMessage(Message msg) {
			
			if(msg.obj.equals(name)){
				if(isDuplicated)
					ui.sendDuplicatedGroupSignal((int)(Math.random() * 100));
						
				// Trovo sempre il nome del service che mi ha creato.
				isDuplicated = true;
			}
		}
	}
}
