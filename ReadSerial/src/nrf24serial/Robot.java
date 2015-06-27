package nrf24serial;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.fazecast.jSerialComm.SerialPort;

public class Robot {
	private SerialPort port;

	private class Telemetry {
		private float speedMotorA;
		private float speedMotorB;
		private short powerMotorA;
		private short powerMotorB;
		private short targetSpeedMotorA;
		private short targetSpeedMotorB;

		public String toString() {
			return String.format("speed-A: %f, speed-B: %f, power-A: %d, power-B: %d, target-A: %d, target-B: %d", //
					speedMotorA, speedMotorB, powerMotorA, powerMotorB, targetSpeedMotorA, targetSpeedMotorB);
		}
	};

	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Usage: <cmd> {COMPORT}");
			return;
		}
		String args2[] = {"COM15"};
		Robot main = new Robot(args2);
		main.run();
	}

	public Robot(String args[]) {
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
			nrf24Serial.setRate(1);
			nrf24Serial.setRXAddr("NODE2");
			nrf24Serial.startListening();
			// nrf24Serial.dump();
			byte data[] = new byte[16];
			Telemetry telemetry = new Telemetry();
			while (true) {
				nrf24Serial.read(data);
				ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
				telemetry.speedMotorA = bb.getFloat();
				telemetry.speedMotorB = bb.getFloat();
				telemetry.powerMotorA = bb.getShort();
				telemetry.powerMotorB = bb.getShort();
				telemetry.targetSpeedMotorA = bb.getShort();
				telemetry.targetSpeedMotorB = bb.getShort();
				System.out.println("Telemetry: " + telemetry.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
} // End of class
// End of file