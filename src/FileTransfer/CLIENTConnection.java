package FileTransfer;

import java.io.*;
import java.net.*;
import java.util.Date;

public class CLIENTConnection implements Runnable {

    private Socket clientSocket;
    ObjectOutputStream out;
    ObjectInputStream in;
    String message = "";

    public CLIENTConnection(Socket client) {
        this.clientSocket = client;
    }

    @Override
    public void run() {
        try {
            System.out.println("Thread started with name: " + Thread.currentThread().getName());
//            try {
//                readResponse();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            respond();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readResponse() throws IOException, InterruptedException {
        String userInput;
        String result = "";
        InputStream in = clientSocket.getInputStream();
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(in));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        while ((userInput = stdIn.readLine()) != null) {
            if (userInput.contains("####")) { //här matchar vi
                System.out.println("client> " + userInput);
                out.write("Hello phone");
                out.newLine();
                out.flush();
                break;
            }
            result = result.concat(userInput);
        }
        System.out.println(result);
    }

    private void respond() throws IOException{
        out = new ObjectOutputStream(clientSocket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(clientSocket.getInputStream());
        do{
            try {
                message = (String) in.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            System.out.println("client>" + message);
            if(message.contains("####")){
                out.writeObject("bye");
            out.writeObject("hello phone");
            out.flush();
            out.close();
                break;
            }

        } while(!message.isEmpty());

    }
}
    /*
            in = new BufferedReader(new InputStreamReader(
                    clientSocket.getInputStream()));
//            String clientSelection;
            int selection;
            String clientSelection = in.readLine();
            if(clientSelection.equals("1")){
                selection = 1;
            } else if (clientSelection.equals("2")){
                selection = 2;
            } else selection = 0;
//            while ((clientSelection = in.readLine()) != null) {
//            while((selection != 0){
                switch (selection) {
                    case 1:
                        receiveFile();
                        break;
                    case 2:
                        String outGoingFileName;
                        while ((outGoingFileName = in.readLine()) != null) {
                            sendFile(outGoingFileName);
                        }

                        break;
                    default:
                        System.out.println("Incorrect command received.");
                        break;
                }
                in.close();
//                break;
//            }

        } catch (IOException ex) {
            Logger.getLogger(CLIENTConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveFile() {
        try {
            int bytesRead;

            DataInputStream clientData = new DataInputStream(clientSocket.getInputStream());

            String fileName = clientData.readUTF();
            OutputStream output = new FileOutputStream(("received_from_client_" + fileName));
            long size = clientData.readLong();
            byte[] buffer = new byte[1024];
            while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                output.write(buffer, 0, bytesRead);
                size -= bytesRead;
            }

            output.close();
            clientData.close();

            System.out.println("File "+fileName+" received from client.");
        } catch (IOException ex) {
            System.err.println("Client error. Connection closed.");
        }
    }

    public void sendFile(String fileName) {
        try {
            //handle file read
            File myFile = new File(fileName);
            byte[] mybytearray = new byte[(int) myFile.length()];

            FileInputStream fis = new FileInputStream(myFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            //bis.read(mybytearray, 0, mybytearray.length);

            DataInputStream dis = new DataInputStream(bis);
            dis.readFully(mybytearray, 0, mybytearray.length);

            //handle file send over socket
            OutputStream os = clientSocket.getOutputStream();

            //Sending file name and file size to the server
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeUTF(myFile.getName());
            dos.writeLong(mybytearray.length);
            dos.write(mybytearray, 0, mybytearray.length);
            dos.flush();
            System.out.println("File "+fileName+" sent to client.");
        } catch (Exception e) {
            System.err.println("File does not exist!");
        }
    }
}
    */