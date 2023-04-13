import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.*;

/**
 * MessageReceiver implements a message queue that uses UDP sockets to receive messages on a specific port.
 * The main class is MessageQueue, which contains an ArrayList of received messages and a constructor
 * that starts a new thread to receive incoming messages.
 */
public class MessageReceiver {
    public final PriorityQueue<byte[]> receivedMessages;

    public MessageReceiver(int portNum) throws SocketException {
        this.receivedMessages = new PriorityQueue<>(Building.MAX_ELEVATOR_MESSAGE, (msg1, msg2) -> {
            int priority1 = msg1[0];
            int priority2 = msg2[0];
            return Integer.compare(priority1, priority2);
        });

        Thread thread = new Thread(new Receiver(portNum, receivedMessages));
        thread.start();
    }

    /**
     * Returns the oldest received message from the queue, blocking if necessary until a message is available.
     *
     * @return byte[] message
     * @throws InterruptedException
     */
    public byte[] getMessage() throws InterruptedException {
        synchronized(receivedMessages) {
            while(receivedMessages.isEmpty()) {
                receivedMessages.wait();
            }

            return receivedMessages.poll();
        }
    }

    /**
     * Returns true if the message queue is currently empty else false.
     * @return boolean isEmpty
     */
    public boolean receivedMessagesIsEmpty() {
        synchronized(receivedMessages) {
            return receivedMessages.isEmpty();
        }
    }
}

/**
 * The Receiver class implements the receiving functionality. It creates a new DatagramSocket bound to the specified port number,
 * and continuously receives incoming messages using the receive() method. When a new message is received, it is added to the
 * receivedMessages list in the MessageQueue object and a notification is sent to any waiting threads using notifyAll().
 */
class Receiver implements Runnable {
    private int portNum;
    private DatagramPacket receivePacket;
    private DatagramSocket receiveSocket;
    public final PriorityQueue<byte[]> receivedMessages;

    public Receiver(int portNum, PriorityQueue<byte[]> receivedMessages) throws SocketException {
        this.portNum = portNum;
        this.receivedMessages = receivedMessages;
        receiveSocket = new DatagramSocket(portNum);
    }


    /**
     * Receives incoming messages and reply with an acknowledgement
     */
    public void receive() throws IOException {
        byte msg[] = new byte[1000];
        receivePacket = new DatagramPacket(msg, msg.length);
        receiveSocket.receive(receivePacket);

        synchronized(receivedMessages) {
            receivedMessages.add(msg);
            receivedMessages.notifyAll();
        }

        byte[] ackMsg = new byte[] {Building.ACKNOWLEDGE};
        DatagramPacket ackPacket = new DatagramPacket(ackMsg, ackMsg.length, receivePacket.getAddress(), receivePacket.getPort());
        DatagramSocket sendSocket = new DatagramSocket();
        sendSocket.send(ackPacket);
        System.out.println("Receiver: Message received and ack packet sent");
    }

    @Override
    public void run() {
        while(true) {
            try {
                receive();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
