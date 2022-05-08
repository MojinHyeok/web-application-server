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
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.User;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
	private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

	private Socket connection;

	public RequestHandler(Socket connectionSocket) {
		this.connection = connectionSocket;
	}

	public void run() {
		log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
				connection.getPort());

		/*
		 * InputStream 서버의 입장에서 클라이언트가 전송하는 데이터는 InputStream을 활용하여 읽을 수 있습니다.
		 * OutputStream 서버가 클라이언트에게 전송해야하는 데이터들을 OutputStream을 활용하여 전달합니다.
		 */
		try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
			/**
			 * 요구사항 1처리 InputStream은 처리가 힘드니 BufferedReader로 바꿔서 처리
			 */
			BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			// 테스트용도로 찍어보자~!
			String line = br.readLine();
			log.debug("request line : {}", line);
			String url = HttpRequestUtils.getURl(line);
			Map<String, String> headers = new HashMap<String, String>();
			while( !"".equals(line) ) {
				log.debug("header : {}", line);
				line =br.readLine();
				String[] headerTokens = line.split(": ");
				if(headerTokens.length ==2) {
					
					headers.put(headerTokens[0], headerTokens[1]);
				}
			}
			
			if(url.startsWith("/user/create")) {
				
				String requestBody = IOUtils.readData(br, Integer.parseInt(headers.get("Content-Length")));
				log.debug("Request Body : {}",requestBody);
				Map<String, String> params = HttpRequestUtils.parseQueryString(requestBody);
				User user = new User(params.get("userId"), params.get("password"), params.get("name"), params.get("email"));
				log.debug("User  : {} ",user);

				DataOutputStream dos = new DataOutputStream(out);
				response302Header(dos);
			} else {
				
				DataOutputStream dos = new DataOutputStream(out);
				byte[] body = Files.readAllBytes(new File("./webapp"+ url).toPath());
				response200Header(dos, body.length);
				responseBody(dos, body);
			}
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	/*
	 * 302의 경우 특정url로 이동시키기 때문에 바디정보나 이런것들은 별로 필요로 하지 않습니다.
	 * 특정 url정보만 필요로 함.
	 */
	private void response302Header(DataOutputStream dos) {
		try {
			dos.writeBytes("HTTP/1.1 302 Found \r\n");
			dos.writeBytes("Location : /index.html\r\n");
			dos.writeBytes("\r\n");
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
