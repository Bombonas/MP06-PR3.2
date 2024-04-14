package cat.iesesteveterradas;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.basex.api.client.ClientSession;
import org.basex.core.BaseXException;
import org.basex.core.cmd.Open;
import org.basex.core.cmd.XQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;

import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonNumber;
import org.bson.BsonString;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.w3c.dom.Node;


public class PR32CreateMain {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);   
    
    public static void main(String[] args) throws IOException {
        String host = "127.0.0.1";
        int port = 1984;
        String username = "admin"; // Default username
        String password = "admin"; // Default password

        // Establish a connection to the BaseX server
        try (ClientSession session = new ClientSession(host, port, username, password)) {
            logger.info("Connected to BaseX server.");

            session.execute(new Open("BBDD-PR3")); 

            String myQuery = """
                declare option output:method "xml";
                declare option output:indent "yes";
                <questions>{
                for $question at $i in /posts/row[@PostTypeId='1'] 
                where $i <= 50
                return
                    <question>{
                    $question/@Id,
                    $question/@PostTypeId,
                    $question/@AcceptedAnswerId,
                    $question/@CreationDate,
                    $question/@Score,
                    $question/@ViewCount,
                    $question/@Body,
                    $question/@OwnerUserId,
                    $question/@LastActivityDate,
                    $question/@Title,
                    $question/@Tags,
                    $question/@AnswerCount,
                    $question/@CommentCount,
                    $question/@ContentLicense
                    }</question>
                }
                </questions>                 
            """;

            // Execute the query
            String result = session.execute(new XQuery(myQuery));

            //@SuppressWarnings("deprecation")
            //String escapedResult = StringEscapeUtils.unescapeHtml4(result);

            logger.info("Query Result (Escaped HTML Entities):");
            //logger.info(escapedResult);

            try (var mongoClient = MongoClients.create("mongodb://root:example@localhost:27017")) {
                MongoDatabase database = mongoClient.getDatabase("PR32"); 
                MongoCollection<Document> collection = database.getCollection("Posts");
                
                org.jsoup.nodes.Document doc = Jsoup.parse(result, "", Parser.xmlParser());
                Elements rows = doc.select("question");
                
                for (Element row : rows) {
                    String id = row.attr("Id");
                    String postTypeId = row.attr("PostTypeId");
                    String acceptedAnswerId = row.attr("AcceptedAnswerId");
                    String creationDate = row.attr("CreationDate");
                    String score = row.attr("Score");
                    String body = row.attr("Body");
                    String ownerUserId = row.attr("OwnerUserId");
                    String lastActivityDate = row.attr("LastActivityDate");
                    String title = row.attr("Title");
                    String tags = row.attr("Tags");
                    String answerCount = row.attr("AnswerCount");
                    String commentCount = row.attr("CommentCount");
                    String contentLicense = row.attr("ContentLicense");
                    int viewCount = Integer.parseInt(row.attr("ViewCount"));
    
                    org.bson.Document document = new org.bson.Document();
                    document.put("id", new BsonString(id));
                    document.put("postTypeId", new BsonString(postTypeId));
                    document.put("acceptedAnswerId", new BsonString(acceptedAnswerId));
                    document.put("creationDate", new BsonString(creationDate));
                    document.put("score", new BsonString(score));
                    document.put("body", new BsonString(body));
                    document.put("ownerUserId", new BsonString(ownerUserId));
                    document.put("lastActivityDate", new BsonString(lastActivityDate));
                    document.put("title", new BsonString(title));
                    document.put("tags", new BsonString(tags));
                    document.put("answerCount", new BsonString(answerCount));
                    document.put("commentCount", new BsonString(commentCount));
                    document.put("contentLicense", new BsonString(contentLicense));
                    document.put("viewCount", new BsonInt32(viewCount));
                    
                    collection.insertOne(document);
            }
                
            } catch (Exception e) {
                System.err.println("An error occurred: " + e.getMessage());
            }

            
        } catch (BaseXException e) {
            logger.error("Error connecting or executing the query: " + e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }       
    }
}
