package commconsistency.domain;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import commconsistency.dao.CommentScopeRepository;
import commconsistency.dao.RepositoryFactory;
import commconsistency.dao.SubCommentEntryRepository;
import commconsistency.service.CommentScopeService;

public class Test {
	
	public static void main(String[] args) throws IOException {
		MongoDatabase database = new MongoClient("192.168.1.54", 27017).getDatabase("scopebase");
		
	}

}
