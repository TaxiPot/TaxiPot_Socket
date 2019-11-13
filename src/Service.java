import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Vector;

public class Service extends Thread {

    Room myRoom;

    BufferedReader in;
    OutputStream out;

    Vector<Service> allV;//모든 사용자(대기실사용자 + 대화방사용자)
    Vector<Service> waitV;//대기실 사용자
    Vector<Room> roomV;//개설된 대화방 Room-vs(Vector) : 대화방사용자

    Socket s;

    String nickName;

    public Service(Socket s, Server server) {
        allV = server.allV;
        waitV = server.waitV;
        roomV = server.roomV;

        this.s = s;

        try {
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            out = s.getOutputStream();

            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }//생성자

    @Override
    public void run() {
        try {

            while (true) {
                String msg = in.readLine();

                if (msg == null) return;

                if (msg.trim().length() > 0) {
                    System.out.println("from Client: " + msg + ":" +
                            s.getInetAddress().getHostAddress());

                    String msgs[] = msg.split("\\|");
                    String protocol = msgs[0];

                    switch (protocol) {
                        case "100":
                            allV.add(this);
                            waitV.add(this);
                            break;

                        case "150":
                            nickName = msgs[1];

                            messageWait("160|" + getRoomInfo());
                            messageWait("180|" + getWaitInwon());

                            break;
                        case "160": //방만들기 (대화방 입장)
                            myRoom = new Room();
                            myRoom.title = msgs[1];//방제목
                            myRoom.count = 1;
                            myRoom.boss = nickName;

                            roomV.add(myRoom);

                            //대기실----> 대화방 이동!!
                            waitV.remove(this);
                            myRoom.userV.add(this);

                            messageTo("205|test");

                            messageRoom("200|" + nickName);//방인원에게 입장 알림

                            //대기실 사용자들에게 방정보를 출력
                            //예) 대화방명:JavaLove
                            //-----> roomInfo(JList) :  JavaLove--1
                            messageWait("160|" + getRoomInfo());
                            messageWait("180|" + getWaitInwon());
                            break;

                        case "170": //(대기실에서) 대화방 인원정보
                            messageTo("170|" + getRoomInwon(msgs[1]));
                            break;

                        case "175": //(대화방에서) 대화방 인원정보
                            messageRoom("175|" + getRoomInwon());
                            break;

                        case "200": //방들어가기 (대화방 입장) ----> msgs[] = {"200","자바방"}
                            for (int i = 0; i < roomV.size(); i++) {//방이름 찾기!!
                                Room r = roomV.get(i);
                                if (r.title.equals(msgs[1])) {//일치하는 방 찾음!!
                                    myRoom = r;
                                    myRoom.count++;//인원수 1증가
                                    break;
                                }
                            }//for

                            //대기실----> 대화방 이동!!
                            waitV.remove(this);
                            myRoom.userV.add(this);

                            if (myRoom.userV.size() == 1) {
                                messageTo("205|CLEAR");
                            }

                            messageRoom("200|" + nickName);//방인원에게 입장 알림

                            //들어갈 방의 title전달
                            messageTo("202|" + myRoom.title);

                            messageWait("160|" + getRoomInfo());
                            messageWait("180|" + getWaitInwon());
                            break;

                        case "300": //메시지
                            messageRoom("300|[" + nickName + "]▶ " + msgs[1]);
                            //클라이언트에게 메시지 보내기
                            break;

                        case "400":
                            myRoom.count--;//인원수 감소

                            messageRoom("400|" + nickName);//방인원들에게 퇴장 알림!!

                            //대화방----> 대기실 이동!!
                            myRoom.userV.remove(this);
                            waitV.add(this);

                            if(myRoom.count == 0) {
                                roomV.remove(myRoom);
                            }

                            String[] roomList = getRoomInfo().split(",");

                            // 대기실에 유저정보 다시출력
                            messageWait("180|" + getWaitInwon());

                            //대화방 퇴장후 방인원 다시출력
                            messageRoom("175|" + getRoomInwon());

                            //대기실에 방정보 다시출력
                            messageWait("160|" + getRoomInfo());
                            break;

                        case "500":
                            allV.remove(this);
                            waitV.remove(this);

                            messageWait("180|" + getWaitInwon());
                            break;

                    }//서버 switch
                }//if
            }//while
        } catch (IOException e) {
            System.out.println("★");
            e.printStackTrace();
        }
    }//run

    public String getRoomInfo() {
        String str = "";
        for (int i = 0; i < roomV.size(); i++) {
            //"자바방--1,오라클방--1,JDBC방--1"
            Room r = roomV.get(i);
            str += r.title + "--" + r.count;
            if (i < roomV.size() - 1) str += ",";
        }
        return str;
    }//getRoomInfo

    public String getRoomInwon() {//같은방의 인원정보
        String str = "";
        for (int i = 0; i < myRoom.userV.size(); i++) {
            //"길동,라임,주원"
            Service ser = myRoom.userV.get(i);
            str += ser.nickName;
            if (i < myRoom.userV.size() - 1) str += ",";
        }
        return str;
    }//getRoomInwon

    public String getRoomInwon(String title) {//방제목 클릭시 방의 인원정보
        String str = "";

        for (int i = 0; i < roomV.size(); i++) {
            //"길동,라임,주원"
            Room room = roomV.get(i);
            if (room.title.equals(title)) {
                for (int j = 0; j < room.userV.size(); j++) {
                    Service ser = room.userV.get(j);
                    str += ser.nickName;
                    if (j < room.userV.size() - 1) str += ",";
                }
                break;
            }
        }
        return str;
    }//getRoomInwon

    public String getWaitInwon() {
        String str = "";
        for (int i = 0; i < waitV.size(); i++) {
            //"길동,라임,주원"
            Service ser = waitV.get(i);
            str += ser.nickName;
            if (i < waitV.size() - 1) str += ",";
        }
        return str;
    }//getWaitInwon

    public void messageAll(String msg) {//전체사용자
        //접속된 모든 클라이언트(대기실+대화방)에게 메시지 전달
        for (int i = 0; i < allV.size(); i++) {//벡터 인덱스
            Service service = allV.get(i); //각각의 클라이언트 얻어오기
            try {
                service.messageTo(msg);
            } catch (IOException e) {
                //에러발생 ---> 클라이언트 접속 끊음!!
                allV.remove(i--); //접속 끊긴 클라이언트를 벡터에서 삭제!!
                System.out.println("클라이언트 접속 끊음!!");
            }
        }
    }//messageAll

    public void messageWait(String msg) {//대기실 사용자
        for (int i = 0; i < waitV.size(); i++) {//벡터 인덱스
            Service service = waitV.get(i); //각각의 클라이언트 얻어오기
            try {
                service.messageTo(msg);
            } catch (IOException e) {
                //에러발생 ---> 클라이언트 접속 끊음!!
                waitV.remove(i--); //접속 끊긴 클라이언트를 벡터에서 삭제!!
                System.out.println("클라이언트 접속 끊음!!");
            }
        }
    }//messageWait

    public void messageRoom(String msg) {//대화방사용자
        for (int i = 0; i < myRoom.userV.size(); i++) {//벡터 인덱스
            Service service = myRoom.userV.get(i); //각각의 클라이언트 얻어오기
            try {
                service.messageTo(msg);
            } catch (IOException e) {
                //에러발생 ---> 클라이언트 접속 끊음!!
                myRoom.userV.remove(i--); //접속 끊긴 클라이언트를 벡터에서 삭제!!
                System.out.println("클라이언트 접속 끊음!!");
            }
        }
    }//messageAll

    public void messageTo(String msg) throws IOException {
        //특정 클라이언트에게 메시지 전달 (실제 서버--->클라이언트 메시지 전달)
        out.write((msg + "\n").getBytes());
    }
}