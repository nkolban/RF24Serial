package nrf24serial;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.fazecast.jSerialComm.SerialPort;

public class Main {
	private SerialPort port;

	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Usage: <cmd> {COMPORT}");
			return;
		}
		Main main = new Main(args);
		main.run();
	}

	public Main(String args[]) {
		System.out.println("Working with port: " + args[0]);
		port = SerialPort.getCommPort(args[0]);
		port.setBaudRate(9600);
		boolean result = port.openPort();
		if (result == false) {
			System.out.println("Failed to open the port called " + args[0]);
			System.exit(0);
		}
		System.out.println("Opened port: " + port.getDescriptivePortName());
	}

	private void run() {
		try {
			Thread.sleep(2000);
			RF24Serial nrf24Serial = new RF24Serial(port);
			nrf24Serial.test();
			nrf24Serial.test();
			//nrf24Serial.setRate(1);
			nrf24Serial.setRXAddr("NODE2");
			nrf24Serial.setTXAddr("NODE1");
			nrf24Serial.startListening();
			nrf24Serial.dump();
			byte data[] = new byte[4];
			nrf24Serial.read(data);
			long i = Integer.toUnsignedLong(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt());
			System.out.println("Read value: " + i);
			nrf24Serial.read(data);
			i = Integer.toUnsignedLong(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt());
			System.out.println("Read value: " + i);
			/*
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			while (true) {
				String line = br.readLine() + "\n";
				port.getOutputStream().write(line.getBytes());

			}*/
			//Thread.sleep(30*1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

//	private void addListener() {
//		port.addDataListener(new SerialPortDataListener() {
//			
//			@Override
//			public void serialEvent(SerialPortEvent serialPortEvent) {
//				// TODO Auto-generated method stub
//				//System.out.println("Avail");
//				try {
//					while(serialPortEvent.getSerialPort().getInputStream().available() > 0) {
//						int c = serialPortEvent.getSerialPort().getInputStream().read();
//						System.out.print((char) c);
//					}
//				} catch(Exception e) {
//					e.printStackTrace();
//				}
//			}
//			
//			@Override
//			public int getListeningEvents() {
//				// TODO Auto-generated method stub
//				return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
//			}
//		});
//	}
//	private void readFromSerial() {
//		Thread t = new Thread(() -> {
//			try {
//				InputStream is = port.getInputStream();
//				while (true) {
//					int c = is.read();
//					//System.out.print("#");
//					System.out.print((char) c);
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		});
//		t.setDaemon(true);
//		t.start();
//	}

} // End of class
// End of file