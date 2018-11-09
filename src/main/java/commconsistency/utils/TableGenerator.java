package commconsistency.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

public class TableGenerator {
	static Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

	static {
		root.setLevel(Level.INFO);
	}

	public static void generateCommentScopeTable() {
		MongoDatabase database = new MongoClient("192.168.1.54", 27017).getDatabase("scopebase");
		MongoCollection<Document> classes = database.getCollection("class_message");
		MongoCollection<Document> comments = database.getCollection("comment");
		MongoCollection<Document> scopeComments = database.getCollection("comment_scope");

		BasicDBObject query = new BasicDBObject();
		// query.put("project", "struts");

		FindIterable<Document> iter = comments.find();
		MongoCursor<Document> cursor = iter.iterator();
		while (cursor.hasNext()) {
			Document doc = cursor.next();
			// if(!doc.getString("type").equals("purpose ")) {
			// continue;
			// }
			int classID = doc.getInteger("class_id");
			List<String> commentStrings = (List<String>) doc.get("comment");
			List<String> codeStrings = (List<String>) doc.get("codes");
			String className = (String) doc.get("class_name");

			List<String> words = new ArrayList<String>();
			String splitToken = " .,;:/&|`~%+=-*<>$#@!^\\()[]{}''\"\r\n\t";
			for (String sentense : commentStrings) {
				StringTokenizer st = new StringTokenizer(sentense, splitToken, false);
				while (st.hasMoreTokens()) {
					words.add(st.nextToken());
				}
			}
			if (words.size() < 5) {
				continue;
			}
			if (codeStrings.size() > 30 || codeStrings.size() < 3) {
				continue;
			}

			int startLine = doc.getInteger("scope_start_line");
			int endLine = doc.getInteger("scope_end_line");
			BasicDBObject query2 = new BasicDBObject();
			query2.put("class_id", classID);
			Document clazz = classes.find(query2).first();

			List<String> codes = (List<String>) clazz.get("codes");

			StringBuilder sb = new StringBuilder();
			for (String code : codes) {
				sb.append(code).append("\r\n");
			}

			ASTParser astParser = ASTParser.newParser(AST.JLS8);
			astParser.setSource(sb.toString().toCharArray());
			astParser.setKind(ASTParser.K_COMPILATION_UNIT);

			CompilationUnit unit = (CompilationUnit) (astParser.createAST(null));
			TypeDeclaration type = (TypeDeclaration) unit.types().get(0);

			MethodDeclaration[] methods = type.getMethods();
			int methodStartLine = 0, methodEndLine = 0;
			for (MethodDeclaration method : methods) {
				int t_methodStartLine = unit.getLineNumber(method.getStartPosition());
				int t_methodEndLine = unit.getLineNumber(method.getStartPosition() + method.getLength() - 1);

				if (t_methodStartLine <= startLine && t_methodEndLine >= endLine) {
					methodStartLine = t_methodStartLine;
					methodEndLine = t_methodEndLine;
					break;
				}
			}

			Document scopeDocument = new Document();
			scopeDocument.put("comment_id", doc.get("comment_id"));
			scopeDocument.put("class_id", doc.get("class_id"));
			scopeDocument.put("project", clazz.get("project"));
			scopeDocument.put("class_name", clazz.get("class_name"));
			scopeDocument.put("type", doc.get("type"));
			scopeDocument.put("method_start_line", methodStartLine);
			scopeDocument.put("method_end_line", methodEndLine);
			scopeDocument.put("scope_start_line", startLine);
			scopeDocument.put("scope_end_line", endLine);
			scopeDocument.put("comment_start_line", doc.get("comment_start_line"));
			scopeDocument.put("comment_end_line", doc.get("comment_end_line"));
			scopeDocument.put("codes", codes);
			List<Integer> verifyScopeEndLine = new ArrayList<Integer>();
			scopeDocument.put("vscope_end_line", verifyScopeEndLine);
			scopeComments.insertOne(scopeDocument);

		}

	}

	public static void generateSubCommentScopeTable() {
		MongoDatabase database = new MongoClient("192.168.1.54", 27017).getDatabase("scopebase");
		MongoCollection<Document> scopeComments = database.getCollection("comment_scope");
		MongoCollection<Document> subScopeComments = database.getCollection("sub_comment_scope");
		FindIterable<Document> iter = scopeComments.find();
		MongoCursor<Document> cursor = iter.iterator();
		while (cursor.hasNext()) {
			Document doc = cursor.next();
			Document subScope = new Document();

			subScope.append("comment_id", doc.getInteger("comment_id"));
			subScope.append("project", doc.getString("project"));
			subScope.append("class_name", doc.getString("class_name"));
			subScope.append("type", doc.getString("type"));
			subScope.append("isverify", false);
			subScopeComments.insertOne(subScope);
		}

	}

	public static void generateSubCommentScopeTable4Project(String project) throws IOException {
		List<String> lines = FileUtils.readLines(new File("data/verifyid_v2.csv"), "UTF-8");
		List<Integer> IDs = new ArrayList<Integer>();
		for (String line : lines) {
			IDs.add(Integer.valueOf(line.split(",")[0]));
		}
		MongoDatabase database = new MongoClient("192.168.1.54", 27017).getDatabase("scopebase");
		MongoCollection<Document> rScopeComments = database.getCollection("r_comment_scope_jss");
		MongoCollection<Document> scopeComments = database.getCollection("comment_scope");
		MongoCollection<Document> subScopeComments = database.getCollection("r_sub_comment_scope_jss");
		//MongoCollection<Document> comments = database.getCollection("comment");
		int count = 0;
		Set<Integer> set = new HashSet<Integer>();
		while (count < 600) {
			Random r = new Random();
			int min = 1053000;
			int max = 1088000;
			int randID = min + r.nextInt(max - min) + 1;
			
			Document query = new Document();
			query.put("comment_id", randID);
			FindIterable<Document> iter = scopeComments.find(query);
			if (iter != null && iter.first() != null) {
				Document doc = iter.first();
				
				String name = doc.getString("project");
				int commentID = doc.getInteger("comment_id");
				int commentStart = doc.getInteger("comment_start_line");
				int commentEnd = doc.getInteger("comment_end_line");
				List<String> codes = (List<String>) doc.get("codes");
				String commentString = "";
				for(int i = commentStart; i <= commentEnd; i++) {
					commentString += codes.get(i - 1);
				}
				System.out.println(commentString);
				if(StringFilter(commentString).equals("")) {
					System.out.println("Error!");
					continue;
				}
				System.out.println(name);
				if (!project.equals(name)) {
					continue;
				}
				//int commentID = doc.getInteger("comment_id");
			
				if (IDs.contains(commentID) && set.contains(commentID)) {
					System.out.println("Duplicated!");
					continue;
				}
				set.add(commentID);	
				Document subScope = new Document();
				subScope.append("comment_id", doc.getInteger("comment_id"));
				subScope.append("project", doc.getString("project"));
				subScope.append("class_name", doc.getString("class_name"));
				subScope.append("type", doc.getString("type"));
				subScope.append("isverify", false);
				subScopeComments.insertOne(subScope);
				doc.remove("_id");
				rScopeComments.insertOne(doc);
				count++;
				
				System.out.println(commentID);
				
			}
		}
		System.out.println("Insert " + count + " for " + project);

	}
	
	// 过滤特殊字符 
	public static String StringFilter(String str) throws PatternSyntaxException { 
	// 只允许字母和数字 // String regEx ="[^a-zA-Z0-9]"; 
	// 清除掉所有特殊字符 
		String regEx="[`~!@#$%^&*()+=|{}':;',//\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]"; 
		Pattern p = Pattern.compile(regEx); 
		Matcher m = p.matcher(str);
		return m.replaceAll("").trim();
	} 
	
	public static void getCommentIDList(String file) throws IOException {
		MongoDatabase database = new MongoClient("192.168.1.54", 27017).getDatabase("scopebase");
		MongoCollection<Document> subScopeComments = database.getCollection("r_sub_comment_scope_jss");
		FindIterable<Document> iter = subScopeComments.find();
		MongoCursor<Document> cursor = iter.iterator();
		List<String> result = new ArrayList<String>();
		while (cursor.hasNext()) {
			Document doc = cursor.next();
			String commentID = String.valueOf(doc.get("comment_id"));
			result.add(commentID);
		}
		FileUtils.writeLines(new File(file), result);
	}

	public static void main(String[] args) throws IOException {

		// generateCommentScopeTable();
		//
		// generateSubCommentScopeTable();

		//generateSubCommentScopeTable4Project("jdk");
		getCommentIDList("src/main/resources/file/r_commentidlist.txt");
	}

}
