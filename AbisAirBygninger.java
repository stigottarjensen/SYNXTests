import java.net.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import org.json.*;

//java -cp .:json.jar:mssql.jar Tripletex2MSSQLServer.java

public class Tripletex2MSSQLServer {

    private enum HTTPMethod {
        GET, POST, PUT, DELETE
    }

    private String HTTPClient(Properties pr, HTTPMethod method, String tripletexUrl, String query, String body)
            throws Exception {
        if (query != null)
            tripletexUrl = tripletexUrl + "?" + query;
        URI uri = URI.create(tripletexUrl);
        URL Url = uri.toURL();
        URLConnection uc = Url.openConnection();
        uc.setRequestProperty("Authorization", pr.getProperty("Authorization"+test));
        uc.setDoInput(true);
        if (method == HTTPMethod.POST) {
            uc.setDoOutput(true);
            OutputStreamWriter osw = new OutputStreamWriter(uc.getOutputStream());
            osw.write(body);
            osw.close();
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }

    private int getIdFromJSON(JSONObject value, String element) {
        Object obj = value.get(element);
        if (obj instanceof JSONObject)
            return value.getJSONObject(element).getInt("id");
        else
            return 0;
    }

    private void InsertTimesheet(String json, Properties pr, int month, int year) throws Exception {
        JSONObject jo = new JSONObject(json);
        JSONArray ja = new JSONArray(jo.get("values").toString());

        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        Connection con = DriverManager.getConnection("jdbc:sqlserver://" + pr.getProperty("dbServer") + ":" +
                pr.getProperty("dbPort") + ";databaseName=" +
                pr.getProperty("dbName") + ";encrypt=true;trustServerCertificate=true;",
                pr.getProperty("dbUser"),
                pr.getProperty("dbPassword"));
        con.setAutoCommit(false);
        PreparedStatement ps;
        ps = con.prepareStatement("DELETE FROM TripletexTimer WHERE MONTH(PDate)=? AND YEAR(PDate)=?");
        ps.setInt(1, month + 1);// month is 0-11, sql must have 1-12
        ps.setInt(2, year);
        ps.executeUpdate();
        con.commit();
        ps = con.prepareStatement(
                "INSERT INTO TripletexTimer (ExtId, PDate, ProjectId, EmployeeId, ActivityId, Hours) VALUES (?,?,?,?,?,?)");
        for (int i = 0; i < ja.length(); i++) {
            System.out.print(i + " ");
            JSONObject value = ja.getJSONObject(i);
            ps.setInt(1, value.getInt("id"));
            ps.setString(2, value.getString("date"));
            ps.setInt(3, getIdFromJSON(value, "project"));
            ps.setInt(4, getIdFromJSON(value, "employee"));
            ps.setInt(5, getIdFromJSON(value, "activity"));
            ps.setInt(6, value.getInt("hours"));
            ps.addBatch();
        }
        ps.executeBatch();
        con.commit();
        con.close();
    }

    private void getFromTripletex(Properties pr)throws Exception {
        String result = HTTPClient(pr, HTTPMethod.GET,
                pr.getProperty("tripletexProject"+test), null, null);
        System.out.println(result);
        System.out.println(HTTPClient(pr, HTTPMethod.GET,
                    pr.getProperty("tripletexProject"+test) + "/resourcePlanBudget?projectId=136434216", null, null));
        // if (result!="") return;
        // JSONObject jo = new JSONObject(result);
        // JSONArray ja = new JSONArray(jo.get("values").toString());
        // for (int i = 0; i < ja.length(); i++) {
        //     System.out.print(i + " ");
        //     JSONObject value = ja.getJSONObject(i);
        //     int id = value.getInt("id");
        //     try {
        //     System.out.println(HTTPClient(pr, HTTPMethod.GET,
        //             pr.getProperty("tripletexProject"+test) + "/resourcePlanBudget?projectId=" + id, null, null));
        //     System.out.println("//////////////////////////////////");
        //     } catch (Exception e) {System.out.println("Error: "+e.toString());} 
        // }

    }

    String test="Test";

    public static void main(String[] args) throws Exception {
        System.out.println("************jada***********");
        Tripletex2MSSQLServer tt2mssqls = new Tripletex2MSSQLServer();
        if (args!=null && args.length>0 && args[0].toLowerCase().startsWith("t")) 
            tt2mssqls.test="Test";
        else
            tt2mssqls.test="";
        Properties pr = new Properties();
        pr.loadFromXML(new FileInputStream("TripletexParams.xml"));
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        String month1to12 = month > 8 ? (month + 1) + "" : "0" + (month + 1); // java uses 0-11, but everything else
                                                                              // uses 1-12
        String dateFrom = year + "-" + month1to12 + "-01";
        String dateTo = year + "-" + month1to12 + "-" + getLastDateOfMonths(month, year);
        String query = "dateFrom=" + dateFrom + "&dateTo=" + dateTo;
        tt2mssqls.getFromTripletex(pr);
        // String body =
        // "{\"project\":{\"id\":2264484},\"activity\":{\"id\":48529},\"employee\":{\"id\":1769400},\"hours\":5.5,\"date\":\"2023-06-24\"}";
        // JSONObject jso = new JSONObject(body);
        // System.out.println((jso));
        // String result = HTTPClient(pr,HTTPMethod.POST,null,body);
        String result = tt2mssqls.HTTPClient(pr, HTTPMethod.GET,
                pr.getProperty("tripletexTimesheet"+tt2mssqls.test), query, null);
        JSONObject jo = new JSONObject(result);
        JSONArray ja = new JSONArray(jo.get("values").toString());

        tt2mssqls.InsertTimesheet(result, pr, month, year);

        System.out.println("--------------------------------");
    }

    private static int getLastDateOfMonths(int month, int year) {
        if ("0 2 4 6 7 9 11".contains("" + month))
            return 31;
        if ("3 5 8 10".contains("" + month))
            return 30;
        // only february left
        if (year % 4 == 0)
            return 29;
        else
            return 28;
    }
}

/*
 * Response body
 * Download
 * {
 * "value": {
 * "id": 631962710,
 * "version": 1,
 * "url": "tripletex.no/v2/token/session/631962710",
 * "consumerToken": {
 * "id": 4945,
 * "url": "tripletex.no/v2/token/consumer/4945"
 * },
 * "employeeToken": {
 * "id": 1486292,
 * "url": "tripletex.no/v2/token/employee/1486292"
 * },
 * "expirationDate": "2023-12-31",
 * "token":
 * "eyJ0b2tlbklkIjo2MzE5NjI3MTAsInRva2VuIjoiNjYyNGYzYzctYjNlZi00Nzg5LThlM2YtZmQ3OGFjYmU4ODI3In0=",
 * "encryptionKey": null
 * }
 * }
 */

/*
 * Welcome to Tripletex API 2.0!
 * We are very excited for you to join our growing list of integrations with
 * Tripletex. The API is based on international principles like RESTfull, CRUD
 * and OpenAPI Specifications. First things first: If you need any assistance
 * you can find FAQ, documentation and a contact form to get in touch with our
 * API support team at https://developer.tripletex.no. On this page we also post
 * news that are relevant to integration developers. You can also find some code
 * samples at our GitHub
 * https://github.com/Tripletex/tripletex-api2/tree/master/examples
 * 
 * Your token
 * The tokens below is for access to the production environment (tripletex.no).
 * For access to the test environment (api.tripletex.io) see this article:
 * https://developer.tripletex.no/docs/documentation/getting-started/1-creating-
 * a-test-account/
 * 
 * Every integration is provided a API-token (ConsumerToken) with an application
 * name. Here is your token:
 * ConsumerToken:
 * <eyJ0b2tlbklkIjo0OTQ1LCJ0b2tlbiI6IjNlNTIzMzEwLWUxY2YtNGE2Yi05ZGQxLTYxMWZiMjFlNjQ2NSJ9>
 * Application name: <Nornir_Internal>
 * 
 * If you would like to change your assigned Application name, please contact us
 * via https://developer.tripletex.no/contact-us/. Replies to this email address
 * will not be read.
 * 
 * How to connect to the API:
 * The API module must be activated by an administrator for the Tripletex
 * company and an employee token must be created. End user documentation
 * regarding this can be found here:
 * https://hjelp.tripletex.no/hc/no/articles/4409557117713-Integrasjoner
 * With a ConsumerToken and EmployeeToken you can use the API to create the
 * SessionToken. See
 * https://developer.tripletex.no/docs/documentation/authentication-and-tokens/
 * for details on how to authenticate.
 * 
 * If we need to get in touch
 * We will notify you as the technical contact as well as update our developer
 * portal regarding big changes in functionality and potential breaking changes.
 * Let us know via the contact form in our developer portal if this needs to be
 * changed. https://tripletex.no/v2-docs/ contains our documentation as well as
 * a system to execute API requests.
 * 
 * 
 * We look forward to your feedback from using the API and will answer any
 * questions you have.
 */
