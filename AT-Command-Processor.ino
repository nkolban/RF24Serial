/**
 * AT Command processor for nRF24L01 via a serial port.
 * 
 * Take incoming commands from the Serial port and process those commands
 * through an nRF24L01.
 * 
 * The defined commands are:
 * - AT
 * - AT+RATE={1|2} // 1 = 1 MBps, 2 = 2 MBps
 * - AT+DIR={TX|RX}
 * - AT+RXADDR=Up to 5 characters (5*8 = 40 bits)
 * - AT+TXADDR=Up to 5 characters (5*8 = 40 bits)
 * - AT+CHANNEL={0-127}
 * - AT+WRITE={length}
 * - AT+READ={length}
 * - AT+DUMP
 * - AT+PALEVEL={MIN|LOW|HIGH|MAX}
 * - AT+CARRIER
 * - AT+BAUD={rate}
 * 
 * Commands received will ALWAYS be responded to with either a line that starts
 * with OK or FAIL.  There may be other lines that might be discarded.
 * 
 * For commands that return data such as AT+READ, the expected data is returned
 * first.
 * 
 * The primary logic of this app is that we receive a line of incoming data and
 * then perform a series "if then else if ..." switches over the content.  There
 * are alternatives to this style such as a switch table but I don't believe
 * they offer much (if any) of an execution advantage.  The down-side of the
 * current mechanism is that it "looks" like spaghetti but is in fact just
 * if/then/else if repeated over and over.
 * 
 * Neil Kolban (kolban1@kolban.com)
 * 2015-06-16
 * 
 */

#include <SPI.h>
#include <printf.h>
#include "RF24.h"

#define CE_PIN    10
#define CS_PIN    9
#define MAX_TXMIT 32
#define MAX_ADDR  5

#define TERMINATE_CHAR '#'


// Comment/uncomment the following line to add debugging.
//#define DEBUG 1

RF24 nRF24(CE_PIN, CS_PIN);

void setup()
{
	Serial.begin(9600);
	printf_begin();
	Serial.setTimeout(30*1000L); // Timeout in msecs
	nRF24.begin();
	nRF24.setPALevel(RF24_PA_LOW);
	//nRF24.setAddressWidth(5);
} // End of setup

void loop()
{
	runCommand();
} // End of loop

void runCommand() {
	if (!Serial.available()) {
		return;
	}
	String line = Serial.readStringUntil(TERMINATE_CHAR);
	if (line.length() == 0) {
		return;
	}
	//Serial.print("We got the following line - ");
	//Serial.println("\"" + line + "\"");
	if (line.length() == 0) {
		sayOK();
		return;
	}
	if (line.compareTo("AT") == 0) {
		sayOK();
	}
	//
	// AT+RATE
	//
	else if (line.startsWith("AT+RATE=")) {
		String param = getParameter(line);
		if (param.compareTo("1") == 0) {
#ifdef DEBUG
	Serial.println(">>> setDataRate(RF24_1MBPS)");
#endif			
			if (nRF24.setDataRate(RF24_1MBPS)) {
				sayOK();
				return;
			}
			sayFail("setDataRate to 1MBPs");
			return;
		} else if (param.compareTo("2") == 0) {
#ifdef DEBUG
	Serial.println(">>> setDataRate(RF24_2MBPS)");
#endif
			if (nRF24.setDataRate(RF24_2MBPS)) {
				sayOK();
				return;
			}
			sayFail("setDataRate to 2MBPs");
			return;
		}
	} // End of AT+RATE
	//
	// AT+DIR
	//
	else if (line.startsWith("AT+DIR=")) {
		String param = getParameter(line);
		if (param.compareTo("TX") == 0) {
			nRF24.stopListening();
#ifdef DEBUG
	Serial.println(">>> stopListening()");
#endif	
			sayOK();
			return;
		} else if (param.compareTo("RX") == 0) {
			nRF24.startListening();
#ifdef DEBUG
	Serial.println(">>> startListening()");
#endif			
			sayOK();
			return;
		}
	} // End of AT+DIR
	//
	// AT+CHANNEL
	//
	else if (line.startsWith("AT+CHANNEL=")) {
		String param = getParameter(line);
		int channel=param.toInt();
		if (channel >=0 && channel <= 127) {
			nRF24.setChannel((uint8_t)channel);
#ifdef DEBUG
	Serial.println(">>> setChannel(" + param + ")");
#endif				
			sayOK();
			return;
		}
		sayFail("Invalid channel");
		return;
	} // End of AT+CHANNEL
	//
	// AT+RXADDR
	//
	else if (line.startsWith("AT+RXADDR=")) {
		byte addr[8]; // 8 bytes * 8 bits = 64 bits
		String param = getParameter(line);
		memset(addr, 0, sizeof(addr));
		param.getBytes(addr, MAX_ADDR+1);
		nRF24.openReadingPipe(1, *(uint64_t *)addr);
#ifdef DEBUG
		Serial.println(">>> openReadingPipe(1," + param + ")");
#endif			
		sayOK();
		return;
	} // End of AT+RXADDR
	//
	// AT+TXADDR
	//
	else if (line.startsWith("AT+TXADDR=")) {
		byte addr[8];
		String param = getParameter(line);
		memset(addr, 0, sizeof(addr));
		param.getBytes(addr, MAX_ADDR+1);
		nRF24.openWritingPipe(*(uint64_t *)addr);
#ifdef DEBUG
	Serial.println(">>> openWritingPipe(" + param + ")");
#endif			
		sayOK();
		return;
	} // End of AT+TXADDR
	//
	// AT+WRITE
	//
	else if (line.startsWith("AT+WRITE=")) {
		char data[MAX_TXMIT];
		String param = getParameter(line);
		int length=param.toInt();
		if (length > 0 && length <= MAX_TXMIT) {
			if (Serial.readBytes(data, length) == length)
			{
				nRF24.write(data, length);
#ifdef DEBUG
				Serial.println(">>> write()");
#endif				
				sayOK();
				return;
			}
			else {
				sayFail("Failed to read expected number of bytes");
			}
		} else {
			sayFail("Invalid length (must be > 0 and <= 32)");
		}
		return;
	} // End of AT+WRITE
	//
	// AT+READ
	//
	else if (line.startsWith("AT+READ=")) {
		char data[MAX_TXMIT];
		String param = getParameter(line);
		int length=param.toInt();
		if (length > 0 && length <= MAX_TXMIT) {
			long startTime=millis();
			while(millis()-startTime < 10000 && nRF24.available() == false) {
				// do nothing
			}
			if (nRF24.available() == true) {
				nRF24.read(data, length);
				Serial.write((uint8_t *)data, length);
#ifdef DEBUG
	Serial.println(">>> read()");
#endif				
				sayOK();
				return;
			} else {
				memset(data, 'X', length);
				Serial.write((uint8_t *)data, length);
				sayFail("No data available");
			}
		} else {
			sayFail("Invalid length (must be > 0 and <= 32)");
		}
		return;
	} // End of AT+READ
	//
	// AT+DUMP
	//
	else if (line.startsWith("AT+DUMP")) {
		nRF24.printDetails();
#ifdef DEBUG
	Serial.println(">>> dump()");
#endif		
		sayOK();
		return;
	} // End of AT+DUMP
	//
	// AT+PALEVEL
	//
	else if (line.startsWith("AT+PALEVEL=")) {
		String param = getParameter(line);
		if (param.compareTo("MIN") == 0) {
			nRF24.setPALevel(RF24_PA_MIN);
#ifdef DEBUG
	Serial.println(">>> setPALevel(LOW)");
#endif				
			sayOK();
			return;			
		} else if (param.compareTo("LOW") == 0) {
			nRF24.setPALevel(RF24_PA_LOW);
#ifdef DEBUG
	Serial.println(">>> setPALevel(LOW)");
#endif				
			sayOK();
			return;
		} else if (param.compareTo("HIGH") == 0) {
			nRF24.setPALevel(RF24_PA_HIGH);
#ifdef DEBUG
	Serial.println(">>> setPALevel(LOW)");
#endif				
			sayOK();
			return;			
		} else if (param.compareTo("MAX") == 0) {
			nRF24.setPALevel(RF24_PA_MAX);
#ifdef DEBUG
	Serial.println(">>> setPALevel(LOW)");
#endif				
			sayOK();
			return;			
		} else {
			sayFail("Unknown PA Level");
			return;
		}
	} // End of AT+PALEVEL
	//
	// AT+CARRIER
	//
	else if (line.startsWith("AT+CARRIER")) {
		if (nRF24.testCarrier()) {
			sayOK();
			return;
		}
		sayFail("No carrier");
		return;
	} // End of AT+CARRIER
	//
	// AT+BAUD
	//
	else if (line.startsWith("AT+BAUD=")) {
		String param = getParameter(line);
		Serial.end();
		long speed = param.toInt();
		Serial.begin(speed);
		sayOK();
		return;
	} // End of AT+BAUD
	//
	// Don't know what the command is
	//
	else {
		sayFail("Unrecognized command");
		return;
	}
	return;
} // End of runCommand

/**
 * Parse the line and return all the data after the first '=' character.
 */
String getParameter(String line) {
	return line.substring(line.indexOf('=') + 1);
} // End of getParameter

/**
 * Respond with OK
 */
void sayOK() {
	Serial.print("OK\n");
	//Serial.flush();
} // End of sayOK

/**
 * Respond with FAIL
 */
void sayFail(String message) {
	Serial.print("FAIL: " + message + "\n");
} // End of sayFail
// End of file