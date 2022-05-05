package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler extends Thread {
	private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

	private Socket connection;

	public RequestHandler(Socket connectionSocket) {
		this.connection = connectionSocket;
	}

	public void run() {
		log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
				connection.getPort());

		try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
			/**
			 * 요구사항 1처리 InputStream은 처리가 힘드니 BufferedReader로 바꿔서 처리
			 */
			BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			// 테스트용도로 찍어보자~!
			String line = br.readLine();
			
			log.debug("request line : {}", line);
			String[] tmp = line.split(" ");
			String rootFilePath = System.getProperty("user.dir");
			String htmlPath = rootFilePath +"\\webapp" + tmp[1];
			while( !line.equals("") ) {
				line = br.readLine();
				log.debug("header : {} ", line);
			}
			DataOutputStream dos = new DataOutputStream(out);
			byte[] body = "Hello World".getBytes();
			body = Files.readAllBytes(new File(htmlPath).toPath());
			response200Header(dos, body.length);
			responseBody(dos, body);
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
		try {
			dos.writeBytes("HTTP/1.1 200 OK \r\n");
			dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
			dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	private void responseBody(DataOutputStream dos, byte[] body) {
		try {
			dos.write(body, 0, body.length);
			dos.flush();
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
}
