import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class Server implements Runnable {

    Vector<Service> allV;
    Vector<Service> waitV;
    Vector<Room> roomV;


    public Server() {
        allV = new Vector<>();
        waitV = new Vector<>();
        roomV = new Vector<>();

        new Thread(this).start();
    }//생성자


    @Override
    public void run() {
        try {
            ServerSocket ss = new ServerSocket(5000);

            System.out.println("Start Server.......");
            while (true) {
                Socket s = ss.accept();
                Service ser = new Service(s, this);
                //allV.add(ser);//전체사용자에 등록
                //waitV.add(ser);//대기실사용자에 등록
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }//run

    public static void main(String[] args) {
        new Server();
    }


}