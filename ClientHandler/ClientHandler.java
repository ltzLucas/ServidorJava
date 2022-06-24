package br.edu.up.SocketServer.ClientHandler;

// 1. Open a socket.
// 2. Open an input stream and output stream to the socket.
// 3. Read from and write to the stream according to the server's protocol.
// 4. Close the streams.
// 5. Close the socket.

import java.io.*;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import br.edu.up.SocketServer.Model.ErrorModel;
import br.edu.up.SocketServer.Model.MessageModel;

public class ClientHandler implements Runnable {

    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();

    private Socket socket;
    public DataInputStream dataInputStream;
    public DataOutputStream dataOutputStream;
    private String ip;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.ip = socket.getInetAddress().getHostAddress();
            this.dataInputStream = new DataInputStream(socket.getInputStream());
            this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
            clientHandlers.add(this);

            MessageModel message = new MessageModel();
            message.Identificador = "Servidor";
            message.Mensagem = "Novo usuario conectado!";
            message.Data = "23/06/2022";

            broadcastMessage(message.toString());
        } catch (IOException e) {
            closeEverything(socket, dataInputStream, dataOutputStream);
        }
    }

    @Override
    public void run() {
        String messageFromClient;

        while (socket.isConnected()) {
            try {
                messageFromClient = dataInputStream.readUTF();

                JSONObject jsonObject = new JSONObject(messageFromClient);

                ErrorModel errorModel = null;
                if (jsonObject.getString("Identificador") == "") {
                    errorModel = new ErrorModel("Identificador não fornecido", 2);
                } else if (jsonObject.getString("Mensagem") == "") {
                    errorModel = new ErrorModel("Mensagem não fornecida", 4);
                } else if (jsonObject.getString("Data") == "") {
                    errorModel = new ErrorModel("Data não fornecida", 3);
                }
                if (errorModel != null) {
                    this.dataOutputStream.writeUTF(errorModel.toString());
                }

                broadcastMessage(messageFromClient);
            } catch (JSONException e) {
                ErrorModel errorModel = new ErrorModel("Json mal formatado.", 1);
                try {
                    this.dataOutputStream.writeUTF(errorModel.toString());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            } catch (IOException e) {
                closeEverything(socket, dataInputStream, dataOutputStream);
                break;
            }
        }
    }

    public void broadcastMessage(String messageToSend) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if (!clientHandler.ip.equals(ip)) {
                    System.out.println(messageToSend);
                    clientHandler.dataOutputStream.writeUTF(messageToSend);
                }
            } catch (IOException e) {
                System.out.println(e);
                closeEverything(socket, dataInputStream, dataOutputStream);
            }
        }
    }

    public void removeClientHandler() {
        clientHandlers.remove(this);
        broadcastMessage("SERVIDOR: O " + ip + " saiu do chat!");
    }

    public void closeEverything(Socket socket, DataInputStream bufferedReader, DataOutputStream bufferedWriter) {
        removeClientHandler();
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClientHandler();
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
