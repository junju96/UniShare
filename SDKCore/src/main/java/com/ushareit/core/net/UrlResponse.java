package com.ushareit.core.net;

import com.ushareit.core.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import okhttp3.Response;

public class UrlResponse {
    private Map<String, List<String>> headers;

    private String content;
    private int statusCode;
    private String statusMessage;

    UrlResponse(Response response) throws IOException {
        headers = response.headers().toMultimap();
        statusCode = response.code();
        statusMessage = response.message();
        try {
            content = response.body().string();
        }catch (NullPointerException e) {
            throw new IOException("response body is null");
        }
    }

    UrlResponse(HttpURLConnection conn) throws IOException {
        headers = conn.getHeaderFields();
        statusCode = conn.getResponseCode();
        statusMessage = conn.getResponseMessage();

        InputStream input = null;
        try {
            try {
                input = conn.getInputStream();
            } catch (IOException e) {
                input = conn.getErrorStream();
            }
            if (input != null)
                content = Utils.inputStreamToString(input, true);
        } finally {
            Utils.close(input);
        }
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UrlResponse [statusCode=").append(statusCode).append(", statusMessage=")
                .append(statusMessage).append(",content=").append(content).append("]");
        return builder.toString();
    }
}
