package org.opencb.opencga.server.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.opencb.opencga.account.CloudSessionManager;
import org.opencb.opencga.account.io.IOManagementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.opencb.cellbase.core.lib.dbquery.QueryOptions;
import org.opencb.commons.containers.QueryResponse;
import org.opencb.opencga.lib.common.Config;

@Path("/")
public class GenericWSServer {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());
    protected static Properties properties;
    protected static Config config;

    protected UriInfo uriInfo;
    protected String accountId;
    protected String sessionIp;

    // Common input arguments
    protected MultivaluedMap<String, String> params;
    protected QueryOptions queryOptions;
    
    // Common output members
    protected String outputFormat;
    protected long startTime;
    protected long endTime;
    
    protected static ObjectWriter jsonObjectWriter;
    protected static ObjectMapper jsonObjectMapper;

    //General params
    @DefaultValue("")
    @QueryParam("sessionid")
    protected String sessionId;

    @DefaultValue("json")
    @QueryParam("of")
    protected String of;


    /**
     * Only one CloudSessionManager
     */
    protected static CloudSessionManager cloudSessionManager;

    static {
        try {
            cloudSessionManager = new CloudSessionManager();
        } catch (IOException | IOManagementException e) {
            e.printStackTrace();
        }

        jsonObjectMapper = new ObjectMapper();
        jsonObjectWriter = jsonObjectMapper.writer();

        InputStream is = CloudSessionManager.class.getClassLoader().getResourceAsStream("application.properties");
        properties = new Properties();
        try {
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public GenericWSServer(@Context UriInfo uriInfo, @Context HttpServletRequest httpServletRequest) throws IOException {
        this.startTime = System.currentTimeMillis();
        this.uriInfo = uriInfo;
        this.params = this.uriInfo.getQueryParameters();
        this.queryOptions = new QueryOptions();
        parseCommonQueryParameters(this.params);
        
//        this.sessionId = (this.params.get("sessionid") != null) ? this.params.get("sessionid").get(0) : "";
//        this.of = (this.params.get("of") != null) ? this.params.get("of").get(0) : "";
        this.sessionIp = httpServletRequest.getRemoteAddr();

//		UserAgent userAgent = UserAgent.parseUserAgentString(httpServletRequest.getHeader("User-Agent"));
//
//		Browser br = userAgent.getBrowser();
//
//		OperatingSystem op = userAgent.getOperatingSystem();

        logger.debug(uriInfo.getRequestUri().toString());
        // logger.info("------------------->" + br.getName());
        // logger.info("------------------->" + br.getBrowserType().getName());
        // logger.info("------------------->" + op.getName());
        // logger.info("------------------->" + op.getId());
        // logger.info("------------------->" + op.getDeviceType().getName());

        File dqsDir = new File(properties.getProperty("DQS.PATH"));
        if (dqsDir.exists()) {
            File accountsDir = new File(properties.getProperty("ACCOUNTS.PATH"));
            if (!accountsDir.exists()) {
                accountsDir.mkdir();
            }
        }
    }

    
    /**
     * This method parse common query parameters from the URL
     *
     * @param multivaluedMap
     */
    private void parseCommonQueryParameters(MultivaluedMap<String, String> multivaluedMap) {
        queryOptions.put("exclude", (multivaluedMap.get("exclude") != null) ? multivaluedMap.get("exclude").get(0) : "");
        queryOptions.put("include", (multivaluedMap.get("include") != null) ? multivaluedMap.get("include").get(0) : "");
        queryOptions.put("metadata", (multivaluedMap.get("metadata") != null) ? multivaluedMap.get("metadata").get(0).equals("true") : true);

        outputFormat = (multivaluedMap.get("of") != null) ? multivaluedMap.get("of").get(0) : "json";
    }

    
    
    @GET
    @Path("/echo/{message}")
    public Response echoGet(@PathParam("message") String message) {
        logger.info(sessionId);
        logger.info(of);
        return createOkResponse(message);
    }

    protected Response createErrorResponse(Object o) {
        String objMsg = o.toString();
        if (objMsg.startsWith("ERROR:")) {
            return buildResponse(Response.ok("" + o));
        } else {
            return buildResponse(Response.ok("ERROR: " + o));
        }
    }

    protected Response createOkResponse(Object obj) {
        switch (outputFormat.toLowerCase()) {
            case "json":
                return createJsonResponse(obj);
            case "xml":
                return createOkResponse(obj, MediaType.APPLICATION_XML_TYPE);
            default:
                return buildResponse(Response.ok(obj));
        }
    }

    protected Response createJsonResponse(Object obj) {
        endTime = System.currentTimeMillis() - startTime;
        QueryResponse queryResponse = new QueryResponse();
        queryResponse.put("time", endTime);
        queryResponse.put("version", (params.get("version") != null) ? params.get("version") : null);
        queryResponse.put("species", (params.get("species") != null) ? params.get("species") : null);
        queryResponse.put("queryOptions", queryOptions);
        queryResponse.put("response", obj);

        try {
            return buildResponse(Response.ok(jsonObjectWriter.writeValueAsString(queryResponse), MediaType.APPLICATION_JSON_TYPE));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.error("Error parsing queryResponse object");
            return null;
        }
    }

    protected Response createOkResponse(Object o1, MediaType o2) {
        return buildResponse(Response.ok(o1, o2));
    }

    protected Response createOkResponse(Object o1, MediaType o2, String fileName) {
        return buildResponse(Response.ok(o1, o2).header("content-disposition", "attachment; filename =" + fileName));
    }

    private Response buildResponse(ResponseBuilder responseBuilder) {
        return responseBuilder.header("Access-Control-Allow-Origin", "*").build();
    }
}
