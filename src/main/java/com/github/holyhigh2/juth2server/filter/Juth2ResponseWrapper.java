package com.github.holyhigh2.juth2server.filter;

import com.github.holyhigh2.juth2server.Juth2Log;
import org.apache.commons.codec.binary.Base64;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * 拦截response回写接口，用于记录日志
 *
 * @author holyhigh https://github.com/holyhigh2
 */
class Juth2ResponseWrapper extends HttpServletResponseWrapper {
    private String uri;
    private String refer;
    private String origin;
    private String auth;

    public Juth2ResponseWrapper(HttpServletResponse response,HttpServletRequest request) {
        super(response);
        uri = request.getRequestURI();
        origin = request.getHeader("Origin");
        refer = request.getHeader("Referer");
        auth = request.getHeader("Authorization");
        if(auth != null){
            auth = auth.split("\\.")[1];
            auth = new String(Base64.decodeBase64(auth));
        }
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        if(msg!=null && msg.startsWith("Juth2-Server: ")){
            Juth2Log.warning(msg+" | "+uri+" | "+(origin==null?refer:origin)+" | "+auth);
        }

        super.sendError(sc, msg);
    }
}
