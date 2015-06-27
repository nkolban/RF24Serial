package nrf24serial;

import com.fazecast.jSerialComm.SerialPort;

public class RF24Serial {
	private boolean debug = false;
	private char TERMINATE_CHAR = '#';
	/**
	 * Port used to communicate with Serial nRF24.
	 */
	private SerialPort port;

	public RF24Serial(SerialPort port) {
		this.port = port;
		init();
	} // End of constructor

	public RF24Serial(String portString) {
		this.port = SerialPort.getCommPort(portString);
		this.port.setBaudRate(9600);
		init();
	} // End of constructor
	
	/**
	 * Initialize the class object.  This can be called by multiple different
	 * constructors.
	 */
	private void init() {
		port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 2000, 2000);
		if (debug) {
			dumpSerial();
		}
	} // End of init

	/**
	 * Send a simple test that will just return an OK indication.
	 * @throws RF24Exception
	 */
	public void test() throws RF24Exception {
		writeLine("AT");
		checkOK();
	} // End of test

	public void setRate(int rate) throws RF24Exception {
		writeLine(String.format("AT+RATE=%d", rate));
		checkOK();
	} // End of setRate

	public void stopListening() throws RF24Exception {
		writeLine("AT+DIR=TX");
		checkOK();
	} // End of stopListening

	public void startListening() throws RF24Exception {
		writeLine("AT+DIR=RX");
		checkOK();
	} // End of startListening

	public void setTXAddr(String addr) throws RF24Exception {
		writeLine(String.format("AT+TXADDR=%s", addr));
		checkOK();
	} // End of setTXAddr

	public void setRXAddr(String addr) throws RF24Exception {
		writeLine(String.format("AT+RXADDR=%s", addr));
		checkOK();
	} // End of setRXAddr

	public void read(byte data[]) throws RF24Exception {
		writeLine(String.format("AT+READ=%d", data.length));
		readBytes(data);
		checkOK();
	} // End of read

	public void write(byte data[]) throws RF24Exception {
		writeLine(String.format("AT+WRITE=%d", data.length));
		writeBytes(data);
		checkOK();
	} // End of write

	public void dump() throws RF24Exception {
		writeLine("AT+DUMP");
		checkOK();
	} // End of dump

	/**
	 * Write a line of text to the Serial nRF24.
	 * 
	 * @param line
	 *            The line of text to write to the Serial nRF24.
	 */
	private void writeLine(String line) {
		//System.out.println("Sending to Serial NRF24> " + line);
		writeBytes((line + TERMINATE_CHAR).getBytes());
	} // End of writeLine

	/**
	 * Write the bytes to the Serial nRF24.
	 * 
	 * @param data
	 *            The data to be written to the Serial nRF24.
	 */
	private void writeBytes(byte data[]) {
		try {
			if (debug) {
				System.out.println("!! We are transmitting the following to Serial NRF24: " + new String(data));
			}
			port.getOutputStream().write(data);
			port.getOutputStream().flush();
			//Thread.sleep(1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	} // End of writeBytes

	private void readBytes(byte data[]) {
		if (debug) {
			System.out.println("!! read raw bytes: " + data.length);
		}
		//ByteBuffer byteBuffer = ByteBuffer.allocate(4);
		int len = data.length;
		int off = 0;
		byte b[] = new byte[1];
		try {
			while (len > 0) {
				int lengthRead = port.readBytes(b, 1);
				if (lengthRead <= 0) {
					System.out.println("!! readBytes ERROR ERROR ERROR");
				}
				else {
					//byteBuffer.put(b[0]);
					data[off] = b[0];
					len -= lengthRead;
					off += lengthRead;
				}
			}
			if (debug) {
				for (int i=0; i<data.length; i++) {
					System.out.println(String.format("%d - %d", i, data[i]));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	} // End of readBytes

	/**
	 * Read a line of text from the incoming serial port connected to Serial RF24
	 * until a termination character (\n) is found.  Return the resulting line.
	 * @return A line of text received from Serial RF24.
	 */
	private String readLineRaw() {
		try {
			byte b[] = new byte[1];
			StringBuffer result = new StringBuffer();
			while (true) {
				int lengthRead = port.readBytes(b, 1);
				if (lengthRead==-1) {
					if (debug) {
						System.out.println("ERROR!");
					}
					return "";
				}
				char charRead = (char)b[0];
				if (charRead == '\n') {
					if (debug) {
						System.out.println("!! Returning: " + result);
					}
					return result.toString();
				}
				result.append(charRead);
				//System.out.println("!! Data size: " + lengthRead);
				//System.out.print(charRead + " - " + (byte)charRead);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	} // End of readLineRaw

	private boolean checkOK() throws RF24Exception {
		while (true) {
			String line = readLineRaw();
			if (line.length() == 0) {
				//System.out.println("!! Line from Serial NRF24 is empty ");
				return false;
			}
			//System.out.println("!! Line from Serial NRF24 > " + line);
			if (line.startsWith("OK")) {
				return true;
			}
			if (line.startsWith("FAIL")) {
				throw new RF24Exception(line);
			}
			//System.out.println("!! Discarded -> " + line);
		}
	} // End of checkOK
	
	/**
	 * Dump the serial settings that we are currently using.
	 */
	private void dumpSerial() {
		System.out.println("Data Bits: " + port.getNumDataBits());
		System.out.print("Parity: ");
		switch(port.getParity()) {
		case SerialPort.NO_PARITY:
			System.out.println("NO_PARITY");
			break;
		case SerialPort.EVEN_PARITY:
			System.out.println("EVEN_PARITY");
			break;
		case SerialPort.ODD_PARITY:
			System.out.println("ODD_PARITY");
			break;
		case SerialPort.MARK_PARITY:
			System.out.println("MARK_PARITY");
			break;
		case SerialPort.SPACE_PARITY:
			System.out.println("SPACE_PARITY");
			break;				
		}
		System.out.println("Stop Bits: " + port.getNumStopBits());
	} // End of dumpSerial
} // End of class
// End of file