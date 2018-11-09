package commconsistency.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.springframework.context.ApplicationContext;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import commconsistency.dao.CommentScopeRepository;
import commconsistency.dao.RepositoryFactory;
import commconsistency.domain.EndLineVerify;

public class Test {
	public static void main(String[] args) throws IOException {
		MongoClientOptions clientOptions = new MongoClientOptions.Builder().build();
		List<MongoCredential> lstCredentials = Arrays
				.asList(MongoCredential.createMongoCRCredential("liuzhiyong", "admin", "_sysu208".toCharArray()));
		MongoClient client = new MongoClient(new ServerAddress("120.79.66.219", 27027), lstCredentials, clientOptions);
		MongoCollection<Document> verify = client.getDatabase("scopebase").getCollection("r_endline_verify");
		MongoCursor<Document> cursor = verify.find().iterator();
		List<String> verifyLines = new ArrayList<String>();
		int count = 0;
		while (cursor.hasNext()) {
			Document doc = cursor.next();
			int id = (Integer) doc.get("comment_id");
			int end = (Integer) doc.get("endline");
			String temp = id + ":" + end;
			System.out.println(temp);
			verifyLines.add(temp);
			count++;
		}
		System.out.println("Overall: " + count);
		String file = "D:/data/comment_verify_projects_v2.txt";
		FileUtils.writeLines(new File(file), verifyLines);
		System.out.println("Saved to " + file + " successfully!");
		client.close();
	}
}
