package com.example.myapplication;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketClient {
    private String address; // ip адрес сервера, который принимает соединения
    private int port; // номер порта, на который сервер принимает соединения
    private Socket sock_desc = null; // сокет, через которий приложения общается с сервером

    SocketClient(String address, int port) {
        this.address = address;
        this.port = port;
    }


    /**
     *  Открытие нового соединения. Если сокет уже открыт, то он закрывается.
     */
    boolean openConnection() {
            /* Освобождаем ресурсы */
        closeConnection();

        try {
            /* Создаем новый сокет. Указываем на каком ip и порту запущен сервер. */
            sock_desc = new Socket();
            sock_desc.connect(new InetSocketAddress(address, port), 1000);
            sock_desc.setSoTimeout(1000);
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    byte[] getData() {
        byte count = 0;
        byte[] data = new byte[4];
        data[0] = data[1] = data[2] = data[3] = (byte) 254;

        while ( count < 127 ) {
            if (isConnected()) {
                try {
                    sendData(data);
                    sock_desc.getInputStream().read(data);
                    return data;
                } catch (Exception e) {
                }
            }
            count++;
        }
        data[0] = data[1] = data[2] = data[3] = (byte) 253;
        return data;
    }


    /**
     * Метод для закрытия сокета, по которому мы общались.
     */
    void closeConnection() {

        /* Проверяем сокет. Если он не зарыт, то закрываем его и освобдождаем соединение.*/
        if ( isConnected() ) {
            try {
                sock_desc.close();
            } catch (IOException e) {
            }
            finally {
                sock_desc = null;
            }
        }
        sock_desc = null;
    }


    /**
     * Метод для отправки данных по сокету.
     */
    boolean sendData(byte[] data) {
        /* Проверяем сокет. Если он не создан или закрыт, то выдаем исключение */
        if (sock_desc == null || sock_desc.isClosed()) {
            //throw new Exception("Невозможно отправить данные. Сокет не создан или закрыт");
            return false;
        }

        /* Отправка данных */
        try {
            sock_desc.getOutputStream().write(data);
            sock_desc.getOutputStream().flush();
            return true;
        }
        catch (IOException exception) {
            //closeConnection();
            //throw new Exception("Невозможно отправить данные: "+e.getMessage());
        }
        return false;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        closeConnection();
    }

    boolean isConnected() {
        return sock_desc != null && !sock_desc.isClosed();
    }
}
