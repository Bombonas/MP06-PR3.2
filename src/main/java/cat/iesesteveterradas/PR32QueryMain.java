package cat.iesesteveterradas;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

public class PR32QueryMain {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);   
    public static void main(String[] args) {
        // Connectar-se a MongoDB (substitueix amb la teva URI de connexió)
        try (var mongoClient = MongoClients.create("mongodb://root:example@localhost:27017")) {
            MongoDatabase database = mongoClient.getDatabase("PR32"); 
            MongoCollection<Document> collection = database.getCollection("Posts");
            logger.info("Connected to MongoDB");

            // Exercici 1
            List<Document> allDocuments = new ArrayList<>();
            MongoCursor<Document> allCursor = collection.find().iterator();
            while (allCursor.hasNext()) {
                allDocuments.add(allCursor.next());
            }

            double totalViewCount = 0;
            for (Document doc : allDocuments) {
                totalViewCount += doc.getInteger("viewCount");
            }
            double averageViewCount = totalViewCount / allDocuments.size();
            logger.info("Average viewCount: " + averageViewCount);

            Document query = new Document("viewCount", new Document("$gt", averageViewCount));

            // new Document("$expr",
            //     new Document("$gt", List.of("$viewCount", averageViewCount)));
            
            // Realitzar la consulta
            FindIterable<Document> result = collection.find(query);

            logger.info("Query 1 done");

            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage();
                doc.addPage(page);

                // Inicializar contenido del PDF
                try (PDPageContentStream contents = new PDPageContentStream(doc, page)) {
                    contents.beginText();
                    contents.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    contents.setLeading(14.5f);
                    contents.newLineAtOffset(25, 750);

                    // Agregar contenido al PDF
                    for (Document docMongoDB : result) {
                        contents.showText(docMongoDB.getString("title") + "; ViewCounts: " + docMongoDB.getInteger("viewCount"));
                        contents.newLine();
                    }
                    contents.endText();
                }

                // Guardar el PDF
                String outputPath = System.getProperty("user.dir") + "/data/out/informe1.pdf";
                doc.save(outputPath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Exercici 2
            // No tinc resultats amb les paraules per defecte asi que he agragat les paraules D&D i hat que si que se que hi estan.
            List<String> wordsToSearch = Arrays.asList("pug", "wig", "yak", "nap", "jig", "mug", "zap", "gag", "oaf", "elf", "hat", "D&D");
            String regexPattern = String.join("|", wordsToSearch);

            query = new Document("title", new Document("$regex", regexPattern));
            result = collection.find(query);

            logger.info("Query 2 done");

            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage();
                doc.addPage(page);

                // Inicializar contenido del PDF
                try (PDPageContentStream contents = new PDPageContentStream(doc, page)) {
                    contents.beginText();
                    contents.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    contents.setLeading(14.5f);
                    contents.newLineAtOffset(25, 750);

                    // Agregar contenido al PDF
                    for (Document docMongoDB : result) {
                        contents.showText(docMongoDB.getString("title"));
                        contents.newLine();
                    }
                    contents.endText();
                }

                // Guardar el PDF
                String outputPath = System.getProperty("user.dir") + "/data/out/informe2.pdf";
                doc.save(outputPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
        }
    }
}
