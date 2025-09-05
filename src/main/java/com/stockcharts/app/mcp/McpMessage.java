package com.stockcharts.app.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpMessage {
    private String jsonrpc = "2.0";
    private String id;
    private String method;
    private JsonNode params;
    private JsonNode result;
    private McpError error;

    public McpMessage() {}

    public static McpMessage request(String id, String method, JsonNode params) {
        McpMessage msg = new McpMessage();
        msg.id = id;
        msg.method = method;
        msg.params = params;
        return msg;
    }

    public static McpMessage response(String id, JsonNode result) {
        McpMessage msg = new McpMessage();
        msg.id = id;
        msg.result = result;
        return msg;
    }

    public static McpMessage error(String id, int code, String message) {
        McpMessage msg = new McpMessage();
        msg.id = id;
        msg.error = new McpError(code, message);
        return msg;
    }

    public String getJsonrpc() { return jsonrpc; }
    public void setJsonrpc(String jsonrpc) { this.jsonrpc = jsonrpc; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public JsonNode getParams() { return params; }
    public void setParams(JsonNode params) { this.params = params; }
    public JsonNode getResult() { return result; }
    public void setResult(JsonNode result) { this.result = result; }
    public McpError getError() { return error; }
    public void setError(McpError error) { this.error = error; }

    public static class McpError {
        private int code;
        private String message;

        public McpError() {}
        public McpError(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}